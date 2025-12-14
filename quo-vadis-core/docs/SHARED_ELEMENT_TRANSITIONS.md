# Shared Element Transitions

## Overview

Quo Vadis provides first-class support for **Shared Element Transitions** using Compose Multiplatform's `SharedTransitionLayout` API. This enables smooth, Material Design-compliant transitions where UI elements seamlessly animate between screens during navigation.

**Key Features:**
- ✅ Type-safe shared element definitions
- ✅ Works in **both forward AND backward** navigation
- ✅ Compatible with **predictive back gestures**
- ✅ Per-destination opt-in via `destinationWithScopes()`
- ✅ Automatic AnimatedVisibilityScope management
- ✅ No global configuration required
- ✅ Full Multiplatform support

## Quick Start

### 1. Define Destinations with Scopes

Use `destinationWithScopes()` instead of `destination()` in your navigation graph:

```kotlin
navigationGraph("master-detail") {
    startDestination(MasterListDestination)
    
    // Enable shared elements for this destination
    destinationWithScopes(MasterListDestination) { _, navigator, sharedTransitionScope, animatedVisibilityScope ->
        MasterListScreen(
            navigator = navigator,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
    
    destinationWithScopes(DetailDestination::class) { dest, navigator, sharedTransitionScope, animatedVisibilityScope ->
        val detail = dest as DetailDestination
        DetailScreen(
            itemId = detail.itemId,
            navigator = navigator,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }
}
```

### 2. Apply Shared Element Modifiers

Use the convenience extensions to mark shared elements:

```kotlin
@Composable
fun MasterListScreen(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    LazyColumn {
        items(items) { item ->
            Row {
                // Shared element - animates bounds
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .quoVadisSharedElement(
                            key = "icon-${item.id}",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
                
                // Shared bounds - text crossfades during transition
                Text(
                    text = item.title,
                    modifier = Modifier
                        .quoVadisSharedBounds(
                            key = "title-${item.id}",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }
        }
    }
}

@Composable
fun DetailScreen(
    itemId: String,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Column {
        // Same keys = shared transition!
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)  // Different size = animated size change
                .quoVadisSharedElement(
                    key = "icon-$itemId",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
        )
        
        Text(
            text = item.title,
            modifier = Modifier
                .quoVadisSharedBounds(
                    key = "title-$itemId",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
        )
    }
}
```

### 3. Navigate Normally

No special navigation code required! Shared elements work automatically:

```kotlin
// Forward navigation - shared elements animate
navigator.navigate(DetailDestination(itemId))

// Back navigation - shared elements animate in reverse
navigator.navigateBack()

// Predictive back gesture - shared elements follow gesture
// (works automatically on Android 13+ and iOS)
```

## Architecture

### How It Works

1. **SharedTransitionLayout Wrapping**: `NavigationHost` always wraps content in `SharedTransitionLayout`
2. **AnimatedContent Integration**: Both forward and backward navigation use `AnimatedContent` to provide `AnimatedVisibilityScope`
3. **Per-Destination Opt-In**: Use `destinationWithScopes()` to receive scopes; regular `destination()` won't have shared elements
4. **CompositionLocal Propagation**: Scopes are provided via CompositionLocals for easy access

### Key Components

#### 1. SharedElementScope.kt
Provides CompositionLocal access to scopes:

```kotlin
@Composable
fun currentSharedTransitionScope(): SharedTransitionScope?

@Composable
fun currentNavAnimatedVisibilityScope(): AnimatedVisibilityScope?
```

#### 2. SharedElementModifiers.kt
Convenience extensions for applying shared elements:

```kotlin
// For visual elements (icons, images) - animates content
fun Modifier.quoVadisSharedElement(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? = { _, _ -> null }
): Modifier

// For text/containers - crossfades content during transition
fun Modifier.quoVadisSharedBounds(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    resizeMode: SharedTransitionScope.ResizeMode = ScaleToBounds(),
    placeHolderSize: SharedTransitionScope.PlaceHolderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: (LayoutDirection, Density) -> Path? = { _, _ -> null }
): Modifier

// No-op version when scopes are null (graceful degradation)
fun Modifier.quoVadisSharedElementOrNoop(
    key: String,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    // ... same parameters
): Modifier
```

#### 3. NavigationGraph Extensions
New `destinationWithScopes()` builder:

```kotlin
fun NavigationGraphBuilder.destinationWithScopes(
    destination: Destination,
    content: @Composable (
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) -> Unit
)

fun NavigationGraphBuilder.destinationWithScopes(
    destinationClass: KClass<out Destination>,
    content: @Composable (
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) -> Unit
)
```

