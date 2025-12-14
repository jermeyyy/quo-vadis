package com.jermey.navplayground.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.tabs.generated.buildMainTabsNavNode
import com.jermey.quo.vadis.core.navigation.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackMode
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.generated.GeneratedDeepLinkHandlerImpl
import com.jermey.quo.vadis.generated.GeneratedScopeRegistry
import com.jermey.quo.vadis.generated.GeneratedScreenRegistry
import com.jermey.quo.vadis.generated.GeneratedWrapperRegistry

/**
 * Main entry point for the demo application.
 *
 * ## Architecture (New NavNode Tree)
 *
 * This app uses the new **NavNode tree architecture** with KSP-generated components:
 *
 * - **`buildMainTabsNavNode()`**: KSP-generated function that creates the complete
 *   navigation tree from `@Tab` annotations. Returns a `TabNode` with 4 child stacks.
 *
 * - **`TreeNavigator`**: Manages the `StateFlow<NavNode>` tree. Provides navigation
 *   operations (push, pop, switchTab) that produce new immutable tree states.
 *
 * - **`QuoVadisHost`**: Unified composable that renders any NavNode tree structure.
 *   Handles animations, predictive back, and state preservation automatically.
 *
 * - **`GeneratedScreenRegistry`**: KSP-generated object that maps `Destination` types
 *   to `@Screen`-annotated composable functions.
 *
 * - **`GeneratedScopeRegistry`**: KSP-generated object that enables scope-aware navigation.
 *   When navigating from within a TabNode, destinations outside the tab's sealed class
 *   hierarchy are pushed to the parent stack instead of the active tab stack.
 *
 * ## Scope-Aware Navigation
 *
 * With `GeneratedScopeRegistry`, the navigator intelligently routes destinations:
 * - **In-scope destinations** (e.g., HomeTab, ExploreTab) stay within their tab container
 * - **Out-of-scope destinations** (e.g., DetailScreen) navigate above the tab container
 *
 * This preserves the tab container during predictive back gestures, ensuring users
 * can swipe back from detail screens to their original tab position.
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
 * ## Key Benefits
 *
 * 1. **No Manual Graph Registration**: KSP generates everything from annotations
 * 2. **Type-Safe Navigation**: Destinations are data classes/objects, not strings
 * 3. **Automatic State Preservation**: Tab states preserved when switching
 * 4. **Predictive Back Support**: Built-in gesture handling with animations
 * 5. **Shared Element Transitions**: Available via `QuoVadisHostScope`
 * 6. **Scope-Aware Routing**: Destinations automatically route to correct stack level
 *
 * @see buildMainTabsNavNode KSP-generated function from `@Tab` annotation
 * @see GeneratedScreenRegistry KSP-generated from `@Screen` annotations
 * @see GeneratedScopeRegistry KSP-generated from sealed class hierarchies
 * @see NavigationHost Unified navigation host component
 */
@Composable
fun DemoApp() {
    // Step 1: Build navigation tree from KSP-generated function
    // This creates a TabNode with 4 child StackNodes (one per tab)
    val navTree = remember { buildMainTabsNavNode() }

    // Step 2: Create TreeNavigator with the initial tree state and scope registry
    // TreeNavigator manages StateFlow<NavNode> and provides navigation operations
    // GeneratedScopeRegistry enables scope-aware navigation for tab containers
    val navigator = remember {
        TreeNavigator(
            initialState = navTree,
            scopeRegistry = GeneratedScopeRegistry,
            deepLinkHandler = GeneratedDeepLinkHandlerImpl
        )
    }

    // Step 3: Render with NavigationHost
    // The content lambda uses GeneratedScreenRegistry to map destinations to composables
    // GeneratedScopeRegistry ensures out-of-scope destinations navigate above tabs
    NavigationHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        screenRegistry = GeneratedScreenRegistry,
        wrapperRegistry = GeneratedWrapperRegistry,
        scopeRegistry = GeneratedScopeRegistry,
        enablePredictiveBack = true,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE
    )
}
