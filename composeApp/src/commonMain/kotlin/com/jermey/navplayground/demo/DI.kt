package com.jermey.navplayground.demo

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.generated.ComposeAppNavigationConfig
import com.jermey.quo.vadis.generated.Feature1NavigationConfig
import com.jermey.quo.vadis.generated.NavigationApiNavigationConfig
import com.jermey.quo.vadis.generated.Feature2ApiNavigationConfig
import com.jermey.quo.vadis.generated.Feature2NavigationConfig
import com.jermey.quo.vadis.generated.Feature3NavigationConfig
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class NavigationModule {

    @Single
    fun navigationConfig(): NavigationConfig =
        ComposeAppNavigationConfig +
                NavigationApiNavigationConfig +
                Feature1NavigationConfig +
                Feature2ApiNavigationConfig +
                Feature2NavigationConfig +
                Feature3NavigationConfig

    @Single
    fun navigator(navigationConfig: NavigationConfig): Navigator {
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
        return TreeNavigator(
            config = navigationConfig,
            initialState = initialState
        )
    }
}



@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.statedriven")
class StateDrivenDemoModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.tabs")
class TabsDemoModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.backhandler")
class BackHandlerDemoModule
