package com.jermey.quo.vadis.annotations

/**
 * Marks a destination class as modal.
 *
 * Modal destinations are presented with modal presentation semantics
 * (e.g., bottom sheet, dialog, or full-screen overlay) rather than
 * standard push navigation.
 *
 * ## Usage
 *
 * Apply to `@Destination`-annotated classes to indicate they should
 * be presented modally:
 *
 * ```kotlin
 * @Modal
 * @Destination(route = "settings/confirm")
 * data object ConfirmDialog : SettingsDestination()
 *
 * @Modal
 * @Destination(route = "photo/picker")
 * data object PhotoPicker : HomeDestination()
 * ```
 *
 * ## KSP Processing
 *
 * The KSP processor scans for `@Modal` annotations and generates entries
 * in the `ModalRegistry`, which maps destination classes to their modal
 * presentation flag. The navigation system consults this registry to
 * determine presentation style.
 *
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Modal
