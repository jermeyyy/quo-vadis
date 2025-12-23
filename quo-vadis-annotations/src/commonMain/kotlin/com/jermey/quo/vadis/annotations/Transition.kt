package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Specifies the type of transition animation.
 *
 * Use these preset types for common transitions, or [Custom] to reference
 * a custom implementation of the `NavTransition` interface.
 *
 * ## Preset Transitions
 *
 * | Type | Enter | Exit | Best For |
 * |------|-------|------|----------|
 * | SlideHorizontal | Slide in from right | Slide out to left | Stack navigation |
 * | SlideVertical | Slide in from bottom | Slide out to top | Modal sheets |
 * | Fade | Fade in | Fade out | Tab switches, overlays |
 * | None | Instant | Instant | Performance-critical, testing |
 * | Custom | User-defined | User-defined | Complex animations |
 *
 * @see Transition
 */
enum class TransitionType {
    /**
     * Horizontal slide transition.
     *
     * - Enter: Slides in from right
     * - Exit: Slides out to left
     * - Pop Enter: Slides in from left
     * - Pop Exit: Slides out to right
     *
     * Best for: Standard stack-based navigation (push/pop).
     */
    SlideHorizontal,

    /**
     * Vertical slide transition.
     *
     * - Enter: Slides in from bottom
     * - Exit: Slides out to top
     * - Pop Enter: Slides in from top
     * - Pop Exit: Slides out to bottom
     *
     * Best for: Modal sheets, bottom sheets, vertical flows.
     */
    SlideVertical,

    /**
     * Fade transition.
     *
     * - Enter: Fades in from transparent
     * - Exit: Fades out to transparent
     *
     * Best for: Tab switches, overlays, cross-fades.
     */
    Fade,

    /**
     * No transition animation.
     *
     * Content appears and disappears instantly.
     *
     * Best for: Performance-critical scenarios, testing,
     * or when animations are not desired.
     */
    None,

    /**
     * Custom transition defined by a `NavTransition` implementation.
     *
     * When using this type, you must also specify the [Transition.customTransition]
     * parameter with a reference to your custom `NavTransition` class.
     *
     * @see Transition.customTransition
     */
    Custom
}

/**
 * Defines the transition animation when navigating TO this destination.
 *
 * Apply this annotation to `@Destination`-annotated classes to specify
 * how the destination should animate when it becomes visible. The animation
 * is automatically reversed for pop operations (back navigation).
 *
 * ## Usage with Preset Transitions
 *
 * Use [TransitionType] presets for common animations:
 *
 * ```kotlin
 * @Transition(type = TransitionType.SlideHorizontal)
 * @Destination(route = "details/{id}")
 * data class DetailsDestination(val id: String) : HomeDestination()
 *
 * @Transition(type = TransitionType.Fade)
 * @Destination(route = "settings")
 * data object SettingsDestination : HomeDestination()
 *
 * @Transition(type = TransitionType.SlideVertical)
 * @Destination(route = "modal")
 * data object ModalDestination : HomeDestination()
 * ```
 *
 * ## Usage with Custom Transitions
 *
 * For complex animations, create a custom `NavTransition` implementation
 * and reference it using [TransitionType.Custom]:
 *
 * ```kotlin
 * // Define custom transition
 * object ScaleAndFadeTransition {
 *     val transition = NavTransition(
 *         enter = fadeIn() + scaleIn(initialScale = 0.8f),
 *         exit = fadeOut() + scaleOut(targetScale = 1.2f),
 *         popEnter = fadeIn() + scaleIn(initialScale = 1.2f),
 *         popExit = fadeOut() + scaleOut(targetScale = 0.8f)
 *     )
 * }
 *
 * // Apply to destination
 * @Transition(
 *     type = TransitionType.Custom,
 *     customTransition = ScaleAndFadeTransition::class
 * )
 * @Destination(route = "animated")
 * data object AnimatedDestination : HomeDestination()
 * ```
 *
 * ### Custom Transition Requirements
 *
 * The custom transition class must:
 * - Be an `object` or have a no-arg constructor
 * - Have a `transition` property of type `NavTransition`
 *
 * ## Animation Direction
 *
 * The `AnimationCoordinator` automatically handles animation direction:
 * - **Forward navigation**: Uses `enter` and `exit`
 * - **Back navigation**: Uses `popEnter` and `popExit`
 *
 * ## Resolution Order
 *
 * When determining which transition to use, the `AnimationCoordinator`
 * checks in this order:
 *
 * 1. `@Transition` annotation on the destination (via `TransitionRegistry`)
 * 2. Parent container default (tab/pane specific)
 * 3. Global default (`NavTransition.SlideHorizontal`)
 *
 * ## KSP Processing
 *
 * KSP generates entries in `GeneratedTransitionRegistry` mapping each
 * destination class to its `NavTransition` instance. The registry is
 * used by `AnimationCoordinator` at runtime.
 *
 * ## Examples
 *
 * ### Standard Navigation Flow
 * ```kotlin
 * @Stack(name = "home", startDestination = HomeDestination.List::class)
 * sealed class HomeDestination {
 *
 *     // Default transition (SlideHorizontal)
 *     @Destination(route = "list")
 *     data object List : HomeDestination()
 *
 *     // Explicit slide transition
 *     @Transition(type = TransitionType.SlideHorizontal)
 *     @Destination(route = "details/{id}")
 *     data class Details(val id: String) : HomeDestination()
 *
 *     // Modal with vertical slide
 *     @Transition(type = TransitionType.SlideVertical)
 *     @Destination(route = "filter")
 *     data object Filter : HomeDestination()
 *
 *     // Overlay with fade
 *     @Transition(type = TransitionType.Fade)
 *     @Destination(route = "help")
 *     data object Help : HomeDestination()
 * }
 * ```
 *
 * ### Tab Content Transitions
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab {
 *
 *     @Transition(type = TransitionType.Fade)
 *     @Destination(route = "feed")
 *     data object Feed : HomeTab()
 *
 *     @Transition(type = TransitionType.SlideHorizontal)
 *     @Destination(route = "article/{id}")
 *     data class Article(val id: String) : HomeTab()
 * }
 * ```
 *
 * @property type The type of transition animation to use. Defaults to
 *   [TransitionType.SlideHorizontal] for standard push/pop behavior.
 * @property customTransition When [type] is [TransitionType.Custom], this
 *   specifies the class containing the custom `NavTransition`. The class
 *   must have a `transition` property of type `NavTransition`. Defaults to
 *   `Unit::class` to indicate no custom transition.
 *
 * @see TransitionType
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transition(
    /**
     * The type of transition animation to use.
     * Defaults to SlideHorizontal for standard navigation.
     */
    val type: TransitionType = TransitionType.SlideHorizontal,

    /**
     * Custom transition class when [type] is [TransitionType.Custom].
     * The class must have a `transition` property of type `NavTransition`.
     * Defaults to Unit::class (no custom transition).
     */
    val customTransition: KClass<*> = Unit::class
)
