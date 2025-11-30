# Navigation Library - API Reference

## Core Package (`navigation.core`)

### Destination

#### Interface
```kotlin
interface Destination {
    val route: String
    val arguments: Map<String, Any?>
}
```

#### TypedDestination Interface

**Type-safe destination with serializable data:**

```kotlin
interface TypedDestination<T : Any> : Destination {
    val data: T
}
```

**Usage with annotations:**
```kotlin
@Serializable
data class DetailData(val itemId: String, val mode: String = "view")

@Route("detail")
@Argument(DetailData::class)
data class Detail(val itemId: String, val mode: String = "view") 
    : Destination, TypedDestination<DetailData> {
    override val data = DetailData(itemId, mode)
}
```

**Usage with manual DSL:**
```kotlin
typedDestination<DetailData>(
    destination = Detail::class,
    dataClass = DetailData::class
) { data, navigator ->
    DetailScreen(
        itemId = data.itemId,
        mode = data.mode,
        navigator = navigator
    )
}
```

**See:** [TYPED_DESTINATIONS.md](TYPED_DESTINATIONS.md) for complete guide

#### Implementations
- `SimpleDestination(route, arguments)` - Basic destination with arguments
- `TypedDestination<T>` - Typed destination with serializable data
- Custom implementations for feature-specific destinations

#### Factory Functions
```kotlin
destination("route") {
    arg("key", value)
}
```

---

### Navigator

#### Methods
```kotlin
// Basic navigation
fun navigate(destination: Destination, transition: NavigationTransition? = null)
fun navigateBack(): Boolean
fun navigateUp(): Boolean

// Advanced navigation
fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null)
fun navigateAndClearAll(destination: Destination)
fun navigateAndClearTo(destination: Destination, clearRoute: String?, inclusive: Boolean)

// Setup
fun registerGraph(graph: NavigationGraph)
fun setStartDestination(destination: Destination)
fun handleDeepLink(deepLink: DeepLink)
```

#### Properties
```kotlin
val backStack: BackStack
val currentDestination: StateFlow<Destination?>
```

---

### BackStack

#### Methods
```kotlin
// Stack operations
fun push(destination: Destination)
fun pop(): Boolean
fun replace(destination: Destination)
fun replaceAll(destinations: List<Destination>)
fun clear()

// Advanced operations
fun popUntil(predicate: (Destination) -> Boolean): Boolean
fun popTo(route: String): Boolean
fun popToRoot(): Boolean
```

#### Properties
```kotlin
val stack: StateFlow<List<BackStackEntry>>
val current: StateFlow<BackStackEntry?>
val canGoBack: StateFlow<Boolean>
```

---

### NavigationGraph

#### Interface
```kotlin
interface NavigationGraph {
    val graphRoute: String
    val startDestination: Destination
    val destinations: List<DestinationConfig>
    val parentGraph: NavigationGraph?
}
```

#### Builder DSL
```kotlin
navigationGraph("graphRoute") {
    startDestination(destination)
    
    destination(destination) { dest, navigator ->
        // Composable content
    }
    
    deepLinkDestination(route, pattern) { dest, navigator ->
        // Composable content
    }
}
```

---

### NavigationTransition

#### Built-in Transitions
```kotlin
NavigationTransitions.None
NavigationTransitions.Fade
NavigationTransitions.SlideHorizontal
NavigationTransitions.SlideVertical
NavigationTransitions.ScaleIn
```

#### Custom Transition
```kotlin
customTransition {
    enter = slideInHorizontally() + fadeIn()
    exit = slideOutHorizontally() + fadeOut()
    popEnter = slideInHorizontally()
    popExit = slideOutHorizontally()
}
```

---

### DeepLink

#### Creation
```kotlin
val deepLink = DeepLink.parse("app://user/123?source=email")
deepLink.uri          // "app://user/123"
deepLink.parameters   // mapOf("source" to "email")
```

#### Handler
```kotlin
val handler = DefaultDeepLinkHandler()
handler.register("app://user/{userId}") { params ->
    UserDestination(params["userId"]!!)
}
```

---

