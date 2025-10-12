package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.SettingItem
import com.jermey.navplayground.demo.ui.components.SettingsSection

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            SettingsSection("Account") {
                SettingItem("Email notifications", Icons.Default.Email)
                SettingItem("Push notifications", Icons.Default.Notifications)
                SettingItem("Privacy settings", Icons.Default.Lock)
            }
        }

        item {
            SettingsSection("Appearance") {
                SettingItem("Dark mode", Icons.Default.DarkMode)
                SettingItem("Language", Icons.Default.Language)
            }
        }

        item {
            SettingsSection("About") {
                SettingItem("Version", Icons.Default.Info)
                SettingItem("Terms of Service", Icons.Default.Description)
                SettingItem("Privacy Policy", Icons.Default.Policy)
            }
        }
    }
}
