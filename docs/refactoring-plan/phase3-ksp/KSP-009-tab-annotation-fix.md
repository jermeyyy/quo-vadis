# KSP-009: Tab Annotation Pattern Fix for KMP Metadata Compilation

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-009 |
| **Task Name** | Tab Annotation Pattern Fix for KMP Metadata Compilation |
| **Phase** | Phase 3: KSP Processor Rewrite |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | KSP-001, KSP-002 |
| **Blocked By** | None |
| **Blocks** | MIG-007B (Tab System Migration) |

---

## Executive Summary

This specification addresses a critical KSP limitation in Kotlin Multiplatform where `getSymbolsWithAnnotation()` returns empty results for annotations applied to **nested sealed subclasses**. The fix introduces a new annotation pattern where `@TabItem` annotates **top-level classes** (typically `@Stack` classes) rather than nested subclasses, providing both a reliable solution for KSP and improved architecture with type-safe class references.

---

## Problem Statement

### Current Pattern (Problematic)

```kotlin
@Tab(name = "mainTabs", initialTab = "Home")  // initialTab is a String
sealed class MainTabs : Destination {

    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()  // ← Nested class with @TabItem

    @TabItem(label = "Explore", icon = "explore", rootGraph = ExploreDestination::class)
    @Destination(route = "tabs/explore")
    data object Explore : MainTabs()  // ← Nested class with @TabItem
}
```

### Issues

1. **KSP KMP Bug**: In Kotlin Multiplatform metadata compilation (`kspCommonMainKotlinMetadata`), `resolver.getSymbolsWithAnnotation("...TabItem")` returns **0 results** when `@TabItem` is applied to nested sealed subclasses.

2. **Unreliable `getSealedSubclasses()`**: The alternative approach via `classDeclaration.getSealedSubclasses()` also fails in certain KSP configurations.

3. **Workaround Fragility**: The current `TabExtractor` has a cache-based workaround that attempts to match `@TabItem` classes by `parentDeclaration`, but this fails when KSP doesn't see nested class annotations at all.

4. **Type-Unsafe Initial Tab**: The `initialTab` parameter uses `String` matching against subclass names, which is error-prone and lacks compile-time verification.

5. **Redundant Indirection**: Each `@TabItem` must reference a `rootGraph: KClass<*>` to a **separate** `@Stack` class, creating a confusing pattern where the tab identity (nested class) is separate from the tab content (external stack).

### Root Cause Analysis

The KSP limitation appears to be related to how Kotlin Multiplatform handles metadata compilation across multiple platforms. When processing `commonMain` sources:

1. KSP processes symbols in isolation per source set
2. Nested class annotations may not be visible in the symbol table during metadata compilation
3. The `@TabItem` annotation on `data object Home : MainTabs()` is not returned by `getSymbolsWithAnnotation()` even though the annotation exists in source

---

## Solution: New Annotation Pattern

### Design Principles

1. **Tab Items as Top-Level Classes**: `@TabItem` annotates standalone classes (not nested), making them reliably discoverable by KSP
2. **A Tab IS a Stack**: A `@TabItem` class is also a `@Stack` class, eliminating the redundant `rootGraph` reference
3. **Type-Safe Initial Tab**: `@Tab.initialTab` uses `KClass<*>` instead of `String` for compile-time safety
4. **Container References Content**: `@Tab` uses `items` parameter to reference `@TabItem` classes
5. **Backward Compatibility**: The old pattern can coexist during migration (deprecated)

### New Annotation Signatures

```kotlin
// === @Tab Container ===
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    /**
     * Unique identifier for this tab container.
     */
    val name: String,
    
    /**
     * Type-safe reference to the initially selected tab.
     * Must be one of the classes in [items].
     */
    val initialTab: KClass<*>,
    
    /**
     * The @TabItem/@Stack classes that comprise this tab container.
     * Order determines tab order in the UI.
     */
    val items: Array<KClass<*>>
)

// === @TabItem Metadata ===
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem(
    /**
     * Display label for the tab.
     */
    val label: String,
    
    /**
     * Icon identifier (platform-specific).
     */
    val icon: String = ""
)
```

### New Usage Pattern

