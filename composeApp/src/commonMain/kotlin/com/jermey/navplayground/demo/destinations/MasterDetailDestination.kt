package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * Master-Detail pattern destinations.
 *
 * ANNOTATION PATTERN: Destinations with Route Arguments
 *
 * The Detail destination demonstrates:
 * - @Argument annotation on constructor parameters
 * - Route template with parameter placeholders
 * - Type-safe argument passing
 *
 * Generated code includes:
 * - Typed extension: navigator.navigateToDetail(itemId = "123")
 * - Automatic route parameter extraction
 * - Content function receives arguments directly (see ContentDefinitions.kt)
 */
@Stack(name = "master_detail", startDestination = MasterDetailDestination.List::class)
sealed class MasterDetailDestination : NavDestination {
    @Destination(route = "master_detail/list")
    data object List : MasterDetailDestination()

    /**
     * Detail destination with route argument.
     *
     * KSP generates:
     * ```kotlin
     * fun Navigator.navigateToDetail(
     *     itemId: String,
     *     transition: NavigationTransition? = null
     * )
     * ```
     */
    @Destination(route = "master_detail/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String
    ) : MasterDetailDestination()
}
