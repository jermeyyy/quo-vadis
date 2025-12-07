@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// ============================================================
// MIG-001: SIMPLE STACK NAVIGATION RECIPE
// ============================================================

/**
 * # Simple Stack Navigation Recipe
 *
 * Demonstrates basic linear (push/pop) navigation using the NavNode architecture.
 * This is the fundamental navigation pattern for settings screens, single-path flows,
 * and any navigation following a strict back-stack model.
 *
 * ## What This Recipe Shows
 *
 * 1. **Destination Definition** - Using `@Stack` and `@Destination` annotations
 * 2. **Screen Binding** - Using `@Screen` annotation to map destinations to composables
 * 3. **Navigation Operations** - Push (`navigate`), pop (`navigateBack`)
 * 4. **App Setup** - Using `QuoVadisHost` with KSP-generated components
 *
 * ## Migration Summary (Old API → New API)
 *
 * | Old API | New API | Purpose |
 * |---------|---------|---------|
 * | `@Graph("name")` | `@Stack(name = "name")` | Container annotation |
 * | `@Route("path")` | `@Destination(route = "path")` | Screen route definition |
 * | `@Content(Dest::class)` | `@Screen(Dest::class)` | Screen-to-composable binding |
 * | `GraphNavHost(...)` | `QuoVadisHost(...)` | Navigation host component |
 * | `initializeQuoVadisRoutes()` | _(removed)_ | KSP handles automatically |
 * | `navigator.registerGraph(graph)` | _(removed)_ | Tree passed to rememberNavigator |
 * | `navigator.setStartDestination(...)` | _(removed)_ | Defined in @Stack annotation |
 *
 * ## Key Migration Steps
 *
 * 1. **Rename annotation**: `@Graph` → `@Stack`
 * 2. **Rename annotation**: `@Route` → `@Destination`
 * 3. **Rename annotation**: `@Content` → `@Screen`
 * 4. **Remove manual setup**: No more `initializeQuoVadisRoutes()`, `registerGraph()`, `setStartDestination()`
 * 5. **Replace host**: `GraphNavHost` → `QuoVadisHost` with `screenRegistry`
 * 6. **Use generated code**: `buildSettingsNavNode()` and `GeneratedScreenRegistry`
 *
 * ## Production App Setup (After KSP Processing)
 *
 * ```kotlin
 * @Composable
 * fun SettingsApp() {
 *     // KSP generates buildSettingsNavNode() from @Stack annotation
 *     val navTree = remember { buildSettingsNavNode() }
 *
 *     // Create navigator with the initial tree
 *     val navigator = rememberNavigator(navTree)
 *
 *     // QuoVadisHost renders based on navigator state
 *     // screenRegistry is KSP-generated from @Screen annotations
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry
 *     )
 * }
 * ```
 *
 * @see SettingsDestination for destination definitions
 * @see com.jermey.quo.vadis.annotations.Stack
 * @see com.jermey.quo.vadis.annotations.Destination
 * @see com.jermey.quo.vadis.annotations.Screen
 */

// ============================================================
// DESTINATION DEFINITION
// ============================================================

/**
 * Settings navigation destinations using the NavNode API.
 *
 * ## Annotation Usage
 *
 * The `@Stack` annotation on the sealed class:
 * - `name` - Unique identifier for this navigation stack
 * - `startDestination` - Class name (not route) of the initial destination
 *
 * The `@Destination` annotation on each subclass:
 * - `route` - Deep link path for this destination
 *
 * ## Migration from Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Graph("settings", startDestination = "settings/main")
 * sealed class SettingsDestination : Destination {
 *     @Route("settings/main")
 *     data object Main : SettingsDestination()
 * }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Stack(name = "settings", startDestination = "Main")
 * sealed class SettingsDestination : DestinationInterface {
 *     @Destination(route = "settings/main")
 *     data object Main : SettingsDestination()
 * }
 * ```
 *
 * ## Key Differences
 *
 * 1. `@Graph` → `@Stack` - Clearer semantics for linear navigation
 * 2. `@Route` → `@Destination` - Unified destination annotation
 * 3. `startDestination` uses simple class name (`"Main"`) not route (`"settings/main"`)
 */
@Stack(name = "settings", startDestination = "Main")
sealed class SettingsDestination : DestinationInterface {

    /**
     * Main settings screen - the entry point for settings navigation.
     *
     * Defined as `startDestination = "Main"` in the @Stack annotation.
     */
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()

