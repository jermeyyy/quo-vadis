@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.dsl.internal.DslScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private object ScopeHome : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object ScopeDetail : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object ScopeSettings : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object ScopeProfile : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

private object ScopeUnregistered : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class DslScopeRegistryTest : FunSpec({

    // =========================================================================
    // isInScope
    // =========================================================================

    test("isInScope returns true for destination in scope") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("main") to setOf(ScopeHome::class, ScopeDetail::class)
            )
        )

        registry.isInScope(ScopeKey("main"), ScopeHome).shouldBeTrue()
        registry.isInScope(ScopeKey("main"), ScopeDetail).shouldBeTrue()
    }

    test("isInScope returns false for destination not in scope") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("main") to setOf(ScopeHome::class)
            )
        )

        registry.isInScope(ScopeKey("main"), ScopeSettings).shouldBeFalse()
    }

    test("isInScope returns false for unknown scope key") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("main") to setOf(ScopeHome::class)
            )
        )

        registry.isInScope(ScopeKey("unknown"), ScopeHome).shouldBeFalse()
    }

    test("isInScope with empty scopes returns false") {
        val registry = DslScopeRegistry(scopes = emptyMap())

        registry.isInScope(ScopeKey("any"), ScopeHome).shouldBeFalse()
    }

    test("isInScope with multiple scopes checks correct scope") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("home") to setOf(ScopeHome::class, ScopeDetail::class),
                ScopeKey("settings") to setOf(ScopeSettings::class, ScopeProfile::class)
            )
        )

        registry.isInScope(ScopeKey("home"), ScopeHome).shouldBeTrue()
        registry.isInScope(ScopeKey("home"), ScopeSettings).shouldBeFalse()
        registry.isInScope(ScopeKey("settings"), ScopeSettings).shouldBeTrue()
        registry.isInScope(ScopeKey("settings"), ScopeHome).shouldBeFalse()
    }

    // =========================================================================
    // getScopeKey
    // =========================================================================

    test("getScopeKey returns scope key for registered destination") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("main") to setOf(ScopeHome::class, ScopeDetail::class)
            )
        )

        registry.getScopeKey(ScopeHome) shouldBe ScopeKey("main")
        registry.getScopeKey(ScopeDetail) shouldBe ScopeKey("main")
    }

    test("getScopeKey returns null for unregistered destination") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("main") to setOf(ScopeHome::class)
            )
        )

        registry.getScopeKey(ScopeUnregistered).shouldBeNull()
    }

    test("getScopeKey returns null when no scopes registered") {
        val registry = DslScopeRegistry(scopes = emptyMap())

        registry.getScopeKey(ScopeHome).shouldBeNull()
    }

    test("getScopeKey returns first scope when destination in multiple scopes") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("scope-a") to setOf(ScopeHome::class),
                ScopeKey("scope-b") to setOf(ScopeHome::class)
            )
        )

        // First scope wins (iteration order of the map)
        val result = registry.getScopeKey(ScopeHome)
        result.shouldBe(ScopeKey("scope-a"))
    }

    test("getScopeKey with multiple destinations in different scopes") {
        val registry = DslScopeRegistry(
            scopes = mapOf(
                ScopeKey("home-scope") to setOf(ScopeHome::class, ScopeDetail::class),
                ScopeKey("settings-scope") to setOf(ScopeSettings::class, ScopeProfile::class)
            )
        )

        registry.getScopeKey(ScopeHome) shouldBe ScopeKey("home-scope")
        registry.getScopeKey(ScopeDetail) shouldBe ScopeKey("home-scope")
        registry.getScopeKey(ScopeSettings) shouldBe ScopeKey("settings-scope")
        registry.getScopeKey(ScopeProfile) shouldBe ScopeKey("settings-scope")
    }
})
