package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.destinations.SettingsDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailContent(
    title: String,
    navigator: Navigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("This is the $title screen")
        }
    }
}

@Screen(SettingsDestination.Profile::class)
@Composable
fun ProfileSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(title = "Profile", navigator = navigator)
}

@Screen(SettingsDestination.Notifications::class)
@Composable
fun NotificationsSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(title = "Notifications", navigator = navigator)
}

@Screen(SettingsDestination.About::class)
@Composable
fun AboutSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(title = "About", navigator = navigator)
}
