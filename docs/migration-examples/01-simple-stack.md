# Migration Guide: Simple Stack Navigation

> **Difficulty**: Easy | **Time Estimate**: 15-30 minutes | **Prerequisites**: Phase 1-4 complete

This guide demonstrates how to migrate basic linear stack navigation from the old Quo Vadis API to the new NavNode architecture.

---

## Table of Contents

1. [Overview](#overview)
2. [Before (Old API)](#before-old-api)
3. [After (New API)](#after-new-api)
4. [Key Migration Steps](#key-migration-steps)
5. [What KSP Generates](#what-ksp-generates)
6. [Common Pitfalls](#common-pitfalls)

---

## Overview

The new NavNode architecture simplifies navigation setup by:

- **Replacing manual graph registration** with KSP-generated builders
- **Unifying all navigation hosts** into a single `QuoVadisHost`
- **Using clearer annotation names** that describe their purpose
- **Eliminating boilerplate** like `initializeQuoVadisRoutes()` and `LaunchedEffect` setup

### Annotation Changes Summary

| Old Annotation | New Annotation | Purpose |
|----------------|----------------|---------|
| `@Graph("name")` | `@Stack(name = "name", startDestination = "...")` | Define a navigation stack container |
| `@Route("path")` | `@Destination(route = "path")` | Mark a class as a navigation target |
| `@Content(Dest::class)` | `@Screen(Dest::class)` | Bind a Composable to render a destination |

### Host Changes Summary

| Old Component | New Component | Change |
|---------------|---------------|--------|
| `GraphNavHost(graph, navigator, ...)` | `QuoVadisHost(navigator, screenRegistry)` | Unified host, KSP-generated registry |
| `rememberNavigator()` | `rememberNavigator(initialNavNode)` | Navigator initialized with NavNode tree |
| Manual `registerGraph()` + `setStartDestination()` | KSP-generated `buildXxxNavNode()` | Automatic initialization |

---

## Before (Old API)

### Complete Working Example

```kotlin
package com.example.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.*

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Navigation Graph
// ═══════════════════════════════════════════════════════════

/**
 * Settings navigation graph with three destinations.
 * 
 * OLD: Uses @Graph to define the container and @Route for each destination.
 */
@Graph("settings", startDestination = "settings/main")
sealed class SettingsDestination : Destination {
    
    @Route("settings/main")
    data object Main : SettingsDestination()
    
    @Route("settings/account")
    data object Account : SettingsDestination()
    
    @Route("settings/notifications")
    data class Notifications(
        val category: String = "all"
    ) : SettingsDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Bind Content to Destinations
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Uses @Content to bind a Composable function to a destination.
 */
@Content(SettingsDestination.Main::class)
@Composable
fun SettingsMainContent(navigator: Navigator) {
    Column {
        Text("Settings")
        
        Button(onClick = { navigator.navigate(SettingsDestination.Account) }) {
            Text("Account Settings")
        }
        
        Button(onClick = { navigator.navigate(SettingsDestination.Notifications()) }) {
            Text("Notification Settings")
        }
    }
}

@Content(SettingsDestination.Account::class)
@Composable
fun AccountContent(navigator: Navigator) {
    Column {
        Text("Account Settings")
        Text("Manage your account details here.")
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Content(SettingsDestination.Notifications::class)
@Composable
fun NotificationsContent(
    destination: SettingsDestination.Notifications,
    navigator: Navigator
) {
    Column {
        Text("Notification Settings")
        Text("Category: ${destination.category}")
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Set Up the Navigation Host
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Requires manual initialization with LaunchedEffect,
 * registerGraph(), and setStartDestination().
 */
@Composable
fun SettingsApp() {
    // Initialize routes globally (typically in Application.onCreate)
    remember { initializeQuoVadisRoutes() }
    
    // Create navigator and graph
    val navigator = rememberNavigator()
    val graph = remember { settingsGraph() }  // KSP-generated graph factory
    
    // Manual setup required
    LaunchedEffect(navigator, graph) {
        navigator.registerGraph(graph)
        navigator.setStartDestination(SettingsDestination.Main)
    }
    
    // OLD: GraphNavHost renders the navigation
    GraphNavHost(
        graph = graph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### Old API Characteristics

1. **`@Graph` annotation** defines the container with route-based `startDestination`
2. **`@Route` annotation** specifies the deep link path for each destination
3. **`@Content` annotation** binds Composables to destinations
4. **Manual initialization** via `initializeQuoVadisRoutes()`, `registerGraph()`, `setStartDestination()`
5. **`GraphNavHost`** is specific to graph-based navigation (separate hosts for tabs, panes)

---

## After (New API)

### Complete Migrated Example

```kotlin
package com.example.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.rememberNavigator
import com.example.settings.generated.GeneratedScreenRegistry
import com.example.settings.generated.buildSettingsNavNode

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Navigation Stack
// ═══════════════════════════════════════════════════════════

/**
 * Settings navigation stack with three destinations.
 * 
 * NEW: Uses @Stack with name-based startDestination (class name, not route).
 */
@Stack(name = "settings", startDestination = "Main")
sealed class SettingsDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()
    
    @Destination(route = "settings/account")
    data object Account : SettingsDestination()
    
    @Destination(route = "settings/notifications")
    data class Notifications(
        val category: String = "all"
    ) : SettingsDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Bind Screens to Destinations
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Uses @Screen to bind a Composable function to a destination.
 * 
 * For data objects (no parameters), only Navigator is required.
 */
@Screen(SettingsDestination.Main::class)
@Composable
fun SettingsMainScreen(navigator: Navigator) {
    Column {
        Text("Settings")
        
        Button(onClick = { navigator.navigate(SettingsDestination.Account) }) {
            Text("Account Settings")
        }
        
        Button(onClick = { navigator.navigate(SettingsDestination.Notifications()) }) {
            Text("Notification Settings")
        }
    }
}

@Screen(SettingsDestination.Account::class)
@Composable
fun AccountScreen(navigator: Navigator) {
    Column {
        Text("Account Settings")
        Text("Manage your account details here.")
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

/**
 * For data classes (with parameters), the destination instance comes first.
 */
@Screen(SettingsDestination.Notifications::class)
@Composable
fun NotificationsScreen(
    destination: SettingsDestination.Notifications,
    navigator: Navigator
) {
    Column {
        Text("Notification Settings")
        Text("Category: ${destination.category}")
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Set Up the Navigation Host
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Minimal setup with KSP-generated code.
 * No LaunchedEffect, no registerGraph, no setStartDestination.
 */
@Composable
fun SettingsApp() {
    // KSP generates buildSettingsNavNode() from @Stack annotation
    val navTree = remember { buildSettingsNavNode() }
    
    // Navigator is initialized directly with the NavNode tree
    val navigator = rememberNavigator(navTree)
    
    // NEW: QuoVadisHost with KSP-generated screen registry
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry
    )
}
```

### New API Characteristics

1. **`@Stack` annotation** replaces `@Graph` with class-name-based `startDestination`
2. **`@Destination` annotation** replaces `@Route` for marking navigation targets
3. **`@Screen` annotation** replaces `@Content` for binding Composables
4. **No manual initialization** — KSP generates everything
5. **`QuoVadisHost`** is the unified host for all navigation patterns

---

## Key Migration Steps

Follow these steps to migrate your simple stack navigation:

### Step 1: Update Graph Annotation

```diff
- @Graph("settings", startDestination = "settings/main")
+ @Stack(name = "settings", startDestination = "Main")
  sealed class SettingsDestination : Destination {
```

> ⚠️ **Important**: `startDestination` now uses the **class name** (`"Main"`) not the route (`"settings/main"`).

### Step 2: Update Route Annotations

```diff
-     @Route("settings/main")
+     @Destination(route = "settings/main")
      data object Main : SettingsDestination()
```

### Step 3: Update Content Annotations

```diff
- @Content(SettingsDestination.Main::class)
+ @Screen(SettingsDestination.Main::class)
  @Composable
- fun SettingsMainContent(navigator: Navigator) {
+ fun SettingsMainScreen(navigator: Navigator) {
```

> 💡 **Tip**: Renaming functions from `*Content` to `*Screen` is optional but recommended for consistency.

### Step 4: Update Function Signatures for Data Classes

For destinations with parameters, ensure the destination comes **first**:

```diff
  @Screen(SettingsDestination.Notifications::class)
  @Composable
  fun NotificationsScreen(
-     navigator: Navigator,
-     destination: SettingsDestination.Notifications
+     destination: SettingsDestination.Notifications,
+     navigator: Navigator
  ) {
```

### Step 5: Remove Manual Initialization

```diff
  @Composable
  fun SettingsApp() {
-     remember { initializeQuoVadisRoutes() }
-     
-     val navigator = rememberNavigator()
-     val graph = remember { settingsGraph() }
-     
-     LaunchedEffect(navigator, graph) {
-         navigator.registerGraph(graph)
-         navigator.setStartDestination(SettingsDestination.Main)
-     }
+     val navTree = remember { buildSettingsNavNode() }
+     val navigator = rememberNavigator(navTree)
```

### Step 6: Replace GraphNavHost

```diff
-     GraphNavHost(
-         graph = graph,
-         navigator = navigator,
-         defaultTransition = NavigationTransitions.SlideHorizontal
-     )
+     QuoVadisHost(
+         navigator = navigator,
+         screenRegistry = GeneratedScreenRegistry
+     )
  }
```

### Step 7: Update Imports

```diff
- import com.jermey.quo.vadis.core.navigation.Graph
- import com.jermey.quo.vadis.core.navigation.Route
- import com.jermey.quo.vadis.core.navigation.Content
- import com.jermey.quo.vadis.core.navigation.GraphNavHost
+ import com.jermey.quo.vadis.annotations.Destination
+ import com.jermey.quo.vadis.annotations.Screen
+ import com.jermey.quo.vadis.annotations.Stack
+ import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
+ import com.example.settings.generated.GeneratedScreenRegistry
+ import com.example.settings.generated.buildSettingsNavNode
```

### Step 8: Build and Verify

Run your build to trigger KSP code generation:

```bash
./gradlew :app:assembleDebug
```

Check for generated files in:
```
build/generated/ksp/debug/kotlin/com/example/settings/generated/
├── SettingsNavNodeBuilder.kt      # buildSettingsNavNode() function
└── GeneratedScreenRegistry.kt     # ScreenRegistry implementation
```

---

## What KSP Generates

KSP processes your annotations and generates the following code:

### SettingsNavNodeBuilder.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.settings.generated

import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.example.settings.SettingsDestination

/**
 * Builds the initial NavNode tree for the "settings" stack.
 * 
 * @return A StackNode containing the start destination (Main)
 */
fun buildSettingsNavNode(): StackNode {
    return StackNode(
        key = "settings",
        parentKey = null,
        children = listOf(
            ScreenNode(
                key = "settings/Main",
                parentKey = "settings",
                destination = SettingsDestination.Main
            )
        )
    )
}
```

### GeneratedScreenRegistry.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.settings.generated

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry
import com.example.settings.SettingsDestination
import com.example.settings.SettingsMainScreen
import com.example.settings.AccountScreen
import com.example.settings.NotificationsScreen

/**
 * KSP-generated screen registry mapping destinations to composable content.
 */
object GeneratedScreenRegistry : ScreenRegistry {
    
    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        when (destination) {
            // SettingsDestination screens
            is SettingsDestination.Main -> SettingsMainScreen(navigator)
            is SettingsDestination.Account -> AccountScreen(navigator)
            is SettingsDestination.Notifications -> NotificationsScreen(destination, navigator)
            
            else -> error("No screen registered for destination: $destination")
        }
    }
    
    override fun hasContent(destination: Destination): Boolean = when (destination) {
        is SettingsDestination.Main,
        is SettingsDestination.Account,
        is SettingsDestination.Notifications -> true
        else -> false
    }
}
```

### How the Generated Code Works

```
┌─────────────────────────────────────────────────────────────────┐
│                         Your Code                                │
├─────────────────────────────────────────────────────────────────┤
│  @Stack("settings", startDestination = "Main")                   │
│  sealed class SettingsDestination { ... }                        │
│                                                                  │
│  @Screen(SettingsDestination.Main::class)                        │
│  fun SettingsMainScreen(navigator: Navigator) { ... }            │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ KSP Processes
┌─────────────────────────────────────────────────────────────────┐
│                    Generated Code                                │
├─────────────────────────────────────────────────────────────────┤
│  buildSettingsNavNode()     →  Creates initial StackNode tree   │
│  GeneratedScreenRegistry    →  Maps destinations to screens      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ Used by
┌─────────────────────────────────────────────────────────────────┐
│                       Runtime                                    │
├─────────────────────────────────────────────────────────────────┤
│  val navTree = buildSettingsNavNode()                            │
│  val navigator = rememberNavigator(navTree)                      │
│  QuoVadisHost(navigator, GeneratedScreenRegistry)                │
└─────────────────────────────────────────────────────────────────┘
```

---

## Common Pitfalls

| Pitfall | Symptom | Solution |
|---------|---------|----------|
| **Wrong `startDestination` format** | `IllegalArgumentException: Cannot find destination "settings/main"` | Use class name (`"Main"`) not route (`"settings/main"`) |
| **Missing `@Screen` annotation** | `No screen registered for destination: SettingsDestination.Main` | Add `@Screen(SettingsDestination.Main::class)` to your Composable |
| **Wrong parameter order** | Destination data not accessible in screen | Put destination parameter **before** navigator for data classes |
| **Using `@Content` instead of `@Screen`** | Compilation error: `Unresolved reference: Content` | Replace with `@Screen` — annotation was renamed |
| **Old `LaunchedEffect` setup code** | Double navigation or initialization issues | Remove entirely — KSP handles initialization |
| **Missing generated imports** | `Unresolved reference: buildSettingsNavNode` | Run `./gradlew build` to trigger KSP, then import from `.generated` package |
| **Forgetting to extend `Destination`** | KSP warning, screens not found | Ensure sealed class extends `com.jermey.quo.vadis.core.navigation.core.Destination` |
| **Empty route for start destination** | Deep linking doesn't work | Add route to `@Destination` if deep linking is needed |

### Debugging Tips

1. **Check KSP output**: Generated files are in `build/generated/ksp/`
2. **Verify annotations**: Each `@Destination` needs a matching `@Screen`
3. **Build clean**: `./gradlew clean build` forces KSP regeneration
4. **Check sealed class**: All destinations must be inside the `@Stack`-annotated sealed class

---

## Next Steps

After migrating simple stack navigation:

- **[02-master-detail.md](./02-master-detail.md)** — Migrate master-detail (list/detail) patterns
- **[03-tabbed-navigation.md](./03-tabbed-navigation.md)** — Migrate tabbed navigation with bottom bars
- **[04-adaptive-panes.md](./04-adaptive-panes.md)** — Migrate adaptive multi-pane layouts

---

## Related Resources

- [API Change Summary](./api-change-summary.md) — Complete annotation and API reference
- [Phase 1: NavNode Architecture](../refactoring-plan/phase1-core/CORE-001-navnode-hierarchy.md) — NavNode type definitions
- [Phase 2: QuoVadisHost](../refactoring-plan/phase2-renderer/RENDER-004-quovadis-host.md) — Unified renderer details
- [Phase 4: Annotations](../refactoring-plan/phase4-annotations/) — Full annotation specifications
