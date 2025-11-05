package com.jermey.quo.vadis.flowmvi.integration

import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.flowmvi.core.NavigationAction
import com.jermey.quo.vadis.flowmvi.core.NavigationIntent
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import com.jermey.quo.vadis.flowmvi.core.NavigatorContainer
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
import org.koin.dsl.module
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Core Koin module for FlowMVI navigation integration.
 * 
 * Provides base navigation dependencies:
 * - Navigator singleton
 * - NavigatorContainer singleton
 * 
 * Usage:
 * ```kotlin
 * startKoin {
 *     modules(
 *         flowMviNavigationModule,
 *         yourFeatureModules...
 *     )
 * }
 * ```
 */
val flowMviNavigationModule = module {
    // Navigator (can be overridden by providing your own implementation)
    single<Navigator> {
        com.jermey.quo.vadis.core.navigation.core.DefaultNavigator()
    }
    
    // FlowMVI NavigatorContainer
    single<NavigatorContainer> {
        NavigatorContainer(
            navigator = get(),
            debuggable = getProperty("debug.navigation", false)
        )
    }
}

/**
 * Creates a Koin module for a feature-specific store.
 * 
 * Use this factory function to define store modules for your features.
 * The store will be provided as a singleton within the Koin graph.
 * 
 * Usage:
 * ```kotlin
 * val profileModule = featureStoreModule<ProfileState, ProfileIntent, ProfileAction> { navigator ->
 *     ProfileContainer(navigator = navigator, repository = get())
 * }
 * ```
 * 
 * @param S State type (extends MVIState)
 * @param I Intent type (extends MVIIntent)
 * @param A Action type (extends MVIAction)
 * @param factory Factory function creating the container (receives Navigator as parameter)
 * @return Koin module defining the container
 */
inline fun <reified S : MVIState, reified I : MVIIntent, reified A : MVIAction> featureStoreModule(
    crossinline factory: Scope.(Navigator) -> Container<S, I, A>
): Module = module {
    single<Container<S, I, A>> {
        factory(get())
    }
}

/**
 * Creates a Koin module for multiple feature stores.
 * 
 * Use when you have multiple related stores that should be grouped together.
 * 
 * Usage:
 * ```kotlin
 * val profileFeatureModule = featureStoresModule {
 *     single<ProfileContainer> {
 *         ProfileContainer(navigator = get(), repository = get())
 *     }
 *     single<ProfileSettingsContainer> {
 *         ProfileSettingsContainer(navigator = get())
 *     }
 * }
 * ```
 */
fun featureStoresModule(moduleDeclaration: Module.() -> Unit): Module = module(createdAtStart = false) {
    moduleDeclaration()
}

/**
 * Koin module for common store dependencies.
 * 
 * Includes:
 * - Coroutine scope for stores
 * - Configuration providers
 * 
 * Add this module if you need shared infrastructure for stores.
 */
val storeInfrastructureModule = module {
    // Could add shared dependencies like:
    // - Analytics tracker
    // - Error reporter
    // - Logger
}

/**
 * Extension: Get a container with automatic type inference.
 * 
 * Usage:
 * ```kotlin
 * class MyViewModel(private val navigator: Navigator) {
 *     private val profileContainer: ProfileContainer = getKoin().getContainer()
 * }
 * ```
 */
inline fun <reified S : MVIState, reified I : MVIIntent, reified A : MVIAction> org.koin.core.Koin.getContainer(): Container<S, I, A> {
    return get()
}

/**
 * Extension: Inject a container into a component.
 * 
 * Usage:
 * ```kotlin
 * class MyClass : KoinComponent {
 *     private val container: ProfileContainer by injectContainer()
 * }
 * ```
 */
inline fun <reified S : MVIState, reified I : MVIIntent, reified A : MVIAction> org.koin.core.component.KoinComponent.injectContainer(): Lazy<Container<S, I, A>> {
    return inject<Container<S, I, A>>()
}
