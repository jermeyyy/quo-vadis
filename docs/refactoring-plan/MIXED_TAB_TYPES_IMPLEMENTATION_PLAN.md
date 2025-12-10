# Mixed Tab Types Implementation Plan

> **Status**: Approved  
> **Created**: December 10, 2025  
> **Author**: Architect Agent  

## Overview

This plan implements support for mixed tab types in the Quo Vadis navigation library, allowing a `@Tabs` container to include both:
- **Flat screen tabs**: Simple `data object` destinations with no internal navigation
- **Nested stack tabs**: `sealed class` stacks with their own navigation hierarchy

## Target Definition

The goal is to make this definition work properly:

```kotlin
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsTab::class]
)
@Destination(route = "main/tabs")
sealed class MainTabs : DestinationDefinition {

    // Flat screen tab - single screen, no internal navigation
    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()

    // Nested stack tab - has internal navigation
    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.SettingsMain::class)
    sealed class SettingsTab : DestinationDefinition {
        @Destination(route = "settings/tab")
        data object SettingsMain : SettingsTab()
        
        @Destination(route = "settings/advanced")
        data object SettingsAdvanced : SettingsTab()
    }
}
```

## Requirements Summary

| Requirement | Decision |
|-------------|----------|
| Simple tabs (HomeTab, etc.) | Flat screens → become single-screen StackNodes |
| Nested stack tabs (SettingsTab) | Explicitly annotated with `@Stack` |
| MainTabs as Destination | Yes - navigable via `navigator.navigate(MainTabs)` |
| MainTabs in other Stacks | Yes - can be pushed onto another stack |
| Tab type detection | Auto-infer from annotation combinations |
| TabMetadata generation | Yes - from `@TabItem` annotation |
| Navigation to specific tab | Lower priority (can be added later) |
| Annotation naming | Keep `@Tabs` as primary |

---

## Phase 1: Annotation Model Updates

### Goal
Update `@Stack` annotation to support KClass-based `startDestination` for type safety.

### Files to Modify

| File | Path |
|------|------|
| Stack.kt | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt` |

### Changes

**Current**:
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Stack(
    val name: String,
    val startDestination: String = ""
)
```

**Proposed**:
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Stack(
    val name: String,
    val startDestination: String = "",
    val startDestinationClass: KClass<*> = Unit::class  // NEW: Type-safe alternative
)
```

### Behavior
- If `startDestinationClass != Unit::class`, use it (type-safe)
- Otherwise, fall back to string-based `startDestination`
- Empty string + Unit::class = use first destination in declaration order

---

## Phase 2: KSP Model Updates

### Goal
Update `TabItemInfo` to support both flat screen tabs and nested stack tabs.

### Files to Modify

| File | Path |
|------|------|
| TabInfo.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt` |
| StackInfo.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/StackInfo.kt` |

### Changes to TabInfo.kt

**Add enum**:
```kotlin
/**
 * Distinguishes between flat screen tabs and nested stack tabs.
 */
enum class TabItemType {
    /** Tab is a single screen destination (data object with @Destination) */
    FLAT_SCREEN,
    /** Tab has its own navigation stack (sealed class with @Stack) */
    NESTED_STACK
}
```

**Update TabItemInfo**:
```kotlin
/**
 * Extracted metadata from a @TabItem annotation.
 *
 * Supports two tab types:
 *
 * ## Flat Screen Tab
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Destination(route = "main/home")
 * data object HomeTab : MainTabs()
 * ```
 * - [tabType] = FLAT_SCREEN
 * - [destinationInfo] = DestinationInfo for HomeTab
 * - [stackInfo] = null
 *
 * ## Nested Stack Tab
 * ```kotlin
 * @TabItem(label = "Settings", icon = "settings")
 * @Stack(name = "settingsStack", startDestinationClass = SettingsMain::class)
 * sealed class SettingsTab : Destination { ... }
 * ```
 * - [tabType] = NESTED_STACK
 * - [destinationInfo] = null
 * - [stackInfo] = StackInfo for SettingsTab
 */
