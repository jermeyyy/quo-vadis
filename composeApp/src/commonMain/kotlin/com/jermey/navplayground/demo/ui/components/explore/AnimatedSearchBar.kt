package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

private const val ANIMATION_DURATION_MS = 200

/**
 * An animated search bar with smooth transitions.
 *
 * Behavior:
 * - Unfocused: Shows search icon on left
 * - Focused with no text: Shows search icon, cancel button on right
 * - Focused with text: Shows close icon on left (clears text), search button on right
 *
 * @param query Current search query text
 * @param onQueryChange Callback when query text changes
 * @param onFocusChange Callback when focus state changes
 * @param modifier Modifier for the search bar container
 * @param placeholder Placeholder text shown when query is empty
 */
@Composable
fun AnimatedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val hasText = query.isNotEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                },
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                LeadingSearchIcon(
                    hasText = hasText,
                    onClear = { onQueryChange("") }
                )
            },
            trailingIcon = {
                // Show search icon inside field when has text (for visual balance)
                if (hasText) {
                    IconButton(onClick = { focusManager.clearFocus() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = searchFieldColors(),
            shape = RoundedCornerShape(28.dp)
        )

        // Cancel button - only when focused and no text
        AnimatedVisibility(
            visible = isFocused && !hasText,
            enter = fadeIn(tween(ANIMATION_DURATION_MS)) +
                    slideInHorizontally(tween(ANIMATION_DURATION_MS)) { it / 2 },
            exit = fadeOut(tween(ANIMATION_DURATION_MS)) +
                    slideOutHorizontally(tween(ANIMATION_DURATION_MS)) { it / 2 }
        ) {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onQueryChange("")
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search button - only when has text
        AnimatedVisibility(
            visible = hasText,
            enter = fadeIn(tween(ANIMATION_DURATION_MS)) +
                    slideInHorizontally(tween(ANIMATION_DURATION_MS)) { it / 2 },
            exit = fadeOut(tween(ANIMATION_DURATION_MS)) +
                    slideOutHorizontally(tween(ANIMATION_DURATION_MS)) { it / 2 }
        ) {
            TextButton(
                onClick = { focusManager.clearFocus() },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "Search",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LeadingSearchIcon(
    hasText: Boolean,
    onClear: () -> Unit
) {
    AnimatedContent(
        targetState = hasText,
        transitionSpec = {
            (fadeIn(tween(ANIMATION_DURATION_MS)) + scaleIn(tween(ANIMATION_DURATION_MS)))
                .togetherWith(fadeOut(tween(ANIMATION_DURATION_MS)) + scaleOut(tween(ANIMATION_DURATION_MS)))
        },
        label = "leadingIcon"
    ) { showClose ->
        if (showClose) {
            IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(onClick = { /* No action when search icon */ }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun searchFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}
