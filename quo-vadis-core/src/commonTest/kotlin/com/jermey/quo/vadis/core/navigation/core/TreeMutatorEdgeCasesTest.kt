package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.TreeOperationResult
import com.jermey.quo.vadis.core.navigation.internal.tree.result.getOrElse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Unit tests for TreeMutator edge cases and utility operations.
 *
 * Tests cover:
 * - `replaceNode`: replaces any node by key
 * - `removeNode`: removes node and handles various scenarios
 * - `replaceCurrent`: replaces top screen
 * - `canGoBack`: returns correct value
 * - `currentDestination`: returns active destination
 * - Deeply nested tree operations
 * - Empty tree edge cases
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorEdgeCasesTest : FunSpec() {

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

    init {

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("edge-key-${counter++}") }
    }

    beforeTest {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // REPLACE NODE TESTS
    // =========================================================================

    test("replaceNode replaces root node") {
        val oldRoot = StackNode(NodeKey("root"), null, emptyList())
        val newRoot = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination))
        )

        val result = TreeMutator.replaceNode(oldRoot, NodeKey("root"), newRoot)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        result.newTree shouldBeSameInstanceAs newRoot
    }

    test("replaceNode replaces nested screen") {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("stack"), HomeDestination)
        val root = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(targetScreen)
        )

        val newScreen = ScreenNode(NodeKey("target"), NodeKey("stack"), ProfileDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("target"), newScreen)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultStack = result.newTree as StackNode
        resultStack.children.size shouldBe 1
        val replacedScreen = resultStack.children[0] as ScreenNode
        replacedScreen.destination shouldBe ProfileDestination
    }

    test("replaceNode replaces node in TabNode") {
        val targetStack = StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                targetStack,
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val newStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination))
        )

        val result = TreeMutator.replaceNode(root, NodeKey("tab0"), newStack)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultTab = result.newTree as TabNode
        resultTab.stacks[0].children.size shouldBe 1
        resultTab.stacks[1].children.size shouldBe 0
    }

    test("replaceNode returns NodeNotFound for non-existent key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.replaceNode(
            root, NodeKey("nonexistent"), ScreenNode(NodeKey("new"), null, HomeDestination)
        )

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
        result.key shouldBe NodeKey("nonexistent")
    }

    test("replaceNode preserves structural sharing for unchanged branches") {
        val unchangedScreen = ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
        val unchangedStack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(unchangedScreen))

        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                unchangedStack
            ),
            activeStackIndex = 0
        )

        val newScreen = ScreenNode(NodeKey("s1"), NodeKey("tab0"), SettingsDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("s1"), newScreen)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultTab = result.newTree as TabNode
        // tab1 stack should be same reference
        resultTab.stacks[1] shouldBeSameInstanceAs unchangedStack
        resultTab.stacks[1].children[0] shouldBeSameInstanceAs unchangedScreen
    }

    test("replaceNode works with PaneNode") {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("primary-stack"), HomeDestination)
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(targetScreen))
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val newScreen = ScreenNode(NodeKey("target"), NodeKey("primary-stack"), ProfileDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("target"), newScreen)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultPanes = result.newTree as PaneNode
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        val replacedScreen = primaryStack.children[0] as ScreenNode
        replacedScreen.destination shouldBe ProfileDestination
    }

    // =========================================================================
    // REMOVE NODE TESTS
    // =========================================================================

    test("removeNode removes screen from stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultStack = result.newTree as StackNode
        resultStack.children.size shouldBe 1
        resultStack.children[0].key shouldBe NodeKey("s1")
    }

    test("removeNode returns null when removing root") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.removeNode(root, NodeKey("root"))

        result.shouldBeNull()
    }

    test("removeNode throws when removing stack from TabNode") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.removeNode(root, NodeKey("tab0"))
        }
    }

    test("removeNode removes node from nested stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val tabs = (result.newTree as StackNode).children[0] as TabNode
        val tab0 = tabs.stacks[0]
        tab0.children.size shouldBe 1
        tab0.children[0].key shouldBe NodeKey("s1")
    }

    test("removeNode returns NodeNotFound for non-existent key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.removeNode(root, NodeKey("nonexistent"))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
        result.key shouldBe NodeKey("nonexistent")
    }

    test("removeNode throws when removing pane content directly") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), HomeDestination)
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.removeNode(root, NodeKey("primary"))
        }
    }

    test("removeNode removes nested content from PaneNode") {
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), HomeDestination),
                            ScreenNode(NodeKey("s2"), NodeKey("primary-stack"), ProfileDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
        val resultPanes = result.newTree as PaneNode
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        primaryStack.children[0].key shouldBe NodeKey("s1")
    }

    // =========================================================================
    // REPLACE CURRENT TESTS
    // =========================================================================

    test("replaceCurrent replaces top screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = SettingsDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 2 // Same size
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination // First unchanged
        (result.activeChild as ScreenNode).destination shouldBe SettingsDestination // Top replaced
    }

    test("replaceCurrent works with single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("replaceCurrent throws on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            TreeMutator.replaceCurrent(root, HomeDestination)
        }
    }

    test("replaceCurrent throws when no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            TreeMutator.replaceCurrent(root, ProfileDestination)
        }
    }

    test("replaceCurrent targets deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
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

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should have the replacement
        result.stacks[0].children.size shouldBe 2
        (result.stacks[0].activeChild as ScreenNode).destination shouldBe DetailDestination

        // tab1 should be unchanged
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe SettingsDestination
    }

    // =========================================================================
    // CAN GO BACK TESTS
    // =========================================================================

    test("canGoBack returns true when stack has multiple items") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        TreeMutator.canGoBack(root).shouldBeTrue()
    }

    test("canGoBack returns false when stack has single item") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        TreeMutator.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack returns false for empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        TreeMutator.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack returns false for ScreenNode") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        TreeMutator.canGoBack(root).shouldBeFalse()
    }

    test("canGoBack checks deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
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

        TreeMutator.canGoBack(root).shouldBeTrue() // tab0 has 2 items
    }

    test("canGoBack reflects active tab state") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
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
            activeStackIndex = 1 // tab1 is active with single item
        )

        TreeMutator.canGoBack(root).shouldBeFalse()
    }

    // =========================================================================
    // CURRENT DESTINATION TESTS
    // =========================================================================

    test("currentDestination returns active destination") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val current = TreeMutator.currentDestination(root)

        current shouldBe ProfileDestination
    }

    test("currentDestination returns null for empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val current = TreeMutator.currentDestination(root)

        current.shouldBeNull()
    }

    test("currentDestination returns destination from ScreenNode") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val current = TreeMutator.currentDestination(root)

        current shouldBe HomeDestination
    }

    test("currentDestination follows active path in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
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
            activeStackIndex = 1
        )

        val current = TreeMutator.currentDestination(root)

        current shouldBe ProfileDestination
    }

    test("currentDestination follows active pane") {
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s2"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Supporting
        )

        val current = TreeMutator.currentDestination(root)

        current shouldBe DetailDestination
    }

    // =========================================================================
    // DEEPLY NESTED TREE TESTS
    // =========================================================================

    test("operations work on deeply nested structure") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("intro"), NodeKey("root"), HomeDestination),
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
                                            StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                                                    ScreenNode(NodeKey("list"), NodeKey("primary-stack"),
                                                        ListDestination
                                                    )
                                                )
                                            )
                                        ),
                                        PaneRole.Supporting to PaneConfiguration(
                                            StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                                                    ScreenNode(NodeKey("detail"), NodeKey("supporting-stack"),
                                                        DetailDestination
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    activePaneRole = PaneRole.Supporting
                                )
                            )
                        ),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("settings"), NodeKey("tab1"), SettingsDestination)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        // Current destination should be in supporting pane of tab0
        TreeMutator.currentDestination(root) shouldBe DetailDestination

        // Push to deepest active stack
        val afterPush = TreeMutator.push(root, ProfileDestination) { NodeKey("new-screen") }
        val supportingStack = afterPush.findByKey(NodeKey("supporting-stack")) as StackNode
        supportingStack.children.size shouldBe 2

        // Switch tab
        val afterTabSwitch = TreeMutator.switchTab(afterPush, NodeKey("tabs"), 1)
        TreeMutator.currentDestination(afterTabSwitch) shouldBe SettingsDestination

        // CanGoBack should check active path
        TreeMutator.canGoBack(afterTabSwitch).shouldBeFalse() // tab1 has single item
    }

    test("structural sharing preserved in deeply nested operations") {
        val tab1Screen = ScreenNode(NodeKey("settings"), NodeKey("tab1"), SettingsDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("home"), NodeKey("tab0"), HomeDestination)
                            )
                        ),
                        tab1Stack
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.push(root, ProfileDestination) { NodeKey("new") }

        // tab1 branch should be completely unchanged
        val resultTabs = (result as StackNode).children[0] as TabNode
        resultTabs.stacks[1] shouldBeSameInstanceAs tab1Stack
        resultTabs.stacks[1].children[0] shouldBeSameInstanceAs tab1Screen
    }

    // =========================================================================
    // EMPTY TREE EDGE CASES
    // =========================================================================

    test("push to empty root stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.push(root, HomeDestination) { NodeKey("first") }

        val resultStack = result as StackNode
        resultStack.children.size shouldBe 1
    }

    test("pop from tree with single screen returns empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("only"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.pop(root)

        // With default PRESERVE_EMPTY, returns tree with empty stack
        result.shouldNotBeNull()
        (result as StackNode).isEmpty.shouldBeTrue()
    }

    test("operations on empty TabNode stacks") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        // currentDestination should be null
        TreeMutator.currentDestination(root).shouldBeNull()

        // canGoBack should be false
        TreeMutator.canGoBack(root).shouldBeFalse()

        // pop should return null
        TreeMutator.pop(root).shouldBeNull()

        // push should work
        val afterPush = TreeMutator.push(root, HomeDestination) { NodeKey("first") }
        val tabs = afterPush as TabNode
        tabs.stacks[0].children.size shouldBe 1
        tabs.stacks[1].children.size shouldBe 0
    }

    test("switchTab preserves empty stack state") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(root, NodeKey("tabs"), 1) as TabNode

        result.activeStackIndex shouldBe 1
        result.stacks[0].children.size shouldBe 1 // tab0 unchanged
        result.stacks[1].children.size shouldBe 0 // tab1 still empty
    }

    // =========================================================================
    // CONCURRENT-LIKE SCENARIOS
    // =========================================================================

    test("multiple sequential operations maintain integrity") {
        var state: NavNode = StackNode(NodeKey("root"), null, emptyList())

        // Push multiple screens
        state = TreeMutator.push(state, HomeDestination) { NodeKey("s1") }
        state = TreeMutator.push(state, ProfileDestination) { NodeKey("s2") }
        state = TreeMutator.push(state, SettingsDestination) { NodeKey("s3") }

        (state as StackNode).children.size shouldBe 3
        TreeMutator.currentDestination(state) shouldBe SettingsDestination

        // Pop one
        state = TreeMutator.pop(state)!!
        (state as StackNode).children.size shouldBe 2
        TreeMutator.currentDestination(state) shouldBe ProfileDestination

        // Replace current
        state = TreeMutator.replaceCurrent(state, DetailDestination) { NodeKey("s4") }
        (state as StackNode).children.size shouldBe 2
        TreeMutator.currentDestination(state) shouldBe DetailDestination

        // Clear and push
        state = TreeMutator.clearAndPush(state, ListDestination) { NodeKey("s5") }
        (state as StackNode).children.size shouldBe 1
        TreeMutator.currentDestination(state) shouldBe ListDestination
    }

    test("immutability ensures old state is unchanged") {
        val original = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val modified = TreeMutator.push(original, ProfileDestination) { NodeKey("s2") }

        // Original should be unchanged
        original.children.size shouldBe 1
        (original.activeChild as ScreenNode).destination shouldBe HomeDestination

        // Modified should have new state
        (modified as StackNode).children.size shouldBe 2
        (modified.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    } // init
}
