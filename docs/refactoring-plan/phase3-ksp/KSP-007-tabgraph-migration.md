# Task KSP-007: Update TabGraph Generator to NavNode Output

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | KSP-007 |
| **Name** | Migrate TabGraph Generator to NavNode Output |
| **Phase** | 3 - KSP Processor Updates |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | KSP-004 (NavNode Builder Generator) |

## Overview

Update the existing `TabGraphGenerator` to output `TabNode` structures instead of the legacy `TabNavigatorConfig` objects. This aligns the tabbed navigation code generation with the new tree-based architecture while preserving existing semantics like `initialTab` and `primaryTab`.

## Current Implementation

The existing generator produces:

```kotlin
// Currently generated
data class MainTabsConfig(
    val tabs: List<TabDefinition>,
    val initialTab: Int,
    val primaryTab: Int?
)

fun buildMainTabsConfig(): MainTabsConfig = MainTabsConfig(
    tabs = listOf(
        TabDefinition("home", HomeDestination, HomeIcon),
        TabDefinition("profile", ProfileDestination, ProfileIcon)
    ),
    initialTab = 0,
    primaryTab = null
)
```

## Target Implementation

Generate `TabNode` structure:

```kotlin
// New output
fun buildMainTabsNavNode(parentKey: String? = null): TabNode = TabNode(
    key = "tab_MainTabs",
    parentKey = parentKey,
    stacks = listOf(
        StackNode(
            key = "stack_home",
            parentKey = "tab_MainTabs",
            children = listOf(
                ScreenNode(
                    key = "screen_Home",
                    parentKey = "stack_home",
                    destination = HomeDestination
                )
            )
        ),
        StackNode(
            key = "stack_profile", 
            parentKey = "tab_MainTabs",
            children = listOf(
                ScreenNode(
                    key = "screen_Profile",
                    parentKey = "stack_profile",
                    destination = ProfileDestination
                )
            )
        )
    ),
    activeStackIndex = 0  // Maps from initialTab
)

// Backward compatibility wrapper (deprecated)
@Deprecated(
    message = "Use buildMainTabsNavNode() instead",
    replaceWith = ReplaceWith("buildMainTabsNavNode()")
)
fun buildMainTabsConfig(): TabNavigatorConfig = /* legacy implementation */
```

## Implementation

### Step 1: Update TabGraphExtractor

```kotlin
// quo-vadis-ksp/src/main/kotlin/.../TabGraphExtractor.kt

data class TabGraphInfo(
    val name: String,
    val packageName: String,
    val tabs: List<TabInfo>,
    val initialTabIndex: Int,
    val primaryTabIndex: Int?,
    val generateLegacyConfig: Boolean = true  // For backward compat
)

data class TabInfo(
    val name: String,
    val route: RouteInfo,
    val icon: IconInfo?,
    val label: String?,
    val nestedGraph: GraphInfo?  // For tabs containing sub-graphs
)

class TabGraphExtractor(private val logger: KSPLogger) {
    
    fun extract(graphClass: KSClassDeclaration): TabGraphInfo {
        val annotation = graphClass.annotations
            .find { it.shortName.asString() == "TabGraph" }
            ?: throw IllegalArgumentException("Not a TabGraph")
        
        val tabs = graphClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.annotations.any { a -> a.shortName.asString() == "Tab" } }
            .mapIndexed { index, tabClass -> extractTabInfo(tabClass, index) }
            .toList()
        
        val initialTabIndex = annotation.arguments
            .find { it.name?.asString() == "initialTab" }
            ?.value as? Int ?: 0
            
        val primaryTabIndex = annotation.arguments
            .find { it.name?.asString() == "primaryTab" }
            ?.value as? Int
        
        return TabGraphInfo(
            name = graphClass.simpleName.asString(),
            packageName = graphClass.packageName.asString(),
            tabs = tabs,
            initialTabIndex = initialTabIndex,
            primaryTabIndex = primaryTabIndex
        )
    }
}
```

### Step 2: Update TabGraphGenerator for NavNode Output

