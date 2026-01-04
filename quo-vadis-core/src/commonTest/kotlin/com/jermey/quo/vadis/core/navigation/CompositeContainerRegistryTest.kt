@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.ContainerInfo
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeContainerRegistry
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [com.jermey.quo.vadis.core.dsl.registry.CompositeContainerRegistry].
 *
 * Tests cover:
 * - Secondary registry priority over primary for container info
 * - Wrapped builders correctly use the composite's navNodeBuilder
 * - TabsContainer/PaneContainer delegate to the correct registry
 * - hasTabsContainer/hasPaneContainer correctly check both registries
 */
class CompositeContainerRegistryTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

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

    // =========================================================================
    // HELPER BUILDERS
    // =========================================================================

    private fun createTabNodeBuilder(
        containerClass: KClass<out NavDestination>,
        scopeKey: String,
        builderTracker: MutableList<String>? = null
    ): (String, String?, Int) -> TabNode = { key, parentKey, initialTabIndex ->
        builderTracker?.add("builder-called:$key")
        TabNode(
            key = key,
            parentKey = parentKey,
            stacks = listOf(
                StackNode(
                    "$key-tab0",
                    key,
                    listOf(ScreenNode("$key-screen0", "$key-tab0", PrimaryTabs.Tab1))
                ),
                StackNode(
                    "$key-tab1",
                    key,
                    listOf(ScreenNode("$key-screen1", "$key-tab1", PrimaryTabs.Tab2))
                )
            ),
            activeStackIndex = initialTabIndex.coerceIn(0, 1),
            scopeKey = scopeKey
        )
    }

    private fun createPaneNodeBuilder(
        containerClass: KClass<out NavDestination>,
        scopeKey: String,
        builderTracker: MutableList<String>? = null
    ): (String, String?) -> PaneNode = { key, parentKey ->
        builderTracker?.add("builder-called:$key")
        PaneNode(
            key = key,
            parentKey = parentKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("$key-primary", key, PrimaryPane.Pane1)
                )
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = scopeKey
        )
    }

    private fun createTestContainerRegistry(
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

    @Test
    fun `getContainerInfo returns secondary info when secondary has destination`() {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, "PrimaryTabs", primaryTracker),
            initialTabIndex = 0,
            scopeKey = "PrimaryTabs",
            containerClass = PrimaryTabs::class
        )

        val secondaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(SecondaryTabs::class, "SecondaryTabs", secondaryTracker),
            initialTabIndex = 0,
            scopeKey = "SecondaryTabs",
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
        assertNotNull(info)
        assertTrue(info is ContainerInfo.TabContainer)
        assertEquals("SecondaryTabs", info.scopeKey)
    }

    @Test
    fun `getContainerInfo returns primary info when only primary has destination`() {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, "PrimaryTabs", primaryTracker),
            initialTabIndex = 0,
            scopeKey = "PrimaryTabs",
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
        assertNotNull(info)
        assertTrue(info is ContainerInfo.TabContainer)
        assertEquals("PrimaryTabs", info.scopeKey)
    }

    @Test
    fun `getContainerInfo returns null when neither has destination`() {
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

        assertNull(composite.getContainerInfo(UnknownDestination))
    }

    @Test
    fun `getContainerInfo prioritizes secondary when both have destination`() {
        val primaryTracker = mutableListOf<String>()
        val secondaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(SharedTabs::class, "PrimarySharedTabs", primaryTracker),
            initialTabIndex = 0,
            scopeKey = "PrimarySharedTabs",
            containerClass = SharedTabs::class
        )

        val secondaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(
                SharedTabs::class,
                "SecondarySharedTabs",
                secondaryTracker
            ),
            initialTabIndex = 1,
            scopeKey = "SecondarySharedTabs",
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
        assertNotNull(info)
        assertTrue(info is ContainerInfo.TabContainer)
        // Should use secondary's scopeKey
        assertEquals("SecondarySharedTabs", info.scopeKey)
        assertEquals(1, info.initialTabIndex)
    }

    // =========================================================================
    // WRAPPED BUILDER TESTS
    // =========================================================================

    @Test
    fun `wrapped TabContainer builder uses composite navNodeBuilder`() {
        val primaryTracker = mutableListOf<String>()
        val compositeBuilderTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, "PrimaryTabs", primaryTracker),
            initialTabIndex = 0,
            scopeKey = "PrimaryTabs",
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
                    key = key ?: "default-key",
                    parentKey = parentKey,
                    stacks = listOf(
                        StackNode(
                            "stack0",
                            key ?: "default-key",
                            listOf(ScreenNode("screen0", "stack0", PrimaryTabs.Tab1))
                        ),
                        StackNode(
                            "stack1",
                            key ?: "default-key",
                            listOf(ScreenNode("screen1", "stack1", PrimaryTabs.Tab2))
                        )
                    ),
                    activeStackIndex = 0,
                    scopeKey = "CompositeScope"
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1)
        assertNotNull(info)
        assertTrue(info is ContainerInfo.TabContainer)

        // Call the wrapped builder
        val tabNode = info.builder("test-key", "parent-key", 0)

        // Verify composite builder was called (not the original)
        assertTrue(compositeBuilderTracker.isNotEmpty())
        assertEquals("composite-builder:${PrimaryTabs::class}:test-key", compositeBuilderTracker[0])

        // Verify the node was created correctly
        assertEquals("test-key", tabNode.key)
        assertEquals("parent-key", tabNode.parentKey)
    }

    @Test
    fun `wrapped PaneContainer builder uses composite navNodeBuilder`() {
        val primaryTracker = mutableListOf<String>()
        val compositeBuilderTracker = mutableListOf<String>()

        val primaryPaneInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(PrimaryPane::class, "PrimaryPane", primaryTracker),
            initialPane = PaneRole.Primary,
            scopeKey = "PrimaryPane",
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
                    key = key ?: "default-key",
                    parentKey = parentKey,
                    paneConfigurations = mapOf(
                        PaneRole.Primary to PaneConfiguration(
                            ScreenNode("pane-screen", key ?: "default-key", PrimaryPane.Pane1)
                        )
                    ),
                    activePaneRole = PaneRole.Primary,
                    scopeKey = "CompositeScope"
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryPane.Pane1)
        assertNotNull(info)
        assertTrue(info is ContainerInfo.PaneContainer)

        // Call the wrapped builder
        val paneNode = info.builder("test-key", "parent-key")

        // Verify composite builder was called (not the original)
        assertTrue(compositeBuilderTracker.isNotEmpty())
        assertEquals("composite-builder:${PrimaryPane::class}:test-key", compositeBuilderTracker[0])

        // Verify the node was created correctly
        assertEquals("test-key", paneNode.key)
        assertEquals("parent-key", paneNode.parentKey)
    }

    @Test
    fun `wrapped builder throws exception when composite navNodeBuilder returns null`() {
        val primaryTracker = mutableListOf<String>()

        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, "PrimaryTabs", primaryTracker),
            initialTabIndex = 0,
            scopeKey = "PrimaryTabs",
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
        assertNotNull(info)
        assertTrue(info is ContainerInfo.TabContainer)

        // Calling the wrapped builder should throw
        assertFailsWith<IllegalStateException> {
            info.builder("test-key", null, 0)
        }
    }

    @Test
    fun `wrapped TabContainer builder applies initialTabIndex correctly`() {
        val primaryTabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(PrimaryTabs::class, "PrimaryTabs", null),
            initialTabIndex = 0,
            scopeKey = "PrimaryTabs",
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
                    key = key ?: "default",
                    parentKey = parentKey,
                    stacks = listOf(
                        StackNode(
                            "stack0",
                            key ?: "default",
                            listOf(ScreenNode("s0", "stack0", PrimaryTabs.Tab1))
                        ),
                        StackNode(
                            "stack1",
                            key ?: "default",
                            listOf(ScreenNode("s1", "stack1", PrimaryTabs.Tab2))
                        ),
                        StackNode(
                            "stack2",
                            key ?: "default",
                            listOf(ScreenNode("s2", "stack2", PrimaryTabs.Tab1))
                        )
                    ),
                    activeStackIndex = 0,
                    scopeKey = "Test"
                )
            }

        val composite = CompositeContainerRegistry(primary, secondary, navNodeBuilder)

        val info = composite.getContainerInfo(PrimaryTabs.Tab1) as ContainerInfo.TabContainer

        // Test different initialTabIndex values (builder creates 3 stacks with indices 0, 1, 2)
        val node0 = info.builder("key", null, 0)
        assertEquals(0, node0.activeStackIndex)

        val node1 = info.builder("key", null, 1)
        assertEquals(1, node1.activeStackIndex)

        val node2 = info.builder("key", null, 2)
        assertEquals(2, node2.activeStackIndex)

        // Test out of bounds - should be coerced to max stack index
        val nodeHigh = info.builder("key", null, 10)
        assertEquals(2, nodeHigh.activeStackIndex) // Coerced to max index (2 = stacks.size - 1)
    }

    // =========================================================================
    // TABS/PANE CONTAINER WRAPPER TESTS
    // =========================================================================

    @Test
    fun `hasTabsContainer returns true when secondary has container`() {
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

        assertTrue(composite.hasTabsContainer("secondary-tab"))
    }

    @Test
    fun `hasTabsContainer returns true when primary has container`() {
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

        assertTrue(composite.hasTabsContainer("primary-tab"))
    }

    @Test
    fun `hasTabsContainer returns false when neither has container`() {
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

        assertFalse(composite.hasTabsContainer("unknown-tab"))
    }

    @Test
    fun `hasTabsContainer returns true when both have container`() {
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

        assertTrue(composite.hasTabsContainer("shared-tab"))
    }

    @Test
    fun `hasPaneContainer returns true when secondary has container`() {
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

        assertTrue(composite.hasPaneContainer("secondary-pane"))
    }

    @Test
    fun `hasPaneContainer returns true when primary has container`() {
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

        assertTrue(composite.hasPaneContainer("primary-pane"))
    }

    @Test
    fun `hasPaneContainer returns false when neither has container`() {
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

        assertFalse(composite.hasPaneContainer("unknown-pane"))
    }

    @Test
    fun `hasPaneContainer returns true when both have container`() {
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

        assertTrue(composite.hasPaneContainer("shared-pane"))
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    @Test
    fun `composite with both empty registries works correctly`() {
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

        assertNull(composite.getContainerInfo(PrimaryTabs.Tab1))
        assertFalse(composite.hasTabsContainer("any-tab"))
        assertFalse(composite.hasPaneContainer("any-pane"))
    }
}
