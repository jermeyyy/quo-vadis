package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.generated.ComposeAppNavigationConfig
import com.jermey.quo.vadis.generated.Feature1NavigationConfig
import com.jermey.quo.vadis.generated.Feature2NavigationConfig
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
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
}

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.statedriven")
class StateDrivenDemoModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.tabs")
class TabsDemoModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.profile")
class ProfileModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.explore")
class ExploreModule
