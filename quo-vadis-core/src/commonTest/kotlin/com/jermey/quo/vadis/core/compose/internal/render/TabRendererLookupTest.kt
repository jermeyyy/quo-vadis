@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import kotlin.test.Test
import kotlin.test.assertEquals

class TabRendererLookupTest {

    @Test
    fun `resolveTabContainerKey prefers normalized scope key over legacy wrapper key`() {
        val homeStack = StackNode(
            key = NodeKey("home-stack"),
            parentKey = NodeKey("runtime-node-key"),
            children = listOf(
                ScreenNode(
                    key = NodeKey("home-screen"),
                    parentKey = NodeKey("home-stack"),
                    destination = HomeDestination
                )
            )
        )
        val node = TabNode(
            key = NodeKey("runtime-node-key"),
            parentKey = null,
            stacks = listOf(homeStack),
            wrapperKey = "com.example.tabs.DemoTabs",
            scopeKey = ScopeKey("demoTabs")
        )

        assertEquals("demoTabs", resolveTabContainerKey(node))
    }

    @Test
    fun `resolveTabContainerKey falls back to scope key before node key`() {
        val homeStack = StackNode(
            key = NodeKey("home-stack"),
            parentKey = NodeKey("runtime-node-key"),
            children = listOf(
                ScreenNode(
                    key = NodeKey("home-screen"),
                    parentKey = NodeKey("home-stack"),
                    destination = HomeDestination
                )
            )
        )
        val node = TabNode(
            key = NodeKey("runtime-node-key"),
            parentKey = null,
            stacks = listOf(homeStack),
            wrapperKey = null,
            scopeKey = ScopeKey("demoTabs")
        )

        assertEquals("demoTabs", resolveTabContainerKey(node))
    }

    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}