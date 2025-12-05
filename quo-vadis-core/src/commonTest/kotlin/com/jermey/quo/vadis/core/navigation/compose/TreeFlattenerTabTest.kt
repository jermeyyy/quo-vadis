package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TabNode flattening in TreeFlattener.
 *
 * Tests cover:
 * - TAB_WRAPPER and TAB_CONTENT surface creation
 * - Parent-child linking via parentWrapperId
 * - Tab switch animation pairing via previousSurfaceId
 * - AnimationPair generation for tab switches
 * - Caching strategy (intra-tab vs cross-node navigation)
 * - CachingHints wrapperIds and contentIds population
 */
class TreeFlattenerTabTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private val mockContentResolver = TreeFlattener.ContentResolver { {} }
    private val flattener = TreeFlattener(mockContentResolver)

    private fun mockScreen(key: String, parentKey: String? = null): ScreenNode {
        return ScreenNode(key, parentKey, HomeDestination)
    }

    // =========================================================================
    // TAB WRAPPER AND CONTENT SURFACE TESTS
    // =========================================================================

    @Test
    fun `flattenTab produces wrapper and content surfaces`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))
        val tab = TabNode("tabs", null, listOf(stack1, stack2), activeStackIndex = 1)

        val result = flattener.flattenState(tab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(wrapperSurface, "Expected TAB_WRAPPER surface to exist")
        assertNotNull(contentSurface, "Expected TAB_CONTENT surface to exist")
        assertEquals("tabs-wrapper", wrapperSurface.id)
        assertEquals("tabs-content-1", contentSurface.id)
    }

    @Test
    fun `wrapper and content surfaces have correct nodeType`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertEquals(SurfaceNodeType.TAB, wrapperSurface?.nodeType)
        assertEquals(SurfaceNodeType.TAB, contentSurface?.nodeType)
    }

    @Test
    fun `content surface has parentWrapperId pointing to wrapper`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(contentSurface, "Expected TAB_CONTENT surface to exist")
        assertEquals("tabs-wrapper", contentSurface.parentWrapperId)
    }

    @Test
    fun `content surface z-order is higher than wrapper`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(wrapperSurface)
        assertNotNull(contentSurface)
        assertTrue(contentSurface.zOrder > wrapperSurface.zOrder)
    }

    // =========================================================================
    // TAB SWITCH ANIMATION TESTS
    // =========================================================================

    @Test
    fun `tab switch populates previousSurfaceId for animation`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        // Previous state: tab 0 active
        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        // Current state: tab 1 active
        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(contentSurface, "Expected TAB_CONTENT surface to exist")
        assertEquals("tabs-content-0", contentSurface.previousSurfaceId,
            "Content surface should reference previous tab's content surface")
    }

    @Test
    fun `tab switch generates AnimationPair with TAB_SWITCH type`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        // Previous state: tab 0 active
        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        // Current state: tab 1 active
        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        val animationPair = result.animationPairs.find { it.transitionType == TransitionType.TAB_SWITCH }

        assertNotNull(animationPair, "Expected AnimationPair with TAB_SWITCH type")
        assertEquals("tabs-content-1", animationPair.currentId)
        assertEquals("tabs-content-0", animationPair.previousId)
    }

    @Test
    fun `no previousSurfaceId when tab index unchanged`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        // Same tab state - no previous root
        val result = flattener.flattenState(tab)

        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(contentSurface)
        assertNull(contentSurface.previousSurfaceId,
            "No previousSurfaceId when there's no tab switch")
    }

    @Test
    fun `no TAB_SWITCH animation pair when tab index unchanged`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val tabSwitchPair = result.animationPairs.find { it.transitionType == TransitionType.TAB_SWITCH }
        assertNull(tabSwitchPair, "No TAB_SWITCH animation pair without tab switch")
    }

    // =========================================================================
    // CACHING STRATEGY TESTS
    // =========================================================================

    @Test
    fun `intra-tab navigation only caches content not wrapper`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        // Previous state: tab 0 active
        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        // Current state: tab 1 active (intra-tab navigation = tab switch)
        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        // Intra-tab navigation should not cache wrapper
        assertFalse(result.cachingHints.shouldCacheWrapper,
            "Intra-tab navigation should not cache wrapper")
        assertTrue(result.cachingHints.shouldCacheContent,
            "Should cache content")
        assertTrue(result.cachingHints.contentIds.contains("tabs-content-1"),
            "Content IDs should contain active tab content")
    }

    @Test
    fun `cross-node navigation caches whole wrapper`() {
        val screen = ScreenNode("screen1", null, HomeDestination)
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        // Simulate navigation from ScreenNode to TabNode (cross-node navigation)
        val result = flattener.flattenState(tab, previousRoot = screen)

        assertTrue(result.cachingHints.shouldCacheWrapper,
            "Cross-node navigation should cache wrapper")
        assertTrue(result.cachingHints.isCrossNodeTypeNavigation,
            "Should flag as cross-node navigation")
        assertTrue(result.cachingHints.wrapperIds.contains("tabs-wrapper"),
            "Wrapper IDs should contain tab wrapper")
    }

    @Test
    fun `CachingHints contains separate wrapper and content IDs`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        assertTrue(result.cachingHints.wrapperIds.isNotEmpty(),
            "Wrapper IDs should not be empty")
        assertTrue(result.cachingHints.contentIds.isNotEmpty(),
            "Content IDs should not be empty")

        // Wrapper and content IDs should not overlap
        val overlap = result.cachingHints.wrapperIds.intersect(result.cachingHints.contentIds)
        assertTrue(overlap.isEmpty(),
            "Wrapper and content IDs should not overlap")
    }

    @Test
    fun `previous tab content is marked as invalidated on tab switch`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        assertTrue(result.cachingHints.invalidatedIds.contains("tabs-content-0"),
            "Previous tab content should be marked as invalidated")
    }

    // =========================================================================
    // WRAPPER ANIMATION TESTS
    // =========================================================================

    @Test
    fun `wrapper surface has no animation for intra-tab navigation`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }

        assertNotNull(wrapperSurface)
        assertEquals(SurfaceAnimationSpec.None, wrapperSurface.animationSpec,
            "Wrapper should have no animation for intra-tab navigation")
    }

    @Test
    fun `wrapper surface has no previousSurfaceId for intra-tab navigation`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))

        val previousTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )

        val currentTab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1
        )

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }

        assertNotNull(wrapperSurface)
        assertNull(wrapperSurface.previousSurfaceId,
            "Wrapper should have no previousSurfaceId for intra-tab navigation")
    }

    // =========================================================================
    // NESTED CONTENT TESTS
    // =========================================================================

    @Test
    fun `nested stack content is properly flattened`() {
        val screen1 = mockScreen("screen1", "home-stack")
        val screen2 = mockScreen("screen2", "home-stack")
        val stack = StackNode("home-stack", "tabs", listOf(screen1, screen2))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        // Should have wrapper + content + nested screen
        assertTrue(result.surfaces.size >= 3,
            "Should have at least wrapper, content, and screen surfaces")

        val screenSurface = result.surfaces.find { it.id == "screen2" }
        assertNotNull(screenSurface, "Active screen should be flattened")
        assertEquals(SurfaceRenderingMode.STACK_CONTENT, screenSurface.renderingMode)
    }

    @Test
    fun `nested screen has correct parentWrapperId chain`() {
        val screen = mockScreen("screen", "home-stack")
        val stack = StackNode("home-stack", "tabs", listOf(screen))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val screenSurface = result.surfaces.find { it.id == "screen" }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(screenSurface)
        assertNotNull(contentSurface)
        // Screen should have content surface as parent
        assertEquals(contentSurface.id, screenSurface.parentWrapperId)
    }

    // =========================================================================
    // MULTIPLE TABS TESTS
    // =========================================================================

    @Test
    fun `multiple stacks create correct content surface ID`() {
        val stack0 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack1 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))
        val stack2 = StackNode("settings-stack", "tabs", listOf(mockScreen("settings", "settings-stack")))
        val tab = TabNode("tabs", null, listOf(stack0, stack1, stack2), activeStackIndex = 2)

        val result = flattener.flattenState(tab)

        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(contentSurface)
        assertEquals("tabs-content-2", contentSurface.id,
            "Content surface ID should reflect active tab index")
    }

    @Test
    fun `switching from last to first tab works correctly`() {
        val stack0 = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val stack1 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile", "profile-stack")))
        val stack2 = StackNode("settings-stack", "tabs", listOf(mockScreen("settings", "settings-stack")))

        // Previous: last tab active
        val previousTab = TabNode("tabs", null, listOf(stack0, stack1, stack2), activeStackIndex = 2)

        // Current: first tab active
        val currentTab = TabNode("tabs", null, listOf(stack0, stack1, stack2), activeStackIndex = 0)

        val result = flattener.flattenState(currentTab, previousRoot = previousTab)

        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }
        assertNotNull(contentSurface)
        assertEquals("tabs-content-0", contentSurface.id)
        assertEquals("tabs-content-2", contentSurface.previousSurfaceId)

        val animationPair = result.animationPairs.find { it.transitionType == TransitionType.TAB_SWITCH }
        assertNotNull(animationPair)
        assertEquals("tabs-content-0", animationPair.currentId)
        assertEquals("tabs-content-2", animationPair.previousId)
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `single tab produces wrapper and content without animation`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)

        val result = flattener.flattenState(tab)

        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(wrapperSurface)
        assertNotNull(contentSurface)
        assertEquals(SurfaceAnimationSpec.None, wrapperSurface.animationSpec)
        assertNull(contentSurface.previousSurfaceId)
    }

    @Test
    fun `empty stack in tab still creates surfaces`() {
        // Empty stack is technically invalid per TabNode requirements,
        // but the StackNode itself doesn't require children
        val emptyStack = StackNode("empty-stack", "tabs", emptyList())
        val populatedStack = StackNode("home-stack", "tabs", listOf(mockScreen("home", "home-stack")))

        // Use populated stack to satisfy TabNode requirements
        val tab = TabNode("tabs", null, listOf(populatedStack, emptyStack), activeStackIndex = 1)

        val result = flattener.flattenState(tab)

        // Should still create wrapper and content surfaces
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }

        assertNotNull(wrapperSurface)
        assertNotNull(contentSurface)
        assertEquals("tabs-content-1", contentSurface.id)
    }
}
