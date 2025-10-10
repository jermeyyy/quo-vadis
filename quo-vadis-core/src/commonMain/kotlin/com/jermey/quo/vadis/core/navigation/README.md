# Navigation Library for Kotlin Multiplatform & Compose Multiplatform

A comprehensive navigation library designed for Kotlin Multiplatform projects with Compose Multiplatform UI.

## Features

✅ **Modularization Support** - Gray box pattern for feature modules  
✅ **Direct Backstack Access** - Full control over navigation stack  
✅ **Deep Link Support** - URI-based and custom deep linking  
✅ **Transition Animations** - Including shared element transitions  
✅ **Independent** - No dependencies on other navigation libraries  
✅ **MVI Integration** - First-class support for MVI architecture  
✅ **DI Framework Ready** - Easy integration with Koin and other DI frameworks  

## Architecture Overview

### Core Components

1. **Destination** - Represents a navigation target
2. **Navigator** - Central navigation controller
3. **BackStack** - Direct backstack manipulation
4. **NavigationGraph** - Modular navigation graphs
5. **NavigationTransition** - Animation support
6. **DeepLink** - Deep link handling

## Quick Start

### 1. Basic Setup

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.setStartDestination(HomeDestination)
    }
    
    NavHost(
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### 2. Define Destinations

```kotlin
object HomeDestination : Destination {
    override val route = "home"
}

data class DetailDestination(val id: String) : Destination {
    override val route = "detail"
    override val arguments = mapOf("id" to id)
}
```

### 3. Create a Navigation Graph

```kotlin
val featureGraph = navigationGraph("feature") {
    startDestination(HomeDestination)
    
    destination(HomeDestination) { dest, nav ->
        HomeScreen(
            onNavigateToDetail = { id ->
                nav.navigate(DetailDestination(id))
            }
        )
    }
    
    destination(SimpleDestination("detail")) { dest, nav ->
        DetailScreen(
            id = dest.arguments["id"] as? String,
            onBack = { nav.navigateBack() }
        )
    }
}
```

### 4. Use with GraphNavHost

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(featureGraph)
        navigator.setStartDestination(featureGraph.startDestination)
    }
    
    GraphNavHost(
        graph = featureGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.Fade
    )
}
```

## Advanced Usage

### Modularization (Gray Box Pattern)

Each feature module can expose its navigation graph without revealing internal details:

```kotlin
// In feature module
class UserFeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph() = navigationGraph("user") {
        startDestination(UserListDestination)
        
        destination(UserListDestination) { _, nav -> UserListScreen(nav) }
        destination(UserDetailDestination) { _, nav -> UserDetailScreen(nav) }
    }
    
    override fun entryPoints() = listOf(UserListDestination)
}

// In app module
val userNavigation = UserFeatureNavigation()
navigator.registerGraph(userNavigation.provideGraph())

// Navigate to feature
navigator.navigate(userNavigation.entryPoints().first())
```

### Direct Backstack Manipulation

```kotlin
// Clear entire stack and navigate
navigator.navigateAndClearAll(HomeDestination)

// Pop to specific destination
navigator.backStack.popTo("home")

// Replace current screen
navigator.navigateAndReplace(NewDestination)

// Pop until condition
navigator.backStack.popUntil { it.route == "root" }
```

### Deep Links

```kotlin
// Register deep link patterns
val handler = DefaultDeepLinkHandler()
handler.register("app://user/{userId}") { params ->
    UserDetailDestination(params["userId"] ?: "")
}

// Handle deep links
navigator.handleDeepLink(DeepLink.parse("app://user/123"))
```

### Custom Transitions

```kotlin
val customTransition = customTransition {
    enter = slideInHorizontally() + fadeIn()
    exit = slideOutHorizontally() + fadeOut()
    popEnter = slideInHorizontally()
    popExit = slideOutHorizontally()
}

navigator.navigate(destination, customTransition)
```

### Shared Element Transitions

```kotlin
// Mark destination as supporting shared elements
data class DetailDestination(
    val imageKey: String
) : SharedElementDestination {
    override val route = "detail"
    override val sharedElements = listOf(
        SharedElementConfig(imageKey)
    )
}

// In composable
Image(
    modifier = Modifier.sharedElement(key = imageKey),
    // ...
)
```

### MVI Integration

```kotlin
class HomeViewModel(navigator: Navigator) : NavigationViewModel(navigator) {
    
    fun onItemClicked(id: String) {
        handleNavigationIntent(
            NavigationIntent.Navigate(DetailDestination(id))
        )
    }
    
    fun onBackPressed() {
        handleNavigationIntent(NavigationIntent.NavigateBack)
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val navState by viewModel.navigationState.collectAsState()
    
    viewModel.navigationEffects.collectAsEffect { effect ->
        when (effect) {
            is NavigationEffect.NavigationFailed -> {
                // Show error
            }
        }
    }
}
```

### Koin Integration

```kotlin
val navigationModule = module {
    single<Navigator> { DefaultNavigator(get()) }
    single<DeepLinkHandler> { DefaultDeepLinkHandler() }
    factory<NavigationFactory> { DefaultNavigationFactory(get()) }
}

@Composable
fun App() {
    val navigator: Navigator = koinInject()
    
    NavHost(navigator = navigator)
}
```

## API Reference

### Navigator

- `navigate(destination, transition)` - Navigate to destination
- `navigateBack()` - Go back
- `navigateAndClearAll(destination)` - Clear stack and navigate
- `navigateAndClearTo(destination, clearRoute, inclusive)` - Clear to route
- `navigateAndReplace(destination)` - Replace current
- `handleDeepLink(deepLink)` - Process deep link
- `registerGraph(graph)` - Register navigation graph

### BackStack

- `push(destination)` - Add to stack
- `pop()` - Remove from stack
- `popTo(route)` - Pop to specific route
- `popUntil(predicate)` - Pop until condition
- `replace(destination)` - Replace current
- `replaceAll(destinations)` - Replace entire stack
- `clear()` - Clear stack
- `popToRoot()` - Pop to root

### Transitions

- `NavigationTransitions.None` - No animation
- `NavigationTransitions.Fade` - Fade in/out
- `NavigationTransitions.SlideHorizontal` - Horizontal slide
- `NavigationTransitions.SlideVertical` - Vertical slide
- `NavigationTransitions.ScaleIn` - Scale animation

## License

This library is part of the NavPlayground project.

