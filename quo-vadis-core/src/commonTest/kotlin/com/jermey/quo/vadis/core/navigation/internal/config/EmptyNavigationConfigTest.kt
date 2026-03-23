@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private object TestDest : NavDestination

private object OtherDest : NavDestination

class EmptyNavigationConfigTest : FunSpec({

    test("Empty is singleton identity") {
        NavigationConfig.Empty shouldBeSameInstanceAs NavigationConfig.Empty
    }

    test("Empty is EmptyNavigationConfig") {
        NavigationConfig.Empty.shouldBeInstanceOf<EmptyNavigationConfig>()
    }

    test("screenRegistry hasContent returns false") {
        NavigationConfig.Empty.screenRegistry.hasContent(TestDest).shouldBeFalse()
    }

    test("scopeRegistry isInScope returns true (allows all)") {
        NavigationConfig.Empty.scopeRegistry.isInScope(ScopeKey("any"), TestDest).shouldBeTrue()
    }

    test("scopeRegistry getScopeKey returns null") {
        NavigationConfig.Empty.scopeRegistry.getScopeKey(TestDest).shouldBeNull()
    }

    test("transitionRegistry getTransition returns null") {
        NavigationConfig.Empty.transitionRegistry.getTransition(TestDest::class).shouldBeNull()
    }

    test("modalRegistry isModalDestination returns false") {
        NavigationConfig.Empty.modalRegistry.isModalDestination(TestDest::class).shouldBeFalse()
    }

    test("modalRegistry isModalContainer returns false") {
        NavigationConfig.Empty.modalRegistry.isModalContainer("any-key").shouldBeFalse()
    }

    test("deepLinkRegistry resolve returns null") {
        NavigationConfig.Empty.deepLinkRegistry.resolve("https://example.com").shouldBeNull()
    }

    test("deepLinkRegistry getRegisteredPatterns returns empty") {
        NavigationConfig.Empty.deepLinkRegistry.getRegisteredPatterns() shouldBe emptyList()
    }

    test("paneRoleRegistry getPaneRole returns null") {
        NavigationConfig.Empty.paneRoleRegistry.getPaneRole(ScopeKey("any"), TestDest).shouldBeNull()
    }

    test("paneRoleRegistry getPaneRole by class returns null") {
        NavigationConfig.Empty.paneRoleRegistry.getPaneRole(ScopeKey("any"), TestDest::class).shouldBeNull()
    }

    test("buildNavNode returns null") {
        NavigationConfig.Empty.buildNavNode(TestDest::class).shouldBeNull()
    }

    test("buildNavNode with key and parentKey returns null") {
        NavigationConfig.Empty.buildNavNode(TestDest::class, "key", "parent").shouldBeNull()
    }

    test("plus with another config returns the other config") {
        val other = NavigationConfig.Empty
        val result = NavigationConfig.Empty + other
        result shouldBeSameInstanceAs other
    }

    test("plus with a non-empty config returns the other config") {
        val other = object : NavigationConfig by NavigationConfig.Empty {
            override fun plus(other: NavigationConfig): NavigationConfig = this
        }
        val result = NavigationConfig.Empty + other
        result shouldBeSameInstanceAs other
    }
})
