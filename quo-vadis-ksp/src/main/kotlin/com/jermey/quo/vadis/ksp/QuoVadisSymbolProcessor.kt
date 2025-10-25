package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.annotations.Graph

/**
 * KSP processor for Quo Vadis navigation annotations.
 * 
 * Processes @Graph annotated sealed classes and generates:
 * - Route constants
 * - Extension properties (route, data)
 * - Graph builder functions
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Graph::class.qualifiedName!!)
        
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            try {
                processGraphClass(classDeclaration)
            } catch (e: Exception) {
                logger.error("Error processing ${classDeclaration.qualifiedName?.asString()}: ${e.message}", classDeclaration)
            }
        }
        
        return emptyList()
    }
    
    private fun processGraphClass(classDeclaration: KSClassDeclaration) {
        logger.info("Processing graph: ${classDeclaration.qualifiedName?.asString()}")
        
        // Extract graph metadata
        val graphInfo = GraphInfoExtractor.extract(classDeclaration, logger)
        
        // Generate route constants
        RouteConstantsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate extension properties
        DestinationExtensionsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate graph builder function
        GraphBuilderGenerator.generate(graphInfo, codeGenerator, logger)
        
        logger.info("Completed processing graph: ${graphInfo.className}")
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
