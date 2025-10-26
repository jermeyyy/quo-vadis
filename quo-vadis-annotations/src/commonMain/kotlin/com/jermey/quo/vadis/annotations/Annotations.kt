package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class as a navigation graph.
 * 
 * The sealed class should extend [Destination] and contain destination objects/classes
 * representing the screens in this graph. KSP will generate:
 * - Route registration code (`{ClassName}RouteInitializer`)
 * - Graph builder function (`build{ClassName}Graph()`)
 * - Typed destination extensions (for destinations with [@Argument])
 * 
 * @param name The unique identifier for this navigation graph. Used in generated function names.
 * 
 * @sample Basic graph with simple destinations
 * ```kotlin
 * @Graph("main")
 * sealed class MainDestination : Destination {
 *     @Route("main/home")
 *     data object Home : MainDestination()
 *     
 *     @Route("main/settings")
 *     data object Settings : MainDestination()
 * }
 * 
 * // Generated: buildMainDestinationGraph()
 * ```
 * 
 * @sample Graph with typed destinations
 * ```kotlin
 * @Serializable
 * data class DetailData(val itemId: String)
 * 
 * @Graph("feature")
 * sealed class FeatureDestination : Destination {
 *     @Route("feature/list")
 *     data object List : FeatureDestination()
 *     
 *     @Route("feature/detail")
 *     @Argument(DetailData::class)
 *     data class Detail(val itemId: String) 
 *         : FeatureDestination(), TypedDestination<DetailData> {
 *         override val data = DetailData(itemId)
 *     }
 * }
 * ```
 * 
 * @see Route
 * @see Argument
 * @see Content
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String)

/**
 * Specifies the route path for a destination.
 * 
 * The route is used for navigation, deep linking, and automatic route registration.
 * Routes are automatically registered via generated `{GraphName}RouteInitializer` object.
 * 
 * @param path The route path string. Can contain path segments (e.g., "feature/screen").
 *             For dynamic routes, register manually in destination's init block.
 * 
 * @sample Simple route
 * ```kotlin
 * @Route("home")
 * data object Home : MainDestination()
 * ```
 * 
 * @sample Hierarchical route
 * ```kotlin
 * @Route("shop/product/detail")
 * data object ProductDetail : ShopDestination()
 * ```
 * 
 * @sample With typed destination
 * ```kotlin
 * @Serializable
 * data class UserData(val userId: String)
 * 
 * @Route("profile/user")
 * @Argument(UserData::class)
 * data class UserProfile(val userId: String) 
 *     : ProfileDestination(), TypedDestination<UserData> {
 *     override val data = UserData(userId)
 * }
 * ```
 * 
 * @see Graph
 * @see Argument
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)

/**
 * Specifies typed, serializable arguments for a destination.
 * 
 * The destination must implement [TypedDestination]<T> and the data class must be
 * annotated with [@Serializable] from kotlinx.serialization. KSP generates a typed
 * destination extension function for automatic serialization/deserialization.
 * 
 * @param dataClass The [KClass] of the serializable data type. Must be annotated with @Serializable.
 * 
 * @sample Basic typed destination
 * ```kotlin
 * @Serializable
 * data class DetailData(val itemId: String, val mode: String = "view")
 * 
 * @Route("detail")
 * @Argument(DetailData::class)
 * data class Detail(val itemId: String, val mode: String = "view") 
 *     : Destination, TypedDestination<DetailData> {
 *     override val data = DetailData(itemId, mode)
 * }
 * 
 * // Content function receives typed data
 * @Content(Detail::class)
 * @Composable
 * fun DetailContent(data: DetailData, navigator: Navigator) {
 *     Text("Item: ${data.itemId}, Mode: ${data.mode}")
 * }
 * ```
 * 
 * @sample Complex typed destination
 * ```kotlin
 * @Serializable
 * data class FilterData(
 *     val categories: List<String> = emptyList(),
 *     val minPrice: Double? = null,
 *     val maxPrice: Double? = null,
 *     val sortBy: SortOption = SortOption.RELEVANCE
 * )
 * 
 * @Route("search/filter")
 * @Argument(FilterData::class)
 * data class FilterScreen(
 *     val categories: List<String> = emptyList(),
 *     val minPrice: Double? = null,
 *     val maxPrice: Double? = null,
 *     val sortBy: SortOption = SortOption.RELEVANCE
 * ) : SearchDestination(), TypedDestination<FilterData> {
 *     override val data = FilterData(categories, minPrice, maxPrice, sortBy)
 * }
 * ```
 * 
 * @see Route
 * @see Content
 * @see TypedDestination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<*>)

/**
 * Marks a Composable function as the content renderer for a specific destination.
 * 
 * KSP automatically generates graph builder code that wires this Composable function
 * to the specified destination. The function signature must match the destination type:
 * - **Simple destinations**: `@Composable (Navigator) -> Unit`
 * - **Typed destinations** (with [@Argument]): `@Composable (DataType, Navigator) -> Unit`
 * 
 * All [@Content] functions are included in the generated `build{GraphName}Graph()` function.
 * 
 * @param destination The [KClass] of the destination that this Composable renders.
 * 
 * @sample Simple destination content
 * ```kotlin
 * @Content(MainDestination.Home::class)
 * @Composable
 * fun HomeContent(navigator: Navigator) {
 *     HomeScreen(
 *         onNavigateToProfile = { userId ->
 *             navigator.navigate(ProfileDestination.User(userId))
 *         },
 *         onNavigateToSettings = {
 *             navigator.navigate(MainDestination.Settings)
 *         }
 *     )
 * }
 * ```
 * 
 * @sample Typed destination content
 * ```kotlin
 * @Serializable
 * data class ProfileData(val userId: String, val tab: String = "posts")
 * 
 * @Content(ProfileDestination.User::class)
 * @Composable
 * fun UserProfileContent(data: ProfileData, navigator: Navigator) {
 *     ProfileScreen(
 *         userId = data.userId,
 *         initialTab = data.tab,
 *         onEditProfile = {
 *             navigator.navigate(ProfileDestination.Edit(data.userId))
 *         },
 *         onBack = { navigator.navigateBack() }
 *     )
 * }
 * ```
 * 
 * @sample Generated graph builder usage
 * ```kotlin
 * // KSP generates this function
 * fun buildMainDestinationGraph(): NavigationGraph {
 *     return navigationGraph("main") {
 *         startDestination(MainDestination.Home)
 *         
 *         destination(MainDestination.Home) { _, navigator ->
 *             HomeContent(navigator)
 *         }
 *         
 *         typedDestinationUser(
 *             destination = ProfileDestination.User::class
 *         ) { data, navigator ->
 *             UserProfileContent(data, navigator)
 *         }
 *     }
 * }
 * 
 * // Use in your app
 * fun rootGraph() = navigationGraph("root") {
 *     include(buildMainDestinationGraph())
 * }
 * ```
 * 
 * @see Graph
 * @see Route
 * @see Argument
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Content(val destination: KClass<*>)
