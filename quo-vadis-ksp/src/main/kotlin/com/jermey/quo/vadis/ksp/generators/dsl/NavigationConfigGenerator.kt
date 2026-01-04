@file:Suppress("CanConvertToMultiDollarString")

package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.jermey.quo.vadis.ksp.generators.base.DslCodeGenerator
import com.jermey.quo.vadis.ksp.generators.base.StringTemplates
import com.jermey.quo.vadis.ksp.QuoVadisClassNames
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.ContainerInfoModel
import com.jermey.quo.vadis.ksp.models.ContainerType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Main generator for the unified `GeneratedNavigationConfig.kt` file.
 *
 * This generator orchestrates multiple sub-generators to produce a single
 * file that implements [com.jermey.quo.vadis.core.navigation.config.NavigationConfig] using the DSL-based approach.
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
 *         tabsContainer("MainTabs") { ... }
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
 * @param modulePrefix Optional prefix for generated class names (e.g., "ComposeApp" -> "ComposeAppNavigationConfig")
 */
class NavigationConfigGenerator(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    private val packageName: String,
    private val modulePrefix: String = ""
) : DslCodeGenerator(codeGenerator, logger) {

    override val generatedPackage: String = packageName

    // Dynamic names based on modulePrefix
    private val generatedFileName: String = "${modulePrefix}NavigationConfig"
    private val generatedObjectName: String = "${modulePrefix}NavigationConfig"

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
     * @property wrappers List of wrapper info from @TabsContainer/@PaneContainer annotations
     * @property destinations List of all destination info from @Destination annotations
     */
    data class NavigationData(
        val screens: List<ScreenInfo>,
        val stacks: List<StackInfo>,
        val tabs: List<TabInfo>,
        val panes: List<PaneInfo>,
        val transitions: List<TransitionInfo>,
        val wrappers: List<ContainerInfoModel>,
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

        logInfo("Successfully generated $generatedFileName.kt in package $packageName")
    }

    /**
     * Builds the complete FileSpec for GeneratedNavigationConfig.kt.
     */
    private fun buildFileSpec(data: NavigationData): FileSpec {
        // Collect imports from sub-generators
        val dynamicImports = collectDynamicImports(data)

        return createFileBuilder(generatedFileName, packageName)
            .addImports()
            .addDynamicImports(dynamicImports)
            .addType(buildConfigObject(data))
            .build()
    }

    /**
     * Collects dynamic imports from screen and wrapper functions.
     * Also collects destination class imports for screen registry when-dispatch.
     */
    private fun collectDynamicImports(data: NavigationData): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect screen function imports
        if (data.screens.isNotEmpty()) {
            val screenResult = screenBlockGenerator.generateWithImports(data.screens)
            imports.addAll(screenResult.imports)

            // Also collect destination class imports for when-dispatch
            data.screens.forEach { screen ->
                val destQualifiedName = screen.destinationClass.qualifiedName?.asString()
                if (destQualifiedName != null) {
                    // For nested classes, we need to import the top-level class
                    val topLevelClass = getTopLevelClassName(screen.destinationClass)
                    imports.add(topLevelClass)
                }
            }
        }

        // Collect wrapper function imports
        if (data.wrappers.isNotEmpty()) {
            val wrapperResult = wrapperBlockGenerator.generateWithImports(data.wrappers)
            imports.addAll(wrapperResult.imports)
        }

        return imports
    }

    /**
     * Gets the qualified name of the top-level containing class for a potentially nested class.
     * For `com.example.MainTabs.HomeTab`, returns `com.example.MainTabs`.
     * For `com.example.HomeScreen`, returns `com.example.HomeScreen`.
     */
    private fun getTopLevelClassName(classDeclaration: KSClassDeclaration): String {
        var current: KSDeclaration = classDeclaration
        while (current.parentDeclaration is KSClassDeclaration) {
            current = current.parentDeclaration!!
        }
        return current.qualifiedName?.asString() ?: current.simpleName.asString()
    }

    /**
     * Adds all required imports to the FileSpec.
     * 
     * Uses QuoVadisClassNames where possible to ensure correct package paths.
     * Some imports (navigationConfig function, CompositeNavigationConfig class) 
     * cannot be expressed as ClassNames and use direct string imports.
     */
    private fun FileSpec.Builder.addImports(): FileSpec.Builder {
        // Core navigation imports (explicit strings for non-ClassName references)
        addImport("com.jermey.quo.vadis.core.navigation.internal.config", "CompositeNavigationConfig")
        addImport("com.jermey.quo.vadis.core.dsl", "navigationConfig")
        addImport("kotlin.reflect", "KClass")

        // Registry imports
//        addImport("com.jermey.quo.vadis.core.dsl.registry.", "TransitionRegistry")
        addImport("com.jermey.quo.vadis.core.registry", "DeepLinkRegistry")
        addImport("com.jermey.quo.vadis.core.registry", "PaneRoleRegistry")

        // Compose animation imports (for screen registry)
        addImport("androidx.compose.animation", "SharedTransitionScope", "AnimatedVisibilityScope")
        addImport("androidx.compose.runtime", "Composable")

        // Pane imports
        addImport("com.jermey.quo.vadis.core.navigation.pane", "PaneBackBehavior")

        // Transition imports
        addImport("com.jermey.quo.vadis.core.compose.animation", "NavTransition")

        return this
    }

    /**
     * Adds dynamic imports (screen functions, wrapper functions) to the FileSpec.
     */
    private fun FileSpec.Builder.addDynamicImports(imports: Set<String>): FileSpec.Builder {
        imports.forEach { qualifiedName ->
            val lastDotIndex = qualifiedName.lastIndexOf('.')
            if (lastDotIndex > 0) {
                val packageName = qualifiedName.take(lastDotIndex)
                val simpleName = qualifiedName.substring(lastDotIndex + 1)
                addImport(packageName, simpleName)
            }
        }
        return this
    }

    /**
     * Builds the main GeneratedNavigationConfig object.
     *
     * Uses a hybrid approach:
     * - DSL for containers, scopes, transitions (no composable lambdas)
     * - Anonymous object implementations for screenRegistry and containerRegistry
     *   with when-based dispatch to avoid Compose lambda casting issues
     * - Direct reference to generated DeepLinkHandler object
     */
    private fun buildConfigObject(data: NavigationData): TypeSpec {
        val hasRoutes = data.destinations.any { !it.route.isNullOrBlank() }
        val paneRoleData = collectPaneRoleData(data)
        
        return TypeSpec.objectBuilder(generatedObjectName)
            .addKdoc(StringTemplates.NAVIGATION_CONFIG_KDOC)
            .addSuperinterface(QuoVadisClassNames.NAVIGATION_CONFIG)
            .addProperty(buildBaseConfigProperty(data))
            .addProperty(buildScreenRegistryProperty(data.screens))
            .addProperty(buildContainerRegistryProperty(data.wrappers, data.tabs, data.panes))
            .addProperties(buildDelegationProperties(data))
            .addProperty(buildDeepLinkRegistryProperty(hasRoutes))
            .addProperty(buildPaneRoleRegistryProperty(paneRoleData))
            .addFunction(buildBuildNavNodeFunction())
            .addFunction(buildPlusFunction())
            .addProperty(buildRootsProperty(data))
            .build()
    }

    /**
     * Builds the private `baseConfig` property with the DSL configuration.
     *
     * Uses `by lazy` initialization to defer the DSL evaluation until first access.
     * This config only contains non-composable registrations (containers, scopes, transitions).
     * Screen and wrapper registrations are handled separately with when-based dispatch
     * to avoid Compose compiler issues with lambda casting in object initialization.
     */
    private fun buildBaseConfigProperty(data: NavigationData): PropertySpec {
        val dslContent = buildDslContent(data)

        return PropertySpec.builder("baseConfig", QuoVadisClassNames.NAVIGATION_CONFIG)
            .addModifiers(KModifier.PRIVATE)
            .delegate(
                CodeBlock.builder()
                    .beginControlFlow("lazy")
                    .beginControlFlow("navigationConfig")
                    .add(dslContent)
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            )
            .build()
    }

    /**
     * Builds the complete DSL content inside navigationConfig { }.
     *
     * NOTE: This excludes screens and wrappers which are generated as separate
     * anonymous object implementations to avoid Compose lambda casting issues.
     */
    private fun buildDslContent(data: NavigationData): CodeBlock {
        val builder = CodeBlock.builder()

        // CONTAINERS section (no composable lambdas)
        val hasContainers =
            data.tabs.isNotEmpty() || data.stacks.isNotEmpty() || data.panes.isNotEmpty()
        if (hasContainers) {
            builder.add("\n")
            builder.add(StringTemplates.CONTAINERS_SECTION)
            builder.add("\n\n")
            builder.add(containerBlockGenerator.generate(data.tabs, data.stacks, data.panes))
        }

        // SCOPES section (no composable lambdas)
        val scopeData = collectScopeData(data)
        if (scopeData.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.SCOPES_SECTION)
            builder.add("\n\n")
            builder.add(scopeBlockGenerator.generate(scopeData))
        }

        // TRANSITIONS section (no composable lambdas)
        if (data.transitions.isNotEmpty()) {
            builder.add("\n")
            builder.add(StringTemplates.TRANSITIONS_SECTION)
            builder.add("\n\n")
            builder.add(transitionBlockGenerator.generate(data.transitions))
        }

        // DEEP LINKS section (no composable lambdas)
        if (deepLinkBlockGenerator.hasDeepLinks(data.destinations)) {
            builder.add("\n")
            builder.add(StringTemplates.DEEP_LINKS_SECTION)
            builder.add("\n\n")
            builder.add(deepLinkBlockGenerator.generate(data.destinations))
        }

        // NOTE: Screens and wrappers are NOT included in DSL to avoid
        // Compose compiler lambda casting issues. They are generated as
        // separate anonymous object implementations with when-based dispatch.

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

        // Collect from panes - include ALL destinations, not just @PaneItem roots
        data.panes.forEach { pane ->
            val scopeKey = pane.name.ifEmpty { pane.className }
            val scopeMembers = scopes.getOrPut(scopeKey) { mutableListOf() }

            // Add all destinations from the pane's sealed class (including ConversationDetail, etc.)
            pane.allDestinations.forEach { dest ->
                scopeMembers.add(dest.classDeclaration.toClassName())
            }
        }

        return scopes
    }

    /**
     * NOTE: screenRegistry is NOT delegated here because it is implemented as
     * a custom anonymous object with when-based dispatch. containerRegistry is
     * also implemented with when-based dispatch to handle wrapper composables.
     * deepLinkRegistry is NOT delegated because it references the generated handler
     * object directly instead of baseConfig (which would be null from DSL).
     * Only non-composable registries are delegated to baseConfig.
     * 
     * Transition registry is only delegated if there are actual transitions defined,
     * otherwise we provide TransitionRegistry.Empty as default implementation.
     */
    private fun buildDelegationProperties(data: NavigationData): List<PropertySpec> {
        val properties = mutableListOf<PropertySpec>()
        
        // Always delegate scope registry
        properties.add(buildDelegationProperty("scopeRegistry", QuoVadisClassNames.SCOPE_REGISTRY))
        
        // Handle transition registry - delegate if transitions exist, otherwise provide empty default
        if (data.transitions.isNotEmpty()) {
            properties.add(buildDelegationProperty("transitionRegistry", QuoVadisClassNames.TRANSITION_REGISTRY))
        } else {
            // Provide TransitionRegistry.Empty as default when no transitions are defined
            properties.add(
                PropertySpec.builder("transitionRegistry", QuoVadisClassNames.TRANSITION_REGISTRY)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%T.Empty", QuoVadisClassNames.TRANSITION_REGISTRY)
                    .build()
            )
        }
        
        return properties
    }

    /**
     * Builds the deepLinkRegistry property that references the generated registry.
     *
     * Unlike other properties, deepLinkRegistry cannot be delegated to baseConfig
     * because DslNavigationConfig always returns DeepLinkRegistry.Empty.
     * Instead, we reference the generated ${modulePrefix}DeepLinkHandler object directly,
     * or DeepLinkRegistry.Empty if no routes are defined.
     *
     * @param hasRoutes Whether any destinations have routes defined
     * @return PropertySpec for deepLinkRegistry
     */
    private fun buildDeepLinkRegistryProperty(hasRoutes: Boolean): PropertySpec {
        val propertyType = QuoVadisClassNames.DEEP_LINK_REGISTRY
        val handlerName = "${modulePrefix}DeepLinkHandler"

        return PropertySpec.builder("deepLinkRegistry", propertyType)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(if (hasRoutes) handlerName else "DeepLinkRegistry.Empty")
            .build()
    }

    /**
     * Collects pane role data from pane containers.
     *
     * Maps scope keys to a list of (destination class, pane role) pairs.
     * This includes:
     * - @PaneItem annotated destinations (root destinations for each pane)
     * - @Destination(paneRole = ...) annotated destinations (non-root pane members)
     *
     * @param data The navigation data
     * @return Map of scope key to list of (ClassName, PaneRole) pairs
     */
    private fun collectPaneRoleData(
        data: NavigationData
    ): Map<String, List<Pair<ClassName, com.jermey.quo.vadis.ksp.models.PaneRole>>> {
        val result = mutableMapOf<String, MutableList<Pair<ClassName, com.jermey.quo.vadis.ksp.models.PaneRole>>>()

        data.panes.forEach { pane ->
            val scopeKey = pane.name.ifEmpty { pane.className }
            val rolesList = result.getOrPut(scopeKey) { mutableListOf() }

            // Add @PaneItem root destinations
            pane.panes.forEach { paneItem ->
                rolesList.add(paneItem.destination.classDeclaration.toClassName() to paneItem.role)
            }

            // Add @Destination(paneRole = ...) non-root destinations
            pane.allDestinations.forEach { dest ->
                if (dest.paneRole != null) {
                    rolesList.add(dest.classDeclaration.toClassName() to dest.paneRole)
                }
            }
        }

        return result
    }

    /**
     * Builds the paneRoleRegistry property.
     *
     * Generates an object implementing PaneRoleRegistry with when-based dispatch
     * to map destinations to their pane roles.
     */
    private fun buildPaneRoleRegistryProperty(
        paneRoleData: Map<String, List<Pair<ClassName, com.jermey.quo.vadis.ksp.models.PaneRole>>>
    ): PropertySpec {
        if (paneRoleData.isEmpty() || paneRoleData.all { it.value.isEmpty() }) {
            return PropertySpec.builder("paneRoleRegistry", QuoVadisClassNames.PANE_ROLE_REGISTRY)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("PaneRoleRegistry.Empty")
                .build()
        }

        val kClassDestination = QuoVadisClassNames.KCLASS.parameterizedBy(
            WildcardTypeName.producerOf(QuoVadisClassNames.NAV_DESTINATION)
        )

        // Build the getPaneRole(scopeKey, destination) function
        val getByInstanceBuilder = FunSpec.builder("getPaneRole")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("scopeKey", STRING)
            .addParameter("destination", QuoVadisClassNames.NAV_DESTINATION)
            .returns(QuoVadisClassNames.PANE_ROLE.copy(nullable = true))
            .addStatement("return getPaneRole(scopeKey, destination::class)")

        // Build the getPaneRole(scopeKey, destinationClass) function
        val getByClassBuilder = FunSpec.builder("getPaneRole")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("scopeKey", STRING)
            .addParameter("destinationClass", kClassDestination)
            .returns(QuoVadisClassNames.PANE_ROLE.copy(nullable = true))
            .beginControlFlow("return when (scopeKey)")

        paneRoleData.forEach { (scopeKey, rolesList) ->
            if (rolesList.isNotEmpty()) {
                getByClassBuilder.beginControlFlow("%S ->", scopeKey)
                getByClassBuilder.beginControlFlow("when (destinationClass)")
                rolesList.forEach { (destClass, role) ->
                    val roleName = when (role) {
                        com.jermey.quo.vadis.ksp.models.PaneRole.PRIMARY -> "Primary"
                        com.jermey.quo.vadis.ksp.models.PaneRole.SECONDARY -> "Supporting"
                        com.jermey.quo.vadis.ksp.models.PaneRole.EXTRA -> "Extra"
                    }
                    getByClassBuilder.addStatement("%T::class -> PaneRole.%L", destClass, roleName)
                }
                getByClassBuilder.addStatement("else -> null")
                getByClassBuilder.endControlFlow() // when destinationClass
                getByClassBuilder.endControlFlow() // scopeKey case
            }
        }

        getByClassBuilder.addStatement("else -> null")
        getByClassBuilder.endControlFlow() // when scopeKey

        val registryObject = TypeSpec.anonymousClassBuilder()
            .addSuperinterface(QuoVadisClassNames.PANE_ROLE_REGISTRY)
            .addFunction(getByInstanceBuilder.build())
            .addFunction(getByClassBuilder.build())
            .build()

        return PropertySpec.builder("paneRoleRegistry", QuoVadisClassNames.PANE_ROLE_REGISTRY)
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%L", registryObject)
            .build()
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
            .initializer("baseConfig.$name")
            .build()
    }

    /**
     * Builds the `buildNavNode` function implementation.
     */
    private fun buildBuildNavNodeFunction(): FunSpec {
        val kClassDestination = QuoVadisClassNames.KCLASS.parameterizedBy(
            WildcardTypeName.producerOf(QuoVadisClassNames.NAV_DESTINATION)
        )

        return FunSpec.builder("buildNavNode")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destinationClass", kClassDestination)
            .addParameter("key", String::class.asTypeName().copy(nullable = true))
            .addParameter("parentKey", String::class.asTypeName().copy(nullable = true))
            .returns(QuoVadisClassNames.NAV_NODE.copy(nullable = true))
            .addStatement("return baseConfig.buildNavNode(destinationClass, key, parentKey)")
            .build()
    }

    /**
     * Builds the `plus` operator function implementation.
     *
     * Creates a CompositeNavigationConfig with `this` as primary and `other` as secondary.
     * This ensures that the generated config's screenRegistry (which contains the actual
     * screen mappings) is used in the composite, not just baseConfig's empty registry.
     */
    private fun buildPlusFunction(): FunSpec {
        return FunSpec.builder("plus")
            .addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            .addParameter("other", QuoVadisClassNames.NAVIGATION_CONFIG)
            .returns(QuoVadisClassNames.NAVIGATION_CONFIG)
            .addStatement("return CompositeNavigationConfig(this, other)")
            .build()
    }

    // ================================
    // Screen Registry Generation
    // ================================

    /**
     * Builds the screenRegistry property with an anonymous object implementation.
     *
     * Uses when-based dispatch instead of DSL lambdas to avoid Compose compiler
     * issues with lambda casting in object initialization context.
     */
    private fun buildScreenRegistryProperty(screens: List<ScreenInfo>): PropertySpec {
        return PropertySpec.builder("screenRegistry", QuoVadisClassNames.SCREEN_REGISTRY)
            .addModifiers(KModifier.OVERRIDE)
            .initializer("%L", buildScreenRegistryObject(screens))
            .build()
    }

    /**
     * Builds the anonymous ScreenRegistry object with when-based dispatch.
     */
    private fun buildScreenRegistryObject(screens: List<ScreenInfo>): TypeSpec {
        return TypeSpec.anonymousClassBuilder()
            .addSuperinterface(QuoVadisClassNames.SCREEN_REGISTRY)
            .addFunction(buildScreenContentFunction(screens))
            .addFunction(buildScreenHasContentFunction(screens))
            .build()
    }

    /**
     * Builds the Content() composable function with when-based dispatch.
     */
    private fun buildScreenContentFunction(screens: List<ScreenInfo>): FunSpec {
        return FunSpec.builder("Content")
            .addAnnotation(QuoVadisClassNames.COMPOSABLE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destination", QuoVadisClassNames.NAV_DESTINATION)
            .addParameter(
                ParameterSpec.builder(
                    "sharedTransitionScope",
                    QuoVadisClassNames.SHARED_TRANSITION_SCOPE.copy(nullable = true)
                )
                    .build()
            )
            .addParameter(
                ParameterSpec.builder(
                    "animatedVisibilityScope",
                    QuoVadisClassNames.ANIMATED_VISIBILITY_SCOPE.copy(nullable = true)
                )
                    .build()
            )
            .apply {
                if (screens.isEmpty()) {
                    addStatement("error(%P)", "No screen registered for destination: \$destination")
                } else {
                    beginControlFlow("when (destination)")
                    screens.forEach { screen ->
                        val destClassName = buildDestinationClassName(screen.destinationClass)
                        val functionCall = buildScreenFunctionCall(screen)
                        addStatement("is %L -> %L", destClassName, functionCall)
                    }
                    addStatement(
                        "else -> error(%P)",
                        "No screen registered for destination: \$destination"
                    )
                    endControlFlow()
                }
            }
            .build()
    }

    /**
     * Builds the hasContent() function with when-based check.
     */
    private fun buildScreenHasContentFunction(screens: List<ScreenInfo>): FunSpec {
        return FunSpec.builder("hasContent")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destination", QuoVadisClassNames.NAV_DESTINATION)
            .returns(Boolean::class)
            .apply {
                if (screens.isEmpty()) {
                    addStatement("return false")
                } else {
                    beginControlFlow("return when (destination)")
                    val patterns = screens.map { buildDestinationClassName(it.destinationClass) }
                    val patternList = patterns.joinToString(",\n\t\t") { "is $it" }
                    addStatement("$patternList -> true")
                    addStatement("else -> false")
                    endControlFlow()
                }
            }
            .build()
    }

    /**
     * Builds the full nested class name from a KSClassDeclaration.
     * e.g., MainTabs.SettingsTab.SettingsMain
     */
    private fun buildDestinationClassName(destClass: KSClassDeclaration): String {
        val names = mutableListOf<String>()
        var current: KSDeclaration? = destClass
        while (current is KSClassDeclaration) {
            names.add(0, current.simpleName.asString())
            current = current.parentDeclaration
        }
        return names.joinToString(".")
    }

    /**
     * Builds the screen composable function call with appropriate parameters.
     * Uses smart cast for destination parameter (no explicit cast needed after when-is check).
     */
    private fun buildScreenFunctionCall(screen: ScreenInfo): String {
        val funcName = screen.functionName
        val args = mutableListOf<String>()

        // Add destination parameter if needed (smart cast from when-is check)
        if (screen.hasDestinationParam) {
            args.add("destination = destination")
        }

        // Add shared transition scopes if needed
        if (screen.hasSharedTransitionScope) {
            args.add("sharedTransitionScope = sharedTransitionScope!!")
        }
        if (screen.hasAnimatedVisibilityScope) {
            args.add("animatedVisibilityScope = animatedVisibilityScope!!")
        }

        return "$funcName(${args.joinToString(", ")})"
    }

    // ================================
    // Container Registry Generation
    // ================================

    /**
     * Builds the containerRegistry property with an anonymous object implementation.
     *
     * The merged ContainerRegistry handles both:
     * 1. Container building - getContainerInfo() delegated to baseConfig
     * 2. Wrapper rendering - TabsContainer/PaneContainer with when-based dispatch
     *
     * Uses when-based dispatch for wrapper methods to avoid Compose compiler
     * issues with lambda casting in object initialization context.
     *
     * @param wrappers All wrapper info from annotations
     * @param tabs Tab container info (to resolve scopeKeys for tab wrappers)
     * @param panes Pane container info (to resolve scopeKeys for pane wrappers)
     */
    private fun buildContainerRegistryProperty(
        wrappers: List<ContainerInfoModel>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ): PropertySpec {
        val tabWrappers = wrappers.filter { it.containerType == ContainerType.TAB }
        val paneWrappers = wrappers.filter { it.containerType == ContainerType.PANE }

        // Build lookup maps from container target class qualified name to scopeKey
        val tabScopeKeyMap = buildTabScopeKeyMap(tabs)
        val paneScopeKeyMap = buildPaneScopeKeyMap(panes)

        return PropertySpec.builder("containerRegistry", QuoVadisClassNames.CONTAINER_REGISTRY)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                "%L", buildContainerRegistryObject(
                    tabWrappers, paneWrappers, tabScopeKeyMap, paneScopeKeyMap
                )
            )
            .build()
    }

    /**
     * Builds a map from tab container qualified class name to its scopeKey.
     *
     * This allows wrappers to use the same key that the container uses at runtime.
     */
    private fun buildTabScopeKeyMap(tabs: List<TabInfo>): Map<String, String> {
        return tabs.associate { tab ->
            val qualifiedName = tab.classDeclaration.qualifiedName?.asString() ?: tab.className
            val scopeKey = tab.name.ifEmpty { tab.className }
            qualifiedName to scopeKey
        }
    }

    /**
     * Builds a map from pane container qualified class name to its scopeKey.
     *
     * This allows wrappers to use the same key that the container uses at runtime.
     */
    private fun buildPaneScopeKeyMap(panes: List<PaneInfo>): Map<String, String> {
        return panes.associate { pane ->
            val qualifiedName = pane.classDeclaration.qualifiedName?.asString() ?: pane.className
            val scopeKey = pane.name.ifEmpty { pane.className }
            qualifiedName to scopeKey
        }
    }

    /**
     * Gets the wrapper key to use for a wrapper.
     *
     * Uses the container's scopeKey if the wrapper target class matches a container,
     * otherwise falls back to the target class qualified name.
     *
     * Handles the special case where the wrapper targets a Companion object (e.g.,
     * `@TabsContainer(DemoTabs.Companion::class)`) by also checking for the parent
     * class in the scope key map.
     *
     * @param wrapper The wrapper info
     * @param scopeKeyMap Map from qualified class name to scopeKey
     * @return The key to use for wrapper lookup
     */
    private fun getWrapperKey(wrapper: ContainerInfoModel, scopeKeyMap: Map<String, String>): String {
        // Direct match
        scopeKeyMap[wrapper.targetClassQualifiedName]?.let { return it }

        // For Companion objects, try the parent class
        if (wrapper.targetClassSimpleName == "Companion" ||
            wrapper.targetClassQualifiedName.endsWith(".Companion")
        ) {
            val parentQualifiedName = wrapper.targetClassQualifiedName.removeSuffix(".Companion")
            scopeKeyMap[parentQualifiedName]?.let { return it }
        }

        // Fallback to qualified name
        return wrapper.targetClassQualifiedName
    }

    /**
     * Builds the anonymous ContainerRegistry object with when-based dispatch for wrappers.
     *
     * The merged ContainerRegistry:
     * - Delegates getContainerInfo() to baseConfig.containerRegistry
     * - Implements TabsContainer/PaneContainer with when-based dispatch
     * - Provides hasTabsContainer/hasPaneContainer based on registered keys
     *
     * @param tabWrappers Tab wrapper info
     * @param paneWrappers Pane wrapper info
     * @param tabScopeKeyMap Map from tab class qualified name to scopeKey
     * @param paneScopeKeyMap Map from pane class qualified name to scopeKey
     */
    private fun buildContainerRegistryObject(
        tabWrappers: List<ContainerInfoModel>,
        paneWrappers: List<ContainerInfoModel>,
        tabScopeKeyMap: Map<String, String>,
        paneScopeKeyMap: Map<String, String>
    ): TypeSpec {
        return TypeSpec.anonymousClassBuilder()
            .addSuperinterface(QuoVadisClassNames.CONTAINER_REGISTRY)
            .addProperty(buildWrapperKeysProperty("tabsContainerKeys", tabWrappers, tabScopeKeyMap))
            .addProperty(
                buildWrapperKeysProperty(
                    "paneContainerKeys",
                    paneWrappers,
                    paneScopeKeyMap
                )
            )
            .addFunction(buildGetContainerInfoFunction())
            .addFunction(buildTabsContainerFunction(tabWrappers, tabScopeKeyMap))
            .addFunction(buildPaneContainerFunction(paneWrappers, paneScopeKeyMap))
            .addFunction(buildHasTabsContainerFunction())
            .addFunction(buildHasPaneContainerFunction())
            .build()
    }

    /**
     * Builds the getContainerInfo function that delegates to baseConfig.
     */
    private fun buildGetContainerInfoFunction(): FunSpec {
        return FunSpec.builder("getContainerInfo")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destination", QuoVadisClassNames.NAV_DESTINATION)
            .returns(
                QuoVadisClassNames.CONTAINER_INFO.copy(nullable = true)
            )
            .addStatement("return baseConfig.containerRegistry.getContainerInfo(destination)")
            .build()
    }

    /**
     * Builds a private Set<String> property for wrapper keys.
     *
     * @param name Property name (e.g., "tabsContainerKeys")
     * @param wrappers Wrapper info list
     * @param scopeKeyMap Map from qualified class name to scopeKey
     */
    private fun buildWrapperKeysProperty(
        name: String,
        wrappers: List<ContainerInfoModel>,
        scopeKeyMap: Map<String, String>
    ): PropertySpec {
        val keys = wrappers.map { getWrapperKey(it, scopeKeyMap) }
        val setType = SET.parameterizedBy(STRING)
        val initializer = if (keys.isEmpty()) {
            CodeBlock.of("emptySet()")
        } else {
            CodeBlock.of(
                "setOf(%L)",
                keys.joinToString(", ") { "\"$it\"" }
            )
        }

        return PropertySpec.builder(name, setType)
            .addModifiers(KModifier.PRIVATE)
            .initializer(initializer)
            .build()
    }

    /**
     * Builds the TabsContainer composable function with when-based dispatch.
     *
     * @param tabWrappers Tab wrapper info list
     * @param scopeKeyMap Map from qualified class name to scopeKey
     */
    private fun buildTabsContainerFunction(
        tabWrappers: List<ContainerInfoModel>,
        scopeKeyMap: Map<String, String>
    ): FunSpec {
        val contentLambdaType = LambdaTypeName.get(returnType = UNIT)
            .copy(annotations = listOf(AnnotationSpec.builder(QuoVadisClassNames.COMPOSABLE).build()))

        return FunSpec.builder("TabsContainer")
            .addAnnotation(QuoVadisClassNames.COMPOSABLE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("tabNodeKey", STRING)
            .addParameter("scope", QuoVadisClassNames.TABS_CONTAINER_SCOPE)
            .addParameter(ParameterSpec.builder("content", contentLambdaType).build())
            .apply {
                if (tabWrappers.isEmpty()) {
                    addStatement("content()")
                } else {
                    beginControlFlow("when (tabNodeKey)")
                    tabWrappers.forEach { wrapper ->
                        val wrapperKey = getWrapperKey(wrapper, scopeKeyMap)
                        addStatement(
                            "%S -> %M(scope = scope, content = content)",
                            wrapperKey,
                            MemberName(wrapper.packageName, wrapper.functionName)
                        )
                    }
                    addStatement("else -> content()")
                    endControlFlow()
                }
            }
            .build()
    }

    /**
     * Builds the PaneContainer composable function with when-based dispatch.
     *
     * @param paneWrappers Pane container info list
     * @param scopeKeyMap Map from qualified class name to scopeKey
     */
    private fun buildPaneContainerFunction(
        paneWrappers: List<ContainerInfoModel>,
        scopeKeyMap: Map<String, String>
    ): FunSpec {
        val contentLambdaType = LambdaTypeName.get(returnType = UNIT)
            .copy(annotations = listOf(AnnotationSpec.builder(QuoVadisClassNames.COMPOSABLE).build()))

        return FunSpec.builder("PaneContainer")
            .addAnnotation(QuoVadisClassNames.COMPOSABLE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("paneNodeKey", STRING)
            .addParameter("scope", QuoVadisClassNames.PANE_CONTAINER_SCOPE)
            .addParameter(ParameterSpec.builder("content", contentLambdaType).build())
            .apply {
                if (paneWrappers.isEmpty()) {
                    addStatement("content()")
                } else {
                    beginControlFlow("when (paneNodeKey)")
                    paneWrappers.forEach { wrapper ->
                        val wrapperKey = getWrapperKey(wrapper, scopeKeyMap)
                        addStatement(
                            "%S -> %M(scope = scope, content = content)",
                            wrapperKey,
                            MemberName(wrapper.packageName, wrapper.functionName)
                        )
                    }
                    addStatement("else -> content()")
                    endControlFlow()
                }
            }
            .build()
    }

    /**
     * Builds the hasTabsContainer function.
     */
    private fun buildHasTabsContainerFunction(): FunSpec {
        return FunSpec.builder("hasTabsContainer")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("tabNodeKey", STRING)
            .returns(Boolean::class)
            .addStatement("return tabNodeKey in tabsContainerKeys")
            .build()
    }

    /**
     * Builds the hasPaneContainer function.
     */
    private fun buildHasPaneContainerFunction(): FunSpec {
        return FunSpec.builder("hasPaneContainer")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("paneNodeKey", STRING)
            .returns(Boolean::class)
            .addStatement("return paneNodeKey in paneContainerKeys")
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

        val kClassType = QuoVadisClassNames.KCLASS.parameterizedBy(
            WildcardTypeName.producerOf(QuoVadisClassNames.NAV_DESTINATION)
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
        // All type references are now in QuoVadisClassNames for type-safe refactoring
    }
}
