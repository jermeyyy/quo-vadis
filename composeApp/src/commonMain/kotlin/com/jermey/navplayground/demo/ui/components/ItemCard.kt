package com.jermey.navplayground.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.masterdetail.Item


@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apply shared element transition to the icon (now on the left, larger)
            val iconModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier
                        .size(56.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "icon-${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                }
            } else {
                Modifier.size(56.dp)
            }
            
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Item icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = iconModifier
            )
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Apply shared element transition to the title
                val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "title-${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    Modifier
                }
                
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = titleModifier
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
