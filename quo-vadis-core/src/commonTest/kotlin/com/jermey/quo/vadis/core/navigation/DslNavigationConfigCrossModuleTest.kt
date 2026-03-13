@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.dsl.navigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for cross-module container reference resolution in
 * [com.jermey.quo.vadis.core.dsl.DslNavigationConfig] and
 * [com.jermey.quo.vadis.core.navigation.config.CompositeNavigationConfig].
 *
 * These tests verify that [com.jermey.quo.vadis.core.dsl.TabEntry.ContainerReference]
 * resolves correctly both within a single config and across composed configs.
 */
class DslNavigationConfigCrossModuleTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private sealed interface MainTabs : NavDestination {
        data object Tab1 : MainTabs
        data object Tab2 : MainTabs
    }

    private sealed interface OuterTabs : NavDestination {
        data object Tab1 : OuterTabs
    }

    private sealed interface InnerTabs : NavDestination {
        data object TabA : InnerTabs
        data object TabB : InnerTabs
    }

    private sealed interface FeatureStack : NavDestination {
        data object Home : FeatureStack
    }

    private sealed interface OtherStack : NavDestination {
        data object Screen1 : OtherStack
    }

    private sealed interface FeatureFromC : NavDestination {
        data object Root : FeatureFromC
    }

    private sealed interface UnregisteredType : NavDestination

    // =========================================================================
    // 1. buildNavNode builds TabNode for Tabs container
    // =========================================================================

    @Test
    fun `buildNavNode builds TabNode for Tabs container`() {
        // Arrange
        val config = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                tab(MainTabs.Tab2)
            }
        }

        // Act
        val node = config.buildNavNode(MainTabs::class, "root", null)

        // Assert
        assertNotNull(node)
        assertTrue(node is TabNode, "Expected TabNode but was ${node::class.simpleName}")
        val tabNode = node as TabNode
        assertEquals(2, tabNode.stacks.size)
    }

    // =========================================================================
    // 2. buildNavNode builds StackNode for Stack container
    // =========================================================================

    @Test
    fun `buildNavNode builds StackNode for Stack container`() {
        // Arrange
        val config = navigationConfig {
            stack<FeatureStack> {
                screen(FeatureStack.Home)
            }
        }

        // Act
        val node = config.buildNavNode(FeatureStack::class, "feature", null)

        // Assert
        assertNotNull(node)
        assertTrue(node is StackNode, "Expected StackNode but was ${node::class.simpleName}")
        val stackNode = node as StackNode
        assertEquals(1, stackNode.children.size)
        assertTrue(stackNode.children[0] is ScreenNode)
    }

    // =========================================================================
    // 3. ContainerReference in same config resolves correctly
    // =========================================================================

    @Test
    fun `ContainerReference in same config resolves correctly`() {
        // Arrange
        val config = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                containerTab<FeatureStack>()
            }
            stack<FeatureStack> {
                screen(FeatureStack.Home)
            }
        }

        // Act
        val node = config.buildNavNode(MainTabs::class, "root", null)

        // Assert
        assertNotNull(node)
        assertTrue(node is TabNode)
        val tabNode = node as TabNode
        assertEquals(2, tabNode.stacks.size)

        // Tab 0: flat screen tab
        val tab0Stack = tabNode.stacks[0]
        assertTrue(tab0Stack.children[0] is ScreenNode)

        // Tab 1: containerTab referencing FeatureStack → resolved StackNode is used directly
        val tab1Stack = tabNode.stacks[1]
        assertEquals(1, tab1Stack.children.size)
        assertTrue(
            tab1Stack.children[0] is ScreenNode,
            "ContainerReference stack tab should contain the resolved stack's ScreenNode directly"
        )
    }

    // =========================================================================
    // 4. ContainerReference pointing to Tabs creates TabNode inside tab
    // =========================================================================

    @Test
    fun `ContainerReference pointing to Tabs creates TabNode inside tab`() {
        // Arrange
        val config = navigationConfig {
            tabs<OuterTabs> {
                containerTab<InnerTabs>()
            }
            tabs<InnerTabs> {
                tab(InnerTabs.TabA)
                tab(InnerTabs.TabB)
            }
        }

        // Act
        val node = config.buildNavNode(OuterTabs::class, "outer", null)

        // Assert - tree structure: TabNode → StackNode → TabNode
        assertNotNull(node)
        assertTrue(node is TabNode)
        val outerTab = node as TabNode
        assertEquals(1, outerTab.stacks.size)

        val wrapperStack = outerTab.stacks[0]
        assertEquals(1, wrapperStack.children.size)
        assertTrue(
            wrapperStack.children[0] is TabNode,
            "ContainerReference to tabs should produce nested TabNode"
        )
        val innerTab = wrapperStack.children[0] as TabNode
        assertEquals(2, innerTab.stacks.size)
    }

    // =========================================================================
    // 5. CompositeNavigationConfig resolves cross-config ContainerReference
    // =========================================================================

    @Test
    fun `CompositeNavigationConfig resolves cross-config ContainerReference`() {
        // Arrange
        val configA = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                containerTab<FeatureStack>()
            }
        }
        val configB = navigationConfig {
            stack<FeatureStack> {
                screen(FeatureStack.Home)
            }
        }
        val composite = configA + configB

        // Act
        val node = composite.buildNavNode(MainTabs::class, "root", null)

        // Assert
        assertNotNull(node)
        assertTrue(node is TabNode)
        val tabNode = node as TabNode
        assertEquals(2, tabNode.stacks.size)

        // Tab 1: ContainerReference should resolve to FeatureStack from configB
        // The resolved StackNode is used directly as the tab's stack (no double wrapping)
        val tab1Stack = tabNode.stacks[1]
        assertEquals(1, tab1Stack.children.size)
        assertTrue(
            tab1Stack.children[0] is ScreenNode,
            "Cross-config ContainerReference should resolve with ScreenNode child directly"
        )
    }

    // =========================================================================
    // 6. CompositeNavigationConfig resolves cross-config tabs within tabs
    // =========================================================================

    @Test
    fun `CompositeNavigationConfig resolves cross-config tabs within tabs`() {
        // Arrange
        val configA = navigationConfig {
            tabs<OuterTabs> {
                containerTab<InnerTabs>()
            }
        }
        val configB = navigationConfig {
            tabs<InnerTabs> {
                tab(InnerTabs.TabA)
                tab(InnerTabs.TabB)
            }
        }
        val composite = configA + configB

        // Act
        val node = composite.buildNavNode(OuterTabs::class, "outer", null)

        // Assert - tree: TabNode → StackNode → TabNode
        assertNotNull(node)
        assertTrue(node is TabNode)
        val outerTab = node as TabNode
        assertEquals(1, outerTab.stacks.size)

        val wrapperStack = outerTab.stacks[0]
        assertEquals(1, wrapperStack.children.size)
        assertTrue(
            wrapperStack.children[0] is TabNode,
            "Cross-config ContainerReference to tabs should produce nested TabNode"
        )
        val innerTab = wrapperStack.children[0] as TabNode
        assertEquals(2, innerTab.stacks.size)
    }

    // =========================================================================
    // 7. Nested composite (A + B) + C resolves correctly
    // =========================================================================

    @Test
    fun `Nested composite A plus B plus C resolves correctly`() {
        // Arrange
        val configA = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                containerTab<FeatureFromC>()
            }
        }
        val configB = navigationConfig {
            stack<OtherStack> {
                screen(OtherStack.Screen1)
            }
        }
        val configC = navigationConfig {
            stack<FeatureFromC> {
                screen(FeatureFromC.Root)
            }
        }
        val composite = (configA + configB) + configC

        // Act
        val node = composite.buildNavNode(MainTabs::class, "root", null)

        // Assert
        assertNotNull(node)
        assertTrue(node is TabNode)
        val tabNode = node as TabNode
        assertEquals(2, tabNode.stacks.size)

        // Tab 1: ContainerReference should resolve FeatureFromC from configC
        // The resolved StackNode is used directly as the tab's stack (no double wrapping)
        val tab1Stack = tabNode.stacks[1]
        assertEquals(1, tab1Stack.children.size)
        assertTrue(
            tab1Stack.children[0] is ScreenNode,
            "Nested composite should resolve ContainerReference from configC"
        )
    }

    // =========================================================================
    // 8. Unknown ContainerReference returns empty children gracefully
    // =========================================================================

    @Test
    fun `Unknown ContainerReference returns empty children gracefully`() {
        // Arrange
        val config = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                containerTab<UnregisteredType>()
            }
        }

        // Act
        val node = config.buildNavNode(MainTabs::class, "root", null)

        // Assert - should not crash; unresolved tab stack has empty children
        assertNotNull(node)
        assertTrue(node is TabNode)
        assertEquals(2, node.stacks.size)

        val unresolvedTabStack = node.stacks[1]
        assertTrue(
            unresolvedTabStack.children.isEmpty(),
            "Unresolvable ContainerReference should produce empty children"
        )
    }

    // =========================================================================
    // 9. Non-composite config behavior unchanged with nodeResolver null
    // =========================================================================

    @Test
    fun `Non-composite config behavior unchanged with nodeResolver null`() {
        // Arrange - single config, no composition
        val config = navigationConfig {
            tabs<MainTabs> {
                tab(MainTabs.Tab1)
                tab(MainTabs.Tab2)
            }
        }

        // Act
        val node = config.buildNavNode(MainTabs::class, "root", null)

        // Assert - standard tab behavior, no nodeResolver involved
        assertNotNull(node)
        assertTrue(node is TabNode)
        assertEquals(2, node.stacks.size)

        // Both tabs should have their screen nodes
        val tab0 = node.stacks[0]
        assertEquals(1, tab0.children.size)
        assertTrue(tab0.children[0] is ScreenNode)
        assertEquals(MainTabs.Tab1, (tab0.children[0] as ScreenNode).destination)

        val tab1 = node.stacks[1]
        assertEquals(1, tab1.children.size)
        assertTrue(tab1.children[0] is ScreenNode)
        assertEquals(MainTabs.Tab2, (tab1.children[0] as ScreenNode).destination)
    }
}
