package com.jermey.quo.vadis.ksp

import com.jermey.quo.vadis.core.navigation.compose.animation.TransitionScope
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.compose.registry.RouteRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
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
    val NAVIGATION_TRANSITION: ClassName = NavigationTransition::class.toClassName()
    val ROUTE_REGISTRY: ClassName = RouteRegistry::class.toClassName()
    val NAV_DESTINATION: ClassName = NavDestination::class.toClassName()
    val SCREEN_REGISTRY: ClassName = ScreenRegistry::class.toClassName()
    val GENERATED_DEEP_LINK_HANDLER: ClassName = GeneratedDeepLinkHandler::class.toClassName()
    val DEEP_LINK_RESULT: ClassName = DeepLinkResult::class.toClassName()
    val SCOPE_REGISTRY: ClassName = ScopeRegistry::class.toClassName()
    
    // Compose classes
    val TRANSITION_SCOPE: ClassName = TransitionScope::class.toClassName()
    
    // Container registry classes (unified container building + wrapper rendering)
    val CONTAINER_REGISTRY: ClassName = ContainerRegistry::class.toClassName()
    val CONTAINER_INFO: ClassName = ContainerInfo::class.toClassName()
    val CONTAINER_INFO_TAB_CONTAINER: ClassName = ContainerInfo.TabContainer::class.toClassName()
    val CONTAINER_INFO_PANE_CONTAINER: ClassName = ContainerInfo.PaneContainer::class.toClassName()
    val PANE_ROLE: ClassName = PaneRole::class.toClassName()
    val TABS_CONTAINER_SCOPE: ClassName = TabsContainerScope::class.toClassName()
    val PANE_CONTAINER_SCOPE: ClassName = PaneContainerScope::class.toClassName()
    
    // Annotation classes (for KSP processing)
    val ARGUMENT_ANNOTATION: ClassName = ClassName(
        "com.jermey.quo.vadis.annotations",
        "Argument"
    )
    
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
