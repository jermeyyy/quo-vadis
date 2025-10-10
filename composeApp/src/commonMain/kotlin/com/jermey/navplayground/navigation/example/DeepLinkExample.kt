package com.jermey.navplayground.navigation.example

import com.jermey.navplayground.navigation.core.*

/**
 * Example of deep link configuration and usage.
 */
object DeepLinkExample {

    /**
     * Setup deep link handlers for the application.
     */
    fun setupDeepLinks(navigator: Navigator) {
        val handler = DefaultDeepLinkHandler()

        // Register deep link patterns

        // Pattern: app://home
        handler.register("app://home") { _ ->
            SampleDestinations.Home
        }

        // Pattern: app://product/{id}
        handler.register("app://product/{id}") { params ->
            SampleDestinations.Details(params["id"] ?: "")
        }

        // Pattern: app://user/{userId}/profile
        handler.register("app://user/{userId}/profile") { params ->
            SampleDestinations.Profile(params["userId"] ?: "")
        }

        // Pattern: app://settings
        handler.register("app://settings") { _ ->
            SampleDestinations.Settings
        }

        // You can also use the handler with the navigator
        // navigator.handleDeepLink(DeepLink.parse("app://product/123"))
    }

    /**
     * Example of handling deep links from different sources.
     */
    fun handleUniversalLink(url: String, navigator: Navigator) {
        // Convert universal link to deep link
        val deepLink = when {
            url.startsWith("https://myapp.com/product/") -> {
                val id = url.substringAfterLast("/")
                DeepLink("app://product/$id")
            }
            url.startsWith("https://myapp.com/user/") -> {
                val parts = url.removePrefix("https://myapp.com/user/").split("/")
                if (parts.size >= 2 && parts[1] == "profile") {
                    DeepLink("app://user/${parts[0]}/profile")
                } else null
            }
            else -> null
        }

        deepLink?.let { navigator.handleDeepLink(it) }
    }

    /**
     * Example of deep link with query parameters.
     */
    fun handleDeepLinkWithParams() {
        val deepLink = DeepLink.parse("app://product/123?source=email&campaign=summer")

        // Access parameters
        val source = deepLink.parameters["source"] // "email"
        val campaign = deepLink.parameters["campaign"] // "summer"

        // The uri will be "app://product/123"
        // Parameters can be used for analytics or conditional navigation
    }
}

/**
 * Example navigation graph with deep link support.
 */
fun createDeepLinkEnabledGraph(): NavigationGraph {
    return navigationGraph("deeplink_example") {
        startDestination(SampleDestinations.Home)

        // Using the deepLinkDestination helper
        deepLinkDestination(
            route = "home",
            deepLinkPattern = "app://home"
        ) { _, navigator ->
            HomeScreen(navigator)
        }

        deepLinkDestination(
            route = "details",
            deepLinkPattern = "app://product/{id}"
        ) { dest, navigator ->
            val itemId = dest.arguments["id"] as? String ?: ""
            DetailsScreen(itemId, navigator)
        }

        deepLinkDestination(
            route = "profile",
            deepLinkPattern = "app://user/{userId}/profile"
        ) { dest, navigator ->
            val userId = dest.arguments["userId"] as? String ?: ""
            ProfileScreen(userId, navigator)
        }
    }
}

