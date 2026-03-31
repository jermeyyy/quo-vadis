package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.PopResult
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Unit tests for TreeMutator pane operations.
 *
 * Tests cover:
 * - `navigateToPane`: pushes destination to specific pane
 * - `switchActivePane`: changes activePaneRole
 * - `popPane`: removes from specific pane's stack
 * - `popWithPaneBehavior`: handles PaneBackBehavior modes
 * - `setPaneConfiguration`: updates pane config
 * - `removePaneConfiguration`: removes pane config
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorPaneTest : FunSpec() {

    object ListDestination : NavDestination {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    object DetailDestination : NavDestination {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    object SettingsDestination : NavDestination {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    init {

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("pane-key-${counter++}") }
    }

    beforeTest {
        NavKeyGenerator.reset()
    }

    // Helper to create a standard pane setup
    fun createStandardPaneNode(): PaneNode {
        return PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("list-screen"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail-screen"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
    }

    // =========================================================================
    // NAVIGATE TO PANE TESTS
    // =========================================================================

    test("navigateToPane pushes to target pane") {
        val panes = createStandardPaneNode()

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // Supporting pane should have 2 items now
        val supportingStack = result.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 2
        (supportingStack.activeChild as ScreenNode).destination shouldBe SettingsDestination

        // activePaneRole should still be Primary (switchFocus = false)
        result.activePaneRole shouldBe PaneRole.Primary
    }

    test("navigateToPane with switchFocus changes activePaneRole") {
        val panes = createStandardPaneNode()

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // activePaneRole should switch to Supporting
        result.activePaneRole shouldBe PaneRole.Supporting
    }

    test("navigateToPane does not switch focus when already on target pane") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("list-screen"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Primary,
            destination = SettingsDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        ) as PaneNode

        result.activePaneRole shouldBe PaneRole.Primary
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 2
    }

    test("navigateToPane throws for invalid nodeKey") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.navigateToPane(
                root = panes,
                nodeKey = NodeKey("nonexistent"),
                role = PaneRole.Primary,
                destination = SettingsDestination
            )
        }
    }

    test("navigateToPane throws for unconfigured pane role") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.navigateToPane(
                root = panes,
                nodeKey = NodeKey("panes"),
                role = PaneRole.Supporting, // Not configured
                destination = SettingsDestination
            )
        }
    }

    test("navigateToPane preserves other panes unchanged") {
        val primaryScreen = ScreenNode(NodeKey("list-screen"), NodeKey("primary-stack"), ListDestination)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(primaryScreen))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail-screen"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // Primary pane should be unchanged
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        primaryStack.children[0] shouldBeSameInstanceAs primaryScreen
    }

    // =========================================================================
    // SWITCH ACTIVE PANE TESTS
    // =========================================================================

    test("switchActivePane updates activePaneRole") {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting
        ) as PaneNode

        result.activePaneRole shouldBe PaneRole.Supporting
    }

    test("switchActivePane returns same state when already on target role") {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Primary // Already active
        )

        result shouldBeSameInstanceAs panes
    }

    test("switchActivePane throws for invalid nodeKey") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.switchActivePane(panes, NodeKey("nonexistent"), PaneRole.Supporting)
        }
    }

    test("switchActivePane throws for unconfigured role") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.switchActivePane(panes, NodeKey("panes"), PaneRole.Supporting)
        }
    }

    test("switchActivePane preserves all pane content") {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting
        ) as PaneNode

        // Primary content should be unchanged
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        (primaryStack.activeChild as ScreenNode).destination shouldBe ListDestination

        // Supporting content should be unchanged
        val supportingStack = result.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
        (supportingStack.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    // =========================================================================
    // POP PANE TESTS
    // =========================================================================

    test("popPane removes from active pane") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination),
                            ScreenNode(NodeKey("s2"), NodeKey("primary-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Primary
        )

        result.shouldNotBeNull()
        val resultPanes = result as PaneNode
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        (primaryStack.activeChild as ScreenNode).destination shouldBe ListDestination
    }

    test("popPane returns null when pane stack has single item") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, NodeKey("panes"), PaneRole.Primary)

        result.shouldBeNull()
    }

    test("popPane returns null when pane stack is empty") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, NodeKey("panes"), PaneRole.Primary)

        result.shouldBeNull()
    }

    test("popPane throws for invalid nodeKey") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.popPane(panes, NodeKey("nonexistent"), PaneRole.Primary)
        }
    }

    test("popPane throws for unconfigured role") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.popPane(panes, NodeKey("panes"), PaneRole.Supporting)
        }
    }

    test("popPane from inactive pane does not affect active pane") {
        val panes = PaneNode(
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
                            ScreenNode(NodeKey("s2"), NodeKey("supporting-stack"), DetailDestination),
                            ScreenNode(NodeKey("s3"), NodeKey("supporting-stack"), SettingsDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, NodeKey("panes"), PaneRole.Supporting)

        result.shouldNotBeNull()
        val resultPanes = result as PaneNode

        // Primary should be unchanged
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1

        // Supporting should have 1 item
        val supportingStack = resultPanes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
        (supportingStack.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    // =========================================================================
    // POP WITH PANE BEHAVIOR TESTS
    // =========================================================================

    test("popWithPaneBehavior returns Popped when stack has content") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination),
                            ScreenNode(NodeKey("s2"), NodeKey("primary-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        (result is PopResult.Popped).shouldBeTrue()
        val newPanes = (result as PopResult.Popped).newState as PaneNode
        val primaryStack = newPanes.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
    }

    test("popWithPaneBehavior with PopLatest returns Popped with empty stack when single item") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        // PopLatest still pops, leaving an empty stack
        (result is PopResult.Popped).shouldBeTrue()
        val newPanes = (result as PopResult.Popped).newState as PaneNode
        val primaryStack = newPanes.paneContent(PaneRole.Primary) as StackNode
        primaryStack.isEmpty.shouldBeTrue()
    }

    test("popWithPaneBehavior with PopUntilScaffoldValueChange switches to Primary") {
        val panes = PaneNode(
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
            activePaneRole = PaneRole.Supporting, // Start on Supporting
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        (result is PopResult.Popped).shouldBeTrue()
        val newPanes = (result as PopResult.Popped).newState as PaneNode
        newPanes.activePaneRole shouldBe PaneRole.Primary
    }

    test("popWithPaneBehavior with PopUntilScaffoldValueChange returns RequiresScaffoldChange on Primary") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        (result is PopResult.RequiresScaffoldChange).shouldBeTrue()
    }

    test("popWithPaneBehavior without PaneNode does regular pop") {
        val stack = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), ListDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), DetailDestination)
            )
        )

        val result = TreeMutator.popWithPaneBehavior(stack)

        (result is PopResult.Popped).shouldBeTrue()
        val newStack = (result as PopResult.Popped).newState as StackNode
        newStack.children.size shouldBe 1
    }

    test("popWithPaneBehavior without PaneNode pops to empty stack at root") {
        val stack = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), ListDestination)
            )
        )

        val result = TreeMutator.popWithPaneBehavior(stack)

        // Pops to empty stack with PRESERVE_EMPTY behavior
        (result is PopResult.Popped).shouldBeTrue()
        val newStack = (result as PopResult.Popped).newState as StackNode
        newStack.isEmpty.shouldBeTrue()
    }

    // =========================================================================
    // SET PANE CONFIGURATION TESTS
    // =========================================================================

    test("setPaneConfiguration updates pane config") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val newConfig = PaneConfiguration(
            content = StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                    ScreenNode(NodeKey("s2"), NodeKey("supporting-stack"), DetailDestination)
                )
            ),
            adaptStrategy = AdaptStrategy.Levitate
        )

        val result = TreeMutator.setPaneConfiguration(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            config = newConfig
        ) as PaneNode

        result.paneCount shouldBe 2
        result.paneContent(PaneRole.Supporting).shouldNotBeNull()
        result.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate
    }

    test("setPaneConfiguration replaces existing config") {
        val panes = createStandardPaneNode()

        val newContent = StackNode(NodeKey("new-supporting-stack"), NodeKey("panes"), listOf(
                ScreenNode(NodeKey("new-screen"), NodeKey("new-supporting-stack"), SettingsDestination)
            )
        )
        val newConfig = PaneConfiguration(
            content = newContent,
            adaptStrategy = AdaptStrategy.Hide
        )

        val result = TreeMutator.setPaneConfiguration(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            config = newConfig
        ) as PaneNode

        val supportingContent = result.paneContent(PaneRole.Supporting)
        (supportingContent as StackNode).key shouldBe NodeKey("new-supporting-stack")
        result.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Hide
    }

    test("setPaneConfiguration throws for invalid nodeKey") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.setPaneConfiguration(
                root = panes,
                nodeKey = NodeKey("nonexistent"),
                role = PaneRole.Extra,
                config = PaneConfiguration(ScreenNode(NodeKey("s"), NodeKey("nonexistent"), ListDestination))
            )
        }
    }

    // =========================================================================
    // REMOVE PANE CONFIGURATION TESTS
    // =========================================================================

    test("removePaneConfiguration removes config") {
        val panes = PaneNode(
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
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.removePaneConfiguration(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting
        ) as PaneNode

        result.paneCount shouldBe 1
        result.paneContent(PaneRole.Supporting).shouldBeNull()
        result.paneContent(PaneRole.Primary).shouldNotBeNull()
    }

    test("removePaneConfiguration switches to Primary when removing active pane") {
        val panes = PaneNode(
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
            activePaneRole = PaneRole.Supporting // Supporting is active
        )

        val result = TreeMutator.removePaneConfiguration(
            root = panes,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting
        ) as PaneNode

        result.activePaneRole shouldBe PaneRole.Primary
    }

    test("removePaneConfiguration throws for Primary role") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.removePaneConfiguration(panes, NodeKey("panes"), PaneRole.Primary)
        }
    }

    test("removePaneConfiguration throws for invalid nodeKey") {
        val panes = createStandardPaneNode()

        shouldThrow<IllegalArgumentException> {
            TreeMutator.removePaneConfiguration(panes, NodeKey("nonexistent"), PaneRole.Supporting)
        }
    }

    // =========================================================================
    // PANE INTEGRATION TESTS
    // =========================================================================

    test("full pane workflow - navigate then switch then pop") {
        var current: NavNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("list"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        // Navigate to supporting pane
        current = TreeMutator.navigateToPane(
            root = current,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = DetailDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        )

        var panes = current as PaneNode
        panes.activePaneRole shouldBe PaneRole.Supporting
        (panes.paneContent(PaneRole.Supporting) as StackNode).children.size shouldBe 1

        // Navigate again to supporting
        current = TreeMutator.navigateToPane(
            root = current,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        )

        panes = current as PaneNode
        (panes.paneContent(PaneRole.Supporting) as StackNode).children.size shouldBe 2

        // Pop from supporting
        val popResult = TreeMutator.popPane(current, NodeKey("panes"), PaneRole.Supporting)
        popResult.shouldNotBeNull()

        panes = popResult as PaneNode
        (panes.paneContent(PaneRole.Supporting) as StackNode).children.size shouldBe 1

        // Switch back to primary
        current = TreeMutator.switchActivePane(panes, NodeKey("panes"), PaneRole.Primary)
        panes = current as PaneNode
        panes.activePaneRole shouldBe PaneRole.Primary
    }

    test("pane in nested structure works correctly") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("stack"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val root = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(panes)
        )

        val result = TreeMutator.navigateToPane(
            root = root,
            nodeKey = NodeKey("panes"),
            role = PaneRole.Supporting,
            destination = DetailDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        )

        val resultPanes = result.findByKey(NodeKey("panes")) as PaneNode
        resultPanes.activePaneRole shouldBe PaneRole.Supporting
        val supportingStack = resultPanes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
    }

    } // init
}
