# Task 3.2: Create NavigationConfigGenerator

> **Task Status**: ✅ Completed  
> **Completed**: December 16, 2024  
> **Estimated Effort**: 4-5 days  
> **Dependencies**: Task 3.1 (Generator Base Classes)  
> **Blocks**: Task 3.3, Task 3.4

---

## Objective

Create the main `NavigationConfigGenerator` and its sub-generators that produce the unified `GeneratedNavigationConfig.kt` file. This generator orchestrates multiple specialized sub-generators to produce DSL-style code blocks for screens, containers, scopes, transitions, and wrappers.

**Key Outcome**: A single generated file that:
- Implements `NavigationConfig` interface
- Uses `navigationConfig { }` DSL internally
- Consolidates all registrations in organized sections
- Replaces 6+ currently generated files

---

## Files to Create

### File Structure

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/
├── NavigationConfigGenerator.kt     # Main orchestrating generator
├── ScreenBlockGenerator.kt          # Generates screen<T> blocks
├── ContainerBlockGenerator.kt       # Generates tabs/stack/panes blocks
├── ScopeBlockGenerator.kt           # Generates scope() blocks
├── TransitionBlockGenerator.kt      # Generates transition<T> blocks
├── WrapperBlockGenerator.kt         # Generates wrapper blocks
└── DeepLinkBlockGenerator.kt        # Generates deep link handling
```

---

## Generated Output Structure

The `NavigationConfigGenerator` produces this file structure:

```kotlin
// GeneratedNavigationConfig.kt
package com.example.app.navigation.generated

import com.jermey.quo.vadis.core.navigation.*
import com.jermey.quo.vadis.core.navigation.dsl.*
import com.example.app.destinations.*
import kotlin.reflect.KClass

/**
 * Auto-generated navigation configuration.
 * [KDoc from StringTemplates.NAVIGATION_CONFIG_KDOC]
 */
object GeneratedNavigationConfig : NavigationConfig {
    
    private val config = navigationConfig {
        // ═══════════════════════════════════════════════
        // SCREENS
        // ═══════════════════════════════════════════════
        
        screen<HomeDestination.Feed> { dest ->
            FeedScreen(navigator = navigator)
        }
        // ... more screens
        
        // ═══════════════════════════════════════════════
        // CONTAINERS
        // ═══════════════════════════════════════════════
        
        tabs<MainTabs>(scopeKey = "MainTabs", wrapperKey = "mainTabsWrapper") {
            // ... tab definitions
        }
        // ... more containers
        
        // ═══════════════════════════════════════════════
        // SCOPES
        // ═══════════════════════════════════════════════
        
        scope("MainTabs", MainTabs.HomeTab::class, MainTabs.ExploreTab::class)
        // ... more scopes
        
        // ═══════════════════════════════════════════════
        // TRANSITIONS
        // ═══════════════════════════════════════════════
        
        transition<HomeDestination.Detail>(NavTransitions.SharedElement)
        // ... more transitions
        
        // ═══════════════════════════════════════════════
        // WRAPPERS
        // ═══════════════════════════════════════════════
        
        tabsContainer("mainTabsWrapper") { /* ... */ }
        // ... more wrappers
    }
    
    // ═══════════════════════════════════════════════
    // NAVIGATION CONFIG IMPLEMENTATION
    // ═══════════════════════════════════════════════
    
    override val screenRegistry: ScreenRegistry = config.screenRegistry
    override val scopeRegistry: ScopeRegistry = config.scopeRegistry
    override val transitionRegistry: TransitionRegistry = config.transitionRegistry
    override val containerRegistry: ContainerRegistry = config.containerRegistry // includes wrapper functionality
    override val deepLinkHandler: DeepLinkHandler = config.deepLinkHandler
    
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode? = config.buildNavNode(destinationClass, key, parentKey)
    
    override fun plus(other: NavigationConfig): NavigationConfig =
        CompositeNavigationConfig(this, other)
    
    // ═══════════════════════════════════════════════
    // CONVENIENCE EXTENSIONS
    // ═══════════════════════════════════════════════
    
