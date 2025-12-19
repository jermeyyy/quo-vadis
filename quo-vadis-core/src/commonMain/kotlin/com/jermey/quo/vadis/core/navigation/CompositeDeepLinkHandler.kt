package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Composite deep link handler that delegates to secondary first, then primary.
 */
internal class CompositeDeepLinkHandler(
    private val primary: GeneratedDeepLinkHandler,
    private val secondary: GeneratedDeepLinkHandler
) : GeneratedDeepLinkHandler {

    override fun handleDeepLink(uri: String): DeepLinkResult {
        val secondaryResult = secondary.handleDeepLink(uri)
        if (secondaryResult is DeepLinkResult.Matched) {
            return secondaryResult
        }
        return primary.handleDeepLink(uri)
    }

    override fun createDeepLinkUri(destination: NavDestination, scheme: String): String? {
        return secondary.createDeepLinkUri(destination, scheme)
            ?: primary.createDeepLinkUri(destination, scheme)
    }

    override fun resolve(deepLink: DeepLink): NavDestination? {
        return secondary.resolve(deepLink) ?: primary.resolve(deepLink)
    }

    override fun register(
        pattern: String,
        action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    ) {
        // Register on both handlers to support full pattern matching
        secondary.register(pattern, action)
        primary.register(pattern, action)
    }

    override fun handle(deepLink: DeepLink, navigator: Navigator) {
        // Try secondary first, then primary
        val secondaryResolved = secondary.resolve(deepLink)
        if (secondaryResolved != null) {
            secondary.handle(deepLink, navigator)
        } else {
            primary.handle(deepLink, navigator)
        }
    }
}