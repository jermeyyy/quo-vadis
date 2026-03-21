package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.navigationevent.NavigationEventInfo
import com.jermey.quo.vadis.core.compose.internal.navback.NoScreenInfo
import com.jermey.quo.vadis.core.compose.internal.navback.ScreenNavigationInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeInstanceOf

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
class ScreenNavigationInfoTest : FunSpec({

    // =========================================================================
    // SCREEN NAVIGATION INFO - CREATION TESTS
    // =========================================================================

    test("ScreenNavigationInfo creation with all parameters") {
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
        info.screenId shouldBe screenId
        info.displayName shouldBe displayName
        info.route shouldBe route
    }

    test("ScreenNavigationInfo creation with only screenId") {
        // When
        val info = ScreenNavigationInfo(screenId = "simple_screen")

        // Then
        info.screenId shouldBe "simple_screen"
        info.displayName.shouldBeNull()
        info.route.shouldBeNull()
    }

    test("ScreenNavigationInfo creation with screenId and displayName") {
        // When
        val info = ScreenNavigationInfo(
            screenId = "profile_screen",
            displayName = "Profile"
        )

        // Then
        info.screenId shouldBe "profile_screen"
        info.displayName shouldBe "Profile"
        info.route.shouldBeNull()
    }

    test("ScreenNavigationInfo creation with screenId and route") {
        // When
        val info = ScreenNavigationInfo(
            screenId = "settings_screen",
            route = "/settings"
        )

        // Then
        info.screenId shouldBe "settings_screen"
        info.displayName.shouldBeNull()
        info.route shouldBe "/settings"
    }

    // =========================================================================
    // SCREEN NAVIGATION INFO - INHERITANCE TESTS
    // =========================================================================

    test("ScreenNavigationInfo extends NavigationEventInfo") {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "test")

        // Then
        info.shouldBeInstanceOf<NavigationEventInfo>()
    }

    // =========================================================================
    // SCREEN NAVIGATION INFO - DATA CLASS BEHAVIOR TESTS
    // =========================================================================

    test("ScreenNavigationInfo equals works correctly") {
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
        info2 shouldBe info1
        info3 shouldNotBe info1
    }

    test("ScreenNavigationInfo copy works correctly") {
        // Given
        val original = ScreenNavigationInfo(
            screenId = "original",
            displayName = "Original",
            route = "/original"
        )

        // When
        val copied = original.copy(screenId = "copied")

        // Then
        copied.screenId shouldBe "copied"
        copied.displayName shouldBe "Original"
        copied.route shouldBe "/original"
    }

    test("ScreenNavigationInfo copy with all parameters") {
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
        copied.screenId shouldBe "new_id"
        copied.displayName shouldBe "New Name"
        copied.route shouldBe "/new/route"
    }

    test("ScreenNavigationInfo hashCode is consistent with equals") {
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
        info2.hashCode() shouldBe info1.hashCode()
    }

    // =========================================================================
    // NO SCREEN INFO - SINGLETON TESTS
    // =========================================================================

    test("NoScreenInfo is a data object") {
        // Given/When
        val noScreenInfo = NoScreenInfo

        // Then
        noScreenInfo.shouldBeInstanceOf<NavigationEventInfo>()
    }

    test("NoScreenInfo is singleton") {
        // Given
        val instance1 = NoScreenInfo
        val instance2 = NoScreenInfo

        // Then
        instance2 shouldBeSameInstanceAs instance1
    }

    test("NoScreenInfo equals itself") {
        // Given
        val instance1 = NoScreenInfo
        val instance2 = NoScreenInfo

        // Then
        instance2 shouldBe instance1
    }

    test("NoScreenInfo extends NavigationEventInfo") {
        // Given/When
        val noScreenInfo: NavigationEventInfo = NoScreenInfo

        // Then
        noScreenInfo.shouldBeInstanceOf<NavigationEventInfo>()
    }

    // =========================================================================
    // TYPE DISTINCTION TESTS
    // =========================================================================

    test("ScreenNavigationInfo and NoScreenInfo are distinct types") {
        // Given
        val screenInfo: NavigationEventInfo = ScreenNavigationInfo(screenId = "test")
        val noScreenInfo: NavigationEventInfo = NoScreenInfo

        // Then
        screenInfo.shouldBeInstanceOf<ScreenNavigationInfo>()
        noScreenInfo shouldBeSameInstanceAs NoScreenInfo
        noScreenInfo shouldNotBe screenInfo
    }

    test("when expression works with NavigationEventInfo subtypes") {
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
        screenResult shouldBe "screen: test"
        noScreenResult shouldBe "no screen"
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    test("ScreenNavigationInfo with empty screenId") {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "")

        // Then
        info.screenId shouldBe ""
    }

    test("ScreenNavigationInfo with empty displayName") {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            displayName = ""
        )

        // Then
        info.displayName shouldBe ""
    }

    test("ScreenNavigationInfo with empty route") {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            route = ""
        )

        // Then
        info.route shouldBe ""
    }

    test("ScreenNavigationInfo with special characters in screenId") {
        // Given/When
        val info = ScreenNavigationInfo(screenId = "screen-with_special.chars:123")

        // Then
        info.screenId shouldBe "screen-with_special.chars:123"
    }

    test("ScreenNavigationInfo with unicode in displayName") {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "test",
            displayName = "日本語スクリーン"
        )

        // Then
        info.displayName shouldBe "日本語スクリーン"
    }

    test("ScreenNavigationInfo with route parameters") {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "detail",
            route = "/users/{userId}/posts/{postId}"
        )

        // Then
        info.route shouldBe "/users/{userId}/posts/{postId}"
    }

    test("ScreenNavigationInfo with query parameters in route") {
        // Given/When
        val info = ScreenNavigationInfo(
            screenId = "search",
            route = "/search?q={query}&page={page}"
        )

        // Then
        info.route shouldBe "/search?q={query}&page={page}"
    }

    // =========================================================================
    // COLLECTION USAGE TESTS
    // =========================================================================

    test("ScreenNavigationInfo can be used in collections") {
        // Given
        val info1 = ScreenNavigationInfo(screenId = "screen1")
        val info2 = ScreenNavigationInfo(screenId = "screen2")
        val info3 = ScreenNavigationInfo(screenId = "screen1")

        // When
        val list = listOf(info1, info2, info3)
        val set = setOf(info1, info2, info3)

        // Then
        list.size shouldBe 3
        set.size shouldBe 2 // info1 and info3 are equal
    }

    test("ScreenNavigationInfo can be used as map key") {
        // Given
        val info1 = ScreenNavigationInfo(screenId = "screen1")
        val info2 = ScreenNavigationInfo(screenId = "screen2")

        // When
        val map = mapOf(
            info1 to "First Screen",
            info2 to "Second Screen"
        )

        // Then
        map[info1] shouldBe "First Screen"
        map[ScreenNavigationInfo(screenId = "screen1")] shouldBe "First Screen"
    }
})
