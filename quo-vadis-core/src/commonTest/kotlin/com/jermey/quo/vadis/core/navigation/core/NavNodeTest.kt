package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.node.activePathToLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.allPaneNodes
import com.jermey.quo.vadis.core.navigation.node.allScreens
import com.jermey.quo.vadis.core.navigation.node.allStackNodes
import com.jermey.quo.vadis.core.navigation.node.allTabNodes
import com.jermey.quo.vadis.core.navigation.node.depth
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.node.nodeCount
import com.jermey.quo.vadis.core.navigation.node.paneForRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Comprehensive unit tests for the NavNode hierarchy.
 *
 * Tests cover:
 * - ScreenNode: creation, validation, properties
 * - StackNode: activeChild, canGoBack, isEmpty, size
 * - TabNode: validation (at least one stack, bounds checking), activeStack, stackAt, tabCount
 * - PaneNode: validation, activePane, paneCount
 * - Extension functions: findByKey, activePathToLeaf, activeLeaf, activeStack, allScreens, etc.
 */
@OptIn(InternalQuoVadisApi::class)
class NavNodeTest : FunSpec() {

    object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    object FeedDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    init {

    // =========================================================================
    // SCREEN NODE TESTS
    // =========================================================================

    test("ScreenNode holds destination correctly") {
        val node = ScreenNode(
            key = NodeKey("screen-1"),
            parentKey = NodeKey("stack-1"),
            destination = HomeDestination
        )

        node.key shouldBe NodeKey("screen-1")
        node.parentKey shouldBe NodeKey("stack-1")
        node.destination shouldBe HomeDestination
    }

    test("ScreenNode with null parentKey is valid root screen") {
        val node = ScreenNode(
            key = NodeKey("root-screen"),
            parentKey = null,
            destination = HomeDestination
        )

        node.parentKey.shouldBeNull()
        node.key shouldBe NodeKey("root-screen")
    }

    test("ScreenNode equality based on properties") {
        val node1 = ScreenNode(NodeKey("key1"), NodeKey("parent"), HomeDestination)
        val node2 = ScreenNode(NodeKey("key1"), NodeKey("parent"), HomeDestination)
        val node3 = ScreenNode(NodeKey("key2"), NodeKey("parent"), HomeDestination)

        node2 shouldBe node1
        (node1 == node3).shouldBeFalse()
    }

    // =========================================================================
    // STACK NODE TESTS
    // =========================================================================

