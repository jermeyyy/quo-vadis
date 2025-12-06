package com.jermey.quo.vadis.recipes.shared

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared scaffold component for recipe examples.
 *
 * Provides consistent UI structure across all recipes while keeping
 * the navigation logic in focus.
 *
 * ## Usage
 *
 * ```kotlin
 * @Screen(MyDestination.Home::class)
 * @Composable
 * fun HomeScreen(navigator: Navigator) {
 *     RecipeScaffold(title = "Home") {
 *         // Screen content
 *     }
 * }
 * ```
 *
 * @param title Screen title displayed in top app bar
 * @param showBackButton Whether to show back navigation button
 * @param onBackClick Callback when back button is clicked
 * @param bottomBar Optional bottom bar content
 * @param content Main screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScaffold(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Text("←")
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        content(padding)
    }
}

/**
 * Navigation button for recipe examples.
 *
 * Standard button style for triggering navigation actions.
 *
 * @param text Button label
 * @param onClick Navigation action
 * @param modifier Optional modifier
 */
@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}