## Annotation-based API (`quo-vadis-annotations`)

**KSP-powered code generation for zero-boilerplate navigation.** See [ANNOTATION_API.md](ANNOTATION_API.md) for complete guide.

### @Graph

Marks a sealed class as a navigation graph.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String)
```

**Example:**
```kotlin
@Graph("main")
sealed class MainDestination : Destination {
    @Route("main/home")
    data object Home : MainDestination()
    
    @Route("main/settings")
    data object Settings : MainDestination()
}
```

**Generates:**
- `MainDestinationRouteInitializer` - Route registration
- `buildMainDestinationGraph()` - Graph builder function

---

### @Route

Specifies the route path for a destination.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)
```

**Example:**
```kotlin
@Route("profile/user")
data object UserProfile : ProfileDestination()
```

---

### @Argument

Specifies serializable typed arguments for a destination.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<*>)
```

**Example:**
```kotlin
@Serializable
data class UserData(val userId: String, val displayName: String)

@Route("profile/user")
@Argument(UserData::class)
data class UserProfile(val userId: String, val displayName: String)
    : ProfileDestination(), TypedDestination<UserData> {
    override val data = UserData(userId, displayName)
}
```

**Generates:**
- `typedDestinationUserProfile()` - Typed destination extension

---

### @Content

Wires a Composable function to a destination.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Content(val destination: KClass<*>)
```

**For simple destinations:**
```kotlin
@Content(MainDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(navigator = navigator)
}
```

**For typed destinations:**
```kotlin
@Content(ProfileDestination.UserProfile::class)
@Composable
fun UserProfileContent(data: UserData, navigator: Navigator) {
    ProfileScreen(
        userId = data.userId,
        displayName = data.displayName,
        navigator = navigator
    )
}
```

---

### Generated Code

#### Route Registration

```kotlin
// Auto-generated
object MainDestinationRouteInitializer {
    init {
        RouteRegistry.register(MainDestination.Home::class, "main/home")
        RouteRegistry.register(MainDestination.Settings::class, "main/settings")
    }
}
```

Triggered automatically on first reference to any destination in the sealed class.

#### Graph Builder

```kotlin
// Auto-generated
fun buildMainDestinationGraph(): NavigationGraph {
    return navigationGraph("main") {
        startDestination(MainDestination.Home)
        
        destination(MainDestination.Home) { _, navigator ->
            HomeContent(navigator)
        }
        
        destination(MainDestination.Settings) { _, navigator ->
            SettingsContent(navigator)
        }
    }
}
```

**Usage:**
```kotlin
fun rootGraph() = navigationGraph("root") {
    include(buildMainDestinationGraph())
}
```

#### Typed Destination Extensions

```kotlin
// Auto-generated for @Argument destinations
fun NavigationGraphBuilder.typedDestinationUserProfile(
    destination: KClass<ProfileDestination.UserProfile>,
    transition: NavigationTransition? = null,
    content: @Composable (UserData, Navigator) -> Unit
) {
    typedDestination(
        destination = destination,
        dataClass = UserData::class,
        transition = transition,
        content = content
    )
}
```

Called automatically by generated graph builder.

---

## Compose Package (`navigation.compose`)

### NavHost

```kotlin
@Composable
fun NavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade
)
```

### GraphNavHost

```kotlin
@Composable
fun GraphNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade
)
```

### PredictiveBackNavigation

**Multiplatform predictive back gesture handler with automatic screen caching.**

```kotlin
@Composable
fun PredictiveBackNavigation(
    navigator: Navigator,
    graph: NavigationGraph,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    animationType: PredictiveBackAnimationType = PredictiveBackAnimationType.Material3,
    sensitivity: Float = 1f,
    maxCacheSize: Int = 3
)
```

#### PredictiveBackAnimationType

```kotlin
enum class PredictiveBackAnimationType {
    Material3,  // Scale + translate + rounded corners + shadow
    Scale,      // Simple scale down with fade
    Slide       // Slide right with fade
}
```

