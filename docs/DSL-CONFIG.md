# DSL-Based Configuration

This guide covers the programmatic DSL approach for configuring Quo Vadis navigation, offering maximum control and flexibility for dynamic navigation setups.

## Overview

Quo Vadis supports two configuration approaches:

| Approach | Best For | Characteristics |
|----------|----------|-----------------|
| **Annotations** | Most projects | Declarative, compile-time safety, KSP generates config |
| **DSL** | Dynamic setups | Runtime flexibility, manual control, no code generation |

### When to Choose DSL Configuration

Use the DSL approach when you need:

- **Dynamic navigation graphs** that change based on user state or feature flags
- **Multi-module composition** where modules provide their own navigation config
- **Testing flexibility** with customizable navigation setups
- **Full control** over registration without annotation processing
- **Hybrid setups** combining generated and manual configurations

### Key Components

| Component | Purpose |
|-----------|---------|
| `NavigationConfig` | Unified configuration object containing all registries |
| `ScreenRegistry` | Maps destinations to composable content |
| `ContainerRegistry` | Provides container structures and wrapper composables |
| `TransitionRegistry` | Maps destinations to custom transitions |
| `ScopeRegistry` | Defines navigation scope membership |
| `RouteRegistry` | Maps routes to destination classes |

---

## NavigationConfig

`NavigationConfig` is the central configuration interface that aggregates all navigation registries. It provides a unified contract for the navigation system.

### Interface Structure

```kotlin
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    operator fun plus(other: NavigationConfig): NavigationConfig
}
```

### Creating Configuration with DSL

Use the `navigationConfig` builder function to create a complete configuration:

```kotlin
val config = navigationConfig {
    // Register screens
    screen<HomeScreen> { destination, sharedScope, animScope ->
        { HomeContent(destination) }
    }
    
    // Register containers
    stack<MainStack>("main-scope") {
        screen<HomeScreen>()
        screen<DetailScreen>()
    }
    
    // Register transitions
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    
    // Register container wrappers
    tabsContainer("main-tabs") { content ->
        MyCustomTabBar(content)
    }
}
```

### Combining Configurations

Configurations can be combined using the `+` operator for multi-module setups:

```kotlin
// Feature modules provide their own configs
val featureAConfig = navigationConfig { /* ... */ }
val featureBConfig = navigationConfig { /* ... */ }

// App module combines all configs
val appConfig = featureAConfig + featureBConfig

// Use with navigator
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
NavigationHost(navigator)
```

> **Priority Rule**: When configs are combined, the right-hand config takes priority for duplicate registrations.

---

## ScreenRegistry

The `ScreenRegistry` maps navigation destinations to their composable content.

### Interface

```kotlin
interface ScreenRegistry {
    @Composable
    fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedVisibilityScope: AnimatedVisibilityScope? = null
    )
    
    fun hasContent(destination: NavDestination): Boolean
}
```

### Registering Screens via DSL

Use the `screen<D>` function in the `NavigationConfigBuilder`:

```kotlin
navigationConfig {
    // Basic screen registration
    screen<HomeScreen> { destination, _, _ ->
        { HomeScreenContent() }
    }
    
    // With destination parameters
    screen<ProfileScreen> { destination, _, _ ->
        { ProfileContent(userId = destination.userId) }
    }
    
    // With shared element transitions
    screen<DetailScreen> { destination, sharedScope, animScope ->
        {
            DetailContent(
                item = destination.item,
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = animScope
            )
        }
    }
}
```

### Content Lambda Signature

The screen content lambda receives three parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `destination` | `D` | The destination instance with navigation arguments |
| `sharedTransitionScope` | `SharedTransitionScope?` | Scope for shared element transitions (optional) |
| `animatedVisibilityScope` | `AnimatedVisibilityScope?` | Scope for coordinated animations (optional) |

The lambda returns a `@Composable () -> Unit` that renders the screen content.

---

## ContainerRegistry

The `ContainerRegistry` handles two responsibilities:

1. **Building containers** - Creating `TabNode`/`PaneNode` structures
2. **Rendering wrappers** - Custom UI around navigation content

### Interface

