# Migration Guide: Hierarchical Rendering

**From**: Flattened rendering mode (deprecated)  
**To**: Hierarchical rendering mode (recommended)

This guide covers migrating from the deprecated flattened rendering approach to the new hierarchical rendering architecture.

---

## Overview

Quo Vadis navigation library has two rendering modes:

| Feature | Flattened (Deprecated) | Hierarchical (Recommended) |
|---------|------------------------|---------------------------|
| Tab/pane wrapper composition | Siblings (z-ordered) | True parent-child |
| Animation coordination | Per-surface | Per-container |
| Predictive back | Per-screen | Entire subtree |
| Wrapper definition | Runtime lambdas | `@TabWrapper`/`@PaneWrapper` annotations |
| Content definition | Runtime lambdas | `@Screen` annotations |

The hierarchical mode provides:
- Better animation coordination (tabs animate with their content)
- Simpler mental model (wrappers contain their content as Compose children)
- Improved predictive back gestures (entire TabNode/PaneNode transforms together)
- Type-safe, annotation-based configuration

---

## Quick Migration Checklist

1. [ ] Migrate tab wrappers to `@TabWrapper` annotations
2. [ ] Migrate pane wrappers to `@PaneWrapper` annotations  
3. [ ] Migrate screen content to `@Screen` annotations
4. [ ] Update `QuoVadisHost` to use `RenderingMode.Hierarchical`
5. [ ] Remove runtime wrapper parameters
6. [ ] Verify navigation and animations work correctly

---

## Step 1: Migrate Tab Wrappers

### Before (Deprecated)

```kotlin
// Runtime wrapper lambda
val myTabWrapper: TabWrapper = { tabContent ->
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = { Icon(meta.icon, meta.label) },
                        label = { Text(meta.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabContent()
        }
    }
}

// Usage in QuoVadisHost
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Flattened,
    tabWrapper = myTabWrapper
) { destination ->
    // content
}
```

### After (Recommended)

```kotlin
import com.jermey.quo.vadis.annotations.TabWrapper

// Annotation-based wrapper function
@TabWrapper(tabClass = MainTabs::class)
@Composable
fun TabWrapperScope.MainTabWrapper(content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val meta = tab.metadata
                    NavigationBarItem(
                        selected = activeIndex == index,
                        onClick = { switchTab(index) },
                        icon = { Icon(meta?.icon, meta?.label ?: "") },
                        label = { Text(meta?.label ?: "") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

// Usage - wrapper is automatically resolved by KSP
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Hierarchical
)
```

### Key Differences

| Aspect | Flattened | Hierarchical |
|--------|-----------|--------------|
| Wrapper type | `TabWrapper` typealias | Function with `@TabWrapper` annotation |
| Receiver | `TabWrapperScope` (same) | `TabWrapperScope` (same) |
| Content parameter | `tabContent: @Composable () -> Unit` | `content: @Composable () -> Unit` |
| Tab state access | `activeTabIndex`, `tabMetadata` | `activeIndex`, `tabs` |
| Tab switching | `switchTab(index)` | `switchTab(index)` (same) |
| Registration | Manual at call site | Automatic via KSP |

---

## Step 2: Migrate Pane Wrappers

### Before (Deprecated)

```kotlin
// Runtime wrapper lambda
val myPaneWrapper: PaneWrapper = { paneContents ->
    Row(modifier = Modifier.fillMaxSize()) {
        paneContents.filter { it.isVisible }.forEach { pane ->
            val weight = when (pane.role) {
                PaneRole.Primary -> 0.65f
                PaneRole.Supporting -> 0.35f
                else -> 1f
            }
            Box(modifier = Modifier.weight(weight)) {
                pane.content()
            }
        }
    }
}

// Usage in QuoVadisHost
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Flattened,
    paneWrapper = myPaneWrapper
) { destination ->
    // content
}
```

### After (Recommended)

