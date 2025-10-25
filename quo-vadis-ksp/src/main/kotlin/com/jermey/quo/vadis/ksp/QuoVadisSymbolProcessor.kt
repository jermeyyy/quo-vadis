package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.annotations.Graph

/**
 * KSP processor for Quo Vadis navigation annotations.
 * 
 * Processes:
 * - @Graph annotated sealed classes - generates route registry and typed destination helpers
 * - @Content annotated functions - generates complete graph DSL builders
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    
    private val contentMappings = mutableMapOf<String, ContentFunctionInfo>()
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // First pass: collect @Content functions
        val contentFunctions = resolver.getSymbolsWithAnnotation(Content::class.qualifiedName!!)
        contentFunctions.filterIsInstance<KSFunctionDeclaration>().forEach { function ->
            try {
                processContentFunction(function)
            } catch (e: Exception) {
                logger.error("Error processing @Content function ${function.simpleName.asString()}: ${e.message}", function)
            }
        }
        
        // Second pass: process @Graph classes and generate complete DSL
        val graphSymbols = resolver.getSymbolsWithAnnotation(Graph::class.qualifiedName!!)
        graphSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processGraphClass(classDeclaration)
            } catch (e: Exception) {
                logger.error("Error processing ${classDeclaration.qualifiedName?.asString()}: ${e.message}", classDeclaration)
            }
        }
        
        return emptyList()
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
        
        // Generate route constants
        RouteConstantsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate extension properties
        DestinationExtensionsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate complete graph DSL builder (NEW!)
        CompleteGraphGenerator.generate(graphInfo, contentMappings, codeGenerator, logger)
        
        logger.info("Completed processing graph: ${graphInfo.className}")
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
