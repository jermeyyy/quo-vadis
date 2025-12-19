package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.container.ItemPickerContainer
import com.jermey.navplayground.demo.container.ResultDemoContainer
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.screens.profile.ProfileContainer
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.quo.vadis.core.navigation.core.DefaultDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.generated.GeneratedNavigationConfig
import org.koin.dsl.module

val navigationModule = module {
    single<Navigator> {
        val config = GeneratedNavigationConfig
        val rootDestination = MainTabs::class

        val initialState = config.buildNavNode(
            destinationClass = rootDestination,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                    "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                    "or manually registered in the NavigationConfig."
        )
        TreeNavigator(
            initialState = initialState,
            scopeRegistry = config.scopeRegistry,
            containerRegistry = config.containerRegistry,
            deepLinkHandler = config.deepLinkHandler ?: DefaultDeepLinkHandler(),
        )
    }
}

val profileModule = module {

    single { ProfileRepository() }

    single {
        ProfileContainer(
            navigator = get(),
            repository = get(),
            debuggable = true
        )
    }
}


val resultDemoModule = module {
    container<ResultDemoContainer> {
        ResultDemoContainer(
            navigator = get(),
            screenKey = id
        )
    }
    container<ItemPickerContainer> {
        ItemPickerContainer(
            navigator = get(),
            screenKey = id
        )
    }
}