#### Features
- **Automatic Caching**: Keeps screens alive during gestures for smooth animations
- **Separate Animations**: Different animations for gesture phase vs exit phase
- **Cache Locking**: Prevents premature screen destruction during animations
- **Platform Support**: Works on both Android 13+ and iOS
- **Configurable**: Adjust animation type, sensitivity, and cache size

#### Animation Phases

**Gesture Phase** (user dragging):
- Applies selected animation type (Material3/Scale/Slide)
- Shows screen transforming as user drags
- Previous screen visible underneath
- Scrim layer between screens

**Exit Phase** (after gesture completes):
- Continues with same animation type for consistency
- Smoothly completes the animation (scale down + fade out)
- Defers navigation until animation completes
- Prevents premature screen destruction

#### Example

```kotlin
PredictiveBackNavigation(
    navigator = navigator,
    graph = appGraph,
    enabled = true,
    animationType = PredictiveBackAnimationType.Material3,
    sensitivity = 1f
)
```

---

## Unified GraphNavHost - Complete Animation Support

### GraphNavHost

**PRIMARY navigation host with unified support for all animation scenarios.**

```kotlin
@Composable
fun GraphNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    enableComposableCache: Boolean = true,
    enablePredictiveBack: Boolean = true,
    maxCacheSize: Int = 3
)
```

#### Features
- ✅ **Animated forward navigation** with enter transitions
- ✅ **Animated back navigation** with popEnter/popExit (direction-aware)
- ✅ **Predictive back gestures** with progressive animations
- ✅ **Composable caching** for smooth transitions
- ✅ **Entry locking** prevents cache eviction during animations
- ✅ **Multiplatform logging** for debugging (NAV_DEBUG tag)

#### Animation Behavior

**Forward Navigation:**
```kotlin
navigator.navigate(DetailScreen, NavigationTransitions.SlideHorizontal)
```
- New screen slides in from RIGHT using `enter` transition
- Old screen fades out using `exit` transition
- 300ms duration (NavigationTransitions.ANIMATION_DURATION)

**Back Navigation (Programmatic):**
```kotlin
navigator.navigateBack()
```
- Current screen slides out to RIGHT using `popExit` transition
- Previous screen fades in using `popEnter` transition  
- Uses `animateFloatAsState` for frame-by-frame recomposition
- Proper animation completes over full 300ms

**Predictive Back Gesture:**
- User swipe from edge starts gesture
- Current screen translates RIGHT + scales down (90%)
- Maximum drag limited to 25% of screen width (MAX_GESTURE_PROGRESS)
- Previous screen static behind (no parallax effects)
- Complete gesture → navigation commits with exit animation
- Cancel gesture → animates back to original position

#### Transition Priority Chain
1. **Explicit transition** - Passed to `navigate(destination, transition)`
2. **Destination default** - From `TransitionDestination.defaultTransition`
3. **Graph default** - From `destination(dest, transition) { ... }`
4. **NavHost default** - From `GraphNavHost(defaultTransition = ...)`

#### Example Usage

```kotlin
// Basic usage with default transition
GraphNavHost(
    graph = appGraph,
    navigator = navigator,
    defaultTransition = NavigationTransitions.SlideHorizontal,
    enablePredictiveBack = true
)

// Navigate with explicit transition
navigator.navigate(
    destination = DetailScreen("item-123"),
    transition = NavigationTransitions.SlideHorizontal
)

// Back navigation (uses popExit/popEnter)
navigator.navigateBack()
```

---

## NavigationTransition API

### Interface
```kotlin
interface NavigationTransition {
    val enter: EnterTransition        // Forward navigation entry
    val exit: ExitTransition          // Forward navigation exit
    val popEnter: EnterTransition     // Back navigation entry (revealed screen)
    val popExit: ExitTransition       // Back navigation exit (leaving screen)
}
```

### Pre-built Transitions

**NavigationTransitions.None**
- No animation (instant)

**NavigationTransitions.Fade**
- Crossfade between screens (300ms)
- Use for: Same-level navigation (e.g., bottom nav tabs)

**NavigationTransitions.SlideHorizontal**
- Forward: New screen slides in from RIGHT, old fades out
- Back: Current slides out to RIGHT, previous fades in
- Use for: Drill-down navigation (master-detail, hierarchical flows)