#### 4. NavigationHost Integration
Automatically enables SharedTransitionLayout:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    wrapperRegistry: WrapperRegistry = EmptyWrapperRegistry,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    modifier: Modifier = Modifier
)
// Note: SharedTransitionLayout is ALWAYS enabled internally
// Use destinationWithScopes() to opt-in per destination
```

## API Reference

### SharedElementConfig (Legacy - Deprecated)

The old `SharedElementKey` is deprecated. Use the new extension functions instead.

### Modifier Extensions

#### quoVadisSharedElement
Use for **visual elements** (icons, images, shapes) that should animate smoothly:
- Animates position, size, and shape
- Renders in overlay during transition
- Content animates directly (no crossfade)

```kotlin
Icon(
    modifier = Modifier.quoVadisSharedElement(
        key = "icon-$id",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = { initial, target ->
            tween(durationMillis = 300)
        }
    )
)
```

#### quoVadisSharedBounds
Use for **text and containers** where content should crossfade:
- Animates container bounds
- Crossfades content during transition
- Good for text that changes between screens

```kotlin
Text(
    text = title,
    modifier = Modifier.quoVadisSharedBounds(
        key = "title-$id",
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    )
)
```

#### quoVadisSharedElementOrNoop
Applies shared element if scopes are available, otherwise returns Modifier unchanged:

```kotlin
modifier = Modifier.quoVadisSharedElementOrNoop(
    key = "element-$id",
    sharedTransitionScope = sharedTransitionScope,
    animatedVisibilityScope = animatedVisibilityScope
)
// If scopes are null, element still renders but without transition
```

## Best Practices

### DO ✅

1. **Use Unique Keys**: Ensure shared element keys are unique per item
   ```kotlin
   key = "icon-${item.id}"  // ✅ Unique per item
   ```

2. **Match Keys Exactly**: Source and destination must use identical keys
   ```kotlin
   // List screen
   .quoVadisSharedElement(key = "icon-$itemId", ...)
   
   // Detail screen
   .quoVadisSharedElement(key = "icon-$itemId", ...)  // ✅ Same key
   ```

3. **Choose Right Modifier**:
   - `quoVadisSharedElement()` for icons, images, shapes
   - `quoVadisSharedBounds()` for text, containers

4. **Handle Null Scopes**: Use `quoVadisSharedElementOrNoop()` or null checks
   ```kotlin
   if (sharedTransitionScope != null && animatedVisibilityScope != null) {
       Modifier.quoVadisSharedElement(...)
   } else {
       Modifier  // No transition
   }
   ```

5. **Make Elements Prominent**: Larger elements = more visible transitions
   ```kotlin
   // 56dp in list → 80dp in detail = clear size transition
   ```

### DON'T ❌

1. **Don't Use Same Key for Multiple Elements**: Causes conflicts
   ```kotlin
   // ❌ Both use "icon" - which one transitions?
   Icon(modifier = Modifier.quoVadisSharedElement(key = "icon", ...))
   Icon(modifier = Modifier.quoVadisSharedElement(key = "icon", ...))
   ```

2. **Don't Mix Modifier Types**: Use same type on both screens
   ```kotlin
   // ❌ Mismatch
   // List: .quoVadisSharedElement(key = "item", ...)
   // Detail: .quoVadisSharedBounds(key = "item", ...)  // Won't work well
   ```

3. **Don't Forget AnimatedVisibilityScope**: Required for shared elements to work
   ```kotlin
   // ❌ Missing scope parameter in destinationWithScopes
   destinationWithScopes(Dest) { dest, nav, sharedScope, _ ->
       Screen(sharedScope = sharedScope)  // ❌ Missing animatedVisibilityScope
   }
   ```

4. **Don't Use Global Flags**: Per-destination opt-in is the correct pattern
   ```kotlin
   // ❌ No global enableSharedElements flag exists
   NavigationHost(enableSharedElements = true)  // Doesn't exist!
   
   // ✅ Use destinationWithScopes instead
   destinationWithScopes(Dest) { ... }
   ```
Shared element transitions work on **all supported platforms**:

- ✅ **Android** (all versions)
- ✅ **iOS** (all versions)
- ✅ **Desktop** (JVM)
- ✅ **Web** (JavaScript/Wasm)

Performance is GPU-accelerated on all platforms via Compose's `graphicsLayer`.

## Predictive Back Integration

Shared elements work seamlessly with predictive back:

### Android 13+ Predictive Back Gesture
- User swipes from edge → shared elements follow gesture
- Release to complete → shared elements animate to final position
- Swipe back to cancel → shared elements return to original position

### iOS Swipe Back
- Standard iOS edge swipe works with shared elements
- Smooth integration with iOS navigation patterns

### Programmatic Back
- Back button click → shared elements animate in reverse
- `navigator.navigateBack()` → same animation as forward, but reversed

## Example: Master-Detail Flow

Complete example with shared transitions:

```kotlin
// Destinations
sealed class MasterDetailDestination : Destination {
    object MasterList : MasterDetailDestination() {
        override val route = "master_list"
    }
    
