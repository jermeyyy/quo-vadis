package com.jermey.navplayground.demo.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.masterdetail.Item
import com.jermey.quo.vadis.core.navigation.compose.animation.LocalTransitionScope


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit
) {
    // Get transition scope for shared element transitions
    val transitionScope = LocalTransitionScope.current

    // Base card modifier
    val cardModifier = Modifier.fillMaxWidth()

    // Apply shared bounds if transition scope available
    val finalCardModifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            cardModifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card-container-${item.id}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else {
        cardModifier
    }

    Card(
        onClick = onClick,
        modifier = finalCardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with shared element transition
            val iconModifier = Modifier.size(56.dp)
            val finalIconModifier = if (transitionScope != null) {
                with(transitionScope.sharedTransitionScope) {
                    iconModifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "icon-${item.id}"),
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
                    item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(item.category, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
