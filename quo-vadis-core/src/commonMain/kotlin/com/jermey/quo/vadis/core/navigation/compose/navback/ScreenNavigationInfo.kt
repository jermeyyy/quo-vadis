package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.navigationevent.NavigationEventInfo

/**
 * NavigationEventInfo implementation for Quo Vadis screens.
 * Provides metadata about the current screen for predictive back animations.
 */
public data class ScreenNavigationInfo(
    /** Unique identifier for the screen */
    val screenId: String,
    /** Optional display name for the screen */
    val displayName: String? = null,
    /** Optional route pattern */
    val route: String? = null
) : NavigationEventInfo()

/**
 * Represents "no screen" info for when we're at the root.
 */
public data object NoScreenInfo : NavigationEventInfo()
