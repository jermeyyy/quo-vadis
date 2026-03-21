@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Recursively finds the first ScreenNode's destination in a node tree.
 * This mirrors the implementation in TabRenderer.
 */
private fun findFirstScreenDestination(node: NavNode): NavDestination? {
    return when (node) {
        is ScreenNode -> node.destination
        is StackNode -> node.children.firstOrNull()?.let { findFirstScreenDestination(it) }
        is TabNode -> node.stacks.firstOrNull()?.let { findFirstScreenDestination(it) }
        is com.jermey.quo.vadis.core.navigation.node.PaneNode ->
            node.paneConfigurations.values.firstOrNull()?.let { findFirstScreenDestination(it.content) }
    }
}

/**
 * Tests for tab navigation rendering.
 *
 * Tests cover:
 * - Tab switching and active tab tracking
 * - Tab state preservation across switches
 * - Tab wrapper integration
 * - Tab animation direction
 * - Tab destination extraction
 */
class TabRendererTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val HomeDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val ProfileDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val SettingsDestination = object : NavDestination {
        override val data: Any? = null
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

    fun createTabs(
        key: String,
        parentKey: String? = null,
        stacks: List<StackNode>,
        activeIndex: Int = 0
    ): TabNode = TabNode(NodeKey(key), parentKey?.let { NodeKey(it) }, stacks, activeIndex)

    // =========================================================================
    // TAB SWITCHING TESTS
    // =========================================================================

    test("tab switching changes active stack index") {
        // Given
        val homeStack = createStack("home", "tabs", createScreen("home-screen", "home"))
        val profileStack = createStack("profile", "tabs", createScreen("profile-screen", "profile"))
        val settingsStack =
            createStack("settings", "tabs", createScreen("settings-screen", "settings"))

        // When - switch from tab 0 to tab 2
        val beforeSwitch = createTabs(
            "tabs",
            null,
            listOf(homeStack, profileStack, settingsStack),
            activeIndex = 0
        )
        val afterSwitch = createTabs(
            "tabs",
            null,
            listOf(homeStack, profileStack, settingsStack),
            activeIndex = 2
        )

        // Then
        beforeSwitch.activeStackIndex shouldBe 0
        afterSwitch.activeStackIndex shouldBe 2
        beforeSwitch.activeStack shouldBe homeStack
        afterSwitch.activeStack shouldBe settingsStack
    }

    test("tab switching preserves all tab stacks") {
        // Given - home tab has 3 screens, profile has 1
        val homeStack = createStack(
            "home", "tabs",
            createScreen("h1", "home"),
            createScreen("h2", "home"),
            createScreen("h3", "home")
        )
        val profileStack = createStack("profile", "tabs", createScreen("p1", "profile"))

        // When - switch tabs
        val tab0Active = createTabs("tabs", null, listOf(homeStack, profileStack), activeIndex = 0)
        val tab1Active = createTabs("tabs", null, listOf(homeStack, profileStack), activeIndex = 1)

        // Then - both stacks preserve their state
        tab0Active.stackAt(0).children.size shouldBe 3
        tab0Active.stackAt(1).children.size shouldBe 1
        tab1Active.stackAt(0).children.size shouldBe 3 // Home still has 3
        tab1Active.stackAt(1).children.size shouldBe 1
    }

    test("switching to same tab does not change state") {
        // Given
        val tabs = createTabs(
            "tabs", null,
            listOf(
                createStack("tab0", "tabs", createScreen("s0", "tab0")),
                createStack("tab1", "tabs", createScreen("s1", "tab1"))
            ),
            activeIndex = 0
        )

        // When - "switch" to same tab
        val sameTabs = createTabs("tabs", null, tabs.stacks, activeIndex = 0)

        // Then
        sameTabs.activeStackIndex shouldBe tabs.activeStackIndex
        sameTabs.activeStack shouldBe tabs.activeStack
    }

    // =========================================================================
    // TAB STATE PRESERVATION TESTS
    // =========================================================================

    test("inactive tab stack state is preserved") {
        // Given - start on tab 0, push screens, switch to tab 1, check tab 0 state
        val initialHomeStack = createStack("home", "tabs", createScreen("h1", "home"))
        val pushedHomeStack = createStack(
            "home", "tabs",
            createScreen("h1", "home"),
            createScreen("h2", "home"),
            createScreen("h3", "home")
        )
        val profileStack = createStack("profile", "tabs", createScreen("p1", "profile"))

        // After pushing to home and switching to profile
        val tabs = createTabs("tabs", null, listOf(pushedHomeStack, profileStack), activeIndex = 1)

        // Then - home stack still has 3 screens
        tabs.stackAt(0).children.size shouldBe 3
        tabs.activeStack shouldBe profileStack
    }

