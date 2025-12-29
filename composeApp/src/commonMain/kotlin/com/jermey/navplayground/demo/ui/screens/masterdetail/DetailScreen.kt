package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.ui.components.DetailRow
import com.jermey.navplayground.demo.ui.components.SpecificationRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.compose.animation.TransitionScope
import com.jermey.quo.vadis.core.navigation.Navigator
import org.koin.compose.koinInject

private const val RELATED_ITEMS_COUNT = 5
private const val ANIMATION_DURATION = 300

/**
 * Detail Screen - Shows details of selected item (Detail view)
 * 
 * Enhanced with coordinated animations using AnimatedVisibilityScope:
 * - Background fades in/out with navigation
 * - TopAppBar slides from/to top
 * - Header card uses shared element transition
 * - Content below slides up/down from bottom
 * 
 * All animations are tied to the navigation transition, so they work in reverse too!
 */
@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator = koinInject()
) {
    val itemId = destination.itemId
    val relatedItems = (1..RELATED_ITEMS_COUNT).map { "Related item $it" }
    
    // Get transition scope - animations are tied to this
    val transitionScope = _root_ide_package_.com.jermey.quo.vadis.core.compose.animation.LocalTransitionScope.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated background - fades with navigation
        // Note: animateEnterExit must come BEFORE background to animate the alpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .animateEnterExit(
                    transitionScope,
                    enter = fadeIn(tween(ANIMATION_DURATION)),
                    exit = fadeOut(tween(ANIMATION_DURATION))
                )
                .background(MaterialTheme.colorScheme.surface)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // TopAppBar slides from/to top
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Favorite, "Favorite")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.animateEnterExit(
                    transitionScope,
                    enter = slideInVertically(tween(ANIMATION_DURATION)) { -it } + fadeIn(tween(ANIMATION_DURATION)),
                    exit = slideOutVertically(tween(ANIMATION_DURATION)) { -it } + fadeOut(tween(ANIMATION_DURATION))
                )
            )

            // Scrollable content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card with shared element
                item {
                    DetailHeaderCard(itemId, transitionScope)
                }

                // Content slides up/down - use index for stagger on enter only
                item {
                    Box(
                        modifier = Modifier.animateEnterExit(
                            transitionScope,
                            enter = slideInVertically(tween(ANIMATION_DURATION, delayMillis = 50)) { it / 2 } + fadeIn(tween(ANIMATION_DURATION, delayMillis = 50)),
                            exit = slideOutVertically(tween(ANIMATION_DURATION)) { it / 4 } + fadeOut(tween(ANIMATION_DURATION))
                        )
                    ) {
                        SpecificationsCard()
                    }
                }

                item {
                    Box(
                        modifier = Modifier.animateEnterExit(
                            transitionScope,
                            enter = slideInVertically(tween(ANIMATION_DURATION, delayMillis = 100)) { it / 2 } + fadeIn(tween(ANIMATION_DURATION, delayMillis = 100)),
                            exit = slideOutVertically(tween(ANIMATION_DURATION)) { it / 4 } + fadeOut(tween(ANIMATION_DURATION))
                        )
                    ) {
                        RelatedItemsHeader()
                    }
                }

                items(relatedItems.size) { index ->
                    val delay = 150 + (index * 30)
                    Box(
                        modifier = Modifier.animateEnterExit(
                            transitionScope,
                            enter = slideInVertically(tween(ANIMATION_DURATION, delayMillis = delay)) { it / 2 } + fadeIn(tween(ANIMATION_DURATION, delayMillis = delay)),
                            exit = slideOutVertically(tween(ANIMATION_DURATION)) { it / 4 } + fadeOut(tween(ANIMATION_DURATION))
                        )
                    ) {
                        val relatedId = "related_${itemId}_$index"
                        RelatedItemCard(
                            relatedItemName = relatedItems[index],
                            onNavigateToRelated = { navigator.navigate(MasterDetailDestination.Detail(itemId = relatedId)) }
                        )
                    }
                }

                item {
                    val delay = 150 + (relatedItems.size * 30)
                    Box(
                        modifier = Modifier.animateEnterExit(
                            transitionScope,
                            enter = slideInVertically(tween(ANIMATION_DURATION, delayMillis = delay)) { it / 2 } + fadeIn(tween(ANIMATION_DURATION, delayMillis = delay)),
                            exit = slideOutVertically(tween(ANIMATION_DURATION)) { it / 4 } + fadeOut(tween(ANIMATION_DURATION))
                        )
                    ) {
                        ActionButtons(onBack = { navigator.navigateBack() })
                    }
                }
            }
        }
    }
}