```kotlin
import com.jermey.quo.vadis.annotations.PaneWrapper

// Annotation-based wrapper function
@PaneWrapper(paneClass = ListDetailPane::class)
@Composable
fun PaneWrapperScope.ListDetailPaneWrapper(content: @Composable () -> Unit) {
    if (isExpanded) {
        // Multi-pane layout
        Row(modifier = Modifier.fillMaxSize()) {
            paneContents.forEach { pane ->
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.65f
                    PaneRole.Supporting -> 0.35f
                    else -> 1f
                }
                if (pane.isVisible) {
                    Box(modifier = Modifier.weight(weight)) {
                        pane.content()
                    }
                }
            }
        }
    } else {
        // Single pane (compact) - library handles stack-like navigation
        content()
    }
}

// Usage - wrapper is automatically resolved by KSP
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Hierarchical
)
```

### Key Differences

| Aspect | Flattened | Hierarchical |
|--------|-----------|--------------|
| Wrapper type | `PaneWrapper` typealias | Function with `@PaneWrapper` annotation |
| Receiver | `PaneWrapperScope` (same) | `PaneWrapperScope` (same) |
| Content parameter | `paneContents: List<PaneContent>` | `content: @Composable () -> Unit` |
| Pane access | Via parameter | Via `paneContents` property on scope |
| Layout control | Direct iteration | Responsive via `isExpanded` |
| Registration | Manual at call site | Automatic via KSP |

---

## Step 3: Migrate Screen Content

### Before (Deprecated)

```kotlin
// Content resolver lambda
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Flattened,
    tabWrapper = myTabWrapper
) { destination ->
    when (destination) {
        is HomeDestination -> HomeScreen()
        is ProfileDestination -> ProfileScreen(destination.userId)
        is SettingsDestination -> SettingsScreen()
    }
}
```

### After (Recommended)

```kotlin
import com.jermey.quo.vadis.annotations.Screen

// Each screen is a separate annotated function
@Screen(destination = HomeDestination::class)
@Composable
fun HomeScreen() {
    // Home content
}

@Screen(destination = ProfileDestination::class)
@Composable  
fun ProfileScreen(destination: ProfileDestination) {
    Text("Profile: ${destination.userId}")
}

@Screen(destination = SettingsDestination::class)
@Composable
fun SettingsScreen() {
    // Settings content
}

// QuoVadisHost - content is automatically resolved by KSP
QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Hierarchical
)
```

---

## Step 4: Update QuoVadisHost

### Before (Deprecated)

```kotlin
QuoVadisHost(
    navigator = navigator,
    modifier = Modifier.fillMaxSize(),
    renderingMode = RenderingMode.Flattened,  // Deprecated
    enablePredictiveBack = true,
    animationRegistry = AnimationRegistry.Default,  // Only for flattened
    tabWrapper = myTabWrapper,  // Deprecated parameter
    paneWrapper = myPaneWrapper  // Deprecated parameter
) { destination ->
    // Content resolver
}
```

### After (Recommended)

```kotlin
QuoVadisHost(
    navigator = navigator,
    modifier = Modifier.fillMaxSize(),
    renderingMode = RenderingMode.Hierarchical,  // Recommended
    enablePredictiveBack = true
    // tabWrapper, paneWrapper, content parameters removed
    // Wrappers and screens resolved via @TabWrapper/@PaneWrapper/@Screen annotations
)
```

---

## Migration Path for Custom Animations

### Before (Deprecated)

```kotlin
// AnimationRegistry for flattened mode
val customAnimations = AnimationRegistry { from, to, transitionType ->
    when (transitionType) {
        TransitionType.PUSH -> SurfaceAnimationSpec(
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { -it / 3 }
        )
        // ...
    }
}

QuoVadisHost(
    navigator = navigator,
    renderingMode = RenderingMode.Flattened,
    animationRegistry = customAnimations
) { /* ... */ }
```

### After (Recommended)

