package com.jermey.navplayground.demo

import com.jermey.feature1.resultdemo.Feature1Module
import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.NavigationRoot
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.config.navigationConfig
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.generated.initQuoVadisNavigation
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@KoinApplication(
    modules = [
        NavigationModule::class,
        StateDrivenDemoModule::class,
        TabsDemoModule::class,
        ProfileModule::class,
        ExploreModule::class,
        Feature1Module::class,
    ]
)
object NavPlaygroundKoinApp

@NavigationRoot
object AppNavigation

@Module
class NavigationModule {

    @Single
    fun navigationConfig(): NavigationConfig {
        // Required for KSP backend: forces the generated config object to initialize,
        // registering itself into NavigationConfigRegistry. When using the compiler plugin
        // backend this is a harmless no-op.
        initQuoVadisNavigation()
        return navigationConfig<AppNavigation>()
    }

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