```kotlin
interface ContainerRegistry {
    // Container building
    fun getContainerInfo(destination: NavDestination): ContainerInfo?
    
    // Wrapper rendering
    @Composable
    fun TabsContainer(
        tabNodeKey: String,
        scope: TabsContainerScope,
        content: @Composable () -> Unit
    )
    
    @Composable
    fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    )
    
    fun hasTabsContainer(tabNodeKey: String): Boolean
    fun hasPaneContainer(paneNodeKey: String): Boolean
}
```

### Stack Container Registration

Register linear navigation stacks:

```kotlin
navigationConfig {
    stack<MainStack>("main-scope") {
        screen<HomeScreen>()
        screen<DetailScreen>()
        screen<SettingsScreen>()
    }
}
```

### Tab Container Registration

Register tab-based navigation with the `tabs<D>` builder:

```kotlin
navigationConfig {
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        // Simple flat tabs
        tab(HomeTab, title = "Home", icon = Icons.Default.Home)
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        // Tab with nested navigation stack
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
            screen<ProfileSettingsScreen>()
        }
    }
}
```

### Pane Container Registration

Register adaptive multi-pane layouts:

```kotlin
navigationConfig {
    panes<ListDetailPanes>("list-detail") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.4f, minWidth = 300.dp) {
            root(ListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.6f) {
            root(DetailPlaceholder)
        }
    }
}
```

### Tabs Container Wrapper

Register custom tab UI wrappers:

```kotlin
navigationConfig {
    tabsContainer("main-tabs") { content ->
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        val (label, icon) = when (tab) {
                            is MainTabs.HomeTab -> "Home" to Icons.Default.Home
                            is MainTabs.SearchTab -> "Search" to Icons.Default.Search
                            is MainTabs.ProfileTab -> "Profile" to Icons.Default.Person
                            else -> "Tab" to Icons.Default.Circle
                        }
                        NavigationBarItem(
                            selected = activeTabIndex == index,
                            onClick = { switchTab(index) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                content()  // Library renders active tab content
            }
        }
    }
}
```

**Benefits of Pattern Matching:**
- **Type safety** — Compiler ensures all tab types are handled
- **Full control** — Use any icon library (Material, custom vectors, etc.)
- **Localization** — Labels can use string resources
- **Dynamic styling** — Different styles per tab (badges, colors, etc.)

### TabsContainerScope Properties

The `TabsContainerScope` provides access to tab state:

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigator instance for programmatic navigation |
| `activeTabIndex` | `Int` | Currently selected tab (0-based) |
| `tabCount` | `Int` | Total number of tabs |
| `tabs` | `List<NavDestination>` | Tab destinations for type-safe pattern matching |
| `isTransitioning` | `Boolean` | Whether tab switch animation is in progress |

| Method | Description |
|--------|-------------|
| `switchTab(index)` | Switch to tab at given index |

### Pane Container Wrapper

Register custom pane layout wrappers:

```kotlin
navigationConfig {
    paneContainer("list-detail") { content ->
        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                paneContents.filter { it.isVisible }.forEach { pane ->
                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.4f
                        PaneRole.Supporting -> 0.6f
                        PaneRole.Extra -> 0.25f
                    }
                    Box(Modifier.weight(weight).fillMaxHeight()) {
                        pane.content()
                    }
                }
            }
        } else {
            content()  // Single pane mode
        }
    }
}
```

### PaneContainerScope Properties

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigator instance |
| `activePaneRole` | `PaneRole` | Currently active pane |
| `paneCount` | `Int` | Total configured panes |
| `visiblePaneCount` | `Int` | Currently visible panes |
| `isExpanded` | `Boolean` | Multi-pane mode active |
| `isTransitioning` | `Boolean` | Pane transition in progress |
| `paneContents` | `List<PaneContent>` | Content slots for custom layout |

| Method | Description |
|--------|-------------|
| `navigateToPane(role)` | Navigate to specific pane |

---

## TransitionRegistry

The `TransitionRegistry` maps destinations to custom transition animations.

### Interface

```kotlin
interface TransitionRegistry {
    fun getTransition(destinationClass: KClass<*>): NavTransition?
}
```

### Registering Transitions

