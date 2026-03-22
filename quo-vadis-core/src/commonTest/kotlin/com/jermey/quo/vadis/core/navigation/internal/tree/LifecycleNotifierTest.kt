@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

private object LifecycleTestDest : NavDestination

class LifecycleNotifierTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    val notifier = LifecycleNotifier()

    // =========================================================================
    // Helper functions
    // =========================================================================

    fun screen(key: String, parentKey: String? = null) =
        ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, LifecycleTestDest)

    fun stack(key: String, parentKey: String? = null, vararg children: NavNode) =
        StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, children.toList())

    fun tab(key: String, parentKey: String? = null, vararg stacks: StackNode, activeIndex: Int = 0) =
        TabNode(NodeKey(key), parentKey?.let { NodeKey(it) }, stacks.toList(), activeIndex)

    fun pane(key: String, parentKey: String? = null, configs: Map<PaneRole, PaneConfiguration>) =
        PaneNode(NodeKey(key), parentKey?.let { NodeKey(it) }, configs)

    // =========================================================================
    // notifyRemovedNodes
    // =========================================================================

    test("notifyRemovedNodes calls detachFromNavigator on each node") {
        val s1 = screen("s1", "stack1")
        val s2 = screen("s2", "stack1")
        s1.attachToNavigator()
        s2.attachToNavigator()

        s1.isAttachedToNavigator shouldBe true
        s2.isAttachedToNavigator shouldBe true

        notifier.notifyRemovedNodes(listOf(s1, s2))

        s1.isAttachedToNavigator shouldBe false
        s2.isAttachedToNavigator shouldBe false
    }

    test("notifyRemovedNodes with empty list does nothing") {
        notifier.notifyRemovedNodes(emptyList())
        // No assertion needed - just should not throw
    }

    test("notifyRemovedNodes triggers onDestroy callbacks") {
        val s1 = screen("s1", "stack1")
        s1.attachToNavigator()
        var callbackCalled = false
        s1.addOnDestroyCallback { callbackCalled = true }

        notifier.notifyRemovedNodes(listOf(s1))

        callbackCalled shouldBe true
    }

    // =========================================================================
    // notifyRemovedNodesDetached
    // =========================================================================

    test("notifyRemovedNodesDetached detaches nodes removed from tree") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        s1.attachToNavigator()
        s2.attachToNavigator()

        val oldState = stack("root", null, s1, s2)
        val newState = stack("root", null, s1)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        // s2 was removed, should be detached
        s2.isAttachedToNavigator shouldBe false
        // s1 still in tree, should remain attached
        s1.isAttachedToNavigator shouldBe true
    }

    test("notifyRemovedNodesDetached with identical trees detaches nothing") {
        val s1 = screen("s1", "root")
        s1.attachToNavigator()

        val oldState = stack("root", null, s1)
        val newState = stack("root", null, s1)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        s1.isAttachedToNavigator shouldBe true
    }

    test("notifyRemovedNodesDetached handles tab nodes") {
        val s1 = screen("s1", "tab-stack1")
        val s2 = screen("s2", "tab-stack2")
        s1.attachToNavigator()
        s2.attachToNavigator()

        val stack1 = stack("tab-stack1", "tabs", s1)
        val stack2 = stack("tab-stack2", "tabs", s2)
        val tabOld = tab("tabs", "root", stack1, stack2)

        // New state: only stack1 remains
        val tabNew = tab("tabs", "root", stack1)

        val oldState = stack("root", null, tabOld)
        val newState = stack("root", null, tabNew)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        // s1 still in tree
        s1.isAttachedToNavigator shouldBe true
        // s2 removed
        s2.isAttachedToNavigator shouldBe false
    }

    test("notifyRemovedNodesDetached detaches TabNode itself when removed") {
        val s1 = screen("s1", "tab-stack")
        s1.attachToNavigator()
        val tabStack = stack("tab-stack", "tabs", s1)
        val tabNode = tab("tabs", "root", tabStack)
        tabNode.attachToNavigator()

        val s2 = screen("s2", "root")

        val oldState = stack("root", null, tabNode)
        val newState = stack("root", null, s2)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        tabNode.isAttachedToNavigator shouldBe false
        s1.isAttachedToNavigator shouldBe false
    }

    test("notifyRemovedNodesDetached handles pane nodes") {
        val primaryScreen = screen("primary-s", "primary-stack")
        val supportingScreen = screen("supporting-s", "supporting-stack")
        primaryScreen.attachToNavigator()
        supportingScreen.attachToNavigator()

        val primaryStack = stack("primary-stack", "pane", primaryScreen)
        val supportingStack = stack("supporting-stack", "pane", supportingScreen)

        val paneOld = pane(
            "pane", "root",
            mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            )
        )
        paneOld.attachToNavigator()

        // New state: pane removed, replaced with screen
        val replacement = screen("replacement", "root")
        val oldState = stack("root", null, paneOld)
        val newState = stack("root", null, replacement)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        paneOld.isAttachedToNavigator shouldBe false
        primaryScreen.isAttachedToNavigator shouldBe false
        supportingScreen.isAttachedToNavigator shouldBe false
    }

    test("notifyRemovedNodesDetached with all nodes removed") {
        val s1 = screen("s1", "old-root")
        val s2 = screen("s2", "old-root")
        s1.attachToNavigator()
        s2.attachToNavigator()

        val oldState = stack("old-root", null, s1, s2)
        // Completely different tree
        val s3 = screen("s3", "new-root")
        val newState = stack("new-root", null, s3)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        s1.isAttachedToNavigator shouldBe false
        s2.isAttachedToNavigator shouldBe false
    }

    test("notifyRemovedNodesDetached nested stacks in tabs") {
        val s1 = screen("s1", "inner-stack")
        val s2 = screen("s2", "inner-stack")
        s1.attachToNavigator()
        s2.attachToNavigator()

        val innerStack = stack("inner-stack", "tabs", s1, s2)
        val tabNode = tab("tabs", "root", innerStack)

        // After pop: s2 removed from inner stack
        val innerStackAfter = stack("inner-stack", "tabs", s1)
        val tabNodeAfter = tab("tabs", "root", innerStackAfter)

        val oldState = stack("root", null, tabNode)
        val newState = stack("root", null, tabNodeAfter)

        notifier.notifyRemovedNodesDetached(oldState, newState)

        s1.isAttachedToNavigator shouldBe true
        s2.isAttachedToNavigator shouldBe false
    }
})
