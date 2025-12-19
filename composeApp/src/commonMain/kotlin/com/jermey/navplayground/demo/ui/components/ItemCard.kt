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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.masterdetail.Item
import com.jermey.quo.vadis.core.navigation.compose.animation.quoVadisSharedElement
import com.jermey.quo.vadis.core.navigation.core.sharedBounds
import com.jermey.quo.vadis.core.navigation.core.sharedElement


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit
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
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Item icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .quoVadisSharedElement(sharedElement(key = "icon-${item.id}"))
            )
            
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Apply shared element transition to the title
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.quoVadisSharedElement(sharedBounds(key = "title-${item.id}"))
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