data class TabItemInfo(
    val label: String,
    val icon: String,
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType,
    val destinationInfo: DestinationInfo?,  // For FLAT_SCREEN
    val stackInfo: StackInfo?,               // For NESTED_STACK
    // Legacy field - deprecated
    @Deprecated("Use tabType, destinationInfo, or stackInfo instead")
    val rootGraphClass: KSClassDeclaration? = null
)
```

### Changes to StackInfo.kt

**Add KClass support**:
```kotlin
data class StackInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val destinations: List<DestinationInfo>,
    val startDestinationString: String,           // Legacy string-based
    val startDestinationClass: KSClassDeclaration?, // NEW: Type-safe
    val resolvedStartDestination: DestinationInfo?
)
```

---

## Phase 3: KSP Extractor Updates

### Goal
Update extractors to auto-detect tab types and support KClass-based startDestination.

### Files to Modify

| File | Path |
|------|------|
| TabExtractor.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt` |
| StackExtractor.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/StackExtractor.kt` |

### Changes to TabExtractor.kt

**Add type detection method**:
```kotlin
/**
 * Detects whether a @TabItem is a flat screen or nested stack based on annotations.
 *
 * Detection rules:
 * - @TabItem + @Stack + (sealed class) → NESTED_STACK
 * - @TabItem + @Destination + (data object) → FLAT_SCREEN
 */
private fun detectTabItemType(classDeclaration: KSClassDeclaration): TabItemType {
    val hasStack = classDeclaration.annotations.any { 
        it.shortName.asString() == "Stack" 
    }
    val hasDestination = classDeclaration.annotations.any { 
        it.shortName.asString() == "Destination" 
    }
    val isDataObject = classDeclaration.classKind == ClassKind.OBJECT
    val isSealedClass = classDeclaration.modifiers.contains(Modifier.SEALED)
    
    return when {
        hasStack && isSealedClass -> TabItemType.NESTED_STACK
        hasDestination && isDataObject -> TabItemType.FLAT_SCREEN
        else -> {
            logger.error(
                "@TabItem '${classDeclaration.simpleName.asString()}' must be either:\n" +
                "  - data object with @Destination (flat screen tab)\n" +
                "  - sealed class with @Stack (nested stack tab)",
                classDeclaration
            )
            TabItemType.FLAT_SCREEN // Default to avoid crash
        }
    }
}
```

**Update extraction to use StackExtractor**:
```kotlin
private fun extractTabItemNewPattern(
    classDeclaration: KSClassDeclaration,
    stackExtractor: StackExtractor  // NEW parameter
): TabItemInfo? {
    val tabItemAnnotation = classDeclaration.annotations.find {
        it.shortName.asString() == "TabItem"
    } ?: return null

    val label = /* extract label */
    val icon = /* extract icon */
    
    val tabType = detectTabItemType(classDeclaration)
    
    val destinationInfo: DestinationInfo?
    val stackInfo: StackInfo?
    
    when (tabType) {
        TabItemType.FLAT_SCREEN -> {
            destinationInfo = destinationExtractor.extract(classDeclaration)
            stackInfo = null
        }
        TabItemType.NESTED_STACK -> {
            destinationInfo = null
            stackInfo = stackExtractor.extract(classDeclaration)
        }
    }
    
    return TabItemInfo(
        label = label,
        icon = icon,
        classDeclaration = classDeclaration,
        tabType = tabType,
        destinationInfo = destinationInfo,
        stackInfo = stackInfo
    )
}
```

### Changes to StackExtractor.kt

**Support KClass-based startDestination**:
```kotlin
fun extract(classDeclaration: KSClassDeclaration): StackInfo? {
    val annotation = classDeclaration.annotations.find {
        it.shortName.asString() == "Stack"
    } ?: return null

    val name = /* extract name */
    val startDestinationString = /* extract startDestination string */
    
    // NEW: Extract KClass-based startDestination
    val startDestinationClassArg = annotation.arguments.find {
        it.name?.asString() == "startDestinationClass"
    }?.value as? KSType
    
    val startDestinationClass = startDestinationClassArg?.let { type ->
        val classDecl = type.declaration as? KSClassDeclaration
        val qualifiedName = classDecl?.qualifiedName?.asString()
        if (qualifiedName == "kotlin.Unit") null else classDecl
    }
    
    // Resolve start destination
    val resolvedStartDestination = resolveStartDestination(
        destinations = destinations,
        startDestinationClass = startDestinationClass,
        startDestinationString = startDestinationString
    )
    
    return StackInfo(
        classDeclaration = classDeclaration,
        name = name,
        className = classDeclaration.simpleName.asString(),
        packageName = classDeclaration.packageName.asString(),
        destinations = destinations,
        startDestinationString = startDestinationString,
        startDestinationClass = startDestinationClass,
        resolvedStartDestination = resolvedStartDestination
    )
}

