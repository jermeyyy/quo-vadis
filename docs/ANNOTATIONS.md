# Annotation-Based Navigation Configuration

This guide covers Quo Vadis's annotation-based approach to navigation configuration. The KSP (Kotlin Symbol Processing) code generator processes these annotations to produce type-safe navigation code.

## Overview

### Annotation-Based Approach

Quo Vadis uses annotations to define navigation structure declaratively. This approach provides:

- **Type safety** — Compile-time validation of navigation destinations and arguments
- **Code generation** — KSP generates boilerplate navigation code automatically
- **Deep linking** — Route patterns enable automatic deep link handling
- **IDE support** — Full autocomplete and refactoring support

### What Gets Generated

The KSP processor generates:

| Generated Artifact | Purpose |
|-------------------|---------|
| `NavigationConfig` | Complete navigation tree builder functions |
| `DeepLinkHandler` | Route pattern matching and destination instantiation |
| `ScreenRegistry` | Mapping of destination classes to composable renderers |
| `ContainerRegistry` | Mapping of tab/pane containers to wrapper composables |

### Setup Requirements

Add the KSP processor to your module's `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.quo.vadis.core)
    implementation(libs.quo.vadis.annotations)
    ksp(libs.quo.vadis.ksp)
}
```

---

## @Stack Annotation

### Purpose

Defines a navigation stack with push/pop behavior. Destinations are added to and removed from a stack, with back navigation revealing the previous destination.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `name` | `String` | Unique identifier for the stack (used for key generation) |
| `startDestination` | `KClass<*>` | Initial destination when the stack is created |

### Sealed Class Requirements

- Must be a `sealed class` or `sealed interface`
- All direct subclasses must be annotated with `@Destination`
- Subclasses can be `data object` (no params) or `data class` (with params)

### Example

```kotlin
@Stack(name = "master_detail", startDestination = MasterDetailDestination.List::class)
sealed class MasterDetailDestination : NavDestination {
    
    @Destination(route = "master_detail/list")
    data object List : MasterDetailDestination()

    @Destination(route = "master_detail/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String
    ) : MasterDetailDestination()
}
```

### NavNode Mapping

```
@Stack → StackNode(
    key = "{name}-stack",
    children = [ScreenNode for each @Destination subclass]
)
```

---

## @Destination Annotation

### Purpose

Marks a class or object as a navigation destination within a stack. Each destination represents an individual screen in the navigation graph.

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `route` | `String` | `""` | Route path for deep linking. Empty means not deep-linkable. |

### Route Patterns

| Pattern Type | Example | Description |
|--------------|---------|-------------|
| Simple | `"home"` | Static route segment |
| Path parameter | `"article/{articleId}"` | Extracts `articleId` from URI |
| Multiple params | `"user/{userId}/post/{postId}"` | Multiple path parameters |
| Query params | `"search?query={q}&filter={f}"` | Query string parameters |
| Not deep-linkable | (empty string) | Destination only reachable via code |

### Examples

```kotlin
// Simple route (data object)
@Destination(route = "home")
data object Home : HomeDestination()

// Path parameter (data class)
@Destination(route = "article/{articleId}")
data class Article(
    @Argument val articleId: String
) : HomeDestination()

// Multiple path parameters
@Destination(route = "user/{userId}/post/{postId}")
data class UserPost(
    @Argument val userId: String, 
    @Argument val postId: String
) : HomeDestination()

// Query parameters with optional values
@Destination(route = "search/results")
data class Results(
    @Argument val query: String,
    @Argument(optional = true) val page: Int = 1,
    @Argument(optional = true) val sortAsc: Boolean = true
) : SearchDestination()

// Not deep-linkable (internal screen)
@Destination
data object InternalScreen : AppDestination()
```

---

## @Argument Annotation

### Purpose

Marks a constructor parameter as a navigation argument, enabling type-safe parameter passing and deep link serialization.

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `key` | `String` | `""` | Custom URL parameter key. Defaults to property name. |
| `optional` | `Boolean` | `false` | Whether the argument can be omitted in deep links. |

### Supported Types

| Type | Serialization | Notes |
|------|---------------|-------|
| `String` | Direct | No conversion needed |
| `Int`, `Long`, `Float`, `Double` | `.toString()` / `.toXxx()` | Numeric conversion |
| `Boolean` | `"true"` / `"false"` | Case-insensitive parsing |
| `Enum<T>` | `.name` / `enumValueOf()` | Enum name serialization |
| `@Serializable` | JSON | Requires kotlinx.serialization |
| `List<T>`, `Set<T>` | JSON | Where T is serializable |

### Examples

```kotlin
// Required path parameter
@Destination(route = "profile/{userId}")
data class Profile(
    @Argument val userId: String
) : ProfileDestination()

