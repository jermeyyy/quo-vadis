````markdown
# HIER-011: @Transition Annotation

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-011 |
| **Task Name** | Add @Transition Annotation for Per-Screen Animations |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | HIER-003 (NavTransition data class) |
| **Blocked By** | Phase 1: HIER-003 |
| **Blocks** | HIER-014 |

---

## Overview

This task creates the `@Transition` annotation for specifying custom navigation transitions on individual destinations. This enables per-screen animation customization in the hierarchical rendering engine.

### Context

The hierarchical rendering engine uses `AnimationCoordinator` to resolve transitions for navigation events. By default, it uses a standard slide transition, but destinations can override this with `@Transition` annotations.

### Use Cases

1. **Modal Destinations**: Use fade or slide-up transitions for dialogs/modals
2. **Full-Screen Media**: Use fade transitions for immersive content
3. **Instant Navigation**: Use no animation for tab-content switches
4. **Brand-Specific**: Custom transitions matching app design language

---

## File Location

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Transition.kt
```

---

## Implementation

### TransitionType Enum

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Defines the type of transition animation for a destination.
 *
 * Each type maps to a predefined [NavTransition] in the rendering engine.
 * Use [CUSTOM] with [Transition.customTransition] for advanced animations.
 *
 * ## Transition Previews
 *
 * | Type | Enter | Exit | Pop Enter | Pop Exit |
 * |------|-------|------|-----------|----------|
 * | SLIDE_HORIZONTAL | Slide in from right | Slide out left (parallax) | Slide in from left | Slide out right |
 * | SLIDE_VERTICAL | Slide up from bottom | Fade out | Fade in | Slide down |
 * | FADE | Fade in | Fade out | Fade in | Fade out |
 * | NONE | Instant | Instant | Instant | Instant |
 * | SCALE | Scale up + fade | Scale down + fade | Scale up + fade | Scale down + fade |
 * | DEFAULT | Uses AnimationCoordinator default | | | |
 * | CUSTOM | Defined by customTransition class | | | |
 *
 * @see Transition
 * @see NavTransition
 */
enum class TransitionType {
    /**
     * Use the default transition from AnimationCoordinator.
     *
     * This is the default value. The AnimationCoordinator will apply
     * its configured default transition (typically SLIDE_HORIZONTAL).
     */
    DEFAULT,

    /**
     * Horizontal slide animation (standard navigation).
     *
     * - Enter: Slide in from right
     * - Exit: Slide out to left with parallax effect (30% speed)
     * - Pop Enter: Slide in from left
     * - Pop Exit: Slide out to right
     *
     * Best for: Standard forward/back navigation
     */
    SLIDE_HORIZONTAL,

    /**
     * Vertical slide animation (modal-like).
     *
     * - Enter: Slide up from bottom
     * - Exit: Fade out
     * - Pop Enter: Fade in
     * - Pop Exit: Slide down to bottom
     *
     * Best for: Modal screens, bottom sheets, action screens
     */
    SLIDE_VERTICAL,

    /**
     * Fade animation (crossfade).
     *
     * - Enter: Fade in
     * - Exit: Fade out
     * - Pop Enter: Fade in
     * - Pop Exit: Fade out
     *
     * Best for: Content replacement, media viewers, overlays
     */
    FADE,

    /**
     * No animation (instant transition).
     *
     * Content changes immediately without animation.
     *
     * Best for: Tab content changes, instant state updates
     */
    NONE,

    /**
     * Scale animation with fade.
     *
     * - Enter: Scale up from 85% + fade in
     * - Exit: Scale down to 85% + fade out
     * - Pop Enter: Scale up from 85% + fade in
     * - Pop Exit: Scale down to 85% + fade out
     *
     * Best for: Dialogs, popups, detail views
     */
    SCALE,

    /**
     * Custom transition defined by a NavTransitionProvider class.
     *
     * When using this type, you must also specify [Transition.customTransition]
     * with a class implementing [NavTransitionProvider].
     *
     * Example:
     * ```kotlin
     * @Transition(type = TransitionType.CUSTOM, customTransition = MyTransition::class)
     * @Destination(route = "special")
     * data object SpecialScreen : MyDestinations()
     *
     * object MyTransition : NavTransitionProvider {
     *     override fun provide(): NavTransition = NavTransition(
     *         enter = fadeIn() + scaleIn(initialScale = 0.9f),
     *         exit = fadeOut(),
     *         popEnter = fadeIn(),
     *         popExit = fadeOut() + scaleOut(targetScale = 0.9f)
     *     )
     * }
     * ```
     *
     * @see NavTransitionProvider
     */
    CUSTOM
}
```

