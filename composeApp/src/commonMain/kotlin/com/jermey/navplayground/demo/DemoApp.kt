package com.jermey.navplayground.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.compose.NavigationHost
import com.jermey.quo.vadis.core.compose.util.calculateWindowSizeClass
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import org.koin.compose.koinInject

/**
 * Main entry point for the demo application.
 *
 * Uses the simplified NavigationHost API where config is read from the navigator,
 * eliminating the need to pass config twice.
 *
 * ```kotlin
 * val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
 * // Config is now implicit - read from navigator
 * NavigationHost(navigator)
 * ```
 *
 * ## Navigation Structure
 *
 * ```
 * MainTabs (TabNode)
 * ├── Home (StackNode) → HomeTab.Tab
 * ├── Explore (StackNode) → ExploreTab.Tab
 * ├── Profile (StackNode) → ProfileTab.Tab
 * └── Settings (StackNode) → SettingsTab.Tab
 * ```
 *
 * @see GeneratedNavigationConfig KSP-generated unified config object
 * @see com.jermey.quo.vadis.core.compose.NavigationHost Display navigation content
 */
@Composable
fun DemoApp() {
    val navigator = koinInject<Navigator>()

    // Config is now implicit - NavigationHost reads from navigator
    NavigationHost(
        navigator = navigator,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enablePredictiveBack = true,
        windowSizeClass = calculateWindowSizeClass()
    )
}