```kotlin
// ═══════════════════════════════════════════════════════════════════════════
// STEP 1: Define tab content as @TabItem + @Stack classes (TOP-LEVEL)
// ═══════════════════════════════════════════════════════════════════════════

@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = "Feed")
sealed class HomeTab : Destination {
    
    @Destination(route = "home/feed")
    data object Feed : HomeTab()
    
    @Destination(route = "home/article/{articleId}")
    data class Article(val articleId: String) : HomeTab()
}

@TabItem(label = "Explore", icon = "explore")
@Stack(name = "exploreStack", startDestination = "ExploreRoot")
sealed class ExploreTab : Destination {
    
    @Destination(route = "explore/root")
    data object ExploreRoot : ExploreTab()
    
    @Destination(route = "explore/category/{categoryId}")
    data class Category(val categoryId: String) : ExploreTab()
}

@TabItem(label = "Profile", icon = "person")
@Stack(name = "profileStack", startDestination = "ProfileMain")
sealed class ProfileTab : Destination {
    
    @Destination(route = "profile/main")
    data object ProfileMain : ProfileTab()
    
    @Destination(route = "profile/settings")
    data object Settings : ProfileTab()
}

// ═══════════════════════════════════════════════════════════════════════════
// STEP 2: Define the tab container referencing the @TabItem classes
// ═══════════════════════════════════════════════════════════════════════════

@Tab(
    name = "mainTabs",
    initialTab = HomeTab::class,  // ← Type-safe class reference
    items = [
        HomeTab::class,
        ExploreTab::class,
        ProfileTab::class
    ]
)
object MainTabs  // Simple marker object, no sealed hierarchy needed
```

### Benefits of New Pattern

| Aspect | Old Pattern | New Pattern |
|--------|-------------|-------------|
| **KSP Compatibility** | Fragile (nested annotation issue) | Reliable (top-level classes) |
| **Initial Tab** | String-based (`"Home"`) | Type-safe (`HomeTab::class`) |
| **Tab-Stack Relationship** | Indirect (`@TabItem.rootGraph`) | Direct (same class) |
| **Sealed Subclass Requirement** | Required (`sealed class MainTabs`) | Optional (`object MainTabs`) |
| **Code Locality** | Tab metadata separate from content | Tab metadata with content |
| **IDE Navigation** | Manual lookup | Ctrl+Click on class references |

---

## Implementation Plan

### Phase 1: Annotation Changes (ANN-003-v2)

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

#### 1.1 Update `@Tab` Annotation

```kotlin
/**
 * Marks an object or class as a tabbed navigation container.
 *
 * This annotation defines a collection of tabs, each represented by a
 * @TabItem-annotated @Stack class. The container manages tab state,
 * including the active tab and per-tab back stacks.
 *
 * ## New Pattern (Recommended)
 *
 * Define each tab as a separate @TabItem + @Stack class:
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = "Feed")
 * sealed class HomeTab : Destination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 * }
 *
 * @Tab(
 *     name = "mainTabs",
 *     initialTab = HomeTab::class,
 *     items = [HomeTab::class, ExploreTab::class]
 * )
 * object MainTabs
 * ```
 *
 * ## Legacy Pattern (Deprecated)
 *
 * The old pattern with nested sealed subclasses is deprecated due to
 * KSP limitations in KMP metadata compilation:
 * ```kotlin
 * // DEPRECATED - Do not use
 * @Tab(name = "mainTabs", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *     @TabItem(...) data object Home : MainTabs()
 * }
 * ```
 *
 * @property name Unique identifier for this tab container
 * @property initialTab Class reference to the initially selected tab (must be in [items])
 * @property items Array of @TabItem/@Stack classes in tab order
 *
 * @see TabItem
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    val name: String,
    val initialTab: KClass<*> = Unit::class,  // Unit::class = use first item
    val items: Array<KClass<*>> = []
)
```

#### 1.2 Update `@TabItem` Annotation

```kotlin
/**
 * Provides UI metadata for a tab within a navigation container.
 *
 * Apply this annotation to a @Stack-annotated class to make it available
 * as a tab. The annotated class serves dual purposes:
 * 1. Defines the tab's UI properties (label, icon)
 * 2. Defines the tab's navigation content (as a @Stack)
 *
 * ## Usage
 *
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = "Feed")
 * sealed class HomeTab : Destination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeTab()
 * }
 * ```
 *
 * ## Note on rootGraph Parameter
 *
 * The [rootGraph] parameter is **deprecated** and ignored in the new pattern.
 * When a class has both @TabItem and @Stack, the class itself IS the root graph.
 *
 * @property label Display label for the tab (localized)
 * @property icon Icon identifier (platform-specific)
 * @property rootGraph **Deprecated**: Ignored when used with @Stack on same class
 *
 * @see Tab
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem(
    val label: String,
    val icon: String = "",
    @Deprecated("Use @TabItem + @Stack on same class instead")
    val rootGraph: KClass<*> = Unit::class  // Unit::class = same class is the stack
)
```

### Phase 2: KSP Processor Changes

#### 2.1 New TabExtractor Strategy

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`

```kotlin
/**
 * Extracts @Tab and @TabItem annotations into TabInfo models.
 *
 * Supports two patterns:
 * 1. **New Pattern**: @Tab with explicit `items` array referencing @TabItem/@Stack classes
 * 2. **Legacy Pattern**: @Tab on sealed class with @TabItem on nested subclasses (deprecated)
 *
 * The new pattern is reliable in KMP metadata compilation because it uses
 * top-level class references instead of nested class annotation discovery.
 */