### Transition Annotation

```kotlin
/**
 * Specifies the navigation transition animation for a destination.
 *
 * Apply this annotation to `@Destination`-annotated classes or `@Screen`-annotated
 * functions to customize how the destination animates during navigation.
 *
 * ## Usage on Destination Class
 *
 * ```kotlin
 * @Transition(type = TransitionType.FADE)
 * @Destination(route = "photo/{photoId}")
 * data class PhotoViewer(val photoId: String) : GalleryDestination()
 * ```
 *
 * ## Usage on Screen Function
 *
 * ```kotlin
 * @Transition(type = TransitionType.SLIDE_VERTICAL)
 * @Screen(SettingsDestination.Preferences::class)
 * @Composable
 * fun PreferencesScreen(destination: SettingsDestination.Preferences) { ... }
 * ```
 *
 * ## Priority Rules
 *
 * When both a destination class and its screen function have `@Transition`:
 * 1. Screen function annotation takes precedence
 * 2. Destination class annotation is used as fallback
 * 3. AnimationCoordinator default is used if neither is specified
 *
 * ## Custom Transitions
 *
 * For complex animations, use [TransitionType.CUSTOM] with a provider class:
 *
 * ```kotlin
 * @Transition(type = TransitionType.CUSTOM, customTransition = SharedElementTransition::class)
 * @Destination(route = "detail/{id}")
 * data class DetailScreen(val id: String) : AppDestinations()
 *
 * object SharedElementTransition : NavTransitionProvider {
 *     override fun provide(): NavTransition = NavTransition(
 *         enter = fadeIn(tween(300)) + slideInHorizontally { it / 2 },
 *         exit = fadeOut(tween(300)),
 *         popEnter = fadeIn(tween(300)),
 *         popExit = fadeOut(tween(300)) + slideOutHorizontally { it / 2 }
 *     )
 * }
 * ```
 *
 * ## Generated Code
 *
 * KSP generates a `TransitionRegistry` that maps destinations to transitions:
 *
 * ```kotlin
 * // Generated: GeneratedTransitionRegistry.kt
 * object GeneratedTransitionRegistry : TransitionRegistry {
 *     override fun getTransition(destinationClass: KClass<*>): NavTransition? {
 *         return when (destinationClass) {
 *             PhotoViewer::class -> NavTransition.Fade
 *             DetailScreen::class -> SharedElementTransition.provide()
 *             else -> null  // Use default
 *         }
 *     }
 * }
 * ```
 *
 * ## Animation Timing
 *
 * Default animation durations:
 * - SLIDE_HORIZONTAL: 300ms enter, 300ms exit
 * - SLIDE_VERTICAL: 300ms enter, 250ms exit
 * - FADE: 300ms
 * - SCALE: 300ms
 *
 * Custom transitions can specify their own timing.
 *
 * @property type The transition type to use. Defaults to [TransitionType.DEFAULT].
 * @property customTransition Class implementing [NavTransitionProvider] for custom
 *   transitions. Only used when [type] is [TransitionType.CUSTOM]. The class must
 *   be an `object` or have a no-arg constructor.
 *
 * @see TransitionType
 * @see NavTransitionProvider
 * @see NavTransition
 * @see Destination
 * @see Screen
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Transition(
    /**
     * The type of transition animation.
     */
    val type: TransitionType = TransitionType.DEFAULT,

    /**
     * Custom transition provider class.
     * Only used when type = TransitionType.CUSTOM.
     */
    val customTransition: KClass<*> = Unit::class
)
```

### NavTransitionProvider Interface

Create the provider interface in the core module:

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/animation/NavTransitionProvider.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.animation

