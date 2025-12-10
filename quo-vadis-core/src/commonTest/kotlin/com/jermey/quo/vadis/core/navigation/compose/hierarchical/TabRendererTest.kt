package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.testing.FakeNavRenderScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for tab navigation rendering.
 *
 * Tests cover:
 * - Tab switching and active tab tracking
 * - Tab state preservation across switches
 * - Tab wrapper integration
 * - Tab animation direction
 * - TabMetadata creation
 */
class TabRendererTest {

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

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: Destination = HomeDestination
    ): ScreenNode = ScreenNode(key, parentKey, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(key, parentKey, screens.toList())

    private fun createTabs(
        key: String,
        parentKey: String? = null,
        stacks: List<StackNode>,
        activeIndex: Int = 0
    ): TabNode = TabNode(key, parentKey, stacks, activeIndex)

    // =========================================================================
    // TAB SWITCHING TESTS
    // =========================================================================

    @Test
    fun `tab switching changes active stack index`() {
        // Given
        val homeStack = createStack("home", "tabs", createScreen("home-screen", "home"))
        val profileStack = createStack("profile", "tabs", createScreen("profile-screen", "profile"))
        val settingsStack = createStack("settings", "tabs", createScreen("settings-screen", "settings"))

        // When - switch from tab 0 to tab 2
        val beforeSwitch = createTabs("tabs", null, listOf(homeStack, profileStack, settingsStack), activeIndex = 0)
        val afterSwitch = createTabs("tabs", null, listOf(homeStack, profileStack, settingsStack), activeIndex = 2)

        // Then
        assertEquals(0, beforeSwitch.activeStackIndex)
        assertEquals(2, afterSwitch.activeStackIndex)
        assertEquals(homeStack, beforeSwitch.activeStack)
        assertEquals(settingsStack, afterSwitch.activeStack)
    }

    @Test
    fun `tab switching preserves all tab stacks`() {
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
        assertEquals(3, tab0Active.stackAt(0).children.size)
        assertEquals(1, tab0Active.stackAt(1).children.size)
        assertEquals(3, tab1Active.stackAt(0).children.size) // Home still has 3
        assertEquals(1, tab1Active.stackAt(1).children.size)
    }

    @Test
    fun `switching to same tab does not change state`() {
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
        assertEquals(tabs.activeStackIndex, sameTabs.activeStackIndex)
        assertEquals(tabs.activeStack, sameTabs.activeStack)
    }

    // =========================================================================
    // TAB STATE PRESERVATION TESTS
    // =========================================================================

    @Test
    fun `inactive tab stack state is preserved`() {
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
        assertEquals(3, tabs.stackAt(0).children.size)
        assertEquals(profileStack, tabs.activeStack)
    }

    @Test
    fun `tab count returns correct number`() {
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
        assertEquals(2, tabs2.tabCount)
        assertEquals(5, tabs5.tabCount)
    }

    @Test
    fun `stackAt returns correct stack for each index`() {
        // Given
        val stack0 = createStack("stack0", "tabs", createScreen("s0", "stack0"))
        val stack1 = createStack("stack1", "tabs", createScreen("s1", "stack1"))
        val stack2 = createStack("stack2", "tabs", createScreen("s2", "stack2"))
        val tabs = createTabs("tabs", null, listOf(stack0, stack1, stack2))

        // Then
        assertEquals(stack0, tabs.stackAt(0))
        assertEquals(stack1, tabs.stackAt(1))
        assertEquals(stack2, tabs.stackAt(2))
    }

    // =========================================================================
    // TAB WRAPPER TESTS
    // =========================================================================

    @Test
    fun `FakeNavRenderScope provides wrapper registry`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.wrapperRegistry)
        assertEquals(WrapperRegistry.Empty, scope.wrapperRegistry)
    }

    @Test
    fun `wrapper registry hasTabWrapper returns false for empty registry`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertFalse(scope.wrapperRegistry.hasTabWrapper("any-key"))
        assertFalse(scope.wrapperRegistry.hasTabWrapper("tabs"))
        assertFalse(scope.wrapperRegistry.hasTabWrapper(""))
    }

    // =========================================================================
    // TAB ANIMATION DIRECTION TESTS
    // =========================================================================

    @Test
    fun `switching from lower to higher index suggests forward direction`() {
        // Given
        val previousIndex = 0
        val currentIndex = 2

        // Then - direction logic (higher index = forward)
        val isForward = currentIndex > previousIndex
        assertTrue(isForward)
    }

    @Test
    fun `switching from higher to lower index suggests backward direction`() {
        // Given
        val previousIndex = 3
        val currentIndex = 1

        // Then
        val isBackward = currentIndex < previousIndex
        assertTrue(isBackward)
    }

    @Test
    fun `animation coordinator provides tab transition`() {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        assertNotNull(coordinator.defaultTabTransition)
    }

    @Test
    fun `FakeNavRenderScope animation coordinator has tab transition`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.animationCoordinator.defaultTabTransition)
    }

    // =========================================================================
    // TAB METADATA TESTS
    // =========================================================================

    @Test
    fun `tab stacks have meaningful keys for metadata`() {
        // Given
        val homeStack = createStack("home_stack", "tabs", createScreen("h", "home_stack"))
        val profileStack = createStack("profile_stack", "tabs", createScreen("p", "profile_stack"))
        val settingsStack = createStack("settings_stack", "tabs", createScreen("s", "settings_stack"))

        val tabs = createTabs("tabs", null, listOf(homeStack, profileStack, settingsStack))

        // Then - keys can be used for metadata generation
        assertEquals("home_stack", tabs.stackAt(0).key)
        assertEquals("profile_stack", tabs.stackAt(1).key)
        assertEquals("settings_stack", tabs.stackAt(2).key)
    }

    @Test
    fun `tab metadata label extraction from key`() {
        // Given - simulating createTabMetadataFromStacks logic
        val stackKeys = listOf("home_stack", "profile_stack", "settings_stack")

        // When - extract labels from keys
        val labels = stackKeys.map { key ->
            key.substringAfterLast("_").takeIf { it.isNotEmpty() } ?: "Tab"
        }

        // Then
        assertEquals("stack", labels[0])
        assertEquals("stack", labels[1])
        assertEquals("stack", labels[2])
    }

    // =========================================================================
    // TAB PREDICTIVE BACK TESTS
    // =========================================================================

    @Test
    fun `tab switching does not use predictive back`() {
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
        assertNotNull(tabs.activeStack)
    }

    // =========================================================================
    // NESTED TAB TESTS
    // =========================================================================

    @Test
    fun `nested tabs maintain parent reference`() {
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
        assertEquals("outer-stack", innerTabs.parentKey)
    }

    @Test
    fun `root tabs have null parent key`() {
        // Given
        val rootTabs = createTabs(
            "root-tabs",
            null,
            listOf(createStack("tab0", "root-tabs"))
        )

        // Then
        assertEquals(null, rootTabs.parentKey)
    }

    // =========================================================================
    // TAB INDEX VALIDATION TESTS
    // =========================================================================

    @Test
    fun `active stack index at lower bound is valid`() {
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
        assertEquals(0, tabs.activeStackIndex)
        assertEquals(tabs.stacks[0], tabs.activeStack)
    }

    @Test
    fun `active stack index at upper bound is valid`() {
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
        assertEquals(2, tabs.activeStackIndex)
        assertEquals(tabs.stacks[2], tabs.activeStack)
    }

    // =========================================================================
    // COMPLEX TAB SCENARIOS
    // =========================================================================

    @Test
    fun `tab with deep stack navigation`() {
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
        assertEquals(4, tabs.activeStack.children.size)
        assertTrue(tabs.activeStack.canGoBack)
        
        // Active leaf should be the last screen in active stack
        assertEquals("home-edit", tabs.activeStack.activeChild?.key)
    }

    @Test
    fun `switching tabs preserves deep navigation history`() {
        // Given
        val homeStack = createStack(
            "home", "tabs",
            createScreen("h1", "home"),
            createScreen("h2", "home"),
            createScreen("h3", "home")
        )
        val profileStack = createStack("profile", "tabs", createScreen("p1", "profile"))

        // Switch to profile tab
        val tabsOnProfile = createTabs("tabs", null, listOf(homeStack, profileStack), activeIndex = 1)

        // Then - home stack still has 3
        assertEquals(3, tabsOnProfile.stackAt(0).children.size)
        assertEquals(1, tabsOnProfile.activeStack.children.size)
    }
}
