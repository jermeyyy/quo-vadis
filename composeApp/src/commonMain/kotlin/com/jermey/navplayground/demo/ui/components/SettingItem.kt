package com.jermey.navplayground.demo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SettingItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, "Open") }
    )
}