/**
 * Interface for providing custom navigation transitions.
 *
 * Implement this interface in an `object` or class with no-arg constructor
 * to define custom transition animations for use with [@Transition].
 *
 * ## Usage
 *
 * ```kotlin
 * // Define custom transition provider
 * object BounceTransition : NavTransitionProvider {
 *     override fun provide(): NavTransition = NavTransition(
 *         enter = fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
 *                 scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
 *         exit = fadeOut(tween(200)),
 *         popEnter = fadeIn(tween(200)),
 *         popExit = fadeOut(tween(200)) + scaleOut(tween(200))
 *     )
 * }
 *
 * // Apply to destination
 * @Transition(type = TransitionType.CUSTOM, customTransition = BounceTransition::class)
 * @Destination(route = "celebration")
 * data object CelebrationScreen : AppDestinations()
 * ```
 *
 * ## Guidelines
 *
 * - Keep transitions performant (avoid expensive calculations in [provide])
 * - Consider reduced motion accessibility settings
 * - Match app's design language for consistency
 * - Test on lower-end devices for smooth animations
 *
 * ## Caching
 *
 * The [provide] method may be called multiple times during navigation.
 * If your transition is expensive to create, cache it:
 *
 * ```kotlin
 * object CachedTransition : NavTransitionProvider {
 *     private val cached = NavTransition(...)
 *     override fun provide(): NavTransition = cached
 * }
 * ```
 *
 * @see Transition
 * @see TransitionType
 * @see NavTransition
 */
