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

#### Implementations
- `SimpleDestination(route, arguments)` - Basic destination with arguments
- `TypedDestination<T>(route, data)` - Typed destination with data
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

## MVI Package (`navigation.mvi`)

### NavigationIntent

```kotlin
sealed interface NavigationIntent {
    data class Navigate(destination, transition)
    object NavigateBack
    object NavigateUp
    data class NavigateAndClearAll(destination)
    data class NavigateAndClearTo(destination, clearRoute, inclusive)
    data class NavigateAndReplace(destination, transition)
    data class HandleDeepLink(uri)
}
```

### NavigationViewModel

```kotlin
abstract class NavigationViewModel(navigator: Navigator) : ViewModel() {
    val navigationState: StateFlow<NavigationState>
    val navigationEffects: SharedFlow<NavigationEffect>
    
    fun handleNavigationIntent(intent: NavigationIntent)
}
```

### Effect Collection

```kotlin
@Composable
fun SharedFlow<NavigationEffect>.collectAsEffect(
    onEffect: suspend (NavigationEffect) -> Unit
)
```

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

---

## Integration Package (`navigation.integration`)

### DI Support

```kotlin
interface NavigationFactory {
### Pattern 6: Predictive Back Navigation
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

### Pattern 3: MVI Navigation
```kotlin
class MyViewModel(nav: Navigator) : NavigationViewModel(nav) {
    fun onItemClick(id: String) {
        handleNavigationIntent(
            NavigationIntent.Navigate(DetailDestination(id))
        )
    }
}
```

### Pattern 4: Deep Links
```kotlin
val handler = DefaultDeepLinkHandler()
handler.register("app://product/{id}") { params ->
    ProductDestination(params["id"]!!)
}
navigator.handleDeepLink(DeepLink.parse("app://product/123"))
```

### Pattern 5: Custom Transitions
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

