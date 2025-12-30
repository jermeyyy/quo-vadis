package com.jermey.quo.vadis.annotations

/**
 * Marks a class or object as a navigation destination.
 *
 * Destinations represent individual screens or views in the navigation graph.
 * When processed by KSP, each destination becomes a [ScreenNode] in the
 * navigation tree.
 *
 * ## Usage
 *
 * Apply to data objects for parameter-less destinations:
 * ```kotlin
 * @Destination(route = "home")
 * data object Home : HomeDestination()
 * ```
 *
 * Apply to data classes for destinations with parameters:
 * ```kotlin
 * @Destination(route = "profile/{userId}")
 * data class Profile(val userId: String) : HomeDestination()
 * ```
 *
 * ## Deep Linking
 *
 * The [route] parameter enables deep linking support:
 * - Path parameters: `"profile/{userId}"` extracts `userId` from the URI
 * - Query parameters: `"search?query={q}"` extracts `q` from query string
 * - Empty route means the destination is not deep-linkable
 *
 * ## Pane Navigation
 *
 * When inside a [@Pane] container, use [paneRole] to specify which pane
 * this destination belongs to. This enables transparent navigation where
 * `navigate(destination)` automatically routes to the correct pane's stack.
 *
 * ```kotlin
 * @Pane(name = "messages")
 * sealed class MessagesPane : NavDestination {
 *     @PaneItem(role = PaneRole.PRIMARY)
 *     @Destination(route = "messages/list")
 *     data object List : MessagesPane()
 *
 *     @PaneItem(role = PaneRole.SECONDARY)
 *     @Destination(route = "messages/empty")
 *     data object Empty : MessagesPane()
 *
 *     // Non-root destination that belongs to SECONDARY pane
 *     @Destination(route = "messages/detail/{id}", paneRole = PaneRole.SECONDARY)
 *     data class Detail(val id: String) : MessagesPane()
 * }
 * ```
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [ScreenNode] in the NavNode hierarchy:
 * ```
 * @Destination → ScreenNode(destination = <instance>)
 * ```
 *
 * ## Route Patterns
 *
 * ### Simple Route
 * ```kotlin
 * @Destination(route = "home")
 * data object Home : AppDestination()
 * ```
 *
 * ### Path Parameter
 * ```kotlin
 * @Destination(route = "article/{articleId}")
 * data class Article(val articleId: String) : AppDestination()
 * ```
 *
 * ### Multiple Path Parameters
 * ```kotlin
 * @Destination(route = "user/{userId}/post/{postId}")
 * data class UserPost(val userId: String, val postId: String) : AppDestination()
 * ```
 *
 * ### Query Parameters
 * ```kotlin
 * @Destination(route = "search?query={q}&filter={f}")
 * data class Search(val q: String, val f: String? = null) : AppDestination()
 * ```
 *
 * ### Not Deep-Linkable
 * ```kotlin
 * @Destination
 * data object InternalScreen : AppDestination()
 * ```
 *
 * @property route Route path for deep linking. Supports path parameters
 *   (`{param}`) and query parameters (`?key={value}`). If empty, the
 *   destination is not accessible via deep links.
 *
 * @see Stack
 * @see Tabs
 * @see Pane
 * @see PaneItem
 * @see PaneRole
 * @see Screen
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Destination(
    val route: String = "",
)