// Custom key mapping
@Destination(route = "search?q={query}")
data class Search(
    @Argument(key = "query") val searchTerm: String
) : SearchDestination()

// Optional arguments with defaults
@Destination(route = "products/detail/{id}")
data class Detail(
    @Argument val id: String,
    @Argument(optional = true) val referrer: String? = null,
    @Argument(optional = true) val showReviews: Boolean = false
) : ProductsDestination()

// Multiple optional typed parameters
@Destination(route = "search/results")
data class Results(
    @Argument val query: String,
    @Argument(optional = true) val page: Int = 1,
    @Argument(optional = true) val sortAsc: Boolean = true
) : SearchDestination()
```

### Deep Link Parameter Mapping

Arguments map to URL parameters based on position:

```kotlin
@Destination(route = "user/{userId}/post/{postId}")
data class UserPost(
    @Argument val userId: String,  // Maps to {userId}
    @Argument val postId: String   // Maps to {postId}
)
// Deep link: myapp://user/42/post/123
```

---

## @Screen Annotation

### Purpose

Binds a composable function to render a specific navigation destination. When navigation state changes, `NavigationHost` uses the generated `ScreenRegistry` to find and invoke the matching composable.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `destination` | `KClass<*>` | The destination class this composable renders |

### Function Signature Patterns

**Simple destinations (data objects):**

```kotlin
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    // Render feed content
}
```

**Destinations with data (data classes):**

```kotlin
@Screen(MasterDetailDestination.Detail::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator
) {
    // Access destination.itemId, etc.
    Text("Detail for: ${destination.itemId}")
}
```

**With dependency injection (Koin):**

```kotlin
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    // Screen implementation
}
```

### Complete Example

```kotlin
@Screen(SearchDestination.Results::class)
@Composable
fun SearchResultsScreen(
    destination: SearchDestination.Results,
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Results") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Access typed arguments directly
        Text("Query: ${destination.query}")
        Text("Page: ${destination.page}")
        Text("Sort: ${if (destination.sortAsc) "Ascending" else "Descending"}")
    }
}
```

---

## @Tabs and @TabItem Annotations

### Purpose

Define tabbed navigation containers with independent back stacks per tab, state preservation across switches, and configurable initial tab selection.

### @Tabs Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | `String` | — | Unique identifier for the tab container |
| `initialTab` | `KClass<*>` | `Unit::class` | Initially selected tab. `Unit::class` = first tab. |
| `items` | `Array<KClass<*>>` | `[]` | Tab classes in display order |

### @TabItem Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `label` | `String` | — | Display label for the tab |
| `icon` | `String` | `""` | Icon identifier (platform-specific interpretation) |

### Pattern: Nested Sealed Classes

For tabs with a single destination each:

```kotlin
@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, 
             MainTabs.ProfileTab::class, MainTabs.SettingsTab::class]
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
}
```

### Pattern: Tabs with Nested Stacks

For tabs containing multiple destinations:

```kotlin
@Tabs(
    name = "demoTabs",
    initialTab = DemoTabs.MusicTab::class,
    items = [DemoTabs.MusicTab::class, DemoTabs.MoviesTab::class, DemoTabs.BooksTab::class]
)
sealed class DemoTabs : NavDestination {

    companion object : NavDestination  // Wrapper key for @TabsContainer

    @TabItem(label = "Music", icon = "music_note")
    @Stack(name = "musicStack", startDestination = MusicTab.List::class)
    sealed class MusicTab : DemoTabs() {
        @Destination(route = "demo/tabs/music/list")
        data object List : MusicTab()
    }

    @TabItem(label = "Movies", icon = "movie")
    @Stack(name = "moviesStack", startDestination = MoviesTab.List::class)
    sealed class MoviesTab : DemoTabs() {
        @Destination(route = "demo/tabs/movies/list")
        data object List : MoviesTab()
    }

