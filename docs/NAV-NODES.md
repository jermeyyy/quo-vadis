# Navigation Nodes

This document covers the navigation node types in Quo Vadis—the building blocks of the tree-based navigation model.

## Table of Contents

- [Overview](#overview)
- [NavNode Types](#navnode-types)
  - [ScreenNode](#screennode)
  - [StackNode](#stacknode)
  - [TabNode](#tabnode)
  - [PaneNode](#panenode)
- [Navigation Lifecycle](#navigation-lifecycle)
  - [LifecycleAwareNode Interface](#lifecycleawarenode-interface)
  - [State Transitions](#state-transitions)
  - [Lifecycle Callbacks](#lifecycle-callbacks)
  - [MVI Container Integration](#mvi-container-integration)
- [CompositionLocals](#compositionlocals)
- [Code Examples](#code-examples)
- [Best Practices](#best-practices)

---

## Overview

Quo Vadis represents navigation state as an **immutable tree** of `NavNode` objects. This tree-based model enables:

- **Structural sharing**: Unchanged subtrees are reused across state updates
- **Predictable state**: Navigation history is explicitly modeled
- **Serialization**: Full support for process death survival via `kotlinx.serialization`
- **Type safety**: Sealed hierarchy ensures exhaustive handling

### Key Design Principle

> The NavNode tree represents **logical navigation state**, NOT visual layout state.

| NavNode Stores | Renderer Determines |
|----------------|---------------------|
| Which destinations exist | Which panes are visible |
| Which node is "active" (has focus) | Layout arrangement |
| Adaptation strategies | Animation states |

This separation enables **adaptive morphing**—when a device rotates, the navigation state remains unchanged while the visual representation adapts.

### Tree Structure Example

```
StackNode (root)
└── TabNode (MainTabs)
    ├── StackNode (HomeTab)
    │   ├── ScreenNode (Home)
    │   └── ScreenNode (Detail)
    └── StackNode (ProfileTab)
        └── ScreenNode (Profile)
```

---

## NavNode Types

All nodes implement the `NavNode` sealed interface:

```kotlin
@Serializable
sealed interface NavNode {
    val key: String        // Unique identifier
    val parentKey: String? // Parent node key (null for root)
}
```

### ScreenNode

**Leaf node representing a single destination.**

`ScreenNode` is the terminal state in the navigation tree—it cannot contain children. It holds a reference to the `NavDestination` that defines the content to render.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | `String` | Unique identifier for this screen instance |
| `parentKey` | `String?` | Key of the containing StackNode |
| `destination` | `NavDestination` | The destination data (route, arguments) |

#### Lifecycle Properties (Transient)

| Property | Type | Description |
|----------|------|-------------|
| `isAttachedToNavigator` | `Boolean` | Whether node is in the navigation tree |
| `isDisplayed` | `Boolean` | Whether node is currently visible |
| `composeSavedState` | `Map<String, List<Any?>>?` | Saved Compose state |

#### When to Use

- Every visible screen destination
- Always contained within a `StackNode`
- One `ScreenNode` per unique screen instance

#### Declaration Example

Screens are typically defined via annotations and bound with `@Screen`:

```kotlin
// Destination definition
@Destination(route = "home/article/{articleId}")
data class Article(val articleId: String) : HomeDestination()

// Screen binding
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Text("Article: ${destination.articleId}")
}
```

---

### StackNode

**Container node representing a linear navigation stack.**

`StackNode` maintains an ordered list of child nodes where the **last element is active** (visible). Push operations append to the list, pop operations remove from the tail.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | `String` | Unique identifier for this stack |
| `parentKey` | `String?` | Key of containing TabNode/PaneNode (null if root) |
| `children` | `List<NavNode>` | Ordered list of child nodes (last = active) |
| `scopeKey` | `String?` | Identifier for scope-aware navigation |

#### Computed Properties

| Property | Type | Description |
|----------|------|-------------|
| `activeChild` | `NavNode?` | Currently visible child (last in list) |
| `canGoBack` | `Boolean` | `true` if `size > 1` |
| `isEmpty` | `Boolean` | `true` if no children |
| `size` | `Int` | Number of entries in stack |

#### Behavior

| Operation | Effect |
|-----------|--------|
| **Push** | Appends new node to `children` |
| **Pop** | Removes last node from `children` |
| **Empty Stack** | May cascade pop to parent (configurable) |

#### Scope-Aware Navigation

When `scopeKey` is set, the navigator checks if destinations belong to this stack's scope. Out-of-scope destinations navigate to the parent stack, preserving the current stack for predictive back gestures.

```kotlin
// Auth flow with scope boundary
@Stack(name = "authStack", startDestination = AuthFlow.Login::class)
sealed class AuthFlow : NavDestination {
    @Destination(route = "auth/login")
    data object Login : AuthFlow()
    
    @Destination(route = "auth/register")
    data object Register : AuthFlow()
}
// Navigating to a non-AuthFlow destination will exit this stack
```

#### When to Use

- Root of navigation hierarchy
- Within each tab of a `TabNode`
- Within each pane of a `PaneNode`
- Any linear navigation flow

---

### TabNode

**Container node for tabbed navigation with parallel stacks.**

`TabNode` maintains multiple `StackNode` instances—one for each tab—along with an `activeStackIndex` indicating the selected tab. Each tab preserves its own navigation history independently.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | `String` | Unique identifier for this tab container |
| `parentKey` | `String?` | Key of containing node (null if root) |
| `stacks` | `List<StackNode>` | One StackNode per tab |
| `activeStackIndex` | `Int` | Index of currently active tab (0-based) |
| `wrapperKey` | `String?` | Key for `ContainerRegistry` wrapper lookup |
| `tabMetadata` | `List<GeneratedTabMetadata>` | Metadata for each tab (label, icon) |
| `scopeKey` | `String?` | Identifier for scope-aware navigation |

#### Computed Properties

| Property | Type | Description |
|----------|------|-------------|
| `activeStack` | `StackNode` | Currently selected tab's stack |
| `tabCount` | `Int` | Number of tabs |

#### Lifecycle Properties (Transient)

| Property | Type | Description |
|----------|------|-------------|
| `uuid` | `String` | Unique instance identifier (for scoping) |
| `isAttachedToNavigator` | `Boolean` | Whether node is in the navigation tree |
| `isDisplayed` | `Boolean` | Whether node is currently visible |
| `composeSavedState` | `Map<String, List<Any?>>?` | Saved Compose state |

#### Behavior

| Operation | Effect |
|-----------|--------|
| **Switch Tab** | Updates `activeStackIndex` |
| **Push** | Affects only the active stack |
| **Pop** | Removes from active stack; may switch tabs |

#### Requirements

- Must have at least one stack
- `activeStackIndex` must be valid index

#### Declaration Example

From the demo app [MainTabs.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MainTabs.kt):

```kotlin
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    @Transition(type = TransitionType.Fade)
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    @Transition(type = TransitionType.Fade)
    data object ProfileTab : MainTabs()

    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    sealed class SettingsTab : MainTabs() {
        @Destination(route = "settings/main")
        data object Main : SettingsTab()

        @Destination(route = "settings/profile")
        data object Profile : SettingsTab()
    }
}
```

#### When to Use

- Bottom navigation with independent back stacks
- Tab-based navigation patterns
- Any UI with parallel navigation sections

---

### PaneNode

**Container node for adaptive multi-pane layouts.**

`PaneNode` represents layouts where multiple panes can be displayed simultaneously on large screens or collapsed to single-pane on compact screens.

#### Properties

| Property | Type | Description |
|----------|------|-------------|
| `key` | `String` | Unique identifier for this pane container |
| `parentKey` | `String?` | Key of containing node (null if root) |
| `paneConfigurations` | `Map<PaneRole, PaneConfiguration>` | Pane role to configuration mapping |
| `activePaneRole` | `PaneRole` | Pane with navigation focus |
| `backBehavior` | `PaneBackBehavior` | Back navigation strategy |
| `scopeKey` | `String?` | Identifier for scope-aware navigation |

#### Pane Roles

| Role | Description |
|------|-------------|
| `PaneRole.Primary` | Main content pane (required) |
| `PaneRole.Supporting` | Detail/secondary content |
| `PaneRole.Extra` | Additional content (rare) |

#### Adaptation Strategies

Each pane has an `AdaptStrategy` defining behavior when space is limited:

| Strategy | Behavior |
|----------|----------|
| `AdaptStrategy.Hide` | Hide the pane completely |
| `AdaptStrategy.Levitate` | Show as overlay (modal-like) |
| `AdaptStrategy.Reflow` | Stack vertically under another pane |

#### Adaptive Behavior

| Screen Size | Behavior |
|-------------|----------|
| **Compact** | Only `activePaneRole` is visible |
| **Medium** | Primary visible, others can levitate |
| **Expanded** | Multiple panes displayed side-by-side |

#### Back Navigation Behaviors

| Behavior | Description |
|----------|-------------|
| `PopUntilScaffoldValueChange` | Pop until visible pane layout changes |
| `PopUntilContentChange` | Pop until any visible pane's content changes |

#### Computed Properties

| Property | Type | Description |
|----------|------|-------------|
| `activePaneContent` | `NavNode?` | Content of the focused pane |
| `paneCount` | `Int` | Number of configured panes |
| `configuredRoles` | `Set<PaneRole>` | All configured pane roles |

#### Declaration Example

From the demo app [MessagesPaneDestination.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MessagesPaneDestination.kt):

```kotlin
@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(val conversationId: String) : MessagesPane()
}
```

#### Container Implementation

From [MessagesPaneContainer.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/messages/MessagesPaneContainer.kt):

```kotlin
@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        // Expanded: Two-column layout
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents.filter { it.isVisible }.forEach { pane ->
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.4f
                    PaneRole.Supporting -> 0.6f
                    else -> 0.25f
                }
                Box(modifier = Modifier.weight(weight)) {
                    if (pane.hasContent) pane.content()
                    else EmptyPlaceholder()
                }
            }
        }
    } else {
        // Compact: Single pane
        content()
    }
}
```

#### When to Use

- Master-detail patterns (list + detail)
- Supporting panels (main content + context)
- Foldable/tablet adaptive layouts
- Any layout requiring side-by-side content on larger screens

---

## Navigation Lifecycle

### LifecycleAwareNode Interface

Both container nodes (`TabNode`, `PaneNode`) and screen nodes (`ScreenNode`) implement `LifecycleAwareNode` for consistent lifecycle management.

```kotlin
interface LifecycleAwareNode {
    val isAttachedToNavigator: Boolean
    val isDisplayed: Boolean
    var composeSavedState: Map<String, List<Any?>>?
    
    fun attachToNavigator()
    fun attachToUI()
    fun detachFromUI()
    fun detachFromNavigator()
    fun addOnDestroyCallback(callback: () -> Unit)
    fun removeOnDestroyCallback(callback: () -> Unit)
}
```

### State Transitions

Nodes transition through the following states:

```
[Created] ──► attachToNavigator() ──► [Attached] ──► attachToUI() ──► [Displayed]
                                           ▲                              │
                                           │                              ▼
                                           └────── detachFromUI() ────────┘
                                           │
                                           ▼
                              detachFromNavigator() ──► [Destroyed]
```

| State | `isAttachedToNavigator` | `isDisplayed` | Description |
|-------|-------------------------|---------------|-------------|
| **Created** | `false` | `false` | Node instantiated but not in tree |
| **Attached** | `true` | `false` | In tree but not visible (e.g., behind another screen) |
| **Displayed** | `true` | `true` | Actively visible to user |
| **Destroyed** | `false` | `false` | Removed from tree and not displayed |

### Lifecycle Callbacks

External components can register for destruction events:

```kotlin
screenNode.addOnDestroyCallback {
    // Clean up resources
    coroutineScope.cancel()
    koinScope.close()
}
```

Callbacks are invoked when a node is fully destroyed (both detached from navigator AND not displayed).

### MVI Container Integration

The `quo-vadis-core-flow-mvi` module provides lifecycle-aware MVI containers.

#### Screen-Scoped Container

For state scoped to a single screen:

```kotlin
class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {

    override val store = store(ProfileState.Loading) {
        init {
            intent(ProfileIntent.LoadProfile)
        }
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> handleLoadProfile()
                // ... other intents
            }
        }
    }
}
```

Usage in composable:

```kotlin
@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileScreen() {
    val store = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
    
    subscribe(store) { state ->
        when (state) {
            is ProfileState.Loading -> LoadingIndicator()
            is ProfileState.Content -> ProfileContent(state)
            is ProfileState.Error -> ErrorMessage(state.message)
        }
    }
}
```

#### Container-Scoped (Shared) Container

For state shared across all screens within a Tab/Pane container:

```kotlin
class DemoTabsContainer(
    scope: SharedContainerScope
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {

    override val store = store(DemoTabsState()) {
        reduce { intent ->
            when (intent) {
                is DemoTabsIntent.IncrementViewed -> updateState {
                    copy(totalItemsViewed = totalItemsViewed + 1)
                }
                // ... other intents
            }
        }
    }
}
```

Usage in tab wrapper:

```kotlin
@TabsContainer(DemoTabs::class)
@Composable
fun DemoTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    val store = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    
    CompositionLocalProvider(LocalDemoTabsStore provides store) {
        content()
    }
}
```

Child screens can then access the shared store:

```kotlin
@Screen(DemoTabs.BooksTab::class)
@Composable
fun BooksScreen() {
    val store = LocalDemoTabsStore.current
    
    LaunchedEffect(Unit) {
        store.intent(DemoTabsIntent.IncrementViewed)
    }
}
```

---

## CompositionLocals

Quo Vadis provides several `CompositionLocal` values for accessing navigation context.

### LocalScreenNode

Provides access to the current `ScreenNode` within screen content.

```kotlin
val LocalScreenNode = compositionLocalOf<ScreenNode?> { null }
```

**Usage:**

```kotlin
@Composable
fun MyScreen() {
    val screenNode = LocalScreenNode.current
    // Access screenNode.key, screenNode.destination, etc.
}
```

**Availability:** Only within screen content rendered through `ScreenRenderer`.

### LocalContainerNode

Provides access to the current container node (`TabNode` or `PaneNode`).

```kotlin
val LocalContainerNode = compositionLocalOf<LifecycleAwareNode?> { null }
```

**Usage:**

```kotlin
@Composable
fun TabContent() {
    val containerNode = LocalContainerNode.current
    // Use for container-scoped operations
}
```

**Availability:** Within tab/pane wrapper composables and their children.

### LocalNavigator

Provides access to the `Navigator` instance for programmatic navigation.

```kotlin
val LocalNavigator = compositionLocalOf<Navigator?> { null }
```

**Usage:**

```kotlin
@Composable
fun MyScreen() {
    val navigator = LocalNavigator.current
        ?: error("Must be inside NavigationHost")

    Button(onClick = { navigator.navigateBack() }) {
        Text("Go Back")
    }
}
```

**Availability:** Within `NavigationHost` and all descendants.

### LocalAnimatedVisibilityScope

Provides access to `AnimatedVisibilityScope` for animated visibility modifiers.

```kotlin
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
```

**Usage:**

```kotlin
@Composable
fun MyScreen() {
    val animatedScope = LocalAnimatedVisibilityScope.current
    
    Box(
        modifier = animatedScope?.let {
            with(it) {
                Modifier.animateEnterExit(
                    enter = fadeIn(),
                    exit = fadeOut()
                )
            }
        } ?: Modifier
    ) {
        // Content
    }
}
```

---

## Code Examples

### Basic Navigation

```kotlin
@Composable
fun App() {
    val navigator: Navigator = koinInject()
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig
    )
}

// Navigate to a destination
navigator.navigate(HomeDestination.Article("123"))

// Navigate back
navigator.navigateBack()

// Navigate and clear back stack
navigator.navigateAndClearTo(HomeDestination.Feed, clearRoute = null, inclusive = false)
```

### Screen with Navigator Injection

From [HomeScreen.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/HomeScreen.kt):

```kotlin
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) }
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
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
    }
}
```

### Tree Traversal Extensions

`NavNode` provides extension functions for tree traversal:

```kotlin
// Find node by key
val node = rootNode.findByKey("screen-123")

// Get active path from root to leaf
val path: List<NavNode> = rootNode.activePathToLeaf()

// Get deepest active screen
val activeScreen: ScreenNode? = rootNode.activeLeaf()

// Get active stack (for push/pop operations)
val stack: StackNode? = rootNode.activeStack()

// Get all screens in subtree
val screens: List<ScreenNode> = rootNode.allScreens()

// Check if node can handle back internally
val canHandleBack: Boolean = rootNode.canHandleBackInternally()
```

---

## Best Practices

### When to Use Each Node Type

| Scenario | Node Type |
|----------|-----------|
| Single screen destination | `ScreenNode` |
| Linear navigation flow | `StackNode` |
| Bottom navigation / tabs | `TabNode` |
| List-detail on tablets | `PaneNode` |
| Auth flow with scope boundary | `StackNode` with `scopeKey` |
| Wizard / multi-step process | `StackNode` |

### Tree Structure Patterns

**Simple App (Stack-only):**
```
StackNode (root)
├── ScreenNode (Home)
├── ScreenNode (Detail)
└── ScreenNode (Settings)
```

**Tabbed App:**
```
StackNode (root)
└── TabNode (MainTabs)
    ├── StackNode (HomeTab)
    │   └── ScreenNode (Home)
    ├── StackNode (SearchTab)
    │   └── ScreenNode (Search)
    └── StackNode (ProfileTab)
        └── ScreenNode (Profile)
```

**Adaptive Master-Detail:**
```
StackNode (root)
└── PaneNode (MessagesPane)
    ├── Primary: StackNode
    │   └── ScreenNode (ConversationList)
    └── Supporting: StackNode
        └── ScreenNode (ConversationDetail)
```

### Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Accessing `LocalNavigator` outside `NavigationHost` | Always check for null or use `koinInject()` |
| Forgetting to register screens in `ScreenRegistry` | Use KSP-generated registries |
| Mixing navigation state with UI state | Keep NavNode tree for navigation only |
| Creating multiple `Navigator` instances | Use single instance via DI |
| Not handling empty pane states | Provide placeholder content in pane containers |
| Ignoring scope boundaries | Use `scopeKey` for scoped navigation flows |

### Serialization Considerations

- Only **persistent state** (`key`, `parentKey`, `destination`, etc.) is serialized
- **Transient state** (`isDisplayed`, lifecycle callbacks) is regenerated on restoration
- Use `@Transient` annotation for non-persistent properties
- Test serialization/deserialization for process death scenarios

### Performance Tips

- NavNode trees use **structural sharing**—unchanged subtrees are reused
- Avoid deep nesting (keep tree depth reasonable)
- Use `activeStack()` instead of manual tree traversal when possible
- Let the framework handle animations—don't fight the transition system

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) - Full architecture documentation
- [Navigator Interface](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/navigator/Navigator.kt) - Navigation operations
- [TreeMutator](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeMutator.kt) - Tree manipulation operations
- [FlowMVI Integration](../quo-vadis-core-flow-mvi/) - MVI container patterns