private fun resolveStartDestination(
    destinations: List<DestinationInfo>,
    startDestinationClass: KSClassDeclaration?,
    startDestinationString: String
): DestinationInfo? {
    // Prefer KClass if available
    if (startDestinationClass != null) {
        val qualifiedName = startDestinationClass.qualifiedName?.asString()
        return destinations.find { 
            it.classDeclaration.qualifiedName?.asString() == qualifiedName 
        }
    }
    
    // Fall back to string matching
    if (startDestinationString.isNotEmpty()) {
        return destinations.find { it.className == startDestinationString }
    }
    
    // Default to first destination
    return destinations.firstOrNull()
}
```

---

## Phase 4: NavNode Builder Generator Updates

### Goal
Generate proper NavNode tree with single-screen stacks for flat tabs.

### Files to Modify

| File | Path |
|------|------|
| NavNodeBuilderGenerator.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt` |

### Changes

**Update buildTabNodeCode**:
```kotlin
private fun buildTabNodeCode(tabInfo: TabInfo): CodeBlock {
    val builder = CodeBlock.builder()
        .add("return %T(\n", TAB_NODE)
        .indent()
        .add("key = key,\n")
        .add("parentKey = parentKey,\n")
        .add("stacks = listOf(\n")
        .indent()

    tabInfo.tabs.forEachIndexed { index, tabItem ->
        val tabKey = tabItem.classDeclaration.simpleName.asString().lowercase()
        
        when (tabItem.tabType) {
            TabItemType.FLAT_SCREEN -> {
                // Generate inline single-screen StackNode
                builder.add(buildFlatScreenStackCode(tabInfo, tabItem, tabKey))
            }
            TabItemType.NESTED_STACK -> {
                // Call the generated builder function
                val stackName = tabItem.stackInfo!!.className
                builder.add(
                    "build${stackName}NavNode(key = %P, parentKey = key)",
                    "\$key/$tabKey"
                )
            }
        }
        
        if (index < tabInfo.tabs.size - 1) {
            builder.add(",")
        }
        builder.add("\n")
    }

    builder
        .unindent()
        .add("),\n")
        .add("activeStackIndex = initialTabIndex\n")
        .unindent()
        .add(")\n")

    return builder.build()
}

private fun buildFlatScreenStackCode(
    tabInfo: TabInfo,
    tabItem: TabItemInfo,
    tabKey: String
): CodeBlock {
    val destInfo = tabItem.destinationInfo!!
    val destinationClassName = ClassName(tabInfo.packageName, tabInfo.className)
    val destSimpleName = destInfo.className
    
    return CodeBlock.builder()
        .add("%T(\n", STACK_NODE)
        .indent()
        .add("key = %P,\n", "\$key/$tabKey")
        .add("parentKey = key,\n")
        .add("children = listOf(\n")
        .indent()
        .add("%T(\n", SCREEN_NODE)
        .indent()
        .add("key = %P,\n", "\$key/$tabKey/${destSimpleName.lowercase()}")
        .add("parentKey = %P,\n", "\$key/$tabKey")
        .add("destination = %T.%L\n", destinationClassName, destSimpleName)
        .unindent()
        .add(")\n")
        .unindent()
        .add(")\n")
        .unindent()
        .add(")")
        .build()
}
```

### Generated Output Example

For MainTabs, the generator will produce:

```kotlin
fun buildMainTabsNavNode(
    key: String = "mainTabs-tabs",
    parentKey: String? = null,
    initialTabIndex: Int = 0
): TabNode {
    return TabNode(
        key = key,
        parentKey = parentKey,
        stacks = listOf(
            // HomeTab - flat screen
            StackNode(
                key = "$key/hometab",
                parentKey = key,
                children = listOf(
                    ScreenNode(
                        key = "$key/hometab/hometab",
                        parentKey = "$key/hometab",
                        destination = MainTabs.HomeTab
                    )
                )
            ),
            // ExploreTab - flat screen
            StackNode(
                key = "$key/exploretab",
                parentKey = key,
                children = listOf(
                    ScreenNode(
                        key = "$key/exploretab/exploretab",
                        parentKey = "$key/exploretab",
                        destination = MainTabs.ExploreTab
                    )
                )
            ),
            // ProfileTab - flat screen
            StackNode(
                key = "$key/profiletab",
                parentKey = key,
                children = listOf(
                    ScreenNode(
                        key = "$key/profiletab/profiletab",
                        parentKey = "$key/profiletab",
                        destination = MainTabs.ProfileTab
                    )
                )
            ),
            // SettingsTab - nested stack
            buildSettingsTabNavNode(key = "$key/settingstab", parentKey = key)
        ),
        activeStackIndex = initialTabIndex
    )
}
```

---

## Phase 5: Screen Registry Integration

### Goal
Ensure flat tab destinations are registered in screen registry for rendering.

### Files to Modify

| File | Path |
|------|------|
| ScreenRegistryGenerator.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScreenRegistryGenerator.kt` |

### Changes

The existing generator processes `@Screen` annotations to register composables. For flat tabs:

1. **User must provide `@Screen` composable** for each flat tab destination:
   ```kotlin
   @Screen(MainTabs.HomeTab::class)
   @Composable
   fun HomeTabScreen(navigator: Navigator) {
       // Home tab content
   }
   ```

2. **Generator already handles this** - no changes needed if `@Destination` objects are processed

3. **Verify** that nested sealed class members (like `MainTabs.HomeTab`) are correctly identified

### Verification Needed
- Confirm `DestinationExtractor` handles nested objects like `MainTabs.HomeTab`
- Test that generated registry includes all tab destinations

---

## Phase 6: TabMetadata Generation

### Goal
Generate `TabMetadata` list from `@TabItem` annotations for `TabWrapperScope`.

### Files to Modify

| File | Path |
|------|------|
| NavNodeBuilderGenerator.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt` |

### Changes

**Add metadata function generation**:
```kotlin
fun generateTabBuilder(tabInfo: TabInfo, stackBuilders: Map<String, StackInfo>) {
    val packageName = "${tabInfo.packageName}.$GENERATED_PACKAGE_SUFFIX"
    val fileName = "${tabInfo.className}NavNodeBuilder"

    val fileSpec = FileSpec.builder(packageName, fileName)
        .addFileComment(FILE_COMMENT)
        .apply {
            // Existing imports...
            
            // Add TabMetadata import
            addImport(
                "com.jermey.quo.vadis.core.navigation.compose.wrapper",
                "TabMetadata"
            )
        }
        .addFunction(buildTabNodeFunction(tabInfo))
        .addFunction(buildTabMetadataFunction(tabInfo))  // NEW
        .build()

    fileSpec.writeTo(codeGenerator, Dependencies(false))
}

private fun buildTabMetadataFunction(tabInfo: TabInfo): FunSpec {
    return FunSpec.builder("get${tabInfo.className}Metadata")
        .addKdoc(
            """
            |Returns tab metadata for the "${tabInfo.name}" tab container.
            |
            |This list contains label and icon information for each tab,
            |in the same order as the stacks in the [TabNode].
            |
            |@return List of [TabMetadata] for all tabs
            """.trimMargin()
        )
        .returns(
            ClassName("kotlin.collections", "List")
                .parameterizedBy(TAB_METADATA)
        )
        .addCode(buildTabMetadataCode(tabInfo))
        .build()
}

private fun buildTabMetadataCode(tabInfo: TabInfo): CodeBlock {
    val builder = CodeBlock.builder()
        .add("return listOf(\n")
        .indent()
    
    tabInfo.tabs.forEachIndexed { index, tabItem ->
        builder.add(
            "%T(label = %S, icon = %S)%L\n",
            TAB_METADATA,
            tabItem.label,
            tabItem.icon,
            if (index < tabInfo.tabs.size - 1) "," else ""
        )
    }
    
    builder
        .unindent()
        .add(")\n")
    
    return builder.build()
}
```

### Generated Output

```kotlin
fun getMainTabsMetadata(): List<TabMetadata> = listOf(
    TabMetadata(label = "Home", icon = "home"),
    TabMetadata(label = "Explore", icon = "explore"),
    TabMetadata(label = "Profile", icon = "person"),
    TabMetadata(label = "Settings", icon = "settings")
)
```