```kotlin
navigationConfig {
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
    transition<QuickViewScreen>(NavTransition.ScaleIn)
}
```

### Available Preset Transitions

| Transition | Description |
|------------|-------------|
| `NavTransition.SlideHorizontal` | Slide from right with fade (default for stacks) |
| `NavTransition.SlideVertical` | Slide from bottom with fade (modal-style) |
| `NavTransition.Fade` | Simple fade in/out |
| `NavTransition.ScaleIn` | Scale with fade (zoom effect) |
| `NavTransition.None` | Instant switch, no animation |

### Custom Transitions

Create custom transitions by combining Compose animation primitives:

```kotlin
val myTransition = NavTransition(
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
    popEnter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
    popExit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
)

navigationConfig {
    transition<MyScreen>(myTransition)
}
```

---

## ScopeRegistry

The `ScopeRegistry` determines navigation scope membership, controlling whether destinations stay within containers or navigate outside.

### Interface

```kotlin
interface ScopeRegistry {
    fun isInScope(scopeKey: String, destination: NavDestination): Boolean
    fun getScopeKey(destination: NavDestination): String?
}
```

### Purpose

When navigating from within a tab or pane container, the scope registry determines:

- **In scope**: Destination belongs to this container → navigate within
- **Out of scope**: Destination is external → navigate to parent stack

### Defining Scopes via DSL

```kotlin
navigationConfig {
    // Explicit scope definition
    scope("main-tabs") {
        +HomeScreen::class
        +SearchScreen::class
        +ProfileScreen::class
        +SettingsScreen::class
    }
    
    // Alternative using include
    scope("profile-stack") {
        include(ProfileScreen::class)
        include(EditProfileScreen::class)
        include(ProfileSettingsScreen::class)
    }
}
```

### Automatic Scope Registration

When using `stack`, `tabs`, or `panes` builders, scopes are automatically registered:

```kotlin
navigationConfig {
    // This automatically registers "main" scope with all listed screens
    stack<MainStack>("main") {
        screen<HomeScreen>()
        screen<DetailScreen>()
    }
    
    // This automatically registers "main-tabs" scope with all tab destinations
    tabs<MainTabs>("main-tabs") {
        tab(HomeTab, title = "Home")
        tab(ProfileTab, title = "Profile")
    }
}
```

### Scope Behavior Example

```kotlin
// Given: TabNode with scopeKey = "MainTabs"
// MainTabs contains: Home, Search, Profile

registry.isInScope("MainTabs", HomeDestination)    // true → stays in tab
registry.isInScope("MainTabs", DetailDestination)  // false → navigates outside
```

---

## RouteRegistry

The `RouteRegistry` maps destination classes to their route strings, enabling URI-based navigation.

### Interface

```kotlin
object RouteRegistry {
    fun register(destinationClass: KClass<*>, route: String)
    fun getRoute(destinationClass: KClass<*>): String?
}
```

### Usage

Routes are typically registered automatically via annotations, but can be registered manually:

```kotlin
// Manual route registration
RouteRegistry.register(ProfileScreen::class, "profile/{userId}")
RouteRegistry.register(SettingsScreen::class, "settings")
```

### Route Pattern Syntax

| Pattern | Example | Description |
|---------|---------|-------------|
| Static | `settings` | Exact match |
| Path parameter | `profile/{userId}` | Required parameter |
| Query parameter | `search?q={query}` | Optional parameter |
| Combined | `user/{id}/post/{postId}` | Multiple parameters |

---

## Complete Configuration Example

Here's a full DSL configuration demonstrating all features:

```kotlin
val appNavigationConfig = navigationConfig {
    
    // ═══════════════════════════════════════════════════════
    // SCREEN REGISTRATIONS
    // ═══════════════════════════════════════════════════════
    
    screen<HomeScreen> { destination, _, _ ->
        { HomeContent() }
    }
    
    screen<ProfileScreen> { destination, _, _ ->
        { ProfileContent(userId = destination.userId) }
    }
    
    screen<DetailScreen> { destination, sharedScope, animScope ->
        {
            DetailContent(
                itemId = destination.itemId,
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = animScope
            )
        }
    }
    
    screen<SettingsScreen> { _, _, _ ->
        { SettingsContent() }
    }
    
    // ═══════════════════════════════════════════════════════
    // TAB CONTAINER
    // ═══════════════════════════════════════════════════════
    
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        tab(HomeTab, title = "Home", icon = Icons.Default.Home) {
            screen<HomeScreen>()
            screen<DetailScreen>()
        }
        
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
        }
        
        tab(SettingsTab, title = "Settings", icon = Icons.Default.Settings) {
            screen<SettingsScreen>()
            screen<AboutScreen>()
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // PANE CONTAINER (Master-Detail)
    // ═══════════════════════════════════════════════════════
    
    panes<MessagesPanes>("messages") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.35f, minWidth = 280.dp) {
            root(ConversationListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.65f, minWidth = 400.dp) {
            root(ConversationDetailPlaceholder)
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // TRANSITIONS
    // ═══════════════════════════════════════════════════════
    
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
    
    // ═══════════════════════════════════════════════════════
    // CONTAINER WRAPPERS
    // ═══════════════════════════════════════════════════════
    
    tabsContainer("main-tabs") { content ->
        Column {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val (label, icon) = when (tab) {
                        is HomeTab -> "Home" to Icons.Default.Home
                        is SearchTab -> "Search" to Icons.Default.Search
                        is ProfileTab -> "Profile" to Icons.Default.Person
                        is SettingsTab -> "Settings" to Icons.Default.Settings
                        else -> "Tab" to Icons.Default.Circle
                    }
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        enabled = !isTransitioning
                    )
                }
            }
        }
    }
    
    paneContainer("messages") { content ->
        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                paneContents.filter { it.isVisible }.forEach { pane ->
                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.35f
                        PaneRole.Supporting -> 0.65f
                        PaneRole.Extra -> 0.25f
                    }
                    Box(Modifier.weight(weight).fillMaxHeight()) {
                        pane.content()
                    }
                }
            }
        } else {
            content()
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // ADDITIONAL SCOPES
    // ═══════════════════════════════════════════════════════
    
    scope("modal-flow") {
        +ModalScreen::class
        +ConfirmationScreen::class
        +ResultScreen::class
    }
}
```

### Using the Configuration

```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        initialDestination = MainTabs::class,
        config = appNavigationConfig
    )
    
    NavigationHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        enablePredictiveBack = true,
        windowSizeClass = calculateWindowSizeClass()
    )
}
```

---

## Comparison: DSL vs Annotations

| Feature | DSL | Annotations |
|---------|-----|-------------|
| Runtime flexibility | ✅ Full control | ❌ Compile-time only |
| Code generation | ❌ None needed | ✅ KSP generates config |
| Type safety | ✅ Compile-time | ✅ Compile-time |
| Boilerplate | Medium | Low |
| Dynamic graphs | ✅ Supported | ❌ Not supported |
| Multi-module | ✅ Manual composition | ✅ Auto-merged |
| Learning curve | Medium | Low |

### Hybrid Approach

You can combine generated config with manual DSL additions:

```kotlin
// KSP-generated config
val generatedConfig = GeneratedNavigationConfig

// Manual additions
val dynamicConfig = navigationConfig {
    // Feature-flagged screens
    if (featureFlags.isNewProfileEnabled) {
        screen<NewProfileScreen> { dest, _, _ ->
            { NewProfileContent(dest) }
        }
    }
}

// Combine them
val finalConfig = generatedConfig + dynamicConfig

val navigator = rememberQuoVadisNavigator(MainTabs::class, finalConfig)
```

---

## Best Practices

1. **Use descriptive scope keys** - Name scopes after their containers (e.g., `"main-tabs"`, `"profile-stack"`)

2. **Register all screens** - Every destination that can be navigated to needs a screen registration

3. **Match container and wrapper keys** - Use the same key for container and wrapper registration

4. **Leverage automatic scope registration** - Let `stack`/`tabs`/`panes` builders handle scope membership

5. **Test with `NavigationConfig.Empty`** - Use as baseline in tests to isolate behavior

6. **Compose configs in app module** - Feature modules export configs, app module combines them
