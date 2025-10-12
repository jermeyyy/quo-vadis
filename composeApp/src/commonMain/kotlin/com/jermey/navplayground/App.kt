package com.jermey.navplayground

// Import the comprehensive demo app
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.DemoApp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Use the comprehensive demo app showcasing all navigation patterns
        DemoApp()
    }
}
