package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for NavNodeSerializer.
 *
 * Tests cover:
 * - Round-trip serialization for all NavNode types
 * - Error handling for invalid/null/empty input
 * - Format verification (type discriminator)
 * - Edge cases (empty stacks, deeply nested, all PaneConfiguration options)
 * - Custom destination serializer registration
 */
class NavNodeSerializerTest {

    // =========================================================================
    // SERIALIZABLE TEST DESTINATIONS
    // =========================================================================

    /**
     * Sealed class hierarchy for test destinations.
     * These are @Serializable to support NavNode serialization.
     */
    @Serializable
    sealed class TestDestination : Destination {
        override val data: Any? get() = null
        override val transition: NavigationTransition? get() = null
    }

    @Serializable
    @SerialName("home")
    data object HomeDestination : TestDestination()

    @Serializable
    @SerialName("profile")
    data object ProfileDestination : TestDestination()

    @Serializable
    @SerialName("settings")
    data object SettingsDestination : TestDestination()

    @Serializable
    @SerialName("list")
    data object ListDestination : TestDestination()

    @Serializable
    @SerialName("detail")
    data object DetailDestination : TestDestination()

    /**
     * Custom serializers module for test destinations.
     */
    private val testDestinationModule = SerializersModule {
        polymorphic(Destination::class) {
            subclass(HomeDestination::class)
            subclass(ProfileDestination::class)
            subclass(SettingsDestination::class)
            subclass(ListDestination::class)
            subclass(DetailDestination::class)
        }
    }

    /**
     * Json instance configured with test destination serializers.
     */
    private val testJson by lazy {
        NavNodeSerializer.createJson(testDestinationModule)
    }

    @BeforeTest
    fun setup() {
        // Clear any previously registered custom modules
        DestinationSerializerRegistry.clear()
        // Register our test destination serializers
        DestinationSerializerRegistry.register(testDestinationModule)
    }

    // Helper functions to serialize/deserialize using the test Json instance
    private fun toJson(node: NavNode): String =
        testJson.encodeToString(NavNode.serializer(), node)

    private fun fromJson(json: String): NavNode =
        testJson.decodeFromString(NavNode.serializer(), json)

    private fun fromJsonOrNull(json: String?): NavNode? {
        if (json.isNullOrBlank()) return null
        return try {
            fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // ROUND-TRIP TESTS - ScreenNode
    // =========================================================================

    @Test
    fun `ScreenNode serialization round-trip`() {
        val original = ScreenNode(
            key = "screen-1",
            parentKey = "stack-1",
            destination = HomeDestination
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is ScreenNode, "Restored node should be ScreenNode")
        assertEquals(original.key, restored.key)
        assertEquals(original.parentKey, restored.parentKey)
    }

    @Test
    fun `ScreenNode with null parentKey serialization round-trip`() {
        val original = ScreenNode(
            key = "root-screen",
            parentKey = null,
            destination = HomeDestination
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is ScreenNode)
        assertEquals(original.key, restored.key)
        assertNull((restored as ScreenNode).parentKey)
    }

    // =========================================================================
    // ROUND-TRIP TESTS - StackNode
    // =========================================================================

    @Test
    fun `StackNode serialization round-trip`() {
        val original = StackNode(
            key = "stack-1",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "stack-1", HomeDestination),
                ScreenNode("s2", "stack-1", ProfileDestination)
            )
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is StackNode, "Restored node should be StackNode")
        val restoredStack = restored as StackNode
        assertEquals(original.key, restoredStack.key)
        assertEquals(original.parentKey, restoredStack.parentKey)
        assertEquals(original.children.size, restoredStack.children.size)
        assertEquals(original.children[0].key, restoredStack.children[0].key)
        assertEquals(original.children[1].key, restoredStack.children[1].key)
    }