    @TabItem(label = "Books", icon = "book")
    @Stack(name = "booksStack", startDestination = BooksTab.List::class)
    sealed class BooksTab : DemoTabs() {
        @Destination(route = "demo/tabs/books/list")
        data object List : BooksTab()
    }
}

// Detail destinations (separate from tab stacks)
@Destination(route = "demo/tabs/music/detail/{itemId}")
data class MusicDetail(@Argument val itemId: String) : NavDestination

@Destination(route = "demo/tabs/movies/detail/{itemId}")
data class MoviesDetail(@Argument val itemId: String) : NavDestination

@Destination(route = "demo/tabs/books/detail/{itemId}")
data class BooksDetail(@Argument val itemId: String) : NavDestination
```

### Icon Platforms

The `icon` property has platform-specific interpretation:

| Platform | Interpretation |
|----------|----------------|
| Android | Material icon name (e.g., "home", "search") or drawable resource |
| iOS | SF Symbol name |
| Desktop/Web | Icon library identifier |

---

## @Pane and @PaneItem Annotations

### Purpose

Define adaptive pane layouts that respond to screen size. Multiple panes are visible on large screens; single panes with navigation on compact screens.

### @Pane Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `name` | `String` | — | Unique identifier for the pane container |
| `backBehavior` | `PaneBackBehavior` | `PopUntilScaffoldValueChange` | How back navigation operates |

### PaneBackBehavior Options

| Value | Behavior |
|-------|----------|
| `PopUntilScaffoldValueChange` | Back continues until a pane becomes hidden/shown. Best for list-detail. |
| `PopUntilContentChange` | Back stops when any visible pane's content updates. |
| `PopLatest` | Standard back behavior without pane-aware logic. |

### @PaneItem Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `role` | `PaneRole` | — | Layout positioning (PRIMARY, SECONDARY, EXTRA) |
| `adaptStrategy` | `AdaptStrategy` | `HIDE` | Behavior when screen space is limited |

### PaneRole Options

| Role | Description |
|------|-------------|
| `PRIMARY` | Always visible. Main navigation content (lists, menus). |
| `SECONDARY` | Shows when space allows. Detail content related to primary selection. |
| `EXTRA` | Shows only on large screens. Supplementary content (properties, comments). |

### AdaptStrategy Options

| Strategy | Behavior |
|----------|----------|
| `HIDE` | Pane completely hidden on compact screens |
| `COLLAPSE` | Pane shows minimal representation (icons only) |
| `OVERLAY` | Pane appears as modal/sheet over primary content |
| `REFLOW` | Pane repositions (e.g., stacks vertically) |

### Example: List-Detail Pattern

```kotlin
@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    companion object : NavDestination

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(val conversationId: String) : MessagesPane()
}
```

---

## @TabsContainer and @PaneContainer Annotations

### Purpose

Define wrapper composables that provide UI chrome (tab bars, navigation rails, split views) around tabbed or pane navigation containers.

### @TabsContainer

**Properties:**

| Property | Type | Description |
|----------|------|-------------|
| `tabClass` | `KClass<*>` | The tab container class this wrapper wraps |

**Function Signature:**

```kotlin
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    // Wrapper implementation
}
```

**TabsContainerScope Properties:**

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigation operations |
| `activeTabIndex` | `Int` | Currently selected tab index |
| `tabMetadata` | `List<TabMetadata>` | Metadata for all tabs (label, icon, route) |
| `isTransitioning` | `Boolean` | Whether a transition is in progress |
| `switchTab(index)` | Function | Switch to a different tab |

**Example: Tab Strip Wrapper**

```kotlin
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabs Demo") },
                navigationIcon = {
                    IconButton(onClick = { scope.navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Tab strip
            TabRow(selectedTabIndex = scope.activeTabIndex) {
                scope.tabMetadata.forEachIndexed { index, meta ->
                    Tab(
                        selected = scope.activeTabIndex == index,
                        onClick = { scope.switchTab(index) },
                        enabled = !scope.isTransitioning,
                        text = { Text(meta.label) },
                        icon = { Icon(getTabIcon(meta.route), meta.label) }
                    )
                }
            }
            
            // Tab content
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}
```

### @PaneContainer

**Properties:**

| Property | Type | Description |
|----------|------|-------------|
| `paneClass` | `KClass<*>` | The pane container class this wrapper wraps |

**Function Signature:**

```kotlin
@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    // Container implementation
}
```

**PaneContainerScope Properties:**

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigation operations |
| `paneContents` | `List<PaneContentSlot>` | Content slots for each pane |
| `activePaneRole` | `PaneRole` | Currently focused pane |
| `isExpanded` | `Boolean` | Whether in multi-pane (expanded) mode |

**PaneContentSlot Properties:**

| Property | Type | Description |
|----------|------|-------------|
| `role` | `PaneRole` | The pane's role |
| `isVisible` | `Boolean` | Whether the pane is currently visible |
| `hasContent` | `Boolean` | Whether the pane has content to display |
| `content` | `@Composable () -> Unit` | The pane's content composable |

**Example: List-Detail Container**

```kotlin
@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        // Expanded mode: Two-column layout
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents
                .filter { it.isVisible }
                .sortedBy { it.role }
                .forEachIndexed { index, pane ->
                    if (index > 0) {
                        VerticalDivider()
                    }

                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.4f
                        PaneRole.Supporting -> 0.6f
                        PaneRole.Extra -> 0.25f
                    }

                    Box(modifier = Modifier.weight(weight).fillMaxHeight()) {
                        if (pane.role == PaneRole.Supporting && !pane.hasContent) {
                            EmptyPlaceholder()
                        } else {
                            pane.content()
                        }
                    }
                }
        }
    } else {
        // Compact mode: Single pane
        content()
    }
}
```

---

## @Transition Annotation

### Purpose

Defines the transition animation when navigating to a destination.

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `type` | `TransitionType` | `SlideHorizontal` | Preset transition type |
| `customTransition` | `KClass<*>` | `Unit::class` | Custom transition class (when type = Custom) |

### TransitionType Options

| Type | Enter | Exit | Best For |
|------|-------|------|----------|
| `SlideHorizontal` | Slide from right | Slide to left | Stack navigation |
| `SlideVertical` | Slide from bottom | Slide to top | Modal sheets |
| `Fade` | Fade in | Fade out | Tab switches, overlays |
| `None` | Instant | Instant | Custom animations, testing |
| `Custom` | User-defined | User-defined | Complex animations |

### Examples

```kotlin
// Default horizontal slide
@Transition(type = TransitionType.SlideHorizontal)
@Destination(route = "details/{id}")
data class Details(val id: String) : HomeDestination()

