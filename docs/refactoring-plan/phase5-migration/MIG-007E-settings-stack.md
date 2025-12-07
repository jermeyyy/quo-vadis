# MIG-007E: Settings Stack Navigation

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007E |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | Low |
| **Estimated Time** | 1-2 hours |
| **Dependencies** | [MIG-007A](./MIG-007A-foundation-destinations.md) (destination definitions) |
| **Output** | Migrated settings screens with `@Screen` annotations |

## Objective

Migrate settings screens to use `@Screen` bindings with the new NavNode architecture. This is the **simplest stack navigation pattern** and serves as a reference implementation for other screen migrations.

Settings navigation follows a straightforward push/pop model:
- **Main** → Settings hub with navigation options
- **Profile** → User profile settings (pushed from Main)
- **Notifications** → Notification preferences (pushed from Main)
- **About** → App information (pushed from Main)

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── ui/screens/
│   ├── SettingsScreen.kt           # Main settings hub
│   └── SettingsDetailScreens.kt    # Profile, Notifications, About screens
```

### Current Implementation

| File | Current State |
|------|---------------|
| [SettingsScreen.kt](../../../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/SettingsScreen.kt) | No annotation, navigates to `SettingsDestination.Profile/Notifications/About` |
| [SettingsDetailScreens.kt](../../../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/SettingsDetailScreens.kt) | Generic `SettingsDetailScreen(title, navigator)` composable |

### Reference Recipe

| Recipe | Pattern |
|--------|---------|
| [MIG-001](./MIG-001-simple-stack-example.md) | Simple Stack Navigation |
| [SettingsStackRecipe.kt](../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/stack/SettingsStackRecipe.kt) | Reference implementation |

---

## Prerequisites

Before starting this migration, ensure **MIG-007A** is complete:

- [ ] `SettingsDestination` converted to `@Stack` annotation
- [ ] `SettingsDestination.Main` destination exists
- [ ] `SettingsDestination.Profile` has `@Destination` annotation
- [ ] `SettingsDestination.Notifications` has `@Destination` annotation
- [ ] `SettingsDestination.About` has `@Destination` annotation

**Expected SettingsDestination after MIG-007A:**

```kotlin
@Stack(name = "settings", startDestination = "Main")
sealed class SettingsDestination : DestinationInterface {
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()
    
    @Destination(route = "settings/profile")
    data object Profile : SettingsDestination()
    
    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()
    
    @Destination(route = "settings/about")
    data object About : SettingsDestination()
}
```

---

## Migration Steps

### Step 1: Add @Screen to SettingsScreen

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/SettingsScreen.kt`

```kotlin
// OLD: No annotation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) { ... }

// NEW: Add @Screen annotation
@Screen(SettingsDestination.Main::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) { ... }
```

**Required Import:**
```kotlin
import com.jermey.quo.vadis.annotations.Screen
```

**Note:** The existing navigation calls (`navigator.navigate(SettingsDestination.Profile)`, etc.) are already correct and don't need changes.

---

### Step 2: Create Individual Detail Screens

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/SettingsDetailScreens.kt`

The current implementation uses a single generic `SettingsDetailScreen` with a `title` parameter. For the new architecture, create individual screen composables with `@Screen` annotations:

```kotlin
// OLD: Generic detail screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    title: String,
    navigator: Navigator
) { ... }

// NEW: Individual screens with @Screen annotations

@Screen(SettingsDestination.Profile::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(
        title = "Profile",
        navigator = navigator
    )
}

@Screen(SettingsDestination.Notifications::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(
        title = "Notifications",
        navigator = navigator
    )
}