```kotlin
// quo-vadis-ksp/src/main/kotlin/.../TabGraphGenerator.kt

class TabGraphGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    
    fun generate(tabGraphInfo: TabGraphInfo) {
        val fileSpec = FileSpec.builder(
            tabGraphInfo.packageName,
            "${tabGraphInfo.name}NavNodeBuilder"
        ).apply {
            addImport("com.jermey.quo.vadis.core.navigation.core", "TabNode")
            addImport("com.jermey.quo.vadis.core.navigation.core", "StackNode")
            addImport("com.jermey.quo.vadis.core.navigation.core", "ScreenNode")
            
            // New NavNode builder
            addFunction(generateNavNodeBuilder(tabGraphInfo))
            
            // Legacy compatibility (if enabled)
            if (tabGraphInfo.generateLegacyConfig) {
                addFunction(generateLegacyConfigBuilder(tabGraphInfo))
            }
            
            // Tab metadata for UI (icons, labels)
            addProperty(generateTabMetadata(tabGraphInfo))
        }.build()
        
        fileSpec.writeTo(codeGenerator, aggregating = false)
    }
    
    private fun generateNavNodeBuilder(info: TabGraphInfo): FunSpec {
        return FunSpec.builder("build${info.name}NavNode")
            .addKdoc("Builds the TabNode structure for ${info.name}.\n")
            .addKdoc("@param parentKey Optional parent node key\n")
            .addKdoc("@return TabNode with initialized stacks for each tab\n")
            .addParameter(
                ParameterSpec.builder("parentKey", String::class.asTypeName().copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .returns(TAB_NODE)
            .apply {
                addStatement("val tabKey = %S", "tab_${info.name}")
                addStatement("")
                addStatement("return %T(", TAB_NODE)
                addStatement("    key = tabKey,")
                addStatement("    parentKey = parentKey,")
                addStatement("    stacks = listOf(")
                
                info.tabs.forEachIndexed { index, tab ->
                    val stackKey = "stack_${tab.name}"
                    addStatement("        %T(", STACK_NODE)
                    addStatement("            key = %S,", stackKey)
                    addStatement("            parentKey = tabKey,")
                    addStatement("            children = listOf(")
                    
                    if (tab.nestedGraph != null) {
                        // Nested graph - call its builder
                        addStatement(
                            "                build%LNavNode(parentKey = %S)",
                            tab.nestedGraph.name, stackKey
                        )
                    } else {
                        // Simple route
                        addStatement("                %T(", SCREEN_NODE)
                        addStatement("                    key = %S,", "screen_${tab.route.name}")
                        addStatement("                    parentKey = %S,", stackKey)
                        addStatement("                    destination = %T", tab.route.destinationClass)
                        addStatement("                )")
                    }
                    
                    addStatement("            )")
                    addStatement("        )%L", if (index < info.tabs.lastIndex) "," else "")
                }
                
                addStatement("    ),")
                addStatement("    activeStackIndex = %L", info.initialTabIndex)
                addStatement(")")
            }
            .build()
    }
    
    private fun generateTabMetadata(info: TabGraphInfo): PropertySpec {
        return PropertySpec.builder(
            "${info.name.replaceFirstChar { it.lowercase() }}TabMetadata",
            List::class.asClassName().parameterizedBy(TAB_METADATA)
        )
            .initializer(buildCodeBlock {
                addStatement("listOf(")
                info.tabs.forEachIndexed { index, tab ->
                    addStatement("    %T(", TAB_METADATA)
                    addStatement("        key = %S,", "stack_${tab.name}")
                    addStatement("        label = %S,", tab.label ?: tab.name)
                    tab.icon?.let { icon ->
                        addStatement("        icon = %T,", icon.className)
                    }
                    addStatement("        isPrimary = %L", index == info.primaryTabIndex)
                    addStatement("    )%L", if (index < info.tabs.lastIndex) "," else "")
                }
                addStatement(")")
            })
            .build()
    }
}
```

### Step 3: Preserve Primary Tab Semantics

The `primaryTab` concept (e.g., a "home" tab that back navigation returns to) maps to the new architecture:

```kotlin
// Add to generated code
val primaryTabStackKey: String? = ${
    if (info.primaryTabIndex != null) "\"stack_${info.tabs[info.primaryTabIndex].name}\""
    else "null"
}

// Extension for Navigator to support primary tab behavior
fun Navigator.navigateBackToTab(tabNode: TabNode): NavNode {
    val primaryIndex = tabNode.primaryTabIndex ?: 0
    return tabNode.copy(activeStackIndex = primaryIndex)
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../TabGraphExtractor.kt` | Modify |
| `quo-vadis-ksp/src/main/kotlin/.../TabGraphGenerator.kt` | Modify |
| `quo-vadis-ksp/src/main/kotlin/.../TabMetadata.kt` | New |

## Migration Path

1. **v1.x**: Both `buildXxxConfig()` and `buildXxxNavNode()` generated
2. **v2.0**: `buildXxxConfig()` marked `@Deprecated(WARNING)`
3. **v3.0**: `buildXxxConfig()` marked `@Deprecated(ERROR)`
4. **v4.0**: `buildXxxConfig()` removed

## Acceptance Criteria

- [ ] `buildXxxNavNode()` generates correct `TabNode` structure
- [ ] `initialTab` maps to `activeStackIndex`
- [ ] `primaryTab` semantics preserved in metadata
- [ ] Tab metadata (icons, labels) available separately
- [ ] Legacy `buildXxxConfig()` still works with deprecation
- [ ] Nested graphs within tabs handled correctly
- [ ] Unit tests cover all tab configurations

## Testing Notes

```kotlin
@Test
fun `tab graph generates TabNode with correct structure`() {
    val tabNode = buildMainTabsNavNode()
    
    assertThat(tabNode.stacks).hasSize(3)
    assertThat(tabNode.activeStackIndex).isEqualTo(0)
    assertThat(tabNode.stacks[0].key).isEqualTo("stack_home")
}

@Test
fun `primary tab is tracked in metadata`() {
    assertThat(mainTabsTabMetadata.find { it.isPrimary }?.key)
        .isEqualTo("stack_home")
}
```

## References

- [KSP-004: NavNode Builder Generator](./KSP-004-navnode-generator.md)
- [CORE-001: NavNode Hierarchy](../phase1-core/CORE-001-navnode-hierarchy.md)
- [Current TabGraphGenerator.kt](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/TabGraphGenerator.kt)
