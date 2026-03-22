@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data object Tab1Dest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private data object Tab2Dest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private data object Tab3Dest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private data object NestedScreen1 : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private data object NestedScreen2 : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private data object ContainerDest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class TabsBuilderTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    test("default initialTab is 0") {
        val builder = TabsBuilder()
        builder.initialTab shouldBe 0
    }

    test("flat tab adds FlatScreen entry") {
        val builder = TabsBuilder()
        builder.tab(Tab1Dest, title = "Tab 1", icon = "icon1")

        val config = builder.build()
        config.tabs shouldHaveSize 1
        val tab = config.tabs[0]
        tab.shouldBeInstanceOf<TabEntry.FlatScreen>()
        tab.destination shouldBe Tab1Dest
        tab.destinationClass shouldBe Tab1Dest::class
        tab.title shouldBe "Tab 1"
        tab.icon shouldBe "icon1"
    }

    test("flat tab with null title and icon") {
        val builder = TabsBuilder()
        builder.tab(Tab1Dest)

        val config = builder.build()
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.FlatScreen>()
        tab.title.shouldBeNull()
        tab.icon.shouldBeNull()
    }

    test("nested stack tab adds NestedStack entry") {
        val builder = TabsBuilder()
        builder.tab(Tab1Dest, title = "Home", icon = null) {
            screen<NestedScreen1>()
            screen<NestedScreen2>()
        }

        val config = builder.build()
        config.tabs shouldHaveSize 1
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.NestedStack>()
        tab.rootDestination shouldBe Tab1Dest
        tab.destinationClass shouldBe Tab1Dest::class
        tab.title shouldBe "Home"
        tab.screens shouldHaveSize 2
        tab.screens[0].destinationClass shouldBe NestedScreen1::class
        tab.screens[1].destinationClass shouldBe NestedScreen2::class
    }

    test("containerTab by KClass adds ContainerReference entry") {
        val builder = TabsBuilder()
        builder.containerTab(ContainerDest::class, title = "Container", icon = "ic")

        val config = builder.build()
        config.tabs shouldHaveSize 1
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.ContainerReference>()
        tab.containerClass shouldBe ContainerDest::class
        tab.title shouldBe "Container"
        tab.icon shouldBe "ic"
    }

    test("containerTab reified adds ContainerReference entry") {
        val builder = TabsBuilder()
        builder.containerTab<ContainerDest>(title = "Reified", icon = null)

        val config = builder.build()
        config.tabs shouldHaveSize 1
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.ContainerReference>()
        tab.containerClass shouldBe ContainerDest::class
        tab.title shouldBe "Reified"
    }

    test("initialTab setting is preserved in build") {
        val builder = TabsBuilder()
        builder.initialTab = 2
        builder.tab(Tab1Dest, title = "T1")
        builder.tab(Tab2Dest, title = "T2")
        builder.tab(Tab3Dest, title = "T3")

        val config = builder.build()
        config.initialTab shouldBe 2
    }

    test("multiple tabs build in order") {
        val builder = TabsBuilder()
        builder.tab(Tab1Dest, title = "First")
        builder.tab(Tab2Dest, title = "Second")
        builder.containerTab<ContainerDest>(title = "Third")

        val config = builder.build()
        config.tabs shouldHaveSize 3
        config.tabs[0].shouldBeInstanceOf<TabEntry.FlatScreen>()
        config.tabs[1].shouldBeInstanceOf<TabEntry.FlatScreen>()
        config.tabs[2].shouldBeInstanceOf<TabEntry.ContainerReference>()
    }

    test("build with no tabs returns empty list") {
        val builder = TabsBuilder()
        val config = builder.build()
        config.tabs.shouldBeEmpty()
        config.initialTab shouldBe 0
    }

    test("nested stack tab with empty stack") {
        val builder = TabsBuilder()
        builder.tab(Tab1Dest, title = "Empty", icon = null) {
            // no screens
        }

        val config = builder.build()
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.NestedStack>()
        tab.screens.shouldBeEmpty()
    }

    test("containerTab with default null title and icon") {
        val builder = TabsBuilder()
        builder.containerTab(ContainerDest::class)

        val config = builder.build()
        val tab = config.tabs[0].shouldBeInstanceOf<TabEntry.ContainerReference>()
        tab.title.shouldBeNull()
        tab.icon.shouldBeNull()
    }
})
