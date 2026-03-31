package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import kotlinx.coroutines.Dispatchers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

@OptIn(InternalQuoVadisApi::class)
class TreeNavigatorBackHandlerTest : FunSpec({

    // Test destinations
    val HomeDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val DetailDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    fun createNavigatorWithRegistry(): Pair<TreeNavigator, BackHandlerRegistry> {
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

    test("navigateBack does not trigger registered back handler") {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) { handlerCalled = true; true }

        val result = navigator.navigateBack()

        result.shouldBeTrue()
        handlerCalled.shouldBeFalse()
    }

    test("navigateBack pops stack even when handler would consume") {
        val (navigator, registry) = createNavigatorWithRegistry()

        registry.register(NodeKey("detail")) { true } // Would consume if consulted

        val stateBefore = navigator.state.value
        navigator.navigateBack()
        val stateAfter = navigator.state.value

        // State should have changed (detail popped)
        stateAfter shouldNotBe stateBefore
    }

    // =========================================================================
    // onBack() CONSULTS REGISTRY
    // =========================================================================

    test("onBack triggers registered back handler") {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) {
            handlerCalled = true
            true
        }

        val result = navigator.onBack()

        result.shouldBeTrue()
        handlerCalled.shouldBeTrue()
    }

    test("onBack does not pop when handler consumes event") {
        val (navigator, registry) = createNavigatorWithRegistry()

        registry.register(NodeKey("detail")) { true } // Consumes

        val stateBefore = navigator.state.value
        navigator.onBack()
        val stateAfter = navigator.state.value

        stateAfter shouldBe stateBefore
    }

    test("onBack falls through to tree pop when handler returns false") {
        val (navigator, registry) = createNavigatorWithRegistry()
        var handlerCalled = false

        registry.register(NodeKey("detail")) {
            handlerCalled = true
            false // Does not consume
        }

        val stateBefore = navigator.state.value
        val result = navigator.onBack()
        val stateAfter = navigator.state.value

        result.shouldBeTrue()
        handlerCalled.shouldBeTrue()
        stateAfter shouldNotBe stateBefore
    }

    test("onBack stops propagation when handler returns true") {
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

        handler1Called.shouldBeTrue()
        handler2Called.shouldBeFalse()
    }

    // =========================================================================
    // NO REGISTRY SET
    // =========================================================================

    test("onBack without registry falls through to tree pop") {
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

        result.shouldBeTrue()
    }
})
