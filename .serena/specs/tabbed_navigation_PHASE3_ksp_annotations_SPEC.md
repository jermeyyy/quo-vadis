# Phase 3: KSP Annotations & Code Generation

## Phase Overview

**Objective**: Simplify tabbed navigation setup through KSP (Kotlin Symbol Processing) code generation, reducing boilerplate by ~70%.

**Scope**:
- New annotations: `@TabGraph`, `@Tab`, `@TabContent`
- Enhanced KSP processor to generate tab scaffolding
- Code generation for tab configurations and builders
- Integration with existing `@Graph`, `@Route`, `@Content` system

**Timeline**: 2-3 days

**Dependencies**: 
- Phase 1 (Core Foundation)
- Phase 2 (Compose Integration)
- Existing KSP infrastructure

## Code Generation Goals

### Before (Manual Implementation)
```kotlin
// ~150 lines of boilerplate per tab container

sealed class MainTab : TabDefinition {
    override val label: String get() = "Home"
    override val icon: String get() = "home"
    override val rootDestination = HomeDestination.Root
    
    data object Home : MainTab() {
        override val id = "home"
    }
    // ... repeat for each tab
}

val tabConfig = TabNavigatorConfig(
    allTabs = listOf(MainTab.Home, MainTab.Profile, MainTab.Settings),
    initialTab = MainTab.Home
)

@Composable
fun MainTabContainer(parentNavigator: Navigator) {
    val tabState = rememberTabNavigator(tabConfig, parentNavigator)
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = mapOf(
            MainTab.Home to buildHomeGraph(),
            MainTab.Profile to buildProfileGraph(),
            MainTab.Settings to buildSettingsGraph()
        )
    )
}
```

### After (Annotation-Based)
```kotlin
// ~20 lines with generation

@TabGraph("main")
sealed class MainTab : TabDefinition {
    @Tab(
        route = "home",
        label = "Home",
        icon = "home",
        rootGraph = HomeDestination::class
    )
    data object Home : MainTab()
    
    @Tab(route = "profile", label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    data object Profile : MainTab()
    
    @Tab(route = "settings", label = "Settings", icon = "settings", rootGraph = SettingsDestination::class)
    data object Settings : MainTab()
}

// Generated: buildMainTabGraph(), MainTabContainer composable, MainTabConfig
```

**Reduction**: ~87% less code to write and maintain!

---

## Detailed Implementation Plan

### Step 1: New Annotations

**File**: `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Annotations.kt`

**Add to existing file**:

