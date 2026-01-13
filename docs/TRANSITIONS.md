# Transitions

Quo Vadis provides a comprehensive transition system for animating navigation between screens. This includes built-in transitions, custom transition support, predictive back gestures, and shared element transitions.

---

## Overview

### How Transitions Work in Tree-Based Navigation

Quo Vadis uses a hierarchical navigation tree where each node type (Stack, Tab, Pane) has its own transition behavior:

```
NavigationHost
  └── SharedTransitionLayout (enables shared elements)
        └── NavNodeRenderer (recursive tree renderer)
              ├── StackRenderer → AnimatedNavContent (push/pop transitions)
              ├── TabRenderer → AnimatedNavContent (fade between tabs)
              └── PaneRenderer → AnimatedNavContent (pane reveal/hide)
```

Each renderer uses `AnimatedNavContent` which wraps Compose's `AnimatedContent` with navigation-aware direction detection and predictive back support.

### TransitionRegistry Role

The `TransitionRegistry` provides a lookup mechanism for destination-specific transitions defined via `@Transition` annotations:

```kotlin
interface TransitionRegistry {
    fun getTransition(destinationClass: KClass<*>): NavTransition?
}
```

When navigating, the `AnimationCoordinator` consults this registry:

1. **Check TransitionRegistry** for `@Transition` annotation on the destination
2. **Fall back to defaults** based on navigation context (stack, tab, pane)

### Default vs Custom Transitions

| Context | Default Transition | Behavior |
|---------|-------------------|----------|
| Stack navigation | `SlideHorizontal` | Slides in from right, out to right |
| Tab switching | `Fade` | Crossfade between tabs |
| Pane transitions | `Fade` | Crossfade between panes |

---

## NavigationTransition Interface

The `NavigationTransition` interface defines the core transition abstraction:

```kotlin
interface NavigationTransition {
    val enter: EnterTransition       // Enter when navigating forward
    val exit: ExitTransition         // Exit when navigating forward
    val popEnter: EnterTransition    // Enter when navigating back
    val popExit: ExitTransition      // Exit when navigating back
}
```

### The Four Animation Phases

| Phase | Direction | Description |
|-------|-----------|-------------|
| `enter` | Forward | New screen appearing (push) |
| `exit` | Forward | Current screen disappearing (push) |
| `popEnter` | Back | Previous screen re-appearing (pop) |
| `popExit` | Back | Current screen disappearing (pop) |

### How Transitions Are Resolved

The `AnimationCoordinator` resolves transitions with direction awareness:

```kotlin
fun getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition {
    // For back navigation, look up transition from the EXITING screen (from)
    // For forward navigation, look up transition from the ENTERING screen (to)
    val lookupNode = if (isBack) from else to
    
    // Check TransitionRegistry for @Transition annotation
    screenNode?.destination?.let { dest ->
        transitionRegistry.getTransition(dest::class)?.let { return it }
    }
    
    // Fall back to default
    return defaultTransition
}
```

---

## Built-in Transitions

All preset transitions are available via `NavTransition.Companion` and `NavigationTransitions`:

### Fade Transition

Simple crossfade with no spatial movement:

```kotlin
NavTransition.Fade
// or
NavigationTransitions.Fade
```

| Property | Animation |
|----------|-----------|
| `enter` | `fadeIn(tween(300ms))` |
| `exit` | `fadeOut(tween(300ms))` |
| `popEnter` | `fadeIn(tween(300ms))` |
| `popExit` | `fadeOut(tween(300ms))` |

**Best for:** Tab switching, overlays, modal dialogs

### Slide Horizontal

Platform-like horizontal slide with parallax effect:

```kotlin
NavTransition.SlideHorizontal
// or
NavigationTransitions.SlideHorizontal
```

| Property | Animation |
|----------|-----------|
| `enter` | Slide from right + fade in |
| `exit` | Slide left 30% + fade out (parallax) |
| `popEnter` | Slide from left 30% + fade in (parallax) |
| `popExit` | Slide to right + fade out |

**Best for:** Standard stack navigation (push/pop)

### Slide Vertical

Bottom-to-top slide for modal presentations:

```kotlin
NavTransition.SlideVertical
// or
NavigationTransitions.SlideVertical
```

