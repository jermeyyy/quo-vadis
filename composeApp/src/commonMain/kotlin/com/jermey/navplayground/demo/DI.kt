package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MainTabs
import com.jermey.quo.vadis.annotations.NavigationRoot
import com.jermey.quo.vadis.core.navigation.config.navigationConfig
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@NavigationRoot
object AppNavigation

@Module
class NavigationModule {

    @Single
    fun navigator(): Navigator {
        val navigationConfig = navigationConfig<AppNavigation>()
        val rootDestination = MainTabs::class

        val initialState = navigationConfig.buildNavNode(
            destinationClass = rootDestination,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                    "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                    "or manually registered in the NavigationConfig."
        )
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
@ComponentScan("com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.ui.screens.profile")
class ProfileModule

@Module
@ComponentScan("com.jermey.navplayground.demo.ui.screens.explore")
class ExploreModule
