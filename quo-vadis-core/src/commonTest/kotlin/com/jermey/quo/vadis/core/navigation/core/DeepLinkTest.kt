package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [com.jermey.quo.vadis.core.navigation.DeepLink] parsing and URI reconstruction.
 *
 * Tests cover:
 * - URI parsing (scheme, path, query params)
 * - allParams merging behavior
 * - URI reconstruction via uri property
 * - Edge cases (no scheme, leading slashes, complex query strings)
 */
class DeepLinkTest : FunSpec({

    test("parse extracts scheme correctly") {
        val deepLink = DeepLink.parse("myapp://home/detail")
        deepLink.scheme shouldBe "myapp"
    }

    test("parse extracts path correctly") {
        val deepLink = DeepLink.parse("app://profile/123")
        deepLink.path shouldBe "profile/123"
    }

    test("parse extracts query params") {
        val deepLink = DeepLink.parse("app://search?query=kotlin&page=2")
        deepLink.queryParams["query"] shouldBe "kotlin"
        deepLink.queryParams["page"] shouldBe "2"
    }

    test("parse handles URI without scheme") {
        val deepLink = DeepLink.parse("home/detail/123")
        deepLink.scheme shouldBe "app" // Default scheme
        deepLink.path shouldBe "home/detail/123"
    }

    test("parse handles URI with only path") {
        val deepLink = DeepLink.parse("app://home")
        deepLink.path shouldBe "home"
        deepLink.queryParams shouldBe emptyMap()
    }

    test("allParams merges queryParams and pathParams with pathParams priority") {
        val deepLink = DeepLink(
            scheme = "app",
            path = "profile/123",
            pathParams = mapOf("id" to "123", "source" to "path"),
            queryParams = mapOf("ref" to "email", "source" to "query")
        )
        // Path params take precedence (queryParams + pathParams, pathParams last = wins)
        deepLink.allParams["id"] shouldBe "123"
        deepLink.allParams["ref"] shouldBe "email"
        deepLink.allParams["source"] shouldBe "path" // Path wins
    }

    test("uri property reconstructs URI correctly") {
        val deepLink = DeepLink(
            scheme = "myapp",
            path = "profile/123",
            queryParams = mapOf("ref" to "email", "tab" to "posts")
        )
        val uri = deepLink.uri
        uri.substringBefore("?") shouldBe "myapp://profile/123"
        uri.contains("ref=email").shouldBeTrue()
        uri.contains("tab=posts").shouldBeTrue()
    }

    test("uri property handles empty query params") {
        val deepLink = DeepLink(
            scheme = "app",
            path = "home"
        )
        deepLink.uri shouldBe "app://home"
    }

    test("parse handles complex query strings") {
        val deepLink = DeepLink.parse("app://search?q=hello+world&filter=active&sort=date")
        deepLink.queryParams["q"] shouldBe "hello+world"
        deepLink.queryParams["filter"] shouldBe "active"
        deepLink.queryParams["sort"] shouldBe "date"
    }

    test("parse handles leading slash in path") {
        val deepLink = DeepLink.parse("app:///profile/123")
        deepLink.path shouldBe "profile/123" // Leading slashes trimmed
    }

    test("parse handles https scheme") {
        val deepLink = DeepLink.parse("https://example.com/path/to/resource")
        deepLink.scheme shouldBe "https"
        deepLink.path shouldBe "example.com/path/to/resource"
    }

    test("parse handles empty query value") {
        val deepLink = DeepLink.parse("app://search?q=&filter=active")
        deepLink.queryParams["q"] shouldBe ""
        deepLink.queryParams["filter"] shouldBe "active"
    }

    test("parse handles path without query") {
        val deepLink = DeepLink.parse("app://users/42/posts/99")
        deepLink.path shouldBe "users/42/posts/99"
        deepLink.queryParams shouldBe emptyMap()
    }

    test("default pathParams is empty") {
        val deepLink = DeepLink.parse("app://home")
        deepLink.pathParams shouldBe emptyMap()
    }

    test("allParams returns only queryParams when pathParams is empty") {
        val deepLink = DeepLink(
            scheme = "app",
            path = "home",
            queryParams = mapOf("tab" to "feed")
        )
        deepLink.allParams shouldBe mapOf("tab" to "feed")
    }

    test("allParams returns only pathParams when queryParams is empty") {
        val deepLink = DeepLink(
            scheme = "app",
            path = "profile/123",
            pathParams = mapOf("id" to "123")
        )
        deepLink.allParams shouldBe mapOf("id" to "123")
    }
})
