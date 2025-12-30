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
import com.jermey.quo.vadis.ksp.extractors.ContainerExtractor
import com.jermey.quo.vadis.ksp.generators.DeepLinkHandlerGenerator
import com.jermey.quo.vadis.ksp.generators.dsl.NavigationConfigGenerator
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.ContainerInfoModel
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
 * - @TabsContainer / @PaneContainer - generates wrapper functions for container scopes
 * - @Transition - generates transition registry for animated navigation
 *
 * ## KSP Options
 * - `quoVadis.package` - Target package for generated code
 * - `quoVadis.modulePrefix` - Prefix for generated class names (e.g., "ComposeApp" -> ComposeAppNavigationConfig).
 *   This is required for multi-module projects to avoid class name conflicts.
 * - `quoVadis.strictValidation` - Whether validation errors abort generation (default: true)
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    options: Map<String, String>
) : SymbolProcessor {

    // =========================================================================
    // Configuration
    // =========================================================================

    private val targetPackage: String = options["quoVadis.package"]
        ?: "com.jermey.quo.vadis.generated"
    private val modulePrefix: String = options["quoVadis.modulePrefix"] ?: ""
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
    private val containerExtractor = ContainerExtractor(logger)
    private val transitionExtractor = TransitionExtractor(logger)

    // =========================================================================
    // Deeplink Generator
    // =========================================================================

    private val deepLinkHandlerGenerator =
        DeepLinkHandlerGenerator(codeGenerator, logger, modulePrefix)

    // =========================================================================
    // New DSL Generator (for DSL and BOTH modes)
    // =========================================================================

    private val navigationConfigGenerator = NavigationConfigGenerator(
        codeGenerator = codeGenerator,
        logger = logger,
        packageName = targetPackage,
        modulePrefix = modulePrefix
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
    private val collectedContainers = mutableListOf<ContainerInfoModel>()

    // Originating files for incremental processing
    private val originatingFiles = mutableSetOf<KSFile>()

    // Track if generation has already happened (to handle multi-round processing)
    private var hasGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip if we've already generated code in a previous round
        if (hasGenerated) {
            return emptyList()
        }

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

        logger.info(
            "QuoVadis: Collected ${collectedStacks.size} stacks, ${collectedTabs.size} tabs, " +
                    "${collectedPanes.size} panes, ${collectedScreens.size} screens, " +
                    "${collectedDestinations.size} destinations, ${collectedContainers.size} wrappers, " +
                    "${collectedTransitions.size} transitions"
        )
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
     * Collects @TabsContainer and @PaneContainer annotated functions.
     */
    private fun collectWrappers(resolver: Resolver) {
        val tabsContainers = containerExtractor.extractTabsContainers(resolver)
        val paneContainers = containerExtractor.extractPaneContainers(resolver)
        collectedContainers.addAll(tabsContainers)
        collectedContainers.addAll(paneContainers)
        // Track originating files from wrappers
        tabsContainers.forEach { wrapper ->
            wrapper.functionDeclaration.containingFile?.let { originatingFiles.add(it) }
        }
        paneContainers.forEach { wrapper ->
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
                    paneItem.destination.classDeclaration.containingFile?.let {
                        originatingFiles.add(
                            it
                        )
                    }
                }
            }
        }

        // Also scan for standalone @Destination classes (not inside containers)
        // These are used for navigation targets that don't belong to a specific container
        val destinationSymbols =
            resolver.getSymbolsWithAnnotation(Destination::class.qualifiedName!!)
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

        logger.info("QuoVadis: Generating config")
        generateDslConfig(originatingFilesList)

        // Always generate deep link handler and navigator extensions
        generateDeepLinkHandler()
    }

    /**
     * Generates the unified DSL-based NavigationConfig.
     *
     * This is the new approach that consolidates all navigation configuration
     * into a single file using the DSL pattern.
     */
    private fun generateDslConfig(originatingFilesList: List<KSFile>) {
        if (collectedScreens.isEmpty() && collectedTabs.isEmpty() &&
            collectedStacks.isEmpty() && collectedPanes.isEmpty()
        ) {
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
            wrappers = collectedContainers,
            destinations = collectedDestinations
        )

        navigationConfigGenerator.generate(navigationData, originatingFilesList)
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