// Fade for tab content
@Transition(type = TransitionType.Fade)
@Destination(route = "settings")
data object Settings : HomeDestination()

// Vertical slide for modals
@Transition(type = TransitionType.SlideVertical)
@Destination(route = "modal")
data object Modal : HomeDestination()

// No transition for custom animations
@Transition(type = TransitionType.None)
@Destination(route = "master_detail/detail/{itemId}")
data class Detail(@Argument val itemId: String) : MasterDetailDestination()
```

---

## Generated Code

### What Gets Generated

For each annotated navigation structure, KSP generates:

1. **Builder functions** — `buildXxxNavNode()` functions that construct the navigation tree
2. **Screen registry entries** — Mappings from destination classes to composables
3. **Container registry entries** — Mappings from containers to wrappers
4. **Deep link handlers** — Route pattern matchers and destination factories

### Using Generated Code

**Entry point setup:**

```kotlin
@Composable
fun App() {
    val navTree = remember { buildMainTabsNavNode() }  // KSP-generated
    val navigator = rememberTreeNavigator(initialState = navTree)
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        containerRegistry = GeneratedContainerRegistry
    )
}
```

**Accessing generated registries:**

```kotlin
// Screen registry maps destinations to composables
object GeneratedScreenRegistry : ScreenRegistry {
    override fun getScreen(destination: NavDestination): @Composable (NavDestination, Navigator) -> Unit
}

// Container registry maps tab/pane containers to wrappers
object GeneratedContainerRegistry : ContainerRegistry {
    override fun getTabsContainer(tabClass: KClass<*>): @Composable (TabsContainerScope, @Composable () -> Unit) -> Unit
    override fun getPaneContainer(paneClass: KClass<*>): @Composable (PaneContainerScope, @Composable () -> Unit) -> Unit
}
```

---

## Complete Example

A complete annotation-based navigation setup from the demo app:

### 1. Stack Definitions

```kotlin
// Master-detail stack with typed argument
@Stack(name = "master_detail", startDestination = MasterDetailDestination.List::class)
sealed class MasterDetailDestination : NavDestination {
    @Destination(route = "master_detail/list")
    data object List : MasterDetailDestination()

