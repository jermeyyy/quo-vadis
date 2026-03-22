@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

// =============================================================================
// Test destinations (prefixed to avoid same-package collision)
// =============================================================================

private object TabHome : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "TabHome"
}

private object TabProfile : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "TabProfile"
}

private object TabSettings : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "TabSettings"
}

class TabOperationsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // switchTab — valid cases
    // =========================================================================

    test("switchTab switches from tab 0 to tab 1") {
        val screen0 = ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome)
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab1"), TabProfile)
        val stack0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen0))
        val stack1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen1))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TabOperations.switchTab(tabs, NodeKey("tabs"), 1)

        val resultTabs = result as TabNode
        resultTabs.activeStackIndex shouldBe 1
    }

    test("switchTab updates active tab index correctly for three tabs") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab2"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TabOperations.switchTab(tabs, NodeKey("tabs"), 2) as TabNode

        result.activeStackIndex shouldBe 2
    }

    test("switchTab preserves tab contents after switch") {
        val screen0 = ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome)
        val screen1a = ScreenNode(NodeKey("s1a"), NodeKey("tab1"), TabProfile)
        val screen1b = ScreenNode(NodeKey("s1b"), NodeKey("tab1"), TabSettings)

        val stack0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen0))
        val stack1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen1a, screen1b))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TabOperations.switchTab(tabs, NodeKey("tabs"), 1) as TabNode

        result.stacks[0].children.size shouldBe 1
        (result.stacks[0].children[0] as ScreenNode).destination shouldBe TabHome
        result.stacks[1].children.size shouldBe 2
        (result.stacks[1].children[0] as ScreenNode).destination shouldBe TabProfile
        (result.stacks[1].children[1] as ScreenNode).destination shouldBe TabSettings
    }

    test("switchTab preserves structural sharing of stacks") {
        val screen0 = ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome)
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab1"), TabProfile)

        val stack0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen0))
        val stack1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen1))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TabOperations.switchTab(tabs, NodeKey("tabs"), 1) as TabNode

        result.stacks[0] shouldBeSameInstanceAs stack0
        result.stacks[1] shouldBeSameInstanceAs stack1
        result.stacks[0].children[0] shouldBeSameInstanceAs screen0
        result.stacks[1].children[0] shouldBeSameInstanceAs screen1
    }

    // =========================================================================
    // switchTab — no-op when already on target tab
    // =========================================================================

    test("switchTab returns same root when already on target tab") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 1
        )

        val result = TabOperations.switchTab(tabs, NodeKey("tabs"), 1)

        result shouldBeSameInstanceAs tabs
    }

    // =========================================================================
    // switchTab — nested tree (tab inside a stack)
    // =========================================================================

    test("switchTab works with TabNode nested inside a StackNode") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome))
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), TabProfile))
                )
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabs)
        )

        val result = TabOperations.switchTab(root, NodeKey("tabs"), 1)

        val resultStack = result as StackNode
        val resultTabs = resultStack.children[0] as TabNode
        resultTabs.activeStackIndex shouldBe 1
    }

    test("switchTab works with deeply nested TabNode") {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("tab0"),
            stacks = listOf(
                StackNode(NodeKey("inner-tab0"), NodeKey("inner-tabs"), emptyList()),
                StackNode(NodeKey("inner-tab1"), NodeKey("inner-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val outerTabs = TabNode(
            key = NodeKey("outer-tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(innerTabs))
            ),
            activeStackIndex = 0
        )

        val result = TabOperations.switchTab(outerTabs, NodeKey("inner-tabs"), 1)

        val resultOuter = result as TabNode
        val resultInner = resultOuter.stacks[0].children[0] as TabNode
        resultInner.activeStackIndex shouldBe 1
    }

    // =========================================================================
    // switchTab — error cases
    // =========================================================================

    test("switchTab throws for out-of-bounds tab index (too high)") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        shouldThrow<IllegalArgumentException> {
            TabOperations.switchTab(tabs, NodeKey("tabs"), 5)
        }
    }

    test("switchTab throws for negative tab index") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        shouldThrow<IllegalArgumentException> {
            TabOperations.switchTab(tabs, NodeKey("tabs"), -1)
        }
    }

    test("switchTab throws when key not found in tree") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        shouldThrow<IllegalArgumentException> {
            TabOperations.switchTab(tabs, NodeKey("nonexistent"), 0)
        }
    }

    test("switchTab throws when key points to non-TabNode") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen"), NodeKey("root"), TabHome)
            )
        )

        shouldThrow<IllegalArgumentException> {
            TabOperations.switchTab(root, NodeKey("screen"), 0)
        }
    }

    // =========================================================================
    // switchActiveTab — valid cases
    // =========================================================================

    test("switchActiveTab finds TabNode in active path and switches") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome))
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), TabProfile))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TabOperations.switchActiveTab(root, 1)

        val resultTabs = (result as StackNode).children[0] as TabNode
        resultTabs.activeStackIndex shouldBe 1
    }

    test("switchActiveTab returns same root when already on target tab") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome))
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), TabProfile))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TabOperations.switchActiveTab(root, 0)

        result shouldBeSameInstanceAs root
    }

    test("switchActiveTab finds first TabNode in nested tabs") {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("tab0"),
            stacks = listOf(
                StackNode(NodeKey("inner-tab0"), NodeKey("inner-tabs"),
                    listOf(ScreenNode(NodeKey("s-inner"), NodeKey("inner-tab0"), TabSettings))
                ),
                StackNode(NodeKey("inner-tab1"), NodeKey("inner-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val outerTabs = TabNode(
            key = NodeKey("outer-tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(innerTabs)),
                StackNode(NodeKey("tab1"), NodeKey("outer-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(outerTabs)
        )

        // Should find outer-tabs first in the active path and switch it
        val result = TabOperations.switchActiveTab(root, 1)

        val resultOuterTabs = (result as StackNode).children[0] as TabNode
        resultOuterTabs.activeStackIndex shouldBe 1
    }

    // =========================================================================
    // switchActiveTab — error cases
    // =========================================================================

    test("switchActiveTab throws when no TabNode in active path") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), TabHome)
            )
        )

        shouldThrow<IllegalStateException> {
            TabOperations.switchActiveTab(root, 0)
        }
    }

    test("switchActiveTab throws for empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalStateException> {
            TabOperations.switchActiveTab(root, 0)
        }
    }

    test("switchActiveTab throws for out-of-bounds index") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), TabHome))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        shouldThrow<IllegalArgumentException> {
            TabOperations.switchActiveTab(root, 5)
        }
    }

    test("switchActiveTab throws for ScreenNode root") {
        val root = ScreenNode(NodeKey("screen"), null, TabHome)

        shouldThrow<IllegalStateException> {
            TabOperations.switchActiveTab(root, 0)
        }
    }
})
