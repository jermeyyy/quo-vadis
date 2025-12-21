package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * Promo destination for runtime-registered deep links.
 *
 * ANNOTATION PATTERN: Runtime Deep Link Registration Target
 *
 * This destination is NOT registered via KSP (no route in @Destination).
 * Instead, it's registered at runtime via:
 *
 * ```kotlin
 * navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
 *     PromoDestination(code = params["code"]!!)
 * }
 * ```
 *
 * This demonstrates:
 * - Destinations can exist without compile-time deep link routes
 * - Runtime registration via `DeepLinkRegistry.register()`
 * - Dynamic deep link handling for promotional/marketing campaigns
 *
 * @property code The promotional code from the deep link
 *
 * @see com.jermey.quo.vadis.core.navigation.core.DeepLinkRegistry.register
 */
@Destination
data class PromoDestination(
    @Argument val code: String
) : NavDestination
