@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.NavigationResultManager
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// =============================================================================
// Test destinations
// =============================================================================

private object SkcHome : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Home"
}

private object SkcProfile : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Profile"
}

private object SkcSettings : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Settings"
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


class ScreenKeyCollectorTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // -------------------------------------------------------------------------
    // cancelResultsForKeys — empty set is no-op
    // -------------------------------------------------------------------------

    test("cancelResultsForKeys - empty set does nothing") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        collector.cancelResultsForKeys(emptySet())

        resultManager.pendingCount() shouldBe 0
    }

    // -------------------------------------------------------------------------
    // cancelResultsForKeys — cancels pending results for given keys
    // -------------------------------------------------------------------------

    test("cancelResultsForKeys - cancels pending results for specified keys") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        // Set up pending results
        resultManager.requestResult("s1")
        resultManager.requestResult("s2")
        resultManager.requestResult("s3")
        resultManager.pendingCount() shouldBe 3

        collector.cancelResultsForKeys(setOf(NodeKey("s1"), NodeKey("s3")))

        resultManager.pendingCount() shouldBe 1
        resultManager.hasPendingResult("s2") shouldBe true
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — identical trees
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - identical trees cancels nothing") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("s1")

        val screen = screenNode("s1", "stack", SkcHome)
        val tree = stackNode("stack", null, listOf(screen))

        collector.cancelResultsForDestroyedScreens(tree, tree)

        resultManager.pendingCount() shouldBe 1
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — screen removed
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - popped screen result is cancelled") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("s1")
        resultManager.requestResult("s2")

        val s1 = screenNode("s1", "stack", SkcHome)
        val s2 = screenNode("s2", "stack", SkcProfile)
        val oldTree = stackNode("stack", null, listOf(s1, s2))
        val newTree = stackNode("stack", null, listOf(s1))

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        resultManager.hasPendingResult("s1") shouldBe true
        resultManager.hasPendingResult("s2") shouldBe false
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — flat stack collect all screen keys
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - all screens removed from stack") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("s1")
        resultManager.requestResult("s2")
        resultManager.requestResult("s3")

        val s1 = screenNode("s1", "old-stack", SkcHome)
        val s2 = screenNode("s2", "old-stack", SkcProfile)
        val oldTree = stackNode("old-stack", null, listOf(s1, s2))

        val s3 = screenNode("s3", "new-stack", SkcSettings)
        val newTree = stackNode("new-stack", null, listOf(s3))

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        resultManager.hasPendingResult("s1") shouldBe false
        resultManager.hasPendingResult("s2") shouldBe false
        resultManager.hasPendingResult("s3") shouldBe true
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — tab tree collects keys from all tabs
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - collects keys from all tabs") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("tab1-s1")
        resultManager.requestResult("tab2-s1")

        val tab1Screen = screenNode("tab1-s1", "tab1-stack", SkcHome)
        val tab2Screen = screenNode("tab2-s1", "tab2-stack", SkcProfile)
        val tabStack1 = stackNode("tab1-stack", "tabs", listOf(tab1Screen))
        val tabStack2 = stackNode("tab2-stack", "tabs", listOf(tab2Screen))
        val oldTree = tabNode("tabs", null, listOf(tabStack1, tabStack2))

        // New tree has no tab2 screen
        val newScreen = screenNode("new-s1", "new-stack", SkcSettings)
        val newTree = stackNode("new-stack", null, listOf(newScreen))

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        resultManager.hasPendingResult("tab1-s1") shouldBe false
        resultManager.hasPendingResult("tab2-s1") shouldBe false
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — pane tree collects keys from all panes
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - collects keys from all panes") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("p-s1")
        resultManager.requestResult("sup-s1")

        val primaryScreen = screenNode("p-s1", "p-stack", SkcHome)
        val supportingScreen = screenNode("sup-s1", "sup-stack", SkcProfile)
        val primaryStack = stackNode("p-stack", "pane", listOf(primaryScreen))
        val supportingStack = stackNode("sup-stack", "pane", listOf(supportingScreen))
        val oldTree = paneNode("pane", null, mapOf(
            PaneRole.Primary to PaneConfiguration(primaryStack),
            PaneRole.Supporting to PaneConfiguration(supportingStack)
        ))

        // New tree: only primary pane, different screen
        val newPrimaryScreen = screenNode("p-s2", "p-stack2", SkcSettings)
        val newPrimaryStack = stackNode("p-stack2", "pane2", listOf(newPrimaryScreen))
        val newTree = paneNode("pane2", null, mapOf(
            PaneRole.Primary to PaneConfiguration(newPrimaryStack)
        ))

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        resultManager.hasPendingResult("p-s1") shouldBe false
        resultManager.hasPendingResult("sup-s1") shouldBe false
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — empty stack (no screens)
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - empty stacks have no screen keys") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("s1")

        val oldTree = stackNode("stack1", null, emptyList())
        val newTree = stackNode("stack2", null, emptyList())

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        // No screen keys collected, so s1 remains pending
        resultManager.hasPendingResult("s1") shouldBe true
    }

    // -------------------------------------------------------------------------
    // cancelResultsForDestroyedScreens — no pending results is no-op
    // -------------------------------------------------------------------------

    test("cancelResultsForDestroyedScreens - no pending results is safe") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        val s1 = screenNode("s1", "stack", SkcHome)
        val oldTree = stackNode("stack", null, listOf(s1))
        val newTree = stackNode("stack", null, emptyList())

        collector.cancelResultsForDestroyedScreens(oldTree, newTree)

        resultManager.pendingCount() shouldBe 0
    }

    // -------------------------------------------------------------------------
    // cancelResultsForKeys — non-existent keys are safely ignored
    // -------------------------------------------------------------------------

    test("cancelResultsForKeys - non-existent keys are safely ignored") {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val resultManager = NavigationResultManager()
        val collector = ScreenKeyCollector(scope, resultManager)

        resultManager.requestResult("s1")

        collector.cancelResultsForKeys(setOf(NodeKey("nonexistent")))

        resultManager.pendingCount() shouldBe 1
        resultManager.hasPendingResult("s1") shouldBe true
    }
})
