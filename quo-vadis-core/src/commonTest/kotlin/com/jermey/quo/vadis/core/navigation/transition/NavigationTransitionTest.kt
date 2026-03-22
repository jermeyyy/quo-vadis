package com.jermey.quo.vadis.core.navigation.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NavigationTransitionTest : FunSpec({

    // =========================================================================
    // NavigationTransitions preset instances
    // =========================================================================

    test("NavigationTransitions.None has no animations") {
        val none = NavigationTransitions.None

        none.enter shouldBe EnterTransition.None
        none.exit shouldBe ExitTransition.None
        none.popEnter shouldBe EnterTransition.None
        none.popExit shouldBe ExitTransition.None
    }

    test("NavigationTransitions.Fade has non-None transitions") {
        val fade = NavigationTransitions.Fade

        fade.enter shouldNotBe EnterTransition.None
        fade.exit shouldNotBe ExitTransition.None
        fade.popEnter shouldNotBe EnterTransition.None
        fade.popExit shouldNotBe ExitTransition.None
    }

    test("NavigationTransitions.SlideHorizontal has non-None transitions") {
        val slide = NavigationTransitions.SlideHorizontal

        slide.enter shouldNotBe EnterTransition.None
        slide.exit shouldNotBe ExitTransition.None
        slide.popEnter shouldNotBe EnterTransition.None
        slide.popExit shouldNotBe ExitTransition.None
    }

    test("NavigationTransitions.SlideVertical has non-None transitions") {
        val slide = NavigationTransitions.SlideVertical

        slide.enter shouldNotBe EnterTransition.None
        slide.exit shouldNotBe ExitTransition.None
        slide.popEnter shouldNotBe EnterTransition.None
        slide.popExit shouldNotBe ExitTransition.None
    }

    test("NavigationTransitions.ScaleIn has non-None transitions") {
        val scale = NavigationTransitions.ScaleIn

        scale.enter shouldNotBe EnterTransition.None
        scale.exit shouldNotBe ExitTransition.None
        scale.popEnter shouldNotBe EnterTransition.None
        scale.popExit shouldNotBe ExitTransition.None
    }

    test("ANIMATION_DURATION is 300ms") {
        NavigationTransitions.ANIMATION_DURATION shouldBe 300
    }

    // =========================================================================
    // TransitionBuilder
    // =========================================================================

    test("TransitionBuilder defaults to Fade transitions") {
        val builder = TransitionBuilder()

        builder.enter shouldBe NavigationTransitions.Fade.enter
        builder.exit shouldBe NavigationTransitions.Fade.exit
        builder.popEnter shouldBe NavigationTransitions.Fade.popEnter
        builder.popExit shouldBe NavigationTransitions.Fade.popExit
    }

    test("TransitionBuilder.build creates NavigationTransition with all fields") {
        val builder = TransitionBuilder().apply {
            enter = EnterTransition.None
            exit = ExitTransition.None
            popEnter = EnterTransition.None
            popExit = ExitTransition.None
        }

        val transition = builder.build()

        transition.enter shouldBe EnterTransition.None
        transition.exit shouldBe ExitTransition.None
        transition.popEnter shouldBe EnterTransition.None
        transition.popExit shouldBe ExitTransition.None
    }

    test("TransitionBuilder.build with custom enter only retains defaults for others") {
        val builder = TransitionBuilder().apply {
            enter = EnterTransition.None
        }

        val transition = builder.build()

        transition.enter shouldBe EnterTransition.None
        transition.exit shouldBe NavigationTransitions.Fade.exit
        transition.popEnter shouldBe NavigationTransitions.Fade.popEnter
        transition.popExit shouldBe NavigationTransitions.Fade.popExit
    }

    // =========================================================================
    // customTransition DSL
    // =========================================================================

    test("customTransition creates transition with builder") {
        val transition = customTransition {
            enter = EnterTransition.None
            exit = ExitTransition.None
            popEnter = EnterTransition.None
            popExit = ExitTransition.None
        }

        transition.enter shouldBe EnterTransition.None
        transition.exit shouldBe ExitTransition.None
        transition.popEnter shouldBe EnterTransition.None
        transition.popExit shouldBe ExitTransition.None
    }

    test("customTransition with empty block uses Fade defaults") {
        val transition = customTransition {}

        transition.enter shouldBe NavigationTransitions.Fade.enter
        transition.exit shouldBe NavigationTransitions.Fade.exit
        transition.popEnter shouldBe NavigationTransitions.Fade.popEnter
        transition.popExit shouldBe NavigationTransitions.Fade.popExit
    }

    test("customTransition with partial overrides") {
        val transition = customTransition {
            enter = EnterTransition.None
            popExit = ExitTransition.None
        }

        transition.enter shouldBe EnterTransition.None
        transition.exit shouldBe NavigationTransitions.Fade.exit
        transition.popEnter shouldBe NavigationTransitions.Fade.popEnter
        transition.popExit shouldBe ExitTransition.None
    }

    // =========================================================================
    // SharedElementType enum
    // =========================================================================

    test("SharedElementType has Element and Bounds values") {
        SharedElementType.Element shouldNotBe SharedElementType.Bounds
        SharedElementType.entries.size shouldBe 2
    }

    test("SharedElementType.Element name is correct") {
        SharedElementType.Element.name shouldBe "Element"
    }

    test("SharedElementType.Bounds name is correct") {
        SharedElementType.Bounds.name shouldBe "Bounds"
    }

    // =========================================================================
    // Preset identity checks
    // =========================================================================

    test("preset transitions are stable singleton instances") {
        val none1 = NavigationTransitions.None
        val none2 = NavigationTransitions.None
        none1 shouldBe none2

        val fade1 = NavigationTransitions.Fade
        val fade2 = NavigationTransitions.Fade
        fade1 shouldBe fade2
    }

    test("different preset transitions are distinct") {
        NavigationTransitions.None shouldNotBe NavigationTransitions.Fade
        NavigationTransitions.Fade shouldNotBe NavigationTransitions.SlideHorizontal
        NavigationTransitions.SlideHorizontal shouldNotBe NavigationTransitions.SlideVertical
        NavigationTransitions.SlideVertical shouldNotBe NavigationTransitions.ScaleIn
    }
})
