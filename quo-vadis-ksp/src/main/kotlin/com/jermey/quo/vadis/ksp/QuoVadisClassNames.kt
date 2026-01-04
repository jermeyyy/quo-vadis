package com.jermey.quo.vadis.ksp

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
    val NAVIGATOR: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.navigator", "Navigator")
    val NAVIGATION_CONFIG: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.config", "NavigationConfig")
    val NAVIGATION_TRANSITION: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.transition", "NavigationTransition")
    val ROUTE_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "RouteRegistry")
    val NAV_DESTINATION: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.destination", "NavDestination")
    val SCREEN_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ScreenRegistry")
    val DEEP_LINK_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "DeepLinkRegistry")
    val DEEP_LINK_RESULT: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.destination", "DeepLinkResult")
    val SCOPE_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ScopeRegistry")
    val TRANSITION_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "TransitionRegistry")
    val PANE_ROLE_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "PaneRoleRegistry")
    
    // Deep linking classes
    val DEEP_LINK: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.destination", "DeepLink")
    val DEEP_LINK_RESULT_MATCHED: ClassName = DEEP_LINK_RESULT.nestedClass("Matched")
    val DEEP_LINK_RESULT_NOT_MATCHED: ClassName = DEEP_LINK_RESULT.nestedClass("NotMatched")
    
    // NavNode tree
    val NAV_NODE: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.node", "NavNode")
    
    // Compose classes
    val TRANSITION_SCOPE: ClassName = ClassName("com.jermey.quo.vadis.core.compose.animation", "TransitionScope")
    
    // Container registry classes (unified container building + wrapper rendering)
    val CONTAINER_REGISTRY: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ContainerRegistry")
    val CONTAINER_INFO: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ContainerInfo")
    val CONTAINER_INFO_TAB_CONTAINER: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ContainerInfo.TabContainer")
    val CONTAINER_INFO_PANE_CONTAINER: ClassName = ClassName("com.jermey.quo.vadis.core.registry", "ContainerInfo.PaneContainer")
    val PANE_ROLE: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.pane", "PaneRole")
    val TABS_CONTAINER_SCOPE: ClassName = ClassName("com.jermey.quo.vadis.core.compose.scope", "TabsContainerScope")
    val PANE_CONTAINER_SCOPE: ClassName = ClassName("com.jermey.quo.vadis.core.compose.scope", "PaneContainerScope")
    
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
}
