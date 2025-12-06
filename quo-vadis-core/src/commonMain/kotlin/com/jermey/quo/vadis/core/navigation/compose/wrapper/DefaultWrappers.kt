@file:Suppress("MatchingDeclarationName")

package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Default tab wrapper that renders a basic bottom navigation layout.
 *
 * This implementation provides a Material 3 styled wrapper with:
 * - A [NavigationBar] at the bottom
 * - Tab content filling the remaining space
 *
 * For custom layouts, users should provide their own [TabWrapper].
 *
 * ## Usage
 *
 * ```kotlin
 * TabNode(
 *     tabs = listOf(homeTab, searchTab, profileTab),
 *     wrapper = DefaultTabWrapper
 * )
 * ```
 */
public val DefaultTabWrapper: TabWrapper = { tabContent ->
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = {
                            meta.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = meta.contentDescription ?: meta.label
                                )
                            }
                        },
                        label = { Text(meta.label) },
                        enabled = !isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabContent()
        }
    }
}

/**
 * Tab wrapper with top tab row instead of bottom navigation.
 *
 * Useful for tablet layouts or secondary tab navigation.
 *
 * ## Usage
 *
 * ```kotlin
 * TabNode(
 *     tabs = listOf(tab1, tab2, tab3),
 *     wrapper = TopTabWrapper
 * )
 * ```
 */
public val TopTabWrapper: TabWrapper = { tabContent ->
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = activeTabIndex) {
            tabMetadata.forEachIndexed { index, meta ->
                Tab(
                    selected = activeTabIndex == index,
                    onClick = { switchTab(index) },
                    text = { Text(meta.label) },
                    icon = {
                        meta.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = meta.contentDescription ?: meta.label
                            )
                        }
                    },
                    enabled = !isTransitioning
                )
            }
        }
        HorizontalDivider()
        Box(modifier = Modifier.weight(1f)) {
            tabContent()
        }
    }
}

/**
 * Default pane wrapper that renders panes side-by-side with equal weight.
 *
 * This implementation provides a basic multi-pane layout:
 * - Panes arranged horizontally in a [Row]
 * - Equal width distribution by default
 * - Vertical dividers between panes
 *
 * For custom layouts (weighted widths, draggable dividers, etc.),
 * users should provide their own [PaneWrapper].
 *
 * ## Usage
 *
 * ```kotlin
 * PaneNode(
 *     primaryPane = primaryPaneConfig,
 *     supportingPane = supportingPaneConfig,
 *     wrapper = DefaultPaneWrapper
 * )
 * ```
 */
public val DefaultPaneWrapper: PaneWrapper = { paneContents ->
    val visiblePanes = paneContents.filter { it.isVisible }
    Row(modifier = Modifier.fillMaxSize()) {
        visiblePanes.forEachIndexed { index, pane ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                pane.content()
            }
        }
    }
}

/**
 * Pane wrapper with weighted widths based on pane role.
 *
 * Applies common width ratios:
 * - Primary: 65% (main content area)
 * - Supporting: 35% (secondary content)
 * - Extra: 25% (supplementary content)
 *
 * ## Usage
 *
 * ```kotlin
 * PaneNode(
 *     primaryPane = primaryPaneConfig,
 *     supportingPane = supportingPaneConfig,
 *     wrapper = WeightedPaneWrapper
 * )
 * ```
 */
public val WeightedPaneWrapper: PaneWrapper = { paneContents ->
    val visiblePanes = paneContents.filter { it.isVisible }
    Row(modifier = Modifier.fillMaxSize()) {
        visiblePanes.forEachIndexed { index, pane ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp
                )
            }
            val weight = when (pane.role) {
                PaneRole.Primary -> WEIGHT_PRIMARY
                PaneRole.Supporting -> WEIGHT_SUPPORTING
                PaneRole.Extra -> WEIGHT_EXTRA
            }
            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
            ) {
                pane.content()
            }
        }
    }
}

/** Default weight for Primary pane (main content) */
private const val WEIGHT_PRIMARY = 0.65f

/** Default weight for Supporting pane (secondary content) */
private const val WEIGHT_SUPPORTING = 0.35f

/** Default weight for Extra pane (supplementary content) */
private const val WEIGHT_EXTRA = 0.25f
