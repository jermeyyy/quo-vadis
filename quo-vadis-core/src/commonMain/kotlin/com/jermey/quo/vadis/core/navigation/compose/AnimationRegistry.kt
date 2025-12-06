package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import kotlin.reflect.KClass

/**
 * Centralized registry for navigation transition animations.
 *
 * AnimationRegistry maps navigation transitions to animation specifications.
 * Transitions are identified by:
 * - Source destination class (or wildcard)
 * - Target destination class (or wildcard)
 * - Transition type (PUSH, POP, TAB_SWITCH, PANE_SWITCH, or any)
 *
 * ## Lookup Priority
 *
 * When resolving an animation, the registry uses the following priority order:
 * 1. **Exact match**: `(HomeDestination::class, ProfileDestination::class, PUSH)`
 * 2. **Wildcard target**: `(HomeDestination::class, null, PUSH)`
 * 3. **Wildcard source**: `(null, ProfileDestination::class, PUSH)`
 * 4. **Both wildcards**: `(null, null, PUSH)`
 * 5. **Transition type default**: Registered via [Builder.registerDefault]
 * 6. **Global default**: `SurfaceAnimationSpec.None`
 *
 * ## Basic Usage
 *
 * ```kotlin
 * val registry = AnimationRegistry {
 *     // Register specific transition
 *     register(
 *         from = HomeDestination::class,
 *         to = ProfileDestination::class,
 *         transitionType = TransitionType.PUSH,
 *         spec = SurfaceAnimationSpec(
 *             enter = slideInHorizontally { it },
 *             exit = slideOutHorizontally { -it / 3 }
 *         )
 *     )
 *
 *     // Register default for all forward navigation
 *     registerDefault(
 *         transitionType = TransitionType.PUSH,
 *         spec = StandardAnimations.slideForward()
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
 * ## Type-Safe DSL
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     useSlideForward()
 *     useSlideBackward()
 *
 *     forwardTransition<HomeDestination, ProfileDestination>(
 *         StandardAnimations.scale()
 *     )
 * }
 * ```
 *
 * @see SurfaceAnimationSpec
 * @see TreeFlattener.AnimationResolver
 * @see StandardAnimations
 */
