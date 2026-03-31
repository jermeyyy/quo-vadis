@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull

// ─── Test Destinations ───────────────────────────────────────────────

private data object DestA : NavDestination

private data object DestB : NavDestination

private data class ParameterizedDest(val id: String) : NavDestination

// ─── Tests ───────────────────────────────────────────────────────────

/**
 * Tests for TabNode and PaneNode destination property.
 *
 * Covers:
 * - Creation with and without destination
 * - copy() preserving / changing destination
 * - equals() / hashCode() considering destination
 */
class NodeDestinationTest : FunSpec({

    // ═══════════════════════════════════════════════════════════════════
    // TabNode destination
    // ═══════════════════════════════════════════════════════════════════

    test("TabNode destination defaults to null") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
        )
        tab.destination.shouldBeNull()
    }

    test("TabNode created with destination holds it") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        tab.destination shouldBe DestA
    }

    test("TabNode created with parameterized destination holds it") {
        val dest = ParameterizedDest(id = "42")
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = dest,
        )
        tab.destination shouldBe dest
    }

    test("TabNode copy preserves destination by default") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val copied = tab.copy(activeStackIndex = 0)
        copied.destination shouldBe DestA
    }

    test("TabNode copy can change destination") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val copied = tab.copy(destination = DestB)
        copied.destination shouldBe DestB
    }

    test("TabNode copy can clear destination to null") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val copied = tab.copy(destination = null)
        copied.destination.shouldBeNull()
    }

    test("TabNode equals considers destination") {
        val base = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val same = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        base shouldBe same
    }

    test("TabNode with different destinations are not equal") {
        val a = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val b = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestB,
        )
        a shouldNotBe b
    }

    test("TabNode with destination vs null destination are not equal") {
        val withDest = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val withoutDest = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
        )
        withDest shouldNotBe withoutDest
    }

    test("TabNode hashCode differs when destination differs") {
        val a = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val b = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestB,
        )
        a.hashCode() shouldNotBe b.hashCode()
    }

    test("TabNode hashCode consistent for equal instances") {
        val a = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val b = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        a.hashCode() shouldBe b.hashCode()
    }

    test("TabNode toString includes destination") {
        val tab = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("s0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0,
            destination = DestA,
        )
        val str = tab.toString()
        str.contains("destination=DestA") shouldBe true
    }

    // ═══════════════════════════════════════════════════════════════════
    // PaneNode destination
    // ═══════════════════════════════════════════════════════════════════

    test("PaneNode destination defaults to null") {
        val pane = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
        )
        pane.destination.shouldBeNull()
    }

    test("PaneNode created with destination holds it") {
        val pane = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestB,
        )
        pane.destination shouldBe DestB
    }

    test("PaneNode copy preserves destination by default") {
        val pane = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestA,
        )
        val copied = pane.copy(activePaneRole = PaneRole.Primary)
        copied.destination shouldBe DestA
    }

    test("PaneNode copy can change destination") {
        val pane = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestA,
        )
        val copied = pane.copy(destination = DestB)
        copied.destination shouldBe DestB
    }

    test("PaneNode with different destinations are not equal") {
        val a = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestA,
        )
        val b = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestB,
        )
        a shouldNotBe b
    }

    test("PaneNode hashCode differs when destination differs") {
        val a = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestA,
        )
        val b = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("p1"), NodeKey("panes"), DestA),
                ),
            ),
            activePaneRole = PaneRole.Primary,
            destination = DestB,
        )
        a.hashCode() shouldNotBe b.hashCode()
    }
})
