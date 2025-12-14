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

// Tab navigation
fun switchTab(index: Int)

// Pane navigation (adaptive layouts)
fun navigateToPane(role: PaneRole, destination: Destination)
fun switchActivePane(role: PaneRole)
fun paneContent(role: PaneRole): NavNode?

// State management
fun updateState(newState: NavNode, transition: NavigationTransition? = null)
```

#### Properties
```kotlin
val state: StateFlow<NavNode>                    // Complete navigation tree
val currentDestination: StateFlow<Destination?>  // Active destination
val canNavigateBack: StateFlow<Boolean>          // Back navigation availability
```

---

### NavNode

Immutable tree structure representing navigation state.

#### Node Types
```kotlin
sealed interface NavNode {
    val key: String
    val parentKey: String?
}

// Leaf node for a single destination
data class ScreenNode(
    override val key: String,
    override val parentKey: String? = null,
    val destination: Destination
) : NavNode

// Stack container (back-stack)
data class StackNode(
    override val key: String,
    override val parentKey: String? = null,
    val children: List<NavNode> = emptyList(),
    val scopeKey: String? = null
) : NavNode {
    val activeChild: NavNode?
}

// Tab container
data class TabNode(
    override val key: String,
    override val parentKey: String? = null,
    val children: List<StackNode> = emptyList(),
    val activeIndex: Int = 0,
    val scopeKey: String? = null
) : NavNode {
    val activeStack: StackNode?
}

// Pane container (adaptive layouts)
data class PaneNode(
    override val key: String,
    override val parentKey: String? = null,
    val paneConfigurations: Map<PaneRole, PaneContent> = emptyMap(),
    val activePaneRole: PaneRole? = null,
    val scopeKey: String? = null
) : NavNode {
    fun paneContent(role: PaneRole): NavNode?
}
```

#### Extension Functions
```kotlin
fun NavNode.findByKey(key: String): NavNode?
fun NavNode.activePathToLeaf(): List<NavNode>
fun NavNode.activeLeaf(): ScreenNode?
fun NavNode.activeStack(): StackNode?
fun NavNode.allScreens(): List<ScreenNode>
fun NavNode.depth(): Int
fun NavNode.nodeCount(): Int
fun NavNode.canHandleBackInternally(): Boolean
```

---

### TreeMutator

Pure functional operations for manipulating the NavNode tree.

#### Stack Operations
```kotlin
object TreeMutator {
    // Push destination to active stack
    fun push(
        root: NavNode,
        destination: Destination,
        scopeRegistry: ScopeRegistry? = null,
        keyGenerator: () -> String = { UUID.randomUUID().toString() }
    ): NavNode
    
    // Pop from active stack
    fun pop(root: NavNode, behavior: PopBehavior = PopBehavior.STANDARD): NavNode?
    
    // Replace current destination
    fun replaceCurrent(
        root: NavNode,
        destination: Destination,
        keyGenerator: () -> String = { UUID.randomUUID().toString() }
    ): NavNode
    
    // Clear stack and push
    fun clearAndPush(
        root: NavNode,
        destination: Destination,
        keyGenerator: () -> String = { UUID.randomUUID().toString() }
    ): NavNode
    
    // Pop until predicate matches
    fun popUntil(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode
    
    // Pop to specific route
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode
}
```

#### Tab Operations
```kotlin
object TreeMutator {
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode
}
```

#### Pane Operations
```kotlin
object TreeMutator {
    fun navigateToPane(
        root: NavNode,
        role: PaneRole,
        destination: Destination,
        keyGenerator: () -> String = { UUID.randomUUID().toString() }
    ): NavNode
    
    fun switchActivePane(root: NavNode, paneKey: String, role: PaneRole): NavNode
    fun popPane(root: NavNode, paneKey: String, role: PaneRole): NavNode?
}
```

#### Back Navigation
```kotlin
object TreeMutator {
    sealed interface PopResult {
        data object Empty : PopResult()
        data class Popped(val newState: NavNode) : PopResult()
    }
    