---

## Phase 7: MainTabs as Navigable Destination

### Goal
Enable navigation to MainTabs via `navigator.navigate(MainTabs)` or within stacks.

### Analysis

The `@Destination(route = "main/tabs")` on MainTabs makes it a navigable destination. For this to work:

1. **DeepLink Handler** needs to recognize the route and create the TabNode
2. **TreeNavigator** needs to support pushing a TabNode onto a stack
3. **Screen Registry** needs to map MainTabs to its TabWrapper

### Files to Potentially Modify

| File | Path | Change |
|------|------|--------|
| DeepLinkHandlerGenerator.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt` | Handle routes to @Tabs containers |
| TreeNavigator.kt | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt` | May need updates for TabNode navigation |

### Considerations

1. **When navigating to MainTabs**:
   - Create the full TabNode tree
   - Push it onto the current stack (or replace root)
   - Hierarchical renderer handles TabNode rendering

2. **Deep linking to `main/tabs`**:
   - DeepLinkHandler resolves to MainTabs destination
   - Navigation creates TabNode with default initial tab

3. **Future: Navigation to specific tab**:
   - Deep link: `main/tabs/home` or `main/home`
   - Sets `initialTabIndex` when creating TabNode

### Implementation Detail

The existing infrastructure may already support this since:
- `@Destination` makes it a valid navigation target
- `@TabWrapper(MainTabs::class)` connects wrapper composable
- `WrapperRegistry` maps destination to wrapper
- `NavTreeRenderer` handles TabNode with wrappers

**Testing Required**: Verify if current implementation handles this case.

---

## Phase 8: Validation

### Goal
Add compile-time validation for the new mixed tab types pattern.

### Files to Modify

| File | Path |
|------|------|
| ValidationEngine.kt | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt` |

### Validation Rules

```kotlin
class TabValidationRules {
    
    /**
     * Validates @TabItem annotation usage.
     */
    fun validateTabItem(classDecl: KSClassDeclaration, logger: KSPLogger): Boolean {
        val hasTabItem = classDecl.hasAnnotation("TabItem")
        if (!hasTabItem) return true  // Not a tab item
        
        val hasStack = classDecl.hasAnnotation("Stack")
        val hasDestination = classDecl.hasAnnotation("Destination")
        val isDataObject = classDecl.classKind == ClassKind.OBJECT
        val isSealedClass = classDecl.modifiers.contains(Modifier.SEALED)
        
        // Rule 1: Must have either @Stack or @Destination (not both, not neither)
        if (hasStack && hasDestination) {
            logger.error(
                "@TabItem cannot have both @Stack and @Destination. " +
                "Use @Destination for flat screens, @Stack for nested navigation.",
                classDecl
            )
            return false
        }
        
        if (!hasStack && !hasDestination) {
            logger.error(
                "@TabItem must have either @Destination (flat screen) or @Stack (nested). " +
                "Missing required annotation.",
                classDecl
            )
            return false
        }
        
        // Rule 2: @Destination tab must be data object
        if (hasDestination && !isDataObject) {
            logger.error(
                "@TabItem with @Destination must be a data object. " +
                "Found: ${classDecl.classKind}",
                classDecl
            )
            return false
        }
        
        // Rule 3: @Stack tab must be sealed class
        if (hasStack && !isSealedClass) {
            logger.error(
                "@TabItem with @Stack must be a sealed class. " +
                "Found: ${if (isDataObject) "data object" else "class"}",
                classDecl
            )
            return false
        }
        
        // Rule 4: @Stack tab must have at least one @Destination child
        if (hasStack) {
            val hasDestinationChild = classDecl.getSealedSubclasses().any { subclass ->
                subclass.hasAnnotation("Destination")
            }
            if (!hasDestinationChild) {
                logger.error(
                    "@TabItem with @Stack must have at least one @Destination subclass.",
                    classDecl
                )
                return false
            }
        }
        
        return true
    }
    
