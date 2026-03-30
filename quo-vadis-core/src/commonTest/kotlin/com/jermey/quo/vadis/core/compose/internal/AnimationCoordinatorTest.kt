@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KClass

private object ACTestDest : NavDestination

private object ACCustomTransitionDest : NavDestination {
    override val data: Any? = "custom"
}

class AnimationCoordinatorTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    fun screen(key: String, parentKey: String? = null, dest: NavDestination = ACTestDest) =
        ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, dest)

    fun stack(key: String, parentKey: String? = null, vararg children: ScreenNode) =
        StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, children.toList())

    // =========================================================================
    // Default instance
    // =========================================================================

    test("Default companion instance uses Empty TransitionRegistry") {
        val coordinator = AnimationCoordinator.Default
        // Should not throw and should return default transitions
        val transition = coordinator.getTransition(null, screen("s1"), false)
        transition shouldBe NavTransition.SlideHorizontal
    }

    test("Default companion returns same instance") {
        AnimationCoordinator.Default shouldBe AnimationCoordinator.Default
    }

    // =========================================================================
    // Default transition properties
    // =========================================================================

    test("defaultTransition is SlideHorizontal") {
        val coordinator = AnimationCoordinator()
        coordinator.defaultTransition shouldBe NavTransition.SlideHorizontal
    }

    test("defaultTabTransition is Fade") {
        val coordinator = AnimationCoordinator()
        coordinator.defaultTabTransition shouldBe NavTransition.Fade
    }

    test("defaultPaneTransition is Fade") {
        val coordinator = AnimationCoordinator()
        coordinator.defaultPaneTransition shouldBe NavTransition.Fade
    }

    // =========================================================================
    // getTransition - no registry match (defaults)
    // =========================================================================

    test("getTransition returns defaultTransition when no registry match for forward") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val from = screen("s1")
        val to = screen("s2")

        val result = coordinator.getTransition(from, to, isBack = false)
        result shouldBe NavTransition.SlideHorizontal
    }

    test("getTransition returns defaultTransition when no registry match for back") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val from = screen("s1")
        val to = screen("s2")

        val result = coordinator.getTransition(from, to, isBack = true)
        result shouldBe NavTransition.SlideHorizontal
    }

    test("getTransition with null from returns default for forward") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val to = screen("s1")

        val result = coordinator.getTransition(null, to, isBack = false)
        result shouldBe NavTransition.SlideHorizontal
    }

    test("getTransition with null from returns default for back") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val to = screen("s1")

        val result = coordinator.getTransition(null, to, isBack = true)
        result shouldBe NavTransition.SlideHorizontal
    }

    // =========================================================================
    // getTransition - with registry match
    // =========================================================================

    test("getTransition forward uses to-node destination from registry") {
        val customTransition = NavTransition.SlideVertical
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return if (destinationClass == ACCustomTransitionDest::class) customTransition else null
            }
        }
        val coordinator = AnimationCoordinator(registry)
        val from = screen("s1", dest = ACTestDest)
        val to = screen("s2", dest = ACCustomTransitionDest)

        val result = coordinator.getTransition(from, to, isBack = false)
        result shouldBe customTransition
    }

    test("getTransition back uses from-node destination from registry") {
        val customTransition = NavTransition.SlideVertical
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return if (destinationClass == ACCustomTransitionDest::class) customTransition else null
            }
        }
        val coordinator = AnimationCoordinator(registry)
        val from = screen("s1", dest = ACCustomTransitionDest)
        val to = screen("s2", dest = ACTestDest)

        val result = coordinator.getTransition(from, to, isBack = true)
        result shouldBe customTransition
    }

    test("getTransition forward ignores from-node registry match") {
        val customTransition = NavTransition.SlideVertical
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return if (destinationClass == ACCustomTransitionDest::class) customTransition else null
            }
        }
        val coordinator = AnimationCoordinator(registry)
        val from = screen("s1", dest = ACCustomTransitionDest)
        val to = screen("s2", dest = ACTestDest)

        // Forward: looks up `to` node, which is ACTestDest (not in registry)
        val result = coordinator.getTransition(from, to, isBack = false)
        result shouldBe NavTransition.SlideHorizontal
    }

    // =========================================================================
    // getTransition - non-ScreenNode to-node
    // =========================================================================

    test("getTransition with StackNode as to-node returns default") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val from = screen("s1")
        val toStack = stack("stack1", null, screen("child", "stack1"))

        val result = coordinator.getTransition(from, toStack, isBack = false)
        result shouldBe NavTransition.SlideHorizontal
    }

    // =========================================================================
    // getPaneTransition
    // =========================================================================

    test("getPaneTransition returns default pane transition") {
        val coordinator = AnimationCoordinator()
        val result = coordinator.getPaneTransition(null, PaneRole.Primary)
        result shouldBe NavTransition.Fade
    }

    test("getPaneTransition for Supporting role returns Fade") {
        val coordinator = AnimationCoordinator()
        val result = coordinator.getPaneTransition(PaneRole.Primary, PaneRole.Supporting)
        result shouldBe NavTransition.Fade
    }

    test("getPaneTransition for Extra role returns Fade") {
        val coordinator = AnimationCoordinator()
        val result = coordinator.getPaneTransition(PaneRole.Supporting, PaneRole.Extra)
        result shouldBe NavTransition.Fade
    }

    // =========================================================================
    // getTransition - override priority
    // =========================================================================

    test("getTransition returns transitionOverride when provided, ignoring registry") {
        val registryTransition = NavTransition.SlideVertical
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return if (destinationClass == ACCustomTransitionDest::class) registryTransition else null
            }
        }
        val coordinator = AnimationCoordinator(registry)
        val from = screen("s1", dest = ACTestDest)
        val to = screen("s2", dest = ACCustomTransitionDest)

        val overrideTransition = NavTransition.Fade
        val result = coordinator.getTransition(from, to, isBack = false, transitionOverride = overrideTransition)
        result shouldBe overrideTransition
    }

    test("getTransition falls through to registry when override is null") {
        val registryTransition = NavTransition.SlideVertical
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? {
                return if (destinationClass == ACCustomTransitionDest::class) registryTransition else null
            }
        }
        val coordinator = AnimationCoordinator(registry)
        val from = screen("s1", dest = ACTestDest)
        val to = screen("s2", dest = ACCustomTransitionDest)

        val result = coordinator.getTransition(from, to, isBack = false, transitionOverride = null)
        result shouldBe registryTransition
    }

    test("getTransition falls through to default when both override and registry are null") {
        val coordinator = AnimationCoordinator(TransitionRegistry.Empty)
        val from = screen("s1")
        val to = screen("s2")

        val result = coordinator.getTransition(from, to, isBack = false)
        result shouldBe NavTransition.SlideHorizontal
    }
})
