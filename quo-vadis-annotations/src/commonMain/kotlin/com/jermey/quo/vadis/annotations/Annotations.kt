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
 * @param startDestination The simple name of the destination to use as the start destination.
 *                         If not specified, the first destination in the sealed class will be used.
 * 
 * @sample Basic graph with simple destinations
 * ```kotlin
 * @Graph("main", startDestination = "Home")
 * sealed class MainDestination : Destination {
 *     @Route("main/home")
 *     data object Home : MainDestination()
 *     
 *     @Route("main/settings")
 *     data object Settings : MainDestination()
 * }
 * 
 * // Generated: buildMainDestinationGraph() with Home as start destination
 * ```
 * 
 * @sample Graph with typed destinations
 * ```kotlin
 * @Serializable
 * data class DetailData(val itemId: String)
 * 
 * @Graph("feature", startDestination = "List")
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
annotation class Graph(val name: String, val startDestination: String = "")

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

/**
 * Marks a sealed class as a tab graph container.
 *
 * The sealed class should extend [TabDefinition] and contain tab objects/classes
 * annotated with [@Tab]. KSP will generate:
 * - Tab configuration (`{ClassName}Config`)
 * - Tab navigation container composable (`{ClassName}Container`)
 * - Tab graph builder function (`build{ClassName}Graph()`)
 *
 * @param name Unique identifier for this tab container
 * @param initialTab The class name of the initial tab (defaults to first @Tab)
 * @param primaryTab The class name of the primary/home tab (defaults to initialTab)
 *
 * @sample Basic tab container
 * ```kotlin
 * @TabGraph("main")
 * sealed class MainTab : TabDefinition {
 *     @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     data object Home : MainTab()
 *
 *     @Tab(route = "profile", label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
 *     data object Profile : MainTab()
 * }
 *
 * // Generated:
 * // - MainTabConfig: TabNavigatorConfig
 * // - MainTabContainer: @Composable
 * // - buildMainTabGraph(): NavigationGraph
 * ```
 *
 * @sample Nested tab container
 * ```kotlin
 * @TabGraph("settings", primaryTab = "General")
 * sealed class SettingsTab : TabDefinition {
 *     @Tab(route = "general", label = "General", icon = "settings", rootGraph = GeneralDestination::class)
 *     data object General : SettingsTab()
 *
 *     @Tab(route = "privacy", label = "Privacy", icon = "lock", rootGraph = PrivacyDestination::class)
 *     data object Privacy : SettingsTab()
 *
 *     @Tab(route = "about", label = "About", icon = "info", rootGraph = AboutDestination::class)
 *     data object About : SettingsTab()
 * }
 * ```
 *
 * @see Tab
 * @see TabContent
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabGraph(
    val name: String,
    val initialTab: String = "",
    val primaryTab: String = ""
)

/**
 * Defines a tab within a [@TabGraph] container.
 *
 * Each tab represents an independent navigation hierarchy with its own backstack.
 * KSP uses this annotation to generate tab configuration and setup code.
 *
 * @param route The unique route identifier for this tab
 * @param label The user-visible label for this tab
 * @param icon The icon identifier for this tab (platform-specific)
 * @param rootGraph The [KClass] of the root navigation graph for this tab.
 *                  Should be a sealed class annotated with [@Graph].
 * @param rootDestination Optional specific destination to use as root (defaults to graph's startDestination)
 *
 * @sample Basic tab
 * ```kotlin
 * @Tab(
 *     route = "home",
 *     label = "Home",
 *     icon = "home",
 *     rootGraph = HomeDestination::class
 * )
 * data object Home : MainTab()
 * ```
 *
 * @sample Tab with specific root destination
 * ```kotlin
 * @Tab(
 *     route = "profile",
 *     label = "Profile",
 *     icon = "person",
 *     rootGraph = ProfileDestination::class,
 *     rootDestination = ProfileDestination.Overview::class
 * )
 * data object Profile : MainTab()
 * ```
 *
 * @see TabGraph
 * @see TabContent
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    val route: String,
    val label: String,
    val icon: String,
    val rootGraph: KClass<*>,
    val rootDestination: KClass<*> = Nothing::class
)

/**
 * Marks a Composable function as the custom content renderer for a specific tab.
 *
 * By default, tabs use [TabbedNavHost] with the tab's navigation graph. Use [@TabContent]
 * when you need custom rendering logic, animations, or additional UI elements around
 * the tab's navigation content.
 *
 * The function signature must be: `@Composable (TabDefinition, Navigator) -> Unit`
 *
 * @param tabClass The [KClass] of the tab that this Composable renders.
 *
 * @sample Custom tab content with header
 * ```kotlin
 * @TabContent(MainTab.Home::class)
 * @Composable
 * fun HomeTabContent(tab: TabDefinition, navigator: Navigator) {
 *     Column {
 *         // Custom header
 *         Text("Welcome Home", style = MaterialTheme.typography.h4)
 *         Divider()
 *
 *         // Standard navigation content
 *         GraphNavHost(
 *             graph = homeNavigationGraph,
 *             navigator = navigator
 *         )
 *     }
 * }
 * ```
 *
 * @sample Custom tab with transition overrides
 * ```kotlin
 * @TabContent(MainTab.Profile::class)
 * @Composable
 * fun ProfileTabContent(tab: TabDefinition, navigator: Navigator) {
 *     TabbedNavHost(
 *         tabState = rememberTabNavigatorState(tab),
 *         navigationGraph = profileNavigationGraph,
 *         tabTransitionSpec = TabTransitionSpec.SlideHorizontal
 *     )
 * }
 * ```
 *
 * @see TabGraph
 * @see Tab
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class TabContent(val tabClass: KClass<*>)
