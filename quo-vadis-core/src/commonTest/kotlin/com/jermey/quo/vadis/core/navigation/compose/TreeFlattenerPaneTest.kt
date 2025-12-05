package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PaneNode flattening in TreeFlattener.
 *
 * Tests cover:
 * - Compact width → PANE_AS_STACK surface
 * - Compact width → previousSurfaceId for back navigation
 * - Expanded width → PANE_WRAPPER + PANE_CONTENT surfaces
 * - paneStructures populated with PaneRole + content
 * - parentWrapperId linking
 * - PANE_SWITCH animation pair generation
 * - Caching hints for both modes
 * - Medium width also produces multi-pane output
 */
class TreeFlattenerPaneTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object ListDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SupportingDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private val mockContentResolver = TreeFlattener.ContentResolver { {} }
    private val flattener = TreeFlattener(mockContentResolver)

    private fun mockScreen(key: String, parentKey: String? = null): ScreenNode {
        return ScreenNode(key, parentKey, ListDestination)
    }

    private fun createPaneNode(
        key: String = "panes",
        activePaneRole: PaneRole = PaneRole.Primary,
        paneCount: Int = 2
    ): PaneNode {
        val configs = mutableMapOf<PaneRole, PaneConfiguration>()

        // Always add Primary pane
        configs[PaneRole.Primary] = PaneConfiguration(
            content = mockScreen("primary-screen", key)
        )

        // Add Supporting pane if needed
        if (paneCount >= 2) {
            configs[PaneRole.Supporting] = PaneConfiguration(
                content = mockScreen("supporting-screen", key)
            )
        }

        // Add Extra pane if needed
        if (paneCount >= 3) {
            configs[PaneRole.Extra] = PaneConfiguration(
                content = mockScreen("extra-screen", key)
            )
        }

        return PaneNode(
            key = key,
            parentKey = null,
            paneConfigurations = configs,
            activePaneRole = activePaneRole
        )
    }

    // =========================================================================
    // COMPACT WIDTH (PANE_AS_STACK) TESTS
    // =========================================================================

    @Test
    fun `compact width produces PANE_AS_STACK surface`() {
        val pane = createPaneNode(activePaneRole = PaneRole.Supporting)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp) // Compact width
        )

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val paneSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }

        assertNotNull(paneSurface, "Expected PANE_AS_STACK surface to exist")
        assertEquals("panes-pane-supporting", paneSurface.id)
        assertEquals(SurfaceNodeType.PANE, paneSurface.nodeType)
    }

    @Test
    fun `compact width shows only active pane`() {
        val pane = createPaneNode(activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(pane, null, windowSizeClass)

        // Should have only one pane surface
        val paneSurfaces = result.surfaces.filter {
            it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK
        }
        assertEquals(1, paneSurfaces.size, "Expected exactly one PANE_AS_STACK surface")
        assertEquals("panes-pane-primary", paneSurfaces[0].id)
    }

    @Test
    fun `compact width tracks previousSurfaceId for back navigation`() {
        // Previous state: Primary pane active
        val previousPane = createPaneNode(activePaneRole = PaneRole.Primary)

        // Current state: Supporting pane active (pane switch)
        val currentPane = createPaneNode(activePaneRole = PaneRole.Supporting)

        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(currentPane, previousPane, windowSizeClass)

        val paneSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }

        assertNotNull(paneSurface)
        assertEquals("panes-pane-primary", paneSurface.previousSurfaceId,
            "Should reference previous pane surface for animation")
    }

    @Test
    fun `compact width generates PANE_SWITCH animation pair`() {
        val previousPane = createPaneNode(activePaneRole = PaneRole.Primary)
        val currentPane = createPaneNode(activePaneRole = PaneRole.Supporting)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(currentPane, previousPane, windowSizeClass)

        val animationPair = result.animationPairs.find { it.transitionType == TransitionType.PANE_SWITCH }

        assertNotNull(animationPair, "Expected AnimationPair with PANE_SWITCH type")
        assertEquals("panes-pane-supporting", animationPair.currentId)
        assertEquals("panes-pane-primary", animationPair.previousId)
    }

    @Test
    fun `compact width caching hints match stack behavior`() {
        val pane = createPaneNode(activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(pane, null, windowSizeClass)

        assertFalse(result.cachingHints.shouldCacheWrapper,
            "Stack-like mode should not cache wrapper")
        assertTrue(result.cachingHints.shouldCacheContent,
            "Should cache content")
        assertTrue(result.cachingHints.cacheableIds.contains("panes-pane-primary"),
            "Cacheable IDs should contain pane surface")
    }

    @Test
    fun `compact width marks previous pane as invalidated on switch`() {
        val previousPane = createPaneNode(activePaneRole = PaneRole.Primary)
        val currentPane = createPaneNode(activePaneRole = PaneRole.Supporting)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(currentPane, previousPane, windowSizeClass)

        assertTrue(result.cachingHints.invalidatedIds.contains("panes-pane-primary"),
            "Previous pane should be marked as invalidated")
    }

    // =========================================================================
    // EXPANDED WIDTH (MULTI-PANE) TESTS
    // =========================================================================

    @Test
    fun `expanded width produces PANE_WRAPPER and PANE_CONTENT surfaces`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }

        assertNotNull(wrapperSurface, "Expected PANE_WRAPPER surface")
        assertEquals(2, contentSurfaces.size, "Expected two PANE_CONTENT surfaces")
        assertEquals("panes-wrapper", wrapperSurface.id)
    }

    @Test
    fun `expanded width populates paneStructures`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }

        assertNotNull(wrapperSurface?.paneStructures, "Expected paneStructures to be populated")
        assertEquals(2, wrapperSurface?.paneStructures?.size)

        val roles = wrapperSurface?.paneStructures?.map { it.paneRole }
        assertTrue(roles?.contains(PaneRole.Primary) == true, "Should contain Primary role")
        assertTrue(roles?.contains(PaneRole.Supporting) == true, "Should contain Supporting role")
    }

    @Test
    fun `content surfaces have parentWrapperId pointing to wrapper`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }

        contentSurfaces.forEach { surface ->
            assertEquals("panes-wrapper", surface.parentWrapperId,
                "Content surface ${surface.id} should have wrapper as parent")
        }
    }

    @Test
    fun `content surfaces have correct IDs`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val contentIds = result.surfaces
            .filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }
            .map { it.id }

        assertTrue(contentIds.contains("panes-content-primary"), "Should contain primary content")
        assertTrue(contentIds.contains("panes-content-supporting"), "Should contain supporting content")
    }

    @Test
    fun `expanded width caching hints match tab behavior`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        assertTrue(result.cachingHints.wrapperIds.contains("panes-wrapper"),
            "Wrapper IDs should contain pane wrapper")
        assertTrue(result.cachingHints.contentIds.contains("panes-content-primary"),
            "Content IDs should contain primary pane")
        assertTrue(result.cachingHints.contentIds.contains("panes-content-supporting"),
            "Content IDs should contain supporting pane")
    }

    @Test
    fun `expanded width wrapper and content surfaces have correct nodeType`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }

        assertEquals(SurfaceNodeType.PANE, wrapperSurface?.nodeType)
        contentSurfaces.forEach { surface ->
            assertEquals(SurfaceNodeType.PANE, surface.nodeType)
        }
    }

    @Test
    fun `content surface z-order is higher than wrapper`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }

        assertNotNull(wrapperSurface)
        contentSurfaces.forEach { surface ->
            assertTrue(surface.zOrder > wrapperSurface.zOrder,
                "Content surface ${surface.id} z-order should be higher than wrapper")
        }
    }

    // =========================================================================
    // MEDIUM WIDTH TESTS
    // =========================================================================

    @Test
    fun `medium width also produces multi-pane output`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Medium

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }

        assertNotNull(wrapperSurface, "Medium width should produce multi-pane output")
    }

    @Test
    fun `medium width creates content surfaces for all panes`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(700.dp, 800.dp) // Medium width
        )

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }
        assertEquals(2, contentSurfaces.size, "Should have content surface for each pane")
    }

    // =========================================================================
    // CROSS-NODE NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `cross-node navigation caches whole wrapper in expanded mode`() {
        val screen = ScreenNode("screen1", null, ListDestination)
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        // Simulate navigation from ScreenNode to PaneNode (cross-node navigation)
        val result = flattener.flattenState(pane, screen, windowSizeClass)

        assertTrue(result.cachingHints.shouldCacheWrapper,
            "Cross-node navigation should cache wrapper")
        assertTrue(result.cachingHints.isCrossNodeTypeNavigation,
            "Should flag as cross-node navigation")
        assertTrue(result.cachingHints.cacheableIds.contains("panes-wrapper"),
            "Cacheable IDs should contain pane wrapper")
    }

    @Test
    fun `cross-node navigation generates animation pair for wrapper`() {
        val screen = ScreenNode("screen1", null, ListDestination)
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, screen, windowSizeClass)

        val crossNodePair = result.animationPairs.find {
            it.currentId == "panes-wrapper" && it.previousId == "screen1"
        }

        assertNotNull(crossNodePair, "Expected animation pair for cross-node navigation")
    }

    @Test
    fun `cross-node navigation in compact mode creates animation pair`() {
        val screen = ScreenNode("screen1", null, ListDestination)
        val pane = createPaneNode(activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(pane, screen, windowSizeClass)

        val paneSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }
        assertNotNull(paneSurface)

        // Should have animation pairing for the transition
        assertTrue(result.cachingHints.isCrossNodeTypeNavigation,
            "Should flag as cross-node navigation")
    }

    // =========================================================================
    // WRAPPER ANIMATION TESTS
    // =========================================================================

    @Test
    fun `wrapper surface has no previousSurfaceId for intra-pane navigation`() {
        val previousPane = createPaneNode(activePaneRole = PaneRole.Primary)
        val currentPane = createPaneNode(activePaneRole = PaneRole.Supporting)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(currentPane, previousPane, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }

        assertNotNull(wrapperSurface)
        assertNull(wrapperSurface.previousSurfaceId,
            "Wrapper should have no previousSurfaceId for intra-pane navigation")
    }

    @Test
    fun `wrapper surface has no animation for intra-pane navigation`() {
        val previousPane = createPaneNode(activePaneRole = PaneRole.Primary)
        val currentPane = createPaneNode(activePaneRole = PaneRole.Supporting)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(currentPane, previousPane, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }

        assertNotNull(wrapperSurface)
        assertEquals(SurfaceAnimationSpec.None, wrapperSurface.animationSpec,
            "Wrapper should have no animation for intra-pane navigation")
    }

    // =========================================================================
    // THREE PANE TESTS
    // =========================================================================

    @Test
    fun `three panes produce three content surfaces in expanded mode`() {
        val pane = createPaneNode(paneCount = 3)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }
        assertEquals(3, contentSurfaces.size, "Should have three PANE_CONTENT surfaces")

        val contentIds = contentSurfaces.map { it.id }
        assertTrue(contentIds.contains("panes-content-primary"))
        assertTrue(contentIds.contains("panes-content-supporting"))
        assertTrue(contentIds.contains("panes-content-extra"))
    }

    @Test
    fun `paneStructures contains all three roles`() {
        val pane = createPaneNode(paneCount = 3)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }

        assertNotNull(wrapperSurface?.paneStructures)
        assertEquals(3, wrapperSurface?.paneStructures?.size)

        val roles = wrapperSurface?.paneStructures?.map { it.paneRole }?.toSet()
        assertEquals(setOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra), roles)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `single pane produces stack surface in compact mode`() {
        val pane = createPaneNode(paneCount = 1, activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val paneSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }
        assertNotNull(paneSurface)
        assertEquals("panes-pane-primary", paneSurface.id)
    }

    @Test
    fun `single pane produces wrapper and content in expanded mode`() {
        val pane = createPaneNode(paneCount = 1, activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Expanded

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }

        assertNotNull(wrapperSurface)
        assertEquals(1, contentSurfaces.size)
    }

    @Test
    fun `no previous pane means no previousSurfaceId`() {
        val pane = createPaneNode(activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        // No previous root
        val result = flattener.flattenState(pane, null, windowSizeClass)

        val paneSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }

        assertNotNull(paneSurface)
        assertNull(paneSurface.previousSurfaceId,
            "No previousSurfaceId when there's no previous pane")
    }

    @Test
    fun `no PANE_SWITCH animation pair without pane switch`() {
        val pane = createPaneNode(activePaneRole = PaneRole.Primary)
        val windowSizeClass = WindowSizeClass.Compact

        val result = flattener.flattenState(pane, null, windowSizeClass)

        val paneSwitchPair = result.animationPairs.find { it.transitionType == TransitionType.PANE_SWITCH }
        assertNull(paneSwitchPair, "No PANE_SWITCH animation pair without pane switch")
    }

    // =========================================================================
    // WINDOW SIZE CLASS TESTS
    // =========================================================================

    @Test
    fun `windowSizeClass boundary at 600dp goes to medium`() {
        val pane = createPaneNode(paneCount = 2)

        // At exactly 600dp should be Medium
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))

        assertEquals(WindowWidthSizeClass.Medium, windowSizeClass.widthSizeClass)

        val result = flattener.flattenState(pane, null, windowSizeClass)
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        assertNotNull(wrapperSurface, "600dp width should produce multi-pane layout")
    }

    @Test
    fun `windowSizeClass boundary at 599dp stays compact`() {
        val pane = createPaneNode(paneCount = 2)

        // Just below 600dp should be Compact
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(599.dp, 800.dp))

        assertEquals(WindowWidthSizeClass.Compact, windowSizeClass.widthSizeClass)

        val result = flattener.flattenState(pane, null, windowSizeClass)
        val stackSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_AS_STACK }
        assertNotNull(stackSurface, "599dp width should produce stack-like layout")
    }
}
