package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class or interface as a stack-based navigation container.
 *
 * Stack navigation provides linear, push/pop behavior where destinations
 * are added to and removed from a stack. Back navigation pops the top
 * destination, revealing the previous one.
 *
 * ## Usage
 *
 * Apply to a sealed class containing destination subclasses:
 * ```kotlin
 * @Stack(name = "home", startDestination = "Feed")
 * sealed class HomeDestination {
 *
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 *
 *     @Destination(route = "home/detail/{id}")
 *     data class Detail(val id: String) : HomeDestination()
 * }
 * ```
 *
 * ## Sealed Class Requirements
 *
 * - Must be a `sealed class` or `sealed interface`
 * - All direct subclasses must be annotated with [@Destination]
 * - Subclasses can be `data object` (no params) or `data class` (with params)
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [StackNode] in the NavNode hierarchy:
 * ```
 * @Stack → StackNode(
 *     key = "{name}-stack",
 *     children = [ScreenNode for each @Destination subclass]
 * )
 * ```
 *
 * ## Start Destination Resolution
 *
 * The start destination is resolved with the following priority:
 * 1. Type-safe [startDestinationClass] (if not [Unit])
 * 2. String-based [startDestination] (if not empty)
 * 3. First destination in declaration order
 *
 * If [startDestination] is empty, the first destination in declaration
 * order is used as the initial screen.
 *
 * ## Examples
 *
 * ### Basic Stack Navigation
 * ```kotlin
 * @Stack(name = "home", startDestination = "Feed")
 * sealed class HomeDestination {
 *
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 *
 *     @Destination(route = "home/detail/{id}")
 *     data class Detail(val id: String) : HomeDestination()
 *
 *     @Destination(route = "home/settings")
 *     data object Settings : HomeDestination()
 * }
 * ```
 *
 * ### Type-Safe Start Destination
 * ```kotlin
 * @Stack(name = "home", startDestinationClass = HomeDestination.Feed::class)
 * sealed class HomeDestination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 * }
 * ```
 *
 * ### Stack with Default Start Destination
 * ```kotlin
 * // First destination (Overview) is used as start when startDestination is empty
 * @Stack(name = "profile")
 * sealed class ProfileDestination {
 *
 *     @Destination(route = "profile/overview")
 *     data object Overview : ProfileDestination()  // ← Default start
 *
 *     @Destination(route = "profile/edit")
 *     data object Edit : ProfileDestination()
 *
 *     @Destination(route = "profile/settings")
 *     data object Settings : ProfileDestination()
 * }
 * ```
 *
 * @property name Unique name for this navigation stack. Used for key
 *   generation, debugging, and identification in the navigation tree.
 * @property startDestination Simple class name of the initial destination
 *   (e.g., "Feed" matches `data object Feed`). If empty and [startDestinationClass]
 *   is [Unit], the first destination is used.
 * @property startDestinationClass Type-safe KClass reference to the initial destination.
 *   If not [Unit] (the default), this takes precedence over [startDestination].
 *   Use `MyDestination.Feed::class` for type-safe start destination resolution.
 *
 * @see Destination
 * @see Tabs
 * @see Pane
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Stack(
    val name: String,
    val startDestination: String = "",
    val startDestinationClass: KClass<*> = Unit::class
)
