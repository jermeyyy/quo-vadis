@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.registry.internal.CompositeContainerRegistry
import com.jermey.quo.vadis.core.registry.ContainerInfo
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import kotlin.reflect.KClass
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for [com.jermey.quo.vadis.core.dsl.registry.CompositeContainerRegistry].
 *
 * Tests cover:
 * - Secondary registry priority over primary for container info
 * - Wrapped builders correctly use the composite's navNodeBuilder
 * - TabsContainer/PaneContainer delegate to the correct registry
 * - hasTabsContainer/hasPaneContainer correctly check both registries
 */
private sealed interface PrimaryTabs : NavDestination {
    data object Tab1 : PrimaryTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    data object Tab2 : PrimaryTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}

private sealed interface SecondaryTabs : NavDestination {
    data object Tab1 : SecondaryTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    data object Tab2 : SecondaryTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}

private sealed interface SharedTabs : NavDestination {
    data object Tab1 : SharedTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    data object Tab2 : SharedTabs {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}

private sealed interface PrimaryPane : NavDestination {
    data object Pane1 : PrimaryPane {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}

private sealed interface SecondaryPane : NavDestination {
    data object Pane1 : SecondaryPane {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
}

private data object UnknownDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class CompositeContainerRegistryTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================







    // =========================================================================
    // HELPER BUILDERS
    // =========================================================================

    fun createTabNodeBuilder(
        containerClass: KClass<out NavDestination>,
        scopeKey: ScopeKey,
        builderTracker: MutableList<String>? = null
    ): (NodeKey, NodeKey?, Int) -> TabNode = { key, parentKey, initialTabIndex ->
        builderTracker?.add("builder-called:$key")
        TabNode(
            key = key,
            parentKey = parentKey,
            stacks = listOf(
                StackNode(
                    NodeKey("$key-tab0"),
                    key,
                    listOf(ScreenNode(NodeKey("$key-screen0"), NodeKey("$key-tab0"), PrimaryTabs.Tab1))
                ),
                StackNode(
                    NodeKey("$key-tab1"),
                    key,
                    listOf(ScreenNode(NodeKey("$key-screen1"), NodeKey("$key-tab1"), PrimaryTabs.Tab2))
                )
            ),
            activeStackIndex = initialTabIndex.coerceIn(0, 1),
            scopeKey = scopeKey
        )
    }

    fun createPaneNodeBuilder(
        containerClass: KClass<out NavDestination>,
        scopeKey: ScopeKey,
        builderTracker: MutableList<String>? = null
    ): (NodeKey, NodeKey?) -> PaneNode = { key, parentKey ->
        builderTracker?.add("builder-called:$key")
        PaneNode(
            key = key,
            parentKey = parentKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("$key-primary"), key, PrimaryPane.Pane1)
                )
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = scopeKey
        )
    }

    fun createTestContainerRegistry(
        tabDestinations: Map<NavDestination, ContainerInfo.TabContainer>,
        paneDestinations: Map<NavDestination, ContainerInfo.PaneContainer>,
        tabsContainerKeys: Set<String>,
        paneContainerKeys: Set<String>,
        wrapperTracker: MutableList<String>
    ): ContainerRegistry = object : ContainerRegistry {
        override fun getContainerInfo(destination: NavDestination): ContainerInfo? {
            return tabDestinations[destination] ?: paneDestinations[destination]
        }

        @Composable
        override fun TabsContainer(
            tabNodeKey: String,
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            wrapperTracker.add("TabsContainer:$tabNodeKey")
            content()
        }

        @Composable
        override fun PaneContainer(
            paneNodeKey: String,
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            wrapperTracker.add("PaneContainer:$paneNodeKey")
            content()
        }

        override fun hasTabsContainer(tabNodeKey: String): Boolean = tabNodeKey in tabsContainerKeys
        override fun hasPaneContainer(paneNodeKey: String): Boolean =
            paneNodeKey in paneContainerKeys
    }

    // =========================================================================
    // PRIORITY TESTS - getContainerInfo
    // =========================================================================

    test("getContainerInfo returns secondary info when secondary has destination") {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, ScopeKey("PrimaryTabs"), primaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimaryTabs"),
            containerClass = PrimaryTabs::class
        )

        val secondaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(SecondaryTabs::class, ScopeKey("SecondaryTabs"), secondaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("SecondaryTabs"),
            containerClass = SecondaryTabs::class
        )

