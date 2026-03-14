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
    fun `TabItemType values are DESTINATION, STACK, TABS`() {
        val names = TabItemType.entries.map { it.name }
        assertTrue("DESTINATION" in names)
        assertTrue("STACK" in names)
        assertTrue("TABS" in names)
    }

    @Test
    fun `DESTINATION is the first enum value`() {
        assertEquals(TabItemType.DESTINATION, TabItemType.entries[0])
    }

    @Test
    fun `STACK is the second enum value`() {
        assertEquals(TabItemType.STACK, TabItemType.entries[1])
    }

    @Test
    fun `TABS is the third enum value`() {
        assertEquals(TabItemType.TABS, TabItemType.entries[2])
    }
}
