package com.jermey.quo.vadis.ksp

import com.jermey.quo.vadis.core.navigation.compose.TransitionScope
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationGraphBuilder
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.RouteRegistry
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

/**
 * Type-safe references to Quo Vadis Core API classes.
 * 
 * This object provides compile-time safe references to core navigation classes,
 * ensuring that refactoring in quo-vadis-core is automatically reflected in the KSP processor.
 */
internal object QuoVadisClassNames {
    
    // Core navigation classes
    val NAVIGATOR: ClassName = Navigator::class.toClassName()
    val NAVIGATION_GRAPH: ClassName = NavigationGraph::class.toClassName()
    val NAVIGATION_GRAPH_BUILDER: ClassName = NavigationGraphBuilder::class.toClassName()
    val NAVIGATION_TRANSITION: ClassName = NavigationTransition::class.toClassName()
    val ROUTE_REGISTRY: ClassName = RouteRegistry::class.toClassName()
    
    // Compose classes
    val TRANSITION_SCOPE: ClassName = TransitionScope::class.toClassName()
    
    /**
     * Convert KClass to KotlinPoet ClassName.
     */
    private fun KClass<*>.toClassName(): ClassName {
        val qualifiedName = this.qualifiedName 
            ?: throw IllegalArgumentException("Cannot get qualified name for $this")
        val packageName = qualifiedName.substringBeforeLast('.', "")
        val simpleNames = qualifiedName.substringAfterLast('.').split('.')
        return ClassName(packageName, simpleNames)
    }
}
