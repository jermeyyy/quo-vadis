@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

// =============================================================================
// Test destinations
// =============================================================================

private object TdcHome : NavDestination {
    override fun toString(): String = "Home"
}

private object TdcProfile : NavDestination {
    override fun toString(): String = "Profile"
}

private object TdcSettings : NavDestination {
    override fun toString(): String = "Settings"
}

private object TdcDetail : NavDestination {
    override fun toString(): String = "Detail"
}

// =============================================================================
// Helper functions
// =============================================================================

private fun screenNode(key: String, parentKey: String?, destination: NavDestination) =
    ScreenNode(key = NodeKey(key), parentKey = parentKey?.let { NodeKey(it) }, destination = destination)

private fun stackNode(key: String, parentKey: String?, children: List<com.jermey.quo.vadis.core.navigation.node.NavNode>) =
    StackNode(key = NodeKey(key), parentKey = parentKey?.let { NodeKey(it) }, children = children)

private fun tabNode(
    key: String,
    parentKey: String?,
    stacks: List<StackNode>,
    activeStackIndex: Int = 0
) = TabNode(
    key = NodeKey(key),
    parentKey = parentKey?.let { NodeKey(it) },
    stacks = stacks,
    activeStackIndex = activeStackIndex
)

private fun paneNode(
    key: String,
    parentKey: String?,
    configurations: Map<PaneRole, PaneConfiguration>
) = PaneNode(
    key = NodeKey(key),
    parentKey = parentKey?.let { NodeKey(it) },
    paneConfigurations = configurations
)

// =============================================================================
// Tests
// =============================================================================

class TreeDiffCalculatorTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // -------------------------------------------------------------------------
    // computeDiff — identical trees
    // -------------------------------------------------------------------------

    test("computeDiff - identical trees produce empty diff") {
        val screen = screenNode("s1", "stack", TdcHome)
        val tree = stackNode("stack", null, listOf(screen))

        val diff = TreeDiffCalculator.computeDiff(tree, tree)

        diff.removedScreenKeys.shouldBeEmpty()
        diff.removedLifecycleNodes.shouldBeEmpty()
    }

    // -------------------------------------------------------------------------
    // computeDiff — screen pushed (new screen added)
    // -------------------------------------------------------------------------

    test("computeDiff - new screen pushed has no removed keys") {
        val screen1 = screenNode("s1", "stack", TdcHome)
        val oldTree = stackNode("stack", null, listOf(screen1))

        val screen2 = screenNode("s2", "stack", TdcProfile)
        val newTree = stackNode("stack", null, listOf(screen1, screen2))

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        diff.removedScreenKeys.shouldBeEmpty()
        diff.removedLifecycleNodes.shouldBeEmpty()
    }

    // -------------------------------------------------------------------------
    // computeDiff — screen popped (screen removed)
    // -------------------------------------------------------------------------

    test("computeDiff - screen popped detects removed screen key") {
        val screen1 = screenNode("s1", "stack", TdcHome)
        val screen2 = screenNode("s2", "stack", TdcProfile)
        val oldTree = stackNode("stack", null, listOf(screen1, screen2))

        val newTree = stackNode("stack", null, listOf(screen1))

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        diff.removedScreenKeys shouldBe setOf(NodeKey("s2"))
    }

    // -------------------------------------------------------------------------
    // computeDiff — complete tree replacement
    // -------------------------------------------------------------------------

    test("computeDiff - complete tree replacement detects all old screens as removed") {
        val oldScreen1 = screenNode("old-s1", "old-stack", TdcHome)
        val oldScreen2 = screenNode("old-s2", "old-stack", TdcProfile)
        val oldTree = stackNode("old-stack", null, listOf(oldScreen1, oldScreen2))

        val newScreen = screenNode("new-s1", "new-stack", TdcSettings)
        val newTree = stackNode("new-stack", null, listOf(newScreen))

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        diff.removedScreenKeys shouldBe setOf(NodeKey("old-s1"), NodeKey("old-s2"))
    }

    // -------------------------------------------------------------------------
    // computeDiff — tab switch detection
    // -------------------------------------------------------------------------

    test("computeDiff - tab tree with same screens produces empty screen diff") {
        val screen1 = screenNode("s1", "tab-stack1", TdcHome)
        val screen2 = screenNode("s2", "tab-stack2", TdcProfile)
        val tabStack1 = stackNode("tab-stack1", "tabs", listOf(screen1))
        val tabStack2 = stackNode("tab-stack2", "tabs", listOf(screen2))

        val oldTree = tabNode("tabs", null, listOf(tabStack1, tabStack2), activeStackIndex = 0)
        val newTree = tabNode("tabs", null, listOf(tabStack1, tabStack2), activeStackIndex = 1)

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        // Same screens exist in both trees — no removals
        diff.removedScreenKeys.shouldBeEmpty()
    }

    // -------------------------------------------------------------------------
    // computeDiff — nested pane changes
    // -------------------------------------------------------------------------

    test("computeDiff - pane screen removed detected in diff") {
        val primaryScreen = screenNode("p-s1", "p-stack", TdcHome)
        val supportingScreen = screenNode("sup-s1", "sup-stack", TdcProfile)
        val primaryStack = stackNode("p-stack", "pane", listOf(primaryScreen))
        val supportingStack = stackNode("sup-stack", "pane", listOf(supportingScreen))
        val oldPane = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack),
            PaneRole.Supporting to PaneConfiguration(supportingStack)
        ))

        // New tree: supporting pane has different screen
        val newSupportingScreen = screenNode("sup-s2", "sup-stack", TdcSettings)
        val newSupportingStack = stackNode("sup-stack", "pane", listOf(newSupportingScreen))
        val newPane = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack),
            PaneRole.Supporting to PaneConfiguration(newSupportingStack)
        ))

        val diff = TreeDiffCalculator.computeDiff(oldPane, newPane)

        diff.removedScreenKeys shouldBe setOf(NodeKey("sup-s1"))
    }

    // -------------------------------------------------------------------------
    // computeDiff — lifecycle node removal
    // -------------------------------------------------------------------------

    test("computeDiff - removed tab node appears in removedLifecycleNodes") {
        val screen = screenNode("s1", "tab-stack", TdcHome)
        val tabStack = stackNode("tab-stack", "tabs", listOf(screen))
        val tabs = tabNode("tabs", "root", listOf(tabStack))
        val oldTree = stackNode("root", null, listOf(tabs))

        val newScreen = screenNode("s2", "root", TdcProfile)
        val newTree = stackNode("root", null, listOf(newScreen))

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        diff.removedScreenKeys shouldBe setOf(NodeKey("s1"))
        // TabNode and ScreenNode are LifecycleAwareNodes, StackNode is not
        diff.removedLifecycleNodes shouldHaveSize 2 // TabNode + ScreenNode
    }

    // -------------------------------------------------------------------------
    // computeDiff — multiple screens removed
    // -------------------------------------------------------------------------

    test("computeDiff - multiple screens popped detected") {
        val s1 = screenNode("s1", "stack", TdcHome)
        val s2 = screenNode("s2", "stack", TdcProfile)
        val s3 = screenNode("s3", "stack", TdcSettings)
        val oldTree = stackNode("stack", null, listOf(s1, s2, s3))

        val newTree = stackNode("stack", null, listOf(s1))

        val diff = TreeDiffCalculator.computeDiff(oldTree, newTree)

        diff.removedScreenKeys shouldBe setOf(NodeKey("s2"), NodeKey("s3"))
    }

    // -------------------------------------------------------------------------
    // computeDiff — single screen tree
    // -------------------------------------------------------------------------

    test("computeDiff - single screen replaced") {
        val oldScreen = screenNode("s1", null, TdcHome)
        val newScreen = screenNode("s2", null, TdcProfile)

        val diff = TreeDiffCalculator.computeDiff(oldScreen, newScreen)

        diff.removedScreenKeys shouldBe setOf(NodeKey("s1"))
    }

    // -------------------------------------------------------------------------
    // computeDiff — pane added (no removals)
    // -------------------------------------------------------------------------

    test("computeDiff - adding pane configuration has no removed keys") {
        val primaryScreen = screenNode("p-s1", "p-stack", TdcHome)
        val primaryStack = stackNode("p-stack", "pane", listOf(primaryScreen))
        val oldPane = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack)
        ))

        val supportingScreen = screenNode("sup-s1", "sup-stack", TdcProfile)
        val supportingStack = stackNode("sup-stack", "pane", listOf(supportingScreen))
        val newPane = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack),
            PaneRole.Supporting to PaneConfiguration(supportingStack)
        ))

        val diff = TreeDiffCalculator.computeDiff(oldPane, newPane)

        diff.removedScreenKeys.shouldBeEmpty()
    }
})
