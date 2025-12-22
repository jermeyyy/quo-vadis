# Quo Vadis Navigation Library

Type-safe Kotlin Multiplatform navigation library with Compose Multiplatform UI.

## Project Structure

- **`quo-vadis-core`** - Core navigation library (Maven Central)
- **`quo-vadis-annotations`** - KSP annotations (`@Stack`, `@Destination`, `@Screen`, `@Tab`, `@Pane`)
- **`quo-vadis-ksp`** - KSP code generator
- **`quo-vadis-core-flow-mvi`** - FlowMVI integration
- **`composeApp`** - Demo app (being migrated)

## New Architecture (NavNode Tree)

**Type-safe destinations** with route templates:
```kotlin
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()
}
```

**Screen binding** (replaces `@Content`):
```kotlin
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Text("Article: ${destination.articleId}")  // Type-safe access
}
```

**Entry point** (replaces `GraphNavHost`):
```kotlin
@Composable
fun App() {
    val navTree = remember { buildHomeDestinationNavNode() }  // KSP-generated
    val navigator = TreeNavigator(initialState = navTree)
    
    NavigationHost(navigator = navigator, screenRegistry = GeneratedScreenRegistry)
}
```

## Shared Element Transitions

**Per-destination opt-in** via `destinationWithScopes()`:
```kotlin
destinationWithScopes(Screen) { _, nav, sharedScope, animatedScope ->
    Icon(
        modifier = Modifier.quoVadisSharedElement(
            key = "icon-$id",  // MUST match on both screens
            sharedTransitionScope = sharedScope,
            animatedVisibilityScope = animatedScope
        )
    )
}
```

**Key rules**:
- Use `quoVadisSharedElement()` for icons/images
- Use `quoVadisSharedBounds()` for text/containers
- Keys must match exactly between screens
- Works in BOTH forward AND backward navigation

## Build Commands (Prefer Gradle MCP)

**Use `mcp_gradle-mcp_run_task`** for all Gradle operations:
```python
# Fastest verification (~2 seconds)
mcp_gradle-mcp_run_task(task=":composeApp:assembleDebug")

# Platform builds
mcp_gradle-mcp_run_task(task=":composeApp:linkDebugFrameworkIosSimulatorArm64")  # iOS
mcp_gradle-mcp_run_task(task=":composeApp:jsBrowserDevelopmentRun")              # Web
mcp_gradle-mcp_run_task(task=":composeApp:run")                                  # Desktop

# Testing
mcp_gradle-mcp_run_task(task="test")
mcp_gradle-mcp_run_task(task=":quo-vadis-core:desktopTest")

# Clean
mcp_gradle-mcp_clean()
```

## Critical Rules

1. **Verify with Gradle** - IDE shows false KMP errors; Gradle is source of truth
2. **KDoc required** for all public APIs in `quo-vadis-core`
3**Check refactoring plan** before modifying core/KSP - see `docs/refactoring-plan/`
4**No external nav libraries** in core module

## Key File Locations

| Pattern | Location |
|---------|----------|
| **Refactoring plan** | `docs/refactoring-plan/` |
| NavNode tree types | `quo-vadis-core/.../navigation/core/NavNode.kt` |
| NavigationHost | `quo-vadis-core/.../navigation/compose/NavigationHost.kt` |
| TreeNavigator | `quo-vadis-core/.../navigation/core/TreeNavigator.kt` |
| New annotations | `quo-vadis-annotations/src/.../` |
| Demo destinations | `composeApp/.../demo/destinations/` |

## Documentation

- [Refactoring Plan](docs/refactoring-plan/INDEX.md) - **START HERE for architecture changes**
- [Migration Examples](docs/migration-examples/) - Before/after code patterns
- [ANNOTATION_API.md](quo-vadis-core/docs/ANNOTATION_API.md) - New annotation guide
- [SHARED_ELEMENT_TRANSITIONS.md](quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md) - Shared elements
- Website: https://jermeyyy.github.io/quo-vadis/
