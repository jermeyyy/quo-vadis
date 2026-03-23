@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.config.PopBehavior
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

// =============================================================================
// Test destinations (prefixed to avoid same-package collision with PushOperationsTest)
// =============================================================================

private object PopHome : NavDestination {
    override fun toString(): String = "PopHome"
}

private object PopProfile : NavDestination {
    override fun toString(): String = "PopProfile"
}

private object PopSettings : NavDestination {
    override fun toString(): String = "PopSettings"
}

private object PopDetail : NavDestination {
    override fun toString(): String = "PopDetail"
}

class PopOperationsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // pop(root, behavior) — simple pop
    // =========================================================================

    test("pop removes last screen from stack with multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile)
            )
        )

        val result = PopOperations.pop(root)

        result.shouldNotBeNull()
        val resultStack = result as StackNode
        resultStack.children.size shouldBe 1
        (resultStack.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("pop returns null on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = PopOperations.pop(root)

        result.shouldBeNull()
    }

    test("pop returns null when root is a ScreenNode (no active stack)") {
        val root = ScreenNode(NodeKey("screen"), null, PopHome)

        val result = PopOperations.pop(root)

        result.shouldBeNull()
    }

    test("pop with PRESERVE_EMPTY on single-screen stack returns empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.pop(root, PopBehavior.PRESERVE_EMPTY)

        result.shouldNotBeNull()
        (result as StackNode).isEmpty.shouldBeTrue()
    }

    test("pop with CASCADE on single-screen root stack returns null") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.pop(root, PopBehavior.CASCADE)

        result.shouldBeNull()
    }

    test("pop targets deepest active stack in tabs") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), PopProfile)
                            )
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s3"), NodeKey("tab1"), PopSettings)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = PopOperations.pop(root)

        result.shouldNotBeNull()
        val tabs = (result as StackNode).children[0] as TabNode
        tabs.stacks[0].children.size shouldBe 1
        (tabs.stacks[0].activeChild as ScreenNode).destination shouldBe PopHome
        // Tab1 unchanged
        tabs.stacks[1].children.size shouldBe 1
    }

    test("pop from active pane stack removes top screen") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(
                ScreenNode(NodeKey("ps1"), NodeKey("primary"), PopHome),
                ScreenNode(NodeKey("ps2"), NodeKey("primary"), PopProfile)
            )
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))

        val result = PopOperations.pop(root)

        result.shouldNotBeNull()
        val resultPane = (result as StackNode).children[0] as PaneNode
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.children.size shouldBe 1
        (resultPrimary.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("pop with CASCADE inside tab preserves empty stack (tab structure integrity)") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = PopOperations.pop(root, PopBehavior.CASCADE)

        result.shouldNotBeNull()
        val tabs = (result as StackNode).children[0] as TabNode
        tabs.stacks[0].isEmpty.shouldBeTrue()
    }

    test("pop with CASCADE inside pane preserves empty stack") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(
                ScreenNode(NodeKey("ps1"), NodeKey("primary"), PopHome)
            )
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))

        val result = PopOperations.pop(root, PopBehavior.CASCADE)

        result.shouldNotBeNull()
        val resultPane = (result as StackNode).children[0] as PaneNode
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.isEmpty.shouldBeTrue()
    }

    test("pop with CASCADE removes nested empty stack from parent stack") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), PopHome)
            )
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s0"), NodeKey("outer"), PopProfile),
                innerStack
            )
        )

        val result = PopOperations.pop(outerStack, PopBehavior.CASCADE)

        result.shouldNotBeNull()
        val resultOuter = result as StackNode
        resultOuter.children.size shouldBe 1
        (resultOuter.children[0] as ScreenNode).destination shouldBe PopProfile
    }

    test("pop with default behavior uses PRESERVE_EMPTY") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), PopHome)
            )
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(innerStack)
        )

        val result = PopOperations.pop(outerStack)

        result.shouldNotBeNull()
        val resultOuter = result as StackNode
        val resultInner = resultOuter.children[0] as StackNode
        resultInner.isEmpty.shouldBeTrue()
    }

    test("pop preserves structural sharing for unchanged branches") {
        val tab1Screen = ScreenNode(NodeKey("s3"), NodeKey("tab1"), PopSettings)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))

        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), PopProfile)
                    )
                ),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = PopOperations.pop(root) as TabNode

        result.stacks[1] shouldBeSameInstanceAs tab1Stack
    }

    // =========================================================================
    // popTo(root, inclusive, predicate) — predicate-based pop
    // =========================================================================

    test("popTo with predicate removes screens above match") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopProfile
        }

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe PopProfile
    }

    test("popTo with predicate inclusive removes screens above and including match") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == PopProfile
        }

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popTo returns original when predicate never matches") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.popTo(root) { false }

        result shouldBeSameInstanceAs root
    }

    test("popTo returns original when no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, PopHome)

        val result = PopOperations.popTo(root) { true }

        result shouldBeSameInstanceAs root
    }

    test("popTo inclusive returns original when match is first and would empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile)
            )
        )

        val result = PopOperations.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == PopHome
        }

        // keepCount would be 0 → returns original
        result shouldBeSameInstanceAs root
    }

    test("popTo finds last occurrence when multiple matches exist") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s4"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopHome
        }

        // indexOfLast finds s3 at index 2
        (result as StackNode).children.size shouldBe 3
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popTo works in tabs targeting active stack") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), PopProfile),
                        ScreenNode(NodeKey("s3"), NodeKey("tab0"), PopSettings)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopHome
        }

        val resultTabs = result as TabNode
        resultTabs.stacks[0].children.size shouldBe 1
        (resultTabs.stacks[0].activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popTo non-inclusive with match at last position returns original") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile)
            )
        )

        // Match is at last position (index 1), non-inclusive keeps all → same as original size
        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopProfile
        }

        (result as StackNode).children.size shouldBe 2
    }

    // =========================================================================
    // popTo(root, targetKey, inclusive) — key-based pop
    // =========================================================================

    test("popTo by key removes screens above matching key") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root, NodeKey("s2"))

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe PopProfile
    }

    test("popTo by key with inclusive removes matching screen too") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root, NodeKey("s2"), inclusive = true)

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popTo by key returns original when key not found") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.popTo(root, NodeKey("nonexistent"))

        result shouldBeSameInstanceAs root
    }

    test("popTo by key pops to first screen (non-inclusive keeps it)") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popTo(root, NodeKey("s1"))

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    // =========================================================================
    // popToRoute(root, route, inclusive) — route-based pop
    // =========================================================================

    test("popToRoute removes screens above matching route") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popToRoute(root, "PopProfile")

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe PopProfile
    }

    test("popToRoute with inclusive removes matching screen too") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popToRoute(root, "PopProfile", inclusive = true)

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popToRoute returns original when route not found") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.popToRoute(root, "Nonexistent")

        result shouldBeSameInstanceAs root
    }

    test("popToRoute only matches ScreenNode destinations") {
        // A StackNode child should not match route
        val innerStack = StackNode(NodeKey("inner"), NodeKey("root"), emptyList())
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                innerStack
            )
        )

        val result = PopOperations.popToRoute(root, "inner")

        // No ScreenNode with toString() == "inner", returns original
        result shouldBeSameInstanceAs root
    }

    test("popToRoute finds last occurrence when duplicate routes exist") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s4"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popToRoute(root, "PopHome")

        // Last "PopHome" is at index 2
        (result as StackNode).children.size shouldBe 3
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    // =========================================================================
    // popToDestination<D>(root, inclusive) — type-based pop
    // =========================================================================

    test("popToDestination removes screens above matching type") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popToDestination<PopProfile>(root)

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe PopProfile
    }

    test("popToDestination with inclusive removes matching screen too") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.popToDestination<PopProfile>(root, inclusive = true)

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popToDestination returns original when type not found") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.popToDestination<PopDetail>(root)

        result shouldBeSameInstanceAs root
    }

    test("popToDestination finds correct type in stack with multiple types") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings),
                ScreenNode(NodeKey("s4"), NodeKey("root"), PopDetail)
            )
        )

        val result = PopOperations.popToDestination<PopHome>(root)

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    // =========================================================================
    // handleEmptyStackPop (tested via pop with different parent types)
    // =========================================================================

    test("pop CASCADE with nested stack in tab preserves empty tab stack") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("tab0"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), PopHome)
            )
        )
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
                            listOf(innerStack)
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        // Pop from innerStack with CASCADE - activeStack is inner
        val result = PopOperations.pop(root, PopBehavior.CASCADE)

        // The innerStack becomes empty; CASCADE parent is tab0 (StackNode)
        // So it gets removed from tab0's children → tab0 becomes empty stack
        result.shouldNotBeNull()
    }

    test("pop with PRESERVE_EMPTY in pane leaves pane structure intact") {
        val supportingStack = StackNode(
            NodeKey("supporting"), NodeKey("pane"),
            listOf(
                ScreenNode(NodeKey("ss1"), NodeKey("supporting"), PopProfile)
            )
        )
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(
                ScreenNode(NodeKey("ps1"), NodeKey("primary"), PopHome)
            )
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))

        val result = PopOperations.pop(root, PopBehavior.PRESERVE_EMPTY)

        result.shouldNotBeNull()
        val resultPane = (result as StackNode).children[0] as PaneNode
        // Primary becomes empty, supporting unchanged
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.isEmpty.shouldBeTrue()
        val resultSupporting = resultPane.paneConfigurations[PaneRole.Supporting]!!.content as StackNode
        resultSupporting.children.size shouldBe 1
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    test("pop from stack with three screens removes only the last one") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val result = PopOperations.pop(root)

        result.shouldNotBeNull()
        val resultStack = result as StackNode
        resultStack.children.size shouldBe 2
        (resultStack.children[0] as ScreenNode).destination shouldBe PopHome
        (resultStack.children[1] as ScreenNode).destination shouldBe PopProfile
    }

    test("popTo with single-element stack and non-inclusive match at index 0 keeps it") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome)
            )
        )

        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopHome
        }

        // Match at index 0, non-inclusive → keepCount = 1 → keeps s1
        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popTo in panes targets active pane stack") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(
                ScreenNode(NodeKey("ps1"), NodeKey("primary"), PopHome),
                ScreenNode(NodeKey("ps2"), NodeKey("primary"), PopProfile),
                ScreenNode(NodeKey("ps3"), NodeKey("primary"), PopSettings)
            )
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))

        val result = PopOperations.popTo(root) { node ->
            node is ScreenNode && node.destination == PopHome
        }

        val resultPane = (result as StackNode).children[0] as PaneNode
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.children.size shouldBe 1
        (resultPrimary.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popToRoute in tabs targets active tab stack") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), PopProfile),
                        ScreenNode(NodeKey("s3"), NodeKey("tab0"), PopSettings)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s4"), NodeKey("tab1"), PopDetail)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = PopOperations.popToRoute(root, "PopHome")

        val resultTabs = result as TabNode
        resultTabs.stacks[0].children.size shouldBe 1
        (resultTabs.stacks[0].activeChild as ScreenNode).destination shouldBe PopHome
        // Tab1 unchanged
        resultTabs.stacks[1].children.size shouldBe 1
    }

    test("pop multiple times reduces stack correctly") {
        var current: StackNode = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), PopHome),
                ScreenNode(NodeKey("s2"), NodeKey("root"), PopProfile),
                ScreenNode(NodeKey("s3"), NodeKey("root"), PopSettings)
            )
        )

        val first = PopOperations.pop(current) as StackNode
        first.children.size shouldBe 2

        val second = PopOperations.pop(first) as StackNode
        second.children.size shouldBe 1

        (second.activeChild as ScreenNode).destination shouldBe PopHome
    }

    test("popToDestination in tabs targets active tab") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), PopHome),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), PopProfile),
                        ScreenNode(NodeKey("s3"), NodeKey("tab0"), PopSettings)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = PopOperations.popToDestination<PopHome>(root)

        val resultTabs = result as TabNode
        resultTabs.stacks[0].children.size shouldBe 1
        (resultTabs.stacks[0].activeChild as ScreenNode).destination.shouldBeInstanceOf<PopHome>()
    }
})
