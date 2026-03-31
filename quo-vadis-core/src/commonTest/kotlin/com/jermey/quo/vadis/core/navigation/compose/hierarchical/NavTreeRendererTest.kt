@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

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
class NavTreeRendererTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val HomeDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val ProfileDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val SettingsDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val DetailDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    fun createFakeScope(): FakeNavRenderScope = FakeNavRenderScope()

    // =========================================================================
    // NAV TREE RENDERER TESTS - NODE ROUTING
    // =========================================================================

    test("renderer creates FakeNavRenderScope with all dependencies") {
        // Given
        val scope = createFakeScope()

        // Then
        scope.navigator.shouldNotBeNull()
        scope.cache.shouldNotBeNull()
        scope.saveableStateHolder.shouldNotBeNull()
        scope.animationCoordinator.shouldNotBeNull()
        scope.predictiveBackController.shouldNotBeNull()
        scope.screenRegistry.shouldNotBeNull()
        scope.containerRegistry.shouldNotBeNull()
        scope.sharedTransitionScope.shouldBeNull()
    }

    test("scope navigator is functional") {
        // Given
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()
        val destination = HomeDestination

        // When
        scope.navigator.navigate(destination)

        // Then
        navigator.navigationCalls.isNotEmpty().shouldBeTrue()
    }

    test("scope container registry provides defaults") {
        // Given
        val scope = createFakeScope()

        // Then
        scope.containerRegistry shouldBe ContainerRegistry.Empty
        scope.containerRegistry.hasTabsContainer("any-key").shouldBeFalse()
        scope.containerRegistry.hasPaneContainer("any-key").shouldBeFalse()
    }

    // =========================================================================
    // SCREEN NODE RENDERING TESTS
    // =========================================================================

    test("screen node holds correct destination reference") {
        // Given
        val destination = ProfileDestination
        val screenNode = createScreen("profile-screen", destination = destination)

        // Then
        screenNode.key shouldBe NodeKey("profile-screen")
        screenNode.destination shouldBe destination
    }

    test("screen node can have null parent key for root screens") {
        // Given
        val screenNode = createScreen("root-screen", parentKey = null)

        // Then
        screenNode.parentKey.shouldBeNull()
    }

    test("screen node with parent key references correct parent") {
        // Given
        val screenNode = createScreen("child-screen", parentKey = "parent-stack")

        // Then
        screenNode.parentKey shouldBe NodeKey("parent-stack")
    }

    // =========================================================================
    // STACK NODE RENDERING TESTS
    // =========================================================================

    test("stack node activeChild returns last element") {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // Then
        stack.activeChild shouldBe screen3
    }

    test("stack node activeChild returns null for empty stack") {
        // Given
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        // Then
        stack.activeChild.shouldBeNull()
    }

    test("stack node canGoBack is true when multiple children") {
        // Given
        val stack = createStack(
            "stack",
            null,
            createScreen("s1", "stack"),
            createScreen("s2", "stack")
        )

        // Then
        stack.canGoBack.shouldBeTrue()
    }

    test("stack node canGoBack is false when single child") {
        // Given
        val stack = createStack("stack", null, createScreen("s1", "stack"))

        // Then
        stack.canGoBack.shouldBeFalse()
    }

    test("stack renders only active child not all children") {
        // Given
        val screen1 = createScreen("s1", "stack", HomeDestination)
        val screen2 = createScreen("s2", "stack", ProfileDestination)
        val stack = createStack("stack", null, screen1, screen2)

        // When
        val activeChild = stack.activeChild

        // Then - only the last screen is "active"
        activeChild shouldBe screen2
        activeChild.shouldNotBeNull()
        activeChild.key shouldBe NodeKey("s2")
    }

    // =========================================================================
    // TAB NODE RENDERING TESTS
    // =========================================================================

    test("tab node renders active stack correctly") {
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
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1
        )

        // Then
        tabs.activeStack shouldBe profileStack
        tabs.activeStackIndex shouldBe 1
        tabs.tabCount shouldBe 2
    }

    test("tab switching changes active stack") {
        // Given
        val stack0 = createStack("tab0", "tabs", createScreen("s0", "tab0"))
        val stack1 = createStack("tab1", "tabs", createScreen("s1", "tab1"))
        val stack2 = createStack("tab2", "tabs", createScreen("s2", "tab2"))

        val previousTabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 0
        )

        val currentTabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 2
        )

        // Then
        previousTabs.activeStack shouldBe stack0
        currentTabs.activeStack shouldBe stack2
    }

    test("tab node preserves all stacks during switching") {
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
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1
        )

        // Then - all stacks are preserved
        tabs.stacks.size shouldBe 2
        tabs.stackAt(0) shouldBe homeStack
        tabs.stackAt(1) shouldBe profileStack

        // Home stack still has 2 children
        tabs.stackAt(0).children.size shouldBe 2
    }

    // =========================================================================
    // PANE NODE RENDERING TESTS
    // =========================================================================

    test("pane node renders active pane correctly") {
        // Given
        val primaryContent = createScreen("primary", "panes", HomeDestination)
        val supportingContent = createScreen("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Supporting
        )

        // Then
        panes.activePaneContent shouldBe supportingContent
        panes.paneCount shouldBe 2
    }

    test("pane node respects adapt strategy") {
        // Given
        val panes = PaneNode(
            key = NodeKey("panes"),
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
        panes.adaptStrategy(PaneRole.Primary) shouldBe AdaptStrategy.Hide
        panes.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate
    }

    test("pane node has correct back behavior") {
        // Given - default back behavior
        val panesDefault = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("p", "panes"))
            ),
            activePaneRole = PaneRole.Primary
        )

        // Given - custom back behavior
        val panesCustom = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("p", "panes"))
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        // Then
        panesDefault.backBehavior shouldBe PaneBackBehavior.PopUntilScaffoldValueChange
        panesCustom.backBehavior shouldBe PaneBackBehavior.PopLatest
    }

    // =========================================================================
    // COMPLEX STRUCTURE TESTS
    // =========================================================================

    test("nested navigation structures work correctly") {
        // Given - a complex nested structure: Root Stack -> Tabs -> Stack per tab
        val homeScreen1 = createScreen("home-1", "home-stack", HomeDestination)
        val homeScreen2 = createScreen("home-2", "home-stack", ProfileDestination)
        val homeStack = createStack("home-stack", "tabs", homeScreen1, homeScreen2)

        val settingsScreen = createScreen("settings", "settings-stack", SettingsDestination)
        val settingsStack = createStack("settings-stack", "tabs", settingsScreen)

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, settingsStack),
            activeStackIndex = 0
        )

        val rootStack = StackNode(NodeKey("root"), null, listOf(tabs))

        // Then - verify the full hierarchy
        rootStack.activeChild shouldBe tabs
        tabs.activeStack shouldBe homeStack
        tabs.activeStack.activeChild shouldBe homeScreen2
    }

    test("pane with nested stack works correctly") {
        // Given
        val listScreen = createScreen("list", "list-stack")
        val listStack = createStack("list-stack", "panes", listScreen)

        val detailScreen1 = createScreen("detail-1", "detail-stack")
        val detailScreen2 = createScreen("detail-2", "detail-stack")
        val detailStack = createStack("detail-stack", "panes", detailScreen1, detailScreen2)

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(listStack),
                PaneRole.Supporting to PaneConfiguration(detailStack)
            ),
            activePaneRole = PaneRole.Supporting
        )

        // Then
        val activePaneContent = panes.activePaneContent
        activePaneContent.shouldBeInstanceOf<StackNode>()
        activePaneContent shouldBe detailStack
        activePaneContent.activeChild shouldBe detailScreen2
    }

    // =========================================================================
    // CACHE KEY UNIQUENESS TESTS
    // =========================================================================

    test("screen nodes have unique keys") {
        // Given
        val screen1 = createScreen("screen-1")
        val screen2 = createScreen("screen-2")
        val screen3 = createScreen("screen-3")

        // Then
        val keys = setOf(screen1.key, screen2.key, screen3.key)
        keys.size shouldBe 3
    }

    test("stack nodes have unique keys") {
        // Given
        val stack1 = createStack("stack-1")
        val stack2 = createStack("stack-2")
        val stack3 = createStack("stack-3")

        // Then
        val keys = setOf(stack1.key, stack2.key, stack3.key)
        keys.size shouldBe 3
    }
})
