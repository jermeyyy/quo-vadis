package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Pane
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.ksp.extractors.DestinationExtractor
import com.jermey.quo.vadis.ksp.extractors.PaneExtractor
import com.jermey.quo.vadis.ksp.extractors.ScreenExtractor
import com.jermey.quo.vadis.ksp.extractors.StackExtractor
import com.jermey.quo.vadis.ksp.extractors.TabExtractor
import com.jermey.quo.vadis.ksp.extractors.TransitionExtractor
import com.jermey.quo.vadis.ksp.extractors.WrapperExtractor
import com.jermey.quo.vadis.ksp.generators.ContainerRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.DeepLinkHandlerGenerator
import com.jermey.quo.vadis.ksp.generators.NavNodeBuilderGenerator
import com.jermey.quo.vadis.ksp.generators.NavigatorExtGenerator
import com.jermey.quo.vadis.ksp.generators.ScopeRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.ScreenRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.TransitionRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.WrapperRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.dsl.NavigationConfigGenerator
import com.jermey.quo.vadis.ksp.generators.legacy.LegacyGenerators
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.jermey.quo.vadis.ksp.models.WrapperType
import com.jermey.quo.vadis.ksp.validation.ValidationEngine

/**
 * KSP processor for Quo Vadis navigation annotations.
 *
 * Processes the new NavNode-based architecture annotations:
 * - @Stack - generates StackNode builder functions with start destination
 * - @Tab - generates TabNode builder functions referencing stack builders
 * - @Pane - generates PaneNode builder functions with pane configurations
 * - @Destination - generates deep link handler for URI pattern matching
 * - @Screen - generates screen registry for composable resolution
 *
 * Also generates:
 * - Navigator extension functions for type-safe navigation
 * - Validation of annotation usage and relationships
 *
 * ## Generation Modes
 *
 * Controlled via KSP options:
 * - `quoVadis.mode=dsl` - Generate only DSL config (GeneratedNavigationConfig)
 * - `quoVadis.mode=legacy` - Generate only legacy registries
 * - `quoVadis.mode=both` - Generate both (default during transition)
 *
 * ## KSP Options
 *
 * - `quoVadis.mode` - "dsl", "legacy", or "both" (default: "both")
 * - `quoVadis.package` - Target package for generated code
 * - `quoVadis.strictValidation` - Whether validation errors abort generation (default: true)
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    // =========================================================================
    // Configuration
    // =========================================================================

    /**
     * Generation mode controls which outputs are produced.
     */
    enum class GenerationMode {
        /** Generate only GeneratedNavigationConfig (new DSL-based approach) */
        DSL,
        /** Generate only legacy registries (individual registry objects) */
        LEGACY,
        /** Generate both DSL config and legacy registries (default for transition) */
        BOTH
    }

    private val generationMode: GenerationMode = parseGenerationMode()
    private val targetPackage: String = options["quoVadis.package"]
        ?: "com.example.navigation.generated"
    private val strictValidation: Boolean = options["quoVadis.strictValidation"]
        ?.toBooleanStrictOrNull() ?: true

    // =========================================================================
    // Extractors for new NavNode architecture (KSP-001)
    // =========================================================================

    private val destinationExtractor = DestinationExtractor(logger)
    private val stackExtractor = StackExtractor(destinationExtractor, logger)
    private val tabExtractor = TabExtractor(destinationExtractor, logger, stackExtractor)
    private val paneExtractor = PaneExtractor(destinationExtractor, logger)
    private val screenExtractor = ScreenExtractor(logger)
    private val wrapperExtractor = WrapperExtractor(logger)
    private val transitionExtractor = TransitionExtractor(logger)

    // =========================================================================
    // Legacy Generators (for LEGACY and BOTH modes)
    // =========================================================================

    // Generator for NavNode builders (KSP-002)
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)

    // Generator for screen registry (KSP-003)
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)

    // Generator for wrapper registry (HIER-015)
    private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)

    // Generator for transition registry (HIER-015)
    private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)

    // Generator for scope registry (scoped navigation)
    private val scopeRegistryGenerator = ScopeRegistryGenerator(codeGenerator, logger)

    // Generator for container registry (container-aware navigation)
    private val containerRegistryGenerator = ContainerRegistryGenerator(codeGenerator, logger)

    // Generator for deep link handler (KSP-004)
    private val deepLinkHandlerGenerator = DeepLinkHandlerGenerator(codeGenerator, logger)

    // Generator for navigator extensions (KSP-005)
    private val navigatorExtGenerator = NavigatorExtGenerator(codeGenerator, logger)

    // =========================================================================
    // New DSL Generator (for DSL and BOTH modes)
    // =========================================================================

    private val navigationConfigGenerator = NavigationConfigGenerator(
        codeGenerator = codeGenerator,
        logger = logger,
        packageName = targetPackage
    )

    // Legacy generators wrapper (for LEGACY and BOTH modes)
    private val legacyGenerators = LegacyGenerators(
        codeGenerator = codeGenerator,
        logger = logger
    )

    // =========================================================================
    // Validation engine (KSP-006)
    // =========================================================================

    private val validationEngine = ValidationEngine(logger)

    // =========================================================================
    // State
    // =========================================================================

    // Collected stack info for tab builder dependencies
    private val stackInfoMap = mutableMapOf<String, StackInfo>()

    // Collected infos for validation, generation, and navigator extensions
    private val collectedStacks = mutableListOf<StackInfo>()
    private val collectedTabs = mutableListOf<TabInfo>()
    private val collectedPanes = mutableListOf<PaneInfo>()
    private val collectedScreens = mutableListOf<ScreenInfo>()
    private val collectedDestinations = mutableListOf<DestinationInfo>()
    private val collectedTransitions = mutableListOf<TransitionInfo>()
    private val collectedWrappers = mutableListOf<WrapperInfo>()

    // Originating files for incremental processing
    private val originatingFiles = mutableSetOf<KSFile>()

    // Track if generation has already happened (to handle multi-round processing)
    private var hasGenerated = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip if we've already generated code in a previous round
        if (hasGenerated) {
            return emptyList()
        }

        logger.info("QuoVadis: Starting symbol processing (mode: $generationMode)")

        // Phase 1: Collection - Extract all annotated symbols
        collectAllSymbols(resolver)

        // Phase 2: Validation
        val isValid = validationEngine.validate(
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            screens = collectedScreens,
            allDestinations = collectedDestinations,
            resolver = resolver
        )

        if (!isValid) {
            logger.error("QuoVadis: Validation failed - skipping code generation")
            if (strictValidation) {
                return emptyList()
            }
        }

        // Phase 3: Generation - Branch based on generation mode
        generateOutput()

        // Mark generation as complete to prevent duplicate generation in multi-round processing
        hasGenerated = true
        logger.info("QuoVadis: Processing complete")

        return emptyList()
    }

    // =========================================================================
    // Configuration Parsing
    // =========================================================================

    /**
     * Parses the generation mode from KSP options.
     *
     * @return The generation mode, defaulting to BOTH during transition period
     */
    private fun parseGenerationMode(): GenerationMode {
        return when (options["quoVadis.mode"]?.lowercase()) {
            "dsl" -> GenerationMode.DSL
            "legacy" -> GenerationMode.LEGACY
            "both" -> GenerationMode.BOTH
            else -> GenerationMode.BOTH // Default during transition period
        }
    }

    // =========================================================================
    // Phase 1: Collection
    // =========================================================================

    /**
     * Collects all annotated symbols from the resolver.
     *
     * This phase extracts all navigation-related annotations and populates
     * the collected* lists and originatingFiles set.
     */
    private fun collectAllSymbols(resolver: Resolver) {
        // Step 1: Extract stack info (no dependencies)
        collectStacks(resolver)

        // Step 2: Populate @TabItem cache and extract tab info
        // This must happen before extracting tabs due to KSP sealed subclass limitations in KMP
        tabExtractor.populateTabItemCache(resolver)
        collectTabs(resolver)

        // Step 3: Extract pane info
        collectPanes(resolver)

        // Step 4: Extract screens
        collectedScreens.addAll(screenExtractor.extractAll(resolver))
        // Track originating files from screens
        collectedScreens.forEach { screen ->
            screen.functionDeclaration.containingFile?.let { originatingFiles.add(it) }
        }

        // Step 5: Collect all destinations (from containers and standalone)
        collectAllDestinations(resolver)

        // Step 6: Extract wrappers
        collectWrappers(resolver)

        // Step 7: Extract transitions
        collectTransitions(resolver)

        logger.info("QuoVadis: Collected ${collectedStacks.size} stacks, ${collectedTabs.size} tabs, " +
            "${collectedPanes.size} panes, ${collectedScreens.size} screens, " +
            "${collectedDestinations.size} destinations, ${collectedWrappers.size} wrappers, " +
            "${collectedTransitions.size} transitions")
    }

    /**
     * Collects @Stack annotated classes.
     */
    private fun collectStacks(resolver: Resolver) {
        val stackSymbols = resolver.getSymbolsWithAnnotation(Stack::class.qualifiedName!!)
        stackSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractStackInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Stack $className: ${e.message}", classDeclaration)
            }
        }
    }

    /**
     * Collects @Tabs annotated classes.
     */
    private fun collectTabs(resolver: Resolver) {
        val tabsSymbols = resolver.getSymbolsWithAnnotation(Tabs::class.qualifiedName!!)
        tabsSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractTabInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Tab $className: ${e.message}", classDeclaration)
            }
        }
    }

    /**
     * Collects @Pane annotated classes.
     */
    private fun collectPanes(resolver: Resolver) {
        val paneSymbols = resolver.getSymbolsWithAnnotation(Pane::class.qualifiedName!!)
        paneSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractPaneInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Pane $className: ${e.message}", classDeclaration)
            }
        }
    }

    /**
     * Collects @TabWrapper and @PaneWrapper annotated functions.
     */
    private fun collectWrappers(resolver: Resolver) {
        val tabWrappers = wrapperExtractor.extractTabWrappers(resolver)
        val paneWrappers = wrapperExtractor.extractPaneWrappers(resolver)
        collectedWrappers.addAll(tabWrappers)
        collectedWrappers.addAll(paneWrappers)
        // Track originating files from wrappers
        tabWrappers.forEach { wrapper ->
            wrapper.functionDeclaration.containingFile?.let { originatingFiles.add(it) }
        }
        paneWrappers.forEach { wrapper ->
            wrapper.functionDeclaration.containingFile?.let { originatingFiles.add(it) }
        }
    }

    /**
     * Collects @Transition annotated classes.
     */
    private fun collectTransitions(resolver: Resolver) {
        val transitions = transitionExtractor.extractAll(resolver)
        collectedTransitions.addAll(transitions)
        // Track originating files from transitions
        transitions.forEach { transition ->
            originatingFiles.add(transition.containingFile)
        }
    }

    /**
     * Collect all destinations from stacks, tabs, panes, and standalone @Destination classes.
     *
     * This includes:
     * - Destinations inside @Stack containers
     * - FLAT_SCREEN tab destinations (from destinationInfo)
     * - Legacy tab destinations
     * - Pane destinations
     * - Standalone @Destination classes not inside any container
     */
    private fun collectAllDestinations(resolver: Resolver) {
        val seenQualifiedNames = mutableSetOf<String>()

        // Collect from stacks
        collectedStacks.forEach { stack ->
            stack.destinations.forEach { dest ->
                if (seenQualifiedNames.add(dest.qualifiedName)) {
                    collectedDestinations.add(dest)
                    dest.classDeclaration.containingFile?.let { originatingFiles.add(it) }
                }
            }
        }

        // Collect destinations from tabs:
        // - FLAT_SCREEN tabs: use destinationInfo
        // - NESTED_STACK tabs: destinations are in the stackInfo (already collected above)
        collectedTabs.forEach { tab ->
            tab.tabs.forEach { tabItem ->
                // For FLAT_SCREEN tabs, destinationInfo contains the destination
                tabItem.destinationInfo?.let { dest ->
                    if (seenQualifiedNames.add(dest.qualifiedName)) {
                        collectedDestinations.add(dest)
                        dest.classDeclaration.containingFile?.let { originatingFiles.add(it) }
                    }
                }
            }
        }

        // Collect from panes
        collectedPanes.forEach { pane ->
            pane.panes.forEach { paneItem ->
                if (seenQualifiedNames.add(paneItem.destination.qualifiedName)) {
                    collectedDestinations.add(paneItem.destination)
                    paneItem.destination.classDeclaration.containingFile?.let { originatingFiles.add(it) }
                }
            }
        }

        // Also scan for standalone @Destination classes (not inside containers)
        // These are used for navigation targets that don't belong to a specific container
        val destinationSymbols = resolver.getSymbolsWithAnnotation(Destination::class.qualifiedName!!)
        destinationSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            val destInfo = destinationExtractor.extract(classDeclaration)
            if (destInfo != null && seenQualifiedNames.add(destInfo.qualifiedName)) {
                collectedDestinations.add(destInfo)
                classDeclaration.containingFile?.let { originatingFiles.add(it) }
            }
        }
    }

    /**
     * Extract stack info without generating code.
     */
    private fun extractStackInfo(classDeclaration: KSClassDeclaration) {
        val stackInfo = stackExtractor.extract(classDeclaration)
        if (stackInfo == null) {
            logger.warn("Could not extract StackInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Store for tab builder dependencies
        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: stackInfo.className
        stackInfoMap[qualifiedName] = stackInfo

        // Collect for validation and navigator extensions
        collectedStacks.add(stackInfo)

        // Track originating file for incremental processing
        classDeclaration.containingFile?.let { originatingFiles.add(it) }
    }

    /**
     * Extract tab info without generating code.
     */
    private fun extractTabInfo(classDeclaration: KSClassDeclaration) {
        val tabInfo = tabExtractor.extract(classDeclaration)
        if (tabInfo == null) {
            logger.warn("Could not extract TabInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Collect for validation and navigator extensions
        collectedTabs.add(tabInfo)

        // Track originating file for incremental processing
        classDeclaration.containingFile?.let { originatingFiles.add(it) }
    }

    /**
     * Extract pane info without generating code.
     */
    private fun extractPaneInfo(classDeclaration: KSClassDeclaration) {
        val paneInfo = paneExtractor.extract(classDeclaration)
        if (paneInfo == null) {
            logger.warn("Could not extract PaneInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Collect for validation and navigator extensions
        collectedPanes.add(paneInfo)

        // Track originating file for incremental processing
        classDeclaration.containingFile?.let { originatingFiles.add(it) }
    }

    // =========================================================================
    // Phase 3: Generation
    // =========================================================================

    /**
     * Orchestrates code generation based on the configured generation mode.
     */
    private fun generateOutput() {
        val originatingFilesList = originatingFiles.toList()

        when (generationMode) {
            GenerationMode.DSL -> {
                logger.info("QuoVadis: Generating DSL config only")
                generateDslConfig(originatingFilesList)
            }
            GenerationMode.LEGACY -> {
                logger.info("QuoVadis: Generating legacy registries only")
                generateLegacyRegistries(originatingFilesList, withDeprecations = false)
            }
            GenerationMode.BOTH -> {
                logger.info("QuoVadis: Generating both DSL config and legacy registries (with deprecations)")
                generateDslConfig(originatingFilesList)
                generateLegacyRegistries(originatingFilesList, withDeprecations = true)
            }
        }

        // Always generate deep link handler and navigator extensions
        generateDeepLinkHandler()
        // generateNavigatorExtensions() // Currently disabled
    }

    /**
     * Generates the unified DSL-based NavigationConfig.
     *
     * This is the new approach that consolidates all navigation configuration
     * into a single file using the DSL pattern.
     */
    private fun generateDslConfig(originatingFilesList: List<KSFile>) {
        if (collectedScreens.isEmpty() && collectedTabs.isEmpty() &&
            collectedStacks.isEmpty() && collectedPanes.isEmpty()) {
            logger.warn("QuoVadis: No navigation elements found, skipping DSL config generation")
            return
        }

        logger.info("QuoVadis: Generating DSL NavigationConfig...")

        val navigationData = NavigationConfigGenerator.NavigationData(
            screens = collectedScreens,
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            transitions = collectedTransitions,
            wrappers = collectedWrappers,
            destinations = collectedDestinations
        )

        navigationConfigGenerator.generate(navigationData, originatingFilesList)
    }

    /**
     * Generates legacy registry objects (deprecated in BOTH mode).
     *
     * This maintains backward compatibility during the transition period.
     * When `withDeprecations` is true, all generated code includes @Deprecated
     * annotations pointing users to the new DSL-based approach.
     */
    private fun generateLegacyRegistries(
        originatingFilesList: List<KSFile>,
        withDeprecations: Boolean
    ) {
        logger.info("QuoVadis: Generating legacy registries (deprecated=$withDeprecations)...")

        // Generate NavNode builders
        generateStackBuilders()
        generateTabBuilders()
        generatePaneBuilders()

        // Generate screen registry
        generateScreenRegistry(collectedScreens)

        // Generate wrapper registry
        generateWrapperRegistry()

        // Generate transition registry
        generateTransitionRegistry()

        // Generate scope registry
        generateScopeRegistry()

        // Generate container registry
        generateContainerRegistry()

        // Note: LegacyGenerators wrapper is available for future use when
        // individual generators are updated to support deprecation annotations.
        // For now, we use the existing individual generators directly.
    }

    // =========================================================================
    // Legacy Generator Methods
    // =========================================================================

    /**
     * Generate stack builders for all collected stacks.
     */
    private fun generateStackBuilders() {
        collectedStacks.forEach { stackInfo ->
            try {
                navNodeBuilderGenerator.generateStackBuilder(stackInfo)
                logger.info("Generated NavNode builder for @Stack: ${stackInfo.className}")
            } catch (e: IllegalStateException) {
                logger.error("Error generating NavNode builder for @Stack ${stackInfo.className}: ${e.message}")
            }
        }
    }

    /**
     * Generate tab builders for all collected tabs.
     */
    private fun generateTabBuilders() {
        collectedTabs.forEach { tabInfo ->
            try {
                navNodeBuilderGenerator.generateTabBuilder(tabInfo, stackInfoMap)
                logger.info("Generated NavNode builder for @Tab: ${tabInfo.className}")
            } catch (e: IllegalStateException) {
                logger.error("Error generating NavNode builder for @Tab ${tabInfo.className}: ${e.message}")
            }
        }
    }

    /**
     * Generate pane builders for all collected panes.
     */
    private fun generatePaneBuilders() {
        collectedPanes.forEach { paneInfo ->
            try {
                navNodeBuilderGenerator.generatePaneBuilder(paneInfo)
                logger.info("Generated NavNode builder for @Pane: ${paneInfo.className}")
            } catch (e: IllegalStateException) {
                logger.error("Error generating NavNode builder for @Pane ${paneInfo.className}: ${e.message}")
            }
        }
    }

    /**
     * Generate screen registry from collected screens.
     */
    private fun generateScreenRegistry(screens: List<ScreenInfo>) {
        try {
            screenRegistryGenerator.generate(screens)
        } catch (e: IllegalStateException) {
            logger.error("Error generating screen registry: ${e.message}")
        }
    }

    /**
     * Generate WrapperRegistry from collected wrappers.
     */
    private fun generateWrapperRegistry() {
        val tabWrappers = collectedWrappers.filter { it.wrapperType == WrapperType.TAB }
        val paneWrappers = collectedWrappers.filter { it.wrapperType == WrapperType.PANE }

        // Skip generation if no wrappers found
        if (tabWrappers.isEmpty() && paneWrappers.isEmpty()) {
            logger.info("No @TabWrapper or @PaneWrapper annotations found, skipping WrapperRegistry generation")
            return
        }

        // Determine base package from first wrapper
        val basePackage = tabWrappers.firstOrNull()?.packageName
            ?: paneWrappers.firstOrNull()?.packageName
            ?: return

        try {
            wrapperRegistryGenerator.generate(tabWrappers, paneWrappers, basePackage)
            logger.info("Generated WrapperRegistry with ${tabWrappers.size} tab wrappers and ${paneWrappers.size} pane wrappers")
        } catch (e: Exception) {
            logger.error("Error generating WrapperRegistry: ${e.message}")
        }
    }

    /**
     * Generate TransitionRegistry from collected transitions.
     */
    private fun generateTransitionRegistry() {
        // Skip generation if no transitions found
        if (collectedTransitions.isEmpty()) {
            logger.info("No @Transition annotations found, skipping TransitionRegistry generation")
            return
        }

        // Determine base package from first transition
        val basePackage = collectedTransitions.first().destinationQualifiedName.substringBeforeLast('.')

        try {
            transitionRegistryGenerator.generate(collectedTransitions, basePackage)
            logger.info("Generated TransitionRegistry with ${collectedTransitions.size} transitions")
        } catch (e: Exception) {
            logger.error("Error generating TransitionRegistry: ${e.message}")
        }
    }

    /**
     * Generate scope registry for scoped navigation.
     *
     * Creates a registry that maps scope keys (sealed class names) to their
     * member destinations. Used by TreeMutator for scope-aware navigation.
     */
    private fun generateScopeRegistry() {
        if (collectedTabs.isEmpty() && collectedPanes.isEmpty() && collectedStacks.isEmpty()) {
            logger.info("No @Tab, @Pane, or @Stack annotations found, skipping ScopeRegistry generation")
            return
        }

        val basePackage = collectedTabs.firstOrNull()?.packageName
            ?: collectedPanes.firstOrNull()?.packageName
            ?: collectedStacks.firstOrNull()?.packageName
            ?: return

        try {
            scopeRegistryGenerator.generate(collectedTabs, collectedPanes, collectedStacks, basePackage)
        } catch (e: Exception) {
            logger.error("Error generating ScopeRegistry: ${e.message}")
        }
    }

    /**
     * Generate container registry for container-aware navigation.
     *
     * Creates a registry that maps destinations within @Tab and @Pane containers
     * to their builder functions. Used by TreeNavigator for automatic container creation.
     */
    private fun generateContainerRegistry() {
        if (collectedTabs.isEmpty() && collectedPanes.isEmpty()) {
            logger.info("No @Tab or @Pane annotations found, skipping ContainerRegistry generation")
            return
        }

        try {
            containerRegistryGenerator.generate(collectedTabs, collectedPanes)
            logger.info("Generated ContainerRegistry with ${collectedTabs.size} tab containers and ${collectedPanes.size} pane containers")
        } catch (e: Exception) {
            logger.error("Error generating ContainerRegistry: ${e.message}")
        }
    }

    /**
     * Generate navigator convenience extensions for all collected containers.
     *
     * Creates extension functions on Navigator for type-safe navigation:
     * - `to{Destination}()` for stack destinations
     * - `switchTo{Pane}Pane()` for pane switching
     *
     * Note: Tab switching extensions are no longer generated.
     */
    @Suppress("unused")
    private fun generateNavigatorExtensions() {
        if (collectedStacks.isEmpty() && collectedTabs.isEmpty() && collectedPanes.isEmpty()) {
            return
        }

        val basePackage = collectedStacks.firstOrNull()?.packageName
            ?: collectedTabs.firstOrNull()?.packageName
            ?: collectedPanes.firstOrNull()?.packageName
            ?: return

        try {
            navigatorExtGenerator.generate(collectedStacks, collectedTabs, collectedPanes, basePackage)
        } catch (e: IllegalStateException) {
            logger.error("Error generating navigator extensions: ${e.message}")
        }
    }

    // =========================================================================
    // Deep Link Handler Generation (KSP-004)
    // =========================================================================

    /**
     * Generate the deep link handler.
     *
     * The deep link handler maps URI patterns to destination instances,
     * enabling deep linking support for the navigation system.
     *
     * Only destinations with non-empty route patterns are included in generation.
     */
    private fun generateDeepLinkHandler() {
        try {
            deepLinkHandlerGenerator.generate(collectedDestinations)
        } catch (e: IllegalStateException) {
            logger.error("Error generating deep link handler: ${e.message}")
        }
    }
}

/**
 * Provider for QuoVadisSymbolProcessor.
 */
class QuoVadisSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return QuoVadisSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options
        )
    }
}
