package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

/**
 * Extracted metadata from a [@Modal][com.jermey.quo.vadis.annotations.Modal] annotation.
 *
 * Modal info connects a destination or container class to its modal presentation flag.
 * The KSP processor uses this to generate `ModalRegistry` entries
 * that determine which destinations and containers are presented modally.
 *
 * ## Generated Code Usage
 *
 * The KSP generator uses this info to create DSL calls in the
 * `navigationConfig { }` block:
 *
 * ```kotlin
 * // For destinations
 * modal<ConfirmDialog>()
 *
 * // For containers
 * modalContainer("PhotoPickerTabs")
 * ```
 *
 * @property annotatedClass The class declaration annotated with @Modal
 * @property qualifiedName Fully qualified name of the annotated class (e.g., "com.example.ConfirmDialog")
 * @property isDestination True if the class has @Destination, false if it's a container (@Tabs/@Stack/@Pane)
 * @property containerName The name from @Tabs/@Stack/@Pane annotation if this is a container; null for destinations
 * @property containingFile The KSP file containing this annotation, for incremental compilation tracking
 */
data class ModalInfo(
    val annotatedClass: KSClassDeclaration,
    val qualifiedName: String,
    val isDestination: Boolean,
    val containerName: String?,
    val containingFile: KSFile
)
