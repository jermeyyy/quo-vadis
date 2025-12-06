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
import com.jermey.quo.vadis.ksp.generators.ScreenRegistryGenerator
import com.jermey.quo.vadis.ksp.models.StackInfo

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

    // Collected stack info for tab builder dependencies
    private val stackInfoMap = mutableMapOf<String, StackInfo>()
    
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

        // Fourth pass: generate NavNode builders for new architecture
        processNavNodeBuilders(resolver)

        // Fifth pass: process @Screen annotations for screen registry generation
        processScreenRegistry(resolver)

        // Sixth pass: process @Destination annotations for deep link handler generation
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
     * 1. Stack builders first (no dependencies)
     * 2. Tab builders second (depend on stack builders)
     * 3. Pane builders last (depend on stack builders)
     */
    private fun processNavNodeBuilders(resolver: Resolver) {
        // Step 1: Extract and generate stack builders first (they have no dependencies)
        val stackSymbols = resolver.getSymbolsWithAnnotation(Stack::class.qualifiedName!!)
        stackSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processStackNavNodeBuilder(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error generating NavNode builder for @Stack $className: ${e.message}", classDeclaration)
            }
        }

        // Step 2: Extract and generate tab builders (depend on stack builders being available)
        val tabSymbols = resolver.getSymbolsWithAnnotation(Tab::class.qualifiedName!!)
        tabSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processTabNavNodeBuilder(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error generating NavNode builder for @Tab $className: ${e.message}", classDeclaration)
            }
        }

        // Step 3: Extract and generate pane builders
        val paneSymbols = resolver.getSymbolsWithAnnotation(Pane::class.qualifiedName!!)
        paneSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processPaneNavNodeBuilder(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error generating NavNode builder for @Pane $className: ${e.message}", classDeclaration)
            }
        }
    }

    /**
     * Process a @Stack annotated class to generate its NavNode builder.
     */
    private fun processStackNavNodeBuilder(classDeclaration: KSClassDeclaration) {
        val stackInfo = stackExtractor.extract(classDeclaration)
        if (stackInfo == null) {
            logger.warn("Could not extract StackInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Store for tab builder dependencies
        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: stackInfo.className
        stackInfoMap[qualifiedName] = stackInfo

        // Generate the builder
        navNodeBuilderGenerator.generateStackBuilder(stackInfo)
        logger.info("Generated NavNode builder for @Stack: ${stackInfo.className}")
    }

    /**
     * Process a @Tab annotated class to generate its NavNode builder.
     */
    private fun processTabNavNodeBuilder(classDeclaration: KSClassDeclaration) {
        val tabInfo = tabExtractor.extract(classDeclaration)
        if (tabInfo == null) {
            logger.warn("Could not extract TabInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Generate the builder with stack dependencies
        navNodeBuilderGenerator.generateTabBuilder(tabInfo, stackInfoMap)
        logger.info("Generated NavNode builder for @Tab: ${tabInfo.className}")
    }

    /**
     * Process a @Pane annotated class to generate its NavNode builder.
     */
    private fun processPaneNavNodeBuilder(classDeclaration: KSClassDeclaration) {
        val paneInfo = paneExtractor.extract(classDeclaration)
        if (paneInfo == null) {
            logger.warn("Could not extract PaneInfo from ${classDeclaration.qualifiedName?.asString()}")
            return
        }

        // Generate the builder
        navNodeBuilderGenerator.generatePaneBuilder(paneInfo)
        logger.info("Generated NavNode builder for @Pane: ${paneInfo.className}")
    }

    // =========================================================================
    // Screen Registry Generation (KSP-003)
    // =========================================================================

    /**
     * Process @Screen annotations to generate the screen registry.
     *
     * The screen registry maps destinations to their corresponding composable
     * screen functions, providing a central dispatch mechanism for rendering
     * screen content.
     */
    private fun processScreenRegistry(resolver: Resolver) {
        try {
            val screens = screenExtractor.extractAll(resolver)
            screenRegistryGenerator.generate(screens)
        } catch (e: IllegalStateException) {
            logger.error("Error generating screen registry: ${e.message}")
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
