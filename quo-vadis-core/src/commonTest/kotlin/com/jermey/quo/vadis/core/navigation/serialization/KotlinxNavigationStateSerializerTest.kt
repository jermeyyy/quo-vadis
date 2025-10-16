package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.SimpleDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for kotlinx.serialization-based NavigationStateSerializer
 */
class KotlinxNavigationStateSerializerTest {

    private val serializer = KotlinxNavigationStateSerializer()

    @Test
    fun `serialize and deserialize destination`() {
        val destination = SimpleDestination(
            route = "test_route",
            arguments = mapOf("arg1" to "value1", "arg2" to "value2")
        )

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized)
        assertEquals(destination.route, deserialized.route)
        assertEquals(destination.arguments.size, deserialized.arguments.size)
    }

    @Test
    fun `serialize and deserialize empty destination`() {
        val destination = SimpleDestination(route = "empty_route")

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized)
        assertEquals(destination.route, deserialized.route)
        assertTrue(deserialized.arguments.isEmpty())
    }

    @Test
    fun `serialize and deserialize backstack`() {
        val entries = listOf(
            BackStackEntry(
                id = "entry1",
                destination = SimpleDestination("route1", mapOf("key1" to "val1"))
            ),
            BackStackEntry(
                id = "entry2",
                destination = SimpleDestination("route2", mapOf("key2" to "val2"))
            ),
            BackStackEntry(
                id = "entry3",
                destination = SimpleDestination("route3")
            )
        )

        val serialized = serializer.serializeBackStack(entries)
        val deserialized = serializer.deserializeBackStack(serialized)

        assertEquals(entries.size, deserialized.size)
        entries.forEachIndexed { index, entry ->
            assertEquals(entry.id, deserialized[index].id)
            assertEquals(entry.destination.route, deserialized[index].destination.route)
        }
    }

    @Test
    fun `deserialize empty backstack string returns empty list`() {
        val deserialized = serializer.deserializeBackStack("")
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun `deserialize invalid json returns empty list`() {
        val deserialized = serializer.deserializeBackStack("invalid json {]}")
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun `deserialize invalid destination json returns null`() {
        val deserialized = serializer.deserializeDestination("not valid json")
        assertEquals(null, deserialized)
    }

    @Test
    fun `serialized format is valid JSON`() {
        val destination = SimpleDestination(
            route = "test",
            arguments = mapOf("arg" to "value")
        )
        val serialized = serializer.serializeDestination(destination)
        
        // Should be able to parse it again
        val deserialized = serializer.deserializeDestination(serialized)
        assertNotNull(deserialized)
    }
}
