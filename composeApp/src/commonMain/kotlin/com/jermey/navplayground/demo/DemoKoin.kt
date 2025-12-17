package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.ui.screens.profile.ProfileContainer
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection for the demo app.
 * 
 * Call this before any composables that use koinInject().
 */
fun initKoin() {
    startKoin {
        modules(
            navigationModule,
            profileModule,
            resultDemoModule
        )
    }
}

/**
 * Navigation module - provides Navigator singleton.
 */
val navigationModule = module {
    single<Navigator> { TreeNavigator() }
}

/**
 * Profile feature module - demonstrates FlowMVI with Koin.
 */
val profileModule = module {
    // Repository
    single { ProfileRepository() }
    
    // Container (FlowMVI store)
    single {
        ProfileContainer(
            navigator = get(),
            repository = get(),
            debuggable = true // Enable debug logging
        )
    }
}

/**
 * Result Demo module - provides dependencies for navigation result demo.
 *
 * Note: ResultDemoContainer and ItemPickerContainer are created directly in screens
 * using `remember` to scope their lifecycle to composition. They require:
 * - navigator: Injected via koinInject()
 * - coroutineScope: Created via rememberCoroutineScope()
 * - screenKey: Obtained from LocalScreenNode.current?.key
 */
val resultDemoModule = module {
    // Dependencies used by result demo containers are provided by navigationModule
}
