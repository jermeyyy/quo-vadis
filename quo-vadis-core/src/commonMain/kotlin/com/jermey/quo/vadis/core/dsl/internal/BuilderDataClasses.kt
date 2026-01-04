package com.jermey.quo.vadis.core.dsl.internal

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.dsl.PaneEntry
import com.jermey.quo.vadis.core.dsl.TabEntry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.reflect.KClass

/**
 * Represents a registered screen with its composable content.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Screen entries are managed internally by the navigation system.
 *
 * This data class holds the association between a destination class and
 * the composable function that renders its content.
 *
 * @property destinationClass The class of the destination this entry handles
 * @property content The composable function that renders the screen content
 */
@InternalQuoVadisApi
@OptIn(ExperimentalSharedTransitionApi::class)
data class ScreenEntry(
    val destinationClass: KClass<out NavDestination>,
    val content: @Composable (
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?,
    ) -> Unit
)

/**
 * Built configuration for a pane's content.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Built pane configurations are managed internally by the navigation system.
 *
 * Contains the resolved settings from [PaneContentBuilder].
 *
 * @property rootDestination The root destination for the pane, or null if not set
 * @property isAlwaysVisible Whether the pane should always be visible
 */
@InternalQuoVadisApi
data class BuiltPaneContent(
    val rootDestination: NavDestination?,
    val isAlwaysVisible: Boolean
)

/**
 * Built configuration for a tabs container.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Built tabs configurations are managed internally by the navigation system.
 *
 * Contains the resolved settings from [TabsBuilder].
 *
 * @property tabs List of tab entries in display order
 * @property initialTab Index of the initially selected tab
 */
@InternalQuoVadisApi
data class BuiltTabsConfig(
    val tabs: List<TabEntry>,
    val initialTab: Int
)

/**
 * Built configuration for a panes container.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Built panes configurations are managed internally by the navigation system.
 *
 * Contains the resolved settings from [PanesBuilder].
 *
 * @property panes Map of pane roles to their configurations
 * @property initialPane The initially active pane role
 * @property backBehavior Back navigation behavior for the container
 */
@InternalQuoVadisApi
data class BuiltPanesConfig(
    val panes: Map<PaneRole, PaneEntry>,
    val initialPane: PaneRole,
    val backBehavior: PaneBackBehavior
)
