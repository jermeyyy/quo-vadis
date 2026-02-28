package com.jermey.navplayground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jermey.navplayground.demo.DemoApp
import com.jermey.navplayground.demo.ui.theme.NavPlaygroundTheme
import com.jermey.navplayground.demo.ui.theme.rememberThemeManager
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

@Composable
fun App() {
    val themeManager = rememberThemeManager()
    val themeMode by themeManager.themeMode.collectAsState()
    KoinApplication(configuration = koinConfiguration<NavPlaygroundKoinApp>()) {
        NavPlaygroundTheme(themeMode = themeMode) {
            DemoApp()
        }
    }
}
