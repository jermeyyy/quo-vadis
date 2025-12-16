package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
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
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    // Extractors for new NavNode architecture (KSP-001)
    private val destinationExtractor = DestinationExtractor(logger)
    private val stackExtractor = StackExtractor(destinationExtractor, logger)
    private val tabExtractor = TabExtractor(destinationExtractor, logger, stackExtractor)
    private val paneExtractor = PaneExtractor(destinationExtractor, logger)

    // Generator for NavNode builders (KSP-002)
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)

    // Extractor and generator for screen registry (KSP-003)
    private val screenExtractor = ScreenExtractor(logger)
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)

    // Extractor and generator for wrapper registry (HIER-015)
    private val wrapperExtractor = WrapperExtractor(logger)
    private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)

    // Extractor and generator for transition registry (HIER-015)
    private val transitionExtractor = TransitionExtractor(logger)
    private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)

    // Generator for scope registry (scoped navigation)
    private val scopeRegistryGenerator = ScopeRegistryGenerator(codeGenerator, logger)

    // Generator for container registry (container-aware navigation)
    private val containerRegistryGenerator = ContainerRegistryGenerator(codeGenerator, logger)

    // Generator for deep link handler (KSP-004)
    private val deepLinkHandlerGenerator = DeepLinkHandlerGenerator(codeGenerator, logger)

    // Generator for navigator extensions (KSP-005)
    private val navigatorExtGenerator = NavigatorExtGenerator(codeGenerator, logger)

    // Validation engine (KSP-006)
    private val validationEngine = ValidationEngine(logger)

    // Collected stack info for tab builder dependencies
    private val stackInfoMap = mutableMapOf<String, StackInfo>()

    // Collected infos for navigator extension generation
    private val collectedStacks = mutableListOf<StackInfo>()
    private val collectedTabs = mutableListOf<TabInfo>()
    private val collectedPanes = mutableListOf<PaneInfo>()
    
    // Track if generation has already happened (to handle multi-round processing)
    private var hasGenerated = false
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip if we've already generated code in a previous round
        if (hasGenerated) {
            return emptyList()
        }
        
        // First pass: generate NavNode builders for new architecture (includes validation and screen registry)
        processNavNodeBuilders(resolver)

        // Second pass: process @Destination annotations for deep link handler generation
        processDeepLinkHandler(resolver)

        return emptyList()
    }

    // =========================================================================
    // NavNode Builder Generation (KSP-002)
    // =========================================================================

    /**
     * Process @Stack, @Tab, and @Pane annotations to generate NavNode builder functions.
     *
     * This implements the new tree-based navigation architecture where:
     * - @Stack generates StackNode builders with start destination
     * - @Tab generates TabNode builders referencing stack builders
     * - @Pane generates PaneNode builders with pane configurations
     *
     * Processing order matters:
     * 1. Extract all containers first (stacks, tabs, panes)
     * 2. Extract screens and all destinations for validation
     * 3. Run validation (KSP-006) - stop if errors
     * 4. Generate builders only if validation passes
     */
    private fun processNavNodeBuilders(resolver: Resolver) {
        // Step 1: Extract stack info (no dependencies)
        val stackSymbols = resolver.getSymbolsWithAnnotation(Stack::class.qualifiedName!!)
        stackSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractStackInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Stack $className: ${e.message}", classDeclaration)
            }
        }

        // Step 2: Populate @TabItem cache and extract tab info
        // This must happen before extracting tabs due to KSP sealed subclass limitations in KMP
        tabExtractor.populateTabItemCache(resolver)
        val tabsSymbols = resolver.getSymbolsWithAnnotation(Tabs::class.qualifiedName!!)
        tabsSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractTabInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Tab $className: ${e.message}", classDeclaration)
            }
        }

        // Step 3: Extract pane info
        val paneSymbols = resolver.getSymbolsWithAnnotation(Pane::class.qualifiedName!!)
        paneSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                extractPaneInfo(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error extracting @Pane $className: ${e.message}", classDeclaration)
            }
        }

        // Step 4: Extract screens and all destinations for validation
        val screens = screenExtractor.extractAll(resolver)
        val allDestinations = collectAllDestinations(resolver)

        // Step 5: Run validation (KSP-006)
        val isValid = validationEngine.validate(
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            screens = screens,
            allDestinations = allDestinations,
            resolver = resolver
        )

        if (!isValid) {
            logger.error("Validation failed - skipping code generation")
            return
        }

        // Step 6: Generate NavNode builders (only if validation passes)
        generateStackBuilders()
        generateTabBuilders()
        generatePaneBuilders()

        // Step 7: Generate navigator extensions (KSP-005)
