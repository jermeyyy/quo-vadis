package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.destinations.DeepLinkDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.tabs.MainTabs
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Sets up deep link handlers for the demo application.
 *
 * Registers URI patterns that can be used to navigate directly to specific screens.
 * 
 * Note: This function is currently not used in the tabbed navigation setup.
 * Deep links would need to be registered differently for the tab-based architecture.
 */
fun setupDemoDeepLinks(navigator: Navigator) {
    val handler = navigator.getDeepLinkHandler()

    handler.register("app://demo/home") { _, nav, _ ->
        nav.navigate(MainTabs.HomeTab)
    }

    handler.register("app://demo/item/{id}") { _, nav, params ->
        nav.navigate(MasterDetailDestination.Detail(params["id"] ?: "unknown"))
    }

    handler.register("app://demo/process/start") { _, nav, _ ->
        nav.navigate(ProcessDestination.Start)
    }

    handler.register("app://demo/tabs") { _, nav, _ ->
        nav.navigate(TabsDestination.Main)
    }

    handler.register("app://demo/settings") { _, nav, _ ->
        nav.navigate(MainTabs.SettingsTab.Main)
    }

    handler.register("app://demo/deeplink") { _, nav, _ ->
        nav.navigate(DeepLinkDestination.Demo)
    }
}

