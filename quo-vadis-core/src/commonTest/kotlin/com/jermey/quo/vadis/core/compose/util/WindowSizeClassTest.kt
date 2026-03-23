@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.util

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WindowSizeClassTest : FunSpec({

    // =========================================================================
    // WindowWidthSizeClass.fromWidth
    // =========================================================================

    test("width below 600dp is Compact") {
        WindowWidthSizeClass.fromWidth(0.dp) shouldBe WindowWidthSizeClass.Compact
        WindowWidthSizeClass.fromWidth(320.dp) shouldBe WindowWidthSizeClass.Compact
        WindowWidthSizeClass.fromWidth(599.dp) shouldBe WindowWidthSizeClass.Compact
    }

    test("width at 600dp is Medium") {
        WindowWidthSizeClass.fromWidth(600.dp) shouldBe WindowWidthSizeClass.Medium
    }

    test("width between 600dp and 840dp is Medium") {
        WindowWidthSizeClass.fromWidth(700.dp) shouldBe WindowWidthSizeClass.Medium
        WindowWidthSizeClass.fromWidth(839.dp) shouldBe WindowWidthSizeClass.Medium
    }

    test("width at 840dp is Expanded") {
        WindowWidthSizeClass.fromWidth(840.dp) shouldBe WindowWidthSizeClass.Expanded
    }

    test("width above 840dp is Expanded") {
        WindowWidthSizeClass.fromWidth(1024.dp) shouldBe WindowWidthSizeClass.Expanded
        WindowWidthSizeClass.fromWidth(1920.dp) shouldBe WindowWidthSizeClass.Expanded
    }

    // =========================================================================
    // WindowHeightSizeClass.fromHeight
    // =========================================================================

    test("height below 480dp is Compact") {
        WindowHeightSizeClass.fromHeight(0.dp) shouldBe WindowHeightSizeClass.Compact
        WindowHeightSizeClass.fromHeight(320.dp) shouldBe WindowHeightSizeClass.Compact
        WindowHeightSizeClass.fromHeight(479.dp) shouldBe WindowHeightSizeClass.Compact
    }

    test("height at 480dp is Medium") {
        WindowHeightSizeClass.fromHeight(480.dp) shouldBe WindowHeightSizeClass.Medium
    }

    test("height between 480dp and 900dp is Medium") {
        WindowHeightSizeClass.fromHeight(600.dp) shouldBe WindowHeightSizeClass.Medium
        WindowHeightSizeClass.fromHeight(899.dp) shouldBe WindowHeightSizeClass.Medium
    }

    test("height at 900dp is Expanded") {
        WindowHeightSizeClass.fromHeight(900.dp) shouldBe WindowHeightSizeClass.Expanded
    }

    test("height above 900dp is Expanded") {
        WindowHeightSizeClass.fromHeight(1080.dp) shouldBe WindowHeightSizeClass.Expanded
    }

    // =========================================================================
    // WindowWidthSizeClass companion constants
    // =========================================================================

    test("CompactMaxWidth is 600dp") {
        WindowWidthSizeClass.CompactMaxWidth shouldBe 600.dp
    }

    test("MediumMaxWidth is 840dp") {
        WindowWidthSizeClass.MediumMaxWidth shouldBe 840.dp
    }

    // =========================================================================
    // WindowHeightSizeClass companion constants
    // =========================================================================

    test("CompactMaxHeight is 480dp") {
        WindowHeightSizeClass.CompactMaxHeight shouldBe 480.dp
    }

    test("MediumMaxHeight is 900dp") {
        WindowHeightSizeClass.MediumMaxHeight shouldBe 900.dp
    }

    // =========================================================================
    // WindowSizeClass.calculateFromSize(DpSize)
    // =========================================================================

    test("calculateFromSize with compact dimensions") {
        val result = WindowSizeClass.calculateFromSize(DpSize(320.dp, 480.dp))
        result.widthSizeClass shouldBe WindowWidthSizeClass.Compact
        result.heightSizeClass shouldBe WindowHeightSizeClass.Medium
    }

    test("calculateFromSize with medium dimensions") {
        val result = WindowSizeClass.calculateFromSize(DpSize(700.dp, 600.dp))
        result.widthSizeClass shouldBe WindowWidthSizeClass.Medium
        result.heightSizeClass shouldBe WindowHeightSizeClass.Medium
    }

    test("calculateFromSize with expanded dimensions") {
        val result = WindowSizeClass.calculateFromSize(DpSize(1920.dp, 1080.dp))
        result.widthSizeClass shouldBe WindowWidthSizeClass.Expanded
        result.heightSizeClass shouldBe WindowHeightSizeClass.Expanded
    }

    // =========================================================================
    // WindowSizeClass.calculateFromSize(Dp, Dp)
    // =========================================================================

    test("calculateFromSize with width and height params") {
        val result = WindowSizeClass.calculateFromSize(800.dp, 1200.dp)
        result.widthSizeClass shouldBe WindowWidthSizeClass.Medium
        result.heightSizeClass shouldBe WindowHeightSizeClass.Expanded
    }

    test("calculateFromSize with compact width and compact height") {
        val result = WindowSizeClass.calculateFromSize(400.dp, 300.dp)
        result.widthSizeClass shouldBe WindowWidthSizeClass.Compact
        result.heightSizeClass shouldBe WindowHeightSizeClass.Compact
    }

    // =========================================================================
    // Companion constants
    // =========================================================================

    test("Compact constant has compact width and medium height") {
        WindowSizeClass.Compact.widthSizeClass shouldBe WindowWidthSizeClass.Compact
        WindowSizeClass.Compact.heightSizeClass shouldBe WindowHeightSizeClass.Medium
    }

    test("Medium constant has medium width and medium height") {
        WindowSizeClass.Medium.widthSizeClass shouldBe WindowWidthSizeClass.Medium
        WindowSizeClass.Medium.heightSizeClass shouldBe WindowHeightSizeClass.Medium
    }

    test("Expanded constant has expanded width and expanded height") {
        WindowSizeClass.Expanded.widthSizeClass shouldBe WindowWidthSizeClass.Expanded
        WindowSizeClass.Expanded.heightSizeClass shouldBe WindowHeightSizeClass.Expanded
    }

    // =========================================================================
    // Boolean convenience properties
    // =========================================================================

    test("isCompactWidth returns true for Compact width") {
        WindowSizeClass.Compact.isCompactWidth.shouldBeTrue()
    }

    test("isCompactWidth returns false for Medium width") {
        WindowSizeClass.Medium.isCompactWidth.shouldBeFalse()
    }

    test("isCompactWidth returns false for Expanded width") {
        WindowSizeClass.Expanded.isCompactWidth.shouldBeFalse()
    }

    test("isAtLeastMediumWidth returns false for Compact") {
        WindowSizeClass.Compact.isAtLeastMediumWidth.shouldBeFalse()
    }

    test("isAtLeastMediumWidth returns true for Medium") {
        WindowSizeClass.Medium.isAtLeastMediumWidth.shouldBeTrue()
    }

    test("isAtLeastMediumWidth returns true for Expanded") {
        WindowSizeClass.Expanded.isAtLeastMediumWidth.shouldBeTrue()
    }

    test("isExpandedWidth returns false for Compact") {
        WindowSizeClass.Compact.isExpandedWidth.shouldBeFalse()
    }

    test("isExpandedWidth returns false for Medium") {
        WindowSizeClass.Medium.isExpandedWidth.shouldBeFalse()
    }

    test("isExpandedWidth returns true for Expanded") {
        WindowSizeClass.Expanded.isExpandedWidth.shouldBeTrue()
    }

    // =========================================================================
    // Data class equality
    // =========================================================================

    test("WindowSizeClass equality works correctly") {
        val a = WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium)
        val b = WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium)
        a shouldBe b
    }

    test("WindowSizeClass inequality on different width") {
        val a = WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium)
        val b = WindowSizeClass(WindowWidthSizeClass.Expanded, WindowHeightSizeClass.Medium)
        a shouldNotBe b
    }

    test("WindowSizeClass inequality on different height") {
        val a = WindowSizeClass(WindowWidthSizeClass.Medium, WindowHeightSizeClass.Compact)
        val b = WindowSizeClass(WindowWidthSizeClass.Medium, WindowHeightSizeClass.Expanded)
        a shouldNotBe b
    }
})
