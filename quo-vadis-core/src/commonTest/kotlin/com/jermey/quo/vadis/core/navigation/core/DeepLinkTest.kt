package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [com.jermey.quo.vadis.core.navigation.DeepLink] parsing and URI reconstruction.
 *
 * Tests cover:
 * - URI parsing (scheme, path, query params)
 * - allParams merging behavior
 * - URI reconstruction via uri property
 * - Edge cases (no scheme, leading slashes, complex query strings)
 */
class DeepLinkTest {

    @Test
    fun `parse extracts scheme correctly`() {
        val deepLink = DeepLink.parse("myapp://home/detail")
        assertEquals("myapp", deepLink.scheme)
    }

    @Test
    fun `parse extracts path correctly`() {
        val deepLink = DeepLink.parse("app://profile/123")
        assertEquals("profile/123", deepLink.path)
    }

    @Test
    fun `parse extracts query params`() {
        val deepLink = DeepLink.parse("app://search?query=kotlin&page=2")
        assertEquals("kotlin", deepLink.queryParams["query"])
        assertEquals("2", deepLink.queryParams["page"])
    }

    @Test
    fun `parse handles URI without scheme`() {
        val deepLink = DeepLink.parse("home/detail/123")
        assertEquals("app", deepLink.scheme) // Default scheme
        assertEquals("home/detail/123", deepLink.path)
    }

    @Test
    fun `parse handles URI with only path`() {
        val deepLink = DeepLink.parse("app://home")
        assertEquals("home", deepLink.path)
        assertEquals(emptyMap(), deepLink.queryParams)
    }

    @Test
    fun `allParams merges queryParams and pathParams with pathParams priority`() {
        val deepLink = DeepLink(
            scheme = "app",
            path = "profile/123",
            pathParams = mapOf("id" to "123", "source" to "path"),
            queryParams = mapOf("ref" to "email", "source" to "query")
        )
        // Path params take precedence (queryParams + pathParams, pathParams last = wins)
        assertEquals("123", deepLink.allParams["id"])
        assertEquals("email", deepLink.allParams["ref"])
        assertEquals("path", deepLink.allParams["source"]) // Path wins
    }

    @Test
    fun `uri property reconstructs URI correctly`() {
        val deepLink = DeepLink(
            scheme = "myapp",
            path = "profile/123",
            queryParams = mapOf("ref" to "email", "tab" to "posts")
        )
        val uri = deepLink.uri
        assertEquals("myapp://profile/123", uri.substringBefore("?"))
        assertTrue(uri.contains("ref=email"))
        assertTrue(uri.contains("tab=posts"))
    }

    @Test
    fun `uri property handles empty query params`() {
        val deepLink = DeepLink(
            scheme = "app",
            path = "home"
        )
        assertEquals("app://home", deepLink.uri)
    }

    @Test
    fun `parse handles complex query strings`() {
        val deepLink = DeepLink.parse("app://search?q=hello+world&filter=active&sort=date")
        assertEquals("hello+world", deepLink.queryParams["q"])
        assertEquals("active", deepLink.queryParams["filter"])
        assertEquals("date", deepLink.queryParams["sort"])
    }

    @Test
    fun `parse handles leading slash in path`() {
        val deepLink = DeepLink.parse("app:///profile/123")
        assertEquals("profile/123", deepLink.path) // Leading slashes trimmed
    }

    @Test
    fun `parse handles https scheme`() {
        val deepLink = DeepLink.parse("https://example.com/path/to/resource")
        assertEquals("https", deepLink.scheme)
        assertEquals("example.com/path/to/resource", deepLink.path)
    }

    @Test
    fun `parse handles empty query value`() {
        val deepLink = DeepLink.parse("app://search?q=&filter=active")
        assertEquals("", deepLink.queryParams["q"])
        assertEquals("active", deepLink.queryParams["filter"])
    }

    @Test
    fun `parse handles path without query`() {
        val deepLink = DeepLink.parse("app://users/42/posts/99")
        assertEquals("users/42/posts/99", deepLink.path)
        assertEquals(emptyMap(), deepLink.queryParams)
    }

    @Test
    fun `default pathParams is empty`() {
        val deepLink = DeepLink.parse("app://home")
        assertEquals(emptyMap(), deepLink.pathParams)
    }

    @Test
    fun `allParams returns only queryParams when pathParams is empty`() {
        val deepLink = DeepLink(
            scheme = "app",
            path = "home",
            queryParams = mapOf("tab" to "feed")
        )
        assertEquals(mapOf("tab" to "feed"), deepLink.allParams)
    }

    @Test
    fun `allParams returns only pathParams when queryParams is empty`() {
        val deepLink = DeepLink(
            scheme = "app",
            path = "profile/123",
            pathParams = mapOf("id" to "123")
        )
        assertEquals(mapOf("id" to "123"), deepLink.allParams)
    }
}
