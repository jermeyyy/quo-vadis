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
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for pane layout rendering.
 *
 * Tests cover:
 * - Pane visibility based on AdaptStrategy
 * - Expanded vs compact mode rendering
 * - Pane switching
 * - Multi-pane layout scenarios
 * - PaneContent building
 */
class PaneRendererTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ExtraDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = ListDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    private fun createPanes(
        key: String,
        parentKey: String? = null,
        configurations: Map<PaneRole, PaneConfiguration>,
        activeRole: PaneRole = PaneRole.Primary,
        backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
    ): PaneNode = PaneNode(NodeKey(key), parentKey?.let { NodeKey(it) }, configurations, activeRole, backBehavior)

    /**
     * Simulates buildPaneContentList logic for testing.
     * Determines visibility based on expanded mode and adapt strategy.
     */
    private fun simulateBuildPaneContentList(
        node: PaneNode,
        isExpanded: Boolean
    ): List<PaneContentTestResult> {
        return node.configuredRoles.map { role ->
            val config = node.paneConfigurations[role]!!
            val isVisible = when {
                !isExpanded -> role == node.activePaneRole
                role == PaneRole.Primary -> true
                else -> config.adaptStrategy != AdaptStrategy.Hide
            }
            PaneContentTestResult(role, isVisible)
        }
    }

    private data class PaneContentTestResult(
        val role: PaneRole,
        val isVisible: Boolean
    )

    // =========================================================================
    // EXPANDED MODE TESTS (Multi-pane visible)
    // =========================================================================

    @Test
    fun `expanded mode shows all panes without Hide strategy`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Levitate
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Levitate
                )
            ),
            activeRole = PaneRole.Primary
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = true)

        // Then - both panes visible in expanded mode
        assertEquals(2, contents.size)
        assertTrue(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertTrue(contents.find { it.role == PaneRole.Supporting }!!.isVisible)
    }

    @Test
    fun `expanded mode hides panes with Hide strategy`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Levitate
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Hide // Will be hidden
                )
            ),
            activeRole = PaneRole.Primary
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = true)

        // Then - Primary visible, Supporting hidden
        assertTrue(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertFalse(contents.find { it.role == PaneRole.Supporting }!!.isVisible)
    }

    @Test
    fun `expanded mode always shows Primary regardless of strategy`() {
        // Given - Primary with Hide strategy (unusual but valid)
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Hide // Unusual but tested
                )
            ),
            activeRole = PaneRole.Primary
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = true)

        // Then - Primary is ALWAYS visible in expanded mode
        assertTrue(contents.find { it.role == PaneRole.Primary }!!.isVisible)
    }

    // =========================================================================
    // COMPACT MODE TESTS (Single pane visible)
    // =========================================================================

    @Test
    fun `compact mode shows only active pane`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
                PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes"))
            ),
            activeRole = PaneRole.Supporting // Supporting is active
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = false)

        // Then - only Supporting visible
        assertFalse(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertTrue(contents.find { it.role == PaneRole.Supporting }!!.isVisible)
    }

    @Test
    fun `compact mode shows Primary when active`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
                PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = false)

        // Then - only Primary visible
        assertTrue(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertFalse(contents.find { it.role == PaneRole.Supporting }!!.isVisible)
    }

    @Test
    fun `compact mode ignores adapt strategy for visibility`() {
        // Given - Hide strategy shouldn't matter in compact mode
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Levitate
                )
            ),
            activeRole = PaneRole.Supporting
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = false)

        // Then - only active pane visible, regardless of strategy
        assertFalse(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertTrue(contents.find { it.role == PaneRole.Supporting }!!.isVisible)
    }

    // =========================================================================
    // PANE SWITCHING TESTS
    // =========================================================================

    @Test
    fun `pane switching changes active pane role`() {
        // Given
        val config = mapOf(
            PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
            PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes"))
        )

        val beforeSwitch = createPanes("panes", null, config, activeRole = PaneRole.Primary)
        val afterSwitch = createPanes("panes", null, config, activeRole = PaneRole.Supporting)

        // Then
        assertEquals(PaneRole.Primary, beforeSwitch.activePaneRole)
        assertEquals(PaneRole.Supporting, afterSwitch.activePaneRole)
    }

    @Test
    fun `pane switching preserves content in both panes`() {
        // Given
        val primaryContent = createScreen("primary", "panes")
        val supportingContent = createScreen("supporting", "panes")

        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activeRole = PaneRole.Supporting
        )

        // Then
        assertEquals(primaryContent, panes.paneContent(PaneRole.Primary))
        assertEquals(supportingContent, panes.paneContent(PaneRole.Supporting))
    }

    // =========================================================================
    // PANE CONFIGURATION TESTS
    // =========================================================================

    @Test
    fun `pane returns correct adapt strategy per role`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Levitate
                ),
                PaneRole.Extra to PaneConfiguration(
                    createScreen("extra", "panes"),
                    AdaptStrategy.Reflow
                )
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertEquals(AdaptStrategy.Hide, panes.adaptStrategy(PaneRole.Primary))
        assertEquals(AdaptStrategy.Levitate, panes.adaptStrategy(PaneRole.Supporting))
        assertEquals(AdaptStrategy.Reflow, panes.adaptStrategy(PaneRole.Extra))
    }

    @Test
    fun `pane returns null adapt strategy for unconfigured role`() {
        // Given - only Primary configured
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertNotNull(panes.adaptStrategy(PaneRole.Primary))
        assertNull(panes.adaptStrategy(PaneRole.Supporting))
        assertNull(panes.adaptStrategy(PaneRole.Extra))
    }

    @Test
    fun `pane configured roles returns all configured roles`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
                PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertEquals(setOf(PaneRole.Primary, PaneRole.Supporting), panes.configuredRoles)
        assertEquals(2, panes.paneCount)
    }

    // =========================================================================
    // PANE BACK BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun `pane has default back behavior`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertEquals(PaneBackBehavior.PopUntilScaffoldValueChange, panes.backBehavior)
    }

    @Test
    fun `pane supports custom back behavior`() {
        // Given
        val popLatestPanes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        val popDestinationPanes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange
        )

        // Then
        assertEquals(PaneBackBehavior.PopLatest, popLatestPanes.backBehavior)
        assertEquals(
            PaneBackBehavior.PopUntilCurrentDestinationChange,
            popDestinationPanes.backBehavior
        )
    }

    // =========================================================================
    // PANE WRAPPER TESTS
    // =========================================================================

    @Test
    fun `FakeNavRenderScope container registry has no pane wrapper by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertFalse(scope.containerRegistry.hasPaneContainer("any-key"))
        assertFalse(scope.containerRegistry.hasPaneContainer("panes"))
    }

    @Test
    fun `container registry is empty by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertEquals(ContainerRegistry.Empty, scope.containerRegistry)
    }

    // =========================================================================
    // THREE-PANE LAYOUT TESTS
    // =========================================================================

    @Test
    fun `three pane layout has correct pane count`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
                PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes")),
                PaneRole.Extra to PaneConfiguration(createScreen("extra", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertEquals(3, panes.paneCount)
        assertEquals(
            setOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra),
            panes.configuredRoles
        )
    }

    @Test
    fun `three pane expanded mode visibility`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(
                    createScreen("primary", "panes"),
                    AdaptStrategy.Levitate
                ),
                PaneRole.Supporting to PaneConfiguration(
                    createScreen("supporting", "panes"),
                    AdaptStrategy.Hide // Hidden in expanded
                ),
                PaneRole.Extra to PaneConfiguration(
                    createScreen("extra", "panes"),
                    AdaptStrategy.Reflow
                )
            ),
            activeRole = PaneRole.Primary
        )

        // When
        val contents = simulateBuildPaneContentList(panes, isExpanded = true)

        // Then
        assertTrue(contents.find { it.role == PaneRole.Primary }!!.isVisible)
        assertFalse(contents.find { it.role == PaneRole.Supporting }!!.isVisible) // Hide strategy
        assertTrue(contents.find { it.role == PaneRole.Extra }!!.isVisible)
    }

    // =========================================================================
    // PANE WITH NESTED STACK TESTS
    // =========================================================================

    @Test
    fun `pane can contain stack as content`() {
        // Given
        val listStack = createStack(
            "list-stack", "panes",
            createScreen("list-1", "list-stack"),
            createScreen("list-2", "list-stack")
        )

        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(listStack)
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        val primaryContent = panes.paneContent(PaneRole.Primary)
        assertTrue(primaryContent is StackNode)
        assertEquals(2, (primaryContent as StackNode).children.size)
    }

    @Test
    fun `pane nested stack maintains navigation state`() {
        // Given
        val listStack = createStack(
            "list-stack", "panes",
            createScreen("l1", "list-stack"),
            createScreen("l2", "list-stack"),
            createScreen("l3", "list-stack")
        )
        val detailStack = createStack(
            "detail-stack", "panes",
            createScreen("d1", "detail-stack")
        )

        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(listStack),
                PaneRole.Supporting to PaneConfiguration(detailStack)
            ),
            activeRole = PaneRole.Supporting
        )

        // Then - list stack maintains its navigation history
        val listContent = panes.paneContent(PaneRole.Primary) as StackNode
        assertEquals(3, listContent.children.size)
        assertTrue(listContent.canGoBack)
    }

    // =========================================================================
    // PANE ACTIVE CONTENT TESTS
    // =========================================================================

    @Test
    fun `activePaneContent returns content of active pane`() {
        // Given
        val primaryContent = createScreen("primary-screen", "panes")
        val supportingContent = createScreen("supporting-screen", "panes")

        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activeRole = PaneRole.Supporting
        )

        // Then
        assertEquals(supportingContent, panes.activePaneContent)
    }

    @Test
    fun `paneContent returns correct content for each role`() {
        // Given
        val primaryContent = createScreen("primary", "panes")
        val supportingContent = createScreen("supporting", "panes")
        val extraContent = createScreen("extra", "panes")

        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent),
                PaneRole.Extra to PaneConfiguration(extraContent)
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertEquals(primaryContent, panes.paneContent(PaneRole.Primary))
        assertEquals(supportingContent, panes.paneContent(PaneRole.Supporting))
        assertEquals(extraContent, panes.paneContent(PaneRole.Extra))
    }

    @Test
    fun `paneContent returns null for unconfigured role`() {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        assertNull(panes.paneContent(PaneRole.Supporting))
        assertNull(panes.paneContent(PaneRole.Extra))
    }
}
