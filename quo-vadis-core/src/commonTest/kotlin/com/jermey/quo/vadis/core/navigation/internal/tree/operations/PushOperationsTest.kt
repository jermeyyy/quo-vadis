@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.reflect.KClass

// =============================================================================
// Test destinations
// =============================================================================

private object HomeDestination : NavDestination {
    override fun toString(): String = "Home"
}

private object ProfileDestination : NavDestination {
    override fun toString(): String = "Profile"
}

private object SettingsDestination : NavDestination {
    override fun toString(): String = "Settings"
}

private object DetailDestination : NavDestination {
    override fun toString(): String = "Detail"
}

private object OutOfScopeDestination : NavDestination {
    override fun toString(): String = "OutOfScope"
}

private sealed interface ScopedDestination : NavDestination {
    data object TabA : ScopedDestination {
        override fun toString(): String = "TabA"
    }

    data object TabB : ScopedDestination {
        override fun toString(): String = "TabB"
    }

    data object TabC : ScopedDestination {
        override fun toString(): String = "TabC"
    }
}

private object PanePrimaryDestination : NavDestination {
    override fun toString(): String = "PanePrimary"
}

private object PaneSupportingDestination : NavDestination {
    override fun toString(): String = "PaneSupporting"
}

class PushOperationsTest : FunSpec({

    // =========================================================================
    // HELPERS
    // =========================================================================

    fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("key-${counter++}") }
    }

    val testScopeRegistry = object : ScopeRegistry {
        val scopes = mapOf(
            "TestScope" to setOf(
                ScopedDestination.TabA::class,
                ScopedDestination.TabB::class,
                ScopedDestination.TabC::class,
            )
        )

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    val testPaneRoleRegistry = object : PaneRoleRegistry {
        val roles = mapOf<String, Map<KClass<out NavDestination>, PaneRole>>(
            "PaneScope" to mapOf(
                PanePrimaryDestination::class to PaneRole.Primary,
                PaneSupportingDestination::class to PaneRole.Supporting,
            )
        )

        override fun getPaneRole(scopeKey: ScopeKey, destination: NavDestination): PaneRole? {
            return roles[scopeKey.value]?.entries?.find { (clazz, _) ->
                clazz.isInstance(destination)
            }?.value
        }

        override fun getPaneRole(
            scopeKey: ScopeKey,
            destinationClass: KClass<out NavDestination>
        ): PaneRole? {
            return roles[scopeKey.value]?.get(destinationClass)
        }
    }

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // push(root, destination, generateKey) — simple push
    // =========================================================================

    test("push adds screen to empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, HomeDestination, gen)

        result.shouldBeInstanceOf<StackNode>()
        result.children.size shouldBe 1
        val screen = result.children[0] as ScreenNode
        screen.destination shouldBe HomeDestination
        screen.parentKey shouldBe NodeKey("root")
    }

    test("push appends to existing stack") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, ProfileDestination, gen) as StackNode

        result.children.size shouldBe 2
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
    }

    test("push targets deepest active stack in tabs") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                            )
                        ),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
                    ),
                    activeStackIndex = 0
                )
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, ProfileDestination, gen) as StackNode
        val tabs = result.children[0] as TabNode

        tabs.stacks[0].children.size shouldBe 2
        (tabs.stacks[0].activeChild as ScreenNode).destination shouldBe ProfileDestination
        tabs.stacks[1].children.size shouldBe 0
    }

    test("push targets deepest active stack through pane") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), HomeDestination))
        )
        val supportingStack = StackNode(
            NodeKey("supporting"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ss1"), NodeKey("supporting"), ProfileDestination))
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, DetailDestination, gen) as StackNode
        val resultPane = result.children[0] as PaneNode
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode

        resultPrimary.children.size shouldBe 2
        (resultPrimary.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("push throws when no active stack found") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            PushOperations.push(root, ProfileDestination)
        }
    }

    test("push preserves structural sharing") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, ProfileDestination, gen) as StackNode

        result.children[0] shouldBeSameInstanceAs screen1
    }

    // =========================================================================
    // pushToStack(root, stackKey, destination, generateKey)
    // =========================================================================

    test("pushToStack targets specific stack by key") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result =
            PushOperations.pushToStack(root, NodeKey("tab1"), ProfileDestination, gen) as TabNode

        result.stacks[0].children.size shouldBe 0
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pushToStack adds to existing children") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result =
            PushOperations.pushToStack(root, NodeKey("root"), ProfileDestination, gen) as StackNode

        result.children.size shouldBe 2
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pushToStack throws for non-existent key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            PushOperations.pushToStack(root, NodeKey("missing"), HomeDestination)
        }
    }

    test("pushToStack throws for non-StackNode key") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen"), NodeKey("root"), HomeDestination)
            )
        )

        shouldThrow<IllegalArgumentException> {
            PushOperations.pushToStack(root, NodeKey("screen"), ProfileDestination)
        }
    }

    test("pushToStack works with deeply nested stack") {
        val targetStack = StackNode(NodeKey("target"), NodeKey("inner-tabs"), emptyList())
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("outer-tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("outer-tabs"), listOf(
                                TabNode(
                                    key = NodeKey("inner-tabs"),
                                    parentKey = NodeKey("tab0"),
                                    stacks = listOf(targetStack),
                                    activeStackIndex = 0
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.pushToStack(root, NodeKey("target"), ProfileDestination, gen)
        val foundStack = result.findByKey(NodeKey("target")) as StackNode

        foundStack.children.size shouldBe 1
        (foundStack.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pushToStack to inactive tab stack does not change active tab") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result =
            PushOperations.pushToStack(root, NodeKey("tab1"), ProfileDestination, gen) as TabNode

        result.activeStackIndex shouldBe 0
        result.stacks[1].children.size shouldBe 1
    }

    // =========================================================================
    // push(root, destination, scopeRegistry, paneRoleRegistry, generateKey)
    // — scope-aware push (covers determinePushStrategy, findTabWithDestination,
    //   pushToActiveStack, pushOutOfScope, pushToPaneStack indirectly)
    // =========================================================================

    test("scope-aware push with empty registries delegates to simple push") {
        val root = StackNode(
            NodeKey("root"), null,
            listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination))
        )
        val gen = createKeyGenerator()

        val result = PushOperations.push(
            root, DetailDestination, ScopeRegistry.Empty, PaneRoleRegistry.Empty, gen
        ) as StackNode

        result.children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("scope-aware push in-scope destination pushes to active stack") {
        val homeScreen = ScreenNode(NodeKey("s-home"), NodeKey("tab0"), ScopedDestination.TabA)
        val tabBScreen = ScreenNode(NodeKey("s-tabB"), NodeKey("tab1"), ScopedDestination.TabB)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(homeScreen)),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tabBScreen))
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("TestScope")
                )
            )
        )
        val gen = createKeyGenerator()

        // TabC is in scope but not in any tab stack → push to active stack
        val result =
            PushOperations.push(root, ScopedDestination.TabC, testScopeRegistry, generateKey = gen)

        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode
        tabNode.activeStackIndex shouldBe 0
        val activeStack = tabNode.stacks[0]
        activeStack.children.size shouldBe 2
        (activeStack.activeChild as ScreenNode).destination shouldBe ScopedDestination.TabC
    }

    test("scope-aware push switches to tab containing destination") {
        val homeScreen = ScreenNode(NodeKey("s-home"), NodeKey("tab0"), ScopedDestination.TabA)
        val tabBScreen = ScreenNode(NodeKey("s-tabB"), NodeKey("tab1"), ScopedDestination.TabB)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(homeScreen)),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tabBScreen))
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("TestScope")
                )
            )
        )
        val gen = createKeyGenerator()

        // TabB exists in tab1 → should switch to tab1
        val result =
            PushOperations.push(root, ScopedDestination.TabB, testScopeRegistry, generateKey = gen)

        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode
        tabNode.activeStackIndex shouldBe 1
        // No new screens pushed – just tab switch
        tabNode.stacks[0].children.size shouldBe 1
        tabNode.stacks[1].children.size shouldBe 1
    }

    test("scope-aware push does not switch tab if destination is in active tab") {
        val homeScreen = ScreenNode(NodeKey("s-home"), NodeKey("tab0"), ScopedDestination.TabA)
        val tabBScreen = ScreenNode(NodeKey("s-tabB"), NodeKey("tab1"), ScopedDestination.TabB)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(homeScreen)),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tabBScreen))
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("TestScope")
                )
            )
        )
        val gen = createKeyGenerator()

        // TabA exists in tab0 which is already active → no switch, push to active
        val result =
            PushOperations.push(root, ScopedDestination.TabA, testScopeRegistry, generateKey = gen)

        val resultStack = result as StackNode
        val tabNode = resultStack.children[0] as TabNode
        tabNode.activeStackIndex shouldBe 0
    }

    test("scope-aware push out-of-scope destination goes to parent stack") {
        val homeScreen = ScreenNode(NodeKey("s-home"), NodeKey("tab0"), ScopedDestination.TabA)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(homeScreen)),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("TestScope")
                )
            )
        )
        val gen = createKeyGenerator()

        val result =
            PushOperations.push(root, OutOfScopeDestination, testScopeRegistry, generateKey = gen)

        val resultStack = result as StackNode
        resultStack.children.size shouldBe 2
        resultStack.children[0].shouldBeInstanceOf<TabNode>()
        val newScreen = resultStack.children[1] as ScreenNode
        newScreen.destination shouldBe OutOfScopeDestination
        newScreen.parentKey shouldBe NodeKey("root")
    }

    test("scope-aware push out-of-scope preserves tab container state") {
        val homeScreen = ScreenNode(NodeKey("s-home"), NodeKey("tab0"), ScopedDestination.TabA)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(homeScreen)),
                    ),
                    activeStackIndex = 0,
                    scopeKey = ScopeKey("TestScope")
                )
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.push(
            root,
            OutOfScopeDestination,
            testScopeRegistry,
            generateKey = gen
        ) as StackNode
        val tabNode = result.children[0] as TabNode

        tabNode.stacks.size shouldBe 1
        tabNode.activeStackIndex shouldBe 0
        tabNode.stacks[0].children.size shouldBe 1
    }

    test("scope-aware push routes to pane stack by role") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), PanePrimaryDestination))
        )
        val supportingStack = StackNode(
            NodeKey("supporting"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ss1"), NodeKey("supporting"), HomeDestination))
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = ScopeKey("PaneScope")
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))
        val gen = createKeyGenerator()

        // PaneSupportingDestination has role=Supporting in testPaneRoleRegistry
        // and we need a scope registry that keeps it in scope
        val paneScopeRegistry = object : ScopeRegistry {
            override fun isInScope(scopeKey: ScopeKey, destination: NavDestination) = true
            override fun getScopeKey(destination: NavDestination): ScopeKey? = null
        }

        val result = PushOperations.push(
            root, PaneSupportingDestination, paneScopeRegistry, testPaneRoleRegistry, gen
        ) as StackNode

        val resultPane = result.children[0] as PaneNode
        val resultSupporting =
            resultPane.paneConfigurations[PaneRole.Supporting]!!.content as StackNode

        resultSupporting.children.size shouldBe 2
        (resultSupporting.activeChild as ScreenNode).destination shouldBe PaneSupportingDestination
        resultPane.activePaneRole shouldBe PaneRole.Supporting
    }

    test("scope-aware push out-of-scope pane goes to parent stack") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), HomeDestination))
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack)
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = ScopeKey("PaneScope")
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))
        val gen = createKeyGenerator()

        // OutOfScopeDestination is NOT in PaneScope
        val paneScopeRegistry = object : ScopeRegistry {
            override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
                return destination !is OutOfScopeDestination
            }

            override fun getScopeKey(destination: NavDestination): ScopeKey? = null
        }

        val result = PushOperations.push(
            root, OutOfScopeDestination, paneScopeRegistry, testPaneRoleRegistry, gen
        ) as StackNode

        result.children.size shouldBe 2
        result.children[0].shouldBeInstanceOf<PaneNode>()
        (result.children[1] as ScreenNode).destination shouldBe OutOfScopeDestination
    }

    test("scope-aware push with tab without scopeKey pushes to active stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(
                            NodeKey("tab0"), NodeKey("tabs"),
                            listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination))
                        ),
                    ),
                    activeStackIndex = 0,
                    scopeKey = null // no scope
                )
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.push(
            root,
            DetailDestination,
            testScopeRegistry,
            generateKey = gen
        ) as StackNode
        val tabNode = result.children[0] as TabNode
        val activeStack = tabNode.stacks[0]

        activeStack.children.size shouldBe 2
        (activeStack.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("scope-aware push with scoped stack out-of-scope goes to parent") {
        val innerScreen = ScreenNode(NodeKey("s1"), NodeKey("inner"), ScopedDestination.TabA)
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("root"),
            children = listOf(innerScreen),
            scopeKey = ScopeKey("TestScope")
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(innerStack)
        )
        val gen = createKeyGenerator()

        val result = PushOperations.push(
            root,
            OutOfScopeDestination,
            testScopeRegistry,
            generateKey = gen
        ) as StackNode

        // Out of scope from inner → should push to root
        result.children.size shouldBe 2
        result.children[0].shouldBeInstanceOf<StackNode>()
        (result.children[1] as ScreenNode).destination shouldBe OutOfScopeDestination
    }

    // =========================================================================
    // clearAndPush
    // =========================================================================

    test("clearAndPush clears stack and pushes single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.clearAndPush(root, DetailDestination, gen) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("clearAndPush works on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.clearAndPush(root, HomeDestination, gen) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("clearAndPush targets deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result = PushOperations.clearAndPush(root, DetailDestination, gen) as TabNode

        result.stacks[0].children.size shouldBe 1
        (result.stacks[0].activeChild as ScreenNode).destination shouldBe DetailDestination
        result.stacks[1].children.size shouldBe 1
    }

    test("clearAndPush throws when no active stack found") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            PushOperations.clearAndPush(root, ProfileDestination)
        }
    }

    test("clearAndPush assigns correct parentKey") {
        val root = StackNode(
            key = NodeKey("my-stack"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("my-stack"), HomeDestination)
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.clearAndPush(root, DetailDestination, gen) as StackNode

        (result.activeChild as ScreenNode).parentKey shouldBe NodeKey("my-stack")
    }

    // =========================================================================
    // clearStackAndPush
    // =========================================================================

    test("clearStackAndPush clears specific stack and pushes") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination),
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result = PushOperations.clearStackAndPush(
            root,
            NodeKey("tab1"),
            DetailDestination,
            gen
        ) as TabNode

        result.stacks[0].children.size shouldBe 1
        (result.stacks[0].activeChild as ScreenNode).destination shouldBe HomeDestination
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("clearStackAndPush works on already-empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.clearStackAndPush(
            root,
            NodeKey("root"),
            HomeDestination,
            gen
        ) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("clearStackAndPush throws for non-existent key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            PushOperations.clearStackAndPush(root, NodeKey("missing"), HomeDestination)
        }
    }

    test("clearStackAndPush throws for non-StackNode key") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen"), NodeKey("root"), HomeDestination)
            )
        )

        shouldThrow<IllegalArgumentException> {
            PushOperations.clearStackAndPush(root, NodeKey("screen"), ProfileDestination)
        }
    }

    test("clearStackAndPush preserves structural sharing for untouched branches") {
        val tab1Stack = StackNode(
            NodeKey("tab1"), NodeKey("tabs"), listOf(
                ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
            )
        )
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                tab1Stack
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result = PushOperations.clearStackAndPush(
            root,
            NodeKey("tab0"),
            DetailDestination,
            gen
        ) as TabNode

        result.stacks[1] shouldBeSameInstanceAs tab1Stack
    }

    // =========================================================================
    // replaceCurrent
    // =========================================================================

    test("replaceCurrent replaces top screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.replaceCurrent(root, DetailDestination, gen) as StackNode

        result.children.size shouldBe 2
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe DetailDestination
    }

    test("replaceCurrent works with single screen stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.replaceCurrent(root, ProfileDestination, gen) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("replaceCurrent throws on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            PushOperations.replaceCurrent(root, HomeDestination)
        }
    }

    test("replaceCurrent throws when no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            PushOperations.replaceCurrent(root, ProfileDestination)
        }
    }

    test("replaceCurrent targets deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result = PushOperations.replaceCurrent(root, DetailDestination, gen) as TabNode

        result.stacks[0].children.size shouldBe 2
        (result.stacks[0].children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.stacks[0].children[1] as ScreenNode).destination shouldBe DetailDestination
        // tab1 unchanged
        result.stacks[1].children.size shouldBe 1
    }

    test("replaceCurrent assigns correct parentKey") {
        val root = StackNode(
            key = NodeKey("my-stack"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("my-stack"), HomeDestination)
            )
        )
        val gen = createKeyGenerator()

        val result = PushOperations.replaceCurrent(root, DetailDestination, gen) as StackNode
        (result.activeChild as ScreenNode).parentKey shouldBe NodeKey("my-stack")
    }

    // =========================================================================
    // pushAll
    // =========================================================================

    test("pushAll adds multiple screens in order") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(
            root, listOf(HomeDestination, ProfileDestination, SettingsDestination), gen
        ) as StackNode

        result.children.size shouldBe 3
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
        (result.children[2] as ScreenNode).destination shouldBe SettingsDestination
    }

    test("pushAll appends to existing stack") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(
            root, listOf(ProfileDestination, SettingsDestination), gen
        ) as StackNode

        result.children.size shouldBe 3
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
        (result.children[2] as ScreenNode).destination shouldBe SettingsDestination
    }

    test("pushAll with empty list returns same tree") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(root, emptyList(), gen)

        result shouldBeSameInstanceAs root
    }

    test("pushAll targets deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(
            root, listOf(ProfileDestination, SettingsDestination), gen
        ) as TabNode

        result.stacks[0].children.size shouldBe 3
        result.stacks[1].children.size shouldBe 0
    }

    test("pushAll throws when no active stack found") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            PushOperations.pushAll(root, listOf(ProfileDestination))
        }
    }

    test("pushAll with single destination works like push") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(root, listOf(HomeDestination), gen) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("pushAll assigns correct parentKey to all screens") {
        val root = StackNode(NodeKey("my-stack"), null, emptyList())
        val gen = createKeyGenerator()

        val result = PushOperations.pushAll(
            root, listOf(HomeDestination, ProfileDestination), gen
        ) as StackNode

        result.children.forEach { child ->
            (child as ScreenNode).parentKey shouldBe NodeKey("my-stack")
        }
    }

    // =========================================================================
    // Edge case: push duplicate destination
    // =========================================================================

    test("push allows duplicate destination instances") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))
        val gen = createKeyGenerator()

        val result = PushOperations.push(root, HomeDestination, gen) as StackNode

        result.children.size shouldBe 2
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe HomeDestination
    }

    // =========================================================================
    // Edge case: pane role routing with primary role and role switching
    // =========================================================================

    test("scope-aware push to primary pane switches active pane to primary") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), HomeDestination))
        )
        val supportingStack = StackNode(
            NodeKey("supporting"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ss1"), NodeKey("supporting"), HomeDestination))
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack),
                PaneRole.Supporting to PaneConfiguration(supportingStack)
            ),
            activePaneRole = PaneRole.Supporting, // currently on Supporting
            scopeKey = ScopeKey("PaneScope")
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))
        val gen = createKeyGenerator()
        val allInScope = object : ScopeRegistry {
            override fun isInScope(scopeKey: ScopeKey, destination: NavDestination) = true
            override fun getScopeKey(destination: NavDestination): ScopeKey? = null
        }

        val result = PushOperations.push(
            root, PanePrimaryDestination, allInScope, testPaneRoleRegistry, gen
        ) as StackNode

        val resultPane = result.children[0] as PaneNode
        resultPane.activePaneRole shouldBe PaneRole.Primary
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.children.size shouldBe 2
        (resultPrimary.activeChild as ScreenNode).destination shouldBe PanePrimaryDestination
    }

    test("scope-aware push with no pane role pushes to active pane stack") {
        val primaryStack = StackNode(
            NodeKey("primary"), NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ps1"), NodeKey("primary"), HomeDestination))
        )
        val pane = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryStack)
            ),
            activePaneRole = PaneRole.Primary,
            scopeKey = ScopeKey("PaneScope")
        )
        val root = StackNode(NodeKey("root"), null, listOf(pane))
        val gen = createKeyGenerator()
        val allInScope = object : ScopeRegistry {
            override fun isInScope(scopeKey: ScopeKey, destination: NavDestination) = true
            override fun getScopeKey(destination: NavDestination): ScopeKey? = null
        }

        // DetailDestination has no pane role registered → pushes to active stack
        val result = PushOperations.push(
            root, DetailDestination, allInScope, testPaneRoleRegistry, gen
        ) as StackNode

        val resultPane = result.children[0] as PaneNode
        val resultPrimary = resultPane.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        resultPrimary.children.size shouldBe 2
        (resultPrimary.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    // =========================================================================
    // Edge case: key generation produces unique keys
    // =========================================================================

    test("push generates unique keys for each screen") {
        val root = StackNode(NodeKey("root"), null, emptyList())
        val gen = createKeyGenerator()

        var current = PushOperations.push(root, HomeDestination, gen) as StackNode
        current = PushOperations.push(current, ProfileDestination, gen) as StackNode
        current = PushOperations.push(current, SettingsDestination, gen) as StackNode

        val keys = current.children.map { (it as ScreenNode).key }
        keys.distinct().size shouldBe 3
    }
})