    sealed interface BackResult {
        data class Handled(val newState: NavNode) : BackResult()
        data object DelegateToSystem : BackResult()
        data object CannotHandle : BackResult()
    }
    
    fun popWithTabBehavior(root: NavNode): BackResult
    fun canGoBack(root: NavNode): Boolean
    fun currentDestination(root: NavNode): Destination?
    fun canHandleBackNavigation(root: NavNode): Boolean
    fun wouldCascade(root: NavNode): Boolean
}
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

### @TabWrapper (Hierarchical Rendering)

Marks a composable function as a tab wrapper for use with `RenderingMode.Hierarchical`.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TabWrapper(val tabClass: KClass<*>)
```

**Signature Requirements:**
- Receiver: `TabWrapperScope`
- Parameter: `content: @Composable () -> Unit`

**Example:**
```kotlin
@TabWrapper(tabClass = MainTabs::class)
@Composable
fun TabWrapperScope.MainTabWrapper(content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = activeIndex == index,
                        onClick = { switchTab(index) },
                        icon = { Icon(tab.metadata?.icon, tab.metadata?.label ?: "") },
                        label = { Text(tab.metadata?.label ?: "") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()  // Library renders active tab content here
        }
    }
}
```

**TabWrapperScope Properties:**
- `navigator: Navigator` - Current navigator instance
- `activeIndex: Int` - Currently selected tab index
- `tabs: List<TabInfo>` - List of all tab configurations
- `switchTab(index: Int)` - Function to change tabs

---

### @PaneWrapper (Hierarchical Rendering)

Marks a composable function as a pane wrapper for adaptive layouts.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PaneWrapper(val paneClass: KClass<*>)
```

**Signature Requirements:**
- Receiver: `PaneWrapperScope`
- Parameter: `content: @Composable () -> Unit`

**Example:**
```kotlin
@PaneWrapper(paneClass = ListDetailPane::class)
@Composable
fun PaneWrapperScope.ListDetailWrapper(content: @Composable () -> Unit) {
    if (isExpanded) {
        // Multi-pane layout for larger screens
        Row(modifier = Modifier.fillMaxSize()) {
            paneContents.forEach { pane ->
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.65f
                    PaneRole.Supporting -> 0.35f
                    else -> 1f
                }
                if (pane.isVisible) {
                    Box(modifier = Modifier.weight(weight)) {
                        pane.content()
                    }
                }
            }
        }
    } else {
        // Single pane for compact screens
        content()
    }
}
```

**PaneWrapperScope Properties:**
- `navigator: Navigator` - Current navigator instance
- `paneContents: List<PaneContentSlot>` - Pane configurations
- `activePaneRole: PaneRole?` - Currently active pane (compact mode)
- `isExpanded: Boolean` - True if multi-pane layout should be used

---

### @Transition (Hierarchical Rendering)

Defines per-destination transition animations.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transition(
    val type: TransitionType,
    val customTransition: KClass<*> = Unit::class
)
```

**TransitionType Values:**
- `SlideHorizontal` - Slide in/out from right
- `SlideVertical` - Slide in/out from bottom
- `Fade` - Fade in/out
- `None` - No animation
- `Custom` - Use custom transition class

**Example:**
```kotlin
// Standard transition type
@Destination(route = "details/{id}")
@Transition(type = TransitionType.SlideHorizontal)
data class DetailsDestination(val id: String)

// Custom transition
@Destination(route = "modal")
@Transition(type = TransitionType.Custom, customTransition = ModalTransition::class)
data class ModalDestination