public class AnimationRegistry private constructor(
    private val registrations: Map<AnimationKey, SurfaceAnimationSpec>,
    private val defaults: Map<TransitionType?, SurfaceAnimationSpec>
) {

    /**
     * Key for animation lookup.
     *
     * Combines source destination class, target destination class, and
     * transition type to uniquely identify a transition animation.
     * Null values act as wildcards.
     */
    private data class AnimationKey(
        val fromClass: KClass<out Destination>?,
        val toClass: KClass<out Destination>?,
        val transitionType: TransitionType?
    )

    /**
     * Looks up the animation for a transition.
     *
     * Uses the priority order described in the class documentation
     * to find the most specific matching animation specification.
     *
     * @param from The source destination class (null if unknown or for wildcard match)
     * @param to The target destination class (null if unknown or for wildcard match)
     * @param transitionType The type of transition
     * @return The animation spec for this transition, or [SurfaceAnimationSpec.None] if not found
     */
    public fun resolve(
        from: KClass<out Destination>?,
        to: KClass<out Destination>?,
        transitionType: TransitionType
    ): SurfaceAnimationSpec {
        // Priority 1: Exact match
        registrations[AnimationKey(from, to, transitionType)]?.let { return it }

        // Priority 2: Wildcard target
        registrations[AnimationKey(from, null, transitionType)]?.let { return it }

        // Priority 3: Wildcard source
        registrations[AnimationKey(null, to, transitionType)]?.let { return it }

        // Priority 4: Both wildcards with transition type
        registrations[AnimationKey(null, null, transitionType)]?.let { return it }

        // Priority 5: Transition-type-specific default
        defaults[transitionType]?.let { return it }

        // Priority 6: Global default
        return defaults[null] ?: SurfaceAnimationSpec.None
    }

    /**
     * Converts this registry to a [TreeFlattener.AnimationResolver].
     *
     * This allows the AnimationRegistry to be used directly with TreeFlattener
     * for resolving animations during tree flattening.
     *
     * @return An [AnimationResolver][TreeFlattener.AnimationResolver] backed by this registry
     */
    public fun toAnimationResolver(): TreeFlattener.AnimationResolver {
        return TreeFlattener.AnimationResolver { from, to, transitionType ->
            val fromClass = (from as? ScreenNode)?.destination?.let { it::class }
            val toClass = (to as? ScreenNode)?.destination?.let { it::class }
            resolve(fromClass, toClass, transitionType)
        }
    }

    /**
     * Creates a copy of this registry with additional registrations.
     *
     * Useful for extending an existing registry (like [Default]) with
     * additional custom animations without modifying the original.
     *
     * ## Example
     *
     * ```kotlin
     * val customRegistry = AnimationRegistry.Default.copy {
     *     register(
     *         from = ModalDestination::class,
     *         transitionType = TransitionType.PUSH,
     *         spec = StandardAnimations.slideVertical()
     *     )
     * }
     * ```
     *
     * @param block Configuration block for additional registrations
     * @return A new [AnimationRegistry] with combined registrations
     */
    public fun copy(block: Builder.() -> Unit): AnimationRegistry {
        val builder = Builder()
        // Re-register all existing registrations via the public API
        registrations.forEach { (key, spec) ->
            builder.register(key.fromClass, key.toClass, key.transitionType, spec)
        }
        defaults.forEach { (transitionType, spec) ->
            builder.registerDefault(transitionType, spec)
        }
        builder.apply(block)
        return builder.build()
    }

    /**
     * Combines this registry with another, with the other taking precedence.
     *
     * When the same key exists in both registries, the animation from
     * [other] will be used.
     *
     * @param other The registry to combine with
     * @return A new [AnimationRegistry] with combined registrations
     */
    public operator fun plus(other: AnimationRegistry): AnimationRegistry {
        return AnimationRegistry(
            registrations = this.registrations + other.registrations,
            defaults = this.defaults + other.defaults
        )
    }

    /**
     * Builder for [AnimationRegistry].
     *
     * Provides methods for registering animations for specific transitions
     * or as defaults for transition types.
     */
    public class Builder internal constructor() {
        private val registrations = mutableMapOf<AnimationKey, SurfaceAnimationSpec>()
        private val defaults = mutableMapOf<TransitionType?, SurfaceAnimationSpec>()

        /**
         * Registers an animation for a specific transition.
         *
         * @param from Source destination class (null for wildcard)
         * @param to Target destination class (null for wildcard)
         * @param transitionType Transition type (null for any transition type)
         * @param spec The animation specification
         */
        public fun register(
            from: KClass<out Destination>? = null,
            to: KClass<out Destination>? = null,
            transitionType: TransitionType? = null,
            spec: SurfaceAnimationSpec
        ) {
            registrations[AnimationKey(from, to, transitionType)] = spec
        }

        /**
         * Registers a default animation for a transition type.
         *
         * Default animations are used when no specific registration matches.
         *
         * @param transitionType Transition type (null for global default)
         * @param spec The animation specification
         */
        public fun registerDefault(
            transitionType: TransitionType? = null,
            spec: SurfaceAnimationSpec
        ) {
            defaults[transitionType] = spec
        }

        /**
         * Registers the built-in slide animation for PUSH navigation.
         *
         * Equivalent to:
         * ```kotlin
         * registerDefault(TransitionType.PUSH, StandardAnimations.slideForward())
         * ```
         */
        public fun useSlideForward() {
            registerDefault(TransitionType.PUSH, StandardAnimations.slideForward())
        }

        /**
         * Registers the built-in slide animation for POP navigation.
         *
         * Equivalent to:
         * ```kotlin
         * registerDefault(TransitionType.POP, StandardAnimations.slideBackward())
         * ```
         */
        public fun useSlideBackward() {
            registerDefault(TransitionType.POP, StandardAnimations.slideBackward())
        }

        /**
         * Registers the built-in fade animation.
         *
         * @param transitionType The transition type (null for all types)
         */
        public fun useFade(transitionType: TransitionType? = null) {
            registerDefault(transitionType, StandardAnimations.fade())
        }

        /**
         * Registers a fade animation for tab switches.
         *
         * Equivalent to:
         * ```kotlin
         * registerDefault(TransitionType.TAB_SWITCH, StandardAnimations.fade())
         * ```
         */
        public fun useFadeForTabs() {
            registerDefault(TransitionType.TAB_SWITCH, StandardAnimations.fade())
        }

        /**
         * Registers no animation for pane switches.
         *
         * Equivalent to:
         * ```kotlin
         * registerDefault(TransitionType.PANE_SWITCH, SurfaceAnimationSpec.None)
         * ```
         */
        public fun useNoAnimationForPanes() {
            registerDefault(TransitionType.PANE_SWITCH, SurfaceAnimationSpec.None)
        }

        /**
         * Registers a custom animation using a DSL block.
         *
         * ## Example
         *
         * ```kotlin
         * transition<HomeDestination, ProfileDestination>(TransitionType.PUSH) {
         *     StandardAnimations.scale()
         * }
         * ```
         *
         * @param F Source destination type
         * @param T Target destination type
         * @param transitionType The transition type
         * @param spec Lambda that returns the animation specification
         */
        public inline fun <reified F : Destination, reified T : Destination> transition(
            transitionType: TransitionType = TransitionType.PUSH,
            noinline spec: () -> SurfaceAnimationSpec
        ) {
            register(F::class, T::class, transitionType, spec())
        }

        internal fun build(): AnimationRegistry {
            return AnimationRegistry(registrations.toMap(), defaults.toMap())
        }
    }

    public companion object {
        /**
         * Creates an [AnimationRegistry] with the given configuration.
         *
         * @param block Configuration block for the registry builder
         * @return A new [AnimationRegistry] instance
         */
        public operator fun invoke(block: Builder.() -> Unit): AnimationRegistry {
            return Builder().apply(block).build()
        }

        /**
         * Default animation registry with standard slide animations.
         *
         * Provides:
         * - Slide forward animation for PUSH transitions
         * - Slide backward animation for POP transitions
         * - Fade animation for TAB_SWITCH transitions
         * - No animation for PANE_SWITCH transitions
         */
        public val Default: AnimationRegistry = AnimationRegistry {
            useSlideForward()
            useSlideBackward()
            useFadeForTabs()
            useNoAnimationForPanes()
        }

        /**
         * Empty animation registry (no animations).
         *
         * All transitions will use [SurfaceAnimationSpec.None], resulting
         * in instant appearance/disappearance without visual transitions.
         */
        public val None: AnimationRegistry = AnimationRegistry {
            registerDefault(spec = SurfaceAnimationSpec.None)
        }
    }
}

