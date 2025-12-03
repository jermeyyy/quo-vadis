# Task ANN-003: Add Transition Metadata to @Route

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-003 |
| **Name** | Enhance @Route with Transition Hints |
| **Phase** | 4 - Annotations Enhancement |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | None |

## Overview

Add optional transition metadata to the `@Route` annotation, allowing developers to specify enter/exit animations declaratively. The KSP processor will use this to populate the `AnimationRegistry`.

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/.../annotations/Annotations.kt

/**
 * Predefined transition types for navigation animations.
 */
enum class TransitionType {
    /** No animation */
    NONE,
    /** Horizontal slide (left/right) */
    SLIDE_HORIZONTAL,
    /** Vertical slide (up/down) */
    SLIDE_VERTICAL,
    /** Fade in/out */
    FADE,
    /** Scale up/down with fade */
    SCALE,
    /** Material shared axis X */
    SHARED_AXIS_X,
    /** Material shared axis Y */
    SHARED_AXIS_Y,
    /** Material shared axis Z */
    SHARED_AXIS_Z,
    /** Material fade through */
    FADE_THROUGH,
    /** Use default animation from AnimationRegistry */
    DEFAULT
}

/**
 * Marks a class or object as a navigation destination (route).
 * 
 * @param path URL path for deep linking (optional)
 * @param enterTransition Animation when navigating TO this destination
 * @param exitTransition Animation when navigating AWAY from this destination
 * @param popEnterTransition Animation when returning TO this destination via back
 * @param popExitTransition Animation when popping this destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(
    val path: String = "",
    val enterTransition: TransitionType = TransitionType.DEFAULT,
    val exitTransition: TransitionType = TransitionType.DEFAULT,
    val popEnterTransition: TransitionType = TransitionType.DEFAULT,
    val popExitTransition: TransitionType = TransitionType.DEFAULT
)
```

## Usage Examples

```kotlin
@Graph(name = "settings")
sealed class SettingsGraph {
    
    // Uses default transitions
    @Route
    object Main : SettingsGraph()
    
    // Custom slide transition
    @Route(
        enterTransition = TransitionType.SLIDE_HORIZONTAL,
        popExitTransition = TransitionType.SLIDE_HORIZONTAL
    )
    object Privacy : SettingsGraph()
    
    // Modal-style scale transition
    @Route(
        enterTransition = TransitionType.SCALE,
        exitTransition = TransitionType.FADE,
        popEnterTransition = TransitionType.FADE,
        popExitTransition = TransitionType.SCALE
    )
    data class Confirmation(val message: String) : SettingsGraph()
    
    // No animation (instant switch)
    @Route(
        enterTransition = TransitionType.NONE,
        exitTransition = TransitionType.NONE
    )
    object Loading : SettingsGraph()
}
```

## Generated AnimationRegistry Entries

```kotlin
// Generated: SettingsGraphAnimations.kt

fun registerSettingsGraphTransitions(registry: AnimationRegistry) {
    // Privacy screen transitions
    registry.register(
        fromAny = true,
        to = Privacy::class,
        direction = NavigationDirection.FORWARD,
        spec = AnimationSpecs.slideHorizontalEnter()
    )
    registry.register(
        from = Privacy::class,
        toAny = true,
        direction = NavigationDirection.BACKWARD,
        spec = AnimationSpecs.slideHorizontalExit()
    )
    
    // Confirmation screen transitions
    registry.register(
        fromAny = true,
        to = Confirmation::class,
        direction = NavigationDirection.FORWARD,
        spec = AnimationSpecs.scaleEnter()
    )
    // ... etc
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/Annotations.kt` | Modify |
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/TransitionType.kt` | New |

## Acceptance Criteria

- [ ] `TransitionType` enum created with all standard transitions
- [ ] `@Route` updated with transition parameters
- [ ] All parameters are optional with DEFAULT as default value
- [ ] KDoc documents each transition type
- [ ] Backward compatible (existing @Route usage unaffected)

## References

- [RENDER-006: AnimationRegistry](../phase2-renderer/RENDER-006-animation-registry.md)
- [Current NavigationTransition.kt](../../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationTransition.kt)
