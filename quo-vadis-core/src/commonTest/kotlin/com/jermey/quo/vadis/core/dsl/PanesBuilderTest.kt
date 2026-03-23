@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private data object PaneListDestination : NavDestination

private data object PaneDetailDestination : NavDestination

private data object PaneExtraDestination : NavDestination

class PanesBuilderTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    test("default initialPane is Primary") {
        val builder = PanesBuilder()
        builder.initialPane shouldBe PaneRole.Primary
    }

    test("default backBehavior is PopLatest") {
        val builder = PanesBuilder()
        builder.backBehavior shouldBe PaneBackBehavior.PopLatest
    }

    test("primary pane is configured correctly") {
        val builder = PanesBuilder()
        builder.primary(weight = 0.4f, minWidth = 300.dp) {
            root(PaneListDestination)
            alwaysVisible()
        }

        val config = builder.build()
        config.panes shouldContainKey PaneRole.Primary
        val pane = config.panes[PaneRole.Primary]!!
        pane.role shouldBe PaneRole.Primary
        pane.weight shouldBe 0.4f
        pane.minWidth shouldBe 300.dp
        pane.content.rootDestination shouldBe PaneListDestination
        pane.content.isAlwaysVisible.shouldBeTrue()
    }

    test("secondary pane is configured correctly") {
        val builder = PanesBuilder()
        builder.secondary(weight = 0.6f) {
            root(PaneDetailDestination)
        }

        val config = builder.build()
        config.panes shouldContainKey PaneRole.Supporting
        val pane = config.panes[PaneRole.Supporting]!!
        pane.role shouldBe PaneRole.Supporting
        pane.weight shouldBe 0.6f
        pane.minWidth shouldBe 0.dp
        pane.content.rootDestination shouldBe PaneDetailDestination
        pane.content.isAlwaysVisible.shouldBeFalse()
    }

    test("extra pane is configured correctly") {
        val builder = PanesBuilder()
        builder.extra(weight = 0.25f, minWidth = 200.dp) {
            root(PaneExtraDestination)
        }

        val config = builder.build()
        config.panes shouldContainKey PaneRole.Extra
        val pane = config.panes[PaneRole.Extra]!!
        pane.role shouldBe PaneRole.Extra
        pane.weight shouldBe 0.25f
        pane.minWidth shouldBe 200.dp
        pane.content.rootDestination shouldBe PaneExtraDestination
    }

    test("tertiary is alias for secondary") {
        val builder = PanesBuilder()
        builder.tertiary(weight = 0.5f) {
            root(PaneDetailDestination)
        }

        val config = builder.build()
        config.panes shouldContainKey PaneRole.Supporting
        val pane = config.panes[PaneRole.Supporting]!!
        pane.content.rootDestination shouldBe PaneDetailDestination
    }

    test("initialPane setting is preserved in build") {
        val builder = PanesBuilder()
        builder.initialPane = PaneRole.Supporting
        builder.primary { root(PaneListDestination) }
        builder.secondary { root(PaneDetailDestination) }

        val config = builder.build()
        config.initialPane shouldBe PaneRole.Supporting
    }

    test("backBehavior setting is preserved in build") {
        val builder = PanesBuilder()
        builder.backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        builder.primary { root(PaneListDestination) }

        val config = builder.build()
        config.backBehavior shouldBe PaneBackBehavior.PopUntilScaffoldValueChange
    }

    test("three-pane layout") {
        val builder = PanesBuilder()
        builder.primary(weight = 0.25f) { root(PaneListDestination) }
        builder.secondary(weight = 0.35f) { root(PaneDetailDestination) }
        builder.extra(weight = 0.4f) { root(PaneExtraDestination) }

        val config = builder.build()
        config.panes shouldHaveSize 3
        config.panes shouldContainKey PaneRole.Primary
        config.panes shouldContainKey PaneRole.Supporting
        config.panes shouldContainKey PaneRole.Extra
    }

    test("pane without root destination has null rootDestination") {
        val builder = PanesBuilder()
        builder.primary {
            // no root set
        }

        val config = builder.build()
        val pane = config.panes[PaneRole.Primary]!!
        pane.content.rootDestination.shouldBeNull()
    }

    test("default weight is 1f and default minWidth is 0.dp") {
        val builder = PanesBuilder()
        builder.primary { root(PaneListDestination) }

        val config = builder.build()
        val pane = config.panes[PaneRole.Primary]!!
        pane.weight shouldBe 1f
        pane.minWidth shouldBe 0.dp
    }

    test("build with no panes returns empty map") {
        val builder = PanesBuilder()
        val config = builder.build()
        config.panes shouldHaveSize 0
        config.initialPane shouldBe PaneRole.Primary
        config.backBehavior shouldBe PaneBackBehavior.PopLatest
    }

    test("all PaneBackBehavior values can be set") {
        PaneBackBehavior.entries.forEach { behavior ->
            val builder = PanesBuilder()
            builder.backBehavior = behavior
            builder.build().backBehavior shouldBe behavior
        }
    }
})