| Property | Animation |
|----------|-----------|
| `enter` | Slide from bottom + fade in |
| `exit` | Slide up 30% + fade out (parallax) |
| `popEnter` | Slide from top 30% + fade in (parallax) |
| `popExit` | Slide to bottom + fade out |

**Best for:** Modal sheets, bottom sheets, vertical flows

### Scale Transition

Zoom-like effect for emphasis:

```kotlin
NavTransition.ScaleIn
// or
NavigationTransitions.ScaleIn
```

| Property | Animation |
|----------|-----------|
| `enter` | Scale from 80% + fade in |
| `exit` | Scale to 95% + fade out |
| `popEnter` | Scale from 95% + fade in |
| `popExit` | Scale to 80% + fade out |

**Best for:** Detail view transitions, content expansion

### No Transition

Instant switch with no animation:

```kotlin
NavTransition.None
// or
NavigationTransitions.None
```

**Best for:** Performance-sensitive scenarios, testing, when animations would be jarring

### Animation Duration

All built-in transitions use a consistent duration:

```kotlin
const val ANIMATION_DURATION = 300 // milliseconds
```

---

## Custom Transitions

### Creating Custom NavigationTransition

Use the `TransitionBuilder` DSL for custom transitions:

```kotlin
val myTransition = customTransition {
    enter = fadeIn() + expandHorizontally()
    exit = fadeOut() + shrinkHorizontally()
    popEnter = fadeIn() + expandHorizontally(expandFrom = Alignment.End)
    popExit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
}
```

Or create directly with `NavTransition`:

```kotlin
val scaleAndFade = NavTransition(
    enter = scaleIn(initialScale = 0.8f) + fadeIn(),
    exit = scaleOut(targetScale = 1.2f) + fadeOut(),
    popEnter = scaleIn(initialScale = 1.2f) + fadeIn(),
    popExit = scaleOut(targetScale = 0.8f) + fadeOut()
)
```

### Using Compose Animation Specs

Customize timing with animation specs:

```kotlin
val slowSlide = NavTransition(
    enter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    ),
    exit = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    ),
    popEnter = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(durationMillis = 500)
    ),
    popExit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 500)
    )
)
```

### Easing and Duration Customization

Common easing functions:

| Easing | Effect |
|--------|--------|
| `LinearEasing` | Constant speed |
| `FastOutSlowInEasing` | Fast start, slow end (default) |
| `LinearOutSlowInEasing` | Linear start, slow end |
| `FastOutLinearInEasing` | Fast start, linear end |
| `EaseInOut` | Smooth acceleration and deceleration |

Example with spring physics:

```kotlin
val bouncyTransition = NavTransition(
    enter = slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) { it },
    // ... other properties
)
```

---

## Transition Resolution

### Resolution Priority

When determining which transition to use, the system checks in order:

1. **`@Transition` annotation** on the destination class (via `TransitionRegistry`)
2. **Parent container default** (tab/pane specific)
3. **Global default** (`NavTransition.SlideHorizontal`)

### Per-Destination Transitions via Annotations

Apply `@Transition` to destination classes:

```kotlin
@Stack(name = "home", startDestination = HomeDestination.List::class)
sealed class HomeDestination : NavDestination {

    // Default transition (SlideHorizontal)
    @Destination(route = "list")
    data object List : HomeDestination()

    // Explicit slide transition
    @Transition(type = TransitionType.SlideHorizontal)
    @Destination(route = "details/{id}")
    data class Details(val id: String) : HomeDestination()

    // Modal with vertical slide
    @Transition(type = TransitionType.SlideVertical)
    @Destination(route = "filter")
    data object Filter : HomeDestination()

    // Overlay with fade
    @Transition(type = TransitionType.Fade)
    @Destination(route = "help")
    data object Help : HomeDestination()
    
    // No transition (for custom animations)
    @Transition(type = TransitionType.None)
    @Destination(route = "custom")
    data object Custom : HomeDestination()
}
```

### Available TransitionTypes

| Type | Maps To |
|------|---------|
| `TransitionType.SlideHorizontal` | `NavTransition.SlideHorizontal` |
| `TransitionType.SlideVertical` | `NavTransition.SlideVertical` |
| `TransitionType.Fade` | `NavTransition.Fade` |
| `TransitionType.None` | `NavTransition.None` |
| `TransitionType.Custom` | Custom class reference |

