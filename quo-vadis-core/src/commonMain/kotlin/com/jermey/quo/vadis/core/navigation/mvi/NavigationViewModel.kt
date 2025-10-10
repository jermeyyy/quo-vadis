package com.jermey.navplayground.navigation.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jermey.navplayground.navigation.core.DeepLink
import com.jermey.navplayground.navigation.core.Navigator
import kotlinx.coroutines.flow.*
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
@androidx.compose.runtime.Composable
fun <T : NavigationEffect> SharedFlow<T>.collectAsEffect(
    onEffect: suspend (T) -> Unit
) {
    val flow = this
    androidx.compose.runtime.LaunchedEffect(flow) {
        flow.collect { effect ->
            onEffect(effect)
        }
    }
}

