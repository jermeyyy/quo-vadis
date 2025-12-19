package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.jermey.quo.vadis.core.navigation.compose.registry.LocalBackHandlerRegistry

/**
 * A composable that intercepts back press events for custom handling.
 *
 * When a back press occurs, the navigation system checks registered handlers
 * in reverse order (innermost first). If any handler returns `true`, the back
 * event is considered consumed and default navigation does not occur.
 *
 * This is useful for scenarios like:
 * - Confirming before discarding unsaved changes
 * - Closing dialogs or bottom sheets before navigating back
 * - Implementing custom back behavior in specific screens
 *
 * @param enabled Whether this back handler is currently active. When `false`,
 *                the handler is not registered and back events pass through.
 *                Defaults to `true`.
 * @param onBack Callback invoked when back is pressed. Return `true` to consume
 *               the back event and prevent default navigation, `false` to allow
 *               the event to propagate to the next handler or navigation system.
 * @param content The content to display. Back handling is active while this
 *                content is composed.
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun EditScreen(navigator: Navigator) {
 *     var hasUnsavedChanges by remember { mutableStateOf(false) }
 *     var showConfirmDialog by remember { mutableStateOf(false) }
 *
 *     NavBackHandler(enabled = hasUnsavedChanges, onBack = {
 *         showConfirmDialog = true
 *         true // Consume the back event
 *     }) {
 *         // Screen content
 *         if (showConfirmDialog) {
 *             ConfirmDialog(
 *                 onConfirm = { navigator.navigateBack() },
 *                 onDismiss = { showConfirmDialog = false }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * @see LocalBackHandlerRegistry
 * @see com.jermey.quo.vadis.core.navigation.compose.registry.BackHandlerRegistry
 */
@Composable
fun NavBackHandler(
    enabled: Boolean = true,
    onBack: () -> Boolean,
    content: @Composable () -> Unit
) {
    if (enabled) {
        val registry = LocalBackHandlerRegistry.current
        val currentOnBack by rememberUpdatedState(onBack)

        DisposableEffect(registry) {
            val unregister = registry.register { currentOnBack() }
            onDispose { unregister() }
        }
    }

    content()
}

/**
 * Simplified version of [NavBackHandler] that always consumes the back event.
 *
 * This variant is useful when you always want to intercept back events without
 * conditionally propagating them. The back event is automatically consumed
 * (returns `true`) when this handler is enabled.
 *
 * @param enabled Whether this back handler is currently active. When `false`,
 *                the handler is not registered and back events pass through.
 *                Defaults to `true`.
 * @param onBackPressed Callback invoked when back is pressed. The back event is
 *                      always consumed when this handler is enabled.
 * @param content The content to display. Back handling is active while this
 *                content is composed.
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun ModalScreen(onDismiss: () -> Unit) {
 *     ConsumingNavBackHandler(
 *         enabled = true,
 *         onBackPressed = {
 *             // Handle back by dismissing the modal
 *             onDismiss()
 *         }
 *     ) {
 *         // Modal content
 *     }
 * }
 * ```
 *
 * @see NavBackHandler
 */
@Composable
fun ConsumingNavBackHandler(
    enabled: Boolean = true,
    onBackPressed: () -> Unit,
    content: @Composable () -> Unit
) {
    NavBackHandler(
        enabled = enabled,
        onBack = {
            onBackPressed()
            true
        },
        content = content
    )
}
