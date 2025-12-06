package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Pane
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.ksp.extractors.DestinationExtractor
import com.jermey.quo.vadis.ksp.extractors.PaneExtractor
import com.jermey.quo.vadis.ksp.extractors.ScreenExtractor
import com.jermey.quo.vadis.ksp.extractors.StackExtractor
import com.jermey.quo.vadis.ksp.extractors.TabExtractor
import com.jermey.quo.vadis.ksp.generators.DeepLinkHandlerGenerator
import com.jermey.quo.vadis.ksp.generators.NavNodeBuilderGenerator
import com.jermey.quo.vadis.ksp.generators.NavigatorExtGenerator
import com.jermey.quo.vadis.ksp.generators.ScreenRegistryGenerator
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.validation.ValidationEngine

/**
 * KSP processor for Quo Vadis navigation annotations.
 *
 * Processes:
 * - @Graph annotated sealed classes - generates route registry and typed destination helpers
 * - @Content annotated functions - generates complete graph DSL builders
 * - @TabGraph annotated sealed classes - generates tab configuration and containers
 * - @Stack, @Tab, @Pane annotations - generates NavNode builder functions
 * - Route initialization - generates single initialization function for all routes
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val contentMappings = mutableMapOf<String, ContentFunctionInfo>()
    private val allGraphInfos = mutableListOf<GraphInfo>()

    // Extractors for new NavNode architecture (KSP-001)
    private val destinationExtractor = DestinationExtractor(logger)
    private val stackExtractor = StackExtractor(destinationExtractor, logger)
    private val tabExtractor = TabExtractor(destinationExtractor, logger)
    private val paneExtractor = PaneExtractor(destinationExtractor, logger)

    // Generator for NavNode builders (KSP-002)
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)

    // Extractor and generator for screen registry (KSP-003)
    private val screenExtractor = ScreenExtractor(logger)
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)

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
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // First pass: collect @Content functions
        val contentFunctions = resolver.getSymbolsWithAnnotation(Content::class.qualifiedName!!)
        contentFunctions.filterIsInstance<KSFunctionDeclaration>().forEach { function ->
            try {
                processContentFunction(function)
            } catch (e: IllegalStateException) {
                val functionName = function.simpleName.asString()
                logger.error("Error processing @Content function $functionName: ${e.message}", function)
            }
        }
        
        // Second pass: process @Graph classes and generate complete DSL
        val graphSymbols = resolver.getSymbolsWithAnnotation(Graph::class.qualifiedName!!)
        graphSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processGraphClass(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error processing $className: ${e.message}", classDeclaration)
            }
        }
        
        // Third pass: process @Tab classes
        val tabGraphSymbols = resolver.getSymbolsWithAnnotation(Tab::class.qualifiedName!!)
        tabGraphSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processTabGraphClass(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error processing @Tab $className: ${e.message}", classDeclaration)
            }
        }

        // Fourth pass: generate NavNode builders for new architecture (includes validation and screen registry)
        processNavNodeBuilders(resolver)

        // Fifth pass: process @Destination annotations for deep link handler generation
        processDeepLinkHandler(resolver)

        return emptyList()
    }
    
    override fun finish() {
        // Third pass: generate route initialization function after all graphs are processed
        if (allGraphInfos.isNotEmpty()) {
            try {
                RouteInitializationGenerator.generate(allGraphInfos, codeGenerator, logger)
            } catch (e: IllegalStateException) {
                logger.error("Error generating route initialization: ${e.message}")
            }
        }
    }
    
    private fun processContentFunction(function: KSFunctionDeclaration) {
        val contentAnnotation = function.annotations.find {
            it.shortName.asString() == "Content"
        } ?: return
        
        val destinationClassArg = contentAnnotation.arguments.find { it.name?.asString() == "destination" }
        val destinationClass = destinationClassArg?.value as? KSType
        val destinationQualifiedName = destinationClass?.declaration?.qualifiedName?.asString()
        
        if (destinationQualifiedName != null) {
            contentMappings[destinationQualifiedName] = ContentFunctionInfo(
                functionName = function.simpleName.asString(),
                packageName = function.packageName.asString(),
                destinationClass = destinationQualifiedName
            )
            logger.info("Registered @Content function: ${function.simpleName.asString()} for $destinationQualifiedName")
        }
    }
    
    private fun processGraphClass(classDeclaration: KSClassDeclaration) {
        logger.info("Processing graph: ${classDeclaration.qualifiedName?.asString()}")
        
        // Extract graph metadata
        val graphInfo = GraphInfoExtractor.extract(classDeclaration, logger)
        
        // Store for route initialization generation
        allGraphInfos.add(graphInfo)
        
        // Generate route constants
        RouteConstantsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate extension properties
        DestinationExtensionsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate complete graph DSL builder
        GraphGenerator.generate(graphInfo, contentMappings, codeGenerator, logger)
        
        logger.info("Completed processing graph: ${graphInfo.className}")
    }
    
    private fun processTabGraphClass(classDeclaration: KSClassDeclaration) {
        logger.info("Processing tab graph: ${classDeclaration.qualifiedName?.asString()}")

        // Extract tab graph metadata
        val tabGraphInfo = TabGraphExtractor.extract(classDeclaration, logger)

        // Generate tab configuration and container
        TabGraphGenerator.generate(tabGraphInfo, codeGenerator, logger)

        logger.info("Completed processing tab graph: ${tabGraphInfo.className}")
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

        // Step 2: Extract tab info
        val tabSymbols = resolver.getSymbolsWithAnnotation(Tab::class.qualifiedName!!)
        tabSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
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
        val allDestinations = collectAllDestinations()

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
        generateNavigatorExtensions()

        // Step 8: Generate screen registry (moved here to use already extracted screens)
        generateScreenRegistry(screens)
    }

    /**
     * Collect all destinations from stacks, tabs, and panes.
     */
    private fun collectAllDestinations(): List<DestinationInfo> {
        val destinations = mutableListOf<DestinationInfo>()
        collectedStacks.forEach { stack -> destinations.addAll(stack.destinations) }
        collectedTabs.forEach { tab -> destinations.addAll(tab.tabs.map { it.destination }) }
        collectedPanes.forEach { pane -> destinations.addAll(pane.panes.map { it.destination }) }
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
     * Generate navigator convenience extensions for all collected containers.
     *
     * Creates extension functions on Navigator for type-safe navigation:
     * - `to{Destination}()` for stack destinations
     * - `switchTo{Tab}Tab()` for tab switching
     * - `switchTo{Pane}Pane()` for pane switching
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
 * Information about a @Content annotated function.
 */
data class ContentFunctionInfo(
    val functionName: String,
    val packageName: String,
    val destinationClass: String
)

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
