# Navigator

The `Navigator` interface is the central navigation controller in Quo Vadis. It manages navigation state as an immutable tree and provides operations for forward navigation, back navigation, deep link handling, and result passing.

## Table of Contents

- [Overview](#overview)
- [Core Properties](#core-properties)
  - [State Properties](#state-properties)
  - [Transition Properties](#transition-properties)
  - [Manager References](#manager-references)
- [Basic Navigation Operations](#basic-navigation-operations)
  - [Forward Navigation](#forward-navigation)
  - [Back Navigation](#back-navigation)
  - [Clear and Replace Operations](#clear-and-replace-operations)
- [Navigation with Results](#navigation-with-results)
  - [ReturnsResult Interface](#returnsresult-interface)
  - [Navigating for a Result](#navigating-for-a-result)
  - [Returning a Result](#returning-a-result)
- [Pane Operations](#pane-operations)
- [Deep Link Handling](#deep-link-handling)
- [Predictive Back Support](#predictive-back-support)
- [State Observation](#state-observation)
- [TreeNavigator Configuration](#treenavigator-configuration)
- [Common Patterns](#common-patterns)

---

## Overview

The `Navigator` interface serves as the primary navigation API for the application. It provides:

- **State Management**: Navigation state as an immutable tree (`NavNode`)
- **Navigation Operations**: Push, pop, replace, and clear operations
- **Result Passing**: Type-safe result passing between screens
- **Deep Linking**: URI-based navigation with pattern matching
- **Predictive Back**: Android 14+ predictive back gesture support

### Navigator Interface

The base `Navigator` interface defines core navigation capabilities:

```kotlin
@Stable
interface Navigator : BackPressHandler {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val previousDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>
    val config: NavigationConfig

    fun navigate(destination: NavDestination, transition: NavigationTransition? = null)
    fun navigateBack(): Boolean
    fun navigateAndClearTo(destination: NavDestination, clearRoute: String?, inclusive: Boolean)
    fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?)
    fun navigateAndClearAll(destination: NavDestination)
    fun handleDeepLink(uri: String): Boolean
    fun handleDeepLink(deepLink: DeepLink)
    fun getDeepLinkRegistry(): DeepLinkRegistry
    fun updateState(newState: NavNode, transition: NavigationTransition?)
}
```

### TreeNavigator Implementation

`TreeNavigator` is the standard implementation that additionally implements:

- `PaneNavigator` - For multi-pane adaptive layouts
- `TransitionController` - For animation state management (internal)
- `ResultCapable` - For navigation result passing (internal)

### Obtaining a Navigator Instance

**Via Koin (Recommended):**

```kotlin
// DI.kt - Registration
val navigationModule = module {
    single<NavigationConfig> {
        ComposeAppNavigationConfig +
            Feature1NavigationConfig +
            Feature2NavigationConfig
    }
    
    single<Navigator> {
        val config = get<NavigationConfig>()
        val initialState = config.buildNavNode(
            destinationClass = MainTabs::class,
            parentKey = null
        ) ?: error("No container registered for MainTabs")
        
        TreeNavigator(
            config = config,
            initialState = initialState
        )
    }
}

// In Composables
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    // Use navigator
}
```

**Manual Creation:**

```kotlin
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = buildInitialState()
)
```

---

## Core Properties

### State Properties

| Property | Type | Description |
|----------|------|-------------|
| `state` | `StateFlow<NavNode>` | The complete navigation tree (single source of truth) |
| `currentDestination` | `StateFlow<NavDestination?>` | The active leaf destination |
| `previousDestination` | `StateFlow<NavDestination?>` | The destination before current (for back preview) |
| `canNavigateBack` | `StateFlow<Boolean>` | Whether back navigation is possible |
| `config` | `NavigationConfig` | The navigation configuration with all registries |

#### state: StateFlow<NavNode>

The primary source of truth for all navigation state. The tree structure can contain:

- `StackNode` - Linear navigation history
- `TabNode` - Parallel tab-based navigation
- `PaneNode` - Adaptive multi-pane layouts
- `ScreenNode` - Individual destinations

```kotlin
// Observe navigation tree
navigator.state.collect { navNode ->
    val depth = calculateTreeDepth(navNode)
    println("Navigation depth: $depth")
}
```

#### currentDestination: StateFlow<NavDestination?>

Derived from `state` for convenience. Returns the deepest active `ScreenNode`'s destination.

```kotlin
// In ViewModel/Container
navigator.currentDestination.collect { destination ->
    when (destination) {
        is HomeDestination.Article -> trackArticleView(destination.articleId)
        is ProfileDestination.Settings -> showSettingsHint()
        else -> Unit
    }
}
```

#### previousDestination: StateFlow<NavDestination?>

The destination that would be shown after back navigation. Useful for:
- Showing "back to X" hints
- Determining if back is semantically meaningful

```kotlin
val showBackHint by navigator.previousDestination.collectAsState()

if (showBackHint != null) {
    Text("← Back to ${showBackHint!!.route}")
}
```

#### canNavigateBack: StateFlow<Boolean>

Indicates whether `navigateBack()` would succeed. Use for UI state:

```kotlin
val canGoBack by navigator.canNavigateBack.collectAsState()

IconButton(
    onClick = { navigator.navigateBack() },
    enabled = canGoBack
) {
    Icon(Icons.Default.ArrowBack, "Back")
}
```

### Transition Properties

`TreeNavigator` exposes transition state for animations:

| Property | Type | Description |
|----------|------|-------------|
| `transitionState` | `StateFlow<TransitionState>` | Current animation state |
| `currentTransition` | `StateFlow<NavigationTransition?>` | Active transition definition |

#### TransitionState

The sealed interface representing animation states:

```kotlin
sealed interface TransitionState {
    data object Idle : TransitionState
    
    data class InProgress(
        val transition: NavigationTransition,
        val progress: Float,          // 0.0 to 1.0
        val fromKey: String?,
        val toKey: String?
    ) : TransitionState
    
    data class PredictiveBack(
        val progress: Float,
        val currentKey: String?,
        val previousKey: String?,
        val touchX: Float,            // 0.0 to 1.0
        val touchY: Float,            // 0.0 to 1.0
        val isCommitted: Boolean
    ) : TransitionState
    
    data class Seeking(
        val transition: NavigationTransition,
        val progress: Float,
        val isPaused: Boolean
    ) : TransitionState
}
```

### Manager References

`TreeNavigator` provides access to internal managers via `ResultCapable`:

| Property | Type | Description |
|----------|------|-------------|
| `resultManager` | `NavigationResultManager` | Handles result passing between screens |

---

## Basic Navigation Operations

### Forward Navigation

#### navigate(destination, transition?)

Push a destination onto the active stack:

```kotlin
// Simple navigation
navigator.navigate(MasterDetailDestination.List)

// With explicit transition
navigator.navigate(
    destination = MasterDetailDestination.Detail(itemId = "123"),
    transition = NavigationTransitions.SlideHorizontal
)

// Navigation respects destination's default transition
@Destination(route = "home/feed")
@Transition(type = TransitionType.Fade)
data object Feed : HomeDestination()

navigator.navigate(HomeDestination.Feed)  // Uses Fade transition
```

**From screen content** (composeApp demo pattern):

```kotlin
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    NavigationPatternCard(
        title = "Master-Detail",
        onClick = {
            navigator.navigate(
                MasterDetailDestination.List,
                NavigationTransitions.SlideHorizontal
            )
        }
    )
}
```

### Back Navigation

#### navigateBack(): Boolean

Pop from the active stack. Returns `true` if navigation occurred.

```kotlin
// Simple back
val didNavigate = navigator.navigateBack()

// Conditional back (e.g., save prompt)
fun onBackPressed() {
    if (hasUnsavedChanges) {
        showSaveDialog()
    } else {
        navigator.navigateBack()
    }
}
```

**Back behavior in different contexts:**

| Context | Behavior |
|---------|----------|
| Stack with > 1 entry | Pop top entry |
| Stack with 1 entry | Cascade to parent or delegate to system |
| Tab container | Switch to initial tab, then cascade |
| Pane container | Depends on `PaneBackBehavior` and window size |

### Clear and Replace Operations

#### navigateAndClearTo(destination, clearRoute?, inclusive)

Navigate while clearing the backstack to a specific point:

```kotlin
// Clear to home, then push settings
navigator.navigateAndClearTo(
    destination = SettingsDestination.Main,
    clearRoute = "home/feed",
    inclusive = false  // Keep home/feed in stack
)

// Clear including the target route
navigator.navigateAndClearTo(
    destination = HomeDestination.Feed,
    clearRoute = "auth/login",
    inclusive = true  // Remove login screen too
)
```

#### navigateAndReplace(destination, transition?)

Replace the current destination without affecting the rest of the stack:

```kotlin
// Replace current screen
navigator.navigateAndReplace(
    destination = ProfileDestination.EditProfile,
    transition = NavigationTransitions.Fade
)
```

Use case: Wizard flows where steps replace each other:

```kotlin
// Step 1 → Step 2 (replace, not push)
navigator.navigateAndReplace(ProcessDestination.Step2)
```

#### navigateAndClearAll(destination)

Reset navigation to a single destination (clear entire backstack):

```kotlin
// After logout - reset to login
navigator.navigateAndClearAll(AuthDestination.Login)

// After onboarding - reset to main
navigator.navigateAndClearAll(MainTabs.HomeTab)
```

---

## Navigation with Results

Quo Vadis supports type-safe result passing between screens, similar to `startActivityForResult`.

### ReturnsResult Interface

Marker interface for destinations that return results:

```kotlin
/**
 * @param R The result type (must be non-null)
 */
interface ReturnsResult<R : Any>
```

### Defining a Result-Returning Destination

```kotlin
// The result type
data class SelectedItem(val id: String, val name: String)

// Destination that returns a result
@Destination(route = "picker/items")
data object ItemPicker : PickerDestination(), ReturnsResult<SelectedItem>
```

### Navigating for a Result

Use the `navigateForResult` extension function to navigate and suspend until a result is returned:

```kotlin
suspend fun <R : Any, D> Navigator.navigateForResult(
    destination: D
): R? where D : NavDestination, D : ReturnsResult<R>
```

**Usage in a container:**

```kotlin
class ResultDemoContainer(
    scope: NavigationContainerScope
) : NavigationContainer<ResultDemoState, Intent, Action>(scope) {

    private suspend fun Ctx.pickItem() {
        updateState { copy(isLoading = true) }
        
        coroutineScope.launch {
            // Navigate and await result (suspends until result or cancellation)
            val result: SelectedItem? = navigator.navigateForResult(
                ResultDemoDestination.ItemPicker
            )
            
            updateState {
                copy(
                    selectedItem = result,
                    isLoading = false,
                    message = if (result != null) 
                        "Selected: ${result.name}" 
                    else 
                        "Selection cancelled"
                )
            }
        }
    }
}
```

### Returning a Result

Use `navigateBackWithResult` to return a result and navigate back:

```kotlin
fun <R : Any> Navigator.navigateBackWithResult(result: R)
```

**Usage:**

```kotlin
class ItemPickerContainer(
    scope: NavigationContainerScope
) : NavigationContainer<PickerState, Intent, Action>(scope) {

    private fun Ctx.selectItem(item: Item) {
        val result = SelectedItem(id = item.id, name = item.name)
        
        // Return result and navigate back
        navigator.navigateBackWithResult(result)
    }
    
    private fun Ctx.cancel() {
        // Navigate back without result (caller receives null)
        navigator.navigateBack()
    }
}
```

### Result Lifecycle

| Action | Result |
|--------|--------|
| `navigateBackWithResult(value)` | Caller receives `value` |
| `navigateBack()` | Caller receives `null` |
| Screen destroyed (cleared) | Caller receives `null` |

---

## Pane Operations

For adaptive multi-pane layouts, cast to `PaneNavigator` or use the extension:

```kotlin
val paneNavigator = navigator.asPaneNavigator()
```

### Available Operations

| Method | Description |
|--------|-------------|
| `isPaneAvailable(role)` | Check if a pane role is configured |
| `paneContent(role)` | Get the current content of a pane |
| `navigateToPane(destination, role)` | Navigate within a specific pane |
| `navigateBackInPane(role)` | Pop from a specific pane's stack |

### Pane Navigation Pattern

From the messages demo:

```kotlin
@Screen(MessagesPane.ConversationList::class)
@Composable
fun ConversationListScreen(navigator: Navigator = koinInject()) {
    LazyColumn {
        items(conversations) { conversation ->
            ConversationItem(
                conversation = conversation,
                onClick = {
                    // Navigate in supporting pane (replaces content)
                    navigator.asPaneNavigator()
                        ?.navigateToPane(
                            MessagesPane.ConversationDetail(conversation.id)
                        )
                }
            )
        }
    }
}
```

### Checking Pane Availability

```kotlin
val paneNavigator = navigator.asPaneNavigator()

if (paneNavigator?.isPaneAvailable(PaneRole.Extra) == true) {
    // Show extra content option
    Button(onClick = { 
        paneNavigator.navigateToPane(ExtraContent, PaneRole.Extra) 
    }) {
        Text("Show Extra Panel")
    }
}
```

---

## Deep Link Handling

### handleDeepLink(uri): Boolean

Process a URI string and navigate to the matching destination:

```kotlin
// Handle incoming deep link
val handled = navigator.handleDeepLink("app://master_detail/detail/42")

if (!handled) {
    showError("Unknown deep link")
}
```

### handleDeepLink(deepLink)

Alternative using a `DeepLink` object:

```kotlin
val deepLink = DeepLink.parse("app://search/results?query=kotlin&page=2")
navigator.handleDeepLink(deepLink)
```

### getDeepLinkRegistry(): DeepLinkRegistry

Access the registry for runtime pattern registration:

```kotlin
// Register a promo handler at runtime
LaunchedEffect(Unit) {
    navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
        PromoDestination(code = params["code"]!!)
    }
}

// Now "app://promo/SAVE20" navigates to PromoDestination("SAVE20")
```

### Deep Link Demo Pattern

From [DeepLinkDemoScreen.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/DeepLinkDemoScreen.kt):

```kotlin
@Screen(DeepLinkDestination.Demo::class)
@Composable
fun DeepLinkDemoScreen(navigator: Navigator = koinInject()) {
    // Runtime registration
    LaunchedEffect(Unit) {
        navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
            PromoDestination(code = params["code"]!!)
        }
    }
    
    // UI with deep link testing
    DeepLinkCard(
        title = "Navigate to Item Detail",
        deepLink = "app://master_detail/detail/42",
        onClick = { 
            navigator.handleDeepLink("app://master_detail/detail/42") 
        }
    )
}
```

---

## Predictive Back Support

`TreeNavigator` supports Android 14+ predictive back gestures through the `TransitionController` interface.

### Gesture Lifecycle Methods

| Method | Called When |
|--------|-------------|
| `startPredictiveBack()` | User initiates back gesture |
| `updatePredictiveBack(progress, touchX, touchY)` | Gesture moves |
| `cancelPredictiveBack()` | User cancels gesture |
| `commitPredictiveBack()` | User completes gesture |

### Integration

Predictive back is automatically handled when using `NavigationHost` with `enablePredictiveBack = true`:

```kotlin
@Composable
fun DemoApp() {
    val navigator = koinInject<Navigator>()
    
    NavigationHost(
        navigator = navigator,
        enablePredictiveBack = true,  // Enable gesture support
        windowSizeClass = calculateWindowSizeClass()
    )
}
```

### Observing Predictive Back State

```kotlin
val transitionState by (navigator as? TreeNavigator)
    ?.transitionState
    ?.collectAsState() 
    ?: remember { mutableStateOf(TransitionState.Idle) }

when (transitionState) {
    is TransitionState.PredictiveBack -> {
        val state = transitionState as TransitionState.PredictiveBack
        // Show preview with progress: state.progress
        // Touch position: state.touchX, state.touchY
        // Whether committed: state.isCommitted
    }
    else -> { /* Normal state */ }
}
```

---

## State Observation

### Collecting State in Compose

```kotlin
@Composable
fun NavigationAwareContent(navigator: Navigator) {
    // Current destination
    val currentDest by navigator.currentDestination.collectAsState()
    
    // Can go back
    val canBack by navigator.canNavigateBack.collectAsState()
    
    // Full tree (for debugging)
    val navTree by navigator.state.collectAsState()
    
    // Transition state (TreeNavigator only)
    val transition by (navigator as? TreeNavigator)
        ?.transitionState
        ?.collectAsState()
        ?: remember { mutableStateOf(TransitionState.Idle) }
}
```

### Reacting to Navigation Changes

```kotlin
@Composable
fun AnalyticsTracker(navigator: Navigator) {
    val currentDestination by navigator.currentDestination.collectAsState()
    
    LaunchedEffect(currentDestination) {
        currentDestination?.let { dest ->
            analytics.trackScreen(dest.route)
        }
    }
}
```

### Derived Properties

```kotlin
@Composable
fun NavigationInfo(navigator: Navigator) {
    val state by navigator.state.collectAsState()
    
    // Derive depth
    val depth = remember(state) {
        state.activePathToLeaf().size
    }
    
    // Derive tab index
    val activeTab = remember(state) {
        state.findFirst<TabNode>()?.activeStackIndex
    }
    
    Text("Depth: $depth, Tab: ${activeTab ?: "N/A"}")
}
```

---

## TreeNavigator Configuration

### Constructor Parameters

```kotlin
class TreeNavigator(
    override val config: NavigationConfig = NavigationConfig.Empty,
    private val coroutineScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    ),
    initialState: NavNode? = null
)
```

| Parameter | Description | Default |
|-----------|-------------|---------|
| `config` | Navigation configuration with all registries | `NavigationConfig.Empty` |
| `coroutineScope` | Scope for derived state computations | Main dispatcher scope |
| `initialState` | Initial navigation tree | Empty stack |

### NavigationConfig

The config combines all registries needed for navigation:

```kotlin
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val containerRegistry: ContainerRegistry
    val scopeRegistry: ScopeRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val transitionRegistry: TransitionRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    // Build initial state from destination class
    fun buildNavNode(destinationClass: KClass<*>, parentKey: String?): NavNode?
}
```

### Combining Configs (Multi-Module)

```kotlin
single<NavigationConfig> {
    ComposeAppNavigationConfig +      // Demo app
        Feature1NavigationConfig +    // Feature module 1
        Feature2NavigationConfig      // Feature module 2
}
```

### Runtime Properties

`TreeNavigator` has additional runtime properties set by `NavigationHost`:

```kotlin
// Set by NavigationHost
navigator.backHandlerRegistry = backHandlerRegistry  // User-defined back handlers
navigator.windowSizeClass = calculateWindowSizeClass()  // For adaptive pane behavior
```

---

## Common Patterns

### Navigation from Screen Content

Inject navigator and call navigation methods directly:

```kotlin
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    Column {
        Button(onClick = { 
            navigator.navigate(ProfileDestination.Main) 
        }) {
            Text("Go to Profile")
        }
        
        Button(
            onClick = { navigator.navigateBack() },
            enabled = navigator.canNavigateBack.collectAsState().value
        ) {
            Text("Back")
        }
    }
}
```

### Navigation from MVI Containers

Use the navigator provided through `NavigationContainerScope`:

```kotlin
class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {

    override val store = store(ProfileState.Loading) {
        reduce { intent ->
            when (intent) {
                is ProfileIntent.NavigateToSettings -> {
                    navigator.navigate(MainTabs.SettingsTab.Main)
                }
                is ProfileIntent.NavigateBack -> {
                    navigator.navigateBack()
                }
                is ProfileIntent.Logout -> {
                    repository.logout()
                    navigator.navigateAndClearAll(AuthDestination.Login)
                }
                // ... other intents
            }
        }
    }
}
```

### Conditional Navigation

```kotlin
fun handleNavigation(navigator: Navigator, user: User?) {
    when {
        user == null -> {
            // Not logged in - go to auth
            navigator.navigateAndClearAll(AuthDestination.Login)
        }
        !user.hasCompletedOnboarding -> {
            // Needs onboarding
            navigator.navigate(OnboardingDestination.Welcome)
        }
        else -> {
            // Normal flow
            navigator.navigate(MainTabs.HomeTab)
        }
    }
}
```

### Navigation Guards

```kotlin
class AuthGuardContainer(
    scope: NavigationContainerScope,
    private val authRepository: AuthRepository
) : NavigationContainer<GuardState, Intent, Action>(scope) {

    init {
        // Watch auth state
        coroutineScope.launch {
            authRepository.isAuthenticated.collect { isAuth ->
                if (!isAuth) {
                    // Force logout - clear and go to login
                    navigator.navigateAndClearAll(AuthDestination.Login)
                }
            }
        }
    }
}
```

### Tab Switching with State Preservation

```kotlin
@Composable
fun BottomNavigationBar(navigator: Navigator) {
    val state by navigator.state.collectAsState()
    val tabNode = state.findFirst<TabNode>()
    val activeIndex = tabNode?.activeStackIndex ?: 0
    
    NavigationBar {
        tabNode?.tabMetadata?.forEachIndexed { index, meta ->
            NavigationBarItem(
                selected = index == activeIndex,
                onClick = {
                    // Navigate to tab's start destination
                    // TreeNavigator handles tab switching automatically
                    val tabDestination = tabNode.stacks[index]
                        .children.firstOrNull()
                        ?.activeLeaf()
                        ?.destination
                    
                    tabDestination?.let { navigator.navigate(it) }
                },
                icon = { Icon(meta.icon, meta.label) },
                label = { Text(meta.label) }
            )
        }
    }
}
```

---

## See Also

- [NAV-NODES.md](NAV-NODES.md) - Navigation node types and tree structure
- [ARCHITECTURE.md](ARCHITECTURE.md) - Full architecture documentation
- [DSL-CONFIG.md](DSL-CONFIG.md) - DSL configuration options
- [ANNOTATIONS.md](ANNOTATIONS.md) - Annotation reference
