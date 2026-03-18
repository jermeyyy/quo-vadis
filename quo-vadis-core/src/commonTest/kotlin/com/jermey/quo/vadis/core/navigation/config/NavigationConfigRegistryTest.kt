package com.jermey.quo.vadis.core.navigation.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [NavigationConfigRegistry].
 *
 * Verifies the internal registry used by KSP-generated aggregated configs
 * to enable runtime resolution via [navigationConfig].
 */
@OptIn(InternalQuoVadisApi::class)
class NavigationConfigRegistryTest {

    private object TestRootA
    private object TestRootB
    private object UnregisteredRoot

    @Test
    fun `register and get returns registered config`() {
        val config = NavigationConfig.Empty
        NavigationConfigRegistry.register(TestRootA::class, config)

        val result = NavigationConfigRegistry.get(TestRootA::class)
        assertNotNull(result)
        assertEquals(config, result)
    }

    @Test
    fun `get returns null for unregistered root`() {
        val result = NavigationConfigRegistry.get(UnregisteredRoot::class)
        assertNull(result)
    }

    @Test
    fun `register overwrites previous registration`() {
        val config1 = NavigationConfig.Empty
        val config2 = NavigationConfig.Empty + NavigationConfig.Empty
        NavigationConfigRegistry.register(TestRootB::class, config1)
        NavigationConfigRegistry.register(TestRootB::class, config2)

        val result = NavigationConfigRegistry.get(TestRootB::class)
        assertEquals(config2, result)
    }
}