```kotlin
// Per-destination transitions via @Transition annotation
@Destination(route = "profile/{userId}")
@Transition(type = TransitionType.SlideHorizontal)
data class ProfileDestination(val userId: String)

// Or custom transition
@Destination(route = "modal")
@Transition(type = TransitionType.Custom, customTransition = ModalTransition::class)
data class ModalDestination

// Custom transition class
object ModalTransition : CustomNavTransition {
    override fun createNavTransition(): NavTransition = NavTransition(
        enter = slideInVertically { it } + fadeIn(),
        exit = fadeOut(),
        popEnter = fadeIn(),
        popExit = slideOutVertically { it } + fadeOut()
    )
}
```

---

## Deprecated APIs Summary

The following APIs are deprecated and will be removed in a future version:

### Classes/Interfaces

| Deprecated | Replacement |
|------------|-------------|
| `TreeFlattener` | Internal to hierarchical rendering |
| `RenderableSurface` | Internal to hierarchical rendering |
| `SurfaceNodeType` | Internal to hierarchical rendering |
| `SurfaceRenderingMode` | Internal to hierarchical rendering |
| `SurfaceTransitionState` | Internal to hierarchical rendering |
| `SurfaceAnimationSpec` | `NavTransition` |
| `PaneStructure` | `PaneContentSlot` |

### Type Aliases

| Deprecated | Replacement |
|------------|-------------|
| `TabWrapper` (typealias) | `@TabWrapper` annotation |
| `PaneWrapper` (typealias) | `@PaneWrapper` annotation |

### Enum Values

| Deprecated | Replacement |
|------------|-------------|
| `RenderingMode.Flattened` | `RenderingMode.Hierarchical` |

### Function Parameters

| Deprecated | Replacement |
|------------|-------------|
| `QuoVadisHost(tabWrapper = ...)` | `@TabWrapper` annotation |
| `QuoVadisHost(paneWrapper = ...)` | `@PaneWrapper` annotation |
| `QuoVadisHost(animationRegistry = ...)` | `@Transition` annotation |
| `QuoVadisHost(content = ...)` | `@Screen` annotation |

---

## Common Migration Issues

### Issue 1: TabWrapperScope API differences

**Problem**: `tabMetadata` property doesn't exist in hierarchical mode.

**Solution**: Use `tabs` property which provides `TabInfo` objects:

```kotlin
// Before
tabMetadata.forEachIndexed { index, meta -> /* ... */ }

// After
tabs.forEachIndexed { index, tab ->
    val meta = tab.metadata
    // ...
}
```

### Issue 2: Animations not working

**Problem**: Custom animations from `AnimationRegistry` not applied.

**Solution**: Use `@Transition` annotations on destination classes or create a custom `TransitionRegistry`:

```kotlin
@Destination(route = "details")
@Transition(type = TransitionType.SlideHorizontal)
data class DetailsDestination(val id: String)
```

### Issue 3: Pane wrapper receives content differently

**Problem**: Pane content iteration changed.

**Solution**: In hierarchical mode, use `paneContents` from scope:

```kotlin
// Before: parameter
val myPaneWrapper: PaneWrapper = { paneContents -> /* iterate paneContents */ }

// After: scope property
@PaneWrapper(paneClass = MyPane::class)
@Composable
fun PaneWrapperScope.MyPaneWrapper(content: @Composable () -> Unit) {
    paneContents.forEach { pane -> /* ... */ }
}
```

### Issue 4: KSP not generating wrappers

**Problem**: `@TabWrapper`/`@PaneWrapper` functions not found.

**Solution**: Ensure:
1. KSP plugin is applied: `id("com.google.devtools.ksp")`
2. Annotation processor dependency is added: `ksp(project(":quo-vadis-ksp"))`
3. Build project to generate files: `./gradlew kspKotlin`
4. Generated files are in `build/generated/ksp/`

---

## Version Timeline

| Version | Status |
|---------|--------|
| 2.0.0 | Hierarchical mode introduced, default to Flattened |
| 2.1.0 | Default changed to Hierarchical, Flattened deprecated |
| 3.0.0 (planned) | Flattened mode and related APIs removed |

---

## Need Help?

- Check the [API Reference](API_REFERENCE.md) for new annotation APIs
- See [ARCHITECTURE.md](ARCHITECTURE.md) for hierarchical rendering internals
- File issues on GitHub for migration problems