    test("StackNode activeChild returns last element") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("stack"), ProfileDestination)
        val screen3 = ScreenNode(NodeKey("s3"), NodeKey("stack"), SettingsDestination)

        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(screen1, screen2, screen3)
        )

        stack.activeChild shouldBe screen3
    }

    test("StackNode activeChild returns null when empty") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = emptyList()
        )

        stack.activeChild.shouldBeNull()
    }

    test("StackNode canGoBack true when multiple children") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("stack"), ProfileDestination)
            )
        )

        stack.canGoBack.shouldBeTrue()
    }

    test("StackNode canGoBack false when single child") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination))
        )

        stack.canGoBack.shouldBeFalse()
    }

    test("StackNode canGoBack false when empty") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        stack.canGoBack.shouldBeFalse()
    }

    test("StackNode isEmpty true when no children") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        stack.isEmpty.shouldBeTrue()
        stack.size shouldBe 0
    }

    test("StackNode isEmpty false when has children") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination))
        )

        stack.isEmpty.shouldBeFalse()
        stack.size shouldBe 1
    }

    test("StackNode size reflects children count") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("stack"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("stack"), SettingsDestination)
            )
        )

        stack.size shouldBe 3
    }

    test("StackNode with nested stack") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("inner"), HomeDestination))
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(innerStack)
        )

        outerStack.activeChild shouldBe innerStack
        outerStack.canGoBack.shouldBeFalse()
    }

    // =========================================================================
    // TAB NODE TESTS
    // =========================================================================

    test("TabNode requires at least one stack") {
        shouldThrow<IllegalArgumentException> {
            TabNode(
                key = NodeKey("tabs"),
                parentKey = null,
                stacks = emptyList(),
                activeStackIndex = 0
            )
        }
    }

    test("TabNode validates activeStackIndex bounds - too high") {
        val stack = StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList())

        shouldThrow<IllegalArgumentException> {
            TabNode(
                key = NodeKey("tabs"),
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = 5
            )
        }
    }

    test("TabNode validates negative activeStackIndex") {
        val stack = StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList())

        shouldThrow<IllegalArgumentException> {
            TabNode(
                key = NodeKey("tabs"),
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = -1
            )
        }
    }

    test("TabNode activeStack returns correct stack") {
        val stack0 = StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())
        val stack1 = StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList())
        val stack2 = StackNode(NodeKey("s2"), NodeKey("tabs"), emptyList())

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 1
        )

        tabs.activeStack shouldBe stack1
        tabs.tabCount shouldBe 3
    }

    test("TabNode stackAt returns correct stack") {
        val stack0 = StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())
        val stack1 = StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList())

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        tabs.stackAt(0) shouldBe stack0
        tabs.stackAt(1) shouldBe stack1
    }

    test("TabNode stackAt throws for invalid index") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )

        shouldThrow<IndexOutOfBoundsException> {
            tabs.stackAt(5)
        }
    }

    test("TabNode tabCount returns number of stacks") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("s2"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        tabs.tabCount shouldBe 3
    }

    test("TabNode activeStackIndex at boundary is valid") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("s1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 1 // Last valid index
        )

        tabs.activeStackIndex shouldBe 1
        tabs.activeStack shouldBe tabs.stacks[1]
    }

    // =========================================================================
    // PANE NODE TESTS
    // =========================================================================

    test("PaneNode requires Primary pane") {
        shouldThrow<IllegalArgumentException> {
            PaneNode(
                key = NodeKey("panes"),
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Supporting to PaneConfiguration(
                        ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                    )
                ),
                activePaneRole = PaneRole.Supporting
            )
        }
    }

    test("PaneNode validates activePaneRole exists in configurations") {
        shouldThrow<IllegalArgumentException> {
            PaneNode(
                key = NodeKey("panes"),
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Primary to PaneConfiguration(
                        ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                    )
                ),
                activePaneRole = PaneRole.Supporting // Not in configurations
            )
        }
    }

    test("PaneNode activePane returns correct pane content") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), ListDestination)
        val supportingContent = ScreenNode(NodeKey("supporting"), NodeKey("panes"), DetailDestination)

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Supporting
        )

        panes.activePaneContent shouldBe supportingContent
        panes.paneCount shouldBe 2
    }

    test("PaneNode paneCount returns number of configured panes") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("p2"), NodeKey("panes"), DetailDestination)
                ),
                PaneRole.Extra to PaneConfiguration(
                    ScreenNode(NodeKey("p3"), NodeKey("panes"), SettingsDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.paneCount shouldBe 3
    }

    test("PaneNode paneContent returns content for given role") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), ListDestination)
        val supportingContent = ScreenNode(NodeKey("supporting"), NodeKey("panes"), DetailDestination)

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.paneContent(PaneRole.Primary) shouldBe primaryContent
        panes.paneContent(PaneRole.Supporting) shouldBe supportingContent
        panes.paneContent(PaneRole.Extra).shouldBeNull()
    }

    test("PaneNode adaptStrategy returns strategy for given role") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination),
                    AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("p2"), NodeKey("panes"), DetailDestination),
                    AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.adaptStrategy(PaneRole.Primary) shouldBe AdaptStrategy.Hide
        panes.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate
        panes.adaptStrategy(PaneRole.Extra).shouldBeNull()
    }

    test("PaneNode configuredRoles returns all configured roles") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("p2"), NodeKey("panes"), DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.configuredRoles shouldBe setOf(PaneRole.Primary, PaneRole.Supporting)
    }

    test("PaneNode with default backBehavior") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.backBehavior shouldBe PaneBackBehavior.PopUntilScaffoldValueChange
    }

    test("PaneNode with custom backBehavior") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        panes.backBehavior shouldBe PaneBackBehavior.PopLatest
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - findByKey
    // =========================================================================

    test("findByKey finds root node") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        root.findByKey(NodeKey("root")) shouldBe root
    }

    test("findByKey finds nested screen in StackNode") {
        val screen = ScreenNode(NodeKey("target"), NodeKey("stack"), HomeDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("other"), NodeKey("root"), ProfileDestination),
                screen
            )
        )

        root.findByKey(NodeKey("target")) shouldBe screen
    }

    test("findByKey returns null when not found") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        root.findByKey(NodeKey("nonexistent")).shouldBeNull()
    }

    test("findByKey finds node in TabNode") {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("tab1"), HomeDestination)
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(targetScreen))
            ),
            activeStackIndex = 0
        )

        root.findByKey(NodeKey("target")) shouldBe targetScreen
    }

    test("findByKey finds node in inactive tab") {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("tab1"), HomeDestination)
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s0"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(targetScreen))
            ),
            activeStackIndex = 0 // tab0 is active, but we're looking in tab1
        )

        tabs.findByKey(NodeKey("target")) shouldBe targetScreen
    }

    test("findByKey finds node in PaneNode") {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("panes"), HomeDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("other"), NodeKey("panes"), ProfileDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(targetScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.findByKey(NodeKey("target")) shouldBe targetScreen
    }

    test("findByKey in deeply nested structure") {
        val targetScreen = ScreenNode(NodeKey("deep-target"), NodeKey("inner-stack"), HomeDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                PaneNode(
                                    key = NodeKey("panes"),
                                    parentKey = NodeKey("tab0"),
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            StackNode(NodeKey("inner-stack"), NodeKey("panes"), listOf(targetScreen))
                                        )
                                    ),
                                    activePaneRole = PaneRole.Primary
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        root.findByKey(NodeKey("deep-target")) shouldBe targetScreen
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activePathToLeaf
    // =========================================================================

    test("activePathToLeaf returns single element for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val path = screen.activePathToLeaf()

        path.size shouldBe 1
        path[0] shouldBe screen
    }

    test("activePathToLeaf returns empty-ish path for empty StackNode") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        val path = stack.activePathToLeaf()

        path.size shouldBe 1
        path[0] shouldBe stack
    }

    test("activePathToLeaf returns complete path through StackNode") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("stack"), HomeDestination)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen))

        val path = stack.activePathToLeaf()

        path.size shouldBe 2
        path[0] shouldBe stack
        path[1] shouldBe screen
    }

    test("activePathToLeaf returns complete path through TabNode") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("stack"), HomeDestination)
        val stack = StackNode(NodeKey("stack"), NodeKey("tabs"), listOf(screen))
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack),
            activeStackIndex = 0
        )

        val path = tabs.activePathToLeaf()

        path.size shouldBe 3
        path[0] shouldBe tabs
        path[1] shouldBe stack
        path[2] shouldBe screen
    }

    test("activePathToLeaf returns complete path through PaneNode") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("panes"), HomeDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(screen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val path = panes.activePathToLeaf()

        path.size shouldBe 2
        path[0] shouldBe panes
        path[1] shouldBe screen
    }

    test("activePathToLeaf follows active tab only") {
        val activeScreen = ScreenNode(NodeKey("active-screen"), NodeKey("tab0"), HomeDestination)
        val inactiveScreen = ScreenNode(NodeKey("inactive-screen"), NodeKey("tab1"), ProfileDestination)

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(activeScreen)),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(inactiveScreen))
            ),
            activeStackIndex = 0
        )

        val path = tabs.activePathToLeaf()

        path.size shouldBe 3
        path.contains(activeScreen).shouldBeTrue()
        path.contains(inactiveScreen).shouldBeFalse()
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activeLeaf
    // =========================================================================

    test("activeLeaf returns ScreenNode itself") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.activeLeaf() shouldBe screen
    }

    test("activeLeaf returns null when no screens in stack") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        stack.activeLeaf().shouldBeNull()
    }

    test("activeLeaf returns deepest ScreenNode in StackNode") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("stack"), HomeDestination)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen))

        stack.activeLeaf() shouldBe screen
    }

    test("activeLeaf returns deepest active ScreenNode in TabNode") {
        val activeScreen = ScreenNode(NodeKey("active"), NodeKey("tab0"), HomeDestination)
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(activeScreen)),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("inactive"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        tabs.activeLeaf() shouldBe activeScreen
    }

    test("activeLeaf returns deepest active ScreenNode in PaneNode") {
        val activeScreen = ScreenNode(NodeKey("active"), NodeKey("primary-stack"), HomeDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(activeScreen))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("inactive"), NodeKey("panes"), DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.activeLeaf() shouldBe activeScreen
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activeStack
    // =========================================================================

    test("activeStack returns null for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.activeStack().shouldBeNull()
    }

    test("activeStack returns self for StackNode with no deeper stacks") {
        val stack = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination))
        )

        stack.activeStack() shouldBe stack
    }

    test("activeStack returns deepest active StackNode") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(ScreenNode(NodeKey("s"), NodeKey("inner"), HomeDestination))
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(innerStack)
        )

        outerStack.activeStack() shouldBe innerStack
    }

    test("activeStack returns deepest stack in TabNode") {
        val deepStack = StackNode(NodeKey("deep"), NodeKey("tab0"), listOf(
                ScreenNode(NodeKey("s"), NodeKey("deep"), HomeDestination)
            )
        )
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(deepStack)),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        tabs.activeStack() shouldBe deepStack
    }

    test("activeStack returns deepest stack in PaneNode") {
        val deepStack = StackNode(NodeKey("deep"), NodeKey("primary"), listOf(
                ScreenNode(NodeKey("s"), NodeKey("deep"), HomeDestination)
            )
        )
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary"), NodeKey("panes"), listOf(deepStack))
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.activeStack() shouldBe deepStack
    }

    test("activeStack returns TabNode activeStack when it has no deeper stacks") {
        val tabStack = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                ScreenNode(NodeKey("s"), NodeKey("tab0"), HomeDestination)
            )
        )
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )

        tabs.activeStack() shouldBe tabStack
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allScreens
    // =========================================================================

    test("allScreens returns single screen for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val screens = screen.allScreens()

        screens.size shouldBe 1
        screens.contains(screen).shouldBeTrue()
    }

    test("allScreens returns empty list for empty StackNode") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        val screens = stack.allScreens()

        screens.isEmpty().shouldBeTrue()
    }

    test("allScreens returns all screens in StackNode") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("stack"), ProfileDestination)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen1, screen2))

        val screens = stack.allScreens()

        screens.size shouldBe 2
        screens.contains(screen1).shouldBeTrue()
        screens.contains(screen2).shouldBeTrue()
    }

    test("allScreens returns all screens from all tabs in TabNode") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
        val screen3 = ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen1, screen2)),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen3))
            ),
            activeStackIndex = 0
        )

        val allScreens = tabs.allScreens()

        allScreens.size shouldBe 3
        allScreens.contains(screen1).shouldBeTrue()
        allScreens.contains(screen2).shouldBeTrue()
        allScreens.contains(screen3).shouldBeTrue()
    }

    test("allScreens returns all screens from all panes in PaneNode") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("primary"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("supporting"), DetailDestination)

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(screen1),
                PaneRole.Supporting to PaneConfiguration(screen2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val allScreens = panes.allScreens()

        allScreens.size shouldBe 2
        allScreens.contains(screen1).shouldBeTrue()
        allScreens.contains(screen2).shouldBeTrue()
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - paneForRole
    // =========================================================================

    test("paneForRole returns null for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.paneForRole(PaneRole.Primary).shouldBeNull()
    }

    test("paneForRole returns content for matching role in PaneNode") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), HomeDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.paneForRole(PaneRole.Primary) shouldBe primaryContent
    }

    test("paneForRole searches recursively through StackNode") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), HomeDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("stack"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val stack = StackNode(NodeKey("stack"), null, listOf(panes))

        stack.paneForRole(PaneRole.Primary) shouldBe primaryContent
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allPaneNodes
    // =========================================================================

    test("allPaneNodes returns empty for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.allPaneNodes().isEmpty().shouldBeTrue()
    }

    test("allPaneNodes returns all PaneNodes in tree") {
        val innerPane = PaneNode(
            key = NodeKey("inner-pane"),
            parentKey = NodeKey("outer-pane"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("s1"), NodeKey("inner-pane"), HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val outerPane = PaneNode(
            key = NodeKey("outer-pane"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerPane)
            ),
            activePaneRole = PaneRole.Primary
        )

        val allPanes = outerPane.allPaneNodes()

        allPanes.size shouldBe 2
        allPanes.contains(outerPane).shouldBeTrue()
        allPanes.contains(innerPane).shouldBeTrue()
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allTabNodes
    // =========================================================================

    test("allTabNodes returns empty for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.allTabNodes().isEmpty().shouldBeTrue()
    }

    test("allTabNodes returns all TabNodes in tree") {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("tab0"),
            stacks = listOf(StackNode(NodeKey("inner-tab0"), NodeKey("inner-tabs"), emptyList())),
            activeStackIndex = 0
        )
        val outerTabs = TabNode(
            key = NodeKey("outer-tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(innerTabs))
            ),
            activeStackIndex = 0
        )

        val allTabs = outerTabs.allTabNodes()

        allTabs.size shouldBe 2
        allTabs.contains(outerTabs).shouldBeTrue()
        allTabs.contains(innerTabs).shouldBeTrue()
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allStackNodes
    // =========================================================================

    test("allStackNodes returns empty for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.allStackNodes().isEmpty().shouldBeTrue()
    }

    test("allStackNodes returns all StackNodes in tree") {
        val stack1 = StackNode(NodeKey("stack1"), NodeKey("tabs"), emptyList())
        val stack2 = StackNode(NodeKey("stack2"), NodeKey("tabs"), emptyList())
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )
        val root = StackNode(NodeKey("root"), null, listOf(tabs))

        val allStacks = root.allStackNodes()

        allStacks.size shouldBe 3
        allStacks.contains(root).shouldBeTrue()
        allStacks.contains(stack1).shouldBeTrue()
        allStacks.contains(stack2).shouldBeTrue()
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - depth
    // =========================================================================

    test("depth returns 0 for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.depth() shouldBe 0
    }

    test("depth returns 0 for empty StackNode") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        stack.depth() shouldBe 0
    }

    test("depth returns correct value for nested structure") {
        val screen = ScreenNode(NodeKey("screen"), NodeKey("stack"), HomeDestination)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen))

        stack.depth() shouldBe 1
    }

    test("depth calculates max depth in TabNode") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        tabs.depth() shouldBe 2 // tabs -> stack -> screen
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - nodeCount
    // =========================================================================

    test("nodeCount returns 1 for ScreenNode") {
        val screen = ScreenNode(NodeKey("screen"), null, HomeDestination)

        screen.nodeCount() shouldBe 1
    }

    test("nodeCount returns 1 for empty StackNode") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        stack.nodeCount() shouldBe 1
    }

    test("nodeCount returns correct total for nested structure") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        // 1 (tabs) + 2 (stacks) + 2 (screens) = 5
        tabs.nodeCount() shouldBe 5
    }

    // =========================================================================
    // NAV KEY GENERATOR TESTS
    // =========================================================================

    test("NavKeyGenerator generates unique keys") {
        NavKeyGenerator.reset()

        val key1 = NodeKey(NavKeyGenerator.generate())
        val key2 = NodeKey(NavKeyGenerator.generate())
        val key3 = NodeKey(NavKeyGenerator.generate())

        (key1 == key2).shouldBeFalse()
        (key2 == key3).shouldBeFalse()
        (key1 == key3).shouldBeFalse()
    }

    test("NavKeyGenerator includes debug label when provided") {
        NavKeyGenerator.reset()

        val key = NavKeyGenerator.generate("home")

        key.startsWith("home-").shouldBeTrue()
    }

    test("NavKeyGenerator uses default prefix when no label") {
        NavKeyGenerator.reset()

        val key = NodeKey(NavKeyGenerator.generate())

        key.value.startsWith("node-").shouldBeTrue()
    }

    test("NavKeyGenerator reset restarts counter") {
        NavKeyGenerator.reset()
        val key1 = NodeKey(NavKeyGenerator.generate())

        NavKeyGenerator.reset()
        val key2 = NodeKey(NavKeyGenerator.generate())

        key2 shouldBe key1
    }

    // =========================================================================
    // COMPLEX INTEGRATION TESTS
    // =========================================================================

    test("complex tree navigation scenario") {
        // Build a complex tree: root stack -> tabs -> nested stacks with screens
        val homeScreen = ScreenNode(NodeKey("home-screen"), NodeKey("home-stack"), HomeDestination)
        val profileScreen1 = ScreenNode(NodeKey("profile-screen-1"), NodeKey("profile-stack"), ProfileDestination)
        val profileScreen2 = ScreenNode(NodeKey("profile-screen-2"), NodeKey("profile-stack"), DetailDestination)

        val homeStack = StackNode(NodeKey("home-stack"), NodeKey("tabs"), listOf(homeScreen))
        val profileStack =
            StackNode(NodeKey("profile-stack"), NodeKey("tabs"), listOf(profileScreen1, profileScreen2))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1 // Profile tab is active
        )

        val rootStack = StackNode(NodeKey("root"), null, listOf(tabs))

        // Verify activeLeaf
        rootStack.activeLeaf() shouldBe profileScreen2

        // Verify activeStack
        rootStack.activeStack() shouldBe profileStack

        // Verify activePathToLeaf
        val path = rootStack.activePathToLeaf()
        path.size shouldBe 4
        path[0] shouldBe rootStack
        path[1] shouldBe tabs
        path[2] shouldBe profileStack
        path[3] shouldBe profileScreen2

        // Verify allScreens
        val allScreens = rootStack.allScreens()
        allScreens.size shouldBe 3

        // Verify findByKey works across the tree
        rootStack.findByKey(NodeKey("home-screen")) shouldBe homeScreen
        rootStack.findByKey(NodeKey("profile-screen-1")) shouldBe profileScreen1
        rootStack.findByKey(NodeKey("tabs")) shouldBe tabs
    }

    test("pane-based adaptive layout scenario") {
        // Build a list-detail pane layout
        val listScreen = ScreenNode(NodeKey("list"), NodeKey("list-stack"), ListDestination)
        val detailScreen = ScreenNode(NodeKey("detail"), NodeKey("detail-stack"), DetailDestination)

        val listStack = StackNode(NodeKey("list-stack"), NodeKey("panes"), listOf(listScreen))
        val detailStack = StackNode(NodeKey("detail-stack"), NodeKey("panes"), listOf(detailScreen))

        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = listStack,
                    adaptStrategy = AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    content = detailStack,
                    adaptStrategy = AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Supporting,
            backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange
        )

        // Verify activeLeaf follows activePaneRole
        panes.activeLeaf() shouldBe detailScreen

        // Verify activeStack follows activePaneRole
        panes.activeStack() shouldBe detailStack

        // Verify paneContent
        panes.paneContent(PaneRole.Primary) shouldBe listStack
        panes.paneContent(PaneRole.Supporting) shouldBe detailStack

        // Verify adaptStrategies
        panes.adaptStrategy(PaneRole.Primary) shouldBe AdaptStrategy.Hide
        panes.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate

        // Verify allScreens includes both panes
        val allScreens = panes.allScreens()
        allScreens.size shouldBe 2
        allScreens.contains(listScreen).shouldBeTrue()
        allScreens.contains(detailScreen).shouldBeTrue()
    }

    } // init
}
