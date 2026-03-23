@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data object CbStackDest : NavDestination

private data object CbTabsDest : NavDestination

private data object CbPanesDest : NavDestination

private data object CbScreenDest : NavDestination

private data object CbTab1 : NavDestination

private data object CbPrimaryDest : NavDestination

class ContainerBuilderTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    test("Stack holds destinationClass, scopeKey, and screens") {
        val screens = listOf(
            StackScreenEntry(
                destination = CbScreenDest,
                destinationClass = CbScreenDest::class,
                key = "screen-0"
            )
        )
        val stack = ContainerBuilder.Stack(
            destinationClass = CbStackDest::class,
            scopeKey = ScopeKey("stack-scope"),
            screens = screens
        )

        stack.shouldBeInstanceOf<ContainerBuilder.Stack>()
        stack.destinationClass shouldBe CbStackDest::class
        stack.scopeKey shouldBe ScopeKey("stack-scope")
        stack.screens.size shouldBe 1
        stack.screens[0].key shouldBe "screen-0"
    }

    test("Tabs holds destinationClass, scopeKey, and config") {
        val builder = TabsBuilder()
        builder.initialTab = 1
        builder.tab(CbTab1, title = "Tab")
        val config = builder.build()

        val tabs = ContainerBuilder.Tabs(
            destinationClass = CbTabsDest::class,
            scopeKey = ScopeKey("tabs-scope"),
            config = config
        )

        tabs.shouldBeInstanceOf<ContainerBuilder.Tabs>()
        tabs.destinationClass shouldBe CbTabsDest::class
        tabs.scopeKey shouldBe ScopeKey("tabs-scope")
        tabs.config.initialTab shouldBe 1
        tabs.config.tabs.size shouldBe 1
    }

    test("Panes holds destinationClass, scopeKey, and config") {
        val panesBuilder = PanesBuilder()
        panesBuilder.primary { root(CbPrimaryDest) }
        val config = panesBuilder.build()

        val panes = ContainerBuilder.Panes(
            destinationClass = CbPanesDest::class,
            scopeKey = ScopeKey("panes-scope"),
            config = config
        )

        panes.shouldBeInstanceOf<ContainerBuilder.Panes>()
        panes.destinationClass shouldBe CbPanesDest::class
        panes.scopeKey shouldBe ScopeKey("panes-scope")
        panes.config.panes.size shouldBe 1
    }

    test("all subtypes are ContainerBuilder") {
        val stack = ContainerBuilder.Stack(
            destinationClass = CbStackDest::class,
            scopeKey = ScopeKey("s"),
            screens = emptyList()
        )
        val tabs = ContainerBuilder.Tabs(
            destinationClass = CbTabsDest::class,
            scopeKey = ScopeKey("t"),
            config = TabsBuilder().build()
        )
        val panes = ContainerBuilder.Panes(
            destinationClass = CbPanesDest::class,
            scopeKey = ScopeKey("p"),
            config = PanesBuilder().build()
        )

        stack.shouldBeInstanceOf<ContainerBuilder>()
        tabs.shouldBeInstanceOf<ContainerBuilder>()
        panes.shouldBeInstanceOf<ContainerBuilder>()
    }

    test("Stack with empty screens list") {
        val stack = ContainerBuilder.Stack(
            destinationClass = CbStackDest::class,
            scopeKey = ScopeKey("empty"),
            screens = emptyList()
        )

        stack.screens.size shouldBe 0
    }

    test("data class equality for Stack") {
        val screens = listOf(
            StackScreenEntry(destination = null, destinationClass = CbScreenDest::class, key = "k")
        )
        val a = ContainerBuilder.Stack(CbStackDest::class, ScopeKey("s"), screens)
        val b = ContainerBuilder.Stack(CbStackDest::class, ScopeKey("s"), screens)

        a shouldBe b
    }

    test("data class equality for Tabs") {
        val config = TabsBuilder().build()
        val a = ContainerBuilder.Tabs(CbTabsDest::class, ScopeKey("t"), config)
        val b = ContainerBuilder.Tabs(CbTabsDest::class, ScopeKey("t"), config)

        a shouldBe b
    }

    test("data class equality for Panes") {
        val config = PanesBuilder().build()
        val a = ContainerBuilder.Panes(CbPanesDest::class, ScopeKey("p"), config)
        val b = ContainerBuilder.Panes(CbPanesDest::class, ScopeKey("p"), config)

        a shouldBe b
    }

    test("abstract properties are accessible on sealed type") {
        val container: ContainerBuilder = ContainerBuilder.Stack(
            destinationClass = CbStackDest::class,
            scopeKey = ScopeKey("test"),
            screens = emptyList()
        )

        container.destinationClass shouldBe CbStackDest::class
        container.scopeKey shouldBe ScopeKey("test")
    }
})
