package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.TransitionState
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.testing.withDestination
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.navigator.NavigationErrorHandler
import kotlinx.coroutines.Dispatchers
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

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
 * - Deep link and graph management: handleDeepLink, registerGraph, getDeepLinkRegistry
 * - Parent navigator support: activeChild, setActiveChild
 */
@OptIn(InternalQuoVadisApi::class)
class TreeNavigatorTest : FunSpec() {

    object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    object FeedDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "feed"
    }

    object TestTransition : NavigationTransition {
        override val enter: EnterTransition = EnterTransition.None
        override val exit: ExitTransition = ExitTransition.None
        override val popEnter: EnterTransition = EnterTransition.None
        override val popExit: ExitTransition = ExitTransition.None
    }

    init {

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    beforeTest {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // INITIALIZATION TESTS
    // =========================================================================

    test("default constructor creates navigator with empty root stack") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value
        (state is StackNode).shouldBeTrue()
        (state as StackNode).isEmpty.shouldBeTrue()
    }

    test("constructor with initial state uses provided state") {
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 2
        navigator.currentDestination.value shouldBe ProfileDestination
        navigator.previousDestination.value shouldBe HomeDestination
    }

    test("transitionState starts as Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        val transitionState = navigator.transitionState.value

        transitionState shouldBe TransitionState.Idle
    }

    test("canNavigateBack initially false with single screen") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.canNavigateBack.value.shouldBeFalse()
    }

    test("previousDestination is null with single screen") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.previousDestination.value.shouldBeNull()
    }

    // =========================================================================
    // NAVIGATE TESTS
    // =========================================================================

    test("navigate pushes to active stack") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.navigate(ProfileDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 2
        navigator.currentDestination.value shouldBe ProfileDestination
    }

    test("navigate updates previousDestination") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.navigate(ProfileDestination)

        navigator.previousDestination.value shouldBe HomeDestination
    }

    test("navigate updates canNavigateBack") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.canNavigateBack.value.shouldBeFalse()

        navigator.navigate(ProfileDestination)

        navigator.canNavigateBack.value.shouldBeTrue()
    }

    test("navigate multiple screens builds stack correctly") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 3
        navigator.currentDestination.value shouldBe SettingsDestination
        navigator.previousDestination.value shouldBe ProfileDestination
    }

    test("navigate with transition updates transitionState") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.navigate(ProfileDestination, TestTransition)

        val transitionState = navigator.transitionState.value
        (transitionState is TransitionState.InProgress).shouldBeTrue()
        (transitionState as TransitionState.InProgress).transition shouldBe TestTransition
        transitionState.progress shouldBe 0f
    }

    test("navigate to empty navigator creates new stack") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        navigator.navigate(HomeDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe HomeDestination
    }

    // =========================================================================
    // NAVIGATE BACK TESTS
    // =========================================================================

    test("navigateBack pops from active stack") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        val result = navigator.navigateBack()

        result.shouldBeTrue()
        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe HomeDestination
    }

    test("handleBackInternal returns false on single-item root stack") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        // Single screen at root - should delegate to system (close app)
        // New back handling preserves root constraint: root stack always keeps 1 item
        val result = navigator.onBack()
        result.shouldBeFalse()

        // Current destination should still be there (not popped)
        navigator.currentDestination.value shouldBe HomeDestination
    }

    test("navigateBack updates previousDestination") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateBack()

        navigator.previousDestination.value shouldBe HomeDestination
    }

    test("navigateBack updates canNavigateBack") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.canNavigateBack.value.shouldBeTrue()

        navigator.navigateBack()

        navigator.canNavigateBack.value.shouldBeFalse()
    }

    test("handleBackInternal multiple times until single item at root") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        // Pop SettingsDestination
        navigator.onBack().shouldBeTrue()
        navigator.currentDestination.value shouldBe ProfileDestination

        // Pop ProfileDestination
        navigator.onBack().shouldBeTrue()
        navigator.currentDestination.value shouldBe HomeDestination

        // HomeDestination is the last item - should delegate to system (return false)
        // New back handling preserves root constraint: can't pop the last item at root
        navigator.onBack().shouldBeFalse()
        navigator.currentDestination.value shouldBe HomeDestination
    }

    // =========================================================================
    // NAVIGATE AND REPLACE TESTS
    // =========================================================================

    test("navigateAndReplace replaces current screen") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.navigateAndReplace(SettingsDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 2
        navigator.currentDestination.value shouldBe SettingsDestination
        navigator.previousDestination.value shouldBe HomeDestination
    }

    test("navigateAndReplace at root replaces root") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.navigateAndReplace(ProfileDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe ProfileDestination
    }

    test("navigateAndReplace with transition updates transitionState") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.navigateAndReplace(ProfileDestination, TestTransition)

        val transitionState = navigator.transitionState.value
        (transitionState is TransitionState.InProgress).shouldBeTrue()
    }

    // =========================================================================
    // NAVIGATE AND CLEAR ALL TESTS
    // =========================================================================

    test("navigateAndClearAll resets to single destination") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateAndClearAll(HomeDestination)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe HomeDestination
        navigator.canNavigateBack.value.shouldBeFalse()
    }

    test("navigateAndClearAll updates derived states") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.navigateAndClearAll(SettingsDestination)

        navigator.previousDestination.value.shouldBeNull()
        navigator.canNavigateBack.value.shouldBeFalse()
    }

    // =========================================================================
    // NAVIGATE AND CLEAR TO TESTS
    // =========================================================================

    test("navigateAndClearTo pops to specified route and pushes") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        // Note: The actual route matching depends on destination implementation
        // This tests the basic flow
        navigator.navigateAndClearTo(DetailDestination, null, false)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 4 // No clearing happened since clearRoute is null
        navigator.currentDestination.value shouldBe DetailDestination
    }

    // =========================================================================
    // TAB NAVIGATION TESTS
    // =========================================================================

    test("switchActiveTab changes active tab") {
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                            )
                        ),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val newState = TreeMutator.switchActiveTab(navigator.state.value, 1)
        navigator.updateState(newState)

        val tabs = (navigator.state.value as StackNode).children[0] as TabNode
        tabs.activeStackIndex shouldBe 1
        navigator.currentDestination.value shouldBe ProfileDestination
    }

    test("switchActiveTab preserves all tab stacks") {
        // Wrap TabNode in root StackNode for proper structure
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val newState = TreeMutator.switchActiveTab(navigator.state.value, 1)
        navigator.updateState(newState)

        val root = navigator.state.value as StackNode
        val tabs = root.children[0] as TabNode
        tabs.stacks[0].children.size shouldBe 2 // tab0 unchanged
        tabs.stacks[1].children.size shouldBe 1 // tab1 unchanged
    }

    test("switchActiveTab throws for invalid index") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        shouldThrow<IllegalArgumentException> {
            TreeMutator.switchActiveTab(navigator.state.value, 5)
        }
    }

    test("switchActiveTab throws for negative index") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        shouldThrow<IllegalArgumentException> {
            TreeMutator.switchActiveTab(navigator.state.value, -1)
        }
    }

    test("activeTabIndex returns current tab index") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 1
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.activeTabIndex shouldBe 1
    }

    test("activeTabIndex returns null when no TabNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.activeTabIndex.shouldBeNull()
    }

    test("navigate pushes to active tab stack") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.navigate(SettingsDestination)

        val root = navigator.state.value as StackNode
        val tabs = root.children[0] as TabNode
        tabs.stacks[0].children.size shouldBe 2 // Pushed to active tab
        tabs.stacks[1].children.size shouldBe 1 // Other tab unchanged
    }

    // =========================================================================
    // PANE NAVIGATION TESTS
    // =========================================================================

    test("isPaneAvailable returns true for configured role") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("detail"), NodeKey("panes"), DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.isPaneAvailable(PaneRole.Primary).shouldBeTrue()
        navigator.isPaneAvailable(PaneRole.Supporting).shouldBeTrue()
    }

    test("isPaneAvailable returns false for unconfigured role") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.isPaneAvailable(PaneRole.Extra).shouldBeFalse()
    }

    test("isPaneAvailable returns false when no PaneNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.isPaneAvailable(PaneRole.Primary).shouldBeFalse()
    }

    test("paneContent returns content for specified role") {
        val primaryContent = ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
        val supportingContent = ScreenNode(NodeKey("detail"), NodeKey("panes"), DetailDestination)
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        // Note: The actual content nodes may have updated parentKeys after wrapping
        val primary = navigator.paneContent(PaneRole.Primary) as? ScreenNode
        val supporting = navigator.paneContent(PaneRole.Supporting) as? ScreenNode
        primary?.destination shouldBe ListDestination
        supporting?.destination shouldBe DetailDestination
    }

    test("paneContent returns null for unconfigured role") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.paneContent(PaneRole.Extra).shouldBeNull()
    }

    test("paneContent returns null when no PaneNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.paneContent(PaneRole.Primary).shouldBeNull()
    }

    test("navigateBackInPane pops from specified pane") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail1"), NodeKey("supporting-stack"), DetailDestination),
                            ScreenNode(NodeKey("detail2"), NodeKey("supporting-stack"), SettingsDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        result.shouldBeTrue()
        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
    }

    test("navigateBackInPane returns false when pane stack is empty") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        result.shouldBeFalse()
    }

    test("navigateBackInPane returns false when no PaneNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        result.shouldBeFalse()
    }

    test("clearPane resets pane stack to root") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail1"), NodeKey("supporting-stack"), DetailDestination),
                            ScreenNode(NodeKey("detail2"), NodeKey("supporting-stack"), SettingsDestination),
                            ScreenNode(NodeKey("detail3"), NodeKey("supporting-stack"), ProfileDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.clearPane(PaneRole.Supporting)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
        (supportingStack.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("clearPane throws when no PaneNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.errorHandler = NavigationErrorHandler.ThrowOnError

        shouldThrow<IllegalStateException> {
            navigator.clearPane(PaneRole.Supporting)
        }
    }

    // =========================================================================
    // TRANSITION STATE MANAGEMENT TESTS
    // =========================================================================

    test("updateTransitionProgress updates InProgress state") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        navigator.updateTransitionProgress(0.5f)

        val transitionState = navigator.transitionState.value as TransitionState.InProgress
        transitionState.progress shouldBe 0.5f
    }

    test("updateTransitionProgress does nothing when Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        navigator.updateTransitionProgress(0.5f)

        navigator.transitionState.value shouldBe TransitionState.Idle
    }

    test("completeTransition sets state to Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        navigator.completeTransition()

        navigator.transitionState.value shouldBe TransitionState.Idle
    }

    test("startPredictiveBack initiates PredictiveBack state") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.startPredictiveBack()

        val transitionState = navigator.transitionState.value
        (transitionState is TransitionState.PredictiveBack).shouldBeTrue()
        (transitionState as TransitionState.PredictiveBack).progress shouldBe 0f
    }

    test("startPredictiveBack sets correct keys") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)

        navigator.startPredictiveBack()

        val transitionState = navigator.transitionState.value as TransitionState.PredictiveBack
        transitionState.currentKey.shouldNotBeNull()
        transitionState.previousKey.shouldNotBeNull()
    }

    test("updatePredictiveBack updates progress and touch coordinates") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(0.5f, 0.3f, 0.7f)

        val transitionState = navigator.transitionState.value as TransitionState.PredictiveBack
        transitionState.progress shouldBe 0.5f
        transitionState.touchX shouldBe 0.3f
        transitionState.touchY shouldBe 0.7f
    }

    test("updatePredictiveBack clamps progress to valid range") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(1.5f, 0f, 0f)

        val transitionState = navigator.transitionState.value as TransitionState.PredictiveBack
        transitionState.progress shouldBe 1f
    }

    test("updatePredictiveBack clamps touch coordinates to valid range") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.updatePredictiveBack(0.5f, -0.5f, 1.5f)

        val transitionState = navigator.transitionState.value as TransitionState.PredictiveBack
        transitionState.touchX shouldBe 0f
        transitionState.touchY shouldBe 1f
    }

    test("cancelPredictiveBack sets state to Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.cancelPredictiveBack()

        navigator.transitionState.value shouldBe TransitionState.Idle
    }

    test("commitPredictiveBack completes navigation and sets Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.startPredictiveBack()

        navigator.commitPredictiveBack()

        navigator.transitionState.value shouldBe TransitionState.Idle
        navigator.currentDestination.value shouldBe HomeDestination
    }

    // =========================================================================
    // GRAPH AND DEEP LINK TESTS
    // =========================================================================

    test("getDeepLinkRegistry returns registry for pattern registration") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        val registry = navigator.getDeepLinkRegistry()

        registry.shouldNotBeNull()
    }

    test("getDeepLinkRegistry allows runtime pattern registration") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        val registry = navigator.getDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            ProfileDestination
        }

        registry.canHandle("app://profile/123").shouldBeTrue()
    }

    test("handleDeepLink navigates to registered destination") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        val registry = navigator.getDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            ProfileDestination
        }

        val handled = navigator.handleDeepLink("app://profile/123")

        handled.shouldBeTrue()
        navigator.currentDestination.value shouldBe ProfileDestination
    }

    test("handleDeepLink returns false for unregistered pattern") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        val handled = navigator.handleDeepLink("app://unknown/path")

        handled.shouldBeFalse()
        navigator.currentDestination.value shouldBe HomeDestination
    }

    test("handleDeepLink with DeepLink object navigates correctly") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        val registry = navigator.getDeepLinkRegistry()

        registry.register("settings") { SettingsDestination }

        val deepLink = DeepLink.parse("app://settings")
        navigator.handleDeepLink(deepLink)

        navigator.currentDestination.value shouldBe SettingsDestination
    }

    test("registry registerAction executes custom navigation logic") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        val registry = navigator.getDeepLinkRegistry()

        registry.registerAction("back") { nav, _ ->
            nav.navigateBack()
        }

        val handled = navigator.handleDeepLink("app://back")

        handled.shouldBeTrue()
        navigator.currentDestination.value shouldBe HomeDestination
    }

    // =========================================================================
    // UPDATE STATE TESTS
    // =========================================================================

    test("updateState directly sets new state") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        val newState = StackNode(
            key = NodeKey("new-root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("new-root"), ProfileDestination),
                ScreenNode(NodeKey("s2"), NodeKey("new-root"), SettingsDestination)
            )
        )

        navigator.updateState(newState, null)

        val state = navigator.state.value as StackNode
        state.key shouldBe NodeKey("new-root")
        state.children.size shouldBe 2
        navigator.currentDestination.value shouldBe SettingsDestination
    }

    test("updateState with transition sets transition state") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        val newState = StackNode(
            key = NodeKey("new-root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("new-root"), ProfileDestination)
            )
        )

        navigator.updateState(newState, TestTransition)

        val transitionState = navigator.transitionState.value
        (transitionState is TransitionState.InProgress).shouldBeTrue()
    }

    test("updateState without transition sets Idle") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination, TestTransition)

        val newState = navigator.state.value

        navigator.updateState(newState, null)

        navigator.transitionState.value shouldBe TransitionState.Idle
    }

    // =========================================================================
    // DERIVED STATE SYNCHRONIZATION TESTS
    // =========================================================================

    test("derived states update synchronously after navigation") {
        val navigator = TreeNavigator.withDestination(HomeDestination)

        // Before navigation
        navigator.currentDestination.value shouldBe HomeDestination
        navigator.previousDestination.value.shouldBeNull()
        navigator.canNavigateBack.value.shouldBeFalse()

        // After navigation
        navigator.navigate(ProfileDestination)

        navigator.currentDestination.value shouldBe ProfileDestination
        navigator.previousDestination.value shouldBe HomeDestination
        navigator.canNavigateBack.value.shouldBeTrue()
    }

    test("derived states update after back navigation") {
        val navigator = TreeNavigator.withDestination(HomeDestination)
        navigator.navigate(ProfileDestination)
        navigator.navigate(SettingsDestination)

        navigator.navigateBack()

        navigator.currentDestination.value shouldBe ProfileDestination
        navigator.previousDestination.value shouldBe HomeDestination
        navigator.canNavigateBack.value.shouldBeTrue()
    }

    test("derived states update after tab switch") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val newState = TreeMutator.switchActiveTab(navigator.state.value, 1)
        navigator.updateState(newState)

        navigator.currentDestination.value shouldBe ProfileDestination
    }

    // =========================================================================
    // COMPLEX NAVIGATION SCENARIO TESTS
    // =========================================================================

    test("complex navigation flow with tabs") {
        // Start with tabs wrapped in root stack
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("home-tab"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("home-screen"), NodeKey("home-tab"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("profile-tab"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("profile-screen"), NodeKey("profile-tab"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        // Navigate in home tab
        navigator.navigate(DetailDestination)
        navigator.currentDestination.value shouldBe DetailDestination

        // Switch to profile tab
        var newState = TreeMutator.switchActiveTab(navigator.state.value, 1)
        navigator.updateState(newState)
        navigator.currentDestination.value shouldBe ProfileDestination

        // Navigate in profile tab
        navigator.navigate(SettingsDestination)
        navigator.currentDestination.value shouldBe SettingsDestination

        // Go back in profile tab
        navigator.navigateBack()
        navigator.currentDestination.value shouldBe ProfileDestination

        // Switch back to home tab - should preserve navigation state
        newState = TreeMutator.switchActiveTab(navigator.state.value, 0)
        navigator.updateState(newState)
        navigator.currentDestination.value shouldBe DetailDestination

        // Go back in home tab
        navigator.navigateBack()
        navigator.currentDestination.value shouldBe HomeDestination
    }

    } // init
}
