package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.compose.registry.RouteRegistry

/**
 * Marker interface for navigation destinations.
 * 
 * The actual route resolution is handled internally by the library via RouteRegistry.
 * 
 * KSP generates the necessary code to register routes, so destinations remain simple POJOs.
 */
interface Destination {
    /**
     * Optional data passed to this destination.
     * Should be a serializable type when using TypedDestination.
     * Use null for destinations without data.
     */
    val data: Any? get() = null

    /**
     * Default transition when navigating TO this destination.
     * If null, uses NavHost's default transition.
     */
    val transition: NavigationTransition? get() = null
}

/**
 * Extension to get the route for any Destination.
 * Routes are resolved from RouteRegistry (populated by KSP-generated code).
 */
val Destination.route: String
    get() {
        val kClass = this::class
        val route = RouteRegistry.getRoute(kClass)
        println("DEBUG: Destination.route - class=$kClass, simpleName=${kClass.simpleName}, route=$route")
        return route ?: error("Route not registered for ${kClass.simpleName}. Ensure @Route annotation is present.")
    }