// =============================================================================
// DSL Extensions
// =============================================================================

/**
 * Creates an [AnimationRegistry] using the DSL.
 *
 * This is a convenience function equivalent to [AnimationRegistry.invoke].
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     useSlideForward()
 *     useSlideBackward()
 * }
 * ```
 *
 * @param block Configuration block for the registry builder
 * @return A new [AnimationRegistry] instance
 */
public fun animationRegistry(block: AnimationRegistry.Builder.() -> Unit): AnimationRegistry {
    return AnimationRegistry(block)
}

/**
 * Convenience function for specifying forward (PUSH) transition animations.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     forwardTransition<HomeDestination, ProfileDestination>(
 *         StandardAnimations.scale()
 *     )
 * }
 * ```
 *
 * @param F Source destination type
 * @param T Target destination type
 * @param spec The animation specification for this transition
 */
public inline fun <reified F : Destination, reified T : Destination> AnimationRegistry.Builder.forwardTransition(
    spec: SurfaceAnimationSpec
) {
    register(F::class, T::class, TransitionType.PUSH, spec)
}

/**
 * Convenience function for specifying backward (POP) transition animations.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     backwardTransition<ProfileDestination, HomeDestination>(
 *         StandardAnimations.slideBackward()
 *     )
 * }
 * ```
 *
 * @param F Source destination type (where we're coming from)
 * @param T Target destination type (where we're going back to)
 * @param spec The animation specification for this transition
 */
public inline fun <reified F : Destination, reified T : Destination> AnimationRegistry.Builder.backwardTransition(
    spec: SurfaceAnimationSpec
) {
    register(F::class, T::class, TransitionType.POP, spec)
}

/**
 * Registers both forward and backward animations at once.
 *
 * This is useful when you want to define complementary animations
 * for navigation between two destinations in both directions.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     biDirectionalTransition<ListDestination, DetailDestination>(
 *         forward = StandardAnimations.sharedAxis(SharedAxis.X),
 *         backward = StandardAnimations.sharedAxis(SharedAxis.X)
 *     )
 * }
 * ```
 *
 * @param From Source destination type for forward, target for backward
 * @param To Target destination type for forward, source for backward
 * @param forward Animation for PUSH transition (From → To)
 * @param backward Animation for POP transition (To → From)
 */
public inline fun <reified From : Destination, reified To : Destination> AnimationRegistry.Builder.biDirectionalTransition(
    forward: SurfaceAnimationSpec,
    backward: SurfaceAnimationSpec = StandardAnimations.slideBackward()
) {
    register(From::class, To::class, TransitionType.PUSH, forward)
    register(To::class, From::class, TransitionType.POP, backward)
}

/**
 * Registers a tab switch animation for transitions between tabs.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     tabSwitchTransition(StandardAnimations.fade())
 * }
 * ```
 *
 * @param spec The animation specification for tab switches
 */
public fun AnimationRegistry.Builder.tabSwitchTransition(spec: SurfaceAnimationSpec) {
    registerDefault(TransitionType.TAB_SWITCH, spec)
}

/**
 * Registers a pane switch animation for transitions between panes.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     paneSwitchTransition(StandardAnimations.fade())
 * }
 * ```
 *
 * @param spec The animation specification for pane switches
 */
public fun AnimationRegistry.Builder.paneSwitchTransition(spec: SurfaceAnimationSpec) {
    registerDefault(TransitionType.PANE_SWITCH, spec)
}
