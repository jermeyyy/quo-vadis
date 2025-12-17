package com.jermey.navplayground.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.core.navigation.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.compose.rememberQuoVadisNavigator
import com.jermey.quo.vadis.generated.GeneratedNavigationConfig

/**
 * Main entry point for the demo application.
 *
 * ```kotlin
 * val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
 * NavigationHost(
 *     navigator = navigator,
 *     config = GeneratedNavigationConfig
 * )
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
 * @see rememberQuoVadisNavigator Composable navigator creation
 * @see NavigationHost Display navigation content
 */
@Composable
fun DemoApp() {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig
    )

    // Render with NavigationHost using unified config
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enablePredictiveBack = true,
    )
}
