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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

private data class PaneContentTestResult(
    val role: PaneRole,
    val isVisible: Boolean
)

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
class PaneRendererTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val ListDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val DetailDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val ExtraDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = ListDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    fun createPanes(
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
    fun simulateBuildPaneContentList(
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

    // =========================================================================
    // EXPANDED MODE TESTS (Multi-pane visible)
    // =========================================================================

    test("expanded mode shows all panes without Hide strategy") {
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
        contents.size shouldBe 2
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeTrue()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeTrue()
    }

    test("expanded mode hides panes with Hide strategy") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeTrue()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeFalse()
    }

    test("expanded mode always shows Primary regardless of strategy") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeTrue()
    }

    // =========================================================================
    // COMPACT MODE TESTS (Single pane visible)
    // =========================================================================

    test("compact mode shows only active pane") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeFalse()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeTrue()
    }

    test("compact mode shows Primary when active") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeTrue()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeFalse()
    }

    test("compact mode ignores adapt strategy for visibility") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeFalse()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeTrue()
    }

    // =========================================================================
    // PANE SWITCHING TESTS
    // =========================================================================

    test("pane switching changes active pane role") {
        // Given
        val config = mapOf(
            PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes")),
            PaneRole.Supporting to PaneConfiguration(createScreen("supporting", "panes"))
        )

        val beforeSwitch = createPanes("panes", null, config, activeRole = PaneRole.Primary)
        val afterSwitch = createPanes("panes", null, config, activeRole = PaneRole.Supporting)

        // Then
        beforeSwitch.activePaneRole shouldBe PaneRole.Primary
        afterSwitch.activePaneRole shouldBe PaneRole.Supporting
    }

    test("pane switching preserves content in both panes") {
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
        panes.paneContent(PaneRole.Primary) shouldBe primaryContent
        panes.paneContent(PaneRole.Supporting) shouldBe supportingContent
    }

    // =========================================================================
    // PANE CONFIGURATION TESTS
    // =========================================================================

    test("pane returns correct adapt strategy per role") {
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
        panes.adaptStrategy(PaneRole.Primary) shouldBe AdaptStrategy.Hide
        panes.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate
        panes.adaptStrategy(PaneRole.Extra) shouldBe AdaptStrategy.Reflow
    }

    test("pane returns null adapt strategy for unconfigured role") {
        // Given - only Primary configured
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        panes.adaptStrategy(PaneRole.Primary).shouldNotBeNull()
        panes.adaptStrategy(PaneRole.Supporting).shouldBeNull()
        panes.adaptStrategy(PaneRole.Extra).shouldBeNull()
    }

    test("pane configured roles returns all configured roles") {
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
        panes.configuredRoles shouldBe setOf(PaneRole.Primary, PaneRole.Supporting)
        panes.paneCount shouldBe 2
    }

    // =========================================================================
    // PANE BACK BEHAVIOR TESTS
    // =========================================================================

    test("pane has default back behavior") {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        panes.backBehavior shouldBe PaneBackBehavior.PopUntilScaffoldValueChange
    }

    test("pane supports custom back behavior") {
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
        popLatestPanes.backBehavior shouldBe PaneBackBehavior.PopLatest
        popDestinationPanes.backBehavior shouldBe PaneBackBehavior.PopUntilCurrentDestinationChange
    }

    // =========================================================================
    // PANE WRAPPER TESTS
    // =========================================================================

    test("FakeNavRenderScope container registry has no pane wrapper by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry.hasPaneContainer("any-key").shouldBeFalse()
        scope.containerRegistry.hasPaneContainer("panes").shouldBeFalse()
    }

    test("container registry is empty by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry shouldBe ContainerRegistry.Empty
    }

    // =========================================================================
    // THREE-PANE LAYOUT TESTS
    // =========================================================================

    test("three pane layout has correct pane count") {
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
        panes.paneCount shouldBe 3
        panes.configuredRoles shouldBe setOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra)
    }

    test("three pane expanded mode visibility") {
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
        contents.find { it.role == PaneRole.Primary }!!.isVisible.shouldBeTrue()
        contents.find { it.role == PaneRole.Supporting }!!.isVisible.shouldBeFalse() // Hide strategy
        contents.find { it.role == PaneRole.Extra }!!.isVisible.shouldBeTrue()
    }

    // =========================================================================
    // PANE WITH NESTED STACK TESTS
    // =========================================================================

    test("pane can contain stack as content") {
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
        primaryContent.shouldBeInstanceOf<StackNode>()
        primaryContent.children.size shouldBe 2
    }

    test("pane nested stack maintains navigation state") {
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
        listContent.children.size shouldBe 3
        listContent.canGoBack.shouldBeTrue()
    }

    // =========================================================================
    // PANE ACTIVE CONTENT TESTS
    // =========================================================================

    test("activePaneContent returns content of active pane") {
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
        panes.activePaneContent shouldBe supportingContent
    }

    test("paneContent returns correct content for each role") {
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
        panes.paneContent(PaneRole.Primary) shouldBe primaryContent
        panes.paneContent(PaneRole.Supporting) shouldBe supportingContent
        panes.paneContent(PaneRole.Extra) shouldBe extraContent
    }

    test("paneContent returns null for unconfigured role") {
        // Given
        val panes = createPanes(
            "panes", null,
            mapOf(
                PaneRole.Primary to PaneConfiguration(createScreen("primary", "panes"))
            ),
            activeRole = PaneRole.Primary
        )

        // Then
        panes.paneContent(PaneRole.Supporting).shouldBeNull()
        panes.paneContent(PaneRole.Extra).shouldBeNull()
    }
})
