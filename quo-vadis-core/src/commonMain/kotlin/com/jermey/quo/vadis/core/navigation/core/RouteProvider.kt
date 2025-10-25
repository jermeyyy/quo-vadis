package com.jermey.quo.vadis.core.navigation.core

import kotlin.reflect.KClass

/**
 * Registry for destination routes.
 * Used by KSP-generated code to register route mappings.
 * 
 * @suppress This is an internal API used by generated code.
 */
object RouteRegistry {
    private val routes = mutableMapOf<KClass<*>, String>()
    
    /**
     * Register a route for a destination class.
     */
    fun register(destinationClass: KClass<*>, route: String) {
        println("DEBUG: RouteRegistry.register - class=$destinationClass, route=$route")
        routes[destinationClass] = route
    }
    
    /**
     * Get the registered route for a destination class.
     */
    fun getRoute(destinationClass: KClass<*>): String? {
        val route = routes[destinationClass]
        println("DEBUG: RouteRegistry.getRoute - class=$destinationClass, route=$route, allRoutes=${routes.keys.map { it.simpleName }}")
        return route
    }
    
    /**
     * Debug helper to list all registered routes.
     */
    fun listAllRoutes(): Map<KClass<*>, String> = routes.toMap()
}

/**
 * Get the route for this destination.
 * First checks for KSP-registered routes, then falls back to manual implementation.
 */
fun Destination.getRouteOrDefault(): String {
    // Try to get from registry first (KSP-generated)
    return RouteRegistry.getRoute(this::class) ?: this.route
}