    /**
     * Account settings screen for user profile and login options.
     */
    @Destination(route = "settings/account")
    data object Account : SettingsDestination()

    /**
     * Notification settings screen for push notification preferences.
     */
    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()
}

// ============================================================
// SCREENS
// ============================================================

/**
 * Main settings screen - entry point for settings navigation.
 *
 * Demonstrates navigation to child screens via [Navigator.navigate].
 *
 * ## Screen Binding
 *
 * The `@Screen` annotation binds this composable to `SettingsDestination.Main`.
 * KSP generates a registry entry mapping the destination class to this function.
 *
 * ## Migration from Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Content(SettingsDestination.Main::class)
 * @Composable
 * fun SettingsMainContent(navigator: Navigator) { ... }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Screen(SettingsDestination.Main::class)
 * @Composable
 * fun SettingsMainScreen(navigator: Navigator) { ... }
 * ```
 *
 * @param navigator The navigator for navigation actions
 */
@Screen(SettingsDestination.Main::class)
@Composable
fun SettingsMainScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Push Account screen onto stack
        Button(onClick = { navigator.navigate(SettingsDestination.Account) }) {
            Text("Account Settings")
        }

        // Push Notifications screen onto stack
        Button(onClick = { navigator.navigate(SettingsDestination.Notifications) }) {
            Text("Notification Settings")
        }
    }
}

/**
 * Account settings screen for user profile management.
 *
 * Demonstrates back navigation via [Navigator.navigateBack].
 *
 * @param navigator The navigator for navigation actions
 */
@Screen(SettingsDestination.Account::class)
@Composable
fun AccountScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Account Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Manage your profile and login options.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pop this screen from the stack
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to Settings")
        }
    }
}

/**
 * Notification settings screen for push notification preferences.
 *
 * Demonstrates back navigation via [Navigator.navigateBack].
 *
 * @param navigator The navigator for navigation actions
 */
@Screen(SettingsDestination.Notifications::class)
@Composable
fun NotificationsScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Notification Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Configure how and when you receive notifications.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pop this screen from the stack
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to Settings")
        }
    }
}

// ============================================================
// APP ENTRY POINT (Conceptual - requires KSP generation)
// ============================================================

/**
 * Entry point for the Simple Stack Navigation recipe.
 *
 * ## Production Setup (After KSP Processing)
 *
 * ```kotlin
 * @Composable
 * fun SettingsApp() {
 *     // KSP generates buildSettingsNavNode() from @Stack annotation
 *     val navTree = remember { buildSettingsNavNode() }
 *
 *     // Create navigator with the initial tree
 *     val navigator = rememberNavigator(navTree)
 *
 *     // QuoVadisHost renders based on navigator state
 *     // GeneratedScreenRegistry is KSP-generated from @Screen annotations
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry
 *     )
 * }
 * ```
 *
 * ## Comparison with Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Composable
 * fun SettingsApp() {
 *     // Manual initialization required
 *     remember { initializeQuoVadisRoutes() }
 *
 *     val navigator = rememberNavigator()
 *     val graph = remember { settingsGraph() }
 *
 *     // Manual registration and start destination setup
 *     LaunchedEffect(navigator, graph) {
 *         navigator.registerGraph(graph)
 *         navigator.setStartDestination(SettingsDestination.Main)
 *     }
 *
 *     GraphNavHost(
 *         graph = graph,
 *         navigator = navigator,
 *         defaultTransition = NavigationTransitions.SlideHorizontal
 *     )
 * }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Composable
 * fun SettingsApp() {
 *     val navTree = remember { buildSettingsNavNode() }  // KSP-generated
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry  // KSP-generated
 *     )
 * }
 * ```
 *
 * ## Key Improvements
 *
 * 1. **Less Boilerplate**: No manual initialization or registration
 * 2. **Type Safety**: Start destination validated at compile time
 * 3. **Single Host**: QuoVadisHost handles all node types
 * 4. **Automatic Binding**: @Screen annotations generate registry
 */
@Composable
fun SettingsStackApp() {
    // This is a conceptual placeholder.
    // In production, KSP generates:
    // - buildSettingsNavNode() from @Stack/@Destination annotations
    // - GeneratedScreenRegistry from @Screen annotations
    //
    // Usage:
    //   val navTree = remember { buildSettingsNavNode() }
    //   val navigator = rememberNavigator(navTree)
    //   QuoVadisHost(navigator = navigator, screenRegistry = GeneratedScreenRegistry)

    Text("SettingsStackApp - See KDoc for production implementation")
}
