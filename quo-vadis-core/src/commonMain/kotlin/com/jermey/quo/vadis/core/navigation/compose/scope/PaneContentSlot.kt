package com.jermey.quo.vadis.core.navigation.compose.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Represents a content slot within a pane layout.
 *
 * Used by the hierarchical renderer to provide structured content slots
 * to pane wrapper composables. Each slot corresponds to a semantic role
 * in an adaptive layout (Primary, Supporting, Extra).
 *
 * ## Purpose
 *
 * In adaptive layouts, screens may need to render multiple panes
 * simultaneously based on available space. PaneContentSlot encapsulates
 * the content for each pane along with visibility state, allowing
 * wrapper composables to arrange panes appropriately.
 *
 * ## Usage in PaneWrapperScope
 *
 * ```kotlin
 * @Composable
 * fun MyPaneWrapper(scope: PaneWrapperScope) {
 *     Row {
 *         scope.slots.filter { it.isVisible }.forEach { slot ->
 *             when (slot.role) {
 *                 PaneRole.Primary -> PrimaryPane { slot.content() }
 *                 PaneRole.Supporting -> SupportingPane { slot.content() }
 *                 PaneRole.Extra -> ExtraPane { slot.content() }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Visibility Management
 *
 * The [isVisible] property is determined by the current adaptive state
 * and [AdaptStrategy][com.jermey.quo.vadis.core.navigation.core.AdaptStrategy].
 * Wrappers should respect this flag to properly hide/show panes during
 * screen size changes.
 *
 * @property role The semantic role of this pane slot (Primary, Supporting, Extra)
 * @property isVisible Whether this slot should be visible in current layout configuration
 * @property content The composable content for this slot
 *
 * @see PaneRole
 * @see com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
 */
@Stable
public data class PaneContentSlot(
    val role: PaneRole,
    val isVisible: Boolean,
    val content: @Composable () -> Unit
)