**NavigationTransitions.SlideVertical**
- Forward: New screen slides in from BOTTOM, old fades out
- Back: Current slides out to BOTTOM, previous fades in
- Use for: Modal-like navigation, bottom sheets

**NavigationTransitions.ScaleIn**
- Forward: New screen scales up from center with fade
- Back: Current scales down to center with fade
- Use for: Special states (success screens, completion screens)

### TransitionDestination Interface

**Define default transition for a destination:**
```kotlin
object DetailScreen : TransitionDestination {
    override val route = "detail/{id}"
    override val defaultTransition = NavigationTransitions.SlideHorizontal
    
    override val arguments: Map<String, Any?> = mapOf("id" to id)
}

// Now just:
navigator.navigate(DetailScreen) // Uses SlideHorizontal automatically
```

### Custom Transitions

```kotlin
val customTransition = customTransition {
    enter = slideInHorizontally { it / 2 } + fadeIn()
    exit = slideOutHorizontally { -it / 3 } + fadeOut()
    popEnter = fadeIn()
    popExit = slideOutHorizontally { it } + fadeOut()
}
```

---

## Shared Element Transitions (Foundation)

### SharedElementNavHost

**Wrapper for shared element support (future):**
```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true
)
```

### SharedElementTransition API

```kotlin
// Define shared element keys
val heroImageKey = SharedElementKey("hero_image", SharedElementType.Bounds)

// Create transition with shared elements
val transition = NavigationTransitions.SlideHorizontal.withSharedElements(
    listOf(SharedElementKey("hero_image"))
)

// Navigate with shared elements
navigator.navigate(DetailScreen, transition)
```

**Helper Functions:**
```kotlin
SharedElementTransitions.slideWithHero(heroKey: String)
SharedElementTransitions.fadeWithSharedBounds(vararg keys: String)
```

**Status:** API foundation complete, implementation requires `SharedTransitionLayout` integration.

---

### ComposableCache

**Internal caching system for predictive back navigation.**

```kotlin
class ComposableCache(maxCacheSize: Int = 3) {
    fun lockEntry(entryId: String)
    fun unlockEntry(entryId: String)
    
    @Composable
    fun Entry(
        entry: BackStackEntry,
        saveableStateHolder: SaveableStateHolder,
        content: @Composable (BackStackEntry) -> Unit
    )
}

@Composable
fun rememberComposableCache(maxCacheSize: Int = 3): ComposableCache
```

### Factory

```kotlin
@Composable
fun rememberNavigator(
    deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
): Navigator
```

---

## Shared Element Transitions (`navigation.compose`)

**Type-safe shared element transitions using Compose Multiplatform's SharedTransitionLayout API.**

> 📖 **Full Guide**: See [SHARED_ELEMENT_TRANSITIONS.md](SHARED_ELEMENT_TRANSITIONS.md) for comprehensive documentation.

### Overview

Quo Vadis provides first-class support for shared element transitions that work in **both forward and backward** navigation, including predictive back gestures. SharedTransitionLayout is always enabled in GraphNavHost, and destinations opt-in via `destinationWithScopes()`.

### destinationWithScopes()

Enable shared elements for specific destinations:

```kotlin
fun NavigationGraphBuilder.destinationWithScopes(
    destination: Destination,
    content: @Composable (
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) -> Unit
)

fun NavigationGraphBuilder.destinationWithScopes(
    destinationClass: KClass<out Destination>,
    content: @Composable (
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) -> Unit
)
```

**Example:**
```kotlin
navigationGraph("master-detail") {
    destinationWithScopes(MasterList) { _, nav, shared, animated ->
        MasterListScreen(nav, shared, animated)
    }
    
    destinationWithScopes(Detail::class) { dest, nav, shared, animated ->
        DetailScreen((dest as Detail).id, nav, shared, animated)
    }
}
```

### Modifier Extensions

#### quoVadisSharedElement()

For **visual elements** (icons, images, shapes) that animate smoothly:

