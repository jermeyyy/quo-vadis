package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
class TreeMutatorPaneTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    private object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    private object ExtraDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "extra"
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    private fun createKeyGenerator(): () -> String {
        var counter = 0
        return { "pane-key-${counter++}" }
    }

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // Helper to create a standard pane setup
    private fun createStandardPaneNode(): PaneNode {
        return PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("list-screen", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("detail-screen", "supporting-stack", DetailDestination)
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

    @Test
    fun `navigateToPane pushes to target pane`() {
        val panes = createStandardPaneNode()

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // Supporting pane should have 2 items now
        val supportingStack = result.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(2, supportingStack.children.size)
        assertEquals(SettingsDestination, (supportingStack.activeChild as ScreenNode).destination)

        // activePaneRole should still be Primary (switchFocus = false)
        assertEquals(PaneRole.Primary, result.activePaneRole)
    }

    @Test
    fun `navigateToPane with switchFocus changes activePaneRole`() {
        val panes = createStandardPaneNode()

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // activePaneRole should switch to Supporting
        assertEquals(PaneRole.Supporting, result.activePaneRole)
    }

    @Test
    fun `navigateToPane does not switch focus when already on target pane`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("list-screen", "primary-stack", ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Primary,
            destination = SettingsDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        ) as PaneNode

        assertEquals(PaneRole.Primary, result.activePaneRole)
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        assertEquals(2, primaryStack.children.size)
    }

    @Test
    fun `navigateToPane throws for invalid nodeKey`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.navigateToPane(
                root = panes,
                nodeKey = "nonexistent",
                role = PaneRole.Primary,
                destination = SettingsDestination
            )
        }
    }

    @Test
    fun `navigateToPane throws for unconfigured pane role`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.navigateToPane(
                root = panes,
                nodeKey = "panes",
                role = PaneRole.Supporting, // Not configured
                destination = SettingsDestination
            )
        }
    }

    @Test
    fun `navigateToPane preserves other panes unchanged`() {
        val primaryScreen = ScreenNode("list-screen", "primary-stack", ListDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", listOf(primaryScreen))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("detail-screen", "supporting-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.navigateToPane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        ) as PaneNode

        // Primary pane should be unchanged
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)
        assertSame(primaryScreen, primaryStack.children[0])
    }

    // =========================================================================
    // SWITCH ACTIVE PANE TESTS
    // =========================================================================

    @Test
    fun `switchActivePane updates activePaneRole`() {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting
        ) as PaneNode

        assertEquals(PaneRole.Supporting, result.activePaneRole)
    }

    @Test
    fun `switchActivePane returns same state when already on target role`() {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Primary // Already active
        )

        assertSame(panes, result)
    }

    @Test
    fun `switchActivePane throws for invalid nodeKey`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchActivePane(panes, "nonexistent", PaneRole.Supporting)
        }
    }

    @Test
    fun `switchActivePane throws for unconfigured role`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchActivePane(panes, "panes", PaneRole.Supporting)
        }
    }

    @Test
    fun `switchActivePane preserves all pane content`() {
        val panes = createStandardPaneNode()

        val result = TreeMutator.switchActivePane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting
        ) as PaneNode

        // Primary content should be unchanged
        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)
        assertEquals(ListDestination, (primaryStack.activeChild as ScreenNode).destination)

        // Supporting content should be unchanged
        val supportingStack = result.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
        assertEquals(DetailDestination, (supportingStack.activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // POP PANE TESTS
    // =========================================================================

    @Test
    fun `popPane removes from active pane`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination),
                            ScreenNode("s2", "primary-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Primary
        )

        assertNotNull(result)
        val resultPanes = result as PaneNode
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)
        assertEquals(ListDestination, (primaryStack.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popPane returns null when pane stack has single item`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, "panes", PaneRole.Primary)

        assertNull(result)
    }

    @Test
    fun `popPane returns null when pane stack is empty`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, "panes", PaneRole.Primary)

        assertNull(result)
    }

    @Test
    fun `popPane throws for invalid nodeKey`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.popPane(panes, "nonexistent", PaneRole.Primary)
        }
    }

    @Test
    fun `popPane throws for unconfigured role`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.popPane(panes, "panes", PaneRole.Supporting)
        }
    }

    @Test
    fun `popPane from inactive pane does not affect active pane`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("s2", "supporting-stack", DetailDestination),
                            ScreenNode("s3", "supporting-stack", SettingsDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popPane(panes, "panes", PaneRole.Supporting)

        assertNotNull(result)
        val resultPanes = result as PaneNode

        // Primary should be unchanged
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)

        // Supporting should have 1 item
        val supportingStack = resultPanes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
        assertEquals(DetailDestination, (supportingStack.activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // POP WITH PANE BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun `popWithPaneBehavior returns Popped when stack has content`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination),
                            ScreenNode("s2", "primary-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        assertTrue(result is TreeMutator.PopResult.Popped)
        val newPanes = (result as TreeMutator.PopResult.Popped).newState as PaneNode
        val primaryStack = newPanes.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)
    }

    @Test
    fun `popWithPaneBehavior with PopLatest returns Popped with empty stack when single item`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        // PopLatest still pops, leaving an empty stack
        assertTrue(result is TreeMutator.PopResult.Popped)
        val newPanes = (result as TreeMutator.PopResult.Popped).newState as PaneNode
        val primaryStack = newPanes.paneContent(PaneRole.Primary) as StackNode
        assertTrue(primaryStack.isEmpty)
    }

    @Test
    fun `popWithPaneBehavior with PopUntilScaffoldValueChange switches to Primary`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("s2", "supporting-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Supporting, // Start on Supporting
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        assertTrue(result is TreeMutator.PopResult.Popped)
        val newPanes = (result as TreeMutator.PopResult.Popped).newState as PaneNode
        assertEquals(PaneRole.Primary, newPanes.activePaneRole)
    }

    @Test
    fun `popWithPaneBehavior with PopUntilScaffoldValueChange returns RequiresScaffoldChange on Primary`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        )

        val result = TreeMutator.popWithPaneBehavior(panes)

        assertTrue(result is TreeMutator.PopResult.RequiresScaffoldChange)
    }

    @Test
    fun `popWithPaneBehavior without PaneNode does regular pop`() {
        val stack = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", ListDestination),
                ScreenNode("s2", "root", DetailDestination)
            )
        )

        val result = TreeMutator.popWithPaneBehavior(stack)

        assertTrue(result is TreeMutator.PopResult.Popped)
        val newStack = (result as TreeMutator.PopResult.Popped).newState as StackNode
        assertEquals(1, newStack.children.size)
    }

    @Test
    fun `popWithPaneBehavior without PaneNode pops to empty stack at root`() {
        val stack = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", ListDestination)
            )
        )

        val result = TreeMutator.popWithPaneBehavior(stack)

        // Pops to empty stack with PRESERVE_EMPTY behavior
        assertTrue(result is TreeMutator.PopResult.Popped)
        val newStack = (result as TreeMutator.PopResult.Popped).newState as StackNode
        assertTrue(newStack.isEmpty)
    }

    // =========================================================================
    // SET PANE CONFIGURATION TESTS
    // =========================================================================

    @Test
    fun `setPaneConfiguration updates pane config`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val newConfig = PaneConfiguration(
            content = StackNode(
                "supporting-stack", "panes", listOf(
                    ScreenNode("s2", "supporting-stack", DetailDestination)
                )
            ),
            adaptStrategy = AdaptStrategy.Levitate
        )

        val result = TreeMutator.setPaneConfiguration(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            config = newConfig
        ) as PaneNode

        assertEquals(2, result.paneCount)
        assertNotNull(result.paneContent(PaneRole.Supporting))
        assertEquals(AdaptStrategy.Levitate, result.adaptStrategy(PaneRole.Supporting))
    }

    @Test
    fun `setPaneConfiguration replaces existing config`() {
        val panes = createStandardPaneNode()

        val newContent = StackNode(
            "new-supporting-stack", "panes", listOf(
                ScreenNode("new-screen", "new-supporting-stack", SettingsDestination)
            )
        )
        val newConfig = PaneConfiguration(
            content = newContent,
            adaptStrategy = AdaptStrategy.Hide
        )

        val result = TreeMutator.setPaneConfiguration(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            config = newConfig
        ) as PaneNode

        val supportingContent = result.paneContent(PaneRole.Supporting)
        assertEquals("new-supporting-stack", (supportingContent as StackNode).key)
        assertEquals(AdaptStrategy.Hide, result.adaptStrategy(PaneRole.Supporting))
    }

    @Test
    fun `setPaneConfiguration throws for invalid nodeKey`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.setPaneConfiguration(
                root = panes,
                nodeKey = "nonexistent",
                role = PaneRole.Extra,
                config = PaneConfiguration(ScreenNode("s", "nonexistent", ListDestination))
            )
        }
    }

    // =========================================================================
    // REMOVE PANE CONFIGURATION TESTS
    // =========================================================================

    @Test
    fun `removePaneConfiguration removes config`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("s2", "supporting-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.removePaneConfiguration(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting
        ) as PaneNode

        assertEquals(1, result.paneCount)
        assertNull(result.paneContent(PaneRole.Supporting))
        assertNotNull(result.paneContent(PaneRole.Primary))
    }

    @Test
    fun `removePaneConfiguration switches to Primary when removing active pane`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        "supporting-stack", "panes", listOf(
                            ScreenNode("s2", "supporting-stack", DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Supporting // Supporting is active
        )

        val result = TreeMutator.removePaneConfiguration(
            root = panes,
            nodeKey = "panes",
            role = PaneRole.Supporting
        ) as PaneNode

        assertEquals(PaneRole.Primary, result.activePaneRole)
    }

    @Test
    fun `removePaneConfiguration throws for Primary role`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.removePaneConfiguration(panes, "panes", PaneRole.Primary)
        }
    }

    @Test
    fun `removePaneConfiguration throws for invalid nodeKey`() {
        val panes = createStandardPaneNode()

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.removePaneConfiguration(panes, "nonexistent", PaneRole.Supporting)
        }
    }

    // =========================================================================
    // PANE INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `full pane workflow - navigate then switch then pop`() {
        var current: NavNode = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("list", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        // Navigate to supporting pane
        current = TreeMutator.navigateToPane(
            root = current,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = DetailDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        )

        var panes = current as PaneNode
        assertEquals(PaneRole.Supporting, panes.activePaneRole)
        assertEquals(1, (panes.paneContent(PaneRole.Supporting) as StackNode).children.size)

        // Navigate again to supporting
        current = TreeMutator.navigateToPane(
            root = current,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = SettingsDestination,
            switchFocus = false,
            generateKey = createKeyGenerator()
        )

        panes = current as PaneNode
        assertEquals(2, (panes.paneContent(PaneRole.Supporting) as StackNode).children.size)

        // Pop from supporting
        val popResult = TreeMutator.popPane(current, "panes", PaneRole.Supporting)
        assertNotNull(popResult)

        panes = popResult as PaneNode
        assertEquals(1, (panes.paneContent(PaneRole.Supporting) as StackNode).children.size)

        // Switch back to primary
        current = TreeMutator.switchActivePane(panes, "panes", PaneRole.Primary)
        panes = current as PaneNode
        assertEquals(PaneRole.Primary, panes.activePaneRole)
    }

    @Test
    fun `pane in nested structure works correctly`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = "stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        "primary-stack", "panes", listOf(
                            ScreenNode("s1", "primary-stack", ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode("supporting-stack", "panes", emptyList())
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val root = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(panes)
        )

        val result = TreeMutator.navigateToPane(
            root = root,
            nodeKey = "panes",
            role = PaneRole.Supporting,
            destination = DetailDestination,
            switchFocus = true,
            generateKey = createKeyGenerator()
        )

        val resultPanes = result.findByKey("panes") as PaneNode
        assertEquals(PaneRole.Supporting, resultPanes.activePaneRole)
        val supportingStack = resultPanes.paneContent(PaneRole.Supporting) as StackNode
        assertEquals(1, supportingStack.children.size)
    }
}
