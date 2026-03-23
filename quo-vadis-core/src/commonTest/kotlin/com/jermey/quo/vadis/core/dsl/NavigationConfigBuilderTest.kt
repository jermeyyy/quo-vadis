@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data object HomeDestination : NavDestination

private data object DetailDestination : NavDestination

private data object SettingsDestination : NavDestination

private data object ModalDestination : NavDestination

private sealed interface MainTabs : NavDestination {
    data object Tab1 : MainTabs

    data object Tab2 : MainTabs
}

private data object CfgPanesDest : NavDestination

private data object CfgPrimaryDest : NavDestination

private data object CfgSecondaryDest : NavDestination

class NavigationConfigBuilderTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    test("screen registration adds to screenRegistry") {
        val config = navigationConfig {
            screen<HomeDestination> { _, _, _ ->
                {}
            }
        }

        config.screenRegistry.hasContent(HomeDestination).shouldBeTrue()
    }

    test("screen registry returns false for unregistered destination") {
        val config = navigationConfig {
            screen<HomeDestination> { _, _, _ -> {} }
        }

        config.screenRegistry.hasContent(DetailDestination).shouldBeFalse()
    }

    test("stack registration adds to containerRegistry") {
        val config = navigationConfig {
            stack<HomeDestination>("home-stack") {
                screen(HomeDestination)
            }
        }

        val node = config.buildNavNode(HomeDestination::class)
        node.shouldNotBeNull()
    }

    test("tabs registration adds to containerRegistry") {
        val config = navigationConfig {
            tabs<MainTabs.Tab1>("main-tabs") {
                initialTab = 0
                tab(MainTabs.Tab1, title = "Tab 1")
                tab(MainTabs.Tab2, title = "Tab 2")
            }
        }

        val node = config.buildNavNode(MainTabs.Tab1::class)
        node.shouldNotBeNull()
    }

    test("panes registration adds to containerRegistry") {
        val config = navigationConfig {
            panes<CfgPanesDest>("panes-scope") {
                primary {
                    root(CfgPrimaryDest)
                }
                secondary {
                    root(CfgSecondaryDest)
                }
            }
        }

        val node = config.buildNavNode(CfgPanesDest::class)
        node.shouldNotBeNull()
    }

    test("scope registration populates scopeRegistry") {
        val config = navigationConfig {
            scope("my-scope") {
                +HomeDestination::class
                +DetailDestination::class
            }
        }

        config.scopeRegistry.isInScope(ScopeKey("my-scope"), HomeDestination).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("my-scope"), DetailDestination).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("my-scope"), SettingsDestination).shouldBeFalse()
    }

    test("transition registration populates transitionRegistry") {
        val config = navigationConfig {
            transition<HomeDestination>(NavTransition.SlideHorizontal)
        }

        config.transitionRegistry.getTransition(HomeDestination::class) shouldBe NavTransition.SlideHorizontal
    }

    test("transition registry returns null for unregistered destination") {
        val config = navigationConfig {
            transition<HomeDestination>(NavTransition.Fade)
        }

        config.transitionRegistry.getTransition(DetailDestination::class).shouldBeNull()
    }

    test("modal registration populates modalRegistry") {
        val config = navigationConfig {
            modal<ModalDestination>()
        }

        config.modalRegistry.isModalDestination(ModalDestination::class).shouldBeTrue()
        config.modalRegistry.isModalDestination(HomeDestination::class).shouldBeFalse()
    }

    test("modalContainer registration populates modalRegistry") {
        val config = navigationConfig {
            modalContainer("bottom-sheet")
        }

        config.modalRegistry.isModalContainer("bottom-sheet").shouldBeTrue()
        config.modalRegistry.isModalContainer("other-key").shouldBeFalse()
    }

    test("stack auto-registers scope membership for its screens") {
        val config = navigationConfig {
            stack<HomeDestination>("home-stack") {
                screen<DetailDestination>()
                screen<SettingsDestination>()
            }
        }

        config.scopeRegistry.isInScope(ScopeKey("home-stack"), DetailDestination).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("home-stack"), SettingsDestination).shouldBeTrue()
    }

    test("tabs auto-registers scope membership for flat screen tabs") {
        val config = navigationConfig {
            tabs<MainTabs.Tab1>("main-tabs") {
                tab(MainTabs.Tab1, title = "T1")
                tab(MainTabs.Tab2, title = "T2")
            }
        }

        config.scopeRegistry.isInScope(ScopeKey("main-tabs"), MainTabs.Tab1).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("main-tabs"), MainTabs.Tab2).shouldBeTrue()
    }

    test("build produces a valid NavigationConfig") {
        val config = navigationConfig {
            screen<HomeDestination> { _, _, _ -> {} }
            stack<DetailDestination>("detail-stack") {
                screen<HomeDestination>()
            }
            scope("extra-scope") {
                +SettingsDestination::class
            }
            transition<HomeDestination>(NavTransition.Fade)
            modal<ModalDestination>()
        }

        config.shouldBeInstanceOf<NavigationConfig>()
        config.screenRegistry.hasContent(HomeDestination).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("extra-scope"), SettingsDestination).shouldBeTrue()
        config.transitionRegistry.getTransition(HomeDestination::class) shouldBe NavTransition.Fade
        config.modalRegistry.isModalDestination(ModalDestination::class).shouldBeTrue()
    }

    test("stack with default scopeKey uses class simpleName") {
        val config = navigationConfig {
            stack<HomeDestination> {
                screen<DetailDestination>()
            }
        }

        // Default scopeKey is the class simpleName
        config.scopeRegistry.isInScope(
            ScopeKey(HomeDestination::class.simpleName!!),
            DetailDestination
        ).shouldBeTrue()
    }

    test("buildNavNode returns null for unregistered destination") {
        val config = navigationConfig {}
        config.buildNavNode(HomeDestination::class).shouldBeNull()
    }

    test("empty config has no registrations") {
        val config = navigationConfig {}
        config.screenRegistry.hasContent(HomeDestination).shouldBeFalse()
        config.buildNavNode(HomeDestination::class).shouldBeNull()
    }
})

class ScopeBuilderTest : FunSpec({

    test("unaryPlus adds destination class to members") {
        val builder = ScopeBuilder()
        with(builder) {
            +HomeDestination::class
            +DetailDestination::class
        }

        builder.members shouldContain HomeDestination::class
        builder.members shouldContain DetailDestination::class
    }

    test("include adds destination class via reified type") {
        val builder = ScopeBuilder()
        builder.include<HomeDestination>()

        builder.members shouldContain HomeDestination::class
    }

    test("addMember adds class directly") {
        val builder = ScopeBuilder()
        builder.addMember(SettingsDestination::class)

        builder.members shouldContain SettingsDestination::class
    }

    test("duplicate adds are idempotent") {
        val builder = ScopeBuilder()
        builder.addMember(HomeDestination::class)
        builder.addMember(HomeDestination::class)

        builder.members.size shouldBe 1
    }
})
