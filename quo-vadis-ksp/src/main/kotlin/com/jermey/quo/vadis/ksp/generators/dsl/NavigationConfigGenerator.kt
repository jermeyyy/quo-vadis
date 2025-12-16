package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.jermey.quo.vadis.ksp.generators.base.DslCodeGenerator
import com.jermey.quo.vadis.ksp.generators.base.StringTemplates
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Main generator for the unified `GeneratedNavigationConfig.kt` file.
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
 * ## Example Generated Output
 *
 * ```kotlin
 * object GeneratedNavigationConfig : NavigationConfig {
 *
 *     private val config = navigationConfig {
 *         // SCREENS
 *         screen<HomeDestination.Feed> { ... }
 *
 *         // CONTAINERS
 *         tabs<MainTabs>(scopeKey = "MainTabs") { ... }
 *
 *         // SCOPES
 *         scope("MainTabs", MainTabs.Home::class, ...)
 *
 *         // TRANSITIONS
 *         transition<DetailDestination>(NavTransition.SlideHorizontal)
 *
 *         // WRAPPERS
 *         tabWrapper("MainTabs") { ... }
 *     }
 *
 *     override val screenRegistry = config.screenRegistry
 *     // ... other registry delegations
 *
 *     override fun buildNavNode(...) = config.buildNavNode(...)
 *     override fun plus(other) = CompositeNavigationConfig(this, other)
 *
 *     val roots: Set<KClass<out Destination>> = setOf(MainTabs::class)
 * }
 * ```
 *
 * ## Sub-Generators
 *
 * - [ScreenBlockGenerator] - Generates `screen<T>` blocks
 * - [ContainerBlockGenerator] - Generates `tabs`, `stack`, `panes` blocks
 * - [ScopeBlockGenerator] - Generates `scope()` calls
 * - [TransitionBlockGenerator] - Generates `transition<T>` calls
 * - [WrapperBlockGenerator] - Generates `tabWrapper`/`paneWrapper` blocks
 * - [DeepLinkBlockGenerator] - Generates deep link configuration (placeholder)
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
     *
     * This aggregates all the data extracted from annotations by the
     * various extractors, providing a single input to the generator.
     *
     * @property screens List of screen info from @Screen annotations
     * @property stacks List of stack info from @Stack annotations
     * @property tabs List of tab info from @Tab annotations
     * @property panes List of pane info from @Pane annotations
     * @property transitions List of transition info from @Transition annotations
     * @property wrappers List of wrapper info from @TabWrapper/@PaneWrapper annotations
     * @property destinations List of all destination info from @Destination annotations
     */
    data class NavigationData(
        val screens: List<ScreenInfo>,
        val stacks: List<StackInfo>,
        val tabs: List<TabInfo>,
        val panes: List<PaneInfo>,
        val transitions: List<TransitionInfo>,
        val wrappers: List<WrapperInfo>,
        val destinations: List<DestinationInfo>
    ) {
        /**
         * Creates an empty NavigationData instance.
         */
        companion object {
            val EMPTY: NavigationData = NavigationData(
                screens = emptyList(),
                stacks = emptyList(),
                tabs = emptyList(),
                panes = emptyList(),
                transitions = emptyList(),
                wrappers = emptyList(),
                destinations = emptyList()
            )
        }
    }

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
        val totalItems = data.screens.size + data.tabs.size + data.stacks.size +
            data.panes.size + data.transitions.size + data.wrappers.size

        logInfo("Generating NavigationConfig with $totalItems total items:")
        logInfo("  - ${data.screens.size} screens")
        logInfo("  - ${data.tabs.size} tab containers")
        logInfo("  - ${data.stacks.size} stack containers")
        logInfo("  - ${data.panes.size} pane containers")
        logInfo("  - ${data.transitions.size} transitions")
        logInfo("  - ${data.wrappers.size} wrappers")

        val fileSpec = buildFileSpec(data)
        writeFile(fileSpec, originatingFiles)

        logInfo("Successfully generated $GENERATED_FILE_NAME.kt in package $packageName")
    }

    /**
     * Builds the complete FileSpec for GeneratedNavigationConfig.kt.
     */
    private fun buildFileSpec(data: NavigationData): FileSpec {
        // Collect imports from sub-generators
        val dynamicImports = collectDynamicImports(data)

        return createFileBuilder(GENERATED_FILE_NAME, packageName)
            .addImports()
            .addDynamicImports(dynamicImports)
            .addType(buildConfigObject(data))
            .build()
    }

    /**
     * Collects dynamic imports from screen and wrapper functions.
     */
    private fun collectDynamicImports(data: NavigationData): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect screen function imports
        if (data.screens.isNotEmpty()) {
            val screenResult = screenBlockGenerator.generateWithImports(data.screens)
            imports.addAll(screenResult.imports)
        }

        // Collect wrapper function imports
        if (data.wrappers.isNotEmpty()) {
            val wrapperResult = wrapperBlockGenerator.generateWithImports(data.wrappers)
            imports.addAll(wrapperResult.imports)
        }

        return imports
    }

    /**
     * Adds all required imports to the FileSpec.
     */
    private fun FileSpec.Builder.addImports(): FileSpec.Builder {
        // Core navigation imports
        addImport("com.jermey.quo.vadis.core.navigation", "NavigationConfig")
        addImport("com.jermey.quo.vadis.core.navigation.core", "Destination")
        addImport("com.jermey.quo.vadis.core.navigation.core", "NavNode")
        addImport("com.jermey.quo.vadis.core.navigation.dsl", "navigationConfig")
        addImport("com.jermey.quo.vadis.core.navigation.compose.registry",
            "ScreenRegistry", "WrapperRegistry", "ScopeRegistry",
            "TransitionRegistry", "ContainerRegistry")
        addImport("com.jermey.quo.vadis.core.navigation.core", "GeneratedDeepLinkHandler")
        addImport("com.jermey.quo.vadis.core.navigation.compose.animation", "NavTransition")
        addImport("kotlin.reflect", "KClass")

        return this
    }

    /**
     * Adds dynamic imports (screen functions, wrapper functions) to the FileSpec.
     */
    private fun FileSpec.Builder.addDynamicImports(imports: Set<String>): FileSpec.Builder {
        imports.forEach { qualifiedName ->
            val lastDotIndex = qualifiedName.lastIndexOf('.')
            if (lastDotIndex > 0) {
                val packageName = qualifiedName.substring(0, lastDotIndex)
                val simpleName = qualifiedName.substring(lastDotIndex + 1)
                addImport(packageName, simpleName)
            }
        }
        return this
    }

    /**
     * Builds the main GeneratedNavigationConfig object.
     */
    private fun buildConfigObject(data: NavigationData): TypeSpec {
        return TypeSpec.objectBuilder(GENERATED_OBJECT_NAME)
            .addKdoc(StringTemplates.NAVIGATION_CONFIG_KDOC)
            .addSuperinterface(NAVIGATION_CONFIG_CLASS)
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

        return PropertySpec.builder("config", NAVIGATION_CONFIG_CLASS)
            .addModifiers(KModifier.PRIVATE)
            .initializer(
                CodeBlock.builder()
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

        // DEEP LINKS section (placeholder)
        if (deepLinkBlockGenerator.hasDeepLinks(data.destinations)) {
            builder.add("\n")
            builder.add(StringTemplates.DEEP_LINKS_SECTION)
            builder.add("\n\n")
            builder.add(deepLinkBlockGenerator.generate(data.destinations))
        }

        return builder.build()
    }

    /**
     * Collects scope data from containers.
     *
     * Scopes are derived from container definitions:
     * - Tabs: Tab item destinations belong to the tab container's scope
     * - Stacks: Stack destinations belong to the stack's scope
     * - Panes: Pane item destinations belong to the pane container's scope
     *
     * @param data The navigation data
     * @return Map of scope key to list of destination ClassNames
     */
    private fun collectScopeData(data: NavigationData): Map<String, List<ClassName>> {
        val scopes = mutableMapOf<String, MutableList<ClassName>>()

        // Collect from tabs
        data.tabs.forEach { tab ->
            val scopeKey = tab.name.ifEmpty { tab.className }
            val scopeMembers = scopes.getOrPut(scopeKey) { mutableListOf() }

            tab.tabs.forEach { tabItem ->
                // Add the tab item itself
                scopeMembers.add(tabItem.classDeclaration.toClassName())

                // If it's a nested stack, add the stack's destinations too
                tabItem.stackInfo?.destinations?.forEach { dest ->
                    scopeMembers.add(dest.classDeclaration.toClassName())
                }

                // If it's a flat screen with destination info
                tabItem.destinationInfo?.let { destInfo ->
                    scopeMembers.add(destInfo.classDeclaration.toClassName())
                }
            }
        }

        // Collect from stacks
        data.stacks.forEach { stack ->
            val scopeKey = stack.name.ifEmpty { stack.className }
            val scopeMembers = scopes.getOrPut(scopeKey) { mutableListOf() }

            stack.destinations.forEach { dest ->
                scopeMembers.add(dest.classDeclaration.toClassName())
            }
        }

        // Collect from panes
        data.panes.forEach { pane ->
            val scopeKey = pane.name.ifEmpty { pane.className }
            val scopeMembers = scopes.getOrPut(scopeKey) { mutableListOf() }

            pane.panes.forEach { paneItem ->
                scopeMembers.add(paneItem.destination.classDeclaration.toClassName())
            }
        }

        return scopes
    }

    /**
     * Builds the delegation properties for NavigationConfig implementation.
     */
    private fun buildDelegationProperties(): List<PropertySpec> {
        return listOf(
            buildDelegationProperty("screenRegistry", SCREEN_REGISTRY_CLASS),
            buildDelegationProperty("wrapperRegistry", WRAPPER_REGISTRY_CLASS),
            buildDelegationProperty("scopeRegistry", SCOPE_REGISTRY_CLASS),
            buildDelegationProperty("transitionRegistry", TRANSITION_REGISTRY_CLASS),
            buildDelegationProperty("containerRegistry", CONTAINER_REGISTRY_CLASS),
            buildDelegationProperty("deepLinkHandler", DEEP_LINK_HANDLER_CLASS, nullable = true)
        )
    }

    /**
     * Builds a single delegation property.
     */
    private fun buildDelegationProperty(
        name: String,
        type: ClassName,
        nullable: Boolean = false
    ): PropertySpec {
        val propertyType = if (nullable) type.copy(nullable = true) else type
        return PropertySpec.builder(name, propertyType)
            .addModifiers(KModifier.OVERRIDE)
            .initializer("config.$name")
            .build()
    }

    /**
     * Builds the `buildNavNode` function implementation.
     */
    private fun buildBuildNavNodeFunction(): FunSpec {
        val kClassDestination = KCLASS_CLASS.parameterizedBy(
            WildcardTypeName.producerOf(DESTINATION_CLASS)
        )

        return FunSpec.builder("buildNavNode")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destinationClass", kClassDestination)
            .addParameter("key", String::class.asTypeName().copy(nullable = true))
            .addParameter("parentKey", String::class.asTypeName().copy(nullable = true))
            .returns(NAV_NODE_CLASS.copy(nullable = true))
            .addStatement("return config.buildNavNode(destinationClass, key, parentKey)")
            .build()
    }

    /**
     * Builds the `plus` operator function implementation.
     *
     * Delegates to config.plus(other) to leverage the DSL-built config's
     * plus implementation, avoiding direct use of internal CompositeNavigationConfig.
     */
    private fun buildPlusFunction(): FunSpec {
        return FunSpec.builder("plus")
            .addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            .addParameter("other", NAVIGATION_CONFIG_CLASS)
            .returns(NAVIGATION_CONFIG_CLASS)
            .addStatement("return config + other")
            .build()
    }

    /**
     * Builds the `roots` property listing all root containers.
     *
     * Root containers are entry points for navigation:
     * - Tab containers (usually root)
     * - Top-level stack containers
     * - Top-level pane containers
     */
    private fun buildRootsProperty(data: NavigationData): PropertySpec {
        val rootClasses = mutableListOf<ClassName>()

        // Tab containers are typically roots
        data.tabs.forEach { tab ->
            rootClasses.add(tab.classDeclaration.toClassName())
        }

        // Stack containers can be roots
        // Note: In a real scenario, you might want to mark which stacks are "root" stacks
        // For now, we include stacks that are not nested inside tabs
        data.stacks.forEach { stack ->
            // Check if this stack is nested inside a tab
            val isNestedInTab = data.tabs.any { tab ->
                tab.tabs.any { tabItem ->
                    tabItem.stackInfo?.classDeclaration?.qualifiedName?.asString() ==
                        stack.classDeclaration.qualifiedName?.asString()
                }
            }
            if (!isNestedInTab) {
                rootClasses.add(stack.classDeclaration.toClassName())
            }
        }

        // Pane containers can be roots
        data.panes.forEach { pane ->
            rootClasses.add(pane.classDeclaration.toClassName())
        }

        val kClassType = KCLASS_CLASS.parameterizedBy(
            WildcardTypeName.producerOf(DESTINATION_CLASS)
        )
        val setType = ClassName("kotlin.collections", "Set").parameterizedBy(kClassType)

        val initializerBuilder = CodeBlock.builder()

        if (rootClasses.isEmpty()) {
            initializerBuilder.add("emptySet()")
        } else {
            initializerBuilder.add("setOf(\n")
                .indent()

            rootClasses.forEachIndexed { index, className ->
                initializerBuilder.add("%T::class", className)
                if (index < rootClasses.size - 1) {
                    initializerBuilder.add(",")
                }
                initializerBuilder.add("\n")
            }

            initializerBuilder.unindent()
                .add(")")
        }

        return PropertySpec.builder("roots", setType)
            .addKdoc("Root destinations available for navigation.")
            .initializer(initializerBuilder.build())
            .build()
    }

    private companion object {
        // Type references
        val NAVIGATION_CONFIG_CLASS = ClassName("com.jermey.quo.vadis.core.navigation", "NavigationConfig")
        val DESTINATION_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination")
        val NAV_NODE_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.core", "NavNode")
        val KCLASS_CLASS = ClassName("kotlin.reflect", "KClass")

        // Registry type references
        val SCREEN_REGISTRY_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.compose.registry", "ScreenRegistry")
        val WRAPPER_REGISTRY_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.compose.registry", "WrapperRegistry")
        val SCOPE_REGISTRY_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.compose.registry", "ScopeRegistry")
        val TRANSITION_REGISTRY_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.compose.registry", "TransitionRegistry")
        val CONTAINER_REGISTRY_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.compose.registry", "ContainerRegistry")
        val DEEP_LINK_HANDLER_CLASS = ClassName("com.jermey.quo.vadis.core.navigation.core", "GeneratedDeepLinkHandler")
    }
}