    @Destination(route = "master_detail/detail/{itemId}")
    @Transition(type = TransitionType.None)
    data class Detail(
        @Argument val itemId: String
    ) : MasterDetailDestination()
}
```

### 2. Tab Navigation

```kotlin
@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, 
             MainTabs.ProfileTab::class, MainTabs.SettingsTab::class]
)
sealed class MainTabs : NavDestination {

    companion object : NavDestination

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    @Transition(type = TransitionType.Fade)
    sealed class SettingsTab : MainTabs() {
        @Destination(route = "settings/main")
        data object Main : SettingsTab()

        @Destination(route = "settings/profile")
        @Transition(type = TransitionType.SlideHorizontal)
        data object Profile : SettingsTab()
    }
}
```

### 3. Pane Navigation

```kotlin
@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    companion object : NavDestination

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(val conversationId: String) : MessagesPane()
}
```

### 4. Screen Bindings

```kotlin
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    // Home screen content
}

@Screen(MasterDetailDestination.Detail::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator = koinInject()
) {
    Text("Detail for: ${destination.itemId}")
}
```

### 5. Container Wrappers

```kotlin
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(topBar = { /* ... */ }) {
        Column {
            TabRow(selectedTabIndex = scope.activeTabIndex) { /* ... */ }
            content()
        }
    }
}

