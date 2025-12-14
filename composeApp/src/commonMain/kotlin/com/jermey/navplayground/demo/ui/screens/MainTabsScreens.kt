package com.jermey.navplayground.demo.ui.screens

import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.tabs.MainTabs
import com.jermey.navplayground.demo.ui.screens.profile.ProfileScreen
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Screen mappings for MainTabs destinations.
 *
 * These wrapper functions map the MainTabs flat tab destinations to the actual
 * screen composables. With the new mixed tab types pattern:
 * - HomeTab, ExploreTab, ProfileTab are FLAT_SCREEN tabs (data objects with @Destination)
 * - SettingsTab is a NESTED_STACK tab (sealed class with @Stack)
 *
 * The @Screen annotations bind these destinations to their composable content.
 */

/**
 * Screen for MainTabs.HomeTab - displays the home content.
 */
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeTabScreen(navigator: Navigator) {
    HomeScreen(navigator = navigator)
}

/**
 * Screen for MainTabs.ExploreTab - displays the explore content.
 */
@Screen(MainTabs.ExploreTab::class)
@Composable
fun ExploreTabScreen(navigator: Navigator) {
    ExploreScreen(navigator = navigator)
}

/**
 * Screen for MainTabs.ProfileTab - displays the profile content.
 */
@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileTabScreen(navigator: Navigator) {
    ProfileScreen(navigator = navigator)
}

/**
 * Screen for MainTabs.SettingsTab.Main - the root settings screen.
 *
 * Note: SettingsTab is a NESTED_STACK, so we bind to its start destination.
 */
@Screen(MainTabs.SettingsTab.Main::class)
@Composable
fun SettingsTabScreen(navigator: Navigator) {
    SettingsScreen(navigator = navigator)
}