// Custom transition implementation
object ModalTransition : CustomNavTransition {
    override fun createNavTransition(): NavTransition = NavTransition(
        enter = slideInVertically { it } + fadeIn(),
        exit = fadeOut(),
        popEnter = fadeIn(),
        popExit = slideOutVertically { it } + fadeOut()
    )
}
```

---

### @Screen (Hierarchical Rendering)

Maps a destination to its screen composable for hierarchical rendering.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Screen(val destination: KClass<*>)
```

**Example:**
```kotlin
@Screen(destination = HomeDestination::class)
@Composable
fun HomeScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Welcome Home")
    }
}

@Screen(destination = ProfileDestination::class)
@Composable
fun ProfileScreen(destination: ProfileDestination) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Profile: ${destination.userId}")
    }
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

### NavigationHost

**Unified navigation host that renders NavNode trees using hierarchical rendering.**

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    wrapperRegistry: WrapperRegistry = EmptyWrapperRegistry,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    maxCacheSize: Int = 10
)
```

#### Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `navigator` | `Navigator` | The navigator instance providing NavNode state |
| `modifier` | `Modifier` | Modifier for the root container |
| `screenRegistry` | `ScreenRegistry` | Registry mapping destinations to composables |
| `wrapperRegistry` | `WrapperRegistry` | Registry for tab/pane wrapper composables |
| `predictiveBackMode` | `PredictiveBackMode` | How predictive back gestures are handled |
| `maxCacheSize` | `Int` | Maximum cached composables |

#### Features
- ✅ **Hierarchical rendering** of NavNode trees
- ✅ **Animated forward navigation** with enter transitions
- ✅ **Animated back navigation** with popEnter/popExit (direction-aware)
- ✅ **Predictive back gestures** with progressive animations
- ✅ **Composable caching** for smooth transitions
- ✅ **Entry locking** prevents cache eviction during animations
- ✅ **Shared element transitions** support

### PredictiveBackMode

```kotlin
enum class PredictiveBackMode {
    ROOT_ONLY,     // Only root stack handles predictive back
    FULL_CASCADE   // All stacks handle predictive back with cascade
}
```

### Predictive Back in NavigationHost

Predictive back is built into `NavigationHost` via the `predictiveBackMode` parameter:

```kotlin
NavigationHost(
    navigator = navigator,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
    // ... other parameters
)
```

#### Features
- **Automatic Caching**: Keeps screens alive during gestures for smooth animations
- **Separate Animations**: Different animations for gesture phase vs exit phase
- **Cache Locking**: Prevents premature screen destruction during animations
- **Platform Support**: Works on both Android 13+ and iOS

#### Animation Phases

**Gesture Phase** (user dragging):
- Screen scales and translates as user drags
- Previous screen visible underneath

**Exit Phase** (after gesture completes):
- Smoothly completes the animation
- Navigation deferred until animation completes

---

## NavigationHost - Complete Animation Support

### NavigationHost

**PRIMARY navigation host with unified support for all animation scenarios.**

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    wrapperRegistry: WrapperRegistry = EmptyWrapperRegistry,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    maxCacheSize: Int = 10
)
```

#### Features
- ✅ **Hierarchical NavNode rendering** with proper parent-child relationships
- ✅ **Animated forward navigation** with enter transitions
- ✅ **Animated back navigation** with popEnter/popExit (direction-aware)
- ✅ **Predictive back gestures** with progressive animations
- ✅ **Composable caching** for smooth transitions
- ✅ **Entry locking** prevents cache eviction during animations
- ✅ **Multiplatform support** (Android, iOS, Desktop, Web)

#### Animation Behavior

**Forward Navigation:**
```kotlin
navigator.navigate(DetailScreen, NavigationTransitions.SlideHorizontal)
```
- New screen slides in from RIGHT using `enter` transition
- Old screen fades out using `exit` transition

**Back Navigation (Programmatic):**
```kotlin
navigator.navigateBack()
```
- Current screen slides out to RIGHT using `popExit` transition
- Previous screen fades in using `popEnter` transition

**Predictive Back Gesture:**
- User swipe from edge starts gesture
- Current screen translates RIGHT + scales down
- Previous screen static behind
- Complete gesture → navigation commits with exit animation
- Cancel gesture → animates back to original position

#### Example Usage

```kotlin
// Basic usage with NavigationHost
NavigationHost(
    navigator = navigator,
    screenRegistry = myScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)

