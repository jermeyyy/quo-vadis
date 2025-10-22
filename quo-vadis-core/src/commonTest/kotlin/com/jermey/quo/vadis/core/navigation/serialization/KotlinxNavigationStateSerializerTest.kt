package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import com.jermey.quo.vadis.core.navigation.core.BasicDestination
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for kotlinx.serialization-based NavigationStateSerializer
 */
class KotlinxNavigationStateSerializerTest {

    private val serializer = KotlinxNavigationStateSerializer()

    @Serializable
    data class TestData(val arg1: String, val arg2: String)

    @Test
    fun `serialize and deserialize typed destination`() {
        val destination = TypedDestination(
            route = "test_route",
            data = TestData("value1", "value2")
        )

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized)
        assertEquals(destination.route, deserialized.route)
        assertNotNull(deserialized.data)
    }

    @Test
    fun `serialize and deserialize empty destination`() {
        val destination = BasicDestination(route = "empty_route")

        val serialized = serializer.serializeDestination(destination)
        val deserialized = serializer.deserializeDestination(serialized)

        assertNotNull(deserialized)
        assertEquals(destination.route, deserialized.route)
        assertEquals(null, deserialized.data)
    }

    @Test
    fun `serialize and deserialize backstack`() {
        val entries = listOf(
            BackStackEntry(
                id = "entry1",
                destination = TypedDestination("route1", TestData("val1", "val2"))
            ),
            BackStackEntry(
                id = "entry2",
                destination = TypedDestination("route2", "simple string data")
            ),
            BackStackEntry(
                id = "entry3",
                destination = BasicDestination("route3")
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
        val destination = TypedDestination(
            route = "test",
            data = "simple string value"
        )
        val serialized = serializer.serializeDestination(destination)
        
        // Should be able to parse it again
        val deserialized = serializer.deserializeDestination(serialized)
        assertNotNull(deserialized)
    }
}