    @Test
    fun `empty StackNode serialization round-trip`() {
        val original = StackNode(
            key = "empty-stack",
            parentKey = null,
            children = emptyList()
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is StackNode)
        val restoredStack = restored as StackNode
        assertEquals(original.key, restoredStack.key)
        assertTrue(restoredStack.isEmpty)
    }

    @Test
    fun `StackNode with single child round-trip`() {
        val original = StackNode(
            key = "single-child-stack",
            parentKey = "parent",
            children = listOf(
                ScreenNode("only-screen", "single-child-stack", HomeDestination)
            )
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is StackNode)
        val restoredStack = restored as StackNode
        assertEquals(1, restoredStack.size)
        assertEquals("only-screen", restoredStack.activeChild?.key)
    }

    // =========================================================================
    // ROUND-TRIP TESTS - TabNode
    // =========================================================================

    @Test
    fun `TabNode serialization round-trip`() {
        val original = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s0", "tab0", HomeDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s1", "tab1", ProfileDestination)
                ))
            ),
            activeStackIndex = 1
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is TabNode, "Restored node should be TabNode")
        val restoredTab = restored as TabNode
        assertEquals(original.key, restoredTab.key)
        assertEquals(original.tabCount, restoredTab.tabCount)
        assertEquals(original.activeStackIndex, restoredTab.activeStackIndex)
        assertEquals(original.stacks[0].key, restoredTab.stacks[0].key)
        assertEquals(original.stacks[1].key, restoredTab.stacks[1].key)
    }

    @Test
    fun `TabNode with single stack round-trip`() {
        val original = TabNode(
            key = "single-tab",
            parentKey = null,
            stacks = listOf(
                StackNode("only-tab", "single-tab", emptyList())
            ),
            activeStackIndex = 0
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is TabNode)
        val restoredTab = restored as TabNode
        assertEquals(1, restoredTab.tabCount)
        assertEquals(0, restoredTab.activeStackIndex)
    }

    @Test
    fun `TabNode activeStackIndex preserved`() {
        val original = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList()),
                StackNode("tab2", "tabs", emptyList())
            ),
            activeStackIndex = 2
        )

        val json = toJson(original)
        val restored = fromJson(json) as TabNode

        assertEquals(2, restored.activeStackIndex)
        assertEquals("tab2", restored.activeStack.key)
    }

    // =========================================================================
    // ROUND-TRIP TESTS - PaneNode
    // =========================================================================

    @Test
    fun `PaneNode serialization round-trip`() {
        val original = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("primary", "panes", ListDestination),
                    adaptStrategy = AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    content = ScreenNode("supporting", "panes", DetailDestination),
                    adaptStrategy = AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Supporting,
            backBehavior = PaneBackBehavior.PopLatest
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is PaneNode, "Restored node should be PaneNode")
        val restoredPane = restored as PaneNode
        assertEquals(original.key, restoredPane.key)
        assertEquals(original.paneCount, restoredPane.paneCount)
        assertEquals(original.activePaneRole, restoredPane.activePaneRole)
        assertEquals(original.backBehavior, restoredPane.backBehavior)
    }

    @Test
    fun `PaneNode with Primary only round-trip`() {
        val original = PaneNode(
            key = "simple-pane",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("p", "simple-pane", HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val json = toJson(original)
        val restored = fromJson(json) as PaneNode

        assertEquals(1, restored.paneCount)
        assertEquals(PaneRole.Primary, restored.activePaneRole)
    }

    @Test
    fun `PaneNode with all PaneRoles round-trip`() {
        val original = PaneNode(
            key = "full-pane",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("primary", "full-pane", HomeDestination),
                    adaptStrategy = AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    content = ScreenNode("supporting", "full-pane", ProfileDestination),
                    adaptStrategy = AdaptStrategy.Levitate
                ),
                PaneRole.Extra to PaneConfiguration(
                    content = ScreenNode("extra", "full-pane", SettingsDestination),
                    adaptStrategy = AdaptStrategy.Reflow
                )
            ),
            activePaneRole = PaneRole.Extra
        )

        val json = toJson(original)
        val restored = fromJson(json) as PaneNode

        assertEquals(3, restored.paneCount)
        assertEquals(setOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra), restored.configuredRoles)
        assertEquals(PaneRole.Extra, restored.activePaneRole)
    }

    @Test
    fun `PaneNode all AdaptStrategy values preserved`() {
        val original = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("p1", "panes", HomeDestination),
                    adaptStrategy = AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    content = ScreenNode("p2", "panes", ProfileDestination),
                    adaptStrategy = AdaptStrategy.Levitate
                ),
                PaneRole.Extra to PaneConfiguration(
                    content = ScreenNode("p3", "panes", SettingsDestination),
                    adaptStrategy = AdaptStrategy.Reflow
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val json = toJson(original)
        val restored = fromJson(json) as PaneNode

        assertEquals(AdaptStrategy.Hide, restored.adaptStrategy(PaneRole.Primary))
        assertEquals(AdaptStrategy.Levitate, restored.adaptStrategy(PaneRole.Supporting))
        assertEquals(AdaptStrategy.Reflow, restored.adaptStrategy(PaneRole.Extra))
    }

    @Test
    fun `PaneNode all PaneBackBehavior values preserved`() {
        // Test each PaneBackBehavior value
        val behaviors = listOf(
            PaneBackBehavior.PopUntilScaffoldValueChange,
            PaneBackBehavior.PopUntilCurrentDestinationChange,
            PaneBackBehavior.PopUntilContentChange,
            PaneBackBehavior.PopLatest
        )

        behaviors.forEach { behavior ->
            val original = PaneNode(
                key = "pane-$behavior",
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Primary to PaneConfiguration(
                        content = ScreenNode("p", "pane-$behavior", HomeDestination)
                    )
                ),
                activePaneRole = PaneRole.Primary,
                backBehavior = behavior
            )

            val json = toJson(original)
            val restored = fromJson(json) as PaneNode

            assertEquals(behavior, restored.backBehavior, "PaneBackBehavior.$behavior should be preserved")
        }
    }

    // =========================================================================
    // ROUND-TRIP TESTS - Complex Nested Trees
    // =========================================================================

    @Test
    fun `complex nested tree serialization round-trip`() {
        // Build a complex tree: root stack -> tabs -> panes -> stacks -> screens
        val original = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode(
                            key = "home-tab",
                            parentKey = "tabs",
                            children = listOf(
                                ScreenNode("home", "home-tab", HomeDestination)
                            )
                        ),
                        StackNode(
                            key = "profile-tab",
                            parentKey = "tabs",
                            children = listOf(
                                PaneNode(
                                    key = "profile-panes",
                                    parentKey = "profile-tab",
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            content = StackNode(
                                                key = "profile-list-stack",
                                                parentKey = "profile-panes",
                                                children = listOf(
                                                    ScreenNode("profile-list", "profile-list-stack", ListDestination)
                                                )
                                            )
                                        ),
                                        PaneRole.Supporting to PaneConfiguration(
                                            content = ScreenNode("profile-detail", "profile-panes", DetailDestination),
                                            adaptStrategy = AdaptStrategy.Levitate
                                        )
                                    ),
                                    activePaneRole = PaneRole.Primary
                                )
                            )
                        )
                    ),
                    activeStackIndex = 1
                )
            )
        )

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is StackNode)
        val restoredRoot = restored as StackNode
        assertEquals("root", restoredRoot.key)

        // Verify structure integrity
        val tabs = restoredRoot.activeChild as TabNode
        assertEquals("tabs", tabs.key)
        assertEquals(2, tabs.tabCount)
        assertEquals(1, tabs.activeStackIndex)

        val profileTab = tabs.activeStack
        assertEquals("profile-tab", profileTab.key)

        val panes = profileTab.activeChild as PaneNode
        assertEquals("profile-panes", panes.key)
        assertEquals(2, panes.paneCount)
    }

    @Test
    fun `deeply nested stacks serialization round-trip`() {
        // Create deeply nested stack structure
        fun createNestedStack(depth: Int, parentKey: String?): StackNode {
            val key = "stack-$depth"
            return if (depth == 0) {
                StackNode(
                    key = key,
                    parentKey = parentKey,
                    children = listOf(
                        ScreenNode("leaf", key, HomeDestination)
                    )
                )
            } else {
                StackNode(
                    key = key,
                    parentKey = parentKey,
                    children = listOf(createNestedStack(depth - 1, key))
                )
            }
        }

        val original = createNestedStack(5, null)

        val json = toJson(original)
        val restored = fromJson(json)

        assertTrue(restored is StackNode)
        assertEquals("stack-5", restored.key)

        // Navigate to the leaf
        var current: NavNode = restored
        var expectedDepth = 5
        while (current is StackNode && current.activeChild != null) {
            assertEquals("stack-$expectedDepth", current.key)
            current = current.activeChild!!
            if (current is StackNode) expectedDepth--
        }
        assertTrue(current is ScreenNode)
        assertEquals("leaf", current.key)
    }

    // =========================================================================
    // ERROR HANDLING TESTS
    // =========================================================================

    @Test
    fun `fromJsonOrNull returns null on invalid JSON`() {
        val result = fromJsonOrNull("invalid json {]}")

        assertNull(result, "Invalid JSON should return null")
    }

    @Test
    fun `fromJsonOrNull returns null on null input`() {
        val result = fromJsonOrNull(null)

        assertNull(result, "Null input should return null")
    }

    @Test
    fun `fromJsonOrNull returns null on empty string`() {
        val result = fromJsonOrNull("")

        assertNull(result, "Empty string should return null")
    }

    @Test
    fun `fromJsonOrNull returns null on blank string`() {
        val result = fromJsonOrNull("   ")

        assertNull(result, "Blank string should return null")
    }

    @Test
    fun `fromJsonOrNull returns null on malformed JSON structure`() {
        val result = fromJsonOrNull("{\"not\": \"a valid navnode\"}")

        assertNull(result, "Malformed JSON structure should return null")
    }

    @Test
    fun `fromJsonOrNull returns valid node on correct JSON`() {
        val original = ScreenNode("test", null, HomeDestination)
        val json = toJson(original)

        val result = fromJsonOrNull(json)

        assertNotNull(result, "Valid JSON should return a node")
        assertEquals("test", result.key)
    }

    // =========================================================================
    // FORMAT VERIFICATION TESTS
    // =========================================================================

    @Test
    fun `serialization includes type discriminator for ScreenNode`() {
        val node = ScreenNode("screen", null, HomeDestination)

        val json = toJson(node)

        assertTrue(json.contains("\"_type\""), "JSON should contain type discriminator")
        assertTrue(json.contains("\"screen\""), "JSON should contain ScreenNode type")
    }

    @Test
    fun `serialization includes type discriminator for StackNode`() {
        val node = StackNode("stack", null, emptyList())

        val json = toJson(node)

        assertTrue(json.contains("\"_type\""), "JSON should contain type discriminator")
        assertTrue(json.contains("\"stack\""), "JSON should contain StackNode type")
    }

    @Test
    fun `serialization includes type discriminator for TabNode`() {
        val node = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("t0", "tabs", emptyList())),
            activeStackIndex = 0
        )

        val json = toJson(node)

        assertTrue(json.contains("\"_type\""), "JSON should contain type discriminator")
        assertTrue(json.contains("\"tab\""), "JSON should contain TabNode type")
    }

    @Test
    fun `serialization includes type discriminator for PaneNode`() {
        val node = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("p", "panes", HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val json = toJson(node)

        assertTrue(json.contains("\"_type\""), "JSON should contain type discriminator")
        assertTrue(json.contains("\"pane\""), "JSON should contain PaneNode type")
    }

    @Test
    fun `serialized JSON contains key property`() {
        val node = ScreenNode("my-unique-key", null, HomeDestination)

        val json = toJson(node)

        assertTrue(json.contains("\"key\""), "JSON should contain key property")
        assertTrue(json.contains("my-unique-key"), "JSON should contain the key value")
    }

    @Test
    fun `pretty print JSON is human readable`() {
        val node = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(ScreenNode("s1", "stack", HomeDestination))
        )

        // Create a pretty print JSON with our test destination module
        val prettyJson = kotlinx.serialization.json.Json {
            serializersModule = com.jermey.quo.vadis.core.navigation.core.navNodeSerializersModule + testDestinationModule
            classDiscriminator = "_type"
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }
        val json = prettyJson.encodeToString(NavNode.serializer(), node)

        assertTrue(json.contains("\n"), "Pretty JSON should contain newlines")
        assertTrue(json.contains("  "), "Pretty JSON should contain indentation")
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    fun `StackNode with mixed child types round-trip`() {
        val original = StackNode(
            key = "mixed",
            parentKey = null,
            children = listOf(
                ScreenNode("screen", "mixed", HomeDestination),
                StackNode("nested-stack", "mixed", emptyList()),
                TabNode(
                    key = "nested-tabs",
                    parentKey = "mixed",
                    stacks = listOf(StackNode("t0", "nested-tabs", emptyList())),
                    activeStackIndex = 0
                )
            )
        )

        val json = toJson(original)
        val restored = fromJson(json) as StackNode

        assertEquals(3, restored.size)
        assertTrue(restored.children[0] is ScreenNode)
        assertTrue(restored.children[1] is StackNode)
        assertTrue(restored.children[2] is TabNode)
    }

    @Test
    fun `special characters in keys preserved`() {
        val specialKey = "key-with/special:characters@test#1"
        val original = ScreenNode(specialKey, null, HomeDestination)

        val json = toJson(original)
        val restored = fromJson(json)

        assertEquals(specialKey, restored.key)
    }

    @Test
    fun `unicode characters in keys preserved`() {
        val unicodeKey = "screen-日本語-🎉-العربية"
        val original = ScreenNode(unicodeKey, null, HomeDestination)

        val json = toJson(original)
        val restored = fromJson(json)

        assertEquals(unicodeKey, restored.key)
    }

    @Test
    fun `very long key preserved`() {
        val longKey = "a".repeat(1000)
        val original = ScreenNode(longKey, null, HomeDestination)

        val json = toJson(original)
        val restored = fromJson(json)

        assertEquals(longKey, restored.key)
    }

    @Test
    fun `TabNode with many tabs round-trip`() {
        val stacks = (0 until 20).map { index ->
            StackNode("tab-$index", "tabs", listOf(
                ScreenNode("screen-$index", "tab-$index", HomeDestination)
            ))
        }
        val original = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = stacks,
            activeStackIndex = 15
        )

        val json = toJson(original)
        val restored = fromJson(json) as TabNode

        assertEquals(20, restored.tabCount)
        assertEquals(15, restored.activeStackIndex)
    }

    @Test
    fun `StackNode with many children round-trip`() {
        val children = (0 until 100).map { index ->
            ScreenNode("screen-$index", "stack", HomeDestination)
        }
        val original = StackNode("stack", null, children)

        val json = toJson(original)
        val restored = fromJson(json) as StackNode

        assertEquals(100, restored.size)
        assertEquals("screen-99", restored.activeChild?.key)
    }

    // =========================================================================
    // CUSTOM JSON CONFIGURATION TESTS
    // =========================================================================

    @Test
    fun `createJson with additional module works`() {
        // Create a custom module that includes our test destinations
        // In real use, this would register app-specific Destination subclasses
        val customModule = SerializersModule {
            polymorphic(Destination::class) {
                subclass(HomeDestination::class)
            }
        }

        val customJson = NavNodeSerializer.createJson(customModule)
        val original = ScreenNode("test", null, HomeDestination)

        val json = customJson.encodeToString(NavNode.serializer(), original)
        val restored = customJson.decodeFromString(NavNode.serializer(), json)

        assertEquals(original.key, restored.key)
    }

    @Test
    fun `DestinationSerializerRegistry clear works`() {
        // Register a module
        DestinationSerializerRegistry.register(SerializersModule { })

        // Clear should not throw
        DestinationSerializerRegistry.clear()

        // combinedModule should still work
        val module = DestinationSerializerRegistry.combinedModule
        assertNotNull(module)
    }

    @Test
    fun `json config ignores unknown keys`() {
        // Create JSON with an extra unknown field and a valid destination
        val jsonWithUnknown = """
            {"_type":"screen","key":"test","parentKey":null,"destination":{"_type":"home"},"unknownField":"value"}
        """.trimIndent()

        // Should not throw due to ignoreUnknownKeys = true
        val restored = testJson.decodeFromString(NavNode.serializer(), jsonWithUnknown)

        assertEquals("test", restored.key)
    }

    // =========================================================================
    // INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `full app state round-trip simulation`() {
        // Simulate a typical app state with tabs, navigation stacks, and adaptive panes
        val appState = StackNode(
            key = "app-root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "main-tabs",
                    parentKey = "app-root",
                    stacks = listOf(
                        // Home tab with nested navigation
                        StackNode(
                            key = "home-stack",
                            parentKey = "main-tabs",
                            children = listOf(
                                ScreenNode("home-main", "home-stack", HomeDestination),
                                ScreenNode("home-detail", "home-stack", DetailDestination)
                            )
                        ),
                        // Profile tab with adaptive panes
                        StackNode(
                            key = "profile-stack",
                            parentKey = "main-tabs",
                            children = listOf(
                                PaneNode(
                                    key = "profile-panes",
                                    parentKey = "profile-stack",
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            content = StackNode(
                                                key = "profile-list",
                                                parentKey = "profile-panes",
                                                children = listOf(
                                                    ScreenNode("list-screen", "profile-list", ListDestination)
                                                )
                                            ),
                                            adaptStrategy = AdaptStrategy.Hide
                                        ),
                                        PaneRole.Supporting to PaneConfiguration(
                                            content = ScreenNode("detail-screen", "profile-panes", DetailDestination),
                                            adaptStrategy = AdaptStrategy.Levitate
                                        )
                                    ),
                                    activePaneRole = PaneRole.Supporting,
                                    backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange
                                )
                            )
                        ),
                        // Settings tab (simple)
                        StackNode(
                            key = "settings-stack",
                            parentKey = "main-tabs",
                            children = listOf(
                                ScreenNode("settings-main", "settings-stack", SettingsDestination)
                            )
                        )
                    ),
                    activeStackIndex = 1 // Profile tab is active
                )
            )
        )

        // Serialize
        val json = toJson(appState)
        assertNotNull(json)
        assertTrue(json.isNotEmpty())

        // Deserialize
        val restored = fromJson(json) as StackNode

        // Verify root
        assertEquals("app-root", restored.key)
        assertEquals(1, restored.size)

        // Verify tabs
        val tabs = restored.activeChild as TabNode
        assertEquals("main-tabs", tabs.key)
        assertEquals(3, tabs.tabCount)
        assertEquals(1, tabs.activeStackIndex)

        // Verify active tab (profile) has panes
        val profileStack = tabs.activeStack
        assertEquals("profile-stack", profileStack.key)
        val panes = profileStack.activeChild as PaneNode
        assertEquals("profile-panes", panes.key)
        assertEquals(PaneRole.Supporting, panes.activePaneRole)
        assertEquals(PaneBackBehavior.PopUntilCurrentDestinationChange, panes.backBehavior)

        // Verify home tab preserved history
        val homeStack = tabs.stackAt(0)
        assertEquals(2, homeStack.size)
        assertEquals("home-detail", homeStack.activeChild?.key)
    }
}
