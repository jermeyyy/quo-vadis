package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.navigationevent.NavigationEventInfo
import com.jermey.quo.vadis.core.compose.navback.NoScreenInfo
import com.jermey.quo.vadis.core.compose.navback.ScreenNavigationInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [ScreenNavigationInfo] and [NoScreenInfo].
 *
 * Tests cover:
 * - ScreenNavigationInfo creation with all parameters
 * - ScreenNavigationInfo creation with default values
 * - NoScreenInfo object singleton behavior
 * - NavigationEventInfo inheritance
 * - Data class equality and copy behavior
 */
class ScreenNavigationInfoTest {

    // =========================================================================
    // SCREEN NAVIGATION INFO - CREATION TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo creation with all parameters`() {
        // Given
        val screenId = "home_screen"
        val displayName = "Home"
        val route = "/home"

        // When
        val info = ScreenNavigationInfo(
            screenId = screenId,
            displayName = displayName,
            route = route
        )

        // Then
        assertEquals(screenId, info.screenId)
        assertEquals(displayName, info.displayName)
        assertEquals(route, info.route)
    }

    @Test
    fun `ScreenNavigationInfo creation with only screenId`() {
        // When
        val info = ScreenNavigationInfo(screenId = "simple_screen")

        // Then
        assertEquals("simple_screen", info.screenId)
        assertNull(info.displayName)
        assertNull(info.route)
    }

    @Test
    fun `ScreenNavigationInfo creation with screenId and displayName`() {
        // When
        val info = ScreenNavigationInfo(
            screenId = "profile_screen",
            displayName = "Profile"
        )

        // Then
        assertEquals("profile_screen", info.screenId)
        assertEquals("Profile", info.displayName)
        assertNull(info.route)
    }

    @Test
    fun `ScreenNavigationInfo creation with screenId and route`() {
        // When
        val info = ScreenNavigationInfo(
            screenId = "settings_screen",
            route = "/settings"
        )

        // Then
        assertEquals("settings_screen", info.screenId)
        assertNull(info.displayName)
        assertEquals("/settings", info.route)
    }

    // =========================================================================
    // SCREEN NAVIGATION INFO - INHERITANCE TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo extends NavigationEventInfo`() {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "test")

        // Then
        assertIs<NavigationEventInfo>(info)
    }

    // =========================================================================
    // SCREEN NAVIGATION INFO - DATA CLASS BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo equals works correctly`() {
        // Given
        val info1 = ScreenNavigationInfo(
            screenId = "screen",
            displayName = "Screen",
            route = "/screen"
        )
        val info2 = ScreenNavigationInfo(
            screenId = "screen",
            displayName = "Screen",
            route = "/screen"
        )
        val info3 = ScreenNavigationInfo(
            screenId = "different",
            displayName = "Screen",
            route = "/screen"
        )

        // Then
        assertEquals(info1, info2)
        assertNotEquals(info1, info3)
    }

    @Test
    fun `ScreenNavigationInfo copy works correctly`() {
        // Given
        val original = ScreenNavigationInfo(
            screenId = "original",
            displayName = "Original",
            route = "/original"
        )

        // When
        val copied = original.copy(screenId = "copied")

        // Then
        assertEquals("copied", copied.screenId)
        assertEquals("Original", copied.displayName)
        assertEquals("/original", copied.route)
    }

    @Test
    fun `ScreenNavigationInfo copy with all parameters`() {
        // Given
        val original = ScreenNavigationInfo(
            screenId = "original",
            displayName = "Original",
            route = "/original"
        )

        // When
        val copied = original.copy(
            screenId = "new_id",
            displayName = "New Name",
            route = "/new/route"
        )

        // Then
        assertEquals("new_id", copied.screenId)
        assertEquals("New Name", copied.displayName)
        assertEquals("/new/route", copied.route)
    }

    @Test
    fun `ScreenNavigationInfo hashCode is consistent with equals`() {
        // Given
        val info1 = ScreenNavigationInfo(
            screenId = "screen",
            displayName = "Screen",
            route = "/screen"
        )
        val info2 = ScreenNavigationInfo(
            screenId = "screen",
            displayName = "Screen",
            route = "/screen"
        )

        // Then
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    // =========================================================================
    // NO SCREEN INFO - SINGLETON TESTS
    // =========================================================================

    @Test
    fun `NoScreenInfo is a data object`() {
        // Given/When
        val noScreenInfo = NoScreenInfo

        // Then
        assertIs<NavigationEventInfo>(noScreenInfo)
    }

    @Test
    fun `NoScreenInfo is singleton`() {
        // Given
        val instance1 = NoScreenInfo
        val instance2 = NoScreenInfo

        // Then
        assertSame(instance1, instance2)
    }

    @Test
    fun `NoScreenInfo equals itself`() {
        // Given
        val instance1 = NoScreenInfo
        val instance2 = NoScreenInfo

        // Then
        assertEquals(instance1, instance2)
    }

    @Test
    fun `NoScreenInfo extends NavigationEventInfo`() {
        // Given/When
        val noScreenInfo: NavigationEventInfo = NoScreenInfo

        // Then
        assertIs<NavigationEventInfo>(noScreenInfo)
    }

    // =========================================================================
    // TYPE DISTINCTION TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo and NoScreenInfo are distinct types`() {
        // Given
        val screenInfo: NavigationEventInfo = ScreenNavigationInfo(screenId = "test")
        val noScreenInfo: NavigationEventInfo = NoScreenInfo

        // Then
        assertIs<ScreenNavigationInfo>(screenInfo)
        assertSame(NoScreenInfo, noScreenInfo)
        assertNotEquals(screenInfo, noScreenInfo)
    }

    @Test
    fun `when expression works with NavigationEventInfo subtypes`() {
        // Given
        val screenInfo: NavigationEventInfo = ScreenNavigationInfo(screenId = "test")
        val noScreenInfo: NavigationEventInfo = NoScreenInfo

        // When
        val screenResult = when (screenInfo) {
            is ScreenNavigationInfo -> "screen: ${screenInfo.screenId}"
            is NoScreenInfo -> "no screen"
            else -> "unknown"
        }

        val noScreenResult = when (noScreenInfo) {
            is ScreenNavigationInfo -> "screen: ${noScreenInfo.screenId}"
            is NoScreenInfo -> "no screen"
            else -> "unknown"
        }

        // Then
        assertEquals("screen: test", screenResult)
        assertEquals("no screen", noScreenResult)
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo with empty screenId`() {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "")

        // Then
        assertEquals("", info.screenId)
    }

    @Test
    fun `ScreenNavigationInfo with empty displayName`() {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            displayName = ""
        )

        // Then
        assertEquals("", info.displayName)
    }

    @Test
    fun `ScreenNavigationInfo with empty route`() {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            route = ""
        )

        // Then
        assertEquals("", info.route)
    }

    @Test
    fun `ScreenNavigationInfo with special characters in screenId`() {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "screen-with_special.chars:123")

        // Then
        assertEquals("screen-with_special.chars:123", info.screenId)
    }

    @Test
    fun `ScreenNavigationInfo with unicode in displayName`() {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            displayName = "日本語スクリーン"
        )

        // Then
        assertEquals("日本語スクリーン", info.displayName)
    }

    @Test
    fun `ScreenNavigationInfo with route parameters`() {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "detail",
            route = "/users/{userId}/posts/{postId}"
        )

        // Then
        assertEquals("/users/{userId}/posts/{postId}", info.route)
    }

    @Test
    fun `ScreenNavigationInfo with query parameters in route`() {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "search",
            route = "/search?q={query}&page={page}"
        )

        // Then
        assertEquals("/search?q={query}&page={page}", info.route)
    }

    // =========================================================================
    // COLLECTION USAGE TESTS
    // =========================================================================

    @Test
    fun `ScreenNavigationInfo can be used in collections`() {
        // Given
        val info1 = ScreenNavigationInfo(screenId = "screen1")
        val info2 = ScreenNavigationInfo(screenId = "screen2")
        val info3 = ScreenNavigationInfo(screenId = "screen1")

        // When
        val list = listOf(info1, info2, info3)
        val set = setOf(info1, info2, info3)

        // Then
        assertEquals(3, list.size)
        assertEquals(2, set.size) // info1 and info3 are equal
    }

    @Test
    fun `ScreenNavigationInfo can be used as map key`() {
        // Given
        val info1 = ScreenNavigationInfo(screenId = "screen1")
        val info2 = ScreenNavigationInfo(screenId = "screen2")

        // When
        val map = mapOf(
            info1 to "First Screen",
            info2 to "Second Screen"
        )

        // Then
        assertEquals("First Screen", map[info1])
        assertEquals("First Screen", map[ScreenNavigationInfo(screenId = "screen1")])
    }
}