    /**
     * Validates @Tabs container items array.
     */
    fun validateTabsItems(tabsClassDecl: KSClassDeclaration, items: List<KSClassDeclaration>, logger: KSPLogger): Boolean {
        var valid = true
        
        items.forEach { itemClass ->
            if (!itemClass.hasAnnotation("TabItem")) {
                logger.error(
                    "@Tabs.items contains '${itemClass.simpleName.asString()}' which is not annotated with @TabItem.",
                    tabsClassDecl
                )
                valid = false
            }
        }
        
        if (items.isEmpty()) {
            logger.error(
                "@Tabs must have at least one item in the items array.",
                tabsClassDecl
            )
            valid = false
        }
        
        return valid
    }
}
```

---

## Implementation Order & Dependencies

```
Phase 1: Annotation Updates ─────┐
                                 │
Phase 2: Model Updates ──────────┼─→ Phase 3: Extractor Updates
                                 │
                                 └─→ Phase 8: Validation
                                            │
                                            ▼
                        Phase 4: NavNode Builder Updates
                                            │
                                            ▼
                        Phase 5: Screen Registry Integration
                                            │
                                            ▼
                        Phase 6: TabMetadata Generation
                                            │
                                            ▼
                        Phase 7: Navigation Integration
```

### Suggested Implementation Sequence

1. **Phase 1** - Quick annotation change
2. **Phase 2** - Model foundation
3. **Phase 3** - Extraction logic
4. **Phase 8** - Validation (can be done with Phase 3)
5. **Phase 4** - Code generation
6. **Phase 5** - Verify registry (may be no-op)
7. **Phase 6** - Metadata generation
8. **Phase 7** - Navigation integration (may require investigation)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing @Tabs usage | Low | High | Keep legacy pattern support; new pattern is additive |
| TabNode rendering issues | Low | Medium | Existing hierarchical renderer supports TabNode |
| Screen registry conflicts | Medium | Medium | Test with existing demo app patterns |
| Complex nested validation | Medium | Low | Start with basic validation, iterate |

---

## Testing Strategy

### Unit Tests
- `TabExtractor` correctly identifies FLAT_SCREEN vs NESTED_STACK
- `StackExtractor` handles KClass-based startDestination
- Generated code compiles correctly

### Integration Tests  
- Full MainTabs definition compiles and runs
- Navigation to MainTabs works
- Tab switching preserves state
- Flat tabs render correctly
- Nested stack tabs allow push/pop

### Demo App
- Update composeApp to use new pattern
- Verify all 4 tabs work as expected
- Test deep linking to tabs

---

## Files Summary

| Phase | File | Action |
|-------|------|--------|
| 1 | `quo-vadis-annotations/.../Stack.kt` | Modify |
| 2 | `quo-vadis-ksp/.../models/TabInfo.kt` | Modify |
| 2 | `quo-vadis-ksp/.../models/StackInfo.kt` | Modify |
| 3 | `quo-vadis-ksp/.../extractors/TabExtractor.kt` | Modify |
| 3 | `quo-vadis-ksp/.../extractors/StackExtractor.kt` | Modify |
| 4 | `quo-vadis-ksp/.../generators/NavNodeBuilderGenerator.kt` | Modify |
| 5 | `quo-vadis-ksp/.../generators/ScreenRegistryGenerator.kt` | Verify |
| 6 | `quo-vadis-ksp/.../generators/NavNodeBuilderGenerator.kt` | Modify (same as Phase 4) |
| 7 | `quo-vadis-ksp/.../generators/DeepLinkHandlerGenerator.kt` | Investigate |
| 7 | `quo-vadis-core/.../TreeNavigator.kt` | Investigate |
| 8 | `quo-vadis-ksp/.../validation/ValidationEngine.kt` | Modify |

---

## Success Criteria

- [ ] MainTabs.kt definition compiles without errors
- [ ] KSP generates correct NavNode builder with mixed tab types
- [ ] Flat screen tabs render their destination composables
- [ ] Nested stack tabs allow internal navigation
- [ ] Tab switching works correctly
- [ ] TabMetadata is available in TabWrapperScope
- [ ] Navigation to MainTabs works from other screens
- [ ] Deep linking to main/tabs works
- [ ] All existing demos continue to work

---

## Notes

- This plan focuses on KSP generation; runtime behavior relies on existing hierarchical rendering
- Phase 7 may require additional investigation of TreeNavigator internals
- Consider creating a migration guide for users of the old @Tabs pattern
