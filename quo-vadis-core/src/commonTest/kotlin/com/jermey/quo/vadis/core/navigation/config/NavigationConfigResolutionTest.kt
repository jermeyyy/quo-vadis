package com.jermey.quo.vadis.core.navigation.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [navigationConfig] inline reified resolution function.
 *
 * Verifies that the function correctly resolves configs from
 * [NavigationConfigRegistry] and provides descriptive errors
 * when resolution fails.
 */
@OptIn(InternalQuoVadisApi::class)
class NavigationConfigResolutionTest {

    private object RegisteredRoot
    private object UnregisteredRoot

    @Test
    fun `navigationConfig resolves registered config via registry`() {
        val config = NavigationConfig.Empty
        NavigationConfigRegistry.register(RegisteredRoot::class, config)

        val result = navigationConfig<RegisteredRoot>()
        assertNotNull(result)
    }

    @Test
    fun `navigationConfig throws for unregistered root`() {
        val exception = assertFailsWith<IllegalStateException> {
            navigationConfig<UnregisteredRoot>()
        }
        assertTrue(
            exception.message?.contains("could not resolve") == true,
            "Error message should describe the resolution failure",
        )
    }
}
