package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Classification of window width into semantic size categories.
 *
 * Based on Material Design 3 breakpoints:
 * - [Compact]: < 600dp - phones in portrait, small tablets
 * - [Medium]: 600dp - 840dp - tablets, foldables, large phones in landscape
 * - [Expanded]: > 840dp - large tablets, desktops
 *
 * @see <a href="https://m3.material.io/foundations/layout/applying-layout/window-size-classes">Material Design Window Size Classes</a>
 */
public enum class WindowWidthSizeClass {
    /**
     * Compact width class for screens < 600dp wide.
     *
     * Typical devices: phones in portrait orientation, small tablets.
     * Recommended layout: Single-column, sequential navigation.
     */
    Compact,

    /**
     * Medium width class for screens 600dp - 840dp wide.
     *
     * Typical devices: tablets, foldables, large phones in landscape.
     * Recommended layout: Two-column layouts, list-detail patterns.
     */
    Medium,

    /**
     * Expanded width class for screens > 840dp wide.
     *
     * Typical devices: large tablets, desktops, laptops.
     * Recommended layout: Multi-column layouts, all panes visible.
     */
    Expanded;

    public companion object {
        /**
         * Breakpoint between Compact and Medium (600dp).
         */
        public val CompactMaxWidth: Dp = 600.dp

        /**
         * Breakpoint between Medium and Expanded (840dp).
         */
        public val MediumMaxWidth: Dp = 840.dp

        /**
         * Calculates the width size class from a width value.
         *
         * @param width The window width in Dp
         * @return The corresponding [WindowWidthSizeClass]
         */
        public fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < CompactMaxWidth -> Compact
            width < MediumMaxWidth -> Medium
            else -> Expanded
        }
    }
}

/**
 * Classification of window height into semantic size categories.
 *
 * Based on Material Design 3 breakpoints:
 * - [Compact]: < 480dp - phones in landscape
 * - [Medium]: 480dp - 900dp - tablets, phones in portrait
 * - [Expanded]: > 900dp - large tablets, desktops
 */
public enum class WindowHeightSizeClass {
    /**
     * Compact height class for screens < 480dp tall.
     *
     * Typical devices: phones in landscape orientation.
     * Recommended layout: Minimize vertical chrome, prioritize content.
     */
    Compact,

    /**
     * Medium height class for screens 480dp - 900dp tall.
     *
     * Typical devices: tablets, phones in portrait.
     * Recommended layout: Standard layouts with navigation elements.
     */
    Medium,

    /**
     * Expanded height class for screens > 900dp tall.
     *
     * Typical devices: large tablets, desktops.
     * Recommended layout: Full layouts with all navigation visible.
     */
    Expanded;

    public companion object {
        /**
         * Breakpoint between Compact and Medium (480dp).
         */
        public val CompactMaxHeight: Dp = 480.dp

        /**
         * Breakpoint between Medium and Expanded (900dp).
         */
        public val MediumMaxHeight: Dp = 900.dp

        /**
         * Calculates the height size class from a height value.
         *
         * @param height The window height in Dp
         * @return The corresponding [WindowHeightSizeClass]
         */
        public fun fromHeight(height: Dp): WindowHeightSizeClass = when {
            height < CompactMaxHeight -> Compact
            height < MediumMaxHeight -> Medium
            else -> Expanded
        }
    }
}

/**
 * Represents the semantic classification of a window's dimensions.
 *
 * This class combines width and height size classes to provide a complete
 * picture of the available screen real estate. It's used by the rendering
 * system to make adaptive layout decisions.
 *
 * ## Usage with PaneNode
 *
 * The [widthSizeClass] determines how [PaneNode] renders:
 * - [WindowWidthSizeClass.Compact] → Single pane (stack-like behavior)
 * - [WindowWidthSizeClass.Medium] → Two panes visible
 * - [WindowWidthSizeClass.Expanded] → All panes visible
 *
 * ## Example
 *
 * ```kotlin
 * val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 600.dp))
 *
 * when (windowSizeClass.widthSizeClass) {
 *     WindowWidthSizeClass.Compact -> // Single pane layout
 *     WindowWidthSizeClass.Medium -> // Two pane layout
 *     WindowWidthSizeClass.Expanded -> // Full multi-pane layout
 * }
 * ```
 *
 * @property widthSizeClass The semantic classification of window width
 * @property heightSizeClass The semantic classification of window height
 */
@Immutable
public data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
) {
    public companion object {
        /**
         * Calculates [WindowSizeClass] from a [DpSize].
         *
         * @param size The window size in Dp units
         * @return The corresponding [WindowSizeClass]
         */
        public fun calculateFromSize(size: DpSize): WindowSizeClass {
            return WindowSizeClass(
                widthSizeClass = WindowWidthSizeClass.fromWidth(size.width),
                heightSizeClass = WindowHeightSizeClass.fromHeight(size.height)
            )
        }

        /**
         * Calculates [WindowSizeClass] from width and height values.
         *
         * @param width The window width in Dp
         * @param height The window height in Dp
         * @return The corresponding [WindowSizeClass]
         */
        public fun calculateFromSize(width: Dp, height: Dp): WindowSizeClass {
            return calculateFromSize(DpSize(width, height))
        }

        /**
         * Default compact window size class (for phones).
         */
        public val Compact: WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium
        )

        /**
         * Default medium window size class (for tablets).
         */
        public val Medium: WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Medium,
            heightSizeClass = WindowHeightSizeClass.Medium
        )

        /**
         * Default expanded window size class (for desktops).
         */
        public val Expanded: WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Expanded
        )
    }

    /**
     * Returns true if this represents a compact width (phone-like).
     */
    public val isCompactWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.Compact

    /**
     * Returns true if this represents a medium or larger width.
     */
    public val isAtLeastMediumWidth: Boolean
        get() = widthSizeClass != WindowWidthSizeClass.Compact

    /**
     * Returns true if this represents an expanded width (desktop-like).
     */
    public val isExpandedWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.Expanded
}

/**
 * Calculates the current window size class.
 *
 * This is the main entry point for obtaining window size information.
 * Each platform provides its own implementation that observes the
 * actual window dimensions and converts them to a [WindowSizeClass].
 *
 * The returned value will automatically recompose when the window
 * size changes (e.g., device rotation, window resize).
 *
 * ## Platform Implementations
 *
 * - **Android**: Uses `calculateWindowSizeClass()` from Material3
 * - **iOS**: Calculates from `UIScreen.main.bounds`
 * - **Desktop**: Calculates from window size state
 * - **Web (JS/Wasm)**: Calculates from `window.innerWidth/innerHeight`
 *
 * @return The current [WindowSizeClass] for the window
 */
@Composable
public expect fun calculateWindowSizeClass(): WindowSizeClass
