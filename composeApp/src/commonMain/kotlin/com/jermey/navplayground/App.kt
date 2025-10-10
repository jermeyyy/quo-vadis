package com.jermey.navplayground

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

// Import the comprehensive demo app
import com.jermey.navplayground.demo.DemoApp

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Use the comprehensive demo app showcasing all navigation patterns
        DemoApp()
    }
}