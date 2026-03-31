package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private sealed interface MainTabs : NavDestination {
    data object HomeTab : MainTabs {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "HomeTab"
    }

    data object SettingsTab : MainTabs {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "SettingsTab"
    }

    data object ProfileTab : MainTabs {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "ProfileTab"
    }
}

private data object OutOfScopeDestination : NavDestination {
    override val transition: NavigationTransition? = null
    override fun toString(): String = "OutOfScope"
}

private data object DetailDestination : NavDestination {
    override val transition: NavigationTransition? = null
    override fun toString(): String = "Detail"
}

/**
 * Tests for scope-aware [com.jermey.quo.vadis.core.navigation.tree.TreeMutator] operations.
 *
 * These tests verify that `TreeMutator.push` with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] correctly routes
 * destinations based on whether they belong to a container's scope.
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorScopeTest : FunSpec({

    // =========================================================================
    // TEST REGISTRY
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from sealed class hierarchies.
     */
    val testRegistry = object : ScopeRegistry {
        val scopes = mapOf(
            "MainTabs" to setOf(
                MainTabs.HomeTab::class,
                MainTabs.SettingsTab::class,
                MainTabs.ProfileTab::class
            )
        )

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    var keyCounter = 0

    fun createKeyGenerator(): () -> NodeKey {
        return { NodeKey("key-${keyCounter++}") }
    }

    beforeTest {
        keyCounter = 0
    }

    /**
     * Build a test tree:
     * ```
     * StackNode (root, key=NodeKey("root"))
     *   └── TabNode (key=NodeKey("tabs"), scopeKey="MainTabs")
     *        ├── StackNode (key=NodeKey("tab0")) ← ACTIVE
     *        │     └── ScreenNode (HomeTab)
     *        └── StackNode (key=NodeKey("tab1"))
     *              └── ScreenNode (SettingsTab)
     * ```
     */
    fun buildTestTree(): StackNode {
        val homeScreen = ScreenNode(
            key = NodeKey("home-screen"),
            parentKey = NodeKey("tab0"),
            destination = MainTabs.HomeTab
        )

        val settingsScreen = ScreenNode(
            key = NodeKey("settings-screen"),
            parentKey = NodeKey("tab1"),
            destination = MainTabs.SettingsTab
        )

        val homeStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(homeScreen)
        )

        val settingsStack = StackNode(
            key = NodeKey("tab1"),
            parentKey = NodeKey("tabs"),
            children = listOf(settingsScreen)
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = ScopeKey("MainTabs")
        )

        return StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
    }

    // =========================================================================
    // IN-SCOPE DESTINATION TESTS
    // =========================================================================

    test("push in-scope destination existing in another tab switches to that tab") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // SettingsTab exists in tab 1, currently active is tab 0
        val result = TreeMutator.push(tree, MainTabs.SettingsTab, testRegistry, generateKey = generateKey)

        // Should switch to tab 1 where SettingsTab already exists
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Active tab should now be tab 1 (settings tab)
        tabNode.activeStackIndex shouldBe 1

        // No new screens should be pushed - just tab switch
        tabNode.stacks[0].children.size shouldBe 1 // Home tab still has 1
        tabNode.stacks[1].children.size shouldBe 1 // Settings tab still has 1
    }

    test("push in-scope destination preserves tab state") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // HomeTab exists in tab 0 which is already active
        val result = TreeMutator.push(tree, MainTabs.HomeTab, testRegistry, generateKey = generateKey)

        // TabNode should still exist with same structure
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Tab structure preserved - no change since already on HomeTab
        tabNode.stacks.size shouldBe 2
        tabNode.activeStackIndex shouldBe 0
        tabNode.scopeKey shouldBe ScopeKey("MainTabs")
    }

    test("push in-scope destination not in any tab goes to active stack") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // ProfileTab is in scope but not in any tab's stack
        val result = TreeMutator.push(tree, MainTabs.ProfileTab, testRegistry, generateKey = generateKey)

        // Should push to the active tab's stack (tab 0)
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Active tab should still be tab 0
        tabNode.activeStackIndex shouldBe 0

        // Active stack should now have 2 children: HomeTab + ProfileTab
        val activeStack = tabNode.stacks[tabNode.activeStackIndex]
        activeStack.children.size shouldBe 2

        val topScreen = activeStack.children.last()
        topScreen.shouldBeInstanceOf<ScreenNode>()
        topScreen.destination shouldBe MainTabs.ProfileTab
    }

    // =========================================================================
    // OUT-OF-SCOPE DESTINATION TESTS
    // =========================================================================

    test("push out-of-scope destination goes to parent stack") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Should push to root stack, not the tab's active stack
        // Root should now have: TabNode + ScreenNode
        val resultStack = result as StackNode
        resultStack.children.size shouldBe 2

        // First child should still be TabNode
        resultStack.children[0].shouldBeInstanceOf<TabNode>()

        // Second child should be the new ScreenNode with out-of-scope destination
        val newScreen = resultStack.children[1]
        newScreen.shouldBeInstanceOf<ScreenNode>()
        newScreen.destination shouldBe OutOfScopeDestination
        newScreen.parentKey shouldBe NodeKey("root")
    }

    test("push out-of-scope destination preserves tab container") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // TabNode should be preserved with original state
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Tab structure unchanged
        tabNode.stacks.size shouldBe 2
        tabNode.activeStackIndex shouldBe 0
        tabNode.scopeKey shouldBe ScopeKey("MainTabs")

        // Tab content unchanged
        tabNode.stacks[0].children.size shouldBe 1
        tabNode.stacks[1].children.size shouldBe 1
    }

    test("push multiple out-of-scope destinations stacks correctly") {
        var tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // Push first out-of-scope destination
        tree = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey) as StackNode

        // Push second out-of-scope destination
        tree = TreeMutator.push(tree, DetailDestination, testRegistry, generateKey = generateKey) as StackNode

        // Root should now have: TabNode + OutOfScope + Detail
        tree.children.size shouldBe 3
        tree.children[0].shouldBeInstanceOf<TabNode>()
        tree.children[1].shouldBeInstanceOf<ScreenNode>()
        tree.children[2].shouldBeInstanceOf<ScreenNode>()

        (tree.children[1] as ScreenNode).destination shouldBe OutOfScopeDestination
        (tree.children[2] as ScreenNode).destination shouldBe DetailDestination
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS (BACKWARD COMPATIBILITY)
    // =========================================================================

    test("push with Empty registry always goes to active stack") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // With Empty registry, even out-of-scope destinations go to active stack
        val result = TreeMutator.push(tree, OutOfScopeDestination, ScopeRegistry.Empty, generateKey = generateKey)

        // Should push to active tab stack (backward compatible behavior)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        resultStack.children.size shouldBe 1
        resultStack.children[0].shouldBeInstanceOf<TabNode>()

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        activeStack.children.size shouldBe 2

        val topScreen = activeStack.children.last()
        topScreen.shouldBeInstanceOf<ScreenNode>()
        topScreen.destination shouldBe OutOfScopeDestination
    }

    test("push without scopeRegistry uses original behavior") {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // Use the overload without ScopeRegistry
        val result = TreeMutator.push(tree, OutOfScopeDestination, generateKey)

        // Should push to active stack (original behavior)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        resultStack.children.size shouldBe 1
        resultStack.children[0].shouldBeInstanceOf<TabNode>()

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        activeStack.children.size shouldBe 2
    }

    // =========================================================================
    // TAB WITHOUT SCOPE KEY TESTS
    // =========================================================================

    /**
     * Build a test tree WITHOUT scopeKey (legacy behavior):
     * ```
     * StackNode (root)
     *   └── TabNode (scopeKey=null)
     *        ├── StackNode (tab0) ← ACTIVE
     *        └── StackNode (tab1)
     * ```
     */
    fun buildTreeWithoutScopeKey(): StackNode {
        val homeScreen = ScreenNode(
            key = NodeKey("home-screen"),
            parentKey = NodeKey("tab0"),
            destination = MainTabs.HomeTab
        )

        val homeStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(homeScreen)
        )

        val settingsStack = StackNode(
            key = NodeKey("tab1"),
            parentKey = NodeKey("tabs"),
            children = emptyList()
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = null // No scope key!
        )

        return StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
    }

    test("push to TabNode without scopeKey goes to active stack") {
        val tree = buildTreeWithoutScopeKey()
        val generateKey = createKeyGenerator()

        // Even with scope registry, TabNode without scopeKey doesn't enforce scope
        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Should push to active tab stack (no scope enforcement)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        resultStack.children.size shouldBe 1
        resultStack.children[0].shouldBeInstanceOf<TabNode>()

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        activeStack.children.size shouldBe 2

        val topScreen = activeStack.children.last()
        topScreen.shouldBeInstanceOf<ScreenNode>()
        topScreen.destination shouldBe OutOfScopeDestination
    }

    // =========================================================================
    // NESTED STRUCTURE TESTS
    // =========================================================================

    /**
     * Build a more complex nested tree:
     * ```
     * StackNode (root)
     *   └── ScreenNode (some root screen)
     *   └── TabNode (scopeKey="MainTabs")
     *        ├── StackNode (tab0) ← ACTIVE
     *        │     └── ScreenNode (HomeTab)
     *        │     └── ScreenNode (HomeTab detail)
     *        └── StackNode (tab1)
     * ```
     */
    fun buildNestedTestTree(): StackNode {
        val rootScreen = ScreenNode(
            key = NodeKey("root-screen"),
            parentKey = NodeKey("root"),
            destination = MainTabs.HomeTab
        )

        val homeScreen = ScreenNode(
            key = NodeKey("home-screen"),
            parentKey = NodeKey("tab0"),
            destination = MainTabs.HomeTab
        )

        val homeDetailScreen = ScreenNode(
            key = NodeKey("home-detail"),
            parentKey = NodeKey("tab0"),
            destination = MainTabs.HomeTab
        )

        val homeStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(homeScreen, homeDetailScreen)
        )

        val settingsStack = StackNode(
            key = NodeKey("tab1"),
            parentKey = NodeKey("tabs"),
            children = emptyList()
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = ScopeKey("MainTabs")
        )

        return StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )
    }

    test("push out-of-scope in nested tree goes to correct parent") {
        val tree = buildNestedTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Out-of-scope should go to root stack
        val resultStack = result as StackNode

        // Root should now have 3 children: rootScreen, TabNode, OutOfScope
        resultStack.children.size shouldBe 3
        resultStack.children[0].shouldBeInstanceOf<ScreenNode>()
        resultStack.children[1].shouldBeInstanceOf<TabNode>()
        resultStack.children[2].shouldBeInstanceOf<ScreenNode>()

        // New screen should be at the end
        val newScreen = resultStack.children[2] as ScreenNode
        newScreen.destination shouldBe OutOfScopeDestination
        newScreen.parentKey shouldBe NodeKey("root")
    }

    test("push in-scope in nested tree goes to active tab stack") {
        val tree = buildNestedTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, MainTabs.SettingsTab, testRegistry, generateKey = generateKey)

        // In-scope should go to active tab stack
        val resultStack = result as StackNode

        // Root should still have 2 children
        resultStack.children.size shouldBe 2

        // Tab's active stack should have 3 children now
        val tabNode = resultStack.children[1] as TabNode
        val activeStack = tabNode.stacks[0]
        activeStack.children.size shouldBe 3

        // New screen at the end
        val topScreen = activeStack.children.last()
        topScreen.shouldBeInstanceOf<ScreenNode>()
        topScreen.destination shouldBe MainTabs.SettingsTab
    }

    // =========================================================================
    // ACTIVE STACK DETECTION TESTS
    // =========================================================================

    test("activeStack returns deepest active stack in tree") {
        val tree = buildTestTree()

        val activeStack = tree.activeStack()

        activeStack.shouldNotBeNull()
        activeStack.key shouldBe NodeKey("tab0")
    }

    test("activeStack follows active tab index") {
        // Create tree with second tab active
        val homeStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), MainTabs.HomeTab))
        )

        val settingsStack = StackNode(
            key = NodeKey("tab1"),
            parentKey = NodeKey("tabs"),
            children = listOf(ScreenNode(NodeKey("s2"), NodeKey("tab1"), MainTabs.SettingsTab))
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 1, // Settings tab active
            scopeKey = ScopeKey("MainTabs")
        )

        val tree = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val activeStack = tree.activeStack()

        activeStack.shouldNotBeNull()
        activeStack.key shouldBe NodeKey("tab1")
    }

})
