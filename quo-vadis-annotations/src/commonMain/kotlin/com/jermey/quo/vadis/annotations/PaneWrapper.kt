package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a Composable function as a pane wrapper for the specified pane destination class.
 *
 * Pane wrappers provide the adaptive layout chrome (split views, side panels, etc.) for
 * multi-pane navigation containers. The wrapper receives a `PaneWrapperScope` that provides
 * access to pane state and layout information, and a content slot where the pane contents
 * are rendered.
 *
 * ## Function Signature Requirements
 *
 * The annotated function must follow this signature:
 * ```kotlin
 * @PaneWrapper(CatalogPane::class)
 * @Composable
 * fun CatalogPaneWrapper(scope: PaneWrapperScope, content: @Composable () -> Unit) {
 *     // Wrapper implementation
 * }
 * ```
 *
 * Parameters (detected by KSP based on types):
 * 1. `scope: PaneWrapperScope` - Provides access to pane state and layout
 * 2. `content: @Composable () -> Unit` - The pane contents to render
 *
 * ## PaneWrapperScope
 *
 * The scope provides:
 * - `navigator` - The Navigator instance for navigation operations
 * - `paneContents` - List of PaneContentSlot for each pane (role, isVisible, content)
 * - `activePaneRole` - The currently focused pane role
 * - `isExpanded` - Whether the layout is in expanded (multi-pane) mode
 *
 * ## Example: Master-Detail List-Detail Wrapper
 *
 * ```kotlin
 * @PaneWrapper(CatalogPane::class)
 * @Composable
 * fun CatalogMasterDetailWrapper(
 *     scope: PaneWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     if (scope.isExpanded) {
 *         // Expanded: Show side-by-side
 *         Row(Modifier.fillMaxSize()) {
 *             // Primary pane (list)
 *             scope.paneContents
 *                 .find { it.role == PaneRole.PRIMARY }
 *                 ?.let { slot ->
 *                     Box(Modifier.weight(0.4f)) {
 *                         slot.content()
 *                     }
 *                 }
 *
 *             // Divider
 *             VerticalDivider()
 *
 *             // Secondary pane (detail)
 *             scope.paneContents
 *                 .find { it.role == PaneRole.SECONDARY }
 *                 ?.let { slot ->
 *                     Box(Modifier.weight(0.6f)) {
 *                         slot.content()
 *                     }
 *                 }
 *         }
 *     } else {
 *         // Compact: Show single pane
 *         content()
 *     }
 * }
 * ```
 *
 * ## Example: Three-Pane Email Wrapper
 *
 * ```kotlin
 * @PaneWrapper(MailPane::class)
 * @Composable
 * fun MailThreePaneWrapper(
 *     scope: PaneWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
 *
 *     when {
 *         windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> {
 *             // Three-column layout
 *             Row(Modifier.fillMaxSize()) {
 *                 // Folders (PRIMARY)
 *                 scope.paneContents
 *                     .find { it.role == PaneRole.PRIMARY }
 *                     ?.content?.invoke()
 *
 *                 VerticalDivider()
 *
 *                 // Message list (SECONDARY)
 *                 scope.paneContents
 *                     .find { it.role == PaneRole.SECONDARY }
 *                     ?.content?.invoke()
 *
 *                 VerticalDivider()
 *
 *                 // Message detail (EXTRA)
 *                 scope.paneContents
 *                     .find { it.role == PaneRole.EXTRA }
 *                     ?.content?.invoke()
 *             }
 *         }
 *         else -> {
 *             // Compact: Single pane with navigation
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * ## KSP Processing
 *
 * KSP generates entries in `GeneratedWrapperRegistry` mapping each pane class
 * to its wrapper function. The registry is used by the hierarchical renderer
 * to resolve which wrapper to use for each `PaneNode`.
 *
 * ## Hierarchical Rendering
 *
 * With the hierarchical rendering engine, the wrapper and its pane contents are
 * composed as a parent-child relationship, enabling:
 * - Coordinated layout transitions between expanded and compact modes
 * - Proper focus management across panes
 * - Unified predictive back gesture handling
 * - State preservation during layout changes
 *
 * @property paneClass The pane container class this wrapper wraps.
 *   Must be a class annotated with [@Pane].
 *
 * @see Pane
 * @see PaneItem
 * @see PaneRole
 * @see AdaptStrategy
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class PaneWrapper(
    /**
     * The pane container class this wrapper wraps.
     * Must be a class annotated with @Pane.
     */
    val paneClass: KClass<*>
)