@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        Row { /* Multi-pane layout */ }
    } else {
        content()
    }
}
```

### 6. App Entry Point

```kotlin
@Composable
fun App() {
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberTreeNavigator(initialState = navTree)
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        containerRegistry = GeneratedContainerRegistry
    )
}
```

---

## Validation & Error Messages

Quo Vadis validates annotation usage at compile time. When validation fails, the build fails with clear error messages that include:

- **Location**: File name and line number
- **Problem**: What's wrong
- **Fix**: How to resolve the issue

### Error Message Format

```
{Description} in file '{fileName}' (line {lineNumber}). Fix: {Suggestion}
```

### @Stack Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must be sealed class | Error | `@Stack 'HomeStack' must be a sealed class in file 'HomeStack.kt' (line 5). Fix: Change 'class HomeStack' to 'sealed class HomeStack'` |
| Start destination must exist | Error | `Invalid startDestination 'InvalidScreen' for @Stack 'homeStack' in file 'HomeStack.kt' (line 5). Fix: Use one of the available destinations: [Feed, Profile, Settings]` |
| Must have destinations | Error | `@Stack 'EmptyStack' has no destinations in file 'EmptyStack.kt' (line 3). Fix: Add at least one @Destination annotated subclass inside this sealed class` |

### @Destination Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must be data object/class | Error | `@Destination 'InvalidDest' must be a data object or data class in file 'Destinations.kt' (line 10). Fix: Use 'data object InvalidDest' or 'data class InvalidDest(...)'` |
| Route param requires constructor param | Error | `Route param '{userId}' in @Destination on UserProfile has no matching constructor parameter in file 'UserProfile.kt' (line 8). Fix: Add a constructor parameter named 'userId' or remove '{userId}' from the route` |
| @Argument param must be in route | Error | `@Argument param 'extraData' in UserProfile is not in route pattern 'user/{userId}' in file 'UserProfile.kt' (line 8). Fix: Add '{extraData}' to the route pattern or remove @Argument annotation` |
| Duplicate routes | Error | `Duplicate route 'home/feed' - also used by: FeedScreen in file 'Feed.kt' (line 12). Fix: Use a unique route pattern for this destination` |
| Must have @Screen binding | Error | `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)` |

**Note:** Constructor parameters without `@Argument` annotation are not required to be in the route. They can be passed programmatically (not via deep links).

### @Argument Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Optional requires default | Error | `@Argument(optional = true) on 'page' in SearchResults requires a default value in file 'Search.kt' (line 15). Fix: Add a default value: page: Int = defaultValue` |
| Path param cannot be optional | Error | `Path parameter '{userId}' in UserProfile cannot be optional in file 'UserProfile.kt' (line 8). Fix: Move this parameter to query parameters (after '?') or remove @Argument(optional = true)` |
| Duplicate argument keys | Error | `Duplicate argument key 'id' in ArticleDetail in file 'Article.kt' (line 20). Fix: Use unique keys for each @Argument parameter` |
| Key not in route | Error | `@Argument key 'userId' on 'user' in Profile is not found in route pattern 'profile/{profileId}' in file 'Profile.kt' (line 10). Fix: Add '{userId}' to the route pattern, or change the argument key to match: [profileId]` |

### @Screen Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Invalid destination reference | Error | `@Screen(InvalidClass::class) references a class without @Destination in file 'Screens.kt' (line 25). Fix: Add @Destination annotation to InvalidClass or reference a valid destination` |
| Duplicate screen bindings | Error | `Multiple @Screen bindings for HomeDestination.Feed: FeedScreen, FeedScreenDuplicate in file 'Screens.kt' (line 30). Fix: Keep only one @Screen function for this destination` |

### @Tabs Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must be sealed class | Error | `@Tabs 'MainTabs' must be a sealed class in file 'MainTabs.kt' (line 5). Fix: Change 'class MainTabs' to 'sealed class MainTabs'` |
| Invalid initial tab | Error | `Invalid initialTab 'InvalidTab' for @Tabs 'mainTabs' in file 'MainTabs.kt' (line 5). Fix: Use one of the available tabs: [HomeTab, ProfileTab, SettingsTab]` |
| Empty tabs | Error | `@Tabs container 'EmptyTabs' has no @TabItem entries in file 'EmptyTabs.kt' (line 3). Fix: Add at least one @TabItem annotated class to the items array` |

### @TabItem Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must have @Stack or @Destination | Error | `@TabItem 'HomeTab' has neither @Stack nor @Destination in file 'Tabs.kt' (line 15). Fix: Add @Stack for nested navigation or @Destination for flat screen` |
| Cannot have both @Stack and @Destination | Error | `@TabItem 'InvalidTab' has both @Stack and @Destination in file 'Tabs.kt' (line 20). Fix: Use @Stack for nested navigation OR @Destination for flat screen, not both` |
| NESTED_STACK requires @Stack | Error | `NESTED_STACK tab 'SettingsTab' is missing @Stack annotation in file 'Tabs.kt' (line 25). Fix: Add @Stack annotation to this tab class` |
| NESTED_STACK @Stack must have destinations | Error | `@Stack 'settingsStack' on NESTED_STACK tab 'SettingsTab' has no destinations in file 'Tabs.kt' (line 25). Fix: Add at least one @Destination subclass to this Stack` |
| FLAT_SCREEN must be data object | Error | `FLAT_SCREEN tab 'HomeTab' must be a data object in file 'Tabs.kt' (line 10). Fix: Change to 'data object HomeTab'` |
| FLAT_SCREEN requires @Destination | Error | `FLAT_SCREEN tab 'HomeTab' is missing @Destination in file 'Tabs.kt' (line 10). Fix: Add @Destination annotation with a route` |
| FLAT_SCREEN should have route | Warning | `@Destination on FLAT_SCREEN tab 'HomeTab' has no route in file 'Tabs.kt' (line 10). Fix: Add a route parameter for deep linking support` |

### @Pane Validation

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must be sealed class | Error | `@Pane 'DetailPane' must be a sealed class in file 'DetailPane.kt' (line 5). Fix: Change 'class DetailPane' to 'sealed class DetailPane'` |
| Empty pane | Error | `@Pane container 'EmptyPane' has no @PaneItem entries in file 'EmptyPane.kt' (line 3). Fix: Add at least one @PaneItem annotated class to the items array` |

### Other Validations

| Rule | Severity | Example Message |
|------|----------|-----------------|
| rootGraph must have @Stack | Error | `rootGraph 'InvalidRoot' is not annotated with @Stack in file 'NavConfig.kt' (line 8). Fix: Add @Stack annotation to InvalidRoot` |

---

## See Also

- [DSL-CONFIG.md](DSL-CONFIG.md) — Programmatic navigation configuration
- [NAV-NODES.md](NAV-NODES.md) — Navigation tree structure
- [ARCHITECTURE.md](ARCHITECTURE.md) — Library architecture overview
