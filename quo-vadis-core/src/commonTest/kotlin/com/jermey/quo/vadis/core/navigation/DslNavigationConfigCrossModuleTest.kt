@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.dsl.navigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for cross-module container reference resolution in
 * [com.jermey.quo.vadis.core.dsl.DslNavigationConfig] and
 * [com.jermey.quo.vadis.core.navigation.config.CompositeNavigationConfig].
 *
 * These tests verify that [com.jermey.quo.vadis.core.dsl.TabEntry.ContainerReference]
 * resolves correctly both within a single config and across composed configs.
 */
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

class DslNavigationConfigCrossModuleTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================








    // =========================================================================
    // 1. buildNavNode builds TabNode for Tabs container
    // =========================================================================

    test("buildNavNode builds TabNode for Tabs container") {
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
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks.size shouldBe 2
    }

    // =========================================================================
    // 2. buildNavNode builds StackNode for Stack container
    // =========================================================================

    test("buildNavNode builds StackNode for Stack container") {
        // Arrange
        val config = navigationConfig {
            stack<FeatureStack> {
                screen(FeatureStack.Home)
            }
        }

        // Act
        val node = config.buildNavNode(FeatureStack::class, "feature", null)

        // Assert
        node.shouldNotBeNull()
        val stackNode = node as StackNode
        stackNode.children.size shouldBe 1
        stackNode.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    // =========================================================================
    // 3. ContainerReference in same config resolves correctly
    // =========================================================================

    test("ContainerReference in same config resolves correctly") {
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
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks.size shouldBe 2

        // Tab 0: flat screen tab
        val tab0Stack = tabNode.stacks[0]
        tab0Stack.children[0].shouldBeInstanceOf<ScreenNode>()

        // Tab 1: containerTab referencing FeatureStack → resolved StackNode is used directly
        val tab1Stack = tabNode.stacks[1]
        tab1Stack.children.size shouldBe 1
        tab1Stack.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    // =========================================================================
    // 4. ContainerReference pointing to Tabs creates TabNode inside tab
    // =========================================================================

    test("ContainerReference pointing to Tabs creates TabNode inside tab") {
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
        node.shouldNotBeNull()
        val outerTab = node as TabNode
        outerTab.stacks.size shouldBe 1

        val wrapperStack = outerTab.stacks[0]
        wrapperStack.children.size shouldBe 1
        val innerTab = wrapperStack.children[0] as TabNode
        innerTab.stacks.size shouldBe 2
    }

    // =========================================================================
    // 5. CompositeNavigationConfig resolves cross-config ContainerReference
    // =========================================================================

    test("CompositeNavigationConfig resolves cross-config ContainerReference") {
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
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks.size shouldBe 2

        // Tab 1: ContainerReference should resolve to FeatureStack from configB
        // The resolved StackNode is used directly as the tab's stack (no double wrapping)
        val tab1Stack = tabNode.stacks[1]
        tab1Stack.children.size shouldBe 1
        tab1Stack.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    // =========================================================================
    // 6. CompositeNavigationConfig resolves cross-config tabs within tabs
    // =========================================================================

    test("CompositeNavigationConfig resolves cross-config tabs within tabs") {
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
        node.shouldNotBeNull()
        val outerTab = node as TabNode
        outerTab.stacks.size shouldBe 1

        val wrapperStack = outerTab.stacks[0]
        wrapperStack.children.size shouldBe 1
        val innerTab = wrapperStack.children[0] as TabNode
        innerTab.stacks.size shouldBe 2
    }

    // =========================================================================
    // 7. Nested composite (A + B) + C resolves correctly
    // =========================================================================

    test("Nested composite A plus B plus C resolves correctly") {
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
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.stacks.size shouldBe 2

        // Tab 1: ContainerReference should resolve FeatureFromC from configC
        // The resolved StackNode is used directly as the tab's stack (no double wrapping)
        val tab1Stack = tabNode.stacks[1]
        tab1Stack.children.size shouldBe 1
        tab1Stack.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    // =========================================================================
    // 8. Unknown ContainerReference returns empty children gracefully
    // =========================================================================

    test("Unknown ContainerReference returns empty children gracefully") {
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
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks.size shouldBe 2

        val unresolvedTabStack = tabNode.stacks[1]
        unresolvedTabStack.children.isEmpty().shouldBeTrue()
    }

    // =========================================================================
    // 9. Non-composite config behavior unchanged with nodeResolver null
    // =========================================================================

    test("Non-composite config behavior unchanged with nodeResolver null") {
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
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks.size shouldBe 2

        // Both tabs should have their screen nodes
        val tab0 = tabNode.stacks[0]
        tab0.children.size shouldBe 1
        tab0.children[0].shouldBeInstanceOf<ScreenNode>()
        (tab0.children[0] as ScreenNode).destination shouldBe MainTabs.Tab1

        val tab1 = tabNode.stacks[1]
        tab1.children.size shouldBe 1
        tab1.children[0].shouldBeInstanceOf<ScreenNode>()
        (tab1.children[0] as ScreenNode).destination shouldBe MainTabs.Tab2
    }
})