        val primaryWrapperTracker = mutableListOf<String>()
        val secondaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            mapOf(PrimaryTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            mapOf(SecondaryTabs.Tab1 to secondaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            secondaryWrapperTracker
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(SecondaryTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
        tabInfo.scopeKey shouldBe ScopeKey("SecondaryTabs")
    }

    test("getContainerInfo returns primary info when only primary has destination") {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, ScopeKey("PrimaryTabs"), primaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimaryTabs"),
            containerClass = PrimaryTabs::class
        )

        val primaryWrapperTracker = mutableListOf<String>()
        val secondaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            mapOf(PrimaryTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            secondaryWrapperTracker
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
        tabInfo.scopeKey shouldBe ScopeKey("PrimaryTabs")
    }

    test("getContainerInfo returns null when neither has destination") {
        val primaryWrapperTracker = mutableListOf<String>()
        val secondaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            secondaryWrapperTracker
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.getContainerInfo(UnknownDestination).shouldBeNull()
    }

    test("getContainerInfo prioritizes secondary when both have destination") {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(SharedTabs::class, ScopeKey("PrimarySharedTabs"), primaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimarySharedTabs"),
            containerClass = SharedTabs::class
        )

        val secondaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(
                SharedTabs::class,
                ScopeKey("SecondarySharedTabs"),
                secondaryTracker
            ),
            initialTabIndex = 1,
            scopeKey = ScopeKey("SecondarySharedTabs"),
            containerClass = SharedTabs::class
        )

        val primaryWrapperTracker = mutableListOf<String>()
        val secondaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            mapOf(SharedTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            mapOf(SharedTabs.Tab1 to secondaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            secondaryWrapperTracker
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(SharedTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()
        // Should use secondary's scopeKey
        tabInfo.scopeKey shouldBe ScopeKey("SecondarySharedTabs")
        tabInfo.initialTabIndex shouldBe 1
    }

    // =========================================================================
    // WRAPPED BUILDER TESTS
    // =========================================================================

    test("wrapped TabContainer builder uses composite navNodeBuilder") {
        val primaryTracker = mutableListOf<String>()
        val compositeBuilderTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, ScopeKey("PrimaryTabs"), primaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimaryTabs"),
            containerClass = PrimaryTabs::class
        )

        val primaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            mapOf(PrimaryTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )

        // Custom navNodeBuilder that tracks calls
        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { kclass, key, parentKey ->
                compositeBuilderTracker.add("composite-builder:$kclass:$key")
                TabNode(
                    key = NodeKey(key ?: "default-key"),
                    parentKey = parentKey?.let { NodeKey(it) },
                    stacks = listOf(
                        StackNode(
                            NodeKey("stack0"),
                            NodeKey(key ?: "default-key"),
                            listOf(ScreenNode(NodeKey("screen0"), NodeKey("stack0"), PrimaryTabs.Tab1))
                        ),
                        StackNode(
                            NodeKey("stack1"),
                            NodeKey(key ?: "default-key"),
                            listOf(ScreenNode(NodeKey("screen1"), NodeKey("stack1"), PrimaryTabs.Tab2))
                        )
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("CompositeScope")
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()

        // Call the wrapped builder
        val tabNode = info.builder(NodeKey("test-key"), NodeKey("parent-key"), 0)

        // Verify composite builder was called (not the original)
        compositeBuilderTracker.isNotEmpty().shouldBeTrue()
        compositeBuilderTracker[0] shouldBe "composite-builder:${PrimaryTabs::class}:test-key"

        // Verify the node was created correctly
        tabNode.key shouldBe NodeKey("test-key")
        tabNode.parentKey shouldBe NodeKey("parent-key")
    }

    test("wrapped PaneContainer builder uses composite navNodeBuilder") {
        val primaryTracker = mutableListOf<String>()
        val compositeBuilderTracker = mutableListOf<String>()

        val primaryPaneInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(PrimaryPane::class, ScopeKey("PrimaryPane"), primaryTracker),
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("PrimaryPane"),
            containerClass = PrimaryPane::class
        )

        val primaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            emptyMap(),
            mapOf(PrimaryPane.Pane1 to primaryPaneInfo),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )

        // Custom navNodeBuilder that tracks calls
        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { kclass, key, parentKey ->
                compositeBuilderTracker.add("composite-builder:$kclass:$key")
                PaneNode(
                    key = NodeKey(key ?: "default-key"),
                    parentKey = parentKey?.let { NodeKey(it) },
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            ScreenNode(NodeKey("pane-screen"), NodeKey(key ?: "default-key"), PrimaryPane.Pane1)
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    scopeKey = ScopeKey("CompositeScope")
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryPane.Pane1)
        info.shouldNotBeNull()
        val paneInfo = info.shouldBeInstanceOf<ContainerInfo.PaneContainer>()

        // Call the wrapped builder
        val paneNode = info.builder(NodeKey("test-key"), NodeKey("parent-key"))

        // Verify composite builder was called (not the original)
        compositeBuilderTracker.isNotEmpty().shouldBeTrue()
        compositeBuilderTracker[0] shouldBe "composite-builder:${PrimaryPane::class}:test-key"

        // Verify the node was created correctly
        paneNode.key shouldBe NodeKey("test-key")
        paneNode.parentKey shouldBe NodeKey("parent-key")
    }

    test("wrapped builder throws exception when composite navNodeBuilder returns null") {
        val primaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, ScopeKey("PrimaryTabs"), primaryTracker),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimaryTabs"),
            containerClass = PrimaryTabs::class
        )

        val primaryWrapperTracker = mutableListOf<String>()

        val primary = createTestContainerRegistry(
            mapOf(PrimaryTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            primaryWrapperTracker
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )

        // navNodeBuilder that returns null
        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1)
        info.shouldNotBeNull()
        val tabInfo = info.shouldBeInstanceOf<ContainerInfo.TabContainer>()

        // Calling the wrapped builder should throw
        shouldThrow<IllegalStateException> {
            info.builder(NodeKey("test-key"), null, 0)
        }
    }

    test("wrapped TabContainer builder applies initialTabIndex correctly") {
        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, ScopeKey("PrimaryTabs"), null),
            initialTabIndex = 0,
            scopeKey = ScopeKey("PrimaryTabs"),
            containerClass = PrimaryTabs::class
        )

        val primary = createTestContainerRegistry(
            mapOf(PrimaryTabs.Tab1 to primaryTabInfo),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, key, parentKey ->
                TabNode(
                    key = NodeKey(key ?: "default"),
                    parentKey = parentKey?.let { NodeKey(it) },
                    stacks = listOf(
                        StackNode(
                            NodeKey("stack0"),
                            NodeKey(key ?: "default"),
                            listOf(ScreenNode(NodeKey("s0"), NodeKey("stack0"), PrimaryTabs.Tab1))
                        ),
                        StackNode(
                            NodeKey("stack1"),
                            NodeKey(key ?: "default"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("stack1"), PrimaryTabs.Tab2))
                        ),
                        StackNode(
                            NodeKey("stack2"),
                            NodeKey(key ?: "default"),
                            listOf(ScreenNode(NodeKey("s2"), NodeKey("stack2"), PrimaryTabs.Tab1))
                        )
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("Test")
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1) as ContainerInfo.TabContainer

        // Test different initialTabIndex values (builder creates 3 stacks with indices 0, 1, 2)
        val node0 = info.builder(NodeKey("key"), null, 0)
        node0.activeStackIndex shouldBe 0

        val node1 = info.builder(NodeKey("key"), null, 1)
        node1.activeStackIndex shouldBe 1

        val node2 = info.builder(NodeKey("key"), null, 2)
        node2.activeStackIndex shouldBe 2

        // Test out of bounds - should be coerced to max stack index
        val nodeHigh = info.builder(NodeKey("key"), null, 10)
        nodeHigh.activeStackIndex shouldBe 2 // Coerced to max index (2 = stacks.size - 1)
    }

    // =========================================================================
    // TABS/PANE CONTAINER WRAPPER TESTS
    // =========================================================================

    test("hasTabsContainer returns true when secondary has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("primary-tab"),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("secondary-tab"),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasTabsContainer("secondary-tab").shouldBeTrue()
    }

    test("hasTabsContainer returns true when primary has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("primary-tab"),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("secondary-tab"),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasTabsContainer("primary-tab").shouldBeTrue()
    }

    test("hasTabsContainer returns false when neither has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("primary-tab"),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("secondary-tab"),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasTabsContainer("unknown-tab").shouldBeFalse()
    }

    test("hasTabsContainer returns true when both have container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("shared-tab"),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            setOf("shared-tab"),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasTabsContainer("shared-tab").shouldBeTrue()
    }

    test("hasPaneContainer returns true when secondary has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("primary-pane"),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("secondary-pane"),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasPaneContainer("secondary-pane").shouldBeTrue()
    }

    test("hasPaneContainer returns true when primary has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("primary-pane"),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("secondary-pane"),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasPaneContainer("primary-pane").shouldBeTrue()
    }

    test("hasPaneContainer returns false when neither has container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("primary-pane"),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("secondary-pane"),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasPaneContainer("unknown-pane").shouldBeFalse()
    }

    test("hasPaneContainer returns true when both have container") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("shared-pane"),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            setOf("shared-pane"),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.hasPaneContainer("shared-pane").shouldBeTrue()
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    test("composite with both empty registries works correctly") {
        val primary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )
        val secondary = createTestContainerRegistry(
            emptyMap(),
            emptyMap(),
            emptySet(),
            emptySet(),
            mutableListOf()
        )

        val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode? =
            { _, _, _ -> null }
        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        composite.getContainerInfo(PrimaryTabs.Tab1).shouldBeNull()
        composite.hasTabsContainer("any-tab").shouldBeFalse()
        composite.hasPaneContainer("any-pane").shouldBeFalse()
    }
})