interface NavTransitionProvider {
    /**
     * Provides the navigation transition configuration.
     *
     * @return The NavTransition to use for this destination
     */
    fun provide(): NavTransition
}
```

---

## Integration Points

### 1. KSP Processing (HIER-014)

The `@Transition` annotation is processed by KSP:
- Finds all classes/functions annotated with `@Transition`
- Extracts `type` and `customTransition` values
- Validates `customTransition` implements `NavTransitionProvider` when type is CUSTOM
- Maps to `TransitionInfo` model

### 2. Registry Generation (HIER-014)

The `TransitionRegistryGenerator` produces:
- `GeneratedTransitionRegistry` object implementing `TransitionRegistry`
- `when` expression mapping destination classes to `NavTransition` instances
- Handles all `TransitionType` enum values
- Instantiates custom transition providers

### 3. Runtime Resolution (AnimationCoordinator - HIER-004)

The `AnimationCoordinator` uses the registry:
```kotlin
fun getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition {
    val toDestination = (to as? ScreenNode)?.destination
    val annotatedTransition = toDestination?.let { 
        transitionRegistry.getTransition(it::class)
    }
    return annotatedTransition ?: defaultTransition
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class TransitionAnnotationTest {

    @Test
    fun `annotation targets class and function`() {
        val targets = Transition::class.annotations
            .filterIsInstance<Target>()
            .first()
            .allowedTargets
        
        assertContains(targets, AnnotationTarget.CLASS)
        assertContains(targets, AnnotationTarget.FUNCTION)
    }

    @Test
    fun `annotation has source retention`() {
        val retention = Transition::class.annotations
            .filterIsInstance<Retention>()
            .first()
            .value
        
        assertEquals(AnnotationRetention.SOURCE, retention)
    }

    @Test
    fun `annotation has default type parameter`() {
        val typeParam = Transition::class.primaryConstructor?.parameters
            ?.find { it.name == "type" }
        assertNotNull(typeParam)
        assertTrue(typeParam.isOptional)
    }

    @Test
    fun `annotation has default customTransition parameter`() {
        val customParam = Transition::class.primaryConstructor?.parameters
            ?.find { it.name == "customTransition" }
        assertNotNull(customParam)
        assertTrue(customParam.isOptional)
    }
}

class TransitionTypeTest {

    @Test
    fun `all expected types are defined`() {
        val types = TransitionType.values()
        assertContains(types, TransitionType.DEFAULT)
        assertContains(types, TransitionType.SLIDE_HORIZONTAL)
        assertContains(types, TransitionType.SLIDE_VERTICAL)
        assertContains(types, TransitionType.FADE)
        assertContains(types, TransitionType.NONE)
        assertContains(types, TransitionType.SCALE)
        assertContains(types, TransitionType.CUSTOM)
    }
}
```

### KSP Integration Tests (HIER-014)

```kotlin
@Test
fun `KSP generates registry for annotated destinations`() {
    // Given
    val source = """
        @Transition(type = TransitionType.FADE)
        @Destination(route = "photo")
        data object PhotoScreen : AppDestinations()
        
        @Transition(type = TransitionType.SLIDE_VERTICAL)
        @Destination(route = "modal")
        data object ModalScreen : AppDestinations()
    """
    
    // When processed by KSP
    // Then generates:
    val expectedGenerated = """
        object GeneratedTransitionRegistry : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return when (destinationClass) {
                    PhotoScreen::class -> NavTransition.Fade
                    ModalScreen::class -> NavTransition.SlideVertical
                    else -> null
                }
            }
        }
    """
}

@Test
fun `KSP validates customTransition for CUSTOM type`() {
    // Given
    val source = """
        @Transition(type = TransitionType.CUSTOM)  // Missing customTransition!
        @Destination(route = "special")
        data object SpecialScreen : AppDestinations()
    """
    
    // When processed by KSP
    // Then error: "customTransition required when type is CUSTOM"
}

@Test
fun `KSP validates customTransition implements NavTransitionProvider`() {
    // Given
    val source = """
        object NotAProvider  // Does not implement NavTransitionProvider
        
        @Transition(type = TransitionType.CUSTOM, customTransition = NotAProvider::class)
        @Destination(route = "special")
        data object SpecialScreen : AppDestinations()
    """
    
    // When processed by KSP
    // Then error: "customTransition must implement NavTransitionProvider"
}

@Test
fun `KSP handles screen function annotation`() {
    // Given
    val source = """
        @Destination(route = "settings")
        data object SettingsScreen : AppDestinations()
        
        @Transition(type = TransitionType.SLIDE_VERTICAL)
        @Screen(SettingsScreen::class)
        @Composable
        fun SettingsScreenComposable(dest: SettingsScreen) { }
    """
    
    // When processed by KSP
    // Then registry uses SLIDE_VERTICAL for SettingsScreen
}
```

---

## Acceptance Criteria

- [ ] `@Transition` annotation defined in `quo-vadis-annotations` module
- [ ] `TransitionType` enum defined with all types (DEFAULT, SLIDE_HORIZONTAL, SLIDE_VERTICAL, FADE, NONE, SCALE, CUSTOM)
- [ ] `@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)` specified
- [ ] `@Retention(AnnotationRetention.SOURCE)` specified
- [ ] `type: TransitionType = TransitionType.DEFAULT` parameter defined
- [ ] `customTransition: KClass<*> = Unit::class` parameter defined
- [ ] `NavTransitionProvider` interface defined in `quo-vadis-core`
- [ ] Comprehensive KDoc documentation with examples
- [ ] Unit tests for annotation properties
- [ ] Unit tests for TransitionType enum
- [ ] Annotation compiles on all target platforms (Android, iOS, Desktop, Web)

---

## Notes

### Design Decisions

1. **Target Both Class and Function**: Allows flexibility - annotate either the destination class or the screen composable function.

2. **Unit::class Default**: Using `Unit::class` as default for `customTransition` follows Kotlin convention for optional class parameters.

3. **Provider Interface**: Using a provider interface (vs direct NavTransition in annotation) allows complex transitions with Compose animation specs that can't be expressed as annotation parameters.

### Open Questions

1. **Reduced Motion**: Should we add a `respectReducedMotion: Boolean` parameter to optionally disable animations for accessibility?

2. **Direction Awareness**: Should transitions be able to vary based on navigation direction (e.g., different enter animation from tab vs from deep link)?

### Future Enhancements

- Shared element transition integration
- Gesture-driven transition progress
- Transition duration customization
- Per-route transition overrides

---

## References

- [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md) - Architecture overview
- [HIER-003](HIER-003-nav-transition.md) - NavTransition data class (Phase 1)
- [HIER-004](HIER-004-animation-coordinator.md) - AnimationCoordinator (Phase 1)

````