    data class Detail(val itemId: String) : MasterDetailDestination() {
        override val route = "detail"
        override val arguments = mapOf("itemId" to itemId)
    }
}

// Navigation Graph
fun masterDetailGraph() = navigationGraph("master-detail") {
    startDestination(MasterDetailDestination.MasterList)
    
    destinationWithScopes(MasterDetailDestination.MasterList) { _, nav, shared, animated ->
        MasterListScreen(nav, shared, animated)
    }
    
    destinationWithScopes(MasterDetailDestination.Detail::class) { dest, nav, shared, animated ->
        DetailScreen((dest as MasterDetailDestination.Detail).itemId, nav, shared, animated)
    }
}

// List Screen
@Composable
fun MasterListScreen(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    LazyColumn {
        items(sampleItems) { item ->
            ItemCard(
                item = item,
                onClick = { navigator.navigate(MasterDetailDestination.Detail(item.id)) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Card(onClick = onClick) {
        Row {
            // Icon with shared element transition
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .quoVadisSharedElement(
                        key = "icon-${item.id}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
            
            Column {
                // Title with shared bounds
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.quoVadisSharedBounds(
                        key = "title-${item.id}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
                Text(text = item.subtitle)
            }
        }
    }
}

// Detail Screen
@Composable
fun DetailScreen(
    itemId: String,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Column {
        // Matching icon - grows from 56dp to 80dp
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .quoVadisSharedElement(
                    key = "icon-$itemId",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
        )
        
        // Matching title - animates position
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.quoVadisSharedBounds(
                key = "title-$itemId",
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        )
        
        // Detail content (not shared)
        Text(text = item.fullDescription)
    }
}
```

## Troubleshooting

### Shared Elements Don't Animate

**Check:**
1. Are you using `destinationWithScopes()` for BOTH screens?
2. Do both screens have the same key for the shared element?
3. Are you passing both `sharedTransitionScope` AND `animatedVisibilityScope`?
4. Is the element visible on both screens when transition happens?

### Elements Jump Instead of Animating

**Fix:** Ensure both elements have similar visual characteristics:
- Similar aspect ratio (or use appropriate bounds transform)
- Similar shape (rounded corners, etc.)
- Proper z-index ordering

### Back Navigation Doesn't Work

**This is fixed!** Both forward and back navigation now use `AnimatedContent`, ensuring `AnimatedVisibilityScope` is always available. If you still see issues:
1. Verify you're on the latest version
2. Check that `predictiveBackMode` is set in NavigationHost (for gesture support)
3. Ensure both screens use `destinationWithScopes()`

## Performance Considerations

1. **GPU Acceleration**: All animations use `graphicsLayer` for optimal performance
2. **Minimal Overhead**: SharedTransitionLayout has negligible overhead when not animating
3. **Selective Opt-In**: Only destinations using `destinationWithScopes()` participate
4. **Cache-Friendly**: Works seamlessly with NavigationHost's composable cache

## Migration from Old API

If you were using the deprecated `SharedElementKey`:

### Before (Deprecated)
```kotlin
destination(Screen) { dest, nav ->
    ScreenUI(
        modifier = Modifier.sharedElement(
            SharedElementKey("icon", SharedElementType.Bounds)
        )
    )
}
```

### After (Current)
```kotlin
destinationWithScopes(Screen) { dest, nav, shared, animated ->
    ScreenUI(
        modifier = Modifier.quoVadisSharedBounds(
            key = "icon",
            sharedTransitionScope = shared,
            animatedVisibilityScope = animated
        )
    )
}
```

## Related Documentation

- [API_REFERENCE.md](API_REFERENCE.md) - Complete API documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) - Library architecture details
- [NAVIGATION_IMPLEMENTATION.md](NAVIGATION_IMPLEMENTATION.md) - Implementation guide
- [MULTIPLATFORM_PREDICTIVE_BACK.md](MULTIPLATFORM_PREDICTIVE_BACK.md) - Predictive back gestures
