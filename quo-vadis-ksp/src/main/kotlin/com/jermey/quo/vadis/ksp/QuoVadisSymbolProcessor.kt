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
import com.jermey.quo.vadis.annotations.TabGraph

/**
 * KSP processor for Quo Vadis navigation annotations.
 * 
 * Processes:
 * - @Graph annotated sealed classes - generates route registry and typed destination helpers
 * - @Content annotated functions - generates complete graph DSL builders
 * - @TabGraph annotated sealed classes - generates tab configuration and containers
 * - Route initialization - generates single initialization function for all routes
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    
    private val contentMappings = mutableMapOf<String, ContentFunctionInfo>()
    private val allGraphInfos = mutableListOf<GraphInfo>()
    
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
        
        // Third pass: process @TabGraph classes
        val tabGraphSymbols = resolver.getSymbolsWithAnnotation(TabGraph::class.qualifiedName!!)
        tabGraphSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processTabGraphClass(classDeclaration)
            } catch (e: IllegalStateException) {
                val className = classDeclaration.qualifiedName?.asString()
                logger.error("Error processing @TabGraph $className: ${e.message}", classDeclaration)
            }
        }
        
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
