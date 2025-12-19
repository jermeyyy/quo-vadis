package com.jermey.navplayground.demo

import com.jermey.feature1.resultdemo.container.ItemPickerContainer
import com.jermey.feature1.resultdemo.container.ResultDemoContainer
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.screens.profile.ProfileContainer
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.core.DefaultDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.flowmvi.container
import com.jermey.quo.vadis.generated.ComposeAppNavigationConfig
import com.jermey.quo.vadis.generated.Feature1NavigationConfig
import com.jermey.quo.vadis.generated.Feature2NavigationConfig
import org.koin.dsl.module

val navigationModule = module {
    single<NavigationConfig> {
        ComposeAppNavigationConfig +
                Feature1NavigationConfig +
                Feature2NavigationConfig

    }
    single<Navigator> {
        val navigationConfig = get<NavigationConfig>()
        val rootDestination = MainTabs::class

        val initialState = navigationConfig.buildNavNode(
            destinationClass = rootDestination,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                    "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                    "or manually registered in the NavigationConfig."
        )
        TreeNavigator(
            initialState = initialState,
            scopeRegistry = navigationConfig.scopeRegistry,
            containerRegistry = navigationConfig.containerRegistry,
            deepLinkHandler = navigationConfig.deepLinkHandler ?: DefaultDeepLinkHandler(),
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