```kotlin
fun Modifier.quoVadisSharedElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? = { _, _ -> null }
): Modifier
```

**Example:**
```kotlin
Icon(
    imageVector = Icons.Default.AccountCircle,
    contentDescription = null,
    modifier = Modifier
        .size(56.dp)
        .quoVadisSharedElement(
            key = "icon-${item.id}",
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
)
```

#### quoVadisSharedBounds()

For **text and containers** where content crossfades:

```kotlin
fun Modifier.quoVadisSharedBounds(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    resizeMode: SharedTransitionScope.ResizeMode = ScaleToBounds(),
    placeHolderSize: SharedTransitionScope.PlaceHolderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? = { _, _ -> null }
): Modifier
```

**Example:**
```kotlin
Text(
    text = item.title,
    modifier = Modifier.quoVadisSharedBounds(
        key = "title-${item.id}",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
)
```

#### quoVadisSharedElementOrNoop()

Graceful fallback when scopes are null:

```kotlin
fun Modifier.quoVadisSharedElementOrNoop(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    // ... same parameters as quoVadisSharedElement
): Modifier
```

### CompositionLocal Access

Direct access to scopes via CompositionLocals:

```kotlin
@Composable
fun currentSharedTransitionScope(): SharedTransitionScope?

@Composable
fun currentNavAnimatedVisibilityScope(): AnimatedVisibilityScope?
```

**Example:**
```kotlin
@Composable
fun MyScreen() {
    val sharedScope = currentSharedTransitionScope()
    val animatedScope = currentNavAnimatedVisibilityScope()
    
    Icon(
        modifier = Modifier.quoVadisSharedElement(
            key = "icon",
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = animatedScope
        )
    )
}
```

### Key Features

✅ **Bidirectional Transitions**: Works in both forward AND backward navigation  
✅ **Predictive Back Support**: Shared elements follow predictive back gestures  
✅ **Type-Safe**: Compile-time safe with no string routing  
✅ **Per-Destination Opt-In**: Use `destinationWithScopes()` only where needed  
✅ **Graceful Degradation**: Elements render normally if scopes are null  
✅ **Multiplatform**: Works on Android, iOS, Desktop, Web (JS/Wasm)  

### Usage Pattern

```kotlin
// 1. Define destinations with scopes
destinationWithScopes(Screen1) { _, nav, shared, animated ->
    Screen1(nav, shared, animated)
}

// 2. Mark shared elements with matching keys
@Composable
fun Screen1(nav: Navigator, shared: SharedTransitionScope?, animated: AnimatedVisibilityScope?) {
    Icon(
        modifier = Modifier.quoVadisSharedElement(
            key = "icon-123",  // Must match on both screens
            sharedTransitionScope = shared,
            animatedVisibilityScope = animated
        )
    )
}

// 3. Navigate normally - transitions happen automatically
nav.navigate(Screen2)
nav.navigateBack()  // Transitions work in reverse!
```

---

## MVI Support

