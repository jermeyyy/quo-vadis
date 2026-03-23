@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private data object HomeScreen : NavDestination

private data object DetailScreen : NavDestination

private data object SettingsScreen : NavDestination

class StackBuilderTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    test("screen by instance adds entry with destination and class") {
        val builder = StackBuilder()
        builder.screen(HomeScreen)

        val entries = builder.build()
        entries shouldHaveSize 1
        entries[0].destination shouldBe HomeScreen
        entries[0].destinationClass shouldBe HomeScreen::class
    }

    test("screen by instance uses class simpleName as default key") {
        val builder = StackBuilder()
        builder.screen(HomeScreen)

        val entries = builder.build()
        entries[0].key shouldBe "HomeScreen"
    }

    test("screen by instance with explicit key uses provided key") {
        val builder = StackBuilder()
        builder.screen(HomeScreen, key = "custom-home")

        val entries = builder.build()
        entries[0].key shouldBe "custom-home"
    }

    test("screen by type adds entry with null destination and correct class") {
        val builder = StackBuilder()
        builder.screen<DetailScreen>()

        val entries = builder.build()
        entries shouldHaveSize 1
        entries[0].destination.shouldBeNull()
        entries[0].destinationClass shouldBe DetailScreen::class
    }

    test("screen by type uses class simpleName as default key") {
        val builder = StackBuilder()
        builder.screen<DetailScreen>()

        val entries = builder.build()
        entries[0].key shouldBe "DetailScreen"
    }

    test("screen by type with explicit key uses provided key") {
        val builder = StackBuilder()
        builder.screen<DetailScreen>(key = "detail-custom")

        val entries = builder.build()
        entries[0].key shouldBe "detail-custom"
    }

    test("multiple screens build in order") {
        val builder = StackBuilder()
        builder.screen(HomeScreen)
        builder.screen<DetailScreen>()
        builder.screen(SettingsScreen, key = "settings")

        val entries = builder.build()
        entries shouldHaveSize 3
        entries[0].destinationClass shouldBe HomeScreen::class
        entries[1].destinationClass shouldBe DetailScreen::class
        entries[2].destinationClass shouldBe SettingsScreen::class
        entries[2].key shouldBe "settings"
    }

    test("build returns empty list when no screens added") {
        val builder = StackBuilder()
        builder.build().shouldBeEmpty()
    }

    test("build returns a copy - modifying builder after build does not affect result") {
        val builder = StackBuilder()
        builder.screen(HomeScreen)
        val result = builder.build()

        builder.screen<DetailScreen>()

        result shouldHaveSize 1
    }

    test("screen by instance stores destination reference") {
        val builder = StackBuilder()
        builder.screen(HomeScreen)

        val entries = builder.build()
        entries[0].destination.shouldNotBeNull()
        entries[0].destination shouldBe HomeScreen
    }
})
