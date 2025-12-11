package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for TreeNavigator.
 *
 * Tests cover:
 * - Initialization: setStartDestination, constructor with initial state
 * - Navigation operations: navigate, navigateBack, navigateAndReplace, navigateAndClearAll, navigateAndClearTo
 * - Tab navigation: switchTab, activeTabIndex
 * - Pane navigation: navigateToPane, switchPane, isPaneAvailable, navigateBackInPane, clearPane, paneContent
 * - State flows: state, currentDestination, previousDestination, canNavigateBack, transitionState
 * - Transition management: updateTransitionProgress, startPredictiveBack, updatePredictiveBack, 
 *   cancelPredictiveBack, commitPredictiveBack, completeTransition
 * - Deep link and graph management: handleDeepLink, registerGraph, getDeepLinkHandler
 * - Parent navigator support: activeChild, setActiveChild
 */
class TreeNavigatorTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    private object ListDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    private object FeedDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "feed"
    }

    private object TestTransition : NavigationTransition {
        override val enter: EnterTransition = EnterTransition.None
        override val exit: ExitTransition = ExitTransition.None
        override val popEnter: EnterTransition = EnterTransition.None
        override val popExit: ExitTransition = ExitTransition.None
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // INITIALIZATION TESTS
    // =========================================================================

    @Test
    fun `default constructor creates navigator with empty root stack`() {
        val navigator = TreeNavigator()

        val state = navigator.state.value
        assertTrue(state is StackNode)
        assertTrue((state as StackNode).isEmpty)
    }

    @Test
    fun `setStartDestination initializes state with single screen`() {
        val navigator = TreeNavigator()

        navigator.setStartDestination(HomeDestination)

        val state = navigator.state.value
        assertTrue(state is StackNode)
        assertEquals(1, (state as StackNode).children.size)
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    @Test
    fun `setStartDestination clears existing state`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.setStartDestination(SettingsDestination)

        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(SettingsDestination, navigator.currentDestination.value)
    }

    @Test
    fun `constructor with initial state uses provided state`() {
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination)
            )
        )

        val navigator = TreeNavigator(initialState = initialState)

        val state = navigator.state.value as StackNode
        assertEquals(2, state.children.size)
        assertEquals(ProfileDestination, navigator.currentDestination.value)
        assertEquals(HomeDestination, navigator.previousDestination.value)
    }

    @Test
    fun `transitionState starts as Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        val transitionState = navigator.transitionState.value

        assertEquals(LegacyTransitionState.Idle, transitionState)
    }

    @Test
    fun `canNavigateBack initially false with single screen`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertFalse(navigator.canNavigateBack.value)
    }

    @Test
    fun `previousDestination is null with single screen`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertNull(navigator.previousDestination.value)
    }

    // =========================================================================
    // NAVIGATE TESTS
    // =========================================================================

    @Test
    fun `navigate pushes to active stack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.navigate(ProfileDestination)

        val state = navigator.state.value as StackNode
        assertEquals(2, state.children.size)
        assertEquals(ProfileDestination, navigator.currentDestination.value)
    }

    @Test
    fun `navigate updates previousDestination`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.navigate(ProfileDestination)

        assertEquals(HomeDestination, navigator.previousDestination.value)
    }

    @Test
    fun `navigate updates canNavigateBack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        assertFalse(navigator.canNavigateBack.value)

        navigator.navigate(ProfileDestination)

        assertTrue(navigator.canNavigateBack.value)
    }

    @Test
    fun `navigate multiple screens builds stack correctly`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        val state = navigator.state.value as StackNode
        assertEquals(3, state.children.size)
        assertEquals(SettingsDestination, navigator.currentDestination.value)
        assertEquals(ProfileDestination, navigator.previousDestination.value)
    }

    @Test
    fun `navigate with transition updates transitionState`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.navigate(ProfileDestination, TestTransition)

        val transitionState = navigator.transitionState.value
        assertTrue(transitionState is LegacyTransitionState.InProgress)
        assertEquals(TestTransition, (transitionState as LegacyTransitionState.InProgress).transition)
        assertEquals(0f, transitionState.progress)
    }

    @Test
    fun `navigate to empty navigator creates new stack`() {
        val navigator = TreeNavigator()

        navigator.navigate(HomeDestination)

        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    // =========================================================================
    // NAVIGATE BACK TESTS
    // =========================================================================

    @Test
    fun `navigateBack pops from active stack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        val result = navigator.navigateBack()

        assertTrue(result)
        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    @Test
    fun `handleBackInternal returns false on single-item root stack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        // Single screen at root - should delegate to system (close app)
        // New back handling preserves root constraint: root stack always keeps 1 item
        val result = navigator.handleBackInternal()
        assertFalse(result, "Single item at root should delegate to system")
        
        // Current destination should still be there (not popped)
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    @Test
    fun `navigateBack updates previousDestination`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateBack()

        assertEquals(HomeDestination, navigator.previousDestination.value)
    }

    @Test
    fun `navigateBack updates canNavigateBack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        assertTrue(navigator.canNavigateBack.value)

        navigator.navigateBack()

        assertFalse(navigator.canNavigateBack.value)
    }

    @Test
    fun `handleBackInternal multiple times until single item at root`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        // Pop SettingsDestination
        assertTrue(navigator.handleBackInternal())
        assertEquals(ProfileDestination, navigator.currentDestination.value)

        // Pop ProfileDestination
        assertTrue(navigator.handleBackInternal())
        assertEquals(HomeDestination, navigator.currentDestination.value)

        // HomeDestination is the last item - should delegate to system (return false)
        // New back handling preserves root constraint: can't pop the last item at root
        assertFalse(navigator.handleBackInternal())
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    // =========================================================================
    // NAVIGATE AND REPLACE TESTS
    // =========================================================================

    @Test
    fun `navigateAndReplace replaces current screen`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.navigateAndReplace(SettingsDestination)

        val state = navigator.state.value as StackNode
        assertEquals(2, state.children.size)
        assertEquals(SettingsDestination, navigator.currentDestination.value)
        assertEquals(HomeDestination, navigator.previousDestination.value)
    }

    @Test
    fun `navigateAndReplace at root replaces root`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.navigateAndReplace(ProfileDestination)

        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(ProfileDestination, navigator.currentDestination.value)
    }

    @Test
    fun `navigateAndReplace with transition updates transitionState`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.navigateAndReplace(ProfileDestination, TestTransition)

        val transitionState = navigator.transitionState.value
        assertTrue(transitionState is LegacyTransitionState.InProgress)
    }

    // =========================================================================
    // NAVIGATE AND CLEAR ALL TESTS
    // =========================================================================

    @Test
    fun `navigateAndClearAll resets to single destination`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateAndClearAll(HomeDestination)

        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(HomeDestination, navigator.currentDestination.value)
        assertFalse(navigator.canNavigateBack.value)
    }

    @Test
    fun `navigateAndClearAll updates derived states`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.navigateAndClearAll(SettingsDestination)

        assertNull(navigator.previousDestination.value)
        assertFalse(navigator.canNavigateBack.value)
    }

    // =========================================================================
    // NAVIGATE AND CLEAR TO TESTS
    // =========================================================================

    @Test
    fun `navigateAndClearTo pops to specified route and pushes`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        // Note: The actual route matching depends on destination implementation
        // This tests the basic flow
        navigator.navigateAndClearTo(DetailDestination, null, false)

        val state = navigator.state.value as StackNode
        assertEquals(4, state.children.size) // No clearing happened since clearRoute is null
        assertEquals(DetailDestination, navigator.currentDestination.value)
    }

    // =========================================================================
    // TAB NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `switchTab changes active tab`() {
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("s1", "tab0", HomeDestination)
                        )),
                        StackNode("tab1", "tabs", listOf(
                            ScreenNode("s2", "tab1", ProfileDestination)
                        ))
                    ),
                    activeStackIndex = 0
                )
            )
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.switchTab(1)

        val tabs = (navigator.state.value as StackNode).children[0] as TabNode
        assertEquals(1, tabs.activeStackIndex)
        assertEquals(ProfileDestination, navigator.currentDestination.value)
    }

    @Test
    fun `switchTab preserves all tab stacks`() {
        // Wrap TabNode in root StackNode for proper structure
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination),
                    ScreenNode("s2", "tab0", ProfileDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s3", "tab1", SettingsDestination)
                ))
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.switchTab(1)

        val root = navigator.state.value as StackNode
        val tabs = root.children[0] as TabNode
        assertEquals(2, tabs.stacks[0].children.size) // tab0 unchanged
        assertEquals(1, tabs.stacks[1].children.size) // tab1 unchanged
    }

    @Test
    fun `switchTab throws for invalid index`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertFailsWith<IllegalArgumentException> {
            navigator.switchTab(5)
        }
    }

    @Test
    fun `switchTab throws for negative index`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertFailsWith<IllegalArgumentException> {
            navigator.switchTab(-1)
        }
    }

    @Test
    fun `activeTabIndex returns current tab index`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 1
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertEquals(1, navigator.activeTabIndex)
    }

    @Test
    fun `activeTabIndex returns null when no TabNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertNull(navigator.activeTabIndex)
    }

    @Test
    fun `navigate pushes to active tab stack`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s2", "tab1", ProfileDestination)
                ))
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.navigate(SettingsDestination)

        val root = navigator.state.value as StackNode
        val tabs = root.children[0] as TabNode
        assertEquals(2, tabs.stacks[0].children.size) // Pushed to active tab
        assertEquals(1, tabs.stacks[1].children.size) // Other tab unchanged
    }

    // =========================================================================
    // PANE NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `navigateToPane pushes to specified pane`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", listOf(
                        ScreenNode("list", "primary-stack", ListDestination)
                    ))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.navigateToPane(PaneRole.Supporting, DetailDestination)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
        assertEquals(DetailDestination, (supportingStack.activeChild as ScreenNode).destination)
    }

    @Test
    fun `navigateToPane with switchFocus changes active pane`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.navigateToPane(PaneRole.Supporting, DetailDestination, switchFocus = true)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        assertEquals(PaneRole.Supporting, panes.activePaneRole)
    }

    @Test
    fun `navigateToPane throws when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertFailsWith<IllegalStateException> {
            navigator.navigateToPane(PaneRole.Supporting, DetailDestination)
        }
    }

    @Test
    fun `switchPane changes active pane role`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("detail", "panes", DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.switchPane(PaneRole.Supporting)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        assertEquals(PaneRole.Supporting, panes.activePaneRole)
        assertEquals(DetailDestination, navigator.currentDestination.value)
    }

    @Test
    fun `switchPane throws when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertFailsWith<IllegalStateException> {
            navigator.switchPane(PaneRole.Supporting)
        }
    }

    @Test
    fun `isPaneAvailable returns true for configured role`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("detail", "panes", DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertTrue(navigator.isPaneAvailable(PaneRole.Primary))
        assertTrue(navigator.isPaneAvailable(PaneRole.Supporting))
    }

    @Test
    fun `isPaneAvailable returns false for unconfigured role`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertFalse(navigator.isPaneAvailable(PaneRole.Extra))
    }

    @Test
    fun `isPaneAvailable returns false when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertFalse(navigator.isPaneAvailable(PaneRole.Primary))
    }

    @Test
    fun `paneContent returns content for specified role`() {
        val primaryContent = ScreenNode("list", "panes", ListDestination)
        val supportingContent = ScreenNode("detail", "panes", DetailDestination)
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        // Note: The actual content nodes may have updated parentKeys after wrapping
        val primary = navigator.paneContent(PaneRole.Primary) as? ScreenNode
        val supporting = navigator.paneContent(PaneRole.Supporting) as? ScreenNode
        assertEquals(ListDestination, primary?.destination)
        assertEquals(DetailDestination, supporting?.destination)
    }

    @Test
    fun `paneContent returns null for unconfigured role`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        assertNull(navigator.paneContent(PaneRole.Extra))
    }

    @Test
    fun `paneContent returns null when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertNull(navigator.paneContent(PaneRole.Primary))
    }

    @Test
    fun `navigateBackInPane pops from specified pane`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", listOf(
                        ScreenNode("detail1", "supporting-stack", DetailDestination),
                        ScreenNode("detail2", "supporting-stack", SettingsDestination)
                    ))
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        assertTrue(result)
        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
    }

    @Test
    fun `navigateBackInPane returns false when pane stack is empty`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        assertFalse(result)
    }

    @Test
    fun `navigateBackInPane returns false when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        assertFalse(result)
    }

    @Test
    fun `clearPane resets pane stack to root`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("list", "panes", ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", listOf(
                        ScreenNode("detail1", "supporting-stack", DetailDestination),
                        ScreenNode("detail2", "supporting-stack", SettingsDestination),
                        ScreenNode("detail3", "supporting-stack", ProfileDestination)
                    ))
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.clearPane(PaneRole.Supporting)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
        assertEquals(DetailDestination, (supportingStack.activeChild as ScreenNode).destination)
    }

    @Test
    fun `clearPane throws when no PaneNode exists`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        assertFailsWith<IllegalStateException> {
            navigator.clearPane(PaneRole.Supporting)
        }
    }

    // =========================================================================
    // TRANSITION STATE MANAGEMENT TESTS
    // =========================================================================

    @Test
    fun `updateTransitionProgress updates InProgress state`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        navigator.updateTransitionProgress(0.5f)

        val transitionState = navigator.transitionState.value as LegacyTransitionState.InProgress
        assertEquals(0.5f, transitionState.progress)
    }

    @Test
    fun `updateTransitionProgress does nothing when Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        navigator.updateTransitionProgress(0.5f)

        assertEquals(LegacyTransitionState.Idle, navigator.transitionState.value)
    }

    @Test
    fun `completeTransition sets state to Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        navigator.completeTransition()

        assertEquals(LegacyTransitionState.Idle, navigator.transitionState.value)
    }

    @Test
    fun `startPredictiveBack initiates PredictiveBack state`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.startPredictiveBack()

        val transitionState = navigator.transitionState.value
        assertTrue(transitionState is LegacyTransitionState.PredictiveBack)
        assertEquals(0f, (transitionState as LegacyTransitionState.PredictiveBack).progress)
    }

    @Test
    fun `startPredictiveBack sets correct keys`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.startPredictiveBack()

        val transitionState = navigator.transitionState.value as LegacyTransitionState.PredictiveBack
        assertNotNull(transitionState.currentKey)
        assertNotNull(transitionState.previousKey)
    }

    @Test
    fun `updatePredictiveBack updates progress and touch coordinates`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(0.5f, 0.3f, 0.7f)

        val transitionState = navigator.transitionState.value as LegacyTransitionState.PredictiveBack
        assertEquals(0.5f, transitionState.progress)
        assertEquals(0.3f, transitionState.touchX)
        assertEquals(0.7f, transitionState.touchY)
    }

    @Test
    fun `updatePredictiveBack clamps progress to valid range`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(1.5f, 0f, 0f)

        val transitionState = navigator.transitionState.value as LegacyTransitionState.PredictiveBack
        assertEquals(1f, transitionState.progress)
    }

    @Test
    fun `updatePredictiveBack clamps touch coordinates to valid range`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(0.5f, -0.5f, 1.5f)

        val transitionState = navigator.transitionState.value as LegacyTransitionState.PredictiveBack
        assertEquals(0f, transitionState.touchX)
        assertEquals(1f, transitionState.touchY)
    }

    @Test
    fun `cancelPredictiveBack sets state to Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.cancelPredictiveBack()

        assertEquals(LegacyTransitionState.Idle, navigator.transitionState.value)
    }

    @Test
    fun `commitPredictiveBack completes navigation and sets Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.commitPredictiveBack()

        assertEquals(LegacyTransitionState.Idle, navigator.transitionState.value)
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    // =========================================================================
    // PARENT NAVIGATOR SUPPORT TESTS
    // =========================================================================

    @Test
    fun `activeChild is initially null`() {
        val navigator = TreeNavigator()

        assertNull(navigator.activeChild)
    }

    @Test
    fun `setActiveChild sets child handler`() {
        val navigator = TreeNavigator()
        val childHandler = object : BackPressHandler {
            override fun onBack(): Boolean = false
        }

        navigator.setActiveChild(childHandler)

        assertEquals(childHandler, navigator.activeChild)
    }

    @Test
    fun `setActiveChild with null clears child handler`() {
        val navigator = TreeNavigator()
        val childHandler = object : BackPressHandler {
            override fun onBack(): Boolean = false
        }
        navigator.setActiveChild(childHandler)

        navigator.setActiveChild(null)

        assertNull(navigator.activeChild)
    }

    // =========================================================================
    // GRAPH AND DEEP LINK TESTS
    // =========================================================================

    @Test
    fun `getDeepLinkHandler returns configured handler`() {
        val navigator = TreeNavigator()

        val handler = navigator.getDeepLinkHandler()

        assertNotNull(handler)
    }

    @Test
    fun `registerGraph stores graph`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        val testGraph = object : NavigationGraph {
            override val graphRoute: String = "test-graph"
            override val startDestination: Destination = HomeDestination
            override val destinations: List<DestinationConfig> = listOf(
                DestinationConfig(HomeDestination, content = { _, _ -> })
            )
        }

        navigator.registerGraph(testGraph)
        // Graph registration doesn't expose an API to verify, but it should not throw
    }

    // =========================================================================
    // UPDATE STATE TESTS
    // =========================================================================

    @Test
    fun `updateState directly sets new state`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        val newState = StackNode(
            key = "new-root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "new-root", ProfileDestination),
                ScreenNode("s2", "new-root", SettingsDestination)
            )
        )

        navigator.updateState(newState, null)

        val state = navigator.state.value as StackNode
        assertEquals("new-root", state.key)
        assertEquals(2, state.children.size)
        assertEquals(SettingsDestination, navigator.currentDestination.value)
    }

    @Test
    fun `updateState with transition sets transition state`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        val newState = StackNode(
            key = "new-root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "new-root", ProfileDestination)
            )
        )

        navigator.updateState(newState, TestTransition)

        val transitionState = navigator.transitionState.value
        assertTrue(transitionState is LegacyTransitionState.InProgress)
    }

    @Test
    fun `updateState without transition sets Idle`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        val newState = navigator.state.value

        navigator.updateState(newState, null)

        assertEquals(LegacyTransitionState.Idle, navigator.transitionState.value)
    }

    // =========================================================================
    // DERIVED STATE SYNCHRONIZATION TESTS
    // =========================================================================

    @Test
    fun `derived states update synchronously after navigation`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)

        // Before navigation
        assertEquals(HomeDestination, navigator.currentDestination.value)
        assertNull(navigator.previousDestination.value)
        assertFalse(navigator.canNavigateBack.value)

        // After navigation
        navigator.navigate(ProfileDestination)

        assertEquals(ProfileDestination, navigator.currentDestination.value)
        assertEquals(HomeDestination, navigator.previousDestination.value)
        assertTrue(navigator.canNavigateBack.value)
    }

    @Test
    fun `derived states update after back navigation`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateBack()

        assertEquals(ProfileDestination, navigator.currentDestination.value)
        assertEquals(HomeDestination, navigator.previousDestination.value)
        assertTrue(navigator.canNavigateBack.value)
    }

    @Test
    fun `derived states update after tab switch`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s2", "tab1", ProfileDestination)
                ))
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        navigator.switchTab(1)

        assertEquals(ProfileDestination, navigator.currentDestination.value)
    }

    // =========================================================================
    // COMPLEX NAVIGATION SCENARIO TESTS
    // =========================================================================

    @Test
    fun `complex navigation flow with tabs`() {
        // Start with tabs wrapped in root stack
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("home-tab", "tabs", listOf(
                    ScreenNode("home-screen", "home-tab", HomeDestination)
                )),
                StackNode("profile-tab", "tabs", listOf(
                    ScreenNode("profile-screen", "profile-tab", ProfileDestination)
                ))
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        // Navigate in home tab
        navigator.navigate(DetailDestination)
        assertEquals(DetailDestination, navigator.currentDestination.value)

        // Switch to profile tab
        navigator.switchTab(1)
        assertEquals(ProfileDestination, navigator.currentDestination.value)

        // Navigate in profile tab
        navigator.navigate(SettingsDestination)
        assertEquals(SettingsDestination, navigator.currentDestination.value)

        // Go back in profile tab
        navigator.navigateBack()
        assertEquals(ProfileDestination, navigator.currentDestination.value)

        // Switch back to home tab - should preserve navigation state
        navigator.switchTab(0)
        assertEquals(DetailDestination, navigator.currentDestination.value)

        // Go back in home tab
        navigator.navigateBack()
        assertEquals(HomeDestination, navigator.currentDestination.value)
    }

    @Test
    fun `complex navigation flow with panes`() {
        val paneNode = PaneNode(
            key = "panes",
            parentKey = "root",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("list-stack", "panes", listOf(
                        ScreenNode("list", "list-stack", ListDestination)
                    ))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("detail-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState)

        // Navigate to detail pane
        navigator.navigateToPane(PaneRole.Supporting, DetailDestination, switchFocus = true)
        assertEquals(DetailDestination, navigator.currentDestination.value)

        // Navigate deeper in detail pane
        navigator.navigateToPane(PaneRole.Supporting, SettingsDestination, switchFocus = false)

        // Verify detail pane has 2 screens
        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val detailStack = panes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(2, detailStack.children.size)

        // Go back in detail pane
        navigator.navigateBackInPane(PaneRole.Supporting)
        val rootAfterBack = navigator.state.value as StackNode
        val panesAfterBack = rootAfterBack.children[0] as PaneNode
        val detailStackAfterBack = panesAfterBack.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, detailStackAfterBack.children.size)
    }
}
