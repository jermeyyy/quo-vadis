package com.jermey.navplayground.demo.ui.screens

import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.tabs.ExploreTab
import com.jermey.navplayground.demo.tabs.HomeTab
import com.jermey.navplayground.demo.tabs.ProfileTab
import com.jermey.navplayground.demo.tabs.SettingsTab
import com.jermey.navplayground.demo.ui.screens.profile.ProfileScreen
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Screen mappings for MainTabs destinations.
 *
 * These wrapper functions map the MainTabs destinations (HomeTab.Tab, ExploreTab.Tab, etc.)
 * to the actual screen composables. This is necessary because the same screens are used
 * by both:
 * - TabDestination.* (used by some navigation flows)
 * - HomeTab.Tab, ExploreTab.Tab, etc. (used by the main tabs navigation tree)
 *
 * The @Screen annotation cannot be applied multiple times to the same function,
 * so we create these thin wrapper functions to register the MainTabs destinations.
 */

/**
 * Screen for HomeTab.Tab - delegates to HomeScreen.
 */
@Screen(HomeTab.Tab::class)
@Composable
fun HomeTabScreen(navigator: Navigator) {
    HomeScreen(navigator = navigator)
}

/**
 * Screen for ExploreTab.Tab - delegates to ExploreScreen.
 */
@Screen(ExploreTab.Tab::class)
@Composable
fun ExploreTabScreen(navigator: Navigator) {
    ExploreScreen(navigator = navigator)
}

/**
 * Screen for ProfileTab.Tab - delegates to ProfileScreen.
 */
@Screen(ProfileTab.Tab::class)
@Composable
fun ProfileTabScreen(navigator: Navigator) {
    ProfileScreen(navigator = navigator)
}

/**
 * Screen for SettingsTab.Tab - delegates to SettingsScreen.
 */
@Screen(SettingsTab.Tab::class)
@Composable
fun SettingsTabScreen(navigator: Navigator) {
    SettingsScreen(navigator = navigator)
}
