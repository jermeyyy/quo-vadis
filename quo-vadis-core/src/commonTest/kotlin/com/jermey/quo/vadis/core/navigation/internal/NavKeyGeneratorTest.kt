@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class NavKeyGeneratorTest : FunSpec({

    beforeTest {
        NavKeyGenerator.reset()
    }

    test("generate returns key with default prefix") {
        val key = NavKeyGenerator.generate()
        key shouldStartWith "node-"
    }

    test("generate returns key with custom debug label") {
        val key = NavKeyGenerator.generate("profile")
        key shouldStartWith "profile-"
    }

    test("sequential calls return incrementing keys") {
        val key0 = NavKeyGenerator.generate()
        val key1 = NavKeyGenerator.generate()
        val key2 = NavKeyGenerator.generate()
        key0 shouldBe "node-0"
        key1 shouldBe "node-1"
        key2 shouldBe "node-2"
    }

    test("sequential calls with custom label return incrementing keys") {
        val key0 = NavKeyGenerator.generate("home")
        val key1 = NavKeyGenerator.generate("home")
        key0 shouldBe "home-0"
        key1 shouldBe "home-1"
    }

    test("different prefixes share the same counter") {
        val key0 = NavKeyGenerator.generate("home")
        val key1 = NavKeyGenerator.generate("profile")
        key0 shouldBe "home-0"
        key1 shouldBe "profile-1"
    }

    test("reset sets counter back to zero") {
        NavKeyGenerator.generate()
        NavKeyGenerator.generate()
        NavKeyGenerator.reset()
        val key = NavKeyGenerator.generate()
        key shouldBe "node-0"
    }

    test("generate with null debug label uses default prefix") {
        val key = NavKeyGenerator.generate(null)
        key shouldBe "node-0"
    }
})
