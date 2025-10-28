package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/**
 * Extracts navigation graph information from annotated classes.
 */
object GraphInfoExtractor {
    
    fun extract(graphClass: KSClassDeclaration, logger: KSPLogger): GraphInfo {
        val graphAnnotation = graphClass.annotations
            .first { it.shortName.asString() == "Graph" }
        
        val graphName = graphAnnotation.arguments
            .first { it.name?.asString() == "name" }
            .value as String
        
        val packageName = graphClass.packageName.asString()
        val className = graphClass.simpleName.asString()
        
        // Validate sealed class
        if (graphClass.modifiers.contains(Modifier.SEALED).not()) {
            logger.error("@Graph can only be applied to sealed classes", graphClass)
            error("Graph class must be sealed")
        }
        
        // Extract destinations from sealed subclasses
        val destinations = extractDestinations(graphClass, logger)
        
        return GraphInfo(
            graphClass = graphClass,
            graphName = graphName,
            packageName = packageName,
            className = className,
            destinations = destinations
        )
    }
    
    private fun extractDestinations(
        graphClass: KSClassDeclaration,
        logger: KSPLogger
    ): List<DestinationInfo> {
        return graphClass.getSealedSubclasses().mapNotNull { destinationClass ->
            extractDestinationInfo(destinationClass, logger)
        }.toList()
    }
    
    private fun extractDestinationInfo(
        destinationClass: KSClassDeclaration,
        logger: KSPLogger
    ): DestinationInfo? {
        // Extract @Route
        val routeAnnotation = destinationClass.annotations
            .firstOrNull { it.shortName.asString() == "Route" }
        
        if (routeAnnotation == null) {
            val destName = destinationClass.simpleName.asString()
            logger.warn(
                "Destination $destName has no @Route annotation, skipping",
                destinationClass
            )
            return null
        }
        
        val route = routeAnnotation.arguments
            .first { it.name?.asString() == "path" }
            .value as String
        
        // Extract @Argument if present
        val argumentAnnotation = destinationClass.annotations
            .firstOrNull { it.shortName.asString() == "Argument" }
        
        val argumentType = argumentAnnotation?.let {
            val dataClassArg = it.arguments.first { arg -> 
                arg.name?.asString() == "dataClass" 
            }
            val typeRef = dataClassArg.value as? KSType
            typeRef?.declaration?.qualifiedName?.asString()
        }
        
        return DestinationInfo(
            destinationClass = destinationClass,
            name = destinationClass.simpleName.asString(),
            route = route,
            isObject = destinationClass.classKind == ClassKind.OBJECT,
            isDataClass = destinationClass.modifiers.contains(Modifier.DATA),
            argumentType = argumentType
        )
    }
}
