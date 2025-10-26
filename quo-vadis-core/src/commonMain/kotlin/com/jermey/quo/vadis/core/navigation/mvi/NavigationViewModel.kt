package com.jermey.quo.vadis.core.navigation.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.route
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for screens that use navigation with MVI pattern.
 * Handles navigation intents and provides navigation state.
 */
abstract class NavigationViewModel(
    protected val navigator: Navigator
) : ViewModel() {

    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _navigationEffects = MutableSharedFlow<NavigationEffect>()
    val navigationEffects: SharedFlow<NavigationEffect> = _navigationEffects.asSharedFlow()

    init {
        observeNavigatorState()
    }

    /**
     * Handle navigation intents.
     */
    fun handleNavigationIntent(intent: NavigationIntent) {
        viewModelScope.launch {
            when (intent) {
                is NavigationIntent.Navigate -> {
                    navigator.navigate(intent.destination, intent.transition)
                }

                is NavigationIntent.NavigateBack -> {
                    val success = navigator.navigateBack()
                    if (!success) {
                        _navigationEffects.emit(
                            NavigationEffect.NavigationFailed("Cannot navigate back")
                        )
                    }
                }

                is NavigationIntent.NavigateUp -> {
                    navigator.navigateUp()
                }

                is NavigationIntent.NavigateAndClearAll -> {
                    navigator.navigateAndClearAll(intent.destination)
                }

                is NavigationIntent.NavigateAndClearTo -> {
                    navigator.navigateAndClearTo(
                        intent.destination,
                        intent.clearRoute,
                        intent.inclusive
                    )
                }

                is NavigationIntent.NavigateAndReplace -> {
                    navigator.navigateAndReplace(intent.destination, intent.transition)
                }

                is NavigationIntent.HandleDeepLink -> {
                    try {
                        val deepLink = DeepLink.parse(intent.uri)
                        navigator.handleDeepLink(deepLink)
                        _navigationEffects.emit(NavigationEffect.DeepLinkHandled(true))
                    } catch (e: Exception) {
                        _navigationEffects.emit(NavigationEffect.DeepLinkHandled(false))
                    }
                }
            }
        }
    }

    private fun observeNavigatorState() {
        viewModelScope.launch {
            navigator.backStack.current.collect { entry ->
                _navigationState.update { state ->
                    state.copy(
                        currentRoute = entry?.destination?.route,
                        canGoBack = navigator.backStack.canGoBack.value
                    )
                }
            }
        }
    }
}

/**
 * Extension function to easily collect navigation effects in composables.
 */
@Composable
fun <T : NavigationEffect> SharedFlow<T>.collectAsEffect(
    onEffect: suspend (T) -> Unit
) {
    val flow = this
    LaunchedEffect(flow) {
        flow.collect { effect ->
            onEffect(effect)
        }
    }
}

