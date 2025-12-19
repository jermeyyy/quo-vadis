package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ContainerRegistry] interface and [ContainerInfo] sealed class.
 *
 * Tests cover:
 * - ContainerRegistry.Empty behavior
 * - ContainerInfo.TabContainer properties
 * - ContainerInfo.PaneContainer properties
 * - Custom ContainerRegistry implementations
 */
class ContainerRegistryTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    /**
     * Simulates a destination that is part of a tab container.
     */
    private sealed interface MainTabs : NavDestination {
        data object HomeTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object SettingsTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * Simulates a destination that is part of a pane container.
     */
    private sealed interface DetailPane : NavDestination {
        data object ListItem : DetailPane {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object DetailItem : DetailPane {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * A destination that is NOT part of any container.
     */
    private data object StandaloneDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // HELPER BUILDERS
    // =========================================================================

    /**
     * Creates a minimal TabNode builder for testing.
     */
    private fun createTabNodeBuilder(): (String, String?, Int) -> TabNode = { key, parentKey, initialTabIndex ->
        TabNode(
            key = key,
            parentKey = parentKey,
            stacks = listOf(
                StackNode("$key-tab0", key, listOf(ScreenNode("$key-screen0", "$key-tab0", MainTabs.HomeTab))),
                StackNode("$key-tab1", key, listOf(ScreenNode("$key-screen1", "$key-tab1", MainTabs.SettingsTab)))
            ),
            activeStackIndex = initialTabIndex.coerceIn(0, 1),
            scopeKey = "MainTabs"
        )
    }

    /**
     * Creates a minimal PaneNode builder for testing.
     */
    private fun createPaneNodeBuilder(): (String, String?) -> PaneNode = { key, parentKey ->
        PaneNode(
            key = key,
            parentKey = parentKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("$key-primary", key, DetailPane.ListItem)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("$key-supporting", key, DetailPane.DetailItem)
                )
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = "DetailPane"
        )
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    @Test
    fun `Empty registry returns null for any destination`() {
        assertNull(ContainerRegistry.Empty.getContainerInfo(MainTabs.HomeTab))
        assertNull(ContainerRegistry.Empty.getContainerInfo(MainTabs.SettingsTab))
        assertNull(ContainerRegistry.Empty.getContainerInfo(DetailPane.ListItem))
        assertNull(ContainerRegistry.Empty.getContainerInfo(StandaloneDestination))
    }

    @Test
    fun `Empty registry is a singleton`() {
        val empty1 = ContainerRegistry.Empty
        val empty2 = ContainerRegistry.Empty
        assertEquals(empty1, empty2)
    }

    // =========================================================================
    // TAB CONTAINER INFO TESTS
    // =========================================================================

    @Test
    fun `TabContainer has correct initialTabIndex property`() {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 1,
            scopeKey = "TestScope",
            containerClass = MainTabs::class
        )

        assertEquals(1, info.initialTabIndex)
    }

    @Test
    fun `TabContainer has correct scopeKey property`() {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 0,
            scopeKey = "MainTabs",
            containerClass = MainTabs::class
        )

        assertEquals("MainTabs", info.scopeKey)
    }

    @Test
    fun `TabContainer builder creates valid TabNode`() {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 1,
            scopeKey = "MainTabs",
            containerClass = MainTabs::class
        )

        val tabNode = info.builder("test-tabs", "parent", 1)

        assertEquals("test-tabs", tabNode.key)
        assertEquals("parent", tabNode.parentKey)
        assertEquals(1, tabNode.activeStackIndex)
        assertEquals(2, tabNode.tabCount)
    }

    @Test
    fun `TabContainer builder respects initialTabIndex`() {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 0,
            scopeKey = "MainTabs",
            containerClass = MainTabs::class
        )

        val tabNode = info.builder("tabs", null, info.initialTabIndex)

        assertEquals(0, tabNode.activeStackIndex)
    }

    @Test
    fun `TabContainer equality based on properties`() {
        val builder = createTabNodeBuilder()
        val info1 = ContainerInfo.TabContainer(builder, initialTabIndex = 0, scopeKey = "Scope1", containerClass = MainTabs::class)
        val info2 = ContainerInfo.TabContainer(builder, initialTabIndex = 0, scopeKey = "Scope1", containerClass = MainTabs::class)
        val info3 = ContainerInfo.TabContainer(builder, initialTabIndex = 1, scopeKey = "Scope1", containerClass = MainTabs::class)

        assertEquals(info1, info2)
        assertNotEquals(info1, info3)
    }

    @Test
    fun `TabContainer with different scopeKeys are not equal`() {
        val builder = createTabNodeBuilder()
        val info1 = ContainerInfo.TabContainer(builder, initialTabIndex = 0, scopeKey = "Scope1", containerClass = MainTabs::class)
        val info2 = ContainerInfo.TabContainer(builder, initialTabIndex = 0, scopeKey = "Scope2", containerClass = MainTabs::class)

        assertNotEquals(info1, info2)
    }

    // =========================================================================
    // PANE CONTAINER INFO TESTS
    // =========================================================================

    @Test
    fun `PaneContainer has correct initialPane property`() {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Supporting,
            scopeKey = "TestScope",
            containerClass = DetailPane::class
        )

        assertEquals(PaneRole.Supporting, info.initialPane)
    }

    @Test
    fun `PaneContainer has correct scopeKey property`() {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Primary,
            scopeKey = "DetailPane",
            containerClass = DetailPane::class
        )

        assertEquals("DetailPane", info.scopeKey)
    }

    @Test
    fun `PaneContainer builder creates valid PaneNode`() {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Primary,
            scopeKey = "DetailPane",
            containerClass = DetailPane::class
        )

        val paneNode = info.builder("test-panes", "parent")

        assertEquals("test-panes", paneNode.key)
        assertEquals("parent", paneNode.parentKey)
        assertEquals(2, paneNode.paneCount)
        assertEquals(setOf(PaneRole.Primary, PaneRole.Supporting), paneNode.configuredRoles)
    }

    @Test
    fun `PaneContainer supports all PaneRole values`() {
        val builder = createPaneNodeBuilder()

        listOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra).forEach { role ->
            val info = ContainerInfo.PaneContainer(
                builder = builder,
                initialPane = role,
                scopeKey = "Test",
                containerClass = DetailPane::class
            )
            assertEquals(role, info.initialPane)
        }
    }

    @Test
    fun `PaneContainer equality based on properties`() {
        val builder = createPaneNodeBuilder()
        val info1 = ContainerInfo.PaneContainer(builder, initialPane = PaneRole.Primary, scopeKey = "Scope1", containerClass = DetailPane::class)
        val info2 = ContainerInfo.PaneContainer(builder, initialPane = PaneRole.Primary, scopeKey = "Scope1", containerClass = DetailPane::class)
        val info3 = ContainerInfo.PaneContainer(builder, initialPane = PaneRole.Supporting, scopeKey = "Scope1", containerClass = DetailPane::class)

        assertEquals(info1, info2)
        assertNotEquals(info1, info3)
    }

    @Test
    fun `PaneContainer with different scopeKeys are not equal`() {
        val builder = createPaneNodeBuilder()
        val info1 = ContainerInfo.PaneContainer(builder, initialPane = PaneRole.Primary, scopeKey = "Scope1", containerClass = DetailPane::class)
        val info2 = ContainerInfo.PaneContainer(builder, initialPane = PaneRole.Primary, scopeKey = "Scope2", containerClass = DetailPane::class)

        assertNotEquals(info1, info2)
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS
    // =========================================================================

    @Test
    fun `Custom registry returns TabContainer for registered tab destinations`() {
        val tabContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = "MainTabs",
            containerClass = MainTabs::class
        )

        val registry = createTestContainerRegistry { destination ->
            when (destination) {
                is MainTabs -> tabContainerInfo.copy(
                    initialTabIndex = when (destination) {
                        MainTabs.HomeTab -> 0
                        MainTabs.SettingsTab -> 1
                    }
                )
                else -> null
            }
        }

        val homeInfo = registry.getContainerInfo(MainTabs.HomeTab)
        assertEquals(tabContainerInfo.copy(initialTabIndex = 0), homeInfo)

        val settingsInfo = registry.getContainerInfo(MainTabs.SettingsTab)
        assertEquals(tabContainerInfo.copy(initialTabIndex = 1), settingsInfo)
    }

    @Test
    fun `Custom registry returns PaneContainer for registered pane destinations`() {
        val paneContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = "DetailPane",
            containerClass = DetailPane::class
        )

        val registry = createTestContainerRegistry { destination ->
            when (destination) {
                is DetailPane -> paneContainerInfo.copy(
                    initialPane = when (destination) {
                        DetailPane.ListItem -> PaneRole.Primary
                        DetailPane.DetailItem -> PaneRole.Supporting
                    }
                )
                else -> null
            }
        }

        val listInfo = registry.getContainerInfo(DetailPane.ListItem)
        assertEquals(paneContainerInfo.copy(initialPane = PaneRole.Primary), listInfo)

        val detailInfo = registry.getContainerInfo(DetailPane.DetailItem)
        assertEquals(paneContainerInfo.copy(initialPane = PaneRole.Supporting), detailInfo)
    }

    @Test
    fun `Custom registry returns null for unregistered destinations`() {
        val registry = createTestContainerRegistry { destination ->
            if (destination is MainTabs) {
                ContainerInfo.TabContainer(
                    builder = createTabNodeBuilder(),
                    initialTabIndex = 0,
                    scopeKey = "MainTabs",
                    containerClass = MainTabs::class
                )
            } else {
                null
            }
        }

        assertNull(registry.getContainerInfo(StandaloneDestination))
        assertNull(registry.getContainerInfo(DetailPane.ListItem))
    }

    @Test
    fun `Custom registry can handle mixed container types`() {
        val tabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = "MainTabs",
            containerClass = MainTabs::class
        )
        val paneInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = "DetailPane",
            containerClass = DetailPane::class
        )

        val registry = createTestContainerRegistry { destination ->
            when (destination) {
                is MainTabs -> tabInfo
                is DetailPane -> paneInfo
                else -> null
            }
        }

        val homeResult = registry.getContainerInfo(MainTabs.HomeTab)
        assertEquals(tabInfo, homeResult)
        assertTrue(homeResult is ContainerInfo.TabContainer)

        val detailResult = registry.getContainerInfo(DetailPane.ListItem)
        assertEquals(paneInfo, detailResult)
        assertTrue(detailResult is ContainerInfo.PaneContainer)

        assertNull(registry.getContainerInfo(StandaloneDestination))
    }

    // =========================================================================
    // CONTAINER INFO SEALED CLASS TESTS
    // =========================================================================

    @Test
    fun `ContainerInfo scopeKey is accessible from both variants`() {
        val tabInfo: ContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = "TabScope",
            containerClass = MainTabs::class
        )

        val paneInfo: ContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = "PaneScope",
            containerClass = DetailPane::class
        )

        assertEquals("TabScope", tabInfo.scopeKey)
        assertEquals("PaneScope", paneInfo.scopeKey)
    }

    @Test
    fun `ContainerInfo when expression exhaustive check`() {
        val tabInfo: ContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = "Test",
            containerClass = MainTabs::class
        )
        val paneInfo: ContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = "Test",
            containerClass = DetailPane::class
        )

        // This verifies exhaustive when works correctly
        val tabType = when (tabInfo) {
            is ContainerInfo.TabContainer -> "tab"
            is ContainerInfo.PaneContainer -> "pane"
        }
        assertEquals("tab", tabType)

        val paneType = when (paneInfo) {
            is ContainerInfo.TabContainer -> "tab"
            is ContainerInfo.PaneContainer -> "pane"
        }
        assertEquals("pane", paneType)
    }

    // =========================================================================
    // HELPER FUNCTIONS
    // =========================================================================

    /**
     * Creates a test ContainerRegistry with default wrapper implementations.
     * Only the getContainerInfo function needs custom logic.
     */
    private fun createTestContainerRegistry(
        getContainerInfoImpl: (NavDestination) -> ContainerInfo?
    ): ContainerRegistry = object : ContainerRegistry {
        override fun getContainerInfo(destination: NavDestination): ContainerInfo? =
            getContainerInfoImpl(destination)

        @Composable
        override fun TabsContainer(
            tabNodeKey: String,
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            // Default: just render content
            content()
        }

        @Composable
        override fun PaneContainer(
            paneNodeKey: String,
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            // Default: just render content
            content()
        }

        override fun hasTabsContainer(tabNodeKey: String): Boolean = false
        override fun hasPaneContainer(paneNodeKey: String): Boolean = false
    }
}
