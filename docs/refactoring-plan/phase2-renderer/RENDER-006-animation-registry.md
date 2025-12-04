# RENDER-006: Create AnimationRegistry

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-006 |
| **Task Name** | Create AnimationRegistry |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-001 |
| **Blocked By** | RENDER-001 |
| **Blocks** | - |

---

## Overview

The `AnimationRegistry` provides centralized management of navigation transition animations. Instead of hardcoding animations in each navigation host or destination, animations are registered once and looked up based on:

1. **Source destination class** (where we're coming from)
2. **Target destination class** (where we're going)
3. **Direction** (forward push or backward pop)

### Design Goals

| Goal | Approach |
|------|----------|
| **Centralization** | Single registry for all animations |
| **Type safety** | KClass-based registration |
| **Flexibility** | Wildcards for default behaviors |
| **Composability** | Combine animations with operators |
| **Platform parity** | Same API across all platforms |

### Registry Lookup Priority

1. **Exact match**: `(HomeScreen::class, ProfileScreen::class, FORWARD)`
2. **Wildcard target**: `(HomeScreen::class, *, FORWARD)`
3. **Wildcard source**: `(*, ProfileScreen::class, FORWARD)`
4. **Direction default**: `(*, *, FORWARD)`
5. **Global default**: `(*, *, *)`

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/AnimationRegistry.kt
```

---

## Implementation

### Core AnimationRegistry

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TransitionDirection
import kotlin.reflect.KClass

/**
 * Centralized registry for navigation transition animations.
 * 
 * AnimationRegistry maps navigation transitions to animation specifications.
 * Transitions are identified by:
 * - Source destination class (or wildcard)
 * - Target destination class (or wildcard)
 * - Navigation direction (forward, backward, or any)
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * val registry = AnimationRegistry {
 *     // Register specific transition
 *     register(
 *         from = HomeDestination::class,
 *         to = ProfileDestination::class,
 *         direction = TransitionDirection.FORWARD,
 *         spec = SurfaceAnimationSpec(
 *             enter = slideInHorizontally { it },
 *             exit = slideOutHorizontally { -it / 3 }
 *         )
 *     )
 *     
 *     // Register default for all forward navigation
 *     registerDefault(
 *         direction = TransitionDirection.FORWARD,
 *         spec = slideForward()
 *     )
 * }
 * ```
 * 
 * ## With QuoVadisHost
 * 
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     animationRegistry = registry
 * ) { destination ->
 *     // ...
 * }
 * ```
 * 
 * @see SurfaceAnimationSpec
 * @see TreeFlattener.AnimationResolver
 */
class AnimationRegistry private constructor(
    private val registrations: Map<AnimationKey, SurfaceAnimationSpec>,
    private val defaults: Map<TransitionDirection?, SurfaceAnimationSpec>
) {
    
    /**
     * Key for animation lookup.
     */
    private data class AnimationKey(
        val fromClass: KClass<out Destination>?,
        val toClass: KClass<out Destination>?,
        val direction: TransitionDirection?
    )
    
    /**
     * Looks up the animation for a transition.
     * 
     * @param from The source destination class (null if unknown)
     * @param to The target destination class (null if unknown)
     * @param direction The navigation direction
     * @return The animation spec, or default if not found
     */
    fun resolve(
        from: KClass<out Destination>?,
        to: KClass<out Destination>?,
        direction: TransitionDirection
    ): SurfaceAnimationSpec {
        // Priority 1: Exact match
        registrations[AnimationKey(from, to, direction)]?.let { return it }
        
        // Priority 2: Wildcard target
        registrations[AnimationKey(from, null, direction)]?.let { return it }
        
        // Priority 3: Wildcard source
        registrations[AnimationKey(null, to, direction)]?.let { return it }
        
        // Priority 4: Both wildcards with direction
        registrations[AnimationKey(null, null, direction)]?.let { return it }
        
        // Priority 5: Direction-specific default
        defaults[direction]?.let { return it }
        
        // Priority 6: Global default
        return defaults[null] ?: SurfaceAnimationSpec.None
    }
    
    /**
     * Converts this registry to a TreeFlattener.AnimationResolver.
     */
    fun toAnimationResolver(): TreeFlattener.AnimationResolver {
        return TreeFlattener.AnimationResolver { from, to, direction ->
            val fromClass = (from as? ScreenNode)?.destination?.let { it::class }
            val toClass = (to as? ScreenNode)?.destination?.let { it::class }
            resolve(fromClass, toClass, direction)
        }
    }
    
    /**
     * Creates a copy of this registry with additional registrations.
     */
    fun copy(block: Builder.() -> Unit): AnimationRegistry {
        val builder = Builder()
        builder.registrations.putAll(registrations)
        builder.defaults.putAll(defaults)
        builder.apply(block)
        return builder.build()
    }
    
    /**
     * Builder for AnimationRegistry.
     */
    class Builder {
        internal val registrations = mutableMapOf<AnimationKey, SurfaceAnimationSpec>()
        internal val defaults = mutableMapOf<TransitionDirection?, SurfaceAnimationSpec>()
        
        /**
         * Registers an animation for a specific transition.
         * 
         * @param from Source destination class (null for wildcard)
         * @param to Target destination class (null for wildcard)
         * @param direction Navigation direction (null for any direction)
         * @param spec The animation specification
         */
        fun register(
            from: KClass<out Destination>? = null,
            to: KClass<out Destination>? = null,
            direction: TransitionDirection? = null,
            spec: SurfaceAnimationSpec
        ) {
            registrations[AnimationKey(from, to, direction)] = spec
        }
        
        /**
         * Registers a default animation for a direction.
         * 
         * @param direction Navigation direction (null for global default)
         * @param spec The animation specification
         */
        fun registerDefault(
            direction: TransitionDirection? = null,
            spec: SurfaceAnimationSpec
        ) {
            defaults[direction] = spec
        }
        
        /**
         * Registers the built-in slide animation for forward navigation.
         */
        fun useSlideForward() {
            registerDefault(TransitionDirection.FORWARD, StandardAnimations.slideForward())
        }
        
        /**
         * Registers the built-in slide animation for backward navigation.
         */
        fun useSlideBackward() {
            registerDefault(TransitionDirection.BACKWARD, StandardAnimations.slideBackward())
        }
        
        /**
         * Registers the built-in fade animation.
         */
        fun useFade(direction: TransitionDirection? = null) {
            registerDefault(direction, StandardAnimations.fade())
        }
        
        /**
         * Registers a custom animation DSL block.
         */
        inline fun <reified F : Destination, reified T : Destination> transition(
            direction: TransitionDirection = TransitionDirection.FORWARD,
            noinline spec: () -> SurfaceAnimationSpec
        ) {
            register(F::class, T::class, direction, spec())
        }
        
        internal fun build(): AnimationRegistry {
            return AnimationRegistry(registrations.toMap(), defaults.toMap())
        }
    }
    
    companion object {
        /**
         * Creates an AnimationRegistry with the given configuration.
         */
        operator fun invoke(block: Builder.() -> Unit): AnimationRegistry {
            return Builder().apply(block).build()
        }
        
        /**
         * Default animation registry with standard slide animations.
         */
        val Default: AnimationRegistry = AnimationRegistry {
            useSlideForward()
            useSlideBackward()
        }
        
        /**
         * Empty animation registry (no animations).
         */
        val None: AnimationRegistry = AnimationRegistry {
            registerDefault(spec = SurfaceAnimationSpec.None)
        }
    }
}
```

### Standard Animations

```kotlin
/**
 * Collection of standard navigation animations.
 */
object StandardAnimations {
    
    private val defaultDuration = 300
    private val defaultEasing = FastOutSlowInEasing
    
    /**
     * Standard forward slide animation (slide in from right, push left out).
     */
    fun slideForward(
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInHorizontally(
                animationSpec = tween(duration, easing = easing),
                initialOffsetX = { it } // From right
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { -it / 3 } // Slight parallax left
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }
    
    /**
     * Standard backward slide animation (slide in from left, push right out).
     */
    fun slideBackward(
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInHorizontally(
                animationSpec = tween(duration, easing = easing),
                initialOffsetX = { -it / 3 } // From slight left
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { it } // To right
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }
    
    /**
     * Vertical slide animation (slide up to enter, slide down to exit).
     */
    fun slideVertical(
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInVertically(
                animationSpec = tween(duration, easing = easing),
                initialOffsetY = { it } // From bottom
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutVertically(
                animationSpec = tween(duration, easing = easing),
                targetOffsetY = { -it / 3 } // Slight up
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }
    
    /**
     * Simple fade animation.
     */
    fun fade(
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = fadeIn(
                animationSpec = tween(duration, easing = easing)
            ),
            exit = fadeOut(
                animationSpec = tween(duration, easing = easing)
            )
        )
    }
    
    /**
     * Scale animation (zoom in to enter, zoom out to exit).
     */
    fun scale(
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing,
        initialScale: Float = 0.8f,
        targetScale: Float = 1.1f
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = initialScale
            ) + fadeIn(
                animationSpec = tween(duration, easing = easing)
            ),
            exit = scaleOut(
                animationSpec = tween(duration, easing = easing),
                targetScale = targetScale
            ) + fadeOut(
                animationSpec = tween(duration, easing = easing)
            )
        )
    }
    
    /**
     * Shared axis animation (Material Design).
     * 
     * @param axis The axis of movement (X, Y, or Z)
     */
    fun sharedAxis(
        axis: SharedAxis,
        duration: Int = defaultDuration,
        easing: Easing = defaultEasing
    ): SurfaceAnimationSpec {
        return when (axis) {
            SharedAxis.X -> slideForward(duration, easing)
            SharedAxis.Y -> slideVertical(duration, easing)
            SharedAxis.Z -> scale(duration, easing, 0.8f, 1.05f)
        }
    }
    
    /**
     * Material container transform placeholder.
     * 
     * Note: Full container transform requires SharedTransitionScope,
     * which is handled separately in QuoVadisHost.
     */
    fun containerTransform(
        duration: Int = defaultDuration
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = fadeIn(tween(duration)) + scaleIn(tween(duration), initialScale = 0.92f),
            exit = fadeOut(tween(duration)) + scaleOut(tween(duration), targetScale = 1.05f)
        )
    }
    
    /**
     * Shared axis types for Material Design transitions.
     */
    enum class SharedAxis {
        X, // Horizontal
        Y, // Vertical
        Z  // Depth (scale)
    }
}
```

### Animation Combinators

```kotlin
/**
 * Extension functions for combining animations.
 */

/**
 * Combines two animation specs by merging their enter/exit transitions.
 */
operator fun SurfaceAnimationSpec.plus(other: SurfaceAnimationSpec): SurfaceAnimationSpec {
    return SurfaceAnimationSpec(
        enter = this.enter + other.enter,
        exit = this.exit + other.exit
    )
}

/**
 * Creates a reversed version of this animation spec.
 * 
 * The enter animation becomes the exit, and vice versa.
 */
fun SurfaceAnimationSpec.reversed(): SurfaceAnimationSpec {
    return SurfaceAnimationSpec(
        enter = this.exit.toEnter(),
        exit = this.enter.toExit()
    )
}

/**
 * Applies a delay to both enter and exit animations.
 */
fun SurfaceAnimationSpec.withDelay(delayMillis: Int): SurfaceAnimationSpec {
    return SurfaceAnimationSpec(
        enter = this.enter.delayed(delayMillis),
        exit = this.exit.delayed(delayMillis)
    )
}

// Helper extension to convert exit to enter (approximate)
private fun ExitTransition.toEnter(): EnterTransition {
    // This is a simplification - in practice, you'd need to inspect the transition
    return fadeIn()
}

// Helper extension to convert enter to exit (approximate)
private fun EnterTransition.toExit(): ExitTransition {
    return fadeOut()
}

// Helper to add delay
private fun EnterTransition.delayed(delayMillis: Int): EnterTransition {
    // Note: Compose doesn't have a direct way to delay transitions
    // This would need custom implementation or AnimatedVisibility wrapper
    return this
}

private fun ExitTransition.delayed(delayMillis: Int): ExitTransition {
    return this
}
```

### DSL Extensions

```kotlin
/**
 * DSL for building animation registries.
 */

/**
 * Creates an animation registry using the DSL.
 */
fun animationRegistry(block: AnimationRegistry.Builder.() -> Unit): AnimationRegistry {
    return AnimationRegistry(block)
}

/**
 * Convenience function for specifying transition animations.
 */
inline fun <reified From : Destination, reified To : Destination> AnimationRegistry.Builder.forwardTransition(
    spec: SurfaceAnimationSpec
) {
    register(From::class, To::class, TransitionDirection.FORWARD, spec)
}

inline fun <reified From : Destination, reified To : Destination> AnimationRegistry.Builder.backwardTransition(
    spec: SurfaceAnimationSpec
) {
    register(From::class, To::class, TransitionDirection.BACKWARD, spec)
}

/**
 * Registers both forward and backward animations at once.
 */
inline fun <reified From : Destination, reified To : Destination> AnimationRegistry.Builder.biDirectionalTransition(
    forward: SurfaceAnimationSpec,
    backward: SurfaceAnimationSpec = forward.reversed()
) {
    register(From::class, To::class, TransitionDirection.FORWARD, forward)
    register(To::class, From::class, TransitionDirection.BACKWARD, backward)
}
```

---

## Usage Examples

### Basic Setup

```kotlin
val registry = AnimationRegistry {
    // Use built-in defaults
    useSlideForward()
    useSlideBackward()
}

QuoVadisHost(
    navigator = navigator,
    animationRegistry = registry
) { /* ... */ }
```

### Custom Transitions

```kotlin
val registry = AnimationRegistry {
    // Default slide animations
    useSlideForward()
    useSlideBackward()
    
    // Custom animation for specific transition
    register(
        from = PhotoListDestination::class,
        to = PhotoDetailDestination::class,
        direction = TransitionDirection.FORWARD,
        spec = StandardAnimations.containerTransform()
    )
    
    // Modal presentation (slide up)
    register(
        from = null, // Any source
        to = SettingsDestination::class,
        direction = TransitionDirection.FORWARD,
        spec = StandardAnimations.slideVertical()
    )
}
```

### Type-Safe DSL

```kotlin
val registry = animationRegistry {
    useSlideForward()
    useSlideBackward()
    
    // Type-safe registration
    forwardTransition<HomeDestination, ProfileDestination>(
        StandardAnimations.scale()
    )
    
    // Bidirectional
    biDirectionalTransition<ListDestination, DetailDestination>(
        forward = StandardAnimations.sharedAxis(SharedAxis.X),
        backward = StandardAnimations.sharedAxis(SharedAxis.X).reversed()
    )
}
```

### Material Design Defaults

```kotlin
val materialRegistry = AnimationRegistry {
    // Material Motion guidelines
    registerDefault(
        direction = TransitionDirection.FORWARD,
        spec = StandardAnimations.sharedAxis(SharedAxis.X)
    )
    registerDefault(
        direction = TransitionDirection.BACKWARD,
        spec = StandardAnimations.sharedAxis(SharedAxis.X)
    )
    
    // Z-axis for dialogs/modals
    register(
        from = null,
        to = DialogDestination::class,
        spec = StandardAnimations.sharedAxis(SharedAxis.Z)
    )
}
```

---

## Implementation Steps

### Step 1: Core Registry

1. Create `AnimationRegistry.kt` file
2. Implement `AnimationKey` data class
3. Implement `AnimationRegistry` class with lookup logic
4. Create `Builder` class

### Step 2: Standard Animations

1. Create `StandardAnimations` object
2. Implement `slideForward()` and `slideBackward()`
3. Implement `slideVertical()` and `fade()`
4. Implement `scale()` and `sharedAxis()`

### Step 3: Combinators

1. Implement `plus` operator for combining specs
2. Implement `reversed()` function
3. Add delay support (if feasible)

### Step 4: DSL Extensions

1. Add `animationRegistry` builder function
2. Add type-safe inline functions
3. Add `biDirectionalTransition` helper

### Step 5: Integration

1. Update `QuoVadisHost` to accept registry
2. Implement `toAnimationResolver()` conversion
3. Add to `TreeFlattener`

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/AnimationRegistry.kt` | Create | Main registry implementation |
| `quo-vadis-core/.../compose/StandardAnimations.kt` | Create | Built-in animations |
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | Modify | Accept AnimationRegistry parameter |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Modify | Use AnimationResolver |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-001 (RenderableSurface) | Hard | Uses SurfaceAnimationSpec |

---

## Acceptance Criteria

- [ ] `AnimationRegistry` class with lookup by (from, to, direction)
- [ ] `Builder` class with registration methods
- [ ] Lookup priority: exact → wildcard target → wildcard source → direction default → global
- [ ] `StandardAnimations.slideForward()` and `slideBackward()`
- [ ] `StandardAnimations.slideVertical()` and `fade()`
- [ ] `StandardAnimations.scale()` and `sharedAxis()`
- [ ] `plus` operator for combining animations
- [ ] `reversed()` for creating backward animations
- [ ] DSL extensions (`forwardTransition`, `biDirectionalTransition`)
- [ ] `toAnimationResolver()` integration with TreeFlattener
- [ ] `AnimationRegistry.Default` with standard slide animations
- [ ] `AnimationRegistry.None` with no animations
- [ ] QuoVadisHost integration works
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for lookup priority
- [ ] Unit tests for standard animations

---

## Testing Notes

```kotlin
@Test
fun `exact match takes priority`() {
    val exactSpec = StandardAnimations.fade()
    val wildcardSpec = StandardAnimations.slideForward()
    
    val registry = AnimationRegistry {
        register(HomeDestination::class, ProfileDestination::class, FORWARD, exactSpec)
        register(HomeDestination::class, null, FORWARD, wildcardSpec)
    }
    
    val result = registry.resolve(HomeDestination::class, ProfileDestination::class, FORWARD)
    
    assertEquals(exactSpec, result)
}

@Test
fun `wildcard fallback works`() {
    val wildcardSpec = StandardAnimations.slideForward()
    
    val registry = AnimationRegistry {
        register(null, null, FORWARD, wildcardSpec)
    }
    
    val result = registry.resolve(HomeDestination::class, ProfileDestination::class, FORWARD)
    
    assertEquals(wildcardSpec, result)
}

@Test
fun `Default registry provides slide animations`() {
    val registry = AnimationRegistry.Default
    
    val forward = registry.resolve(null, null, FORWARD)
    val backward = registry.resolve(null, null, BACKWARD)
    
    assertNotEquals(SurfaceAnimationSpec.None, forward)
    assertNotEquals(SurfaceAnimationSpec.None, backward)
}

@Test
fun `copy adds new registrations`() {
    val base = AnimationRegistry.Default
    val customSpec = StandardAnimations.fade()
    
    val extended = base.copy {
        register(ModalDestination::class, null, FORWARD, customSpec)
    }
    
    val result = extended.resolve(ModalDestination::class, null, FORWARD)
    
    assertEquals(customSpec, result)
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - SurfaceAnimationSpec definition
- [RENDER-002A](./RENDER-002A-core-flatten.md) - AnimationResolver integration
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost integration
- [Material Motion](https://m3.material.io/styles/motion/overview) - Design guidelines
- [Compose Animation](https://developer.android.com/jetpack/compose/animation) - API reference
