package com.jermey.navplayground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jermey.navplayground.demo.DemoApp
import com.jermey.navplayground.demo.navigationModule
import com.jermey.navplayground.demo.profileModule
import com.jermey.navplayground.demo.resultDemoModule
import com.jermey.navplayground.demo.tabsDemoModule
import com.jermey.navplayground.demo.ui.theme.NavPlaygroundTheme
import com.jermey.navplayground.demo.ui.theme.rememberThemeManager
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    val themeManager = rememberThemeManager()
    val themeMode by themeManager.themeMode.collectAsState()
    val koinConfiguration = koinConfiguration {
        modules(
            navigationModule,
            profileModule,
            resultDemoModule,
            tabsDemoModule
        )
    }
    KoinApplication(configuration = koinConfiguration) {
        NavPlaygroundTheme(themeMode = themeMode) {
            DemoApp()
        }
    }
}
