package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.ScreenInfo

/**
 * Extracts @Screen annotations into ScreenInfo models.
 *
 * A screen binding connects a @Composable function to a destination class.
 * This extractor handles:
 * - Function name and package extraction
 * - Destination class reference resolution
 * - Detection of optional scope parameters (SharedTransitionScope, AnimatedVisibilityScope)
 *
 * @property logger KSP logger for error/warning output
 */
class ScreenExtractor(
    private val logger: KSPLogger
) {

    companion object {
        private const val SHARED_TRANSITION_SCOPE = "SharedTransitionScope"
        private const val ANIMATED_VISIBILITY_SCOPE = "AnimatedVisibilityScope"
    }

    /**
     * Extract ScreenInfo from a function declaration.
     *
     * @param functionDeclaration The @Composable function annotated with @Screen
     * @return ScreenInfo or null if extraction fails
     */
    fun extract(functionDeclaration: KSFunctionDeclaration): ScreenInfo? {
        val annotation = functionDeclaration.annotations.find {
            it.shortName.asString() == "Screen"
        } ?: return null

        // First argument of @Screen is the destination class (KClass<*>)
        val destinationType = annotation.arguments.firstOrNull()?.value as? KSType
        val destinationClass = destinationType?.declaration as? KSClassDeclaration
        if (destinationClass == null) {
            logger.warn("Could not resolve destination class for @Screen on ${functionDeclaration.simpleName.asString()}")
            return null
        }

        val parameters = functionDeclaration.parameters

        // Check if function has a parameter matching the destination type
        val hasDestinationParam = parameters.any { param ->
            val paramType = param.type.resolve()
            val paramQualifiedName = paramType.declaration.qualifiedName?.asString()
            val destQualifiedName = destinationClass.qualifiedName?.asString()
            paramQualifiedName == destQualifiedName
        }

        // Check for SharedTransitionScope parameter
        val hasSharedTransitionScope = parameters.any { param ->
            param.type.resolve().declaration.simpleName.asString() == SHARED_TRANSITION_SCOPE
        }

        // Check for AnimatedVisibilityScope parameter
        val hasAnimatedVisibilityScope = parameters.any { param ->
            param.type.resolve().declaration.simpleName.asString() == ANIMATED_VISIBILITY_SCOPE
        }

        return ScreenInfo(
            functionDeclaration = functionDeclaration,
            functionName = functionDeclaration.simpleName.asString(),
            destinationClass = destinationClass,
            hasDestinationParam = hasDestinationParam,
            hasSharedTransitionScope = hasSharedTransitionScope,
            hasAnimatedVisibilityScope = hasAnimatedVisibilityScope,
            packageName = functionDeclaration.packageName.asString()
        )
    }

    /**
     * Extract all @Screen-annotated functions from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of ScreenInfo for all @Screen-annotated functions
     */
    fun extractAll(resolver: Resolver): List<ScreenInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Screen")
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