class TabExtractor(
    private val stackExtractor: StackExtractor,
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {
    
    fun extract(classDeclaration: KSClassDeclaration, resolver: Resolver): TabInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Tab"
        } ?: return null
        
        val name = annotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String ?: return null
        
        // New pattern: explicit items array
        val itemsArg = annotation.arguments.find {
            it.name?.asString() == "items"
        }?.value
        
        val tabs = if (itemsArg != null && itemsArg is List<*> && itemsArg.isNotEmpty()) {
            // New pattern: resolve @TabItem/@Stack classes from items array
            extractFromItemsArray(itemsArg, resolver)
        } else {
            // Legacy pattern: try sealed subclasses (may fail in KMP)
            extractFromSealedSubclasses(classDeclaration, resolver)
        }
        
        if (tabs.isEmpty()) {
            logger.error("@Tab '${name}' has no valid tab items", classDeclaration)
            return null
        }
        
        // Resolve initialTab
        val initialTabArg = annotation.arguments.find {
            it.name?.asString() == "initialTab"
        }?.value as? KSType
        
        val initialTabIndex = if (initialTabArg != null) {
            val initialTabClassName = initialTabArg.declaration.qualifiedName?.asString()
            tabs.indexOfFirst { it.stackInfo?.className == initialTabClassName?.substringAfterLast('.') }
                .takeIf { it >= 0 } ?: 0
        } else {
            0  // Default to first tab
        }
        
        return TabInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            initialTabIndex = initialTabIndex,
            tabs = tabs
        )
    }
    
    /**
     * Extract tabs from explicit items array (new pattern).
     */
    private fun extractFromItemsArray(items: List<*>, resolver: Resolver): List<TabItemInfo> {
        return items.mapNotNull { item ->
            val ksType = item as? KSType ?: return@mapNotNull null
            val classDecl = ksType.declaration as? KSClassDeclaration ?: return@mapNotNull null
            
            // Must have @TabItem annotation
            val tabItemAnnotation = classDecl.annotations.find {
                it.shortName.asString() == "TabItem"
            } ?: run {
                logger.error("Class ${classDecl.simpleName.asString()} in @Tab.items must have @TabItem", classDecl)
                return@mapNotNull null
            }
            
            // Extract @TabItem metadata
            val label = tabItemAnnotation.arguments.find { it.name?.asString() == "label" }?.value as? String ?: ""
            val icon = tabItemAnnotation.arguments.find { it.name?.asString() == "icon" }?.value as? String ?: ""
            
            // Must also have @Stack (the class IS the stack)
            val stackInfo = stackExtractor.extract(classDecl)
            if (stackInfo == null) {
                logger.error("@TabItem class ${classDecl.simpleName.asString()} must also have @Stack", classDecl)
                return@mapNotNull null
            }
            
            TabItemInfo(
                label = label,
                icon = icon,
                stackInfo = stackInfo,
                classDeclaration = classDecl
            )
        }
    }
    
    /**
     * Extract tabs from sealed subclasses (legacy pattern).
     * May fail in KMP metadata compilation.
     */
    private fun extractFromSealedSubclasses(
        classDeclaration: KSClassDeclaration,
        resolver: Resolver
    ): List<TabItemInfo> {
        logger.warn("Using legacy @Tab pattern with sealed subclasses - may fail in KMP", classDeclaration)
        // ... existing implementation ...
    }
}
```

#### 2.2 Updated TabInfo Model

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`

```kotlin
/**
 * Extracted information from @Tab annotation.
 *
 * @property classDeclaration The annotated class (container)
 * @property name Tab container identifier
 * @property className Simple class name
 * @property packageName Package name
 * @property initialTabIndex Index of initially selected tab (0-based)
 * @property tabs List of tab items with their stack info
 */
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTabIndex: Int,
    val tabs: List<TabItemInfo>
)

/**
 * Extracted information from @TabItem annotation.
 *
 * In the new pattern, each TabItem IS a Stack.
 *
 * @property label Display label for the tab
 * @property icon Icon identifier
 * @property stackInfo Stack configuration for this tab
 * @property classDeclaration The @TabItem/@Stack class
 */
data class TabItemInfo(
    val label: String,
    val icon: String,
    val stackInfo: StackInfo?,
    val classDeclaration: KSClassDeclaration
)
```

