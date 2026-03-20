package com.jermey.quo.vadis.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import com.jermey.quo.vadis.core.compose.scope.LocalScreenNode
import com.jermey.quo.vadis.core.registry.LocalBackHandlerRegistry

/**
 * Registers a back handler scoped to the current screen in the navigation tree.
 *
 * When the user presses the system back button or performs a back gesture, this handler
 * is consulted before normal back navigation occurs. If [onBack] handles the event
 * (the handler returns `true`), normal navigation is suppressed.
 *
 * The handler is automatically scoped to the current screen via [LocalScreenNode] and
 * registered with the [LocalBackHandlerRegistry]. It is cleaned up when the composable
 * leaves the composition or when [enabled] becomes `false`.
 *
 * **Note:** When active back handlers are registered for a screen, predictive back gestures
 * (swipe preview) are disabled for that screen, falling back to a simple back callback.
 *
 * ## Usage
 *
 * ```kotlin
 * @Screen(MyDestination.Editor::class)
 * @Composable
 * fun EditorScreen(navigator: Navigator) {
 *     var hasUnsavedChanges by remember { mutableStateOf(false) }
 *     var showConfirmDialog by remember { mutableStateOf(false) }
 *
 *     NavBackHandler(enabled = hasUnsavedChanges) {
 *         showConfirmDialog = true
 *     }
 *
 *     // ... editor UI ...
 *
 *     if (showConfirmDialog) {
 *         ConfirmDialog(
 *             onConfirm = { navigator.navigateBack() },
 *             onDismiss = { showConfirmDialog = false }
 *         )
 *     }
 * }
 * ```
 *
 * @param enabled Whether this back handler is active. When `false`, the handler is not
 *   registered and does not intercept back events. Defaults to `true`.
 * @param onBack Called when a back event occurs while this handler is active. The handler
 *   always consumes the event (returns `true` to the registry), so the caller is responsible
 *   for performing any desired navigation via [com.jermey.quo.vadis.core.navigation.navigator.Navigator.navigateBack].
 */
@Composable
fun NavBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val screenNode = LocalScreenNode.current ?: return
    val registry = LocalBackHandlerRegistry.current

    // Use rememberUpdatedState to capture the latest onBack lambda without
    // causing DisposableEffect to re-run when the lambda reference changes.
    val currentOnBack = rememberUpdatedState(onBack)

    DisposableEffect(enabled, screenNode.key, registry) {
        if (!enabled) return@DisposableEffect onDispose {}

        val unregister = registry.register(screenNode.key) {
            currentOnBack.value()
            true
        }

        onDispose { unregister() }
    }
}