//        generateNavigatorExtensions()

        // Step 8: Generate screen registry (moved here to use already extracted screens)
        generateScreenRegistry(screens)

        // Step 9: Extract and generate wrapper registry (HIER-015)
        generateWrapperRegistry(resolver)

        // Step 10: Extract and generate transition registry (HIER-015)
        generateTransitionRegistry(resolver)

        // Step 11: Generate scope registry (scoped navigation)
        generateScopeRegistry()

        // Step 12: Generate container registry (container-aware navigation)
        generateContainerRegistry()
        
        // Mark generation as complete to prevent duplicate generation in multi-round processing
        hasGenerated = true
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
    private fun collectAllDestinations(resolver: Resolver): List<DestinationInfo> {
        val destinations = mutableListOf<DestinationInfo>()
        val seenQualifiedNames = mutableSetOf<String>()

        // Collect from stacks
        collectedStacks.forEach { stack ->
            stack.destinations.forEach { dest ->
                if (seenQualifiedNames.add(dest.qualifiedName)) {
                    destinations.add(dest)
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
                        destinations.add(dest)
                    }
                }
            }
        }

        // Collect from panes
        collectedPanes.forEach { pane ->
            pane.panes.forEach { paneItem ->
                if (seenQualifiedNames.add(paneItem.destination.qualifiedName)) {
                    destinations.add(paneItem.destination)
                }
            }
        }

        // Also scan for standalone @Destination classes (not inside containers)
        // These are used for navigation targets that don't belong to a specific container
        val destinationSymbols = resolver.getSymbolsWithAnnotation(Destination::class.qualifiedName!!)
        destinationSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            val destInfo = destinationExtractor.extract(classDeclaration)
            if (destInfo != null && seenQualifiedNames.add(destInfo.qualifiedName)) {
                destinations.add(destInfo)
            }
        }

        return destinations
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
    }

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
     * Generate screen registry from extracted screens.
     */
    private fun generateScreenRegistry(screens: List<ScreenInfo>) {
        try {
            screenRegistryGenerator.generate(screens)
        } catch (e: IllegalStateException) {
            logger.error("Error generating screen registry: ${e.message}")
        }
    }

    /**
     * Extract @TabWrapper and @PaneWrapper annotations and generate WrapperRegistry.
     */
    private fun generateWrapperRegistry(resolver: Resolver) {
        val tabWrappers = wrapperExtractor.extractTabWrappers(resolver)
        val paneWrappers = wrapperExtractor.extractPaneWrappers(resolver)

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
     * Extract @Transition annotations and generate TransitionRegistry.
     */
    private fun generateTransitionRegistry(resolver: Resolver) {
        val transitions = transitionExtractor.extractAll(resolver)

        // Skip generation if no transitions found
        if (transitions.isEmpty()) {
            logger.info("No @Transition annotations found, skipping TransitionRegistry generation")
            return
        }

        // Determine base package from first transition
        val basePackage = transitions.first().destinationQualifiedName.substringBeforeLast('.')

        try {
            transitionRegistryGenerator.generate(transitions, basePackage)
            logger.info("Generated TransitionRegistry with ${transitions.size} transitions")
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
     * Process @Destination annotations to generate the deep link handler.
     *
     * The deep link handler maps URI patterns to destination instances,
     * enabling deep linking support for the navigation system.
     *
     * Only destinations with non-empty route patterns are included in generation.
     */
    private fun processDeepLinkHandler(resolver: Resolver) {
        try {
            val destinationSymbols = resolver.getSymbolsWithAnnotation(Destination::class.qualifiedName!!)
            val destinations = destinationSymbols
                .filterIsInstance<KSClassDeclaration>()
                .mapNotNull { classDeclaration ->
                    try {
                        destinationExtractor.extract(classDeclaration)
                    } catch (e: IllegalStateException) {
                        val className = classDeclaration.qualifiedName?.asString()
                        logger.error("Error extracting @Destination $className: ${e.message}", classDeclaration)
                        null
                    }
                }
                .toList()

            deepLinkHandlerGenerator.generate(destinations)
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
            logger = environment.logger
        )
    }
}
