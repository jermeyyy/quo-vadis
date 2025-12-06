package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for NavigationStateHolder helper functions.
 *
 * Tests cover:
 * - collectAllKeys: tree traversal to collect all node keys
 * - findAllTabNodes: finding TabNodes in navigation tree
 * - findAllPaneNodes: finding PaneNodes in navigation tree
 * - CacheScope enum verification
 *
 * Note: NavigationStateHolder class methods that require SaveableStateHolder
 * are tested in instrumented tests with Compose runtime.
 */
class NavigationStateHolderTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ListDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun mockScreen(key: String, parentKey: String? = null): ScreenNode {
        return ScreenNode(key, parentKey, HomeDestination)
    }

    private fun mockStack(
        key: String,
        parentKey: String? = null,
        children: List<ScreenNode> = emptyList()
    ): StackNode {
        return StackNode(key, parentKey, children)
    }

    // =========================================================================
    // COLLECT ALL KEYS TESTS
    // =========================================================================

    @Test
    fun `collectAllKeys returns single key for ScreenNode`() {
        val screen = ScreenNode(
            key = "screen-1",
            parentKey = null,
            destination = HomeDestination
        )

        val keys = collectAllKeys(screen)

        assertEquals(setOf("screen-1"), keys)
    }

    @Test
    fun `collectAllKeys finds all keys in StackNode`() {
        val screen1 = mockScreen("s1", "stack")
        val screen2 = mockScreen("s2", "stack")
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        val keys = collectAllKeys(stack)

        assertEquals(setOf("stack", "s1", "s2"), keys)
    }

    @Test
    fun `collectAllKeys finds all keys in tree`() {
        val screen1 = mockScreen("s1", "stack")
        val screen2 = mockScreen("s2", "stack")
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        val keys = collectAllKeys(stack)

        assertEquals(setOf("stack", "s1", "s2"), keys)
    }

    @Test
    fun `collectAllKeys handles tabs`() {
        val homeScreen = mockScreen("home", "home-stack")
        val profileScreen = mockScreen("profile", "profile-stack")

        val homeStack = mockStack("home-stack", "tabs", listOf(homeScreen))
        val profileStack = mockStack("profile-stack", "tabs", listOf(profileScreen))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 0
        )

        val keys = collectAllKeys(tabs)

        assertEquals(
            setOf("tabs", "home-stack", "home", "profile-stack", "profile"),
            keys
        )
    }

    @Test
    fun `collectAllKeys handles panes`() {
        val primaryScreen = ScreenNode("primary-screen", "primary-stack", ListDestination)
        val supportingScreen = ScreenNode("supporting-screen", "supporting-stack", DetailDestination)

        val primaryStack = StackNode("primary-stack", "panes", listOf(primaryScreen))
        val supportingStack = StackNode("supporting-stack", "panes", listOf(supportingScreen))

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            ),
            activePaneRole = PaneRole.Primary
        )

        val keys = collectAllKeys(panes)

        assertEquals(
            setOf("panes", "primary-stack", "primary-screen", "supporting-stack", "supporting-screen"),
            keys
        )
    }

    @Test
    fun `collectAllKeys handles complex nested structures`() {
        // Deep nesting: TabNode > StackNode > ScreenNode
        val homeScreen1 = mockScreen("home-1", "home-stack")
        val homeScreen2 = mockScreen("home-2", "home-stack")
        val profileScreen = mockScreen("profile", "profile-stack")
        val settingsScreen = mockScreen("settings", "settings-stack")

        val homeStack = StackNode("home-stack", "tabs", listOf(homeScreen1, homeScreen2))
        val profileStack = StackNode("profile-stack", "tabs", listOf(profileScreen))
        val settingsStack = StackNode("settings-stack", "tabs", listOf(settingsScreen))

        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, profileStack, settingsStack),
            activeStackIndex = 0
        )

        val root = StackNode("root", null, listOf(tabs))

        val keys = collectAllKeys(root)

        assertEquals(
            setOf(
                "root", "tabs",
                "home-stack", "home-1", "home-2",
                "profile-stack", "profile",
                "settings-stack", "settings"
            ),
            keys
        )
    }

    @Test
    fun `collectAllKeys handles tabs with nested panes`() {
        // Complex: TabNode with PaneNode inside
        val listScreen = ScreenNode("list", "list-stack", ListDestination)
        val detailScreen = ScreenNode("detail", "detail-stack", DetailDestination)

        val listStack = StackNode("list-stack", "panes", listOf(listScreen))
        val detailStack = StackNode("detail-stack", "panes", listOf(detailScreen))

        val panes = PaneNode(
            key = "panes",
            parentKey = "tab-stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(listStack),
                PaneRole.Supporting to PaneConfiguration(detailStack)
            ),
            activePaneRole = PaneRole.Primary
        )

        val tabStack = StackNode("tab-stack", "tabs", listOf(panes))
        val otherStack = StackNode("other-stack", "tabs", listOf(mockScreen("other", "other-stack")))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(tabStack, otherStack),
            activeStackIndex = 0
        )

        val keys = collectAllKeys(tabs)

        assertEquals(
            setOf(
                "tabs",
                "tab-stack", "panes", "list-stack", "list", "detail-stack", "detail",
                "other-stack", "other"
            ),
            keys
        )
    }

    @Test
    fun `collectAllKeys returns empty keys set for empty stack`() {
        val stack = StackNode("empty-stack", null, emptyList())

        val keys = collectAllKeys(stack)

        assertEquals(setOf("empty-stack"), keys)
    }

    // =========================================================================
    // FIND ALL TAB NODES TESTS
    // =========================================================================

    @Test
    fun `findAllTabNodes finds TabNodes in tree`() {
        val homeStack = mockStack("home-stack", "tabs")
        val profileStack = mockStack("profile-stack", "tabs")

        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 0
        )

        val root = StackNode("root", null, listOf(tabs))

        val tabNodes = findAllTabNodes(root)

        assertEquals(1, tabNodes.size)
        assertEquals(tabs, tabNodes[0])
    }

    @Test
    fun `findAllTabNodes returns empty for tree without tabs`() {
        val screen1 = mockScreen("s1", "stack")
        val screen2 = mockScreen("s2", "stack")
        val stack = StackNode("stack", null, listOf(screen1, screen2))

        val tabNodes = findAllTabNodes(stack)

        assertTrue(tabNodes.isEmpty())
    }

    @Test
    fun `findAllTabNodes returns empty for single ScreenNode`() {
        val screen = mockScreen("screen")

        val tabNodes = findAllTabNodes(screen)

        assertTrue(tabNodes.isEmpty())
    }

    @Test
    fun `findAllTabNodes finds nested TabNodes`() {
        // Inner tabs nested inside outer tabs
        val innerStack1 = mockStack("inner-stack-1", "inner-tabs")
        val innerStack2 = mockStack("inner-stack-2", "inner-tabs")

        val innerTabs = TabNode(
            key = "inner-tabs",
            parentKey = "outer-stack-1",
            stacks = listOf(innerStack1, innerStack2),
            activeStackIndex = 0
        )

        val outerStack1 = StackNode("outer-stack-1", "outer-tabs", listOf(innerTabs))
        val outerStack2 = mockStack("outer-stack-2", "outer-tabs")

        val outerTabs = TabNode(
            key = "outer-tabs",
            parentKey = null,
            stacks = listOf(outerStack1, outerStack2),
            activeStackIndex = 0
        )

        val tabNodes = findAllTabNodes(outerTabs)

        assertEquals(2, tabNodes.size)
        assertTrue(tabNodes.contains(outerTabs))
        assertTrue(tabNodes.contains(innerTabs))
    }

    @Test
    fun `findAllTabNodes finds TabNode inside PaneNode`() {
        val tabStack1 = mockStack("tab-stack-1", "tabs")
        val tabStack2 = mockStack("tab-stack-2", "tabs")

        val tabs = TabNode(
            key = "tabs",
            parentKey = "primary-stack",
            stacks = listOf(tabStack1, tabStack2),
            activeStackIndex = 0
        )

        val primaryStack = StackNode("primary-stack", "panes", listOf(tabs))
        val supportingScreen = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val tabNodes = findAllTabNodes(panes)

        assertEquals(1, tabNodes.size)
        assertEquals(tabs, tabNodes[0])
    }

    @Test
    fun `findAllTabNodes finds multiple TabNodes in different branches`() {
        val tabs1Stack = mockStack("tabs1-stack", "tabs1")
        val tabs1 = TabNode(
            key = "tabs1",
            parentKey = "panes",
            stacks = listOf(tabs1Stack),
            activeStackIndex = 0
        )

        val tabs2Stack = mockStack("tabs2-stack", "tabs2")
        val tabs2 = TabNode(
            key = "tabs2",
            parentKey = "panes",
            stacks = listOf(tabs2Stack),
            activeStackIndex = 0
        )

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(tabs1),
                PaneRole.Supporting to PaneConfiguration(tabs2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val tabNodes = findAllTabNodes(panes)

        assertEquals(2, tabNodes.size)
        assertTrue(tabNodes.contains(tabs1))
        assertTrue(tabNodes.contains(tabs2))
    }

    // =========================================================================
    // FIND ALL PANE NODES TESTS
    // =========================================================================

    @Test
    fun `findAllPaneNodes finds PaneNodes in tree`() {
        val primaryScreen = ScreenNode("primary", "panes", ListDestination)
        val supportingScreen = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryScreen),
                PaneRole.Supporting to PaneConfiguration(supportingScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val root = StackNode("root", null, listOf(panes))

        val paneNodes = findAllPaneNodes(root)

        assertEquals(1, paneNodes.size)
        assertEquals(panes, paneNodes[0])
    }

    @Test
    fun `findAllPaneNodes returns empty for tree without panes`() {
        val screen1 = mockScreen("s1", "stack")
        val screen2 = mockScreen("s2", "stack")
        val stack = StackNode("stack", null, listOf(screen1, screen2))

        val paneNodes = findAllPaneNodes(stack)

        assertTrue(paneNodes.isEmpty())
    }

    @Test
    fun `findAllPaneNodes returns empty for single ScreenNode`() {
        val screen = mockScreen("screen")

        val paneNodes = findAllPaneNodes(screen)

        assertTrue(paneNodes.isEmpty())
    }

    @Test
    fun `findAllPaneNodes returns empty for TabNode without panes`() {
        val stack1 = mockStack("stack1", "tabs")
        val stack2 = mockStack("stack2", "tabs")

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        val paneNodes = findAllPaneNodes(tabs)

        assertTrue(paneNodes.isEmpty())
    }

    @Test
    fun `findAllPaneNodes finds nested PaneNodes`() {
        // Inner panes nested inside outer panes
        val innerPrimaryScreen = ScreenNode("inner-primary", "inner-panes", ListDestination)
        val innerSupportingScreen = ScreenNode("inner-supporting", "inner-panes", DetailDestination)

        val innerPanes = PaneNode(
            key = "inner-panes",
            parentKey = "outer-panes",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerPrimaryScreen),
                PaneRole.Supporting to PaneConfiguration(innerSupportingScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val outerPanes = PaneNode(
            key = "outer-panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerPanes)
            ),
            activePaneRole = PaneRole.Primary
        )

        val paneNodes = findAllPaneNodes(outerPanes)

        assertEquals(2, paneNodes.size)
        assertTrue(paneNodes.contains(outerPanes))
        assertTrue(paneNodes.contains(innerPanes))
    }

    @Test
    fun `findAllPaneNodes finds PaneNode inside TabNode`() {
        val primaryScreen = ScreenNode("primary", "panes", ListDestination)
        val supportingScreen = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = "tab-stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryScreen),
                PaneRole.Supporting to PaneConfiguration(supportingScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val tabStack = StackNode("tab-stack", "tabs", listOf(panes))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )

        val paneNodes = findAllPaneNodes(tabs)

        assertEquals(1, paneNodes.size)
        assertEquals(panes, paneNodes[0])
    }

    @Test
    fun `findAllPaneNodes finds multiple PaneNodes in different tabs`() {
        val panes1 = PaneNode(
            key = "panes1",
            parentKey = "tab1-stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(mockScreen("p1-primary", "panes1"))
            ),
            activePaneRole = PaneRole.Primary
        )

        val panes2 = PaneNode(
            key = "panes2",
            parentKey = "tab2-stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(mockScreen("p2-primary", "panes2"))
            ),
            activePaneRole = PaneRole.Primary
        )

        val tab1Stack = StackNode("tab1-stack", "tabs", listOf(panes1))
        val tab2Stack = StackNode("tab2-stack", "tabs", listOf(panes2))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(tab1Stack, tab2Stack),
            activeStackIndex = 0
        )

        val paneNodes = findAllPaneNodes(tabs)

        assertEquals(2, paneNodes.size)
        assertTrue(paneNodes.contains(panes1))
        assertTrue(paneNodes.contains(panes2))
    }

    // =========================================================================
    // CACHE SCOPE TESTS
    // =========================================================================

    @Test
    fun `CacheScope FULL_SCREEN exists`() {
        val scope = CacheScope.FULL_SCREEN

        assertEquals(CacheScope.FULL_SCREEN, scope)
    }

    @Test
    fun `CacheScope WHOLE_WRAPPER exists`() {
        val scope = CacheScope.WHOLE_WRAPPER

        assertEquals(CacheScope.WHOLE_WRAPPER, scope)
    }

    @Test
    fun `CacheScope CONTENT_ONLY exists`() {
        val scope = CacheScope.CONTENT_ONLY

        assertEquals(CacheScope.CONTENT_ONLY, scope)
    }

    @Test
    fun `CacheScope has exactly three values`() {
        val values = CacheScope.entries

        assertEquals(3, values.size)
        assertTrue(values.contains(CacheScope.FULL_SCREEN))
        assertTrue(values.contains(CacheScope.WHOLE_WRAPPER))
        assertTrue(values.contains(CacheScope.CONTENT_ONLY))
    }

    @Test
    fun `CacheScope values are distinct`() {
        val fullScreen = CacheScope.FULL_SCREEN
        val wholeWrapper = CacheScope.WHOLE_WRAPPER
        val contentOnly = CacheScope.CONTENT_ONLY

        assertFalse(fullScreen == wholeWrapper)
        assertFalse(wholeWrapper == contentOnly)
        assertFalse(fullScreen == contentOnly)
    }

    // =========================================================================
    // EDGE CASES AND INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `collectAllKeys and findAllTabNodes produce consistent results`() {
        val homeStack = mockStack("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val profileStack = mockStack("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 0
        )

        val allKeys = collectAllKeys(tabs)
        val allTabNodes = findAllTabNodes(tabs)

        // All tab node keys should be in the collected keys
        allTabNodes.forEach { tabNode ->
            assertTrue(allKeys.contains(tabNode.key))
        }
    }

    @Test
    fun `collectAllKeys and findAllPaneNodes produce consistent results`() {
        val primaryScreen = ScreenNode("primary", "panes", ListDestination)
        val supportingScreen = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryScreen),
                PaneRole.Supporting to PaneConfiguration(supportingScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val allKeys = collectAllKeys(panes)
        val allPaneNodes = findAllPaneNodes(panes)

        // All pane node keys should be in the collected keys
        allPaneNodes.forEach { paneNode ->
            assertTrue(allKeys.contains(paneNode.key))
        }
    }

    @Test
    fun `deep hierarchy with mixed node types`() {
        // Build a complex hierarchy:
        // root (StackNode)
        //   └── tabs (TabNode)
        //       ├── tab0-stack (StackNode)
        //       │   └── panes (PaneNode)
        //       │       ├── Primary: list-stack (StackNode)
        //       │       │   └── list-screen (ScreenNode)
        //       │       └── Supporting: detail-stack (StackNode)
        //       │           └── detail-screen (ScreenNode)
        //       └── tab1-stack (StackNode)
        //           └── settings-screen (ScreenNode)

        val listScreen = ScreenNode("list-screen", "list-stack", ListDestination)
        val detailScreen = ScreenNode("detail-screen", "detail-stack", DetailDestination)
        val settingsScreen = mockScreen("settings-screen", "tab1-stack")

        val listStack = StackNode("list-stack", "panes", listOf(listScreen))
        val detailStack = StackNode("detail-stack", "panes", listOf(detailScreen))

        val panes = PaneNode(
            key = "panes",
            parentKey = "tab0-stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(listStack),
                PaneRole.Supporting to PaneConfiguration(detailStack)
            ),
            activePaneRole = PaneRole.Primary
        )

        val tab0Stack = StackNode("tab0-stack", "tabs", listOf(panes))
        val tab1Stack = StackNode("tab1-stack", "tabs", listOf(settingsScreen))

        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tab0Stack, tab1Stack),
            activeStackIndex = 0
        )

        val root = StackNode("root", null, listOf(tabs))

        // Test collectAllKeys
        val allKeys = collectAllKeys(root)
        assertEquals(
            setOf(
                "root", "tabs",
                "tab0-stack", "panes", "list-stack", "list-screen", "detail-stack", "detail-screen",
                "tab1-stack", "settings-screen"
            ),
            allKeys
        )

        // Test findAllTabNodes
        val tabNodes = findAllTabNodes(root)
        assertEquals(1, tabNodes.size)
        assertEquals(tabs, tabNodes[0])

        // Test findAllPaneNodes
        val paneNodes = findAllPaneNodes(root)
        assertEquals(1, paneNodes.size)
        assertEquals(panes, paneNodes[0])
    }

    @Test
    fun `deeply nested tabs and panes`() {
        // Build a complex hierarchy with nested tabs and panes:
        // tabs1 (TabNode)
        //   └── stack (StackNode)
        //       └── panes (PaneNode)
        //           └── Primary: tabs2 (TabNode)
        //               └── inner-stack (StackNode)
        //                   └── screen (ScreenNode)

        val screen = mockScreen("screen", "inner-stack")
        val innerStack = mockStack("inner-stack", "tabs2", listOf(screen))

        val tabs2 = TabNode(
            key = "tabs2",
            parentKey = "panes",
            stacks = listOf(innerStack),
            activeStackIndex = 0
        )

        val panes = PaneNode(
            key = "panes",
            parentKey = "stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(tabs2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val stack = StackNode("stack", "tabs1", listOf(panes))

        val tabs1 = TabNode(
            key = "tabs1",
            parentKey = null,
            stacks = listOf(stack),
            activeStackIndex = 0
        )

        // Verify all keys collected
        val allKeys = collectAllKeys(tabs1)
        assertEquals(
            setOf("tabs1", "stack", "panes", "tabs2", "inner-stack", "screen"),
            allKeys
        )

        // Verify both TabNodes found
        val tabNodes = findAllTabNodes(tabs1)
        assertEquals(2, tabNodes.size)
        assertTrue(tabNodes.contains(tabs1))
        assertTrue(tabNodes.contains(tabs2))

        // Verify PaneNode found
        val paneNodes = findAllPaneNodes(tabs1)
        assertEquals(1, paneNodes.size)
        assertEquals(panes, paneNodes[0])
    }
}
