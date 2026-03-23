@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

// ─── Test Destinations ───────────────────────────────────────────────

private data object CnfHomeDest : NavDestination

private data object CnfDetailDest : NavDestination

private data object CnfSettingsDest : NavDestination

private data object CnfModalDest : NavDestination

private sealed interface CnfTabs : NavDestination {
    data object Tab1 : CnfTabs
    data object Tab2 : CnfTabs
}

private sealed interface CnfNestedStack : NavDestination {
    data object Root : CnfNestedStack
    data object Child : CnfNestedStack
}

private data object CnfPaneDest : NavDestination

private data object CnfPrimaryDest : NavDestination

private data object CnfSecondaryDest : NavDestination

private data object CnfUnknownDest : NavDestination

// ─── Tests ───────────────────────────────────────────────────────────

class DslNavigationConfigTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // ═══════════════════════════════════════════════════════════════════
    // Registry lazy initialization
    // ═══════════════════════════════════════════════════════════════════

    test("screenRegistry contains registered screens") {
        val config = navigationConfig {
            screen<CnfHomeDest> { _, _, _ -> {} }
        }
        config.screenRegistry.hasContent(CnfHomeDest).shouldBeTrue()
    }

    test("screenRegistry excludes unregistered destinations") {
        val config = navigationConfig {
            screen<CnfHomeDest> { _, _, _ -> {} }
        }
        config.screenRegistry.hasContent(CnfDetailDest).shouldBeFalse()
    }

    test("transitionRegistry returns registered transition") {
        val config = navigationConfig {
            transition<CnfHomeDest>(NavTransition.SlideHorizontal)
        }
        config.transitionRegistry.getTransition(CnfHomeDest::class) shouldBe NavTransition.SlideHorizontal
    }

    test("transitionRegistry returns null for unregistered destination") {
        val config = navigationConfig {
            transition<CnfHomeDest>(NavTransition.Fade)
        }
        config.transitionRegistry.getTransition(CnfDetailDest::class).shouldBeNull()
    }

    test("modalRegistry detects registered modal destinations") {
        val config = navigationConfig {
            modal<CnfModalDest>()
        }
        config.modalRegistry.isModalDestination(CnfModalDest::class).shouldBeTrue()
        config.modalRegistry.isModalDestination(CnfHomeDest::class).shouldBeFalse()
    }

    test("modalRegistry detects registered modal containers") {
        val config = navigationConfig {
            modalContainer("sheet-key")
        }
        config.modalRegistry.isModalContainer("sheet-key").shouldBeTrue()
        config.modalRegistry.isModalContainer("other").shouldBeFalse()
    }

    test("deepLinkRegistry is empty for DSL configs") {
        val config = navigationConfig {}
        config.deepLinkRegistry shouldBe DeepLinkRegistry.Companion.Empty
    }

    // ═══════════════════════════════════════════════════════════════════
    // buildNavNode - Stack
    // ═══════════════════════════════════════════════════════════════════

    test("buildNavNode returns null for unregistered destination") {
        val config = navigationConfig {}
        config.buildNavNode(CnfHomeDest::class).shouldBeNull()
    }

    test("buildNavNode builds StackNode with root screen child") {
        val config = navigationConfig {
            stack<CnfHomeDest>("home") {
                screen(CnfHomeDest)
            }
        }
        val node = config.buildNavNode(CnfHomeDest::class)
        node.shouldNotBeNull()
        val stack = node.shouldBeInstanceOf<StackNode>()
        stack.key.value shouldBe "home"
        stack.scopeKey shouldBe ScopeKey("home")
        stack.children shouldHaveSize 1
        val child = stack.children[0].shouldBeInstanceOf<ScreenNode>()
        child.destination shouldBe CnfHomeDest
        child.key.value shouldBe "home/root"
    }

    test("buildNavNode builds StackNode with parentKey when provided") {
        val config = navigationConfig {
            stack<CnfHomeDest>("home") {
                screen(CnfHomeDest)
            }
        }
        val node = config.buildNavNode(CnfHomeDest::class, "home", "parent-key")
        node.shouldNotBeNull()
        val stack = node as StackNode
        stack.parentKey!!.value shouldBe "parent-key"
    }

    test("buildNavNode builds empty StackNode for stack without destination instances") {
        val config = navigationConfig {
            stack<CnfHomeDest>("home") {
                screen<CnfDetailDest>()
            }
        }
        val node = config.buildNavNode(CnfHomeDest::class)
        node.shouldNotBeNull()
        val stack = node as StackNode
        stack.children shouldHaveSize 0
    }

    test("buildNavNode builds StackNode with empty children for empty stack") {
        val config = navigationConfig {
            stack<CnfHomeDest>("home") {}
        }
        val node = config.buildNavNode(CnfHomeDest::class)
        node.shouldNotBeNull()
        val stack = node as StackNode
        stack.children shouldHaveSize 0
    }

    test("buildNavNode uses scopeKey as default key") {
        val config = navigationConfig {
            stack<CnfHomeDest>("my-scope") {
                screen(CnfHomeDest)
            }
        }
        val node = config.buildNavNode(CnfHomeDest::class)
        node.shouldNotBeNull()
        val stack = node as StackNode
        stack.key.value shouldBe "my-scope"
    }

    test("buildNavNode uses explicit key when provided") {
        val config = navigationConfig {
            stack<CnfHomeDest>("my-scope") {
                screen(CnfHomeDest)
            }
        }
        val node = config.buildNavNode(CnfHomeDest::class, "explicit-key", null)
        node.shouldNotBeNull()
        val stack = node as StackNode
        stack.key.value shouldBe "explicit-key"
    }

    // ═══════════════════════════════════════════════════════════════════
    // buildNavNode - Tabs
    // ═══════════════════════════════════════════════════════════════════

    test("buildNavNode builds TabNode with flat screen tabs") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "Tab 1")
                tab(CnfTabs.Tab2, title = "Tab 2")
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class)
        node.shouldNotBeNull()
        val tabNode = node.shouldBeInstanceOf<TabNode>()
        tabNode.stacks shouldHaveSize 2
        tabNode.wrapperKey shouldBe "tabs"
        tabNode.activeStackIndex shouldBe 0

        tabNode.stacks.forEach { stack ->
            stack.children shouldHaveSize 1
            stack.children[0].shouldBeInstanceOf<ScreenNode>()
        }
    }

    test("buildNavNode builds TabNode with nested stack tab") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfNestedStack.Root, "Nested", null) {
                    screen<CnfNestedStack.Child>()
                }
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class)
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.stacks shouldHaveSize 1
        val tabStack = tabNode.stacks[0]
        tabStack.children shouldHaveSize 1
        val rootScreen = tabStack.children[0].shouldBeInstanceOf<ScreenNode>()
        rootScreen.destination shouldBe CnfNestedStack.Root
    }

    test("buildNavNode builds TabNode with initialTab") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                initialTab = 1
                tab(CnfTabs.Tab1, title = "Tab 1")
                tab(CnfTabs.Tab2, title = "Tab 2")
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class)
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.activeStackIndex shouldBe 1
    }

    test("buildNavNode builds TabNode with container reference to local stack") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "Tab 1")
                containerTab<CnfNestedStack>()
            }
            stack<CnfNestedStack>("nested") {
                screen(CnfNestedStack.Root)
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class)
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.stacks shouldHaveSize 2
        val tab1Stack = tabNode.stacks[1]
        tab1Stack.children shouldHaveSize 1
        tab1Stack.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    test("buildNavNode sets tabMetadata routes from tab entries") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "Tab 1")
                tab(CnfTabs.Tab2, title = "Tab 2")
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class) as TabNode
        node.tabMetadata shouldHaveSize 2
        node.tabMetadata[0].route shouldBe "Tab1"
        node.tabMetadata[1].route shouldBe "Tab2"
    }

    // ═══════════════════════════════════════════════════════════════════
    // buildNavNode - Panes
    // ═══════════════════════════════════════════════════════════════════

    test("buildNavNode builds PaneNode with pane configurations") {
        val config = navigationConfig {
            panes<CnfPaneDest>("panes") {
                primary { root(CnfPrimaryDest) }
                secondary { root(CnfSecondaryDest) }
            }
        }
        val node = config.buildNavNode(CnfPaneDest::class)
        node.shouldNotBeNull()
        val paneNode = node.shouldBeInstanceOf<PaneNode>()
        paneNode.activePaneRole shouldBe PaneRole.Primary
        paneNode.paneConfigurations.keys shouldBe setOf(PaneRole.Primary, PaneRole.Supporting)

        val primaryConfig = paneNode.paneConfigurations[PaneRole.Primary]!!
        val primaryStack = primaryConfig.content.shouldBeInstanceOf<StackNode>()
        primaryStack.children shouldHaveSize 1
        (primaryStack.children[0] as ScreenNode).destination shouldBe CnfPrimaryDest

        val supportingConfig = paneNode.paneConfigurations[PaneRole.Supporting]!!
        val supportingStack = supportingConfig.content.shouldBeInstanceOf<StackNode>()
        supportingStack.children shouldHaveSize 1
        (supportingStack.children[0] as ScreenNode).destination shouldBe CnfSecondaryDest
    }

    test("buildNavNode PaneNode uses alwaysVisible adapt strategy") {
        val config = navigationConfig {
            panes<CnfPaneDest>("panes") {
                primary {
                    root(CnfPrimaryDest)
                    alwaysVisible()
                }
                secondary { root(CnfSecondaryDest) }
            }
        }
        val node = config.buildNavNode(CnfPaneDest::class) as PaneNode
        node.paneConfigurations[PaneRole.Primary]!!.adaptStrategy shouldBe AdaptStrategy.Levitate
        node.paneConfigurations[PaneRole.Supporting]!!.adaptStrategy shouldBe AdaptStrategy.Hide
    }

    test("buildNavNode PaneNode builds empty stack when no root destination") {
        val config = navigationConfig {
            panes<CnfPaneDest>("panes") {
                primary {}
            }
        }
        val node = config.buildNavNode(CnfPaneDest::class) as PaneNode
        val primaryStack = node.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        primaryStack.children shouldHaveSize 0
    }

    test("buildNavNode PaneNode uses configured initialPane") {
        val config = navigationConfig {
            panes<CnfPaneDest>("panes") {
                initialPane = PaneRole.Supporting
                primary { root(CnfPrimaryDest) }
                secondary { root(CnfSecondaryDest) }
            }
        }
        val node = config.buildNavNode(CnfPaneDest::class) as PaneNode
        node.activePaneRole shouldBe PaneRole.Supporting
    }

    // ═══════════════════════════════════════════════════════════════════
    // Scope Registry - combined scopes
    // ═══════════════════════════════════════════════════════════════════

    test("scopeRegistry includes explicit scope members") {
        val config = navigationConfig {
            scope("my-scope") {
                +CnfHomeDest::class
                +CnfDetailDest::class
            }
        }
        config.scopeRegistry.isInScope(ScopeKey("my-scope"), CnfHomeDest).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("my-scope"), CnfDetailDest).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("my-scope"), CnfSettingsDest).shouldBeFalse()
    }

    test("scopeRegistry infers stack screen membership") {
        val config = navigationConfig {
            stack<CnfHomeDest>("home") {
                screen<CnfDetailDest>()
                screen<CnfSettingsDest>()
            }
        }
        config.scopeRegistry.isInScope(ScopeKey("home"), CnfDetailDest).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("home"), CnfSettingsDest).shouldBeTrue()
    }

    test("scopeRegistry infers flat tab membership") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "T1")
                tab(CnfTabs.Tab2, title = "T2")
            }
        }
        config.scopeRegistry.isInScope(ScopeKey("tabs"), CnfTabs.Tab1).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("tabs"), CnfTabs.Tab2).shouldBeTrue()
    }

    test("scopeRegistry infers nested stack tab membership for root and children") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfNestedStack.Root, "N", null) {
                    screen<CnfNestedStack.Child>()
                }
            }
        }
        config.scopeRegistry.isInScope(ScopeKey("tabs"), CnfNestedStack.Root).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("tabs"), CnfNestedStack.Child).shouldBeTrue()
    }

    test("scopeRegistry infers container reference tab membership adds container class") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "T1")
                containerTab<CnfNestedStack>()
            }
            stack<CnfNestedStack>("nested") {
                screen(CnfNestedStack.Root)
                screen<CnfNestedStack.Child>()
            }
        }
        // The stack scope "nested" infers membership for its screens
        config.scopeRegistry.isInScope(ScopeKey("nested"), CnfNestedStack.Root).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("nested"), CnfNestedStack.Child).shouldBeTrue()
    }

    test("scopeRegistry infers pane membership from root destinations") {
        val config = navigationConfig {
            panes<CnfPaneDest>("panes") {
                primary { root(CnfPrimaryDest) }
                secondary { root(CnfSecondaryDest) }
            }
        }
        config.scopeRegistry.isInScope(ScopeKey("panes"), CnfPrimaryDest).shouldBeTrue()
        config.scopeRegistry.isInScope(ScopeKey("panes"), CnfSecondaryDest).shouldBeTrue()
    }

    // ═══════════════════════════════════════════════════════════════════
    // plus() combiner
    // ═══════════════════════════════════════════════════════════════════

    test("plus with EmptyNavigationConfig returns this") {
        val config = navigationConfig {
            screen<CnfHomeDest> { _, _, _ -> {} }
        }
        val result = config + NavigationConfig.Companion.Empty
        result shouldBe config
    }

    test("plus with another config creates composite") {
        val config1 = navigationConfig {
            screen<CnfHomeDest> { _, _, _ -> {} }
        }
        val config2 = navigationConfig {
            screen<CnfDetailDest> { _, _, _ -> {} }
        }
        val composite = config1 + config2
        composite.shouldNotBeSameInstanceAs(config1)
        composite.shouldNotBeSameInstanceAs(config2)
        composite.screenRegistry.hasContent(CnfHomeDest).shouldBeTrue()
        composite.screenRegistry.hasContent(CnfDetailDest).shouldBeTrue()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Container reference with nodeResolver (cross-module)
    // ═══════════════════════════════════════════════════════════════════

    test("container reference uses nodeResolver when local lookup fails") {
        val configA = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                tab(CnfTabs.Tab1, title = "T1")
                containerTab<CnfNestedStack>()
            }
        }
        val configB = navigationConfig {
            stack<CnfNestedStack>("nested") {
                screen(CnfNestedStack.Root)
            }
        }
        val composite = configA + configB

        val node = composite.buildNavNode(CnfTabs.Tab1::class, "root", null)
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.stacks shouldHaveSize 2
        val resolvedStack = tabNode.stacks[1]
        resolvedStack.children shouldHaveSize 1
        resolvedStack.children[0].shouldBeInstanceOf<ScreenNode>()
    }

    test("container reference builds TabNode inside tab for nested tabs") {
        val configA = navigationConfig {
            tabs<CnfTabs.Tab1>("outer") {
                containerTab<CnfNestedStack>()
            }
            tabs<CnfNestedStack>("inner") {
                tab(CnfNestedStack.Root, title = "R")
                tab(CnfNestedStack.Child, title = "C")
            }
        }
        val node = configA.buildNavNode(CnfTabs.Tab1::class, "outer", null)
        node.shouldNotBeNull()
        val outerTab = node as TabNode
        outerTab.stacks shouldHaveSize 1
        val wrapperStack = outerTab.stacks[0]
        wrapperStack.children shouldHaveSize 1
        wrapperStack.children[0].shouldBeInstanceOf<TabNode>()
    }

    test("unresolved container reference produces empty tab stack") {
        val config = navigationConfig {
            tabs<CnfTabs.Tab1>("tabs") {
                containerTab<CnfUnknownDest>()
            }
        }
        val node = config.buildNavNode(CnfTabs.Tab1::class)
        node.shouldNotBeNull()
        val tabNode = node as TabNode
        tabNode.stacks shouldHaveSize 1
        tabNode.stacks[0].children shouldHaveSize 0
    }
})
