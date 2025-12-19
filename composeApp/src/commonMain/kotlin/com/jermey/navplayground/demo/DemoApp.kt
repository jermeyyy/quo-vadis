package com.jermey.navplayground.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.compose.rememberQuoVadisNavigator
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

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
//    val navigator = rememberQuoVadisNavigator(
//        rootDestination = MainTabs::class,
//        config = GeneratedNavigationConfig
//    )
    val navigator = koinInject<Navigator>()
    val navigationConfig = koinInject<NavigationConfig>()

    NavigationHost(
        navigator = navigator,
        config = navigationConfig,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enablePredictiveBack = true,
    )
}
