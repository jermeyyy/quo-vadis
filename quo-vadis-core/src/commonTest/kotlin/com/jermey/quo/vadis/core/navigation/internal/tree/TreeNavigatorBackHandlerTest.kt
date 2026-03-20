package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalQuoVadisApi::class)
class TreeNavigatorBackHandlerTest {

    // Test destinations
    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private fun createNavigatorWithRegistry(): Pair<TreeNavigator, BackHandlerRegistry> {
        val rootStackKey = NodeKey("root-stack")
        val homeScreen = ScreenNode(
            destination = HomeDestination,
            key = NodeKey("home"),
            parentKey = rootStackKey
        )
        val detailScreen = ScreenNode(
            destination = DetailDestination,
            key = NodeKey("detail"),
            parentKey = rootStackKey
        )
        val rootStack = StackNode(
            key = rootStackKey,
            parentKey = null,
            children = listOf(homeScreen, detailScreen)
        )
        val navigator =
            TreeNavigator(initialState = rootStack, coroutineContext = Dispatchers.Unconfined)
        val registry = BackHandlerRegistry()
        navigator.backHandlerRegistry = registry
        return navigator to registry
    }

    // =========================================================================
    // navigateBack() BYPASSES REGISTRY
    // =========================================================================

    @Test
    fun `navigateBack does not trigger registered back handler`() {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) { handlerCalled = true; true }

        val result = navigator.navigateBack()

        assertTrue(result, "navigateBack should succeed (pop detail)")
        assertFalse(handlerCalled, "navigateBack should NOT consult registry")
    }

    @Test
    fun `navigateBack pops stack even when handler would consume`() {
        val (navigator, registry) = createNavigatorWithRegistry()

        registry.register(NodeKey("detail")) { true } // Would consume if consulted

        val stateBefore = navigator.state.value
        navigator.navigateBack()
        val stateAfter = navigator.state.value

        // State should have changed (detail popped)
        assertTrue(stateBefore != stateAfter, "State should change after navigateBack")
    }

    // =========================================================================
    // onBack() CONSULTS REGISTRY
    // =========================================================================

    @Test
    fun `onBack triggers registered back handler`() {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) {
            handlerCalled = true
            true
        }

        val result = navigator.onBack()

        assertTrue(result, "onBack should return true (handler consumed)")
        assertTrue(handlerCalled, "onBack SHOULD consult registry")
    }

    @Test
    fun `onBack does not pop when handler consumes event`() {
        val (navigator, registry) = createNavigatorWithRegistry()

        registry.register(NodeKey("detail")) { true } // Consumes

        val stateBefore = navigator.state.value
        navigator.onBack()
        val stateAfter = navigator.state.value

        assertEquals(stateBefore, stateAfter, "State should NOT change when handler consumes")
    }

    @Test
    fun `onBack falls through to tree pop when handler returns false`() {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) {
            handlerCalled = true
            false // Does not consume
        }

        val stateBefore = navigator.state.value
        val result = navigator.onBack()
        val stateAfter = navigator.state.value

        assertTrue(result, "onBack should succeed (tree pop)")
        assertTrue(handlerCalled, "Handler should be called")
        assertTrue(stateBefore != stateAfter, "State should change (tree pop happened)")
    }

    @Test
    fun `onBack stops propagation when handler returns true`() {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handler1Called = false
        var handler2Called = false

        // Register on screen key (leaf) - will be checked first
        registry.register(NodeKey("detail")) {
            handler1Called = true
            true // Consumes
        }
        // Register on stack key (parent) - should NOT be reached
        registry.register(NodeKey("root-stack")) {
            handler2Called = true
            true
        }

        navigator.onBack()

        assertTrue(handler1Called, "Leaf handler should be called")
        assertFalse(handler2Called, "Parent handler should NOT be called when leaf consumes")
    }

    // =========================================================================
    // NO REGISTRY SET
    // =========================================================================

    @Test
    fun `onBack without registry falls through to tree pop`() {
        val rootStackKey = NodeKey("root-stack")
        val homeScreen = ScreenNode(
            destination = HomeDestination,
            key = NodeKey("home"),
            parentKey = rootStackKey
        )
        val detailScreen = ScreenNode(
            destination = DetailDestination,
            key = NodeKey("detail"),
            parentKey = rootStackKey
        )
        val rootStack = StackNode(
            key = rootStackKey,
            parentKey = null,
            children = listOf(homeScreen, detailScreen)
        )
        val navigator =
            TreeNavigator(initialState = rootStack, coroutineContext = Dispatchers.Unconfined)
        // Note: no registry set

        val result = navigator.onBack()

        assertTrue(result, "onBack should succeed via tree pop when no registry")
    }
}