// Navigate with transition
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

### Shared Elements in NavigationHost

Shared element transitions are fully supported in `NavigationHost`. The hierarchical rendering system provides `SharedTransitionScope` and `AnimatedVisibilityScope` to destinations that opt-in via `destinationWithScopes()`.

**See [SHARED_ELEMENT_TRANSITIONS.md](SHARED_ELEMENT_TRANSITIONS.md) for complete documentation.**

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

Quo Vadis provides first-class support for shared element transitions that work in **both forward and backward** navigation, including predictive back gestures. SharedTransitionLayout is always enabled in NavigationHost, and destinations opt-in via `destinationWithScopes()`.

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
```

### NavNode Extensions

```kotlin
fun NavNode.containsRoute(route: String): Boolean
fun NavNode.findByRoute(route: String): ScreenNode?
val NavNode.routes: List<String>
fun NavNode.findByKey(key: String): NavNode?
fun NavNode.activePathToLeaf(): List<NavNode>
fun NavNode.activeLeaf(): ScreenNode?
fun NavNode.activeStack(): StackNode?
fun NavNode.allScreens(): List<ScreenNode>
fun NavNode.depth(): Int
fun NavNode.nodeCount(): Int
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

## NavigationEvent Back Handling (`navigation.compose.navback`)

The `navback` package provides a modern, multiplatform back handling API that integrates with the AndroidX NavigationEvent library. This provides proper system animation support on Android 14+ and consistent gesture handling across all platforms.

> 📖 **Full Guide**: See [MULTIPLATFORM_PREDICTIVE_BACK.md](MULTIPLATFORM_PREDICTIVE_BACK.md) for comprehensive documentation.

### QuoVadisBackHandler

**Primary composable for handling back gestures with NavigationEvent integration.**

```kotlin
@Composable
fun QuoVadisBackHandler(
    enabled: Boolean = true,
    currentScreenInfo: ScreenNavigationInfo,
    previousScreenInfo: ScreenNavigationInfo? = null,
    onBackProgress: ((BackNavigationEvent) -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    onBackCompleted: () -> Unit,
    content: @Composable () -> Unit
)
```

#### Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `enabled` | `Boolean` | Whether back handling is enabled. When false, gestures pass through to the system. |
| `currentScreenInfo` | `ScreenNavigationInfo` | Info about the currently displayed screen for system animation context. |
| `previousScreenInfo` | `ScreenNavigationInfo?` | Info about the screen that will be revealed on back navigation. Pass null if at root. |
| `onBackProgress` | `((BackNavigationEvent) -> Unit)?` | Called with progress updates during the back gesture (0.0 to 1.0). |
| `onBackCancelled` | `(() -> Unit)?` | Called when the back gesture is cancelled. |
| `onBackCompleted` | `() -> Unit` | Called when the back gesture completes and navigation should occur. |
| `content` | `@Composable () -> Unit` | The content to display. |

#### System Integration

On Android 14+ (API 34+), `QuoVadisBackHandler` properly integrates with the system's `OnBackInvokedDispatcher` to provide:
- System predictive back animations when closing the app
- In-app predictive back animations during navigation
- Proper priority handling for nested handlers

On other platforms, the handler provides gesture detection without system animation.

#### Example: Full Usage

