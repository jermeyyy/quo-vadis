package com.jermey.navplayground.demo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.quo.vadis.core.navigation.core.Destination

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate(MainDestination.Home) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("Explore") },
            selected = currentRoute == "explore",
            onClick = { onNavigate(MainDestination.Explore) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onNavigate(MainDestination.Profile) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { onNavigate(MainDestination.Settings) }
        )
    }
}
