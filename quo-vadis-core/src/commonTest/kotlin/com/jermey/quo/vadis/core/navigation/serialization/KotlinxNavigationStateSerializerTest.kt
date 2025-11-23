package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.RouteRegistry
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for kotlinx.serialization-based NavigationStateSerializer.
 * 
 * Tests serialization and deserialization of:
 * - Simple destinations without data
 * - Typed destinations with serializable data
 * - Full backstack with mixed destination types
 * - Error handling for invalid data
 */
class KotlinxNavigationStateSerializerTest {

    private val serializer = KotlinxNavigationStateSerializer()

    @Serializable
    data class TestData(val arg1: String, val arg2: Int)

    // Test destination without data
    private class SimpleDestination(
        private val routeName: String
    ) : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // Test destination with typed data
    private class TestTypedDestination(
        val testData: TestData
    ) : TypedDestination<TestData> {
        override val data: TestData = testData
        override val transition: NavigationTransition? = null
    }

    @BeforeTest
    fun setup() {
        // Register test routes manually since we don't use KSP in tests
        RouteRegistry.register(SimpleDestination::class, "simple_route")
        RouteRegistry.register(TestTypedDestination::class, "test_typed_route")
    }

    @Test
    fun `serialize and deserialize simple destination without data`() {
        val destination = SimpleDestination("simple_route")

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized, "Deserialized destination should not be null")
        assertNull(deserialized.data, "Simple destination should have null data")
    }

    @Test
    fun `serialize and deserialize typed destination with data`() {
        val testData = TestData("value1", 42)
        val destination = TestTypedDestination(testData)

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized, "Deserialized destination should not be null")
        assertNotNull(deserialized.data, "Typed destination should have data")
        assertTrue(deserialized is TypedDestination<*>, "Should be restored as TypedDestination")
    }

    @Test
    fun `serialize and deserialize backstack with mixed destinations`() {
        val entries = listOf(
            BackStackEntry(
                id = "entry1",
                destination = SimpleDestination("route1")
            ),
            BackStackEntry(
                id = "entry2",
                destination = TestTypedDestination(TestData("test", 123))
            ),
            BackStackEntry(
                id = "entry3",
                destination = SimpleDestination("route3")
            )
        )

        val serialized = serializer.serializeBackStack(entries)
        val deserialized = serializer.deserializeBackStack(serialized)

        assertEquals(entries.size, deserialized.size, "Should have same number of entries")
        
        // Verify IDs are preserved
        entries.forEachIndexed { index, entry ->
            assertEquals(entry.id, deserialized[index].id, "Entry ID should match")
            assertNotNull(deserialized[index].destination, "Destination should be restored")
        }
    }

    @Test
    fun `serialize backstack preserves entry order`() {
        val entries = listOf(
            BackStackEntry(id = "first", destination = SimpleDestination("a")),
            BackStackEntry(id = "second", destination = SimpleDestination("b")),
            BackStackEntry(id = "third", destination = SimpleDestination("c"))
        )

        val serialized = serializer.serializeBackStack(entries)
        val deserialized = serializer.deserializeBackStack(serialized)

        assertEquals(3, deserialized.size, "Should have 3 entries")
        assertEquals("first", deserialized[0].id, "First entry should be first")
        assertEquals("second", deserialized[1].id, "Second entry should be second")
        assertEquals("third", deserialized[2].id, "Third entry should be third")
    }

    @Test
    fun `deserialize empty backstack string returns empty list`() {
        val deserialized = serializer.deserializeBackStack("")
        assertTrue(deserialized.isEmpty(), "Empty string should return empty list")
    }

    @Test
    fun `deserialize blank backstack string returns empty list`() {
        val deserialized = serializer.deserializeBackStack("   ")
        assertTrue(deserialized.isEmpty(), "Blank string should return empty list")
    }

    @Test
    fun `deserialize invalid json returns empty list`() {
        val deserialized = serializer.deserializeBackStack("invalid json {]}")
        assertTrue(deserialized.isEmpty(), "Invalid JSON should return empty list")
    }

    @Test
    fun `deserialize malformed json returns empty list`() {
        val deserialized = serializer.deserializeBackStack("{not a list}")
        assertTrue(deserialized.isEmpty(), "Malformed JSON should return empty list")
    }

    @Test
    fun `deserialize invalid destination json returns null`() {
        val deserialized = serializer.deserializeDestination("not valid json")
        assertNull(deserialized, "Invalid JSON should return null")
    }

    @Test
    fun `deserialize empty destination string returns null`() {
        val deserialized = serializer.deserializeDestination("")
        assertNull(deserialized, "Empty string should return null")
    }

    @Test
    fun `serialized destination is valid JSON`() {
        val destination = SimpleDestination("test_route")
        val serialized = serializer.serializeDestination(destination)
        
        // Should contain JSON structure
        assertTrue(serialized.contains("{"), "Should be JSON object")
        assertTrue(serialized.contains("}"), "Should be closed JSON object")
        
        // Should be able to deserialize it back
        val deserialized = serializer.deserializeDestination(serialized)
        assertNotNull(deserialized, "Should deserialize successfully")
    }

    @Test
    fun `serialized backstack is valid JSON`() {
        val entries = listOf(
            BackStackEntry(id = "test", destination = SimpleDestination("route"))
        )
        val serialized = serializer.serializeBackStack(entries)
        
        // Should contain JSON array structure
        assertTrue(serialized.contains("["), "Should be JSON array")
        assertTrue(serialized.contains("]"), "Should be closed JSON array")
        
        // Should be able to deserialize it back
        val deserialized = serializer.deserializeBackStack(serialized)
        assertEquals(1, deserialized.size, "Should deserialize successfully")
    }

    @Test
    fun `round trip serialization preserves destination properties`() {
        val destination = SimpleDestination("my_route")
        
        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)
        
        assertNotNull(deserialized, "Destination should be restored")
        // Data and transition should be preserved
        assertEquals(destination.data, deserialized.data, "Data should match")
        assertEquals(destination.transition, deserialized.transition, "Transition should match")
    }

    @Test
    fun `empty backstack serializes and deserializes correctly`() {
        val emptyList = emptyList<BackStackEntry>()
        
        val serialized = serializer.serializeBackStack(emptyList)
        val deserialized = serializer.deserializeBackStack(serialized)
        
        assertTrue(deserialized.isEmpty(), "Empty list should remain empty")
    }
}
