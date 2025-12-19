package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a Composable function as a pane container wrapper for the specified pane destination class.
 *
 * Pane containers provide the adaptive layout chrome (split views, side panels, etc.) for
 * multi-pane navigation containers. The wrapper receives a `PaneContainerScope` that provides
 * access to pane state and layout information, and a content slot where the pane contents
 * are rendered.
 *
 * ## Function Signature Requirements
 *
 * The annotated function must follow this signature:
 * ```kotlin
 * @PaneContainer(CatalogPane::class)
 * @Composable
 * fun CatalogPaneContainer(scope: PaneContainerScope, content: @Composable () -> Unit) {
 *     // Container implementation
 * }
 * ```
 *
 * Parameters (detected by KSP based on types):
 * 1. `scope: PaneContainerScope` - Provides access to pane state and layout
 * 2. `content: @Composable () -> Unit` - The pane contents to render
 *
 * ## PaneContainerScope
 *
 * The scope provides:
 * - `navigator` - The Navigator instance for navigation operations
 * - `paneContents` - List of PaneContentSlot for each pane (role, isVisible, content)
 * - `activePaneRole` - The currently focused pane role
 * - `isExpanded` - Whether the layout is in expanded (multi-pane) mode
 *
 * ## Example: Master-Detail List-Detail Container
 *
 * ```kotlin
 * @PaneContainer(CatalogPane::class)
 * @Composable
 * fun CatalogMasterDetailContainer(
 *     scope: PaneContainerScope,
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
 * ## Example: Three-Pane Email Container
 *
 * ```kotlin
 * @PaneContainer(MailPane::class)
 * @Composable
 * fun MailThreePaneContainer(
 *     scope: PaneContainerScope,
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
 * KSP generates entries in `GeneratedNavigationConfig.containerRegistry` mapping each pane class
 * to its container function. The registry is used by the hierarchical renderer
 * to resolve which container to use for each `PaneNode`.
 *
 * ## Hierarchical Rendering
 *
 * With the hierarchical rendering engine, the container and its pane contents are
 * composed as a parent-child relationship, enabling:
 * - Coordinated layout transitions between expanded and compact modes
 * - Proper focus management across panes
 * - Unified predictive back gesture handling
 * - State preservation during layout changes
 *
 * @property paneClass The pane container class this container wraps.
 *   Must be a class annotated with [@Pane].
 *
 * @see Pane
 * @see PaneItem
 * @see PaneRole
 * @see AdaptStrategy
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PaneContainer(
    /**
     * The pane container class this container wraps.
     * Must be a class annotated with @Pane.
     */
    val paneClass: KClass<*>
)