```kotlin
/**
 * Marks a sealed class as a tab graph container.
 * 
 * The sealed class should extend [TabDefinition] and contain tab objects/classes
 * annotated with [@Tab]. KSP will generate:
 * - Tab configuration (`{ClassName}Config`)
 * - Tab navigation container composable (`{ClassName}Container`)
 * - Tab graph builder function (`build{ClassName}Graph()`)
 * 
 * @param name Unique identifier for this tab container
 * @param initialTab The class name of the initial tab (defaults to first @Tab)
 * @param primaryTab The class name of the primary/home tab (defaults to initialTab)
 * 
 * @sample Basic tab container
 * ```kotlin
 * @TabGraph("main")
 * sealed class MainTab : TabDefinition {
 *     @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     data object Home : MainTab()
 *     
 *     @Tab(route = "profile", label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
 *     data object Profile : MainTab()
 * }
 * 
 * // Generated:
 * // - MainTabConfig: TabNavigatorConfig
 * // - buildMainTabGraph(): NavigationGraph
 * // - MainTabContainer(parentNavigator: Navigator)
 * ```
 * 
 * @sample With custom initial/primary tabs
 * ```kotlin
 * @TabGraph(
 *     name = "app",
 *     initialTab = "Dashboard",
 *     primaryTab = "Home"
 * )
 * sealed class AppTab : TabDefinition {
 *     @Tab(...) data object Home : AppTab()
 *     @Tab(...) data object Dashboard : AppTab()
 *     @Tab(...) data object Settings : AppTab()
 * }
 * ```
 * 
 * @see Tab
 * @see TabContent
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabGraph(
    val name: String,
    val initialTab: String = "",
    val primaryTab: String = ""
)

/**
 * Defines a tab within a [@TabGraph] container.
 * 
 * Must be used on objects/classes within a sealed class annotated with [@TabGraph].
 * Each tab must specify its route and the root graph it should display.
 * 
 * @param route Unique route identifier for this tab (used in deep links)
 * @param label Display label for the tab (shown in UI)
 * @param icon Icon identifier for the tab (can be icon name or resource)
 * @param rootGraph The [@Graph] sealed class that this tab displays
 * @param rootDestination Optional specific destination within rootGraph (defaults to startDestination)
 * 
 * @sample Simple tab
 * ```kotlin
 * @Tab(
 *     route = "home",
 *     label = "Home",
 *     icon = "home",
 *     rootGraph = HomeDestination::class
 * )
 * data object Home : MainTab()
 * ```
 * 
 * @sample Tab with specific root destination
 * ```kotlin
 * @Tab(
 *     route = "profile",
 *     label = "Profile",
 *     icon = "person",
 *     rootGraph = ProfileDestination::class,
 *     rootDestination = ProfileDestination.View::class
 * )
 * data object Profile : MainTab()
 * ```
 * 
 * @see TabGraph
 * @see Graph
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    val route: String,
    val label: String,
    val icon: String,
    val rootGraph: KClass<*>,
    val rootDestination: KClass<*> = Nothing::class
)

/**
 * Marks a Composable function as the custom content renderer for a specific tab.
 * 
 * Optional - if not provided, KSP generates default container using tab's rootGraph.
 * Use this when you need custom UI around the tab's navigation (e.g., floating action button).
 * 
 * Function signature must be:
 * ```
 * @Composable
 * fun {Name}Content(
 *     tab: {TabType},
 *     navigator: Navigator,
 *     modifier: Modifier = Modifier
 * )
 * ```
 * 
 * @param tabClass The tab object/class this content renders
 * 
 * @sample Custom tab content with FAB
 * ```kotlin
 * @TabContent(MainTab.Home::class)
 * @Composable
 * fun HomeTabContent(
 *     tab: MainTab.Home,
 *     navigator: Navigator,
 *     modifier: Modifier = Modifier
 * ) {
 *     Scaffold(
 *         floatingActionButton = { FloatingActionButton(...) },
 *         modifier = modifier
 *     ) { padding ->
 *         // Default graph rendering
 *         GraphNavHost(
 *             graph = buildHomeDestinationGraph(),
 *             navigator = navigator,
 *             modifier = Modifier.padding(padding)
 *         )
 *     }
 * }
 * ```
 * 
 * @see TabGraph
 * @see Tab
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class TabContent(val tabClass: KClass<*>)
```

**Key Design Decisions**:
- Consistent with existing `@Graph`, `@Route`, `@Content` pattern
- Type-safe with `KClass` references
- Flexible (custom initial/primary tabs)
- Optional `@TabContent` for advanced customization

---

### Step 2: KSP Data Models

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/TabGraphInfo.kt`

**Purpose**: Data models for tab graph processing

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Information extracted from a @TabGraph annotated sealed class.
 */
data class TabGraphInfo(
    val name: String,
    val className: String,
    val packageName: String,
    val tabs: List<TabInfo>,
    val initialTab: String,
    val primaryTab: String,
    val classDeclaration: KSClassDeclaration
) {
    /**
     * Generate the configuration object name.
     * Example: "MainTab" -> "MainTabConfig"
     */
    val configName: String
        get() = "${className}Config"
    
    /**
     * Generate the container composable name.
     * Example: "MainTab" -> "MainTabContainer"
     */
    val containerName: String
        get() = "${className}Container"
    
    /**
     * Generate the graph builder function name.
     * Example: "MainTab" -> "buildMainTabGraph"
     */
    val graphBuilderName: String
        get() = "build${className}Graph"
    
    /**
     * Resolve initial tab (from annotation or first tab).
     */
    val resolvedInitialTab: TabInfo
        get() = tabs.find { it.name == initialTab } ?: tabs.first()
    
    /**
     * Resolve primary tab (from annotation, initialTab, or first tab).
     */
    val resolvedPrimaryTab: TabInfo
        get() = tabs.find { it.name == primaryTab }
            ?: tabs.find { it.name == initialTab }
            ?: tabs.first()
}

/**
 * Information extracted from a @Tab annotated object/class.
 */
data class TabInfo(
    val name: String,
    val route: String,
    val label: String,
    val icon: String,
    val rootGraphClass: String,
    val rootGraphPackage: String,
    val rootDestinationClass: String?,
    val hasCustomContent: Boolean = false,
    val customContentFunction: String? = null,
    val classDeclaration: KSClassDeclaration
) {
    /**
     * Qualified name of the root graph.
     * Example: "com.example.HomeDestination"
     */
    val rootGraphQualifiedName: String
        get() = "$rootGraphPackage.$rootGraphClass"
    
    /**
     * Generate root graph builder function name.
     * Example: "HomeDestination" -> "buildHomeDestinationGraph"
     */
    val rootGraphBuilderName: String
        get() = "build${rootGraphClass}Graph"
}

/**
 * Information extracted from a @TabContent annotated function.
 */
data class TabContentInfo(
    val tabClassName: String,
    val functionName: String,
    val packageName: String
)
```

---

### Step 3: Tab Graph Extractor

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/TabGraphExtractor.kt`

**Purpose**: Extract tab graph information from annotations

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabContent
import com.jermey.quo.vadis.annotations.TabGraph

/**
 * Extracts [TabGraphInfo] from @TabGraph annotated sealed classes.
 */
class TabGraphExtractor(private val logger: KSPLogger) {
    
    fun extract(classDeclaration: KSClassDeclaration): TabGraphInfo? {
        // Find @TabGraph annotation
        val tabGraphAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "TabGraph"
        } ?: return null
        
        // Extract annotation parameters
        val name = tabGraphAnnotation.arguments.find { it.name?.asString() == "name" }
            ?.value as? String
            ?: run {
                logger.error("@TabGraph must specify 'name' parameter", classDeclaration)
                return null
            }
        
        val initialTab = tabGraphAnnotation.arguments.find { it.name?.asString() == "initialTab" }
            ?.value as? String ?: ""
        
        val primaryTab = tabGraphAnnotation.arguments.find { it.name?.asString() == "primaryTab" }
            ?.value as? String ?: ""
        
        // Verify it's a sealed class
        if (classDeclaration.modifiers.contains(Modifier.SEALED).not()) {
            logger.error("@TabGraph must be on a sealed class", classDeclaration)
            return null
        }
        
        // Extract all @Tab annotated subclasses
        val tabs = extractTabs(classDeclaration)
        if (tabs.isEmpty()) {
            logger.error("@TabGraph sealed class must have at least one @Tab subclass", classDeclaration)
            return null
        }
        
        return TabGraphInfo(
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            tabs = tabs,
            initialTab = initialTab,
            primaryTab = primaryTab,
            classDeclaration = classDeclaration
        )
    }
    
    private fun extractTabs(sealedClass: KSClassDeclaration): List<TabInfo> {
        return sealedClass.getSealedSubclasses()
            .mapNotNull { subclass -> extractTab(subclass) }
            .toList()
    }
    
    private fun extractTab(classDeclaration: KSClassDeclaration): TabInfo? {
        // Find @Tab annotation
        val tabAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Tab"
        } ?: return null
        
        // Extract required parameters
        val route = tabAnnotation.arguments.find { it.name?.asString() == "route" }
            ?.value as? String
            ?: run {
                logger.error("@Tab must specify 'route' parameter", classDeclaration)
                return null
            }
        
        val label = tabAnnotation.arguments.find { it.name?.asString() == "label" }
            ?.value as? String
            ?: run {
                logger.error("@Tab must specify 'label' parameter", classDeclaration)
                return null
            }
        
        val icon = tabAnnotation.arguments.find { it.name?.asString() == "icon" }
            ?.value as? String
            ?: run {
                logger.error("@Tab must specify 'icon' parameter", classDeclaration)
                return null
            }
        
        // Extract rootGraph KClass
        val rootGraphType = tabAnnotation.arguments.find { it.name?.asString() == "rootGraph" }
            ?.value as? KSType
            ?: run {
                logger.error("@Tab must specify 'rootGraph' parameter", classDeclaration)
                return null
            }
        
        val rootGraphDeclaration = rootGraphType.declaration as? KSClassDeclaration
            ?: run {
                logger.error("@Tab rootGraph must be a class", classDeclaration)
                return null
            }
        
        // Extract optional rootDestination
        val rootDestinationType = tabAnnotation.arguments.find { it.name?.asString() == "rootDestination" }
            ?.value as? KSType
        val rootDestinationClass = (rootDestinationType?.declaration as? KSClassDeclaration)
            ?.simpleName?.asString()
            ?.takeIf { it != "Nothing" }
        
        return TabInfo(
            name = classDeclaration.simpleName.asString(),
            route = route,
            label = label,
            icon = icon,
            rootGraphClass = rootGraphDeclaration.simpleName.asString(),
            rootGraphPackage = rootGraphDeclaration.packageName.asString(),
            rootDestinationClass = rootDestinationClass,
            classDeclaration = classDeclaration
        )
    }
    
    /**
     * Find all @TabContent functions for a specific tab graph.
     */
    fun findTabContent(
        resolver: Resolver,
        tabGraphInfo: TabGraphInfo
    ): Map<String, TabContentInfo> {
        val contentMap = mutableMapOf<String, TabContentInfo>()
        
        resolver.getSymbolsWithAnnotation(TabContent::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { function ->
                val annotation = function.annotations.find {
                    it.shortName.asString() == "TabContent"
                }
                
                val tabClass = annotation?.arguments
                    ?.find { it.name?.asString() == "tabClass" }
                    ?.value as? KSType
                
                val tabClassName = (tabClass?.declaration as? KSClassDeclaration)
                    ?.simpleName?.asString()
                
                if (tabClassName != null) {
                    contentMap[tabClassName] = TabContentInfo(
                        tabClassName = tabClassName,
                        functionName = function.simpleName.asString(),
                        packageName = function.packageName.asString()
                    )
                }
            }
        
        return contentMap
    }
}
```

---

### Step 4: Tab Graph Code Generator

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/TabGraphGenerator.kt`

**Purpose**: Generate tab configuration, container, and graph builder

```kotlin
package com.jermey.quo.vadis.ksp

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.OutputStream

/**
 * Generates code for @TabGraph annotated sealed classes.
 * 
 * Generates:
 * 1. Tab configuration object
 * 2. Tab container composable
 * 3. Tab graph builder function
 */
class TabGraphGenerator {
    
    fun generate(
        tabGraphInfo: TabGraphInfo,
        tabContentMap: Map<String, TabContentInfo>,
        outputStream: OutputStream
    ) {
        val fileSpec = FileSpec.builder(
            packageName = tabGraphInfo.packageName,
            fileName = "${tabGraphInfo.className}Generated"
        )
            .addComment("Generated by Quo Vadis KSP. Do not edit manually.")
            .addImport("com.jermey.quo.vadis.core.navigation.core", "TabNavigatorConfig")
            .addImport("com.jermey.quo.vadis.core.navigation.core", "Navigator")
            .addImport("com.jermey.quo.vadis.core.navigation.compose", "TabbedNavHost")
            .addImport("com.jermey.quo.vadis.core.navigation.compose", "rememberTabNavigator")
            .addImport("androidx.compose.runtime", "Composable")
            .addImport("androidx.compose.ui", "Modifier")
            .apply {
                // 1. Generate config object
                addProperty(generateConfigProperty(tabGraphInfo))
                
                // 2. Generate container composable
                addFunction(generateContainerFunction(tabGraphInfo, tabContentMap))
                
                // 3. Generate graph builder
                addFunction(generateGraphBuilderFunction(tabGraphInfo))
            }
            .build()
        
        fileSpec.writeTo(outputStream)
    }
    
    /**
     * Generate: val MainTabConfig: TabNavigatorConfig = ...
     */
    private fun generateConfigProperty(tabGraphInfo: TabGraphInfo): PropertySpec {
        val configInitializer = CodeBlock.builder()
            .addStatement("TabNavigatorConfig(")
            .indent()
            .addStatement("allTabs = listOf(")
            .indent()
            .apply {
                tabGraphInfo.tabs.forEach { tab ->
                    addStatement("%L.%L,", tabGraphInfo.className, tab.name)
                }
            }
            .unindent()
            .addStatement("),")
            .addStatement("initialTab = %L.%L,", tabGraphInfo.className, tabGraphInfo.resolvedInitialTab.name)
            .addStatement("primaryTab = %L.%L", tabGraphInfo.className, tabGraphInfo.resolvedPrimaryTab.name)
            .unindent()
            .addStatement(")")
            .build()
        
        return PropertySpec.builder(
            name = tabGraphInfo.configName,
            type = ClassName("com.jermey.quo.vadis.core.navigation.core", "TabNavigatorConfig")
        )
            .initializer(configInitializer)
            .addKdoc(
                """
                Configuration for %L tabs.
                
                Generated from @TabGraph annotation.
                
                Tabs: %L
                Initial: %L
                Primary: %L
                """.trimIndent(),
                tabGraphInfo.className,
                tabGraphInfo.tabs.joinToString { it.label },
                tabGraphInfo.resolvedInitialTab.label,
                tabGraphInfo.resolvedPrimaryTab.label
            )
            .build()
    }
    
    /**
     * Generate: @Composable fun MainTabContainer(parentNavigator: Navigator) { ... }
     */
    private fun generateContainerFunction(
        tabGraphInfo: TabGraphInfo,
        tabContentMap: Map<String, TabContentInfo>
    ): FunSpec {
        return FunSpec.builder(tabGraphInfo.containerName)
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("parentNavigator", ClassName("com.jermey.quo.vadis.core.navigation.core", "Navigator"))
            .addParameter(
                ParameterSpec.builder("modifier", ClassName("androidx.compose.ui", "Modifier"))
                    .defaultValue("Modifier")
                    .build()
            )
            .addCode(generateContainerBody(tabGraphInfo, tabContentMap))
            .addKdoc(
                """
                Tab navigation container for %L.
                
                Displays a tabbed interface with %L tabs:
                %L
                
                @param parentNavigator Parent navigator for back press delegation
                @param modifier Modifier for the container
                
                Generated from @TabGraph annotation.
                """.trimIndent(),
                tabGraphInfo.className,
                tabGraphInfo.tabs.size,
                tabGraphInfo.tabs.joinToString("\n") { "- ${it.label}" }
            )
            .build()
    }
    
    private fun generateContainerBody(
        tabGraphInfo: TabGraphInfo,
        tabContentMap: Map<String, TabContentInfo>
    ): CodeBlock {
        return CodeBlock.builder()
            .addStatement("val tabState = rememberTabNavigator(%L, parentNavigator)", tabGraphInfo.configName)
            .addStatement("")
            .addStatement("TabbedNavHost(")
            .indent()
            .addStatement("tabState = tabState,")
            .addStatement("tabGraphs = mapOf(")
            .indent()
            .apply {
                tabGraphInfo.tabs.forEach { tab ->
                    addStatement(
                        "%L.%L to %L(),",
                        tabGraphInfo.className,
                        tab.name,
                        tab.rootGraphBuilderName
                    )
                }
            }
            .unindent()
            .addStatement("),")
            .addStatement("parentNavigator = parentNavigator,")
            .addStatement("modifier = modifier")
            .unindent()
            .addStatement(")")
            .build()
    }
    
    /**
     * Generate: fun buildMainTabGraph(): NavigationGraph { ... }
     */
    private fun generateGraphBuilderFunction(tabGraphInfo: TabGraphInfo): FunSpec {
        return FunSpec.builder(tabGraphInfo.graphBuilderName)
            .returns(ClassName("com.jermey.quo.vadis.core.navigation.core", "NavigationGraph"))
            .addCode(
                CodeBlock.builder()
                    .addStatement("return navigationGraph(%S) {", tabGraphInfo.name)
                    .indent()
                    .addStatement("// Tab graphs are composed in %L", tabGraphInfo.containerName)
                    .addStatement("// This function exists for consistency with other graph builders")
                    .unindent()
                    .addStatement("}")
                    .build()
            )
            .addKdoc(
                """
                Build navigation graph for %L tabs.
                
                Note: Tab graphs are automatically composed in %L.
                This function is provided for consistency with other graph builders.
                
                Generated from @TabGraph annotation.
                """.trimIndent(),
                tabGraphInfo.className,
                tabGraphInfo.containerName
            )
            .build()
    }
}
```

---

### Step 5: Enhanced KSP Processor

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`

**Modify existing processor** to handle `@TabGraph`:

```kotlin
class QuoVadisSymbolProcessor(
    // ... existing parameters
) : SymbolProcessor {
    
    private val tabGraphExtractor = TabGraphExtractor(logger)
    private val tabGraphGenerator = TabGraphGenerator()
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // ... existing @Graph processing ...
        
        // NEW: Process @TabGraph annotations
        processTabGraphs(resolver)
        
        return emptyList()
    }
    
    private fun processTabGraphs(resolver: Resolver) {
        resolver.getSymbolsWithAnnotation(TabGraph::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                val tabGraphInfo = tabGraphExtractor.extract(classDeclaration)
                if (tabGraphInfo != null) {
                    // Find custom @TabContent functions
                    val tabContentMap = tabGraphExtractor.findTabContent(resolver, tabGraphInfo)
                    
                    // Generate code
                    val file = codeGenerator.createNewFile(
                        dependencies = Dependencies(false, classDeclaration.containingFile!!),
                        packageName = tabGraphInfo.packageName,
                        fileName = "${tabGraphInfo.className}Generated"
                    )
                    
                    file.use { outputStream ->
                        tabGraphGenerator.generate(tabGraphInfo, tabContentMap, outputStream)
                    }
                    
                    logger.info("Generated tab graph code for ${tabGraphInfo.className}")
                }
            }
    }
}
```

---

### Step 6: Update Existing Tab Implementation in Code

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt`

**Add helper for generated code**:

```kotlin
/**
 * Base implementation for tabs defined via @Tab annotation.
 * 
 * Generated tab classes extend this to provide implementations.
 */
abstract class GeneratedTabDefinition : TabDefinition {
    // Default implementations that can be overridden by generated code
}
```

---

## Example Generated Code

**Input**:
```kotlin
@TabGraph("main")
sealed class MainTab : TabDefinition {
    @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeDestination::class)
    data object Home : MainTab()
    
    @Tab(route = "profile", label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    data object Profile : MainTab()
}
```

**Generated Output**:
```kotlin
// Generated by Quo Vadis KSP. Do not edit manually.

package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.TabbedNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorConfig

/**
 * Configuration for MainTab tabs.
 * 
 * Generated from @TabGraph annotation.
 * 
 * Tabs: Home, Profile
 * Initial: Home
 * Primary: Home
 */
val MainTabConfig: TabNavigatorConfig = TabNavigatorConfig(
    allTabs = listOf(
        MainTab.Home,
        MainTab.Profile,
    ),
    initialTab = MainTab.Home,
    primaryTab = MainTab.Home
)

/**
 * Tab navigation container for MainTab.
 * 
 * Displays a tabbed interface with 2 tabs:
 * - Home
 * - Profile
 * 
 * @param parentNavigator Parent navigator for back press delegation
 * @param modifier Modifier for the container
 * 
 * Generated from @TabGraph annotation.
 */
@Composable
fun MainTabContainer(
    parentNavigator: Navigator,
    modifier: Modifier = Modifier
) {
    val tabState = rememberTabNavigator(MainTabConfig, parentNavigator)
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = mapOf(
            MainTab.Home to buildHomeDestinationGraph(),
            MainTab.Profile to buildProfileDestinationGraph(),
        ),
        parentNavigator = parentNavigator,
        modifier = modifier
    )
}