/**
 * Extension to apply animateEnterExit when TransitionScope is available.
 * Falls back to unmodified if scope is null.
 */
@Composable
private fun Modifier.animateEnterExit(
    transitionScope: TransitionScope?,
    enter: androidx.compose.animation.EnterTransition,
    exit: androidx.compose.animation.ExitTransition
): Modifier {
    return if (transitionScope != null) {
        with(transitionScope.animatedVisibilityScope) {
            this@animateEnterExit.animateEnterExit(enter = enter, exit = exit)
        }
    } else {
        this
    }
}

/**
 * Header card showing main item information with shared element transitions.
 * The entire card transforms from the list item card, while inner content fades in/out.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailHeaderCard(itemId: String, transitionScope: TransitionScope?) {
    // Base card modifier
    val cardModifier = Modifier.fillMaxWidth()

    // Apply shared bounds if transition scope available
    val finalCardModifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            cardModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card-container-$itemId"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else {
        cardModifier
    }

    Card(
        modifier = finalCardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with large icon and title (shared elements)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with shared element transition
                val iconModifier = Modifier.size(80.dp)
                val finalIconModifier = if (transitionScope != null) {
                    with(transitionScope.sharedTransitionScope) {
                        iconModifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "icon-$itemId"),
                            animatedVisibilityScope = transitionScope.animatedVisibilityScope
                        )
                    }
                } else {
                    iconModifier
                }

                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Item icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = finalIconModifier
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        itemId.replace("_", " ").capitalize(),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    // Subtitle with fade animation tied to navigation
                    Text(
                        "Detailed Information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateEnterExit(
                            transitionScope,
                            enter = fadeIn(tween(ANIMATION_DURATION)),
                            exit = fadeOut(tween(ANIMATION_DURATION))
                        )
                    )
                }
            }

            // Description and details with fade animation
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.animateEnterExit(
                    transitionScope,
                    enter = fadeIn(tween(ANIMATION_DURATION, delayMillis = 50)),
                    exit = fadeOut(tween(ANIMATION_DURATION))
                )
            ) {
                Text(
                    text = "This is a detailed view of $itemId. In a real application, " +
                            "this would show comprehensive information about the selected item.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                DetailRow("ID", itemId)
                DetailRow("Category", "Sample Category")
                DetailRow("Status", "Available")
                DetailRow("Price", "$99.99")
            }
        }
    }
}

/**
 * Card displaying item specifications.
 */
@Composable
private fun SpecificationsCard() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Specifications",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SpecificationRow("Weight", "500g")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Dimensions", "10 x 5 x 2 cm")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Material", "Premium")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Color", "Blue")
            }
        }
    }
}

/**
 * Header for related items section.
 */
@Composable
private fun RelatedItemsHeader() {
    Text(
        "Related Items",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Card for a single related item.
 */
@Composable
private fun RelatedItemCard(
    relatedItemName: String,
    onNavigateToRelated: () -> Unit
) {
    Card(
        onClick = onNavigateToRelated,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(relatedItemName) },
            supportingContent = { Text("Tap to view details") },
            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "View") }
        )
    }
}

/**
 * Action buttons at the bottom of the detail screen.
 */
@Composable
private fun ActionButtons(onBack: () -> Unit) {
    Column {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back to List")
            }

            Button(
                onClick = {},
                modifier = Modifier.weight(1f)
            ) {
                Text("Add to Cart")
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