    /**
     * Root destinations available for navigation.
     */
    val roots: Set<KClass<out Destination>> = setOf(
        MainTabs::class,
        ProfileStack::class
    )
}
```

---

## File Specifications

### 1. NavigationConfigGenerator.kt

**Purpose**: Main generator that orchestrates all sub-generators and produces the final file.

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.jermey.quo.vadis.ksp.generators.base.DslCodeGenerator
import com.jermey.quo.vadis.ksp.generators.base.StringTemplates
import com.jermey.quo.vadis.ksp.models.*
import com.squareup.kotlinpoet.*

/**
 * Main generator for the unified GeneratedNavigationConfig.kt file.
 * 
 * This generator orchestrates multiple sub-generators to produce a single
 * file that implements [NavigationConfig] using the DSL-based approach.
 * 
 * ## Generated Structure
 * 
 * The generated file contains:
 * 1. A private `config` property built using `navigationConfig { }`
 * 2. NavigationConfig interface implementation delegating to config
 * 3. Convenience properties (roots, etc.)
 * 
 * ## Sub-Generators
 * 
 * - [ScreenBlockGenerator] - Generates `screen<T>` blocks
 * - [ContainerBlockGenerator] - Generates `tabs`, `stack`, `panes` blocks
 * - [ScopeBlockGenerator] - Generates `scope()` calls
 * - [TransitionBlockGenerator] - Generates `transition<T>` calls
 * - [WrapperBlockGenerator] - Generates `tabsContainer`/`paneContainer` blocks
 * - [DeepLinkBlockGenerator] - Generates deep link configuration
 * 
 * @param codeGenerator KSP code generator
 * @param logger KSP logger
 * @param packageName Target package for generated code
 */
class NavigationConfigGenerator(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    private val packageName: String
) : DslCodeGenerator(codeGenerator, logger) {
    
    override val generatedPackage: String = packageName
    
    // Sub-generators
    private val screenBlockGenerator = ScreenBlockGenerator(logger)
    private val containerBlockGenerator = ContainerBlockGenerator(logger)
    private val scopeBlockGenerator = ScopeBlockGenerator(logger)
    private val transitionBlockGenerator = TransitionBlockGenerator(logger)
    private val wrapperBlockGenerator = WrapperBlockGenerator(logger)
    private val deepLinkBlockGenerator = DeepLinkBlockGenerator(logger)
    
    /**
     * Data class containing all collected navigation information.
     */
    data class NavigationData(
        val screens: List<ScreenInfo>,
        val stacks: List<StackInfo>,
        val tabs: List<TabInfo>,
        val panes: List<PaneInfo>,
        val transitions: List<TransitionInfo>,
        val wrappers: List<WrapperInfo>,
        val destinations: List<DestinationInfo>
    )
    
    /**
     * Generates the unified GeneratedNavigationConfig.kt file.
     * 
     * @param data All collected navigation data from extractors
     * @param originatingFiles Source files that triggered generation
     */
    fun generate(
        data: NavigationData,
        originatingFiles: List<KSFile>
    ) {
        logInfo("Generating NavigationConfig with ${data.screens.size} screens, " +
                "${data.tabs.size + data.stacks.size + data.panes.size} containers")
        
        val fileSpec = buildFileSpec(data)
        writeFile(fileSpec, originatingFiles)
        
        logInfo("Successfully generated $GENERATED_FILE_NAME.kt")
    }
    
    private fun buildFileSpec(data: NavigationData): FileSpec {
        return createFileBuilder(GENERATED_FILE_NAME, packageName)
            .addImports(data)
            .addType(buildConfigObject(data))
            .build()
    }
    
    private fun FileSpec.Builder.addImports(data: NavigationData): FileSpec.Builder {
        // Core navigation imports
        addImport("com.jermey.quo.vadis.core.navigation", "NavigationConfig")
        addImport("com.jermey.quo.vadis.core.navigation", "Destination")
        addImport("com.jermey.quo.vadis.core.navigation", "NavNode")
        addImport("com.jermey.quo.vadis.core.navigation.dsl", "navigationConfig")
        addImport("com.jermey.quo.vadis.core.navigation.compose.registry", 
            "ScreenRegistry", "WrapperRegistry", "ScopeRegistry", 
            "TransitionRegistry", "ContainerRegistry")
        addImport("com.jermey.quo.vadis.core.navigation.core", "DeepLinkHandler")
        addImport("kotlin.reflect", "KClass")
        
        // Destination imports are handled by KotlinPoet via %T formatting
        
        return this
    }
    
    private fun buildConfigObject(data: NavigationData): TypeSpec {
        return TypeSpec.objectBuilder(GENERATED_OBJECT_NAME)
            .addKdoc(StringTemplates.NAVIGATION_CONFIG_KDOC)
            .addSuperinterface(ClassName("com.jermey.quo.vadis.core.navigation", "NavigationConfig"))
            .addProperty(buildConfigProperty(data))
            .addProperties(buildDelegationProperties())
            .addFunction(buildBuildNavNodeFunction())
            .addFunction(buildPlusFunction())
            .addProperty(buildRootsProperty(data))
            .build()
    }
    
    /**
     * Builds the private `config` property with the DSL configuration.
     */
    private fun buildConfigProperty(data: NavigationData): PropertySpec {
        val dslContent = buildDslContent(data)
        
        return PropertySpec.builder(
            "config",
            ClassName("com.jermey.quo.vadis.core.navigation", "NavigationConfig")
        )
            .addModifiers(KModifier.PRIVATE)
            .initializer(CodeBlock.builder()
                .beginControlFlow("navigationConfig")
                .add(dslContent)
                .endControlFlow()
                .build()
            )
            .build()
    }
    
    /**
     * Builds the complete DSL content inside navigationConfig { }.
     */
    private fun buildDslContent(data: NavigationData): CodeBlock {
        val builder = CodeBlock.builder()
        
        // SCREENS section
        if (data.screens.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.SCREENS_SECTION)
            builder.add("\n\n")
            builder.add(screenBlockGenerator.generate(data.screens))
        }
        
        // CONTAINERS section
        val hasContainers = data.tabs.isNotEmpty() || data.stacks.isNotEmpty() || data.panes.isNotEmpty()
        if (hasContainers) {
            builder.add("\n")
            builder.add(StringTemplates.CONTAINERS_SECTION)
            builder.add("\n\n")
            builder.add(containerBlockGenerator.generate(data.tabs, data.stacks, data.panes))
        }
        
        // SCOPES section
        val scopeData = collectScopeData(data)
        if (scopeData.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.SCOPES_SECTION)
            builder.add("\n\n")
            builder.add(scopeBlockGenerator.generate(scopeData))
        }
        
        // TRANSITIONS section
        if (data.transitions.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.TRANSITIONS_SECTION)
            builder.add("\n\n")
            builder.add(transitionBlockGenerator.generate(data.transitions))
        }
        
        // WRAPPERS section
        if (data.wrappers.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.WRAPPERS_SECTION)
            builder.add("\n\n")
            builder.add(wrapperBlockGenerator.generate(data.wrappers))
        }
        
        return builder.build()
    }
    
    /**
     * Collects scope data from containers.
     */
    private fun collectScopeData(data: NavigationData): Map<String, List<ClassName>> {
        val scopes = mutableMapOf<String, MutableList<ClassName>>()
        
        // Collect from tabs
        data.tabs.forEach { tab ->
            val scopeKey = tab.scopeKey ?: tab.containerClass.simpleName.asString()
            scopes.getOrPut(scopeKey) { mutableListOf() }
                .addAll(tab.items.map { resolveClassName(it.destination) })
        }
        
        // Collect from stacks
        data.stacks.forEach { stack ->
            val scopeKey = stack.scopeKey ?: stack.containerClass.simpleName.asString()
            scopes.getOrPut(scopeKey) { mutableListOf() }
                .addAll(stack.screens.map { resolveClassName(it.destination) })
        }
        
        // Collect from panes
        data.panes.forEach { pane ->
            val scopeKey = pane.scopeKey ?: pane.containerClass.simpleName.asString()
            scopes.getOrPut(scopeKey) { mutableListOf() }
                .addAll(pane.items.map { resolveClassName(it.destination) })
        }
        
        return scopes
    }
    
    /**
     * Builds the delegation properties for NavigationConfig implementation.
     */
    private fun buildDelegationProperties(): List<PropertySpec> {
        return listOf(
            buildDelegationProperty("screenRegistry", "ScreenRegistry"),
            buildDelegationProperty("wrapperRegistry", "WrapperRegistry"),
            buildDelegationProperty("scopeRegistry", "ScopeRegistry"),
            buildDelegationProperty("transitionRegistry", "TransitionRegistry"),
            buildDelegationProperty("containerRegistry", "ContainerRegistry"),
            buildDelegationProperty("deepLinkHandler", "DeepLinkHandler", 
                "com.jermey.quo.vadis.core.navigation.core")
        )
    }
    
    private fun buildDelegationProperty(
        name: String, 
        typeName: String,
        packageName: String = "com.jermey.quo.vadis.core.navigation.compose.registry"
    ): PropertySpec {
        return PropertySpec.builder(name, ClassName(packageName, typeName))
            .addModifiers(KModifier.OVERRIDE)
            .initializer("config.$name")
            .build()
    }
    
    /**
     * Builds the buildNavNode function implementation.
     */
    private fun buildBuildNavNodeFunction(): FunSpec {
        return FunSpec.builder("buildNavNode")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destinationClass", 
                ClassName("kotlin.reflect", "KClass")
                    .parameterizedBy(WildcardTypeName.producerOf(
                        ClassName("com.jermey.quo.vadis.core.navigation", "Destination")
                    ))
            )
            .addParameter(
                ParameterSpec.builder("key", String::class.asTypeName().copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("parentKey", String::class.asTypeName().copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .returns(ClassName("com.jermey.quo.vadis.core.navigation", "NavNode").copy(nullable = true))
            .addStatement("return config.buildNavNode(destinationClass, key, parentKey)")
            .build()
    }
    
    /**
     * Builds the plus operator function implementation.
     */
    private fun buildPlusFunction(): FunSpec {
        val configType = ClassName("com.jermey.quo.vadis.core.navigation", "NavigationConfig")
        
        return FunSpec.builder("plus")
            .addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            .addParameter("other", configType)
            .returns(configType)
            .addStatement("return %T(this, other)", 
                ClassName("com.jermey.quo.vadis.core.navigation", "CompositeNavigationConfig"))
            .build()
    }
    
    /**
     * Builds the roots property listing all root containers.
     */
    private fun buildRootsProperty(data: NavigationData): PropertySpec {
        val rootClasses = mutableListOf<ClassName>()
        
        // Tabs are typically roots
        data.tabs.forEach { rootClasses.add(resolveClassName(it.containerClass)) }
        
        // Top-level stacks can be roots
        data.stacks.filter { it.isRoot }.forEach { 
            rootClasses.add(resolveClassName(it.containerClass)) 
        }
        
        // Top-level panes can be roots
        data.panes.filter { it.isRoot }.forEach { 
            rootClasses.add(resolveClassName(it.containerClass)) 
        }
        
        val kClassType = ClassName("kotlin.reflect", "KClass")
            .parameterizedBy(WildcardTypeName.producerOf(
                ClassName("com.jermey.quo.vadis.core.navigation", "Destination")
            ))
        
        val setType = ClassName("kotlin.collections", "Set").parameterizedBy(kClassType)
        
        val initializerBuilder = CodeBlock.builder()
            .add("setOf(\n")
            .indent()
        
        rootClasses.forEachIndexed { index, className ->
            initializerBuilder.add("%T::class", className)
            if (index < rootClasses.size - 1) {
                initializerBuilder.add(",")
            }
            initializerBuilder.add("\n")
        }
        
        initializerBuilder.unindent().add(")")
        
        return PropertySpec.builder("roots", setType)
            .addKdoc("Root destinations available for navigation.")
            .initializer(initializerBuilder.build())
            .build()
    }
}
```

