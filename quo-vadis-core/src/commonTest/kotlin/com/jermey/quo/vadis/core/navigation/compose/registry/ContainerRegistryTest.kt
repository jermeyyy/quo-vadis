@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.registry.ContainerInfo
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

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
        content()
    }

    @Composable
    override fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    ) {
        content()
    }

    override fun hasTabsContainer(tabNodeKey: String): Boolean = false
    override fun hasPaneContainer(paneNodeKey: String): Boolean = false
}

/**
 * Tests for [ContainerRegistry] interface and [ContainerInfo] sealed class.
 *
 * Tests cover:
 * - ContainerRegistry.Empty behavior
 * - ContainerInfo.TabContainer properties
 * - ContainerInfo.PaneContainer properties
 * - Custom ContainerRegistry implementations
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

private data object StandaloneDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class ContainerRegistryTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    /**
     * Simulates a destination that is part of a tab container.
     */

    /**
     * Simulates a destination that is part of a pane container.
     */

    /**
     * A destination that is NOT part of any container.
     */

    // =========================================================================
    // HELPER BUILDERS
    // =========================================================================

    /**
     * Creates a minimal TabNode builder for testing.
     */
    fun createTabNodeBuilder(): (NodeKey, NodeKey?, Int) -> TabNode =
        { key, parentKey, initialTabIndex ->
            TabNode(
                key = key,
                parentKey = parentKey,
                stacks = listOf(
                    StackNode(
                        NodeKey("$key-tab0"),
                        key,
                        listOf(ScreenNode(NodeKey("$key-screen0"), NodeKey("$key-tab0"), MainTabs.HomeTab))
                    ),
                    StackNode(
                        NodeKey("$key-tab1"),
                        key,
                        listOf(ScreenNode(NodeKey("$key-screen1"), NodeKey("$key-tab1"), MainTabs.SettingsTab))
                    )
                ),
                activeStackIndex = initialTabIndex.coerceIn(0, 1),
                scopeKey = ScopeKey("MainTabs")
            )
        }

    /**
     * Creates a minimal PaneNode builder for testing.
     */
    fun createPaneNodeBuilder(): (NodeKey, NodeKey?) -> PaneNode = { key, parentKey ->
        PaneNode(
            key = key,
            parentKey = parentKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("$key-primary"), key, DetailPane.ListItem)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("$key-supporting"), key, DetailPane.DetailItem)
                )
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = ScopeKey("DetailPane")
        )
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    test("Empty registry returns null for any destination") {
        ContainerRegistry.Empty.getContainerInfo(MainTabs.HomeTab).shouldBeNull()
        ContainerRegistry.Empty.getContainerInfo(MainTabs.SettingsTab).shouldBeNull()
        ContainerRegistry.Empty.getContainerInfo(DetailPane.ListItem).shouldBeNull()
        ContainerRegistry.Empty.getContainerInfo(StandaloneDestination).shouldBeNull()
    }

    test("Empty registry is a singleton") {
        val empty1 = ContainerRegistry.Empty
        val empty2 = ContainerRegistry.Empty
        empty2 shouldBe empty1
    }

    // =========================================================================
    // TAB CONTAINER INFO TESTS
    // =========================================================================

    test("TabContainer has correct initialTabIndex property") {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 1,
            scopeKey = ScopeKey("TestScope"),
            containerClass = MainTabs::class
        )

        info.initialTabIndex shouldBe 1
    }

    test("TabContainer has correct scopeKey property") {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("MainTabs"),
            containerClass = MainTabs::class
        )

        info.scopeKey shouldBe ScopeKey("MainTabs")
    }

    test("TabContainer builder creates valid TabNode") {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 1,
            scopeKey = ScopeKey("MainTabs"),
            containerClass = MainTabs::class
        )

        val tabNode = info.builder(NodeKey("test-tabs"), NodeKey("parent"), 1)

        tabNode.key shouldBe NodeKey("test-tabs")
        tabNode.parentKey shouldBe NodeKey("parent")
        tabNode.activeStackIndex shouldBe 1
        tabNode.tabCount shouldBe 2
    }

    test("TabContainer builder respects initialTabIndex") {
        val builder = createTabNodeBuilder()
        val info = ContainerInfo.TabContainer(
            builder = builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("MainTabs"),
            containerClass = MainTabs::class
        )

        val tabNode = info.builder(NodeKey("tabs"), null, info.initialTabIndex)

        tabNode.activeStackIndex shouldBe 0
    }

    test("TabContainer equality based on properties") {
        val builder = createTabNodeBuilder()
        val info1 = ContainerInfo.TabContainer(
            builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("Scope1"),
            containerClass = MainTabs::class
        )
        val info2 = ContainerInfo.TabContainer(
            builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("Scope1"),
            containerClass = MainTabs::class
        )
        val info3 = ContainerInfo.TabContainer(
            builder,
            initialTabIndex = 1,
            scopeKey = ScopeKey("Scope1"),
            containerClass = MainTabs::class
        )

        info2 shouldBe info1
        info3 shouldNotBe info1
    }

    test("TabContainer with different scopeKeys are not equal") {
        val builder = createTabNodeBuilder()
        val info1 = ContainerInfo.TabContainer(
            builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("Scope1"),
            containerClass = MainTabs::class
        )
        val info2 = ContainerInfo.TabContainer(
            builder,
            initialTabIndex = 0,
            scopeKey = ScopeKey("Scope2"),
            containerClass = MainTabs::class
        )

        info2 shouldNotBe info1
    }

    // =========================================================================
    // PANE CONTAINER INFO TESTS
    // =========================================================================

    test("PaneContainer has correct initialPane property") {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Supporting,
            scopeKey = ScopeKey("TestScope"),
            containerClass = DetailPane::class
        )

        info.initialPane shouldBe PaneRole.Supporting
    }

    test("PaneContainer has correct scopeKey property") {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("DetailPane"),
            containerClass = DetailPane::class
        )

        info.scopeKey shouldBe ScopeKey("DetailPane")
    }

    test("PaneContainer builder creates valid PaneNode") {
        val builder = createPaneNodeBuilder()
        val info = ContainerInfo.PaneContainer(
            builder = builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("DetailPane"),
            containerClass = DetailPane::class
        )

        val paneNode = info.builder(NodeKey("test-panes"), NodeKey("parent"))

        paneNode.key shouldBe NodeKey("test-panes")
        paneNode.parentKey shouldBe NodeKey("parent")
        paneNode.paneCount shouldBe 2
        paneNode.configuredRoles shouldBe setOf(PaneRole.Primary, PaneRole.Supporting)
    }

    test("PaneContainer supports all PaneRole values") {
        val builder = createPaneNodeBuilder()

        listOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra).forEach { role ->
            val info = ContainerInfo.PaneContainer(
                builder = builder,
                initialPane = role,
                scopeKey = ScopeKey("Test"),
                containerClass = DetailPane::class
            )
            info.initialPane shouldBe role
        }
    }

    test("PaneContainer equality based on properties") {
        val builder = createPaneNodeBuilder()
        val info1 = ContainerInfo.PaneContainer(
            builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("Scope1"),
            containerClass = DetailPane::class
        )
        val info2 = ContainerInfo.PaneContainer(
            builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("Scope1"),
            containerClass = DetailPane::class
        )
        val info3 = ContainerInfo.PaneContainer(
            builder,
            initialPane = PaneRole.Supporting,
            scopeKey = ScopeKey("Scope1"),
            containerClass = DetailPane::class
        )

        info2 shouldBe info1
        info3 shouldNotBe info1
    }

    test("PaneContainer with different scopeKeys are not equal") {
        val builder = createPaneNodeBuilder()
        val info1 = ContainerInfo.PaneContainer(
            builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("Scope1"),
            containerClass = DetailPane::class
        )
        val info2 = ContainerInfo.PaneContainer(
            builder,
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("Scope2"),
            containerClass = DetailPane::class
        )

        info2 shouldNotBe info1
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS
    // =========================================================================

    test("Custom registry returns TabContainer for registered tab destinations") {
        val tabContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = ScopeKey("MainTabs"),
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
        homeInfo shouldBe tabContainerInfo.copy(initialTabIndex = 0)

        val settingsInfo = registry.getContainerInfo(MainTabs.SettingsTab)
        settingsInfo shouldBe tabContainerInfo.copy(initialTabIndex = 1)
    }

    test("Custom registry returns PaneContainer for registered pane destinations") {
        val paneContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("DetailPane"),
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
        listInfo shouldBe paneContainerInfo.copy(initialPane = PaneRole.Primary)

        val detailInfo = registry.getContainerInfo(DetailPane.DetailItem)
        detailInfo shouldBe paneContainerInfo.copy(initialPane = PaneRole.Supporting)
    }

    test("Custom registry returns null for unregistered destinations") {
        val registry = createTestContainerRegistry { destination ->
            if (destination is MainTabs) {
                ContainerInfo.TabContainer(
                    builder = createTabNodeBuilder(),
                    initialTabIndex = 0,
                    scopeKey = ScopeKey("MainTabs"),
                    containerClass = MainTabs::class
                )
            } else {
                null
            }
        }

        registry.getContainerInfo(StandaloneDestination).shouldBeNull()
        registry.getContainerInfo(DetailPane.ListItem).shouldBeNull()
    }

    test("Custom registry can handle mixed container types") {
        val tabInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = ScopeKey("MainTabs"),
            containerClass = MainTabs::class
        )
        val paneInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("DetailPane"),
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
        homeResult shouldBe tabInfo
        homeResult.shouldBeInstanceOf<ContainerInfo.TabContainer>()

        val detailResult = registry.getContainerInfo(DetailPane.ListItem)
        detailResult shouldBe paneInfo
        detailResult.shouldBeInstanceOf<ContainerInfo.PaneContainer>()

        registry.getContainerInfo(StandaloneDestination).shouldBeNull()
    }

    // =========================================================================
    // CONTAINER INFO SEALED CLASS TESTS
    // =========================================================================

    test("ContainerInfo scopeKey is accessible from both variants") {
        val tabInfo: ContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = ScopeKey("TabScope"),
            containerClass = MainTabs::class
        )

        val paneInfo: ContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("PaneScope"),
            containerClass = DetailPane::class
        )

        tabInfo.scopeKey shouldBe ScopeKey("TabScope")
        paneInfo.scopeKey shouldBe ScopeKey("PaneScope")
    }

    test("ContainerInfo when expression exhaustive check") {
        val tabInfo: ContainerInfo = ContainerInfo.TabContainer(
            builder = createTabNodeBuilder(),
            initialTabIndex = 0,
            scopeKey = ScopeKey("Test"),
            containerClass = MainTabs::class
        )
        val paneInfo: ContainerInfo = ContainerInfo.PaneContainer(
            builder = createPaneNodeBuilder(),
            initialPane = PaneRole.Primary,
            scopeKey = ScopeKey("Test"),
            containerClass = DetailPane::class
        )

        // This verifies exhaustive when works correctly
        val tabType = when (tabInfo) {
            is ContainerInfo.TabContainer -> "tab"
            is ContainerInfo.PaneContainer -> "pane"
        }
        tabType shouldBe "tab"

        val paneType = when (paneInfo) {
            is ContainerInfo.TabContainer -> "tab"
            is ContainerInfo.PaneContainer -> "pane"
        }
        paneType shouldBe "pane"
    }

})