#### 2.3 Updated NavNodeBuilderGenerator

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt`

```kotlin
/**
 * Generate TabNode builder from @Tab annotation.
 */
fun generateTabBuilder(tabInfo: TabInfo) {
    val fileName = "${tabInfo.className}NavNode"
    
    val file = FileSpec.builder(tabInfo.packageName, fileName)
        .addFunction(
            FunSpec.builder("build${tabInfo.className}NavNode")
                .returns(QuoVadisClassNames.TAB_NODE)
                .addKdoc("Builds the TabNode tree for ${tabInfo.className}.\n\n@return TabNode with ${tabInfo.tabs.size} tabs")
                .addCode(buildString {
                    appendLine("return TabNode(")
                    appendLine("    key = %S,", tabInfo.name)
                    appendLine("    parentKey = null,")
                    appendLine("    stacks = listOf(")
                    tabInfo.tabs.forEachIndexed { index, tab ->
                        val stackBuilder = tab.stackInfo?.let { "build${it.className}NavNode()" } ?: "TODO()"
                        appendLine("        ${stackBuilder}.copy(parentKey = %S),", tabInfo.name)
                    }
                    appendLine("    ),")
                    appendLine("    activeStackIndex = ${tabInfo.initialTabIndex},")
                    appendLine("    tabMetadata = listOf(")
                    tabInfo.tabs.forEach { tab ->
                        appendLine("        TabMetadata(label = %S, icon = %S),", tab.label, tab.icon)
                    }
                    appendLine("    )")
                    appendLine(")")
                })
                .build()
        )
        .build()
    
    file.writeTo(codeGenerator, Dependencies(aggregating = true, tabInfo.classDeclaration.containingFile!!))
}
```

### Phase 3: Validation Updates

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

Add validation rules:

```kotlin
// Rule: @Tab.items classes must have @TabItem
fun validateTabItems(tabs: List<TabInfo>): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()
    
    for (tab in tabs) {
        // Check initialTab is in items
        if (tab.initialTabIndex < 0 || tab.initialTabIndex >= tab.tabs.size) {
            errors.add(ValidationError(
                "@Tab '${tab.name}': initialTab class not found in items array",
                tab.classDeclaration
            ))
        }
        
        // Check each item has @TabItem + @Stack
        for (item in tab.tabs) {
            if (item.stackInfo == null) {
                errors.add(ValidationError(
                    "@TabItem '${item.label}' must also be annotated with @Stack",
                    item.classDeclaration
                ))
            }
        }
    }
    
    return errors
}

// Rule: Warn about legacy pattern
fun validateLegacyPattern(tabs: List<TabInfo>): List<ValidationWarning> {
    return tabs.filter { it.isLegacyPattern }.map { tab ->
        ValidationWarning(
            "@Tab '${tab.name}' uses legacy nested subclass pattern which may fail in KMP. " +
            "Migrate to new pattern with explicit items array.",
            tab.classDeclaration
        )
    }
}
```

---

## Migration Guide

### From Old Pattern to New Pattern

#### Before (Legacy Pattern)

```kotlin
// Old: @TabItem on nested subclasses with external rootGraph
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {

    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()

    @TabItem(label = "Explore", icon = "explore", rootGraph = ExploreDestination::class)
    @Destination(route = "tabs/explore")
    data object Explore : MainTabs()
}

// Separate stack definitions
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
}

@Stack(name = "explore", startDestination = "ExploreRoot")
sealed class ExploreDestination : Destination {
    @Destination(route = "explore/root")
    data object ExploreRoot : ExploreDestination()
}
```

#### After (New Pattern)

```kotlin
// New: @TabItem + @Stack on same top-level class
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = "Feed")
sealed class HomeTab : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeTab()
}

@TabItem(label = "Explore", icon = "explore")
@Stack(name = "exploreStack", startDestination = "ExploreRoot")
sealed class ExploreTab : Destination {
    @Destination(route = "explore/root")
    data object ExploreRoot : ExploreTab()
}