### Custom Transition Classes

For complex animations, create a custom transition class:

```kotlin
// Define the custom transition holder
object ScaleAndFadeTransition {
    val transition = NavTransition(
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 1.2f),
        popEnter = fadeIn() + scaleIn(initialScale = 1.2f),
        popExit = fadeOut() + scaleOut(targetScale = 0.8f)
    )
}

// Apply to destination
@Transition(
    type = TransitionType.Custom,
    customTransition = ScaleAndFadeTransition::class
)
@Destination(route = "animated")
data object AnimatedDestination : HomeDestination()
```

**Requirements for custom transition classes:**
- Must be an `object` or have a no-arg constructor
- Must have a `transition` property of type `NavTransition`

### Per-Navigation Transitions via DSL

Register transitions programmatically:

```kotlin
val config = navigationConfig {
    // Register screen content
    screen<DetailScreen> { dest, _, _ ->
        DetailContent(dest)
    }
    
    // Register transitions
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
}
```

---

## Predictive Back Animations

Quo Vadis supports Android's predictive back gesture API, providing smooth gesture-driven animations.

### How Back Gesture Animates

During a predictive back gesture:

1. **Gesture Start**: `PredictiveBackController` activates
2. **Progress Updates**: Visual transforms applied based on gesture progress
3. **Completion/Cancellation**: Animated transition to final state

```kotlin
// Visual behavior during gesture
Box(
    modifier = Modifier.graphicsLayer {
        // Current screen slides right and scales down
        translationX = size.width * progress
        val scale = 1f - (progress * 0.15f)  // Scale factor
        scaleX = scale
        scaleY = scale
    }
)

// Previous screen has parallax effect
Box(
    modifier = Modifier.graphicsLayer {
        // Parallax: moves at 15% of gesture speed
        translationX = -size.width * 0.15f * (1f - progress)
    }
)
```

### Progress-Based Animation

The `PredictiveBackController` provides observable state:

```kotlin
@Stable
class PredictiveBackController {
    val isActive: State<Boolean>    // Whether gesture is active
    val progress: State<Float>      // 0.0 to 1.0 (clamped during gesture)
    val cascadeState: State<CascadeBackState?>  // What will be removed
}
```

**Progress ranges:**
- During gesture: `0.0` to `0.17` (clamped to prevent excessive movement)
- During completion animation: `0.0` to `1.0`
- During cancellation: Animates back to `0.0`

### Cancellation and Completion

```kotlin
// Completion: animate to 1.0, then navigate
suspend fun animateCompleteGesture(onNavigate: () -> Unit) {
    onNavigate()  // Trigger navigation first
    progressAnimatable.animateTo(1f, tween(300ms))
    reset()
}

// Cancellation: animate back to 0.0
suspend fun animateCancelGesture() {
    progressAnimatable.animateTo(0f, tween(200ms))
    reset()
}
```

### Enabling Predictive Back

Predictive back is enabled by default:

```kotlin
NavigationHost(
    navigator = navigator,
    enablePredictiveBack = true  // Default
)
```

Disable for specific scenarios:

```kotlin
NavigationHost(
    navigator = navigator,
    enablePredictiveBack = false  // Disable gesture preview
)
```

---

## Shared Element Transitions

Quo Vadis integrates with Compose's `SharedTransitionLayout` for seamless element morphing between screens.

### SharedTransitionLayout Setup

`NavigationHost` automatically wraps content in `SharedTransitionLayout`:

```kotlin
@Composable
fun NavigationHost(...) {
    SharedTransitionLayout(modifier = modifier) {
        // NavRenderScope gets sharedTransitionScope = this
        NavNodeRenderer(...)
    }
}
```

### TransitionScope from LocalTransitionScope

Access both required scopes via `LocalTransitionScope`:

```kotlin
@Composable
fun MyScreen() {
    val transitionScope = LocalTransitionScope.current
    
    transitionScope?.let { scope ->
        // scope.sharedTransitionScope - for sharedElement/sharedBounds
        // scope.animatedVisibilityScope - for animateEnterExit
    }
}
```

The `TransitionScope` interface combines both scopes:

```kotlin
interface TransitionScope {
    val sharedTransitionScope: SharedTransitionScope
    val animatedVisibilityScope: AnimatedVisibilityScope
}
```

### Modifier.sharedElement() and Modifier.sharedBounds()

**Use `sharedElement`** for exact visual matches (same content):

```kotlin
// List screen
Icon(
    imageVector = Icons.Default.AccountCircle,
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "icon-${item.id}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)

// Detail screen - same key, same icon
Icon(
    imageVector = Icons.Default.AccountCircle,
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "icon-${itemId}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)
```

**Use `sharedBounds`** for containers with different content:

```kotlin
// List item card
Card(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card-${item.id}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)

// Detail card - bounds morph, content changes
Card(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card-${itemId}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)
```

### Matching Shared Elements Across Screens

**Key rules for shared elements:**

1. **Keys must match exactly** between source and destination screens
2. Use unique, stable identifiers (e.g., `"icon-${item.id}"`)
3. Works in **both forward AND backward** navigation

```kotlin
// Pattern: "element-type-unique-id"
key = "card-container-$itemId"
key = "icon-$itemId"
key = "title-$itemId"
```

### AnimatedVisibilityScope Integration

Combine shared elements with enter/exit animations:

```kotlin
@Composable
fun DetailScreen() {
    val transitionScope = LocalTransitionScope.current
    
    // Animated background
    Box(
        modifier = Modifier
            .animateEnterExit(
                transitionScope,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            )
            .background(MaterialTheme.colorScheme.surface)
    )
    
    // Shared element card
    Card(
        modifier = Modifier.sharedBounds(...)
    ) {
        // Content that fades in after card morphs
        Text(
            modifier = Modifier.animateEnterExit(
                transitionScope,
                enter = fadeIn(tween(300, delayMillis = 100)),
                exit = fadeOut(tween(200))
            )
        )
    }
}

// Helper extension
@Composable
private fun Modifier.animateEnterExit(
    transitionScope: TransitionScope?,
    enter: EnterTransition,
    exit: ExitTransition
): Modifier = if (transitionScope != null) {
    with(transitionScope.animatedVisibilityScope) {
        this@animateEnterExit.animateEnterExit(enter = enter, exit = exit)
    }
} else this
```

---

## Stack vs Tab vs Pane Transitions

### Stack Transitions

Stack navigation uses `AnimatedNavContent` with direction detection:

```kotlin
// StackRenderer detects direction by comparing stack sizes
val isBackNavigation = current.children.size < previous.children.size

// Gets transition for the entering/exiting screen
val transition = animationCoordinator.getTransition(
    from = previousActiveChild,
    to = activeChild,
    isBack = isBackNavigation
)

AnimatedNavContent(
    targetState = activeChild,
    transition = transition,
    isBackNavigation = isBackNavigation,
    predictiveBackEnabled = true  // For root stacks
)
```

### Tab Switching Animations

Tabs use **fade transitions** by default and **never** use predictive back:

```kotlin
// TabRenderer
AnimatedNavContent(
    targetState = activeStack,
    transition = animationCoordinator.defaultTabTransition,  // Fade
    isBackNavigation = false,  // Tab switching is never "back"
    predictiveBackEnabled = false  // No gesture preview for tabs
)
```

Tab switching is designed to feel instant and lightweight.

### Pane Reveal Animations (Expanded vs Compact)

Pane behavior adapts to window size:

**Expanded mode (tablets, desktops):**
- Multiple panes visible side-by-side
- No transitions needed for pane visibility

**Compact mode (phones):**
- Single pane visible, behaves like a stack
- Transitions between Primary and Secondary panes

```kotlin
// SinglePaneRenderer (compact mode)
AnimatedNavContent(
    targetState = activePaneContent,
    transition = animationCoordinator.getPaneTransition(
        fromRole = previousNode?.activePaneRole,
        toRole = node.activePaneRole
    ),  // Returns Fade by default
    isBackNavigation = isBackNavigation,
    predictiveBackEnabled = isPredictiveBackActive
)
```

---

## Examples from Demo App

### Per-Destination Transitions

From [MainTabs.kt](../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MainTabs.kt):

