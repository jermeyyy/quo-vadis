package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavigationTransition
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.activeStack
import com.jermey.quo.vadis.core.navigation.tree.TreeMutator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Tests for scope-aware [com.jermey.quo.vadis.core.navigation.tree.TreeMutator] operations.
 *
 * These tests verify that `TreeMutator.push` with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] correctly routes
 * destinations based on whether they belong to a container's scope.
 */
class TreeMutatorScopeTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    /**
     * Simulates a sealed interface for tab destinations.
     * These are "in scope" for a TabNode with scopeKey="MainTabs".
     */
    private sealed interface MainTabs : NavDestination {
        data object HomeTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "HomeTab"
        }

        data object SettingsTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "SettingsTab"
        }

        /**
         * An in-scope destination that is NOT pre-populated in any tab.
         * Should be pushed to the active tab's stack.
         */
        data object ProfileTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "ProfileTab"
        }
    }

    /**
     * A destination that is NOT part of MainTabs scope.
     * Should be pushed outside the tab container.
     */
    private data object OutOfScopeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "OutOfScope"
    }

    /**
     * Another out-of-scope destination for variation.
     */
    private data object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "Detail"
    }

    // =========================================================================
    // TEST REGISTRY
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from sealed class hierarchies.
     */
    private val testRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            "MainTabs" to setOf(
                MainTabs.HomeTab::class,
                MainTabs.SettingsTab::class,
                MainTabs.ProfileTab::class
            )
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    private var keyCounter = 0

    private fun createKeyGenerator(): () -> String {
        return { "key-${keyCounter++}" }
    }

    @BeforeTest
    fun setup() {
        keyCounter = 0
    }

    /**
     * Build a test tree:
     * ```
     * StackNode (root, key="root")
     *   └── TabNode (key="tabs", scopeKey="MainTabs")
     *        ├── StackNode (key="tab0") ← ACTIVE
     *        │     └── ScreenNode (HomeTab)
     *        └── StackNode (key="tab1")
     *              └── ScreenNode (SettingsTab)
     * ```
     */
    private fun buildTestTree(): StackNode {
        val homeScreen = ScreenNode(
            key = "home-screen",
            parentKey = "tab0",
            destination = MainTabs.HomeTab
        )

        val settingsScreen = ScreenNode(
            key = "settings-screen",
            parentKey = "tab1",
            destination = MainTabs.SettingsTab
        )

        val homeStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(homeScreen)
        )

        val settingsStack = StackNode(
            key = "tab1",
            parentKey = "tabs",
            children = listOf(settingsScreen)
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = "MainTabs"
        )

        return StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
    }

    // =========================================================================
    // IN-SCOPE DESTINATION TESTS
    // =========================================================================

    @Test
    fun `push in-scope destination existing in another tab switches to that tab`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // SettingsTab exists in tab 1, currently active is tab 0
        val result = TreeMutator.push(tree, MainTabs.SettingsTab, testRegistry, generateKey = generateKey)

        // Should switch to tab 1 where SettingsTab already exists
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Active tab should now be tab 1 (settings tab)
        assertEquals(1, tabNode.activeStackIndex)

        // No new screens should be pushed - just tab switch
        assertEquals(1, tabNode.stacks[0].children.size) // Home tab still has 1
        assertEquals(1, tabNode.stacks[1].children.size) // Settings tab still has 1
    }

    @Test
    fun `push in-scope destination preserves tab state`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // HomeTab exists in tab 0 which is already active
        val result = TreeMutator.push(tree, MainTabs.HomeTab, testRegistry, generateKey = generateKey)

        // TabNode should still exist with same structure
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Tab structure preserved - no change since already on HomeTab
        assertEquals(2, tabNode.stacks.size)
        assertEquals(0, tabNode.activeStackIndex)
        assertEquals("MainTabs", tabNode.scopeKey)
    }

    @Test
    fun `push in-scope destination not in any tab goes to active stack`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // ProfileTab is in scope but not in any tab's stack
        val result = TreeMutator.push(tree, MainTabs.ProfileTab, testRegistry, generateKey = generateKey)

        // Should push to the active tab's stack (tab 0)
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Active tab should still be tab 0
        assertEquals(0, tabNode.activeStackIndex)

        // Active stack should now have 2 children: HomeTab + ProfileTab
        val activeStack = tabNode.stacks[tabNode.activeStackIndex]
        assertEquals(2, activeStack.children.size)

        val topScreen = activeStack.children.last()
        assertIs<ScreenNode>(topScreen)
        assertEquals(MainTabs.ProfileTab, topScreen.destination)
    }

    // =========================================================================
    // OUT-OF-SCOPE DESTINATION TESTS
    // =========================================================================

    @Test
    fun `push out-of-scope destination goes to parent stack`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Should push to root stack, not the tab's active stack
        // Root should now have: TabNode + ScreenNode
        val resultStack = result as StackNode
        assertEquals(2, resultStack.children.size)

        // First child should still be TabNode
        assertIs<TabNode>(resultStack.children[0])

        // Second child should be the new ScreenNode with out-of-scope destination
        val newScreen = resultStack.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(OutOfScopeDestination, newScreen.destination)
        assertEquals("root", newScreen.parentKey)
    }

    @Test
    fun `push out-of-scope destination preserves tab container`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // TabNode should be preserved with original state
        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode

        // Tab structure unchanged
        assertEquals(2, tabNode.stacks.size)
        assertEquals(0, tabNode.activeStackIndex)
        assertEquals("MainTabs", tabNode.scopeKey)

        // Tab content unchanged
        assertEquals(1, tabNode.stacks[0].children.size)
        assertEquals(1, tabNode.stacks[1].children.size)
    }

    @Test
    fun `push multiple out-of-scope destinations stacks correctly`() {
        var tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // Push first out-of-scope destination
        tree = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey) as StackNode

        // Push second out-of-scope destination
        tree = TreeMutator.push(tree, DetailDestination, testRegistry, generateKey = generateKey) as StackNode

        // Root should now have: TabNode + OutOfScope + Detail
        assertEquals(3, tree.children.size)
        assertIs<TabNode>(tree.children[0])
        assertIs<ScreenNode>(tree.children[1])
        assertIs<ScreenNode>(tree.children[2])

        assertEquals(OutOfScopeDestination, (tree.children[1] as ScreenNode).destination)
        assertEquals(DetailDestination, (tree.children[2] as ScreenNode).destination)
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS (BACKWARD COMPATIBILITY)
    // =========================================================================

    @Test
    fun `push with Empty registry always goes to active stack`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // With Empty registry, even out-of-scope destinations go to active stack
        val result = TreeMutator.push(tree, OutOfScopeDestination, ScopeRegistry.Empty, generateKey = generateKey)

        // Should push to active tab stack (backward compatible behavior)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        assertEquals(1, resultStack.children.size)
        assertIs<TabNode>(resultStack.children[0])

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        assertEquals(2, activeStack.children.size)

        val topScreen = activeStack.children.last()
        assertIs<ScreenNode>(topScreen)
        assertEquals(OutOfScopeDestination, topScreen.destination)
    }

    @Test
    fun `push without scopeRegistry uses original behavior`() {
        val tree = buildTestTree()
        val generateKey = createKeyGenerator()

        // Use the overload without ScopeRegistry
        val result = TreeMutator.push(tree, OutOfScopeDestination, generateKey)

        // Should push to active stack (original behavior)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        assertEquals(1, resultStack.children.size)
        assertIs<TabNode>(resultStack.children[0])

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        assertEquals(2, activeStack.children.size)
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
    private fun buildTreeWithoutScopeKey(): StackNode {
        val homeScreen = ScreenNode(
            key = "home-screen",
            parentKey = "tab0",
            destination = MainTabs.HomeTab
        )

        val homeStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(homeScreen)
        )

        val settingsStack = StackNode(
            key = "tab1",
            parentKey = "tabs",
            children = emptyList()
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = null // No scope key!
        )

        return StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
    }

    @Test
    fun `push to TabNode without scopeKey goes to active stack`() {
        val tree = buildTreeWithoutScopeKey()
        val generateKey = createKeyGenerator()

        // Even with scope registry, TabNode without scopeKey doesn't enforce scope
        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Should push to active tab stack (no scope enforcement)
        val resultStack = result as StackNode

        // Root should still have only TabNode
        assertEquals(1, resultStack.children.size)
        assertIs<TabNode>(resultStack.children[0])

        // Active tab stack should have new screen
        val tabNode = resultStack.children[0] as TabNode
        val activeStack = tabNode.stacks[0]
        assertEquals(2, activeStack.children.size)

        val topScreen = activeStack.children.last()
        assertIs<ScreenNode>(topScreen)
        assertEquals(OutOfScopeDestination, topScreen.destination)
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
    private fun buildNestedTestTree(): StackNode {
        val rootScreen = ScreenNode(
            key = "root-screen",
            parentKey = "root",
            destination = MainTabs.HomeTab
        )

        val homeScreen = ScreenNode(
            key = "home-screen",
            parentKey = "tab0",
            destination = MainTabs.HomeTab
        )

        val homeDetailScreen = ScreenNode(
            key = "home-detail",
            parentKey = "tab0",
            destination = MainTabs.HomeTab
        )

        val homeStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(homeScreen, homeDetailScreen)
        )

        val settingsStack = StackNode(
            key = "tab1",
            parentKey = "tabs",
            children = emptyList()
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0,
            scopeKey = "MainTabs"
        )

        return StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )
    }

    @Test
    fun `push out-of-scope in nested tree goes to correct parent`() {
        val tree = buildNestedTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, OutOfScopeDestination, testRegistry, generateKey = generateKey)

        // Out-of-scope should go to root stack
        val resultStack = result as StackNode

        // Root should now have 3 children: rootScreen, TabNode, OutOfScope
        assertEquals(3, resultStack.children.size)
        assertIs<ScreenNode>(resultStack.children[0])
        assertIs<TabNode>(resultStack.children[1])
        assertIs<ScreenNode>(resultStack.children[2])

        // New screen should be at the end
        val newScreen = resultStack.children[2] as ScreenNode
        assertEquals(OutOfScopeDestination, newScreen.destination)
        assertEquals("root", newScreen.parentKey)
    }

    @Test
    fun `push in-scope in nested tree goes to active tab stack`() {
        val tree = buildNestedTestTree()
        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(tree, MainTabs.SettingsTab, testRegistry, generateKey = generateKey)

        // In-scope should go to active tab stack
        val resultStack = result as StackNode

        // Root should still have 2 children
        assertEquals(2, resultStack.children.size)

        // Tab's active stack should have 3 children now
        val tabNode = resultStack.children[1] as TabNode
        val activeStack = tabNode.stacks[0]
        assertEquals(3, activeStack.children.size)

        // New screen at the end
        val topScreen = activeStack.children.last()
        assertIs<ScreenNode>(topScreen)
        assertEquals(MainTabs.SettingsTab, topScreen.destination)
    }

    // =========================================================================
    // ACTIVE STACK DETECTION TESTS
    // =========================================================================

    @Test
    fun `activeStack returns deepest active stack in tree`() {
        val tree = buildTestTree()

        val activeStack = tree.activeStack()

        assertNotNull(activeStack)
        assertEquals("tab0", activeStack.key)
    }

    @Test
    fun `activeStack follows active tab index`() {
        // Create tree with second tab active
        val homeStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(ScreenNode("s1", "tab0", MainTabs.HomeTab))
        )

        val settingsStack = StackNode(
            key = "tab1",
            parentKey = "tabs",
            children = listOf(ScreenNode("s2", "tab1", MainTabs.SettingsTab))
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 1, // Settings tab active
            scopeKey = "MainTabs"
        )

        val tree = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val activeStack = tree.activeStack()

        assertNotNull(activeStack)
        assertEquals("tab1", activeStack.key)
    }
}
