package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable

/**
 * Type alias for pane wrapper composables.
 *
 * A [PaneWrapper] is a user-provided composable that defines how multiple
 * panes are arranged in a multi-pane layout. The user controls the overall
 * structure (row arrangement, dividers, sizing) while the library provides
 * the content for each pane.
 *
 * ## Contract
 *
 * - The wrapper receives a [PaneWrapperScope] as receiver for accessing pane state
 * - The wrapper receives `paneContents` list that should be rendered
 * - Each pane's `content()` MUST be called exactly once if the pane is visible
 * - The wrapper is responsible for sizing and positioning panes
 * - The library handles pane content and navigation state
 *
 * ## Example: Side-by-Side Layout
 *
 * ```kotlin
 * val myPaneWrapper: PaneWrapper = { paneContents ->
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         paneContents.filter { it.isVisible }.forEach { pane ->
 *             val weight = when (pane.role) {
 *                 PaneRole.Primary -> 0.65f
 *                 PaneRole.Supporting -> 0.35f
 *                 PaneRole.Extra -> 0.25f
 *             }
 *             Box(
 *                 modifier = Modifier
 *                     .weight(weight)
 *                     .fillMaxHeight()
 *             ) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Example: With Dividers
 *
 * ```kotlin
 * val paneWrapperWithDividers: PaneWrapper = { paneContents ->
 *     val visiblePanes = paneContents.filter { it.isVisible }
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         visiblePanes.forEachIndexed { index, pane ->
 *             if (index > 0) {
 *                 VerticalDivider()
 *             }
 *             Box(modifier = Modifier.weight(1f)) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Integration with PaneNode
 *
 * ```kotlin
 * PaneNode(
 *     primaryPane = primaryPaneConfig,
 *     supportingPane = supportingPaneConfig,
 *     wrapper = myPaneWrapper
 * )
 * ```
 *
 * @see PaneWrapperScope
 * @see PaneContent
 * @see DefaultPaneWrapper
 *
 * @deprecated Use the `@com.jermey.quo.vadis.annotations.PaneWrapper` annotation instead.
 *   This runtime type alias is part of the flattened rendering approach which will be removed
 *   in a future version. Migrate by creating a composable function annotated with `@PaneWrapper`
 *   that has `PaneWrapperScope` as receiver and a `content: @Composable () -> Unit` parameter.
 *   See migration guide: `quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md`
 */
@Deprecated(
    message = "Use @com.jermey.quo.vadis.annotations.PaneWrapper annotation instead. " +
        "This runtime type alias is part of the flattened rendering approach which will be removed.",
    level = DeprecationLevel.WARNING
)
public typealias PaneWrapper = @Composable PaneWrapperScope.(
    paneContents: List<PaneContent>
) -> Unit
