@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator

/**
 * Test helper extensions for Navigator initialization.
 *
 * These helpers provide convenient ways to create navigators with initial state,
 * replacing the deprecated `setStartDestination()` pattern in tests.
 *
 * ## Usage Examples
 *
 * Simple destination setup:
 * ```kotlin
 * val navigator = TreeNavigator.withDestination(HomeDestination)
 * ```
 *
 * Custom state setup:
 * ```kotlin
 * val navigator = TreeNavigator.withState(
 *     StackNode.singleScreen(HomeDestination)
 * )
 * ```
 *
 * Complex state setup:
 * ```kotlin
 * val customStack = StackNode(
 *     key = "root",
 *     parentKey = null,
 *     children = listOf(
 *         ScreenNode(key = "home", parentKey = "root", destination = HomeDestination),
 *         ScreenNode(key = "detail", parentKey = "root", destination = DetailDestination)
 *     )
 * )
 * val navigator = TreeNavigator.withState(customStack)
 * ```
 */

/**
 * Creates a TreeNavigator initialized with a single destination.
 * Useful for simple test cases that don't need complex navigation state.
 *
 * @param destination The initial destination to navigate to
 * @return A TreeNavigator with the destination as its initial state
 */
@Suppress("UnusedReceiverParameter")
fun TreeNavigator.Companion.withDestination(destination: NavDestination): TreeNavigator {
    val stackKey = NavKeyGenerator.generate()
    val screenKey = NavKeyGenerator.generate()
    val screenNode = ScreenNode(
        key = screenKey,
        parentKey = stackKey,
        destination = destination
    )
    val rootStack = StackNode(
        key = stackKey,
        parentKey = null,
        children = listOf(screenNode)
    )
    return TreeNavigator(initialState = rootStack)
}

/**
 * Creates a TreeNavigator initialized with a custom NavNode tree.
 * Useful for complex test cases that need specific navigation state.
 *
 * @param initialState The root NavNode to use as initial state
 * @return A TreeNavigator with the provided initial state
 */
@Suppress("UnusedReceiverParameter")
fun TreeNavigator.Companion.withState(initialState: NavNode): TreeNavigator {
    return TreeNavigator(initialState = initialState)
}

/**
 * Creates a simple root StackNode containing a single screen.
 * Useful for creating test states.
 *
 * @param destination The destination for the screen
 * @return A StackNode containing a single screen
 */
@Suppress("UnusedReceiverParameter")
fun StackNode.Companion.singleScreen(destination: NavDestination): StackNode {
    val stackKey = NavKeyGenerator.generate()
    val screenKey = NavKeyGenerator.generate()
    val screenNode = ScreenNode(
        key = screenKey,
        parentKey = stackKey,
        destination = destination
    )
    return StackNode(
        key = stackKey,
        parentKey = null,
        children = listOf(screenNode)
    )
}
