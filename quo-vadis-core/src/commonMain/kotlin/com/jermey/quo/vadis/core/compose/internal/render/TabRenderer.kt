@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.LocalContainerNode
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.compose.scope.TabMetadata
import com.jermey.quo.vadis.core.compose.scope.createTabsContainerScope
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.node.TabNode

/**
 * Renders a [TabNode] with wrapper composition and tab switching animations.
 *
 * This renderer maintains the parent-child relationship between tab wrappers
 * and their content, ensuring coordinated animations and predictive back gestures.
 * It creates a [com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope]
 * for the wrapper composable, caches the entire
 * tab structure (wrapper + content), and handles animated transitions between tabs.
 *
 * ## Lifecycle Management
 *
 * The renderer manages the tab container's UI lifecycle via
 * [com.jermey.quo.vadis.core.navigation.LifecycleAwareNode]:
 * - Calls [TabNode.attachToUI] when the composable enters composition
 * - Calls [TabNode.detachFromUI] when the composable leaves composition
 * - Provides [LocalContainerNode] for child screens to access container context
 *
 * ## Wrapper Composition
 *
 * The renderer invokes [com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.TabsContainer]
 * with a content slot. The wrapper is responsible for rendering the tab UI (bottom navigation, tab bar, etc.), while
 * the content slot renders the active tab's content with animations.
 *
 * ## Caching Strategy
 *
 * The **entire** TabNode (wrapper + content) is cached as a single unit using
 * [ComposableCache.CachedEntry]. This ensures that:
 * - The wrapper maintains its state across navigations within tabs
 * - Tab switching animations are smooth
 * - The tab container is not recreated during back navigation
 *
 * ## Tab Switching Animation
 *
 * When the active tab changes, [AnimatedNavContent] animates the transition
 * between tab contents. The animation direction is determined by comparing
 * the previous and current tab indices:
 * - Lower to higher index: Slide left-to-right
 * - Higher to lower index: Slide right-to-left
 * - Same index (initial): Fade in
 *
 * Note: Tab switching does NOT use predictive back gestures. Predictive back
 * is handled at the stack level within each tab.
 *
 * ## Example Structure
 *
 * ```
 * TabRenderer(tabNode)
 *   └── CachedEntry(tabNode.key)
 *         └── TabsContainer(scope, content)
 *               └── AnimatedNavContent(activeStack)
 *                     └── NavTreeRenderer(stack)
 *                           └── StackRenderer/ScreenRenderer...
 * ```
 *
 * @param node The tab node to render
 * @param previousNode The previous tab state for animation direction detection
 * @param scope The render scope with dependencies and context
 *
 * @see TabNode
 * @see com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
 * @see com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.TabsContainer
 * @see AnimatedNavContent
 * @see LocalContainerNode
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.attachToUI
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.detachFromUI
 */
@Composable
internal fun TabRenderer(
    node: TabNode,
    previousNode: TabNode?,
    scope: NavRenderScope,
) {
    // Get active stack for the current and previous state
    val activeStack = node.activeStack
    val previousActiveStack = previousNode?.activeStack

    // Create TabsContainerScope with tab navigation state and actions
    // The scope is remembered to maintain referential stability
    val tabsContainerScope = remember(node.key, node.activeStackIndex, node.tabCount) {
        createTabsContainerScope(
            navigator = scope.navigator,
            activeTabIndex = node.activeStackIndex,
            tabMetadata = createTabMetadataFromStacks(node),
            isTransitioning = false, // Transition state is tracked by AnimatedNavContent
            onSwitchTab = { index ->
                val newState = TreeMutator.switchActiveTab(scope.navigator.state.value, index)
                scope.navigator.updateState(newState)
            }
        )
    }

    // Derive updated scope when active index changes (without recreating)
    val updatedTabsContainerScope by remember(tabsContainerScope) {
        derivedStateOf {
            createTabsContainerScope(
                navigator = scope.navigator,
                activeTabIndex = node.activeStackIndex,
                tabMetadata = createTabMetadataFromStacks(node),
                isTransitioning = false,
                onSwitchTab = { index ->
                    val newState = TreeMutator.switchActiveTab(scope.navigator.state.value, index)
                    scope.navigator.updateState(newState)
                }
            )
        }
    }

    // Cache the ENTIRE TabNode (wrapper + content) as a unit
    // This ensures the wrapper maintains state during navigation
    // Note: During cascade back, the parent StackRenderer's PredictiveBackContent
    // handles animating this TabNode - we just render content normally
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Lifecycle management: attach/detach UI lifecycle
        DisposableEffect(node) {
            node.attachToUI()
            onDispose {
                node.detachFromUI()
            }
        }

        // Provide container node to children for container-scoped operations
        CompositionLocalProvider(
            LocalContainerNode provides node
        ) {
            // No additional animation needed here - parent handles cascade animation
            // Invoke the registered tabs container wrapper (KSP-generated or default)
            // The wrapper receives the scope and a content slot
            // Use wrapperKey for registry lookup (class simple name), fallback to node.key
            scope.containerRegistry.TabsContainer(
                tabNodeKey = node.wrapperKey ?: node.key,
                scope = updatedTabsContainerScope
            ) {
                // Content slot: animate between tabs (within the wrapper)
                // Tab switching uses the default tab transition (typically Fade)
                // The transition is not looked up per-destination since tabs animate between stacks
                AnimatedNavContent(
                    targetState = activeStack,
                    transition = scope.animationCoordinator.defaultTabTransition,
                    isBackNavigation = false,  // Tab switching is never back navigation
                    scope = scope,
                    // Tab switching is NOT via predictive back
                    // Predictive back is handled within each tab's stack
                    predictiveBackEnabled = false,
                    modifier = Modifier
                ) { stack ->
                    // Recurse to render the active stack
                    NavNodeRenderer(
                        node = stack,
                        previousNode = previousActiveStack,
                        scope = scope
                    )
                }
            }
        }
    }
}

/**
 * Creates [TabMetadata] list from a [TabNode].
 *
 * If the [TabNode.tabMetadata] list (from KSP-generated code) is non-empty,
 * it is converted to runtime [TabMetadata] objects. Otherwise, fallback metadata
 * is generated from stack keys and indices.
 *
 * @param node The TabNode to extract metadata from
 * @return List of [TabMetadata] for each tab in order
 */
internal fun createTabMetadataFromStacks(node: TabNode): List<TabMetadata> {
    // Use KSP-generated metadata if available
    if (node.tabMetadata.isNotEmpty()) {
        return node.tabMetadata.map { generated ->
            TabMetadata(
                label = generated.label,
                icon = null, // Icons are resolved by wrapper via route-based fallback
                route = generated.route,
                contentDescription = null,
                badge = null
            )
        }
    }

    // Fallback: generate metadata from stack keys
    return node.stacks.mapIndexed { index, stack ->
        TabMetadata(
            label = stack.key.substringAfterLast("/").takeIf { it.isNotEmpty() }
                ?: "Tab ${index + 1}",
            icon = null,
            route = stack.key,
            contentDescription = null,
            badge = null
        )
    }
}