For MVI architecture integration, use the separate `quo-vadis-core-flow-mvi` module which provides integration with the [FlowMVI](https://github.com/respawn-app/FlowMVI) library.

See the [FlowMVI Integration Guide](FLOW_MVI.md) for complete documentation.

---

## Utils Package (`navigation.utils`)

### Navigator Extensions

```kotlin
// Navigate with builder
fun Navigator.navigateTo(transition, builder: () -> Destination)

// Conditional navigation
fun Navigator.navigateIfNotCurrent(destination, transition)
fun Navigator.navigateSingleTop(destination, transition)
fun Navigator.navigateSafely(destination, transition, onError)

// Scope
fun Navigator.inScope(block: NavigationScope.() -> Unit)
```

### BackStack Extensions

```kotlin
fun BackStack.contains(route: String): Boolean
fun BackStack.findByRoute(route: String): BackStackEntry?
val BackStack.size: Int
val BackStack.isEmpty: Boolean
val BackStack.routes: List<String>
fun BackStack.popCount(count: Int): Boolean
```

---

## Testing Package (`navigation.testing`)

### FakeNavigator

```kotlin
class FakeNavigator : Navigator {
    val navigationCalls: List<NavigationCall>
    
    fun clearCalls()
    fun verifyNavigateTo(route: String): Boolean
    fun verifyNavigateBack(): Boolean
    fun getNavigateCallCount(route: String): Int
}
```

### Test DSL

```kotlin
navigationTest {
    given { setStartDestination(Home) }
    `when` { navigate(Details) }
    then { assertTrue(verifyNavigateTo("details")) }
}
```

### Pattern 5: Predictive Back Navigation
```kotlin
// With default Material3 animation
PredictiveBackNavigation(
    navigator = navigator,
    graph = graph,
    enabled = true
)

// With custom animation type
PredictiveBackNavigation(
    navigator = navigator,
    graph = graph,
    animationType = PredictiveBackAnimationType.Scale,
    sensitivity = 1.2f  // More sensitive gesture
)
```

---

## Integration Package (`navigation.integration`)

### DI Support

```kotlin
interface NavigationFactory {
    fun createNavigator(): Navigator
    fun createDeepLinkHandler(): DeepLinkHandler
}

@Composable
inline fun <reified T> rememberFromDI(
    container: DIContainer,
    key: Any? = null
): T
```

---

## Serialization Package (`navigation.serialization`)

### State Serializer

```kotlin
interface NavigationStateSerializer {
    fun serializeBackStack(entries: List<BackStackEntry>): String
    fun deserializeBackStack(serialized: String): List<BackStackEntry>
    fun serializeDestination(destination: Destination): String
    fun deserializeDestination(serialized: String): Destination?
}
```

### Extensions

```kotlin
fun BackStack.saveState(serializer: NavigationStateSerializer): String
fun MutableBackStack.restoreState(savedState: String, serializer: NavigationStateSerializer)
```

---

## Common Patterns

### Pattern 1: Simple Navigation
```kotlin
val navigator = rememberNavigator()
navigator.navigate(DetailsDestination("123"))
```

### Pattern 2: Modular Feature
```kotlin
class FeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph() = navigationGraph("feature") {
        startDestination(Screen1)
        destination(Screen1) { _, nav -> Screen1UI(nav) }
    }
}
```

### Pattern 3: Deep Links
```kotlin
val handler = DefaultDeepLinkHandler()
handler.register("app://product/{id}") { params ->
    ProductDestination(params["id"]!!)
}
navigator.handleDeepLink(DeepLink.parse("app://product/123"))
```

### Pattern 4: Custom Transitions
```kotlin
navigator.navigate(
    destination = Details,
    transition = NavigationTransitions.SlideHorizontal
)
```

---

## Type Safety

All navigation is type-safe:

```kotlin
// Define typed destinations
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    
    data class Details(val id: String) : AppDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}

// Navigate with compile-time safety
navigator.navigate(AppDestination.Details("123"))
```

---

## Observable State

All state is reactive:

```kotlin
@Composable
fun MyScreen(navigator: Navigator) {
    val current by navigator.backStack.current.collectAsState()
    val canGoBack by navigator.backStack.canGoBack.collectAsState()
    
    if (canGoBack) {
        BackButton { navigator.navigateBack() }
    }
}
```

---

## Complete Example

```kotlin
// 1. Define destinations
object HomeScreen : Destination {
    override val route = "home"
}

data class DetailScreen(val itemId: String) : Destination {
    override val route = "detail"
    override val arguments = mapOf("itemId" to itemId)
}

// 2. Create graph
val appGraph = navigationGraph("app") {
    startDestination(HomeScreen)
    
    destination(HomeScreen) { _, navigator ->
        HomeUI(onItemClick = { id ->
            navigator.navigate(
                DetailScreen(id),
                NavigationTransitions.SlideHorizontal
            )
        })
    }
    
    destination(SimpleDestination("detail")) { dest, navigator ->
        DetailUI(
            itemId = dest.arguments["itemId"] as String,
            onBack = { navigator.navigateBack() }
        )
    }
}

// 3. Setup navigation
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(HomeScreen)
    }
    
    GraphNavHost(
        graph = appGraph,
        navigator = navigator
    )
}
```