@Screen(SettingsDestination.About::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(navigator: Navigator) {
    SettingsDetailContent(
        title = "About",
        navigator = navigator
    )
}

// Keep the shared content as a private helper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailContent(
    title: String,
    navigator: Navigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("This is the $title screen")
        }
    }
}
```

**Required Imports:**
```kotlin
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.navplayground.demo.destinations.SettingsDestination
```

---

### Step 3: Remove Legacy SettingsDetailScreen

After creating individual screens, the generic `SettingsDetailScreen` function can be:
- **Option A:** Removed entirely (if not used elsewhere)
- **Option B:** Made private and renamed to `SettingsDetailContent` (as shown above)

**Recommendation:** Option B - keeps DRY principle while enabling `@Screen` bindings.

---

### Step 4: Verify Navigation Calls

The existing navigation calls in `SettingsScreen.kt` are already correct:

```kotlin
// These remain unchanged - they use destination objects directly
SettingItem(
    title = "Profile",
    icon = Icons.Default.Person,
    onClick = { navigator.navigate(SettingsDestination.Profile) }
)
SettingItem(
    title = "Notifications",
    icon = Icons.Default.Notifications,
    onClick = { navigator.navigate(SettingsDestination.Notifications) }
)
SettingItem(
    title = "About",
    icon = Icons.Default.Info,
    onClick = { navigator.navigate(SettingsDestination.About) }
)
```

---

## Key Transformation Summary

| Component | Old Pattern | New Pattern |
|-----------|-------------|-------------|
| Main screen | `@Composable fun SettingsScreen(...)` | `@Screen(SettingsDestination.Main::class)` |
| Profile screen | Generic `SettingsDetailScreen("Profile", ...)` | `@Screen(SettingsDestination.Profile::class) fun ProfileSettingsScreen(...)` |
| Notifications screen | Generic `SettingsDetailScreen("Notifications", ...)` | `@Screen(SettingsDestination.Notifications::class) fun NotificationsSettingsScreen(...)` |
| About screen | Generic `SettingsDetailScreen("About", ...)` | `@Screen(SettingsDestination.About::class) fun AboutSettingsScreen(...)` |
| Navigation | `navigator.navigate(SettingsDestination.X)` | _(unchanged)_ |
| Back navigation | `navigator.navigateBack()` | _(unchanged)_ |

---

## Checklist

### Pre-Migration
- [ ] MIG-007A complete (destinations migrated)
- [ ] `SettingsDestination.Main` exists

### Screen Annotations
- [ ] Add `@Screen(SettingsDestination.Main::class)` to `SettingsScreen`
- [ ] Create `ProfileSettingsScreen` with `@Screen(SettingsDestination.Profile::class)`
- [ ] Create `NotificationsSettingsScreen` with `@Screen(SettingsDestination.Notifications::class)`
- [ ] Create `AboutSettingsScreen` with `@Screen(SettingsDestination.About::class)`

### Code Cleanup
- [ ] Add required imports (`com.jermey.quo.vadis.annotations.Screen`)
- [ ] Rename/remove generic `SettingsDetailScreen` as appropriate
- [ ] Verify no dead code remains

### Verification
- [ ] Compilation passes
- [ ] Settings screens render correctly
- [ ] Navigation between settings screens works
- [ ] Back navigation returns to correct screens

---

## Verification Commands

```bash
# Verify compilation after screen changes
./gradlew :composeApp:compileKotlinMetadata

# Check @Screen annotations are in place
grep -r "@Screen" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/Settings*.kt

# Expected output:
# SettingsScreen.kt:@Screen(SettingsDestination.Main::class)
# SettingsDetailScreens.kt:@Screen(SettingsDestination.Profile::class)
# SettingsDetailScreens.kt:@Screen(SettingsDestination.Notifications::class)
# SettingsDetailScreens.kt:@Screen(SettingsDestination.About::class)

# Verify no legacy patterns remain
grep -r "@Content" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/Settings*.kt
# Should return empty

# Run demo app to verify visually
./gradlew :composeApp:run
```

---

## Testing Scenarios

### Manual Testing

1. **Navigate to Settings tab** → SettingsScreen displays correctly
2. **Tap "Profile"** → ProfileSettingsScreen displays with back button
3. **Tap back** → Returns to SettingsScreen
4. **Tap "Notifications"** → NotificationsSettingsScreen displays
5. **Tap back** → Returns to SettingsScreen
6. **Tap "About"** → AboutSettingsScreen displays
7. **System back gesture** → Properly navigates back through stack

### Edge Cases

- Rapid navigation (tap Profile → immediately tap back)
- Deep link to settings sub-screen (if supported)
- Configuration change while on detail screen

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Prerequisite)
- [MIG-001: Simple Stack Recipe](./MIG-001-simple-stack-example.md) (Pattern reference)
- [SettingsStackRecipe.kt](../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/stack/SettingsStackRecipe.kt) (Code reference)

---

## Notes

### Why Individual Screen Functions?

The `@Screen` annotation requires a 1:1 mapping between destination classes and composable functions. This means:

1. **KSP can generate the screen registry** - Maps `SettingsDestination.Profile::class` → `ProfileSettingsScreen()`
2. **Type safety** - Each screen explicitly declares which destination it handles
3. **Predictable navigation** - No runtime string matching or parameter passing

### Code Reuse Pattern

Even with individual screen functions, code reuse is maintained through composition:

```kotlin
@Screen(SettingsDestination.Profile::class)
@Composable
fun ProfileSettingsScreen(navigator: Navigator) {
    // Delegate to shared implementation
    SettingsDetailContent(title = "Profile", navigator = navigator)
}
```

This pattern keeps the benefits of both:
- **Type-safe screen binding** via `@Screen` annotation
- **DRY implementation** via shared `SettingsDetailContent` composable
