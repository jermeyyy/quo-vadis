@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for hierarchical rendering components.
 *
 * Tests cover:
 * - NavTreeRenderer routing logic
 * - Back navigation detection
 * - Tab metadata creation
 * - Pane content list building
 * - Component integration scenarios
 */
class NavTreeRendererTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(key, parentKey, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(key, parentKey, screens.toList())

    private fun createFakeScope(): FakeNavRenderScope = FakeNavRenderScope()

    // =========================================================================
    // NAV TREE RENDERER TESTS - NODE ROUTING
    // =========================================================================

    @Test
    fun `renderer creates FakeNavRenderScope with all dependencies`() {
        // Given
        val scope = createFakeScope()

        // Then
        assertNotNull(scope.navigator, "Navigator should be provided")
        assertNotNull(scope.cache, "Cache should be provided")
        assertNotNull(scope.saveableStateHolder, "SaveableStateHolder should be provided")
        assertNotNull(scope.animationCoordinator, "AnimationCoordinator should be provided")
        assertNotNull(scope.predictiveBackController, "PredictiveBackController should be provided")
        assertNotNull(scope.screenRegistry, "ScreenRegistry should be provided")
        assertNotNull(scope.containerRegistry, "ContainerRegistry should be provided")
        assertNull(scope.sharedTransitionScope, "SharedTransitionScope should be null by default")
    }

    @Test
    fun `scope navigator is functional`() {
        // Given
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()
        val destination = HomeDestination

        // When
        scope.navigator.navigate(destination)

        // Then
        assertTrue(
            navigator.navigationCalls.isNotEmpty(),
            "Navigator should have recorded navigation"
        )
    }

    @Test
    fun `scope container registry provides defaults`() {
        // Given
        val scope = createFakeScope()

        // Then
        assertEquals(ContainerRegistry.Empty, scope.containerRegistry)
        assertFalse(scope.containerRegistry.hasTabsContainer("any-key"))
        assertFalse(scope.containerRegistry.hasPaneContainer("any-key"))
    }

    // =========================================================================
    // SCREEN NODE RENDERING TESTS
    // =========================================================================

    @Test
    fun `screen node holds correct destination reference`() {
        // Given
        val destination = ProfileDestination
        val screenNode = createScreen("profile-screen", destination = destination)

        // Then
        assertEquals("profile-screen", screenNode.key)
        assertEquals(destination, screenNode.destination)
    }

    @Test
    fun `screen node can have null parent key for root screens`() {
        // Given
        val screenNode = createScreen("root-screen", parentKey = null)

        // Then
        assertNull(screenNode.parentKey)
    }

    @Test
    fun `screen node with parent key references correct parent`() {
        // Given
        val screenNode = createScreen("child-screen", parentKey = "parent-stack")

        // Then
        assertEquals("parent-stack", screenNode.parentKey)
    }

    // =========================================================================
    // STACK NODE RENDERING TESTS
    // =========================================================================

    @Test
    fun `stack node activeChild returns last element`() {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // Then
        assertEquals(screen3, stack.activeChild)
    }

    @Test
    fun `stack node activeChild returns null for empty stack`() {
        // Given
        val stack = StackNode("stack", null, emptyList())

        // Then
        assertNull(stack.activeChild)
    }

    @Test
    fun `stack node canGoBack is true when multiple children`() {
        // Given
        val stack = createStack(
            "stack",
            null,
            createScreen("s1", "stack"),
            createScreen("s2", "stack")
        )

        // Then
        assertTrue(stack.canGoBack)
    }

    @Test
    fun `stack node canGoBack is false when single child`() {
        // Given
        val stack = createStack("stack", null, createScreen("s1", "stack"))

        // Then
        assertFalse(stack.canGoBack)
    }

    @Test
    fun `stack renders only active child not all children`() {
        // Given
        val screen1 = createScreen("s1", "stack", HomeDestination)
        val screen2 = createScreen("s2", "stack", ProfileDestination)
        val stack = createStack("stack", null, screen1, screen2)

        // When
        val activeChild = stack.activeChild

        // Then - only the last screen is "active"
        assertEquals(screen2, activeChild)
        assertNotNull(activeChild)
        assertEquals("s2", activeChild.key)
    }

    // =========================================================================
    // TAB NODE RENDERING TESTS
    // =========================================================================

    @Test
    fun `tab node renders active stack correctly`() {
        // Given
        val homeStack = createStack(
            "home-stack", "tabs",
            createScreen("home", "home-stack")
        )
        val profileStack = createStack(
            "profile-stack", "tabs",
            createScreen("profile", "profile-stack")
        )
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1
        )

        // Then
        assertEquals(profileStack, tabs.activeStack)
        assertEquals(1, tabs.activeStackIndex)
        assertEquals(2, tabs.tabCount)
    }

    @Test
    fun `tab switching changes active stack`() {
        // Given
        val stack0 = createStack("tab0", "tabs", createScreen("s0", "tab0"))
        val stack1 = createStack("tab1", "tabs", createScreen("s1", "tab1"))
        val stack2 = createStack("tab2", "tabs", createScreen("s2", "tab2"))

        val previousTabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 0
        )

        val currentTabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 2
        )

        // Then
        assertEquals(stack0, previousTabs.activeStack)
        assertEquals(stack2, currentTabs.activeStack)
    }

    @Test
    fun `tab node preserves all stacks during switching`() {
        // Given
        val homeStack = createStack(
            "home-stack", "tabs",
            createScreen("home-1", "home-stack"),
            createScreen("home-2", "home-stack")
        )
        val profileStack = createStack(
            "profile-stack", "tabs",
            createScreen("profile-1", "profile-stack")
        )

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1
        )

        // Then - all stacks are preserved
        assertEquals(2, tabs.stacks.size)
        assertEquals(homeStack, tabs.stackAt(0))
        assertEquals(profileStack, tabs.stackAt(1))

        // Home stack still has 2 children
        assertEquals(2, tabs.stackAt(0).children.size)
    }

    // =========================================================================
    // PANE NODE RENDERING TESTS
    // =========================================================================

    @Test
    fun `pane node renders active pane correctly`() {
        // Given
        val primaryContent = createScreen("primary", "panes", HomeDestination)
        val supportingContent = createScreen("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Supporting
        )

        // Then
        assertEquals(supportingContent, panes.activePaneContent)
        assertEquals(2, panes.paneCount)
    }

    @Test
    fun `pane node respects adapt strategy`() {
        // Given
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        // Then
        assertEquals(AdaptStrategy.Hide, panes.adaptStrategy(PaneRole.Primary))
        assertEquals(AdaptStrategy.Levitate, panes.adaptStrategy(PaneRole.Supporting))
    }

    @Test
    fun `pane node has correct back behavior`() {
        // Given - default back behavior
        val panesDefault = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("p", "panes"))
            ),
            activePaneRole = PaneRole.Primary
        )

        // Given - custom back behavior
        val panesCustom = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("p", "panes"))
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        // Then
        assertEquals(PaneBackBehavior.PopUntilScaffoldValueChange, panesDefault.backBehavior)
        assertEquals(PaneBackBehavior.PopLatest, panesCustom.backBehavior)
    }

    // =========================================================================
    // COMPLEX STRUCTURE TESTS
    // =========================================================================

    @Test
    fun `nested navigation structures work correctly`() {
        // Given - a complex nested structure: Root Stack -> Tabs -> Stack per tab
        val homeScreen1 = createScreen("home-1", "home-stack", HomeDestination)
        val homeScreen2 = createScreen("home-2", "home-stack", ProfileDestination)
        val homeStack = createStack("home-stack", "tabs", homeScreen1, homeScreen2)

        val settingsScreen = createScreen("settings", "settings-stack", SettingsDestination)
        val settingsStack = createStack("settings-stack", "tabs", settingsScreen)

        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0
        )

        val rootStack = StackNode("root", null, listOf(tabs))

        // Then - verify the full hierarchy
        assertEquals(tabs, rootStack.activeChild)
        assertEquals(homeStack, tabs.activeStack)
        assertEquals(homeScreen2, tabs.activeStack.activeChild)
    }

    @Test
    fun `pane with nested stack works correctly`() {
        // Given
        val listScreen = createScreen("list", "list-stack")
        val listStack = createStack("list-stack", "panes", listScreen)

        val detailScreen1 = createScreen("detail-1", "detail-stack")
        val detailScreen2 = createScreen("detail-2", "detail-stack")
        val detailStack = createStack("detail-stack", "panes", detailScreen1, detailScreen2)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(listStack),
                PaneRole.Supporting to PaneConfiguration(detailStack)
            ),
            activePaneRole = PaneRole.Supporting
        )

        // Then
        val activePaneContent = panes.activePaneContent
        assertTrue(activePaneContent is StackNode)
        assertEquals(detailStack, activePaneContent)
        assertEquals(detailScreen2, (activePaneContent as StackNode).activeChild)
    }

    // =========================================================================
    // CACHE KEY UNIQUENESS TESTS
    // =========================================================================

    @Test
    fun `screen nodes have unique keys`() {
        // Given
        val screen1 = createScreen("screen-1")
        val screen2 = createScreen("screen-2")
        val screen3 = createScreen("screen-3")

        // Then
        val keys = setOf(screen1.key, screen2.key, screen3.key)
        assertEquals(3, keys.size, "All screen keys should be unique")
    }

    @Test
    fun `stack nodes have unique keys`() {
        // Given
        val stack1 = createStack("stack-1")
        val stack2 = createStack("stack-2")
        val stack3 = createStack("stack-3")

        // Then
        val keys = setOf(stack1.key, stack2.key, stack3.key)
        assertEquals(3, keys.size, "All stack keys should be unique")
    }
}
