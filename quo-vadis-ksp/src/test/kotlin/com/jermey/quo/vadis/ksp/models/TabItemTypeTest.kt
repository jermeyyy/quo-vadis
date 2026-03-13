package com.jermey.quo.vadis.ksp.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabItemTypeTest {

    @Test
    fun `TabItemType has three values`() {
        assertEquals(3, TabItemType.entries.size)
    }

    @Test
    fun `TabItemType values are FLAT_SCREEN, NESTED_STACK, CONTAINER_REFERENCE`() {
        val names = TabItemType.entries.map { it.name }
        assertTrue("FLAT_SCREEN" in names)
        assertTrue("NESTED_STACK" in names)
        assertTrue("CONTAINER_REFERENCE" in names)
    }

    @Test
    fun `FLAT_SCREEN is the first enum value`() {
        assertEquals(TabItemType.FLAT_SCREEN, TabItemType.entries[0])
    }

    @Test
    fun `NESTED_STACK is the second enum value`() {
        assertEquals(TabItemType.NESTED_STACK, TabItemType.entries[1])
    }

    @Test
    fun `CONTAINER_REFERENCE is the third enum value`() {
        assertEquals(TabItemType.CONTAINER_REFERENCE, TabItemType.entries[2])
    }
}
