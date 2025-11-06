package com.jermey.quo.vadis.flowmvi.savedstate

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for state restoration functionality.
 *
 * Tests SavedStateManager implementations.
 */
class RestorationPluginTest {

    private lateinit var stateManager: InMemorySavedStateManager

    @BeforeTest
    fun setup() {
        stateManager = InMemorySavedStateManager()
    }

    @Test
    fun `save and restore string`() = runTest {
        stateManager.save("test_key", "test_value")

        val restored = stateManager.restore("test_key")

        assertEquals("test_value", restored)
    }

    @Test
    fun `save and restore json string`() = runTest {
        val jsonState =
            """{"currentRoute":"home","destinationData":null,""" +
                    """"backStackSize":1,"canGoBack":false,"backStackRoutes":[]}"""

        stateManager.save("nav_state", jsonState)

        val restored = stateManager.restore("nav_state")

        assertEquals(jsonState, restored)
    }

    @Test
    fun `restore non-existent key returns null`() = runTest {
        val restored = stateManager.restore("non_existent")

        assertNull(restored)
    }

    @Test
    fun `exists returns correct value`() = runTest {
        assertFalse(stateManager.exists("test_key"))

        stateManager.save("test_key", "value")

        assertTrue(stateManager.exists("test_key"))
    }

    @Test
    fun `delete removes key`() = runTest {
        stateManager.save("test_key", "value")
        assertTrue(stateManager.exists("test_key"))

        stateManager.delete("test_key")

        assertFalse(stateManager.exists("test_key"))
    }

    @Test
    fun `clearAll removes all keys`() = runTest {
        stateManager.save("key1", "value1")
        stateManager.save("key2", "value2")
        stateManager.save("key3", "value3")

        stateManager.clearAll()

        assertFalse(stateManager.exists("key1"))
        assertFalse(stateManager.exists("key2"))
        assertFalse(stateManager.exists("key3"))
    }

    @Test
    fun `multiple keys are independent`() = runTest {
        stateManager.save("key1", "value1")
        stateManager.save("key2", "value2")

        val restored1 = stateManager.restore("key1")
        val restored2 = stateManager.restore("key2")

        assertEquals("value1", restored1)
        assertEquals("value2", restored2)

        stateManager.delete("key1")

        assertFalse(stateManager.exists("key1"))
        assertTrue(stateManager.exists("key2"))
    }
}