// Container with explicit items
@Tab(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class]
)
object MainTabs
```

### Migration Steps

1. **Rename stack classes** to indicate they are tabs:
   - `HomeDestination` → `HomeTab`
   - `ExploreDestination` → `ExploreTab`

2. **Add `@TabItem` to stack classes**:
   ```kotlin
   @TabItem(label = "Home", icon = "home")
   @Stack(name = "homeStack", startDestination = "Feed")
   sealed class HomeTab : Destination { ... }
   ```

3. **Simplify `@Tab` container**:
   ```kotlin
   @Tab(
       name = "mainTabs",
       initialTab = HomeTab::class,
       items = [HomeTab::class, ExploreTab::class]
   )
   object MainTabs  // No sealed hierarchy needed
   ```

4. **Update navigator references**:
   ```kotlin
   // Old
   navigator.switchTab(MainTabs.Home)
   
   // New
   navigator.switchTab(0)  // By index
   // or generate extension:
   navigator.switchToHomeTab()
   ```

5. **Remove obsolete sealed subclasses**:
   - Delete `data object Home : MainTabs()`
   - Delete `data object Explore : MainTabs()`

---

## Test Cases

### Unit Tests

```kotlin
class TabExtractorTest {
    
    @Test
    fun `new pattern - extracts tabs from items array`() {
        // Given: @Tab with items = [HomeTab::class, ExploreTab::class]
        // When: TabExtractor.extract() is called
        // Then: TabInfo has 2 tabs with correct StackInfo
    }
    
    @Test
    fun `new pattern - resolves initialTab from class reference`() {
        // Given: @Tab(initialTab = ExploreTab::class, items = [HomeTab::class, ExploreTab::class])
        // When: TabExtractor.extract() is called
        // Then: TabInfo.initialTabIndex == 1
    }
    
    @Test
    fun `new pattern - validates TabItem must have Stack`() {
        // Given: @Tab(items = [NonStackClass::class])
        // When: ValidationEngine.validate() is called
        // Then: Error reported
    }
    
    @Test
    fun `legacy pattern - warns about KMP compatibility`() {
        // Given: @Tab on sealed class with nested @TabItem subclasses
        // When: TabExtractor.extract() is called
        // Then: Warning logged about KMP compatibility
    }
}
```

### Integration Tests

```kotlin
class TabKspIntegrationTest {
    
    @Test
    fun `generates TabNode builder from new pattern`() {
        // Given: Files with @Tab + @TabItem/@Stack pattern
        // When: KSP processes the files
        // Then: build{Name}NavNode() function is generated correctly
    }
    
    @Test
    fun `KSP finds TabItem annotations on top-level classes`() {
        // Given: @TabItem on top-level sealed class
        // When: resolver.getSymbolsWithAnnotation("...TabItem")
        // Then: Returns the annotated class
    }
}
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `TabAnnotations.kt` | **Modify** | Add `items` to `@Tab`, deprecate `rootGraph` in `@TabItem` |
| `TabExtractor.kt` | **Modify** | Add new pattern extraction, keep legacy fallback |
| `TabInfo.kt` | **Modify** | Update model for new pattern |
| `NavNodeBuilderGenerator.kt` | **Modify** | Generate from new TabInfo structure |
| `ValidationEngine.kt` | **Modify** | Add validation rules for new pattern |
| `QuoVadisSymbolProcessor.kt` | **Modify** | Pass resolver to TabExtractor |

---

## Acceptance Criteria

- [ ] `@Tab` annotation updated with `initialTab: KClass<*>` and `items: Array<KClass<*>>`
- [ ] `@TabItem.rootGraph` deprecated (still compiles for backward compat)
- [ ] `TabExtractor` supports new pattern (items array)
- [ ] `TabExtractor` falls back to legacy pattern with warning
- [ ] `ValidationEngine` validates new pattern constraints
- [ ] `NavNodeBuilderGenerator` generates correct code for new pattern
- [ ] Unit tests for new extraction logic
- [ ] Integration test verifies KSP finds top-level `@TabItem` annotations in KMP
- [ ] Demo app migrated to new pattern compiles successfully
- [ ] Documentation updated with migration guide

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing code | Low | High | Old pattern still supported (deprecated) |
| KSP version differences | Medium | Medium | Test across KSP versions |
| Generated code changes | Medium | Medium | Comprehensive tests before migration |
| IDE support for KClass array | Low | Low | Standard Kotlin feature |

---

## References

- [KSP Issue: Sealed subclass annotation discovery](https://github.com/google/ksp/issues/1150) - Related KSP limitation
- [ANN-003](../phase4-annotations/ANN-003-route-transitions.md) - Original @Tab/@TabItem spec
- [MIG-007B](../phase5-migration/MIG-007B-tab-system.md) - Tab system migration (blocked by this)
- [TabExtractor.kt](../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt) - Current implementation
- [TabAnnotations.kt](../../quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt) - Current annotations

````
