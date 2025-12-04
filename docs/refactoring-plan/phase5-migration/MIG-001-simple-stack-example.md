# MIG-001: Simple Stack Navigation Example

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-001 |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | Phase 1-4 complete |
| **Output** | `docs/migration-examples/01-simple-stack.md` |

## Objective

Create a comprehensive migration example demonstrating how to convert basic linear stack navigation from the old API to the new NavNode architecture.

## Patterns Demonstrated

| Pattern | Old API | New API |
|---------|---------|---------|
| Graph definition | `@Graph("name")` | `@Stack(name = "name")` |
| Route definition | `@Route("path")` | `@Destination(route = "path")` |
| Content binding | `@Content(Dest::class)` | `@Screen(Dest::class)` |
| Navigation host | `GraphNavHost(...)` | `QuoVadisHost(...)` |
| Push navigation | `navigator.navigate(dest)` | `navigator.navigate(dest)` |
| Pop navigation | `navigator.navigateBack()` | `navigator.navigateBack()` |

## Example Content Structure

### 1. Before (Old API)

```kotlin
// === Destination Definition ===
@Graph("settings", startDestination = "main")
sealed class SettingsDestination : Destination {
    @Route("settings/main")
    data object Main : SettingsDestination()
    
    @Route("settings/account")
    data object Account : SettingsDestination()
    
    @Route("settings/notifications")
    data object Notifications : SettingsDestination()
}

// === Content Binding ===
@Content(SettingsDestination.Main::class)
@Composable
fun SettingsMainContent(navigator: Navigator) {
    Column {
        Button(onClick = { navigator.navigate(SettingsDestination.Account) }) {
            Text("Account Settings")
        }
        Button(onClick = { navigator.navigate(SettingsDestination.Notifications) }) {
            Text("Notification Settings")
        }
    }
}

@Content(SettingsDestination.Account::class)
@Composable
fun AccountContent(navigator: Navigator) {
    Column {
        Text("Account Settings")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// === App Setup ===
@Composable
fun SettingsApp() {
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val graph = remember { settingsGraph() }
    
    LaunchedEffect(navigator, graph) {
        navigator.registerGraph(graph)
        navigator.setStartDestination(SettingsDestination.Main)
    }
    
    GraphNavHost(
        graph = graph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### 2. After (New API)

```kotlin
// === Destination Definition ===
@Stack(name = "settings", startDestination = "Main")
sealed class SettingsDestination : Destination {
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()
    
    @Destination(route = "settings/account")
    data object Account : SettingsDestination()
    
    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()
}

// === Screen Binding ===
@Screen(SettingsDestination.Main::class)
@Composable
fun SettingsMainScreen(navigator: Navigator) {
    Column {
        Button(onClick = { navigator.navigate(SettingsDestination.Account) }) {
            Text("Account Settings")
        }
        Button(onClick = { navigator.navigate(SettingsDestination.Notifications) }) {
            Text("Notification Settings")
        }
    }
}

@Screen(SettingsDestination.Account::class)
@Composable
fun AccountScreen(navigator: Navigator) {
    Column {
        Text("Account Settings")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// === App Setup ===
@Composable
fun SettingsApp() {
    val navTree = remember { buildSettingsNavNode() }  // KSP-generated
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry  // KSP-generated
    )
}
```

### 3. Key Migration Steps

1. **Rename annotation**: `@Graph` → `@Stack`
2. **Rename annotation**: `@Route` → `@Destination`
3. **Rename annotation**: `@Content` → `@Screen`
4. **Remove manual setup**: No more `initializeQuoVadisRoutes()`, `registerGraph()`, `setStartDestination()`
5. **Replace host**: `GraphNavHost` → `QuoVadisHost` with `screenRegistry`
6. **Use generated code**: `buildSettingsNavNode()` and `GeneratedScreenRegistry`

### 4. What KSP Generates

```kotlin
// Generated: SettingsNavNodeBuilder.kt
fun buildSettingsNavNode(): StackNode {
    return StackNode(
        id = "settings",
        parentId = null,
        children = listOf(
            ScreenNode(
                id = "settings/Main",
                parentId = "settings",
                destination = SettingsDestination.Main
            )
        )
    )
}

// Generated: GeneratedScreenRegistry.kt (partial)
object GeneratedScreenRegistry : ScreenRegistry {
    override fun render(destination: Destination, navigator: Navigator) {
        when (destination) {
            is SettingsDestination.Main -> SettingsMainScreen(navigator)
            is SettingsDestination.Account -> AccountScreen(navigator)
            is SettingsDestination.Notifications -> NotificationsScreen(navigator)
            // ... other destinations
        }
    }
}
```

## Acceptance Criteria

- [ ] Example compiles and runs
- [ ] Before/After code clearly shows the transformation
- [ ] All annotation changes are documented
- [ ] Generated code examples are accurate
- [ ] Migration steps are numbered and clear
- [ ] Common pitfalls are noted

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Forgetting to update `startDestination` format | Use class name (`"Main"`) not route (`"settings/main"`) |
| Missing `@Screen` on Composables | Every destination needs a corresponding `@Screen` function |
| Old `LaunchedEffect` setup code | Remove entirely - KSP handles initialization |
| Using `@Content` instead of `@Screen` | Annotation was renamed for clarity |

## Related Tasks

- [MIG-002: Master-Detail Pattern](./MIG-002-master-detail-example.md)
- [MIG-007: API Change Summary](./MIG-007-api-change-summary.md)