/**
 * Build navigation graph for MainTab tabs.
 * 
 * Note: Tab graphs are automatically composed in MainTabContainer.
 * This function is provided for consistency with other graph builders.
 * 
 * Generated from @TabGraph annotation.
 */
fun buildMainTabGraph(): NavigationGraph {
    return navigationGraph("main") {
        // Tab graphs are composed in MainTabContainer
        // This function exists for consistency with other graph builders
    }
}
```

---

## File Structure Summary

Modified files:

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/
└── Annotations.kt                (MODIFIED - add ~150 lines)

quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/
├── TabGraphInfo.kt               (NEW - ~150 lines)
├── TabGraphExtractor.kt          (NEW - ~200 lines)
├── TabGraphGenerator.kt          (NEW - ~300 lines)
└── QuoVadisSymbolProcessor.kt    (MODIFIED - add ~50 lines)

quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/
└── TabDefinition.kt              (MODIFIED - add ~20 lines)
```

**Total**: ~870 lines of new/modified code

---

## Quality Checklist

Before completing Phase 3:

### Code Quality
- [ ] Generated code follows Kotlin style guide
- [ ] All generated code has KDoc comments
- [ ] Error messages are clear and actionable
- [ ] Handles edge cases gracefully

### Code Generation
- [ ] Validates annotation usage at compile time
- [ ] Generates clean, readable code
- [ ] No unnecessary imports
- [ ] Proper indentation and formatting

### Testing
- [ ] Unit tests for extractors
- [ ] Integration tests with sample annotations
- [ ] Verify generated code compiles
- [ ] Test error cases (missing parameters, etc.)

### Documentation
- [ ] Annotations have comprehensive KDoc
- [ ] Examples in annotation KDoc
- [ ] Generated code is self-documenting
- [ ] Migration guide from manual setup

---

## Verification Steps

After implementation:

1. **Build**: `./gradlew :quo-vadis-ksp:build`
2. **Test Annotations**: Create sample `@TabGraph` in `composeApp`
3. **Verify Generation**: Check `build/generated/ksp/...` for output
4. **Compile Generated**: Ensure generated code compiles
5. **Run Demo**: Use generated code in demo app

---

**Status**: 🔴 Not Started

**Next Phase**: Phase 4 - Demo App Refactoring

**Depends On**: 
- Phase 1 (Core Foundation) ✅
- Phase 2 (Compose Integration) ✅
