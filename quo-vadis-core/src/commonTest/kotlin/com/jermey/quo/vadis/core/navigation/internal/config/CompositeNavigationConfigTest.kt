@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.dsl.navigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.GeneratedTabMetadata
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

// ── Test destinations ──────────────────────────────────────────────

private sealed interface CompDest : NavDestination {
    data object ScreenA : CompDest
    data object ScreenB : CompDest
}

private sealed interface CompTabs : NavDestination {
    data object Tab1 : CompTabs
    data object Tab2 : CompTabs
}

private sealed interface OtherTabs : NavDestination {
    data object TabX : OtherTabs
}

private sealed interface CompStack : NavDestination {
    data object Home : CompStack
}

class CompositeNavigationConfigTest : FunSpec({

    // ── Registry priority: secondary wins ───────────────────────────

    test("transitionRegistry prefers secondary over primary") {
        val primaryTransition = NavTransition.SlideHorizontal
        val secondaryTransition = NavTransition.Fade

        val primary = navigationConfig {
            transition<CompDest.ScreenA>(primaryTransition)
        }
        val secondary = navigationConfig {
            transition<CompDest.ScreenA>(secondaryTransition)
        }
        val composite = primary + secondary

        composite.transitionRegistry.getTransition(CompDest.ScreenA::class) shouldBe secondaryTransition
    }

    test("transitionRegistry falls back to primary when secondary returns null") {
        val primaryTransition = NavTransition.SlideHorizontal

        val primary = navigationConfig {
            transition<CompDest.ScreenA>(primaryTransition)
        }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.transitionRegistry.getTransition(CompDest.ScreenA::class) shouldBe primaryTransition
    }

    test("transitionRegistry returns null when neither has a match") {
        val primary = navigationConfig { }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.transitionRegistry.getTransition(CompDest.ScreenA::class).shouldBeNull()
    }

    // ── scopeRegistry priority ──────────────────────────────────────

    test("scopeRegistry prefers secondary over primary") {
        val primary = navigationConfig {
            stack<CompStack>("comp-scope") {
                screen(CompStack.Home)
            }
        }
        val secondary = navigationConfig {
            stack<CompStack>("comp-scope-override") {
                screen(CompStack.Home)
            }
        }
        val composite = primary + secondary

        // Secondary config registered CompStack.Home under "comp-scope-override"
        val scopeKey = composite.scopeRegistry.getScopeKey(CompStack.Home)
        scopeKey.shouldNotBeNull()
        scopeKey shouldBe ScopeKey("comp-scope-override")
    }

    // ── modalRegistry priority ──────────────────────────────────────

    test("modalRegistry finds modal from secondary") {
        val primary = navigationConfig { }
        val secondary = navigationConfig {
            modal<CompDest.ScreenA>()
        }
        val composite = primary + secondary

        composite.modalRegistry.isModalDestination(CompDest.ScreenA::class).shouldBeTrue()
    }

    test("modalRegistry finds modal from primary as fallback") {
        val primary = navigationConfig {
            modal<CompDest.ScreenA>()
        }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.modalRegistry.isModalDestination(CompDest.ScreenA::class).shouldBeTrue()
    }

    test("modalRegistry returns false when neither has modal") {
        val primary = navigationConfig { }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.modalRegistry.isModalDestination(CompDest.ScreenA::class).shouldBeFalse()
    }

    // ── buildNavNode ────────────────────────────────────────────────

    test("buildNavNode returns secondary node when available") {
        val primary = navigationConfig { }
        val secondary = navigationConfig {
            stack<CompStack> {
                screen(CompStack.Home)
            }
        }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompStack::class, "root", null)
        node.shouldNotBeNull()
        node.shouldBeInstanceOf<StackNode>()
    }

    test("buildNavNode falls back to primary when secondary returns null") {
        val primary = navigationConfig {
            stack<CompStack> {
                screen(CompStack.Home)
            }
        }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompStack::class, "root", null)
        node.shouldNotBeNull()
        node.shouldBeInstanceOf<StackNode>()
    }

    test("buildNavNode returns null when neither config has node") {
        val primary = navigationConfig { }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.buildNavNode(CompDest.ScreenA::class).shouldBeNull()
    }

    // ── mergeTabNodes ───────────────────────────────────────────────

    test("mergeTabNodes combines tabs from multiple configs") {
        val primary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab1)
            }
        }
        val secondary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab2)
            }
        }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompTabs::class, "tabs", null)
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        // Primary had 1 tab, secondary had 1 tab with different route -> merged to 2
        tabNode.stacks.size shouldBe 2
    }

    test("mergeTabNodes deduplicates tabs with same route") {
        val primary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab1)
                tab(CompTabs.Tab2)
            }
        }
        val secondary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab1)
            }
        }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompTabs::class, "tabs", null)
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        // Tab1 exists in both, so should not be duplicated
        tabNode.stacks.size shouldBe 2
    }

    // ── rekeyStack / rekeySubtree ───────────────────────────────────

    test("merged tabs have correctly rekeyed stacks") {
        val primary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab1)
            }
        }
        val secondary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab2)
            }
        }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompTabs::class, "tabs", null)!!
        val tabNode = node as TabNode

        // The second stack should have a rekeyed key continuing after primary
        val firstKey = tabNode.stacks[0].key.value
        val secondKey = tabNode.stacks[1].key.value
        // Keys should be different
        firstKey shouldBe firstKey // sanity
        firstKey shouldNotBe secondKey
    }

    test("rekeyed stacks have children with updated parentKey prefix") {
        val primary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab1)
            }
        }
        val secondary = navigationConfig {
            tabs<CompTabs> {
                tab(CompTabs.Tab2)
            }
        }
        val composite = primary + secondary

        val node = composite.buildNavNode(CompTabs::class, "tabs", null)!!
        val tabNode = node as TabNode

        // Second stack's children should have parentKey referencing the new stack key
        val secondStack = tabNode.stacks[1]
        for (child in secondStack.children) {
            child.parentKey?.value shouldBe secondStack.key.value
        }
    }

    // ── plus operator ───────────────────────────────────────────────

    test("plus creates CompositeNavigationConfig") {
        val a = navigationConfig { }
        val b = navigationConfig { }
        val result = a + b

        result.shouldBeInstanceOf<CompositeNavigationConfig>()
    }

    test("plus with Empty returns this unchanged") {
        val config = navigationConfig {
            stack<CompStack> {
                screen(CompStack.Home)
            }
        }
        val result = config + NavigationConfig.Empty
        result shouldBeSameInstanceAs config

        // DslNavigationConfig.plus delegates to CompositeNavigationConfig creation,
        // but CompositeNavigationConfig.plus optimizes Empty away
        val composite = config + navigationConfig { }
        val result2 = composite + NavigationConfig.Empty
        result2 shouldBeSameInstanceAs composite
    }

    test("chained plus preserves priority order") {
        val transA = NavTransition.SlideHorizontal
        val transB = NavTransition.Fade
        val transC = NavTransition.SlideVertical

        val configA = navigationConfig { transition<CompDest.ScreenA>(transA) }
        val configB = navigationConfig { transition<CompDest.ScreenA>(transB) }
        val configC = navigationConfig { transition<CompDest.ScreenA>(transC) }

        // (A + B) + C — C should win
        val composite = (configA + configB) + configC
        composite.transitionRegistry.getTransition(CompDest.ScreenA::class) shouldBe transC
    }

    // ── deepLinkRegistry composition ────────────────────────────────

    test("deepLinkRegistry getRegisteredPatterns combines both") {
        val primary = navigationConfig { }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        // Both empty, so combined should be empty
        composite.deepLinkRegistry.getRegisteredPatterns() shouldBe emptyList()
    }

    // ── paneRoleRegistry composition ────────────────────────────────

    test("paneRoleRegistry returns null when neither has role") {
        val primary = navigationConfig { }
        val secondary = navigationConfig { }
        val composite = primary + secondary

        composite.paneRoleRegistry.getPaneRole(ScopeKey("scope"), CompDest.ScreenA).shouldBeNull()
    }
})
