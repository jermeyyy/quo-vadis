package com.jermey.navplayground.demo

import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Sets up deep link handlers for the demo application.
 *
 * Registers URI patterns that can be used to navigate directly to specific screens.
 */
fun setupDemoDeepLinks(navigator: Navigator) {
    val handler = navigator.getDeepLinkHandler()

    handler.register("app://demo/home") { _ ->
        MainDestination.Home
    }

    handler.register("app://demo/item/{id}") { params ->
        MasterDetailDestination.Detail(params["id"] ?: "unknown")
    }

    handler.register("app://demo/process/start") { _ ->
        ProcessDestination.Start
    }

    handler.register("app://demo/tabs") { _ ->
        TabsDestination.Main
    }

    handler.register("app://demo/settings") { _ ->
        MainDestination.Settings
    }

    handler.register("app://demo/deeplink") { _ ->
        MainDestination.DeepLinkDemo
    }
}

