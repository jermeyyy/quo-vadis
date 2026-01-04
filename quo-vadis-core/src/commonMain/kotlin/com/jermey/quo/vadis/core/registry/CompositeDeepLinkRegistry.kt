package com.jermey.quo.vadis.core.registry

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Composite registry that combines a generated registry with a runtime registry.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Composite registries are managed internally by the navigation system.
 *
 * The runtime registry is checked first, allowing runtime registrations to
 * override generated patterns.
 *
 * @param generated The generated registry from KSP (nullable for apps without generated handlers)
 * @param runtime The runtime registry for dynamic registrations
 */
@InternalQuoVadisApi
class CompositeDeepLinkRegistry(
    private val generated: DeepLinkRegistry?,
    private val runtime: RuntimeDeepLinkRegistry = RuntimeDeepLinkRegistry()
) : DeepLinkRegistry {

    override fun resolve(uri: String): NavDestination? {
        // Runtime takes precedence
        return runtime.resolve(uri) ?: generated?.resolve(uri)
    }

    override fun resolve(deepLink: DeepLink): NavDestination? {
        return runtime.resolve(deepLink) ?: generated?.resolve(deepLink)
    }

    override fun register(pattern: String, factory: (params: Map<String, String>) -> NavDestination) {
        runtime.register(pattern, factory)
    }

    override fun registerAction(pattern: String, action: (navigator: Navigator, params: Map<String, String>) -> Unit) {
        runtime.registerAction(pattern, action)
    }

    override fun handle(uri: String, navigator: Navigator): Boolean {
        // Try runtime first
        if (runtime.handle(uri, navigator)) {
            return true
        }

        // Then try generated
        val destination = generated?.resolve(uri)
        if (destination != null) {
            navigator.navigate(destination)
            return true
        }

        return false
    }

    override fun createUri(destination: NavDestination, scheme: String): String? {
        // Generated registry has the route mappings
        return generated?.createUri(destination, scheme)
    }

    override fun canHandle(uri: String): Boolean {
        return runtime.canHandle(uri) || (generated?.canHandle(uri) == true)
    }

    override fun getRegisteredPatterns(): List<String> {
        val runtimePatterns = runtime.getRegisteredPatterns()
        val generatedPatterns = generated?.getRegisteredPatterns() ?: emptyList()
        return runtimePatterns + generatedPatterns
    }
}