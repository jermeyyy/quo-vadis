package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

/**
 * Extracted metadata from a [@Transition][com.jermey.quo.vadis.annotations.Transition] annotation.
 *
 * Transition info connects a destination class to its transition configuration.
 * The KSP processor uses this to generate `GeneratedTransitionRegistry` entries
 * that map destination classes to their [NavTransition][com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition] instances.
 *
 * ## Generated Code Usage
 *
 * The KSP generator uses this info to create entries in `GeneratedTransitionRegistry`:
 *
 * ```kotlin
 * override fun getTransition(destinationClass: KClass<*>): NavTransition? {
 *     return when (destinationClass) {
 *         com.example.DetailsScreen::class -> NavTransition.SlideVertical
 *         com.example.CustomScreen::class -> CustomTransitionProvider().transition
 *         else -> null
 *     }
 * }
 * ```
 *
 * @property destinationClass The class declaration annotated with @Transition
 * @property destinationQualifiedName Fully qualified name of the destination class (e.g., "com.example.DetailsScreen")
 * @property transitionType The type of transition as a string (e.g., "SlideHorizontal", "Fade", "Custom")
 * @property customTransitionClass Qualified name of the custom transition class if type is Custom; null otherwise
 * @property containingFile The KSP file containing this annotation, for incremental compilation tracking
 */
public data class TransitionInfo(
    val destinationClass: KSClassDeclaration,
    val destinationQualifiedName: String,
    val transitionType: String,
    val customTransitionClass: String?,
    val containingFile: KSFile
)
