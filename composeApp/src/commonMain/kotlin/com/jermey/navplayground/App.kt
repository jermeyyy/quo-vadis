package com.jermey.navplayground

// Import the comprehensive demo app
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jermey.navplayground.demo.DemoApp
import com.jermey.navplayground.demo.ui.theme.NavPlaygroundTheme
import com.jermey.navplayground.demo.ui.theme.rememberThemeManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val themeManager = rememberThemeManager()
    val themeMode by themeManager.themeMode.collectAsState()

    NavPlaygroundTheme(themeMode = themeMode) {
        // Use the comprehensive demo app showcasing all navigation patterns
        DemoApp()
    }
}
