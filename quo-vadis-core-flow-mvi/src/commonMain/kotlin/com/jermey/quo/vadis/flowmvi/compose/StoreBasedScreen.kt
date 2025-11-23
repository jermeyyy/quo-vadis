package com.jermey.quo.vadis.flowmvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Store-based screen pattern for consistent screen structure.
 * 
 * Provides a standardized way to create screens with FlowMVI state management:
 * - Automatic state subscription
 * - Action handling
 * - Intent receiver for UI interactions
 * 
 * Benefits:
 * - Consistent screen structure across the app
 * - Automatic lifecycle management
 * - Type-safe state and intent handling
 * - Testable UI components
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun ProfileScreen(container: ProfileContainer = koinInject()) {
 *     StoreScreen(
 *         container = container,
 *         onAction = { action ->
 *             when (action) {
 *                 is ProfileAction.ShowToast -> {
 *                     snackbarHostState.showSnackbar(action.message)
 *                 }
 *             }
 *         }
 *     ) { state, intentReceiver ->
 *         ProfileContent(
 *             state = state,
 *             onIntent = intentReceiver::intent
 *         )
 *     }
 * }
 * ```
 * 
 * @param S State type (must implement MVIState)
 * @param I Intent type (must implement MVIIntent)
 * @param A Action type (must implement MVIAction)
 * @param container The FlowMVI container
 * @param onAction Optional action handler (default: no-op)
 * @param content The composable content receiving state and intent receiver
 */
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreScreen(
    container: Container<S, I, A>,
    onAction: suspend (A) -> Unit = {},
    content: @Composable (state: S, intentReceiver: IntentReceiver<I>) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Start the store when composable enters composition
    LaunchedEffect(container.store) {
        container.store.start(scope)
    }
    
    with(container.store) {
        val state by subscribe { action ->
            onAction(action)
        }
        content(state, this)
    }
}

/**
 * Store-based screen with separate action and content handling.
 * 
 * Use this variant when you need to separate action handling from content rendering,
 * or when actions should be handled at a higher level.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun ProfileScreen(container: ProfileContainer = koinInject()) {
 *     val snackbarHostState = remember { SnackbarHostState() }
 *     
 *     StoreScreenWithActions(
 *         container = container,
 *         actionHandler = { action ->
 *             when (action) {
 *                 is ProfileAction.ShowToast -> {
 *                     launch { snackbarHostState.showSnackbar(action.message) }
 *                 }
 *             }
 *         }
 *     ) { state, intentReceiver ->
 *         Scaffold(
 *             snackbarHost = { SnackbarHost(snackbarHostState) }
 *         ) {
 *             ProfileContent(state, intentReceiver)
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreScreenWithActions(
    container: Container<S, I, A>,
    actionHandler: suspend (A) -> Unit,
    content: @Composable (state: S, intentReceiver: IntentReceiver<I>) -> Unit
) {
    with(container.store) {
        val state by subscribe { action ->
            actionHandler(action)
        }
        content(state, this)
    }
}

/**
 * Lightweight store content wrapper.
 * 
 * Use when you only need state and intent receiver without action handling.
 * Useful for simple screens or nested components.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun SimpleList(container: ListContainer) {
 *     StoreContent(container) { state, intentReceiver ->
 *         LazyColumn {
 *             items(state.items) { item ->
 *                 ItemCard(
 *                     item = item,
 *                     onClick = { intentReceiver.intent(ListIntent.SelectItem(item)) }
 *                 )
 *             }
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreContent(
    container: Container<S, I, A>,
    content: @Composable (state: S, intentReceiver: IntentReceiver<I>) -> Unit
) {
    StoreScreen(
        container = container,
        onAction = {}, // No action handling
        content = content
    )
}

/**
 * Pattern for screens with explicit receiver scope.
 * 
 * Provides intent receiver as a receiver scope, allowing direct `intent()` calls
 * without explicitly passing the receiver.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun ProfileScreen(container: ProfileContainer = koinInject()) {
 *     StoreScreenWithReceiver(container) { state ->
 *         // Intent receiver is implicit scope
 *         Button(onClick = { intent(ProfileIntent.Edit) }) {
 *             Text("Edit")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreScreenWithReceiver(
    container: Container<S, I, A>,
    onAction: suspend (A) -> Unit = {},
    content: @Composable IntentReceiver<I>.(state: S) -> Unit
) {
    with(container.store) {
        val state by subscribe { action -> onAction(action) }
        content(state)
    }
}
