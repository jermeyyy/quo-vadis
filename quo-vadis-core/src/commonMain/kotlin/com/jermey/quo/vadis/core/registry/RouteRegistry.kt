package com.jermey.quo.vadis.core.registry

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
        val allRoutes = routes.keys.map { it.simpleName }
        println("DEBUG: RouteRegistry.getRoute - class=$destinationClass, route=$route, allRoutes=$allRoutes")
        return route
    }

}