```kotlin
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [...])
sealed class MainTabs : NavDestination {

    companion object : NavDestination  // Wrapper key for @TabsContainer

    // Tab roots use Fade for instant switching
    @TabItem
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    // Nested stack with different transitions
    @TabItem
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    @Transition(type = TransitionType.Fade)
    sealed class SettingsTab : MainTabs() {
    
        @Destination(route = "settings/main")
        @Transition(type = TransitionType.Fade)
        data object Main : SettingsTab()

        // Detail screens use horizontal slide
        @Destination(route = "settings/profile")
        @Transition(type = TransitionType.SlideHorizontal)
        data object Profile : SettingsTab()
    }
}
```

> **Note:** `@TabItem` is a marker annotation with no properties. Tab customization (labels, icons) is done in the `@TabsContainer` wrapper using type-safe pattern matching.

### Shared Element Example

From [ItemCard.kt](../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/components/ItemCard.kt) and [DetailScreen.kt](../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/DetailScreen.kt):

**List screen (ItemCard):**

```kotlin
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    val transitionScope = LocalTransitionScope.current

    // Shared bounds for card container
    val finalCardModifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "card-container-${item.id}"
                ),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier

    Card(modifier = finalCardModifier, onClick = onClick) {
        Row {
            // Shared element for icon
            val finalIconModifier = if (transitionScope != null) {
                with(transitionScope.sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = "icon-${item.id}"
                        ),
                        animatedVisibilityScope = transitionScope.animatedVisibilityScope
                    )
                }
            } else Modifier

            Icon(
                Icons.Default.AccountCircle,
                modifier = finalIconModifier.size(56.dp)
            )
            // ... title, subtitle
        }
    }
}
```

**Detail screen:**

```kotlin
@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(destination: MasterDetailDestination.Detail) {
    val transitionScope = LocalTransitionScope.current
    val itemId = destination.itemId

    Column {
        // Animated TopAppBar (not shared, just animated)
        TopAppBar(
            modifier = Modifier.animateEnterExit(
                transitionScope,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            )
        )

        // Header card with shared elements
        Card(
            modifier = if (transitionScope != null) {
                with(transitionScope.sharedTransitionScope) {
                    Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = "card-container-$itemId"
                        ),
                        animatedVisibilityScope = transitionScope.animatedVisibilityScope
                    )
                }
            } else Modifier
        ) {
            Icon(
                Icons.Default.AccountCircle,
                modifier = if (transitionScope != null) {
                    with(transitionScope.sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = "icon-$itemId"
                            ),
                            animatedVisibilityScope = transitionScope.animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            
            // Content fades in after shared element transition
            Column(
                modifier = Modifier.animateEnterExit(
                    transitionScope,
                    enter = fadeIn(tween(300, delayMillis = 50)),
                    exit = fadeOut(tween(300))
                )
            ) {
                Text("Details...")
            }
        }
    }
}
```

### Custom Transition Implementation

From [MasterDetailDestination.kt](../composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MasterDetailDestination.kt):

```kotlin
@Stack(name = "master_detail", startDestination = MasterDetailDestination.List::class)
sealed class MasterDetailDestination : NavDestination {
    
    @Destination(route = "master_detail/list")
    data object List : MasterDetailDestination()

    /**
     * Uses TransitionType.None to disable navigation system animations,
     * allowing the screen to handle its own coordinated animations.
     */
    @Destination(route = "master_detail/detail/{itemId}")
    @Transition(type = TransitionType.None)
    data class Detail(@Argument val itemId: String) : MasterDetailDestination()
}
```

When using `TransitionType.None`, the screen takes full control of animations using `animateEnterExit` modifiers tied to `LocalTransitionScope`.

---

## Summary

| Feature | API | Use Case |
|---------|-----|----------|
| Preset transitions | `NavTransition.SlideHorizontal`, etc. | Standard navigation |
| Annotation-based | `@Transition(type = ...)` | Per-destination customization |
| DSL-based | `transition<Screen>(...)` | Programmatic configuration |
| Custom transitions | `customTransition { }` | Complex animations |
| Predictive back | Automatic with `enablePredictiveBack` | Gesture-driven navigation |
| Shared elements | `LocalTransitionScope.current` | Element morphing between screens |
| Enter/exit animations | `animateEnterExit(...)` | Coordinated screen animations |
