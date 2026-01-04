package com.jermey.quo.vadis.ksp

import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.compose.transition.TransitionScope
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.registry.ContainerInfo
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.registry.RouteRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.navigation.destination.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
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
    val NAVIGATION_CONFIG: ClassName = NavigationConfig::class.toClassName()
    val NAVIGATION_TRANSITION: ClassName = NavigationTransition::class.toClassName()
    val ROUTE_REGISTRY: ClassName = RouteRegistry::class.toClassName()
    val NAV_DESTINATION: ClassName = NavDestination::class.toClassName()
    val SCREEN_REGISTRY: ClassName = ScreenRegistry::class.toClassName()
    val DEEP_LINK_REGISTRY: ClassName = DeepLinkRegistry::class.toClassName()
    val DEEP_LINK_RESULT: ClassName = DeepLinkResult::class.toClassName()
    val SCOPE_REGISTRY: ClassName = ScopeRegistry::class.toClassName()
    val TRANSITION_REGISTRY: ClassName = TransitionRegistry::class.toClassName()
    val PANE_ROLE_REGISTRY: ClassName = PaneRoleRegistry::class.toClassName()
    
    // Deep linking classes
    val DEEP_LINK: ClassName = DeepLink::class.toClassName()
    val DEEP_LINK_RESULT_MATCHED: ClassName = DEEP_LINK_RESULT.nestedClass("Matched")
    val DEEP_LINK_RESULT_NOT_MATCHED: ClassName = DEEP_LINK_RESULT.nestedClass("NotMatched")
    
    // NavNode tree
    val NAV_NODE: ClassName = NavNode::class.toClassName()
    
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
    
    // Compose animation classes (from AndroidX - use hardcoded names)
    val COMPOSABLE: ClassName = ClassName("androidx.compose.runtime", "Composable")
    val SHARED_TRANSITION_SCOPE: ClassName = ClassName("androidx.compose.animation", "SharedTransitionScope")
    val ANIMATED_VISIBILITY_SCOPE: ClassName = ClassName("androidx.compose.animation", "AnimatedVisibilityScope")
    
    // Kotlin standard library classes
    val KCLASS: ClassName = ClassName("kotlin.reflect", "KClass")
    
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
