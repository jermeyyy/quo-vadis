package com.jermey.navplayground.demo

import com.jermey.feature1.resultdemo.container.ItemPickerContainer
import com.jermey.feature1.resultdemo.container.ResultDemoContainer
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.screens.profile.ProfileContainer
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.navplayground.demo.ui.screens.statedriven.StateDrivenContainer
import com.jermey.navplayground.demo.ui.screens.tabs.DemoTabsContainer
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.tree.TreeNavigator
import com.jermey.quo.vadis.flowmvi.navigationContainer
import com.jermey.quo.vadis.flowmvi.sharedNavigationContainer
import com.jermey.quo.vadis.generated.ComposeAppNavigationConfig
import com.jermey.quo.vadis.generated.Feature1NavigationConfig
import com.jermey.quo.vadis.generated.Feature2NavigationConfig
import org.koin.core.component.get
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
        // Config is now passed directly - navigator derives registries from it
        TreeNavigator(
            config = navigationConfig,
            initialState = initialState
        )
    }
}

val profileModule = module {

    single { ProfileRepository() }

    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(
            scope = scope,
            repository = scope.get(),
            debuggable = true
        )
    }
}


val resultDemoModule = module {
    navigationContainer<ResultDemoContainer> { scope ->
        ResultDemoContainer(scope)
    }
    navigationContainer<ItemPickerContainer> { scope ->
        ItemPickerContainer(scope)
    }
}

/**
 * Koin module for the State-Driven Navigation Demo shared container.
 */
val stateDrivenDemoModule = module {
    sharedNavigationContainer<StateDrivenContainer> { scope ->
        StateDrivenContainer(scope)
    }
}

/**
 * Koin module for the Demo Tabs shared container.
 *
 * Registers [DemoTabsContainer] as a shared navigation container,
 * allowing cross-tab state sharing within the DemoTabs.
 */
val tabsDemoModule = module {
    sharedNavigationContainer<DemoTabsContainer> { scope ->
        DemoTabsContainer(scope)
    }
}
