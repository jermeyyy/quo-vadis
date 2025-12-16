package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import kotlin.reflect.KClass

/**
 * Represents a registered screen with its composable content.
 *
 * This data class holds the association between a destination class and
 * the composable function that renders its content.
 *
 * @property destinationClass The class of the destination this entry handles
 * @property content The composable function that renders the screen content
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public data class ScreenEntry(
    val destinationClass: KClass<out Destination>,
    val content: @Composable (
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) -> Unit
)

/**
 * Built configuration for a pane's content.
 *
 * Contains the resolved settings from [PaneContentBuilder].
 *
 * @property rootDestination The root destination for the pane, or null if not set
 * @property isAlwaysVisible Whether the pane should always be visible
 */
public data class BuiltPaneContent(
    val rootDestination: Destination?,
    val isAlwaysVisible: Boolean
)

/**
 * Built configuration for a tabs container.
 *
 * Contains the resolved settings from [TabsBuilder].
 *
 * @property tabs List of tab entries in display order
 * @property initialTab Index of the initially selected tab
 */
public data class BuiltTabsConfig(
    val tabs: List<TabEntry>,
    val initialTab: Int
)

/**
 * Built configuration for a panes container.
 *
 * Contains the resolved settings from [PanesBuilder].
 *
 * @property panes Map of pane roles to their configurations
 * @property initialPane The initially active pane role
 * @property backBehavior Back navigation behavior for the container
 */
public data class BuiltPanesConfig(
    val panes: Map<PaneRole, PaneEntry>,
    val initialPane: PaneRole,
    val backBehavior: PaneBackBehavior
)
