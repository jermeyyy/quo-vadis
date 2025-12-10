package com.jermey.navplayground.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.tabs.generated.buildMainTabsNavNode
import com.jermey.quo.vadis.core.navigation.compose.HierarchicalQuoVadisHost
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.generated.GeneratedScreenRegistry

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
 *
 * @see buildMainTabsNavNode KSP-generated function from `@Tab` annotation
 * @see GeneratedScreenRegistry KSP-generated from `@Screen` annotations
 * @see QuoVadisHost Unified navigation host component
 */
@Composable
fun DemoApp() {
    // Step 1: Build navigation tree from KSP-generated function
    // This creates a TabNode with 4 child StackNodes (one per tab)
    val navTree = remember { buildMainTabsNavNode() }

    // Step 2: Create TreeNavigator with the initial tree state
    // TreeNavigator manages StateFlow<NavNode> and provides navigation operations
    val navigator = remember { TreeNavigator(initialState = navTree) }

    // Step 4: Render with QuoVadisHost
    // The content lambda uses GeneratedScreenRegistry to map destinations to composables
    // renderingMode toggles between Flattened (stable) and Hierarchical (experimental)
    HierarchicalQuoVadisHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        screenRegistry = GeneratedScreenRegistry,
        enablePredictiveBack = true,
    )
//    { destination ->
//        // GeneratedScreenRegistry.Content renders the @Screen-annotated composable
//        // for the given destination. 'this' provides QuoVadisHostScope which extends
//        // SharedTransitionScope for shared element transitions.
//        GeneratedScreenRegistry.Content(
//            destination = destination,
//            navigator = navigator,
//            sharedTransitionScope = this,
//            animatedVisibilityScope = null
//        )
//    }
}
