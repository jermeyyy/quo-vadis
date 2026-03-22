package com.jermey.quo.vadis.core.navigation.internal.registry

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.RouteRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

// Unique test destinations to avoid state leaking in singleton RouteRegistry
private object RouteDest1 : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object RouteDest2 : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object RouteDest3 : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object UnregisteredDest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class RouteRegistryTest : FunSpec({

    test("register a route and retrieve it") {
        RouteRegistry.register(RouteDest1::class, "home/feed")

        RouteRegistry.getRoute(RouteDest1::class) shouldBe "home/feed"
    }

    test("getRoute for unregistered destination returns null") {
        RouteRegistry.getRoute(UnregisteredDest::class).shouldBeNull()
    }

    test("register overwrites previous route") {
        RouteRegistry.register(RouteDest2::class, "original/route")
        RouteRegistry.register(RouteDest2::class, "updated/route")

        RouteRegistry.getRoute(RouteDest2::class) shouldBe "updated/route"
    }

    test("multiple destinations with different routes") {
        RouteRegistry.register(RouteDest1::class, "route/one")
        RouteRegistry.register(RouteDest3::class, "route/three")

        RouteRegistry.getRoute(RouteDest1::class) shouldBe "route/one"
        RouteRegistry.getRoute(RouteDest3::class) shouldBe "route/three"
    }
})
