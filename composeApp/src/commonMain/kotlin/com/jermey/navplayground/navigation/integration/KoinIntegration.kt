package com.jermey.navplayground.navigation.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.navplayground.navigation.core.*

/**
 * Integration helpers for Koin dependency injection.
 *
 * Usage example:
 * ```
 * val appModule = module {
 *     single<Navigator> { DefaultNavigator() }
 *     single<DeepLinkHandler> { DefaultDeepLinkHandler() }
 * }
 * ```
 */

/**
 * Factory interface for creating navigation-related objects with DI.
 */
interface NavigationFactory {
    fun createNavigator(): Navigator
    fun createDeepLinkHandler(): DeepLinkHandler
}

/**
 * Default implementation that can be injected via Koin.
 */
class DefaultNavigationFactory(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
) : NavigationFactory {
    override fun createNavigator(): Navigator {
        return DefaultNavigator(deepLinkHandler)
    }

    override fun createDeepLinkHandler(): DeepLinkHandler {
        return deepLinkHandler
    }
}

/**
 * Extension for getting Navigator from DI container.
 *
 * Example with Koin:
 * ```
 * @Composable
 * fun App() {
 *     val navigator = getKoin().get<Navigator>()
 *     NavHost(navigator = navigator) { ... }
 * }
 * ```
 */
interface DIContainer {
    fun <T : Any> get(clazz: kotlin.reflect.KClass<T>): T
}

inline fun <reified T : Any> DIContainer.get(): T = get(T::class)

/**
 * Helper to remember navigator from DI.
 */
@Composable
inline fun <reified T : Any> rememberFromDI(
    container: DIContainer,
    key: Any? = null
): T {
    return remember(key) {
        container.get<T>()
    }
}
