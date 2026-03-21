package com.jermey.quo.vadis.ksp.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class TabItemTypeTest : FunSpec({

    test("TabItemType has three values") {
        TabItemType.entries.size shouldBe 3
    }

    test("TabItemType values are DESTINATION, STACK, TABS") {
        val names = TabItemType.entries.map { it.name }
        names.shouldContain("DESTINATION")
        names.shouldContain("STACK")
        names.shouldContain("TABS")
    }

    test("DESTINATION is the first enum value") {
        TabItemType.entries[0] shouldBe TabItemType.DESTINATION
    }

    test("STACK is the second enum value") {
        TabItemType.entries[1] shouldBe TabItemType.STACK
    }

    test("TABS is the third enum value") {
        TabItemType.entries[2] shouldBe TabItemType.TABS
    }
})