### 2. ScreenBlockGenerator.kt

**Purpose**: Generates `screen<DestinationType>` DSL blocks from `ScreenInfo` list.

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.generators.base.CodeBlockBuilders
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates `screen<T>` DSL blocks for screen registrations.
 * 
 * Input: List of [ScreenInfo] from ScreenExtractor
 * Output: CodeBlock containing all screen registrations
 * 
 * ## Example Output
 * ```kotlin
 * screen<HomeDestination.Feed> { dest ->
 *     FeedScreen(navigator = navigator)
 * }
 * 
 * screen<HomeDestination.Detail> { dest ->
 *     DetailScreen(destination = dest, navigator = navigator)
 * }
 * ```
 */
class ScreenBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates screen registration blocks for all screens.
     * 
     * @param screens List of screen information from extractor
     * @return CodeBlock containing all screen<T> { } registrations
     */
    fun generate(screens: List<ScreenInfo>): CodeBlock {
        if (screens.isEmpty()) return CodeBlock.of("")
        
        val builder = CodeBlock.builder()
        
        screens.forEachIndexed { index, screen ->
            builder.add(generateScreenBlock(screen))
            if (index < screens.size - 1) {
                builder.add("\n")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Generates a single screen<T> block.
     */
    private fun generateScreenBlock(screen: ScreenInfo): CodeBlock {
        val destinationClass = screen.destination.toClassName()
        val functionName = screen.functionName
        
        // Determine parameters based on ScreenInfo
        val hasDestinationParam = screen.parameters.any { 
            it.name == "destination" || it.type == screen.destination.qualifiedName?.asString()
        }
        val hasNavigatorParam = screen.parameters.any { it.name == "navigator" }
        
        // Build the function call
        val functionCall = buildFunctionCall(screen, hasDestinationParam, hasNavigatorParam)
        
        return CodeBlock.builder()
            .beginControlFlow("screen<%T> { dest ->", destinationClass)
            .addStatement(functionCall)
            .endControlFlow()
            .build()
    }
    
    /**
     * Builds the composable function call with appropriate parameters.
     */
    private fun buildFunctionCall(
        screen: ScreenInfo,
        hasDestinationParam: Boolean,
        hasNavigatorParam: Boolean
    ): String {
        val params = mutableListOf<String>()
        
        // Add destination parameter if needed
        if (hasDestinationParam) {
            // Check if it's the first parameter (positional) or named
            val destParam = screen.parameters.find { 
                it.name == "destination" || it.type == screen.destination.qualifiedName?.asString()
            }
            if (destParam != null) {
                if (destParam.name == "destination") {
                    params.add("destination = dest")
                } else {
                    params.add("${destParam.name} = dest")
                }
            }
        }
        
        // Add navigator parameter
        if (hasNavigatorParam) {
            params.add("navigator = navigator")
        }
        
        // Add other parameters from screen info
        screen.parameters
            .filter { it.name != "destination" && it.name != "navigator" }
            .filter { it.type != screen.destination.qualifiedName?.asString() }
            .forEach { param ->
                when {
                    param.hasDefault -> {} // Skip parameters with defaults
                    param.type == "String" -> params.add("${param.name} = dest.${param.name}")
                    param.type == "Int" -> params.add("${param.name} = dest.${param.name}")
                    else -> params.add("${param.name} = dest.${param.name}")
                }
            }
        
        return "${screen.functionName}(${params.joinToString(", ")})"
    }
}
```

### 3. ContainerBlockGenerator.kt

**Purpose**: Generates `tabs<T>`, `stack<T>`, and `panes<T>` DSL blocks.

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.generators.base.CodeBlockBuilders
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates container DSL blocks (tabs, stack, panes).
 * 
 * ## Example Output - Tabs
 * ```kotlin
 * tabs<MainTabs>(scopeKey = "MainTabs", wrapperKey = "mainWrapper") {
 *     initialTab = 0
 *     tab(MainTabs.HomeTab, title = "Home", icon = Icons.Home)
 *     tab(MainTabs.ExploreTab, title = "Explore") {
 *         screen(ExploreDestination.List)
 *     }
 * }
 * ```
 * 
 * ## Example Output - Stack
 * ```kotlin
 * stack<ProfileStack>(scopeKey = "ProfileStack") {
 *     screen(ProfileDestination.Main)
 * }
 * ```
 * 
 * ## Example Output - Panes
 * ```kotlin
 * panes<MasterDetail>(scopeKey = "MasterDetail") {
 *     initialPane = PaneRole.Primary
 *     primary(weight = 0.4f) { root(MasterDetail.List) }
 *     secondary(weight = 0.6f) { root(MasterDetail.Detail) }
 * }
 * ```
 */
class ContainerBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates all container blocks.
     * 
     * @param tabs List of tab container info
     * @param stacks List of stack container info
     * @param panes List of pane container info
     * @return CodeBlock containing all container definitions
     */
    fun generate(
        tabs: List<TabInfo>,
        stacks: List<StackInfo>,
        panes: List<PaneInfo>
    ): CodeBlock {
        val builder = CodeBlock.builder()
        
        // Generate tabs
        tabs.forEachIndexed { index, tab ->
            builder.add(generateTabsBlock(tab))
            if (index < tabs.size - 1 || stacks.isNotEmpty() || panes.isNotEmpty()) {
                builder.add("\n")
            }
        }
        
        // Generate stacks
        stacks.forEachIndexed { index, stack ->
            builder.add(generateStackBlock(stack))
            if (index < stacks.size - 1 || panes.isNotEmpty()) {
                builder.add("\n")
            }
        }
        
        // Generate panes
        panes.forEachIndexed { index, pane ->
            builder.add(generatePanesBlock(pane))
            if (index < panes.size - 1) {
                builder.add("\n")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Generates a tabs<T> block.
     */
    private fun generateTabsBlock(tab: TabInfo): CodeBlock {
        val containerClass = tab.containerClass.toClassName()
        val scopeKey = tab.scopeKey ?: tab.containerClass.simpleName.asString()
        
        val builder = CodeBlock.builder()
        
        // Build parameters
        val params = mutableListOf("scopeKey = %S")
        val args = mutableListOf<Any>(containerClass, scopeKey)
        
        if (tab.wrapperKey != null) {
            params.add("wrapperKey = %S")
            args.add(tab.wrapperKey)
        }
        
        builder.beginControlFlow("tabs<%T>(${params.joinToString(", ")})", *args.toTypedArray())
        
        // Add initialTab if not 0
        if (tab.initialTabIndex != 0) {
            builder.addStatement("initialTab = %L", tab.initialTabIndex)
        }
        
        // Generate tab entries
        tab.items.forEach { item ->
            builder.add(generateTabEntry(item, tab))
        }
        
        builder.endControlFlow()
        
        return builder.build()
    }
    
    /**
     * Generates a single tab entry (tab).
     */
    private fun generateTabEntry(item: TabInfo.TabItem, parentTab: TabInfo): CodeBlock {
        val destClass = item.destination.toClassName()
        val destExpression = "${parentTab.containerClass.simpleName.asString()}.${item.destination.simpleName.asString()}"
        
        return if (item.hasNestedStack) {
            // tab with nested stack
            CodeBlock.builder()
                .beginControlFlow("tab(%L, title = %S)", destExpression, item.title ?: item.destination.simpleName.asString())
                .apply {
                    item.nestedScreens.forEach { nestedScreen ->
                        addStatement("screen(%T)", nestedScreen.toClassName())
                    }
                }
                .endControlFlow()
                .build()
        } else {
            // Simple tab
            val params = mutableListOf(destExpression)
            if (item.title != null) {
                params.add("title = \"${item.title}\"")
            }
            if (item.icon != null) {
                params.add("icon = ${item.icon}")
            }
            CodeBlock.of("tab(${params.joinToString(", ")})\n")
        }
    }
    
    /**
     * Generates a stack<T> block.
     */
    private fun generateStackBlock(stack: StackInfo): CodeBlock {
        val containerClass = stack.containerClass.toClassName()
        val scopeKey = stack.scopeKey ?: stack.containerClass.simpleName.asString()
        
        val builder = CodeBlock.builder()
            .beginControlFlow("stack<%T>(scopeKey = %S)", containerClass, scopeKey)
        
        // Generate screen entries
        stack.screens.forEach { screen ->
            builder.addStatement("screen(%T)", screen.destination.toClassName())
        }
        
        builder.endControlFlow()
        
        return builder.build()
    }
    
    /**
     * Generates a panes<T> block.
     */
    private fun generatePanesBlock(pane: PaneInfo): CodeBlock {
        val containerClass = pane.containerClass.toClassName()
        val scopeKey = pane.scopeKey ?: pane.containerClass.simpleName.asString()
        
        val builder = CodeBlock.builder()
        
        // Build parameters
        val params = mutableListOf("scopeKey = %S")
        val args = mutableListOf<Any>(containerClass, scopeKey)
        
        if (pane.wrapperKey != null) {
            params.add("wrapperKey = %S")
            args.add(pane.wrapperKey)
        }
        
        builder.beginControlFlow("panes<%T>(${params.joinToString(", ")})", *args.toTypedArray())
        
        // Add initial pane if not Primary
        if (pane.initialPane != "Primary") {
            builder.addStatement("initialPane = PaneRole.%L", pane.initialPane)
        }
        
        // Add back behavior if not default
        if (pane.backBehavior != "PopOrSwitchPane") {
            builder.addStatement("backBehavior = PaneBackBehavior.%L", pane.backBehavior)
        }
        
        // Generate pane entries
        pane.items.forEach { item ->
            builder.add(generatePaneEntry(item))
        }
        
        builder.endControlFlow()
        
        return builder.build()
    }
    
    /**
     * Generates a pane entry (primary, secondary, tertiary).
     */
    private fun generatePaneEntry(item: PaneInfo.PaneItem): CodeBlock {
        val roleName = item.role.lowercase()
        val destClass = item.destination.toClassName()
        
        val params = mutableListOf<String>()
        if (item.weight != 1.0f) {
            params.add("weight = ${item.weight}f")
        }
        if (item.minWidth != null) {
            params.add("minWidth = ${item.minWidth}.dp")
        }
        
        val paramsStr = if (params.isNotEmpty()) "(${params.joinToString(", ")})" else ""
        
        return CodeBlock.builder()
            .beginControlFlow("$roleName$paramsStr")
            .addStatement("root(%T)", destClass)
            .endControlFlow()
            .build()
    }
}
```

### 4. ScopeBlockGenerator.kt

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

/**
 * Generates `scope()` DSL calls for scope definitions.
 * 
 * ## Example Output
 * ```kotlin
 * scope("MainTabs",
 *     MainTabs.HomeTab::class,
 *     MainTabs.ExploreTab::class,
 *     MainTabs.ProfileTab::class
 * )
 * 
 * scope("ProfileStack",
 *     ProfileDestination.Main::class,
 *     ProfileDestination.Settings::class
 * )
 * ```
 */
class ScopeBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates scope definition blocks.
     * 
     * @param scopes Map of scope key to list of destination classes
     * @return CodeBlock containing all scope definitions
     */
    fun generate(scopes: Map<String, List<ClassName>>): CodeBlock {
        if (scopes.isEmpty()) return CodeBlock.of("")
        
        val builder = CodeBlock.builder()
        
        scopes.entries.forEachIndexed { index, (scopeKey, destinations) ->
            if (destinations.isNotEmpty()) {
                builder.add(generateScopeBlock(scopeKey, destinations))
                if (index < scopes.size - 1) {
                    builder.add("\n")
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * Generates a single scope() call.
     */
    private fun generateScopeBlock(scopeKey: String, destinations: List<ClassName>): CodeBlock {
        val builder = CodeBlock.builder()
            .add("scope(%S,\n", scopeKey)
            .indent()
        
        destinations.forEachIndexed { index, className ->
            builder.add("%T::class", className)
            if (index < destinations.size - 1) {
                builder.add(",")
            }
            builder.add("\n")
        }
        
        builder.unindent()
            .add(")\n")
        
        return builder.build()
    }
}
```

### 5. TransitionBlockGenerator.kt

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates `transition<T>` DSL calls for transition registrations.
 * 
 * ## Example Output
 * ```kotlin
 * transition<HomeDestination.Detail>(NavTransitions.SharedElement)
 * transition<ProfileDestination>(NavTransitions.Slide)
 * ```
 */
class TransitionBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates transition registration blocks.
     * 
     * @param transitions List of transition info from extractor
     * @return CodeBlock containing all transition definitions
     */
    fun generate(transitions: List<TransitionInfo>): CodeBlock {
        if (transitions.isEmpty()) return CodeBlock.of("")
        
        val builder = CodeBlock.builder()
        
        transitions.forEachIndexed { index, transition ->
            builder.add(generateTransitionBlock(transition))
            if (index < transitions.size - 1) {
                builder.add("\n")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Generates a single transition<T> call.
     */
    private fun generateTransitionBlock(transition: TransitionInfo): CodeBlock {
        val destClass = transition.destination.toClassName()
        val transitionExpr = buildTransitionExpression(transition)
        
        return CodeBlock.of("transition<%T>($transitionExpr)\n", destClass)
    }
    
    /**
     * Builds the transition expression from TransitionInfo.
     */
    private fun buildTransitionExpression(transition: TransitionInfo): String {
        return when {
            transition.presetName != null -> "NavTransitions.${transition.presetName}"
            transition.customTransition != null -> transition.customTransition
            else -> buildCustomTransition(transition)
        }
    }
    
    /**
     * Builds a custom NavTransition from individual parameters.
     */
    private fun buildCustomTransition(transition: TransitionInfo): String {
        val parts = mutableListOf<String>()
        
        transition.enterTransition?.let { parts.add("enterTransition = $it") }
        transition.exitTransition?.let { parts.add("exitTransition = $it") }
        transition.popEnterTransition?.let { parts.add("popEnterTransition = $it") }
        transition.popExitTransition?.let { parts.add("popExitTransition = $it") }
        
        return if (parts.isEmpty()) {
            "NavTransitions.Default"
        } else {
            "NavTransition(${parts.joinToString(", ")})"
        }
    }
}
```

### 6. WrapperBlockGenerator.kt

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.squareup.kotlinpoet.CodeBlock

/**
 * Generates `tabsContainer` and `paneContainer` DSL blocks.
 * 
 * ## Example Output
 * ```kotlin
 * tabsContainer("mainTabsWrapper") {
 *     CustomTabBar(
 *         tabs = tabs,
 *         selectedIndex = activeTabIndex,
 *         onTabSelected = { switchToTab(it) }
 *     ) {
 *         content()
 *     }
 * }
 * 
 * paneContainer("masterDetailWrapper") {
 *     CustomPaneLayout(
 *         primaryContent = { PrimaryPaneContent() },
 *         secondaryContent = { SecondaryPaneContent() }
 *     )
 * }
 * ```
 */
class WrapperBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates wrapper registration blocks.
     * 
     * @param wrappers List of wrapper info from extractor
     * @return CodeBlock containing all wrapper definitions
     */
    fun generate(wrappers: List<WrapperInfo>): CodeBlock {
        if (wrappers.isEmpty()) return CodeBlock.of("")
        
        val builder = CodeBlock.builder()
        
        val tabsContainers = wrappers.filter { it.type == WrapperInfo.WrapperType.TAB }
        val paneContainers = wrappers.filter { it.type == WrapperInfo.WrapperType.PANE }
        
        // Generate tabs containers
        tabsContainers.forEachIndexed { index, wrapper ->
            builder.add(generateTabsContainerBlock(wrapper))
            if (index < tabsContainers.size - 1 || paneContainers.isNotEmpty()) {
                builder.add("\n")
            }
        }
        
        // Generate pane containers
        paneContainers.forEachIndexed { index, wrapper ->
            builder.add(generatePaneContainerBlock(wrapper))
            if (index < paneContainers.size - 1) {
                builder.add("\n")
            }
        }
        
        return builder.build()
    }
    
    /**
     * Generates a tabsContainer block.
     */
    private fun generateTabsContainerBlock(wrapper: WrapperInfo): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("tabsContainer(%S)", wrapper.key)
            .add(generateWrapperContent(wrapper))
            .endControlFlow()
            .build()
    }
    
    /**
     * Generates a paneContainer block.
     */
    private fun generatePaneContainerBlock(wrapper: WrapperInfo): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("paneContainer(%S)", wrapper.key)
            .add(generateWrapperContent(wrapper))
            .endControlFlow()
            .build()
    }
    
    /**
     * Generates the wrapper content (the composable function call).
     */
    private fun generateWrapperContent(wrapper: WrapperInfo): CodeBlock {
        // Generate the function call with scope parameters
        val functionName = wrapper.functionName
        
        // The wrapper function receives scope with tabs, activeTabIndex, etc.
        // We generate the function call that uses these scope properties
        return CodeBlock.of("%L()\n", functionName)
    }
}
```

### 7. DeepLinkBlockGenerator.kt

```kotlin
package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates deep link configuration in the DSL.
 * 
 * Deep links are typically handled through the route parameter in @Destination,
 * and this generator ensures they're properly included in the config.
 * 
 * Note: Full deep link DSL support may be added in a future iteration.
 * Currently, deep links are handled by the DeepLinkHandler implementation.
 */
class DeepLinkBlockGenerator(
    private val logger: KSPLogger
) {
    
    /**
     * Generates deep link related code.
     * 
     * For now, this is a placeholder for future deep link DSL expansion.
     * The actual deep link handling is done by the generated DeepLinkHandler.
     * 
     * @param destinations List of destinations with route information
     * @return CodeBlock (currently empty, reserved for future use)
     */
    fun generate(destinations: List<DestinationInfo>): CodeBlock {
        // Deep links are currently handled outside the DSL config
        // through the DeepLinkHandler implementation
        // This generator is reserved for future DSL-based deep link configuration
        
        val destinationsWithRoutes = destinations.filter { it.route != null }
        
        if (destinationsWithRoutes.isEmpty()) {
            return CodeBlock.of("")
        }
        
        // Future: Generate deepLink DSL blocks
        // For now, log info about destinations with routes
        logger.info("Found ${destinationsWithRoutes.size} destinations with routes for deep linking")
        
        return CodeBlock.of("// Deep link handling configured via DeepLinkHandler\n")
    }
}
```

---

## Dependencies

### This Task Depends On

| Dependency | Description | Status |
|------------|-------------|--------|
| Task 3.1 | Generator base classes | Required |
| Phase 1 | DSL types (`NavigationConfig`, etc.) | Required |
| Phase 2 | `navigationConfig { }` function | Required |
| Existing Models | `ScreenInfo`, `TabInfo`, etc. | ✅ Available |

### What This Task Blocks

| Task | Dependency Type |
|------|-----------------|
| Task 3.3 (Refactor Existing) | Integration with new generator |
| Task 3.4 (Processor Orchestration) | Uses NavigationConfigGenerator |

---

## Acceptance Criteria Checklist

### NavigationConfigGenerator.kt
- [ ] Implements `DslCodeGenerator` base class
- [ ] Accepts all required data via `NavigationData` class
- [ ] Produces single `GeneratedNavigationConfig.kt` file
- [ ] Generated file has correct package and imports
- [ ] Object implements `NavigationConfig` interface
- [ ] DSL content organized in clear sections with comments
- [ ] `buildNavNode()` delegation implemented correctly
- [ ] `plus()` operator implemented correctly
- [ ] `roots` property includes all root containers

### ScreenBlockGenerator.kt
- [ ] Generates valid `screen<T>` DSL blocks
- [ ] Handles destinations with and without parameters
- [ ] Correctly passes navigator parameter
- [ ] Correctly passes destination parameter when needed
- [ ] Handles nested destination classes

### ContainerBlockGenerator.kt
- [ ] Generates valid `tabs<T>` DSL blocks
- [ ] Generates valid `stack<T>` DSL blocks
- [ ] Generates valid `panes<T>` DSL blocks
- [ ] Handles `initialTab`, `wrapperKey`, `scopeKey` parameters
- [ ] Handles nested tabs with stacks
- [ ] Handles pane weights and min widths

### ScopeBlockGenerator.kt
- [ ] Generates valid `scope()` DSL calls
- [ ] Handles multiple destinations per scope
- [ ] Handles empty scope gracefully

### TransitionBlockGenerator.kt
- [ ] Generates valid `transition<T>` DSL calls
- [ ] Handles preset transitions (NavTransitions.X)
- [ ] Handles custom transition definitions

### WrapperBlockGenerator.kt
- [ ] Generates valid `tabsContainer` blocks
- [ ] Generates valid `paneContainer` blocks
- [ ] Function calls include scope parameters

### Generated Code Quality
- [ ] All generated code compiles without errors
- [ ] Generated code follows Kotlin style conventions
- [ ] Imports are organized and minimal
- [ ] KDoc comments included where appropriate
- [ ] No redundant or dead code generated

### Testing
- [ ] Unit tests for each sub-generator
- [ ] Integration test with sample data produces compilable output
- [ ] Generated code works with existing navigation APIs

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| NavigationConfigGenerator structure | 1 day |
| ScreenBlockGenerator | 0.5 days |
| ContainerBlockGenerator | 1.5 days |
| ScopeBlockGenerator | 0.25 days |
| TransitionBlockGenerator | 0.25 days |
| WrapperBlockGenerator | 0.5 days |
| DeepLinkBlockGenerator | 0.25 days |
| Integration & Testing | 1 day |
| **Total** | **4-5 days** |

---

## Related Files

- [Phase 3 Summary](./SUMMARY.md)
- [Task 3.1 - Generator Base Classes](./TASK-3.1-generator-base-classes.md)
- [Task 3.3 - Refactor Existing Generators](./TASK-3.3-refactor-existing-generators.md)
- [Task 3.4 - Processor Orchestration](./TASK-3.4-processor-orchestration.md)

### Current Generator Files for Reference
- [ScreenRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScreenRegistryGenerator.kt) - Screen generation patterns
- [ContainerRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ContainerRegistryGenerator.kt) - Container patterns
- [NavNodeBuilderGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt) - Node building logic

### Model Files
- [ScreenInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/ScreenInfo.kt)
- [TabInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt)
- [StackInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/StackInfo.kt)
- [PaneInfo.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/PaneInfo.kt)