```kotlin
QuoVadisBackHandler(
    enabled = canGoBack,
    currentScreenInfo = ScreenNavigationInfo(
        screenId = "detail-${item.id}",
        displayName = "Item Details",
        route = "detail/{id}"
    ),
    previousScreenInfo = ScreenNavigationInfo(
        screenId = "list",
        displayName = "Item List",
        route = "list"
    ),
    onBackProgress = { event ->
        // Animate based on gesture progress
        animatedOffset = event.progress * maxOffset
    },
    onBackCancelled = {
        // Reset animation state
        animatedOffset = 0f
    },
    onBackCompleted = {
        navigator.goBack()
    }
) {
    // Screen content
    DetailScreen(item)
}
```

#### Simplified Overload

For simple back handling without progress tracking:

```kotlin
@Composable
fun QuoVadisBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    content: @Composable () -> Unit
)
```

**Example:**
```kotlin
QuoVadisBackHandler(
    enabled = canGoBack,
    onBack = { navigator.goBack() }
) {
    MyScreen()
}
```

---

### BackNavigationEvent

**Platform-agnostic representation of a back navigation event.**

```kotlin
@Immutable
data class BackNavigationEvent(
    val progress: Float,
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val swipeEdge: Int = EDGE_LEFT
) {
    companion object {
        const val EDGE_LEFT: Int = 0
        const val EDGE_RIGHT: Int = 1
    }
}
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `progress` | `Float` | Progress of the back gesture, 0.0 to 1.0 |
| `touchX` | `Float` | X coordinate of the touch point |
| `touchY` | `Float` | Y coordinate of the touch point |
| `swipeEdge` | `Int` | Which edge the swipe started from (`EDGE_LEFT` or `EDGE_RIGHT`) |

---

### BackTransitionState

**Represents the state of a back navigation transition.**

```kotlin
sealed interface BackTransitionState {
    data object Idle : BackTransitionState
    data class InProgress(val event: BackNavigationEvent) : BackTransitionState
}
```

#### States

| State | Description |
|-------|-------------|
| `Idle` | No back gesture in progress |
| `InProgress` | Back gesture is active, contains the current `BackNavigationEvent` |

---

### ScreenNavigationInfo

**NavigationEventInfo implementation for Quo Vadis screens.**

```kotlin
data class ScreenNavigationInfo(
    val screenId: String,
    val displayName: String? = null,
    val route: String? = null
) : NavigationEventInfo()
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `screenId` | `String` | Unique identifier for the screen |
| `displayName` | `String?` | Optional display name for accessibility |
| `route` | `String?` | Optional route pattern |

---

### NoScreenInfo

**Represents "no screen" info for when at the root.**

```kotlin
data object NoScreenInfo : NavigationEventInfo()
```

Use this when there's no previous screen (at root of navigation).

---

### BackAnimationController

**Controller for predictive back animations in the hierarchical navigation system.**

```kotlin
@Stable
class BackAnimationController {
    var isAnimating: Boolean
    var progress: Float
    var currentEvent: BackNavigationEvent?
    
    fun startAnimation(event: BackNavigationEvent)
    fun updateProgress(event: BackNavigationEvent)
    fun completeAnimation()
    fun cancelAnimation()
}
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isAnimating` | `Boolean` | Whether a back gesture animation is currently in progress |
| `progress` | `Float` | Current gesture progress (0.0 to 1.0) |
| `currentEvent` | `BackNavigationEvent?` | Most recent event with full gesture details (null when idle) |

#### Methods

