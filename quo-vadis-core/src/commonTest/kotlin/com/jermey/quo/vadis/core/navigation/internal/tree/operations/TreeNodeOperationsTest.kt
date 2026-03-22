@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.TreeOperationResult
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

// =============================================================================
// Test destinations
// =============================================================================

private object TnoHome : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Home"
}

private object TnoProfile : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Profile"
}

private object TnoSettings : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Settings"
}

private object TnoDetail : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
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

class TreeNodeOperationsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // -------------------------------------------------------------------------
    // replaceNode
    // -------------------------------------------------------------------------

    test("replaceNode - replace screen in flat stack") {
        val screen1 = screenNode("s1", "stack", TnoHome)
        val screen2 = screenNode("s2", "stack", TnoProfile)
        val root = stackNode("stack", null, listOf(screen1, screen2))

        val replacement = screenNode("s2-new", "stack", TnoSettings)
        val result = TreeNodeOperations.replaceNode(root, NodeKey("s2"), replacement)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 2
        newRoot.children[0] shouldBe screen1 // unchanged
        val replacedScreen = newRoot.children[1].shouldBeInstanceOf<ScreenNode>()
        replacedScreen.destination shouldBe TnoSettings
    }

    test("replaceNode - replace root node itself") {
        val root = screenNode("root", null, TnoHome)
        val replacement = screenNode("root-new", null, TnoProfile)

        val result = TreeNodeOperations.replaceNode(root, NodeKey("root"), replacement)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<ScreenNode>()
        newRoot.destination shouldBe TnoProfile
    }

    test("replaceNode - key not found returns NodeNotFound") {
        val root = stackNode("stack", null, listOf(
            screenNode("s1", "stack", TnoHome)
        ))

        val result = TreeNodeOperations.replaceNode(root, NodeKey("nonexistent"), screenNode("x", null, TnoHome))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
        result.key shouldBe NodeKey("nonexistent")
    }

    test("replaceNode - replace stack inside tab node preserves tree structure") {
        val homeScreen = screenNode("home", "tab-stack1", TnoHome)
        val profileScreen = screenNode("profile", "tab-stack2", TnoProfile)
        val tabStack1 = stackNode("tab-stack1", "tabs", listOf(homeScreen))
        val tabStack2 = stackNode("tab-stack2", "tabs", listOf(profileScreen))
        val tabs = tabNode("tabs", "root-stack", listOf(tabStack1, tabStack2))
        val root = stackNode("root-stack", null, listOf(tabs))

        val newStack = stackNode("tab-stack1-new", "tabs", listOf(
            screenNode("home-new", "tab-stack1-new", TnoSettings)
        ))

        val result = TreeNodeOperations.replaceNode(root, NodeKey("tab-stack1"), newStack)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        val newTabs = newRoot.children[0].shouldBeInstanceOf<TabNode>()
        newTabs.stacks.size shouldBe 2
        newTabs.stacks[0].key shouldBe NodeKey("tab-stack1-new")
        newTabs.stacks[1] shouldBe tabStack2 // unchanged
    }

    test("replaceNode - replace screen deep in tab stack") {
        val screen = screenNode("deep-screen", "inner-stack", TnoHome)
        val innerStack = stackNode("inner-stack", "tabs", listOf(screen))
        val tabs = tabNode("tabs", "root", listOf(innerStack))
        val root = stackNode("root", null, listOf(tabs))

        val replacement = screenNode("new-screen", "inner-stack", TnoDetail)
        val result = TreeNodeOperations.replaceNode(root, NodeKey("deep-screen"), replacement)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        val newTabs = newRoot.children[0].shouldBeInstanceOf<TabNode>()
        val newInnerStack = newTabs.stacks[0]
        val newScreen = newInnerStack.children[0].shouldBeInstanceOf<ScreenNode>()
        newScreen.destination shouldBe TnoDetail
    }

    test("replaceNode - replace node in pane node") {
        val primaryScreen = screenNode("primary-s", "primary-stack", TnoHome)
        val supportingScreen = screenNode("supporting-s", "supporting-stack", TnoProfile)
        val primaryStack = stackNode("primary-stack", "pane", listOf(primaryScreen))
        val supportingStack = stackNode("supporting-stack", "pane", listOf(supportingScreen))
        val pane = paneNode("pane", "root", mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack),
            PaneRole.Supporting to PaneConfiguration(supportingStack)
        ))
        val root = stackNode("root", null, listOf(pane))

        val replacement = screenNode("new-s", "supporting-stack", TnoSettings)
        val result = TreeNodeOperations.replaceNode(root, NodeKey("supporting-s"), replacement)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        val newPane = newRoot.children[0].shouldBeInstanceOf<PaneNode>()
        val newSupportingStack = newPane.paneConfigurations[PaneRole.Supporting]!!.content.shouldBeInstanceOf<StackNode>()
        val newScreen = newSupportingStack.children[0].shouldBeInstanceOf<ScreenNode>()
        newScreen.destination shouldBe TnoSettings
        // Primary pane unchanged
        newPane.paneConfigurations[PaneRole.Primary]!!.content shouldBe primaryStack
    }

    test("replaceNode - ScreenNode with missing key returns NodeNotFound") {
        val root = screenNode("leaf", null, TnoHome)

        val result = TreeNodeOperations.replaceNode(root, NodeKey("other"), screenNode("x", null, TnoProfile))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
    }

    // -------------------------------------------------------------------------
    // removeNode
    // -------------------------------------------------------------------------

    test("removeNode - remove screen from flat stack") {
        val screen1 = screenNode("s1", "stack", TnoHome)
        val screen2 = screenNode("s2", "stack", TnoProfile)
        val root = stackNode("stack", null, listOf(screen1, screen2))

        val result = TreeNodeOperations.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        newRoot.children[0] shouldBe screen1
    }

    test("removeNode - remove root returns null") {
        val root = stackNode("root", null, listOf(
            screenNode("s1", "root", TnoHome)
        ))

        val result = TreeNodeOperations.removeNode(root, NodeKey("root"))

        result shouldBe null
    }

    test("removeNode - key not found returns NodeNotFound") {
        val root = stackNode("stack", null, listOf(
            screenNode("s1", "stack", TnoHome)
        ))

        val result = TreeNodeOperations.removeNode(root, NodeKey("nonexistent"))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
        result.key shouldBe NodeKey("nonexistent")
    }

    test("removeNode - remove from nested tab stack") {
        val screen1 = screenNode("s1", "inner-stack", TnoHome)
        val screen2 = screenNode("s2", "inner-stack", TnoProfile)
        val innerStack = stackNode("inner-stack", "tabs", listOf(screen1, screen2))
        val tabs = tabNode("tabs", "root", listOf(innerStack))
        val root = stackNode("root", null, listOf(tabs))

        val result = TreeNodeOperations.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        val newTabs = newRoot.children[0].shouldBeInstanceOf<TabNode>()
        val newInnerStack = newTabs.stacks[0]
        newInnerStack.children.size shouldBe 1
        newInnerStack.children[0].shouldBeInstanceOf<ScreenNode>().destination shouldBe TnoHome
    }

    test("removeNode - remove from pane nested stack") {
        val s1 = screenNode("s1", "pane-stack", TnoHome)
        val s2 = screenNode("s2", "pane-stack", TnoProfile)
        val paneStack = stackNode("pane-stack", "pane", listOf(s1, s2))
        val pane = paneNode("pane", "root", mapOf(
            PaneRole.Primary to PaneConfiguration(paneStack)
        ))
        val root = stackNode("root", null, listOf(pane))

        val result = TreeNodeOperations.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        val newPane = newRoot.children[0].shouldBeInstanceOf<PaneNode>()
        val newPaneStack = newPane.paneConfigurations[PaneRole.Primary]!!.content.shouldBeInstanceOf<StackNode>()
        newPaneStack.children.size shouldBe 1
    }

    test("removeNode - cannot remove stack from TabNode throws exception") {
        val innerStack = stackNode("tab-stack", "tabs", listOf(
            screenNode("s1", "tab-stack", TnoHome)
        ))
        val tabs = tabNode("tabs", null, listOf(innerStack))

        shouldThrow<IllegalArgumentException> {
            TreeNodeOperations.removeNode(tabs, NodeKey("tab-stack"))
        }
    }

    test("removeNode - cannot remove pane content directly throws exception") {
        val paneStack = stackNode("pane-stack", "pane", listOf(
            screenNode("s1", "pane-stack", TnoHome)
        ))
        val pane = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(paneStack)
        ))

        shouldThrow<IllegalArgumentException> {
            TreeNodeOperations.removeNode(pane, NodeKey("pane-stack"))
        }
    }

    test("removeNode - remove first child from stack") {
        val screen1 = screenNode("s1", "stack", TnoHome)
        val screen2 = screenNode("s2", "stack", TnoProfile)
        val screen3 = screenNode("s3", "stack", TnoSettings)
        val root = stackNode("stack", null, listOf(screen1, screen2, screen3))

        val result = TreeNodeOperations.removeNode(root, NodeKey("s1"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val newRoot = result.newTree.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 2
        newRoot.children[0].shouldBeInstanceOf<ScreenNode>().destination shouldBe TnoProfile
        newRoot.children[1].shouldBeInstanceOf<ScreenNode>().destination shouldBe TnoSettings
    }

    test("removeNode - ScreenNode leaf with non-matching key returns NodeNotFound") {
        val root = screenNode("leaf", null, TnoHome)

        val result = TreeNodeOperations.removeNode(root, NodeKey("other"))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
    }
})
