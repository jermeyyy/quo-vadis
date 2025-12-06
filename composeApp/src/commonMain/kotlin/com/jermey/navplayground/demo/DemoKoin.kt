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
            profileModule
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