| Method | Description |
|--------|-------------|
| `startAnimation(event)` | Call when back gesture begins |
| `updateProgress(event)` | Call repeatedly as gesture progresses |
| `completeAnimation()` | Call when gesture completes (navigation occurs) |
| `cancelAnimation()` | Call when gesture is cancelled (user didn't complete) |

#### CompositionLocal Access

```kotlin
val LocalBackAnimationController = staticCompositionLocalOf<BackAnimationController?> { null }

@Composable
fun rememberBackAnimationController(): BackAnimationController
```

#### Example: Using in Renderers

```kotlin
@Composable
fun StackRenderer(node: StackNode, scope: NavRenderScope) {
    val backController = LocalBackAnimationController.current
    
    // Apply animation based on gesture progress
    val offsetX = if (backController?.isAnimating == true) {
        with(LocalDensity.current) {
            (backController.progress * 100).dp.toPx()
        }
    } else 0f
    
    Box(modifier = Modifier.offset { IntOffset(offsetX.toInt(), 0) }) {
        // Content
    }
}
```

---

### Platform Behaviors

| Platform | Behavior |
|----------|----------|
| **Android 14+** | Full predictive back with system animation (home preview when closing app) |
| **Android 13** | Gesture detection with custom animation, no system preview |
| **Android <13** | Falls back to immediate back |
| **iOS** | Edge swipe gesture with custom animation |
| **Desktop** | No gesture, back via keyboard/UI only |
| **Web** | Browser back button, no gesture |

---

## Predictive Back Configuration

### PredictiveBackMode

Controls how predictive back gestures are handled across the navigation tree.

```kotlin
public enum class PredictiveBackMode {
    ROOT_ONLY,
    FULL_CASCADE
}
```

| Mode | Description |
|------|-------------|
| `ROOT_ONLY` | Default. Only the root stack handles predictive back gestures. Nested stacks pop instantly after gesture completion. Recommended for simple navigation structures and performance-constrained devices. |
| `FULL_CASCADE` | All stacks handle predictive back, including animated cascade when popping containers. When back would cascade (pop entire container), the gesture shows a preview of the target screen with the container animating away. Recommended for apps with complex nested navigation. |

**Usage:**

```kotlin
NavigationHost(
    navigator = navigator,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE // Enable cascade animations
)
```

---

### CascadeBackState

State information for predictive back gestures that may cascade through multiple container levels.

```kotlin
@Stable
public data class CascadeBackState(
    val sourceNode: NavNode,      // Node that initiated the back gesture
    val exitingNode: NavNode,     // Node being removed (screen, stack, or tab)
    val targetNode: NavNode?,     // Node revealed after back (null if closing app)
    val cascadeDepth: Int,        // 0 = normal pop, 1+ = cascade levels
    val delegatesToSystem: Boolean // True if back would close the app
)
```

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `sourceNode` | `NavNode` | The node that initiated the back gesture (usually a ScreenNode) |
| `exitingNode` | `NavNode` | The node that will be visually removed. Could be ScreenNode (normal pop), StackNode (cascade pop), or TabNode (tab cascade) |
| `targetNode` | `NavNode?` | The node revealed after back completes. `null` if delegating to system |
| `cascadeDepth` | `Int` | How many levels the cascade goes. 0 = normal pop, 1+ = deeper cascade |
| `delegatesToSystem` | `Boolean` | Whether back would close the app |

#### Factory Function

```kotlin
// Calculate cascade state for current navigation
val cascadeState = calculateCascadeBackState(rootNode)

// Check what will exit
when (cascadeState.exitingNode) {
    is ScreenNode -> // Normal screen pop
    is StackNode -> // Entire stack will be popped
    is TabNode -> // Entire tab container will be popped
}
```

#### Helper Function

```kotlin
// Quick check if back would cascade
if (wouldCascade(rootNode)) {
    // Container will be popped, not just a screen
}
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

// 2. Create screen registry
val appScreenRegistry = ScreenRegistry {
    screen(HomeScreen) { navigator ->
        HomeUI(onItemClick = { id ->
            navigator.navigate(
                DetailScreen(id),
                NavigationTransitions.SlideHorizontal
            )
        })
    }
    
    screen<DetailScreen> { destination, navigator ->
        DetailUI(
            itemId = destination.itemId,
            onBack = { navigator.navigateBack() }
        )
    }
}

// 3. Setup navigation
@Composable
fun App() {
    val navigator = rememberNavigator(startDestination = HomeScreen)
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = appScreenRegistry,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE
    )
}
```

