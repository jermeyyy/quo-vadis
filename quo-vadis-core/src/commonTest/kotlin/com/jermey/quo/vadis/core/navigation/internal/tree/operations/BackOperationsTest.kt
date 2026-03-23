@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.BackResult
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

// =============================================================================
// Test destinations
// =============================================================================

private object Home : NavDestination {
    override fun toString(): String = "Home"
}

private object Profile : NavDestination {
    override fun toString(): String = "Profile"
}

private object Settings : NavDestination {
    override fun toString(): String = "Settings"
}

private object Detail : NavDestination {
    override fun toString(): String = "Detail"
}

private object ListDest : NavDestination {
    override fun toString(): String = "ListDest"
}

class BackOperationsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // canGoBack
    // =========================================================================

    test("canGoBack returns true when active stack has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home),
                ScreenNode(NodeKey("s2"), NodeKey("root"), Profile)
            )
        )

        BackOperations.canGoBack(root).shouldBeTrue()
    }

    test("canGoBack returns false for single-screen root stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home)
            )
        )

        BackOperations.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack returns false for empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = emptyList()
        )

        BackOperations.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack returns false when root is a ScreenNode (no active stack)") {
        val root = ScreenNode(NodeKey("screen"), null, Home)

        BackOperations.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack returns true when nested tab stack has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), Profile)
                            )
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s3"), NodeKey("tab1"), Settings))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        BackOperations.canGoBack(root).shouldBeTrue()
    }

    test("canGoBack returns false when nested tab stack has single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        BackOperations.canGoBack(root).shouldBeFalse()
    }

    // =========================================================================
    // currentDestination
    // =========================================================================

    test("currentDestination returns leaf screen destination") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home),
                ScreenNode(NodeKey("s2"), NodeKey("root"), Profile)
            )
        )

        BackOperations.currentDestination(root) shouldBe Profile
    }

    test("currentDestination returns null for empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        BackOperations.currentDestination(root).shouldBeNull()
    }

    test("currentDestination returns destination in active tab") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s2"), NodeKey("tab1"), Profile))
                        )
                    ),
                    activeStackIndex = 1
                )
            )
        )

        BackOperations.currentDestination(root) shouldBe Profile
    }

    test("currentDestination returns destination in active pane") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                            )
                        ),
                        PaneRole.Supporting to PaneConfiguration(
                            StackNode(
                                NodeKey("supporting"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ss1"), NodeKey("supporting"), Detail))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Supporting
                )
            )
        )

        BackOperations.currentDestination(root) shouldBe Detail
    }

    // =========================================================================
    // popWithTabBehavior — stack pops
    // =========================================================================

    test("popWithTabBehavior pops from stack when it has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home),
                ScreenNode(NodeKey("s2"), NodeKey("root"), Profile)
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    test("popWithTabBehavior returns CannotHandle when root has no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, Home)

        val result = BackOperations.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.CannotHandle>()
    }

    // =========================================================================
    // popWithTabBehavior — root stack back (handleRootStackBack)
    // =========================================================================

    test("popWithTabBehavior delegates to system for root stack with single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home)
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior delegates to system for empty root stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = BackOperations.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    // =========================================================================
    // popWithTabBehavior — tab back (handleTabBack)
    // =========================================================================

    test("popWithTabBehavior pops from tab stack when it has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), Profile)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        val tabs = newRoot.children[0] as TabNode
        tabs.stacks[0].children.size shouldBe 1
        (tabs.stacks[0].activeChild as ScreenNode).destination shouldBe Home
    }

    test("popWithTabBehavior pops entire TabNode when tab stack at root and parent has multiple children") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Profile))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    test("popWithTabBehavior delegates to system when TabNode is on root stack with only one child") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior delegates to system when TabNode is root (no parent)") {
        val tabRoot = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                )
            ),
            activeStackIndex = 0
        )

        val result = BackOperations.popWithTabBehavior(tabRoot)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    // =========================================================================
    // popWithTabBehavior — nested stack back (handleNestedStackBack)
    // =========================================================================

    test("popWithTabBehavior pops nested stack from parent when parent has multiple children") {
        // root -> [Home screen, nested stack -> [Profile]]
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home),
                StackNode(
                    NodeKey("nested"), NodeKey("root"),
                    listOf(ScreenNode(NodeKey("s2"), NodeKey("nested"), Profile))
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    test("popWithTabBehavior delegates to system for nested stack when parent is root with single child") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                StackNode(
                    NodeKey("nested"), NodeKey("root"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("nested"), Home))
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior cascades through nested stacks") {
        // root -> [base screen, middle stack -> [inner stack -> [Home]]]
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                StackNode(
                    key = NodeKey("middle"),
                    parentKey = NodeKey("root"),
                    children = listOf(
                        StackNode(
                            key = NodeKey("inner"),
                            parentKey = NodeKey("middle"),
                            children = listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("inner"), Profile)
                            )
                        )
                    )
                )
            )
        )

        // inner has 1 child, middle has 1 child but root has 2 children
        // -> cascade up: inner can't pop, middle has 1 child so cascades, root pops middle
        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    // =========================================================================
    // popWithTabBehavior — pane back (handlePaneBack)
    // =========================================================================

    test("popWithTabBehavior pops from pane stack when it has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(
                                    ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home),
                                    ScreenNode(NodeKey("ps2"), NodeKey("primary"), Profile)
                                )
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    backBehavior = PaneBackBehavior.PopLatest
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root, isCompact = true)

        // The active stack (primary) has >1 children, so pop directly succeeds
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        handled.newState.shouldNotBeNull()
        val newRoot = handled.newState as StackNode
        val pane = newRoot.children[0] as PaneNode
        val primaryStack = pane.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        (primaryStack.children[0] as ScreenNode).destination shouldBe Home
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("popWithTabBehavior in expanded mode pops entire PaneNode") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), ListDest))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    backBehavior = PaneBackBehavior.PopLatest
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root, isCompact = false)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    test("popWithTabBehavior delegates to system when PaneNode is only child of root in expanded mode") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    backBehavior = PaneBackBehavior.PopLatest
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root, isCompact = false)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior pops entire PaneNode when pane handling exhausted in compact mode") {
        // PaneNode with single-screen primary stack using PopUntilCurrentDestinationChange
        // and no alternative panes → PaneEmpty → popEntirePaneNode
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), ListDest))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root, isCompact = true)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    // =========================================================================
    // popWithTabBehavior — PaneNode as root
    // =========================================================================

    test("popWithTabBehavior delegates to system when PaneNode has no parent") {
        val paneRoot = PaneNode(
            key = NodeKey("pane"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        NodeKey("primary"), NodeKey("pane"),
                        listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        val result = BackOperations.popWithTabBehavior(paneRoot, isCompact = false)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    // =========================================================================
    // canHandleBackNavigation
    // =========================================================================

    test("canHandleBackNavigation returns true when active stack has multiple screens") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home),
                ScreenNode(NodeKey("s2"), NodeKey("root"), Profile)
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns false for root stack with single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), Home)
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns false when no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, Home)

        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns true for tab when parent stack has multiple children") {
        // Tab has single-screen stack but parent stack has base screen + TabNode
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Profile))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns false for tab when parent stack is root with single child") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns true for nested stack when parent can go back") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                StackNode(
                    NodeKey("nested"), NodeKey("root"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("nested"), Profile))
                )
            )
        )

        // Active stack is "nested" (1 child), parent is "root" which canGoBack (2 children)
        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns false for nested stack when parent cannot go back") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                StackNode(
                    NodeKey("nested"), NodeKey("root"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("nested"), Home))
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns true for pane when a pane stack can go back") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(
                                    ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home),
                                    ScreenNode(NodeKey("ps2"), NodeKey("primary"), Profile)
                                )
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns true for pane when pane can be popped from parent") {
        // All pane stacks at root but parent stack has multiple children
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), ListDest))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns false for pane when no pane can go back and parent has single child") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary
                )
            )
        )

        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns false when active stack parentKey points to unknown node") {
        // StackNode with parentKey that does not exist in the tree
        val orphanStack = StackNode(
            key = NodeKey("orphan"),
            parentKey = NodeKey("nonexistent"),
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("orphan"), Home))
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(orphanStack)
        )

        // Active stack is orphanStack (1 child, can't go back)
        // parentKey is "nonexistent" which resolves to null -> falls through to else -> false
        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }

    // =========================================================================
    // handleTabBack — cascade behavior
    // =========================================================================

    test("popWithTabBehavior cascades through tab back to grandparent stack") {
        // root -> [base, grandparent stack -> [tab -> [single screen]]]
        // When tab can't pop, cascade should pop grandparent-stack's child (TabNode)
        // grandparent has 1 child so cascades to root, root has 2 so can pop
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                StackNode(
                    key = NodeKey("grandparent"),
                    parentKey = NodeKey("root"),
                    children = listOf(
                        TabNode(
                            key = NodeKey("tabs"),
                            parentKey = NodeKey("grandparent"),
                            stacks = listOf(
                                StackNode(
                                    NodeKey("tab0"), NodeKey("tabs"),
                                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Profile))
                                )
                            ),
                            activeStackIndex = 0
                        )
                    )
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    // =========================================================================
    // handlePaneBack — popEntirePaneNode
    // =========================================================================

    test("popWithTabBehavior handles pane inside tab cascade") {
        // root -> [base, tab -> [stack -> [pane -> [single screen]]]]
        // Pane exhausted -> popEntirePaneNode -> parent is stack with 1 child ->
        // cascade to tab -> tab parent is root -> root has 2 children -> pop tab node
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            key = NodeKey("tab0"),
                            parentKey = NodeKey("tabs"),
                            children = listOf(
                                PaneNode(
                                    key = NodeKey("pane"),
                                    parentKey = NodeKey("tab0"),
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            StackNode(
                                                NodeKey("primary"), NodeKey("pane"),
                                                listOf(
                                                    ScreenNode(
                                                        NodeKey("ps1"),
                                                        NodeKey("primary"),
                                                        Profile
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    activePaneRole = PaneRole.Primary,
                                    backBehavior = PaneBackBehavior.PopLatest
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root, isCompact = false)

        // In expanded mode, popEntirePaneNode -> tab0 has 1 child so cascade
        // -> handleTabBack -> parent (root) has 2 children -> pop TabNode
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    // =========================================================================
    // BackResult variants
    // =========================================================================

    test("BackResult.Handled wraps new state") {
        val state = StackNode(NodeKey("s"), null, emptyList())
        val result: BackResult = BackResult.Handled(state)

        result.shouldBeInstanceOf<BackResult.Handled>()
        result.newState shouldBe state
    }

    test("BackResult.DelegateToSystem is a singleton") {
        val result: BackResult = BackResult.DelegateToSystem
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("BackResult.CannotHandle is a singleton") {
        val result: BackResult = BackResult.CannotHandle
        result.shouldBeInstanceOf<BackResult.CannotHandle>()
    }

    // =========================================================================
    // popWithTabBehavior — deep stack pops in tabs
    // =========================================================================

    test("popWithTabBehavior pops deep active tab stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), Profile),
                                ScreenNode(NodeKey("s3"), NodeKey("tab0"), Settings)
                            )
                        ),
                        StackNode(
                            NodeKey("tab1"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s4"), NodeKey("tab1"), Detail))
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        val tabs = newRoot.children[0] as TabNode
        tabs.stacks[0].children.size shouldBe 2
        (tabs.stacks[0].activeChild as ScreenNode).destination shouldBe Profile
        // Second tab should be unchanged
        tabs.stacks[1].children.size shouldBe 1
    }

    // =========================================================================
    // popWithTabBehavior — tab inside tab (edge case)
    // =========================================================================

    test("popWithTabBehavior handles TabNode inside another TabNode") {
        // root -> [base, outerTab -> [innerTab -> [single screen]]]
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("base"), NodeKey("root"), Home),
                TabNode(
                    key = NodeKey("outerTabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            key = NodeKey("outerStack0"),
                            parentKey = NodeKey("outerTabs"),
                            children = listOf(
                                TabNode(
                                    key = NodeKey("innerTabs"),
                                    parentKey = NodeKey("outerStack0"),
                                    stacks = listOf(
                                        StackNode(
                                            NodeKey("innerStack0"), NodeKey("innerTabs"),
                                            listOf(
                                                ScreenNode(
                                                    NodeKey("s1"),
                                                    NodeKey("innerStack0"),
                                                    Profile
                                                )
                                            )
                                        )
                                    ),
                                    activeStackIndex = 0
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = BackOperations.popWithTabBehavior(root)

        // Inner tab can't pop, cascades to outerStack0 (1 child), cascades to outerTabs
        // outerTabs parent is root with 2 children -> pop outerTabs
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newRoot = handled.newState.shouldBeInstanceOf<StackNode>()
        newRoot.children.size shouldBe 1
        (newRoot.activeChild as ScreenNode).destination shouldBe Home
    }

    // =========================================================================
    // popWithTabBehavior — pane with multiple pane configurations
    // =========================================================================

    test("popWithTabBehavior pops from pane with supporting pane content in compact mode") {
        // Pane with primary and supporting, active=Supporting, supporting stack has 2 items
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                            )
                        ),
                        PaneRole.Supporting to PaneConfiguration(
                            StackNode(
                                NodeKey("supporting"), NodeKey("pane"),
                                listOf(
                                    ScreenNode(NodeKey("ss1"), NodeKey("supporting"), ListDest),
                                    ScreenNode(NodeKey("ss2"), NodeKey("supporting"), Detail)
                                )
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Supporting,
                    backBehavior = PaneBackBehavior.PopLatest
                )
            )
        )

        // Active stack is supporting with 2 children -> pop succeeds
        val result = BackOperations.popWithTabBehavior(root, isCompact = true)

        result.shouldBeInstanceOf<BackResult.Handled>()
    }

    // =========================================================================
    // canHandleBackNavigation — pane supporting stack can go back
    // =========================================================================

    test("canHandleBackNavigation returns true when supporting pane stack has content to pop") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                PaneNode(
                    key = NodeKey("pane"),
                    parentKey = NodeKey("root"),
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            StackNode(
                                NodeKey("primary"), NodeKey("pane"),
                                listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), Home))
                            )
                        ),
                        PaneRole.Supporting to PaneConfiguration(
                            StackNode(
                                NodeKey("supporting"), NodeKey("pane"),
                                listOf(
                                    ScreenNode(NodeKey("ss1"), NodeKey("supporting"), ListDest),
                                    ScreenNode(NodeKey("ss2"), NodeKey("supporting"), Detail)
                                )
                            )
                        )
                    ),
                    activePaneRole = PaneRole.Primary
                )
            )
        )

        // Even though active pane is Primary (single child), Supporting has 2 -> true
        BackOperations.canHandleBackNavigation(root).shouldBeTrue()
    }

    // =========================================================================
    // canHandleBackNavigation — tab without parent check
    // =========================================================================

    test("canHandleBackNavigation returns false when TabNode parent has no parentKey") {
        // Tab's parent is root stack, tab check needs tabParentKey -> root has no parent
        // But the check looks at tabParent.parentKey -> root.parentKey is null -> false
        // Wait, the check is: tabParent stack children size > 1
        // root has 1 child (tabs only) -> false
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), Home))
                )
            ),
            activeStackIndex = 0
        )

        // TabNode is root (parentKey=null), active stack has 1 child, parent of stack is tabs
        // -> canHandleBackNavigation checks: activeStack can go back (false)
        //    -> parent = tabs (TabNode), then checks tabParentKey = tabs.parentKey = null -> false
        BackOperations.canHandleBackNavigation(root).shouldBeFalse()
    }
})
