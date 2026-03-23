package com.jermey.feature2.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// NavigationMenuDestination from shared navigation-api module
import com.jermey.navplayground.navigation.NavigationMenuDestination
import com.jermey.navplayground.navigation.SettingsTab
import com.jermey.feature2.settings.components.SettingItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.jermey.feature2.settings.components.SettingsSection
import com.jermey.feature2.settings.components.ThemeSettingItem
import com.jermey.feature2.settings.ThemeManager
import com.jermey.navplayground.navigation.ThemeMode
import com.jermey.feature2.settings.rememberThemeManager
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import org.koin.compose.koinInject

/**
 * Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(SettingsTab.Main::class)
@Composable
fun SettingsScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }

    // Theme manager for theme switching
    val themeManager = rememberThemeManager()
    val currentThemeMode by themeManager.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigate(NavigationMenuDestination) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        SettingsScreenContent(
            navigator,
            modifier.hazeSource(state = hazeState),
            paddingValues,
            currentThemeMode,
            themeManager
        )
    }

}

@Composable
private fun SettingsScreenContent(
    navigator: Navigator,
    modifier: Modifier,
    paddingValues: PaddingValues,
    currentThemeMode: ThemeMode,
    themeManager: ThemeManager
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection("General") {
                SettingItem(
                    title = "Profile",
                    icon = Icons.Default.Person,
                    onClick = { navigator.navigate(SettingsTab.Profile) }
                )
                SettingItem(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    onClick = { navigator.navigate(SettingsTab.Notifications) }
                )
                SettingItem(
                    title = "About",
                    icon = Icons.Default.Info,
                    onClick = { navigator.navigate(SettingsTab.About) }
                )
            }
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
                ThemeSettingItem(
                    currentMode = currentThemeMode,
                    onThemeChange = { newMode ->
                        themeManager.setThemeMode(newMode)
                    }
                )
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
        item {
            Spacer(modifier = Modifier.height( 64.dp))
        }
    }
}
