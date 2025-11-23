package com.jermey.quo.vadis.flowmvi.savedstate

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import com.jermey.quo.vadis.flowmvi.utils.emptyNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for NavigationStateSerializer.
 *
 * Tests serialization and deserialization of navigation state including:
 * - Empty state handling
 * - State with destinations
 * - JSON conversion
 * - Round-trip serialization
 */
class StateSerializerTest {
    
    private val serializer = DefaultNavigationStateSerializer()
    
    // Test destination implementation
    private class TestDestination(val name: String) : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
    
    // Test state implementation
    private class TestNavigationState(
        override val currentDestination: Destination?,
        override val backStackSize: Int,
        override val canGoBack: Boolean
    ) : NavigationState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NavigationState) return false
            return currentDestination == other.currentDestination &&
                   backStackSize == other.backStackSize &&
                   canGoBack == other.canGoBack
        }
        
        override fun hashCode(): Int {
            var result = currentDestination?.hashCode() ?: 0
            result = 31 * result + backStackSize
            result = 31 * result + canGoBack.hashCode()
            return result
        }
    }
    
    @Test
    fun `serialize empty state has null route and zero size`() {
        val state = emptyNavigationState()
        
        val serialized = serializer.serialize(state)
        
        assertNull(serialized.currentRoute, "Empty state should have null route")
        assertNull(serialized.destinationData, "Empty state should have null data")
        assertEquals(0, serialized.backStackSize, "Empty state should have zero back stack size")
        assertFalse(serialized.canGoBack, "Empty state should not allow going back")
        assertTrue(serialized.backStackRoutes.isEmpty(), "Empty state should have no back stack routes")
    }
    
    @Test
    fun `serialize state with destination captures route`() {
        val destination = TestDestination("home")
        val state = TestNavigationState(
            currentDestination = destination,
            backStackSize = 1,
            canGoBack = false
        )
        
        val serialized = serializer.serialize(state)
        
        assertEquals("TestDestination", serialized.currentRoute, "Should capture destination class name")
        assertEquals(1, serialized.backStackSize, "Should preserve back stack size")
        assertFalse(serialized.canGoBack, "Should preserve canGoBack flag")
    }
    
    @Test
    fun `serialize state with multiple entries in backstack`() {
        val destination = TestDestination("current")
        val state = TestNavigationState(
            currentDestination = destination,
            backStackSize = 3,
            canGoBack = true
        )
        
        val serialized = serializer.serialize(state)
        
        assertEquals("TestDestination", serialized.currentRoute)
        assertEquals(3, serialized.backStackSize, "Should preserve back stack size")
        assertTrue(serialized.canGoBack, "Should indicate back navigation is possible")
    }
    
    @Test
    fun `deserialize empty state returns null`() {
        val serialized = SerializableNavigationState(
            currentRoute = null,
            destinationData = null,
            backStackSize = 0,
            canGoBack = false,
            backStackRoutes = emptyList()
        )
        
        val state = serializer.deserialize(serialized)
        
        assertNull(state, "Empty serialized state should deserialize to null")
    }
    
    @Test
    fun `deserialize state with route creates state object`() {
        val serialized = SerializableNavigationState(
            currentRoute = "home",
            destinationData = null,
            backStackSize = 1,
            canGoBack = false,
            backStackRoutes = emptyList()
        )
        
        val state = serializer.deserialize(serialized)
        
        assertNotNull(state, "State with route should deserialize")
        assertEquals(1, state.backStackSize, "Back stack size should be preserved")
        assertFalse(state.canGoBack, "canGoBack flag should be preserved")
        assertNull(state.currentDestination, "Basic deserializer doesn't recreate destinations")
    }
    
    @Test
    fun `deserialize preserves backstack metadata`() {
        val serialized = SerializableNavigationState(
            currentRoute = "details",
            destinationData = null,
            backStackSize = 5,
            canGoBack = true,
            backStackRoutes = listOf("home", "list", "details")
        )
        
        val state = serializer.deserialize(serialized)
        
        assertNotNull(state, "State should deserialize")
        assertEquals(5, state.backStackSize, "Should preserve exact back stack size")
        assertTrue(state.canGoBack, "Should indicate back navigation is available")
    }
    
    @Test
    fun `roundtrip serialization preserves metadata`() {
        val originalState = TestNavigationState(
            currentDestination = TestDestination("test"),
            backStackSize = 2,
            canGoBack = true
        )
        
        val serialized = serializer.serialize(originalState)
        val deserialized = serializer.deserialize(serialized)
        
        assertNotNull(deserialized, "Deserialized state should not be null")
        assertEquals(originalState.backStackSize, deserialized.backStackSize, "Back stack size should match")
        assertEquals(originalState.canGoBack, deserialized.canGoBack, "canGoBack flag should match")
    }
    
    @Test
    fun `toJsonString produces valid JSON structure`() {
        val state = emptyNavigationState()
        
        val json = state.toJsonString(serializer)
        
        assertNotNull(json, "JSON string should not be null")
        assertTrue(json.contains("{"), "Should contain opening brace")
        assertTrue(json.contains("}"), "Should contain closing brace")
        assertTrue(json.contains("currentRoute"), "Should contain currentRoute field")
        assertTrue(json.contains("backStackSize"), "Should contain backStackSize field")
        assertTrue(json.contains("canGoBack"), "Should contain canGoBack field")
    }
    
    @Test
    fun `toJsonString with destination includes route`() {
        val state = TestNavigationState(
            currentDestination = TestDestination("profile"),
            backStackSize = 1,
            canGoBack = false
        )
        
        val json = state.toJsonString(serializer)
        
        assertTrue(json.contains("TestDestination"), "JSON should include destination class name")
        assertTrue(json.contains("\"backStackSize\":1"), "JSON should include back stack size")
    }
    
    @Test
    fun `toNavigationState from valid JSON deserializes correctly`() {
        val json = """
            {
                "currentRoute": "home",
                "destinationData": null,
                "backStackSize": 1,
                "canGoBack": false,
                "backStackRoutes": []
            }
        """.trimIndent()
        
        val state = json.toNavigationState(serializer)
        
        assertNotNull(state, "Valid JSON should deserialize to state")
        assertEquals(1, state.backStackSize, "Should parse back stack size")
        assertFalse(state.canGoBack, "Should parse canGoBack flag")
    }
    
    @Test
    fun `toNavigationState from invalid JSON returns null`() {
        val invalidJson = "{ invalid json structure"
        
        val state = invalidJson.toNavigationState(serializer)
        
        assertNull(state, "Invalid JSON should return null")
    }
    
    @Test
    fun `toNavigationState from empty string returns null`() {
        val state = "".toNavigationState(serializer)
        
        assertNull(state, "Empty string should return null")
    }
    
    @Test
    fun `toNavigationState with backstack metadata preserves values`() {
        val json = """
            {
                "currentRoute": "details",
                "destinationData": null,
                "backStackSize": 3,
                "canGoBack": true,
                "backStackRoutes": ["home", "list", "details"]
            }
        """.trimIndent()
        
        val state = json.toNavigationState(serializer)
        
        assertNotNull(state, "Should deserialize successfully")
        assertEquals(3, state.backStackSize, "Should preserve back stack size")
        assertTrue(state.canGoBack, "Should preserve canGoBack flag")
    }
    
    @Test
    fun `serialize then toJsonString produces consistent format`() {
        val state = TestNavigationState(
            currentDestination = TestDestination("settings"),
            backStackSize = 2,
            canGoBack = true
        )
        
        // Direct JSON conversion
        val json1 = state.toJsonString(serializer)
        
        // Manual serialize then encode
        val serialized = serializer.serialize(state)
        val json2 = kotlinx.serialization.json.Json.encodeToString(serialized)
        
        assertEquals(json1, json2, "Both methods should produce same JSON")
    }
}