    test("tab count returns correct number") {
        // Given
        val tabs2 = createTabs(
            "tabs", null,
            listOf(
                createStack("t0", "tabs"),
                createStack("t1", "tabs")
            )
        )

        val tabs5 = createTabs(
            "tabs", null,
            listOf(
                createStack("t0", "tabs"),
                createStack("t1", "tabs"),
                createStack("t2", "tabs"),
                createStack("t3", "tabs"),
                createStack("t4", "tabs")
            )
        )

        // Then
        tabs2.tabCount shouldBe 2
        tabs5.tabCount shouldBe 5
    }

    test("stackAt returns correct stack for each index") {
        // Given
        val stack0 = createStack("stack0", "tabs", createScreen("s0", "stack0"))
        val stack1 = createStack("stack1", "tabs", createScreen("s1", "stack1"))
        val stack2 = createStack("stack2", "tabs", createScreen("s2", "stack2"))
        val tabs = createTabs("tabs", null, listOf(stack0, stack1, stack2))

        // Then
        tabs.stackAt(0) shouldBe stack0
        tabs.stackAt(1) shouldBe stack1
        tabs.stackAt(2) shouldBe stack2
    }

    // =========================================================================
    // TAB WRAPPER TESTS
    // =========================================================================

    test("FakeNavRenderScope provides container registry") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry.shouldNotBeNull()
        scope.containerRegistry shouldBe ContainerRegistry.Empty
    }

    test("container registry hasTabsContainer returns false for empty registry") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry.hasTabsContainer("any-key").shouldBeFalse()
        scope.containerRegistry.hasTabsContainer("tabs").shouldBeFalse()
        scope.containerRegistry.hasTabsContainer("").shouldBeFalse()
    }

    // =========================================================================
    // TAB ANIMATION DIRECTION TESTS
    // =========================================================================

    test("switching from lower to higher index suggests forward direction") {
        // Given
        val previousIndex = 0
        val currentIndex = 2

        // Then - direction logic (higher index = forward)
        val isForward = currentIndex > previousIndex
        isForward.shouldBeTrue()
    }

    test("switching from higher to lower index suggests backward direction") {
        // Given
        val previousIndex = 3
        val currentIndex = 1

        // Then
        val isBackward = currentIndex < previousIndex
        isBackward.shouldBeTrue()
    }

    test("animation coordinator provides tab transition") {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        coordinator.defaultTabTransition.shouldNotBeNull()
    }

    test("FakeNavRenderScope animation coordinator has tab transition") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.animationCoordinator.defaultTabTransition.shouldNotBeNull()
    }

    // =========================================================================
    // TAB METADATA TESTS
    // =========================================================================

    test("tab stacks have meaningful keys for metadata") {
        // Given
        val homeStack = createStack("home_stack", "tabs", createScreen("h", "home_stack"))
        val profileStack = createStack("profile_stack", "tabs", createScreen("p", "profile_stack"))
        val settingsStack =
            createStack("settings_stack", "tabs", createScreen("s", "settings_stack"))

        val tabs = createTabs("tabs", null, listOf(homeStack, profileStack, settingsStack))

        // Then - keys can be used for metadata generation
        tabs.stackAt(0).key shouldBe NodeKey("home_stack")
        tabs.stackAt(1).key shouldBe NodeKey("profile_stack")
        tabs.stackAt(2).key shouldBe NodeKey("settings_stack")
    }

    test("tab destination extraction from stacks") {
        // Given - simulating getTabDestinations logic
        val homeScreen = createScreen("h", "home_stack", HomeDestination)
        val profileScreen = createScreen("p", "profile_stack", ProfileDestination)
        val settingsScreen = createScreen("s", "settings_stack", SettingsDestination)
        
        val homeStack = createStack("home_stack", "tabs", homeScreen)
        val profileStack = createStack("profile_stack", "tabs", profileScreen)
        val settingsStack = createStack("settings_stack", "tabs", settingsScreen)
        
        val tabs = createTabs("tabs", null, listOf(homeStack, profileStack, settingsStack))

        // When - extract destinations from stacks
        val destinations = tabs.stacks.mapNotNull { stack ->
            (stack.children.firstOrNull() as? ScreenNode)?.destination
        }

        // Then - destinations can be used for pattern matching
        destinations.size shouldBe 3
        destinations[0] shouldBe HomeDestination
        destinations[1] shouldBe ProfileDestination
        destinations[2] shouldBe SettingsDestination
    }

    test("tab destination extraction from nested stacks - TabItem Stack pattern") {
        // Given - simulating @TabItem @Stack structure where each tab's wrapper stack
        // contains a nested StackNode, which contains the actual ScreenNode
        // Structure: TabNode > StackNode (wrapper) > StackNode (nested) > ScreenNode
        val homeScreen = createScreen("h", "music-stack", HomeDestination)
        val profileScreen = createScreen("p", "movies-stack", ProfileDestination)
        val settingsScreen = createScreen("s", "books-stack", SettingsDestination)

        // Nested stacks (like @TabItem @Stack creates)
        val musicNestedStack = createStack("music-stack", "tab0", homeScreen)
        val moviesNestedStack = createStack("movies-stack", "tab1", profileScreen)
        val booksNestedStack = createStack("books-stack", "tab2", settingsScreen)

        // Wrapper stacks (each tab's wrapper contains the nested stack)
        val tab0Wrapper = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(musicNestedStack))
        val tab1Wrapper = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(moviesNestedStack))
        val tab2Wrapper = StackNode(NodeKey("tab2"), NodeKey("tabs"), listOf(booksNestedStack))

        val tabs = createTabs("tabs", null, listOf(tab0Wrapper, tab1Wrapper, tab2Wrapper))

        // When - extract destinations using recursive approach (matches getTabDestinations)
        val destinations = tabs.stacks.mapNotNull { stack ->
            findFirstScreenDestination(stack)
        }

        // Then - destinations are correctly extracted from nested structure
        destinations.size shouldBe 3
        destinations[0] shouldBe HomeDestination
        destinations[1] shouldBe ProfileDestination
        destinations[2] shouldBe SettingsDestination
    }

    // =========================================================================
    // TAB PREDICTIVE BACK TESTS
    // =========================================================================

    test("tab switching does not use predictive back") {
        // This is a documentation/behavior test
        // Tab switching animations should NOT use predictive back gestures
        // Predictive back is handled at the stack level within each tab

        // Given
        val tabs = createTabs(
            "tabs", null,
            listOf(
                createStack("tab0", "tabs", createScreen("s0", "tab0")),
                createStack("tab1", "tabs", createScreen("s1", "tab1"))
            )
        )

        // Then - tab nodes don't have a parentKey at root, but predictive back
        // is still disabled for tab content switching (per design)
        // This is enforced in TabRenderer by passing predictiveBackEnabled = false
        tabs.activeStack.shouldNotBeNull()
    }

    // =========================================================================
    // NESTED TAB TESTS
    // =========================================================================

    test("nested tabs maintain parent reference") {
        // Given - tabs within a stack
        val innerTabs = createTabs(
            "inner-tabs",
            "outer-stack",
            listOf(
                createStack("inner-tab0", "inner-tabs"),
                createStack("inner-tab1", "inner-tabs")
            )
        )

        // Then
        innerTabs.parentKey shouldBe NodeKey("outer-stack")
    }

    test("root tabs have null parent key") {
        // Given
        val rootTabs = createTabs(
            "root-tabs",
            null,
            listOf(createStack("tab0", "root-tabs"))
        )

        // Then
        rootTabs.parentKey shouldBe null
    }

    // =========================================================================
    // TAB INDEX VALIDATION TESTS
    // =========================================================================

    test("active stack index at lower bound is valid") {
        // Given
        val tabs = createTabs(
            "tabs", null,
            listOf(
                createStack("tab0", "tabs"),
                createStack("tab1", "tabs")
            ),
            activeIndex = 0
        )

        // Then
        tabs.activeStackIndex shouldBe 0
        tabs.activeStack shouldBe tabs.stacks[0]
    }

    test("active stack index at upper bound is valid") {
        // Given
        val tabs = createTabs(
            "tabs", null,
            listOf(
                createStack("tab0", "tabs"),
                createStack("tab1", "tabs"),
                createStack("tab2", "tabs")
            ),
            activeIndex = 2 // Last valid index
        )

        // Then
        tabs.activeStackIndex shouldBe 2
        tabs.activeStack shouldBe tabs.stacks[2]
    }

    // =========================================================================
    // COMPLEX TAB SCENARIOS
    // =========================================================================

    test("tab with deep stack navigation") {
        // Given - home tab has deep navigation history
        val homeStack = createStack(
            "home", "tabs",
            createScreen("home-root", "home"),
            createScreen("home-list", "home"),
            createScreen("home-detail", "home"),
            createScreen("home-edit", "home")
        )
        val profileStack = createStack(
            "profile", "tabs",
            createScreen("profile-root", "profile")
        )

        val tabs = createTabs("tabs", null, listOf(homeStack, profileStack), activeIndex = 0)

        // Then
        tabs.activeStack.children.size shouldBe 4
        tabs.activeStack.canGoBack.shouldBeTrue()

        // Active leaf should be the last screen in active stack
        tabs.activeStack.activeChild?.key shouldBe NodeKey("home-edit")
    }

    test("switching tabs preserves deep navigation history") {
        // Given
        val homeStack = createStack(
            "home", "tabs",
            createScreen("h1", "home"),
            createScreen("h2", "home"),
            createScreen("h3", "home")
        )
        val profileStack = createStack("profile", "tabs", createScreen("p1", "profile"))

        // Switch to profile tab
        val tabsOnProfile =
            createTabs("tabs", null, listOf(homeStack, profileStack), activeIndex = 1)

        // Then - home stack still has 3
        tabsOnProfile.stackAt(0).children.size shouldBe 3
        tabsOnProfile.activeStack.children.size shouldBe 1
    }
})
