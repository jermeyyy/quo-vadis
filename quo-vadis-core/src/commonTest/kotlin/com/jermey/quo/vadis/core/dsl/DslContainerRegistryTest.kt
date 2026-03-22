@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.ContainerInfo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

// ─── Test Destinations ───────────────────────────────────────────────

private data object RegHomeDest : NavDestination

private data object RegDetailDest : NavDestination

private data object RegSettingsDest : NavDestination

private sealed interface RegTabs : NavDestination {
    data object Tab1 : RegTabs
    data object Tab2 : RegTabs
}

private sealed interface RegNestedStack : NavDestination {
    data object Root : RegNestedStack
    data object Child : RegNestedStack
}

private data object RegPaneDest : NavDestination

private data object RegPrimaryDest : NavDestination

private data object RegSupportingDest : NavDestination

private data object UnregisteredDest : NavDestination

// ─── Tests ───────────────────────────────────────────────────────────

class DslContainerRegistryTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // ═══════════════════════════════════════════════════════════════════
    // getContainerInfo - null cases
    // ═══════════════════════════════════════════════════════════════════

    test("getContainerInfo returns null for unregistered destination") {
        val config = navigationConfig {}
        config.containerRegistry.getContainerInfo(UnregisteredDest).shouldBeNull()
    }

    test("getContainerInfo returns null for stack container root") {
        val config = navigationConfig {
            stack<RegHomeDest>("home") {
                screen(RegHomeDest)
            }
        }
        // Stacks don't produce ContainerInfo
        config.containerRegistry.getContainerInfo(RegHomeDest).shouldBeNull()
    }

    // ═══════════════════════════════════════════════════════════════════
    // getContainerInfo - Tab container root
    // ═══════════════════════════════════════════════════════════════════

    test("getContainerInfo returns TabContainer for tab container root") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
        tabInfo.scopeKey shouldBe ScopeKey("tabs")
        tabInfo.initialTabIndex shouldBe 0
    }

    // ═══════════════════════════════════════════════════════════════════
    // getContainerInfo - Tab member lookup
    // ═══════════════════════════════════════════════════════════════════

    test("getContainerInfo returns TabContainer for flat screen tab member") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        // Tab2 is a member of the tabs container
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab2)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
        tabInfo.scopeKey shouldBe ScopeKey("tabs")
        tabInfo.initialTabIndex shouldBe 1
    }

    test("getContainerInfo returns TabContainer with correct index for nested stack root") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegNestedStack.Root, "Nested", null) {
                    screen<RegNestedStack.Child>()
                }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegNestedStack.Root)
        info.shouldNotBeNull()
        val tabInfo = info as ContainerInfo.TabContainer
        tabInfo.initialTabIndex shouldBe 1
    }

    test("getContainerInfo returns TabContainer for nested stack child destination") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegNestedStack.Root, "Nested", null) {
                    screen<RegNestedStack.Child>()
                }
            }
        }
        // Child is inside a nested stack within tab at index 1
        val info = config.containerRegistry.getContainerInfo(RegNestedStack.Child)
        info.shouldNotBeNull()
        val tabInfo = info as ContainerInfo.TabContainer
        tabInfo.initialTabIndex shouldBe 1
    }

    test("getContainerInfo returns TabContainer for container reference member") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                containerTab<RegNestedStack>()
            }
            stack<RegNestedStack>("nested") {
                screen(RegNestedStack.Root)
                screen<RegNestedStack.Child>()
            }
        }
        // RegNestedStack is a container reference in tabs
        val info = config.containerRegistry.getContainerInfo(RegNestedStack.Root)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
    }

    test("getContainerInfo returns TabContainer for destination inside referenced container") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                containerTab<RegNestedStack>()
            }
            stack<RegNestedStack>("nested") {
                screen(RegNestedStack.Root)
                screen<RegNestedStack.Child>()
            }
        }
        // Child is inside the referenced stack container
        val info = config.containerRegistry.getContainerInfo(RegNestedStack.Child)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
    }

    // ═══════════════════════════════════════════════════════════════════
    // getContainerInfo - Pane container
    // ═══════════════════════════════════════════════════════════════════

    test("getContainerInfo returns PaneContainer for pane container root") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
                secondary { root(RegSupportingDest) }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegPaneDest)
        info.shouldNotBeNull()
        val paneInfo = info.shouldBeInstanceOf<ContainerInfo.PaneContainer>()
        paneInfo.scopeKey shouldBe ScopeKey("panes")
        paneInfo.initialPane shouldBe PaneRole.Primary
    }

    test("getContainerInfo returns PaneContainer for pane member destination") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
                secondary { root(RegSupportingDest) }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegPrimaryDest)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.PaneContainer>()
    }

    test("getContainerInfo returns PaneContainer for supporting pane member") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
                secondary { root(RegSupportingDest) }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegSupportingDest)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.PaneContainer>()
    }

    // ═══════════════════════════════════════════════════════════════════
    // TabContainer builder function
    // ═══════════════════════════════════════════════════════════════════

    test("TabContainer builder produces correct TabNode") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab1) as ContainerInfo.TabContainer
        val tabNode = info.builder(
            com.jermey.quo.vadis.core.navigation.node.NodeKey("test-key"),
            null,
            0
        )
        tabNode.stacks.size shouldBe 2
        tabNode.activeStackIndex shouldBe 0
    }

    test("TabContainer builder adjusts activeStackIndex when different from requested") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab1) as ContainerInfo.TabContainer
        val tabNode = info.builder(
            com.jermey.quo.vadis.core.navigation.node.NodeKey("test-key"),
            null,
            1
        )
        tabNode.activeStackIndex shouldBe 1
    }

    test("TabContainer builder coerces out-of-range tab index") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab1) as ContainerInfo.TabContainer
        val tabNode = info.builder(
            com.jermey.quo.vadis.core.navigation.node.NodeKey("test-key"),
            null,
            99
        )
        // 99 is coerced to stacks.size - 1 = 1
        tabNode.activeStackIndex shouldBe 1
    }

    // ═══════════════════════════════════════════════════════════════════
    // PaneContainer builder function
    // ═══════════════════════════════════════════════════════════════════

    test("PaneContainer builder produces correct PaneNode") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
                secondary { root(RegSupportingDest) }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegPaneDest) as ContainerInfo.PaneContainer
        val paneNode = info.builder(
            com.jermey.quo.vadis.core.navigation.node.NodeKey("pane-key"),
            null
        )
        paneNode.paneConfigurations.size shouldBe 2
    }

    // ═══════════════════════════════════════════════════════════════════
    // hasTabsContainer / hasPaneContainer
    // ═══════════════════════════════════════════════════════════════════

    test("hasTabsContainer returns true for registered wrapper key") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
            }
            tabsContainer("tabs") { content ->
                content()
            }
        }
        config.containerRegistry.hasTabsContainer("tabs").shouldBeTrue()
    }

    test("hasTabsContainer returns false for unregistered key") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
            }
        }
        config.containerRegistry.hasTabsContainer("unknown").shouldBeFalse()
    }

    test("hasPaneContainer returns true for registered wrapper key") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
            }
            paneContainer("panes") { content ->
                content()
            }
        }
        config.containerRegistry.hasPaneContainer("panes").shouldBeTrue()
    }

    test("hasPaneContainer returns false for unregistered key") {
        val config = navigationConfig {
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
            }
        }
        config.containerRegistry.hasPaneContainer("unknown").shouldBeFalse()
    }

    // ═══════════════════════════════════════════════════════════════════
    // findTabIndex - correct index returned for various entry types
    // ═══════════════════════════════════════════════════════════════════

    test("tab member at index 0 returns TabContainer with initialTabIndex 0") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab1) as ContainerInfo.TabContainer
        info.initialTabIndex shouldBe 0
    }

    test("tab member at index 1 returns TabContainer with initialTabIndex 1") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                tab(RegTabs.Tab2, title = "T2")
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegTabs.Tab2) as ContainerInfo.TabContainer
        info.initialTabIndex shouldBe 1
    }

    // ═══════════════════════════════════════════════════════════════════
    // isDestinationInContainer - recursive container lookup
    // ═══════════════════════════════════════════════════════════════════

    test("destination inside nested tabs is found via container reference") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("outer") {
                containerTab<RegNestedStack>()
            }
            tabs<RegNestedStack>("inner") {
                tab(RegNestedStack.Root, title = "R")
                tab(RegNestedStack.Child, title = "C")
            }
        }
        // Root is a member of inner tabs, which is referenced by outer tabs
        val info = config.containerRegistry.getContainerInfo(RegNestedStack.Root)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
    }

    test("destination inside pane referenced from tabs is found") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                containerTab<RegPaneDest>()
            }
            panes<RegPaneDest>("panes") {
                primary { root(RegPrimaryDest) }
            }
        }
        val info = config.containerRegistry.getContainerInfo(RegPrimaryDest)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Edge case: container reference to unregistered container
    // ═══════════════════════════════════════════════════════════════════

    test("container reference to unregistered type returns fallback tab index") {
        val config = navigationConfig {
            tabs<RegTabs.Tab1>("tabs") {
                tab(RegTabs.Tab1, title = "T1")
                containerTab<UnregisteredDest>()
            }
        }
        // UnregisteredDest itself is mapped in the container reference
        val info = config.containerRegistry.getContainerInfo(UnregisteredDest)
        info.shouldNotBeNull()
        info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
    }
})
