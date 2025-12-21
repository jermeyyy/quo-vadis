# Deep Link API Refactoring Plan

## Problem Statement

The current deep linking feature is broken and has API complexity issues:

1. **`@Argument` annotation data is not utilized** - KSP generator only extracts `routeParams` from `{param}` placeholders in route patterns, ignoring rich metadata from `@Argument` annotation (key, optional, serializerType)

2. **Type conversions are missing** - Generated code uses `params["id"]!!` directly as String, ignoring the `SerializerType` enum that exists in `ParamInfo`

3. **Query parameters are ignored** - `DeepLink.parse()` extracts query params but generated handler doesn't use them when constructing destinations

4. **Runtime registration is a no-op** - The `register()` method in generated handlers does nothing, preventing runtime deep link registration

5. **API is complex and confusing** - Multiple overlapping interfaces (`DeepLinkHandler`, `GeneratedDeepLinkHandler`), inconsistent behavior between generated and default handlers

---

## Requirements

1. User should be able to provide `@Arguments` to paths containing parameters
2. API should allow converting deep link route string to destination for navigation
3. Deep link should consist of schema, path, and arguments (both path and query)
4. API should allow using generated deep link registry AND registering new ones at runtime

---

## Proposed Solution

### New Deep Link Data Model

```kotlin
/**
 * Represents a parsed deep link with all its components.
 *
 * @property scheme URI scheme (e.g., "app", "https", "myapp")
 * @property path Path after scheme (e.g., "profile/123" from "app://profile/123")
 * @property pathParams Parameters extracted from path (e.g., {"id": "123"} from "{id}")
 * @property queryParams Parameters from query string (e.g., {"ref": "email"} from "?ref=email")
 */
data class DeepLink(
    val scheme: String,
    val path: String,
    val pathParams: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap()
) {
    /**
     * All parameters merged (path params take precedence over query params).
     */
    val allParams: Map<String, String>
        get() = queryParams + pathParams

    /**
     * Original URI string representation.
     */
    val uri: String
        get() = buildString {
            append(scheme)
            append("://")
            append(path)
            if (queryParams.isNotEmpty()) {
                append("?")
                append(queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            }
        }

    companion object {
        /**
         * Parse a URI string into a DeepLink.
         */
        fun parse(uri: String): DeepLink {
            val schemeEnd = uri.indexOf("://")
            val scheme = if (schemeEnd > 0) uri.substring(0, schemeEnd) else ""
            
            val pathStart = if (schemeEnd > 0) schemeEnd + 3 else 0
            val queryStart = uri.indexOf("?", pathStart)
            
            val path = if (queryStart > 0) {
                uri.substring(pathStart, queryStart)
            } else {
                uri.substring(pathStart)
            }
            
            val queryParams = if (queryStart > 0) {
                parseQueryParams(uri.substring(queryStart + 1))
            } else {
                emptyMap()
            }
            
            return DeepLink(
                scheme = scheme,
                path = path.trimStart('/'),
                queryParams = queryParams
            )
        }
        
        private fun parseQueryParams(query: String): Map<String, String> {
            return query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        }
    }
}
```

### Unified Deep Link Handler API

```kotlin
/**
 * Registry for deep link patterns and their handlers.
 *
 * Combines generated route patterns with runtime registrations.
 * Supports both path parameters ({param}) and query parameters.
 */
interface DeepLinkRegistry {
    
    /**
     * Resolve a deep link URI to a destination.
     *
     * @param uri The full URI string (e.g., "app://profile/123?ref=email")
     * @return The destination if matched, null otherwise
     */
    fun resolve(uri: String): NavDestination?
    
    /**
     * Resolve a parsed DeepLink to a destination.
     *
     * @param deepLink The parsed deep link
     * @return The destination if matched, null otherwise
     */
    fun resolve(deepLink: DeepLink): NavDestination?
    
    /**
     * Register a runtime deep link pattern.
     *
     * @param pattern Route pattern (e.g., "profile/{userId}")
     * @param factory Function to create destination from extracted parameters
     */
    fun register(
        pattern: String,
        factory: (params: Map<String, String>) -> NavDestination
    )
    
    /**
     * Register a runtime deep link pattern with navigation action.
     *
     * @param pattern Route pattern (e.g., "profile/{userId}")
     * @param action Action to execute when pattern matches
     */
    fun register(
        pattern: String,
        action: (navigator: Navigator, params: Map<String, String>) -> Unit
    )
    
    /**
     * Create a deep link URI from a destination.
     *
     * @param destination The destination to create URI for
     * @param scheme The URI scheme to use
     * @return The URI string, or null if destination has no route
     */
    fun createUri(destination: NavDestination, scheme: String = "app"): String?
    
    /**
     * Check if a URI matches any registered pattern.
     *
     * @param uri The URI to check
     * @return True if the URI matches a pattern
     */
    fun canHandle(uri: String): Boolean
    
    /**
     * Get all registered route patterns.
     *
     * @return List of route pattern strings
     */
    fun getRegisteredPatterns(): List<String>
}
```

### Navigator Extension

```kotlin
/**
 * Handle a deep link URI and navigate to the matched destination.
 *
 * @param uri The deep link URI string
 * @return True if navigation occurred, false if no match
 */
fun Navigator.handleDeepLink(uri: String): Boolean {
    val destination = getDeepLinkRegistry().resolve(uri)
    if (destination != null) {
        navigate(destination)
        return true
    }
    return false
}

/**
 * Handle a parsed DeepLink and navigate to the matched destination.
 *
 * @param deepLink The parsed deep link
 * @return True if navigation occurred, false if no match
 */
fun Navigator.handleDeepLink(deepLink: DeepLink): Boolean {
    val destination = getDeepLinkRegistry().resolve(deepLink)
    if (destination != null) {
        navigate(destination)
        return true
    }
    return false
}
```

---

## Implementation Tasks

### Phase 1: Core API Changes (quo-vadis-core)

#### Task 1.1: Refactor DeepLink Data Class
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt`

- Add `scheme` property (extracted from URI)
- Add `pathParams` property (empty initially, populated by registry)
- Rename `parameters` to `queryParams` for clarity
- Add `allParams` computed property (merged path + query)
- Update `parse()` to extract scheme
- Add `uri` computed property for reconstruction

#### Task 1.2: Create DeepLinkRegistry Interface
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLinkRegistry.kt`

- Create new `DeepLinkRegistry` interface (replaces/extends `DeepLinkHandler`)
- Add `resolve(uri)` and `resolve(deepLink)` methods
- Add `register(pattern, factory)` for destination factories
- Add `register(pattern, action)` for navigation actions
- Add `createUri()` for reverse routing
- Add `canHandle()` and `getRegisteredPatterns()` utility methods

#### Task 1.3: Create RuntimeDeepLinkRegistry Implementation
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/RuntimeDeepLinkRegistry.kt`

- Implement `DeepLinkRegistry` interface
- Store runtime registrations in mutable lists
- Implement pattern matching with proper param extraction
- Support both path and query parameters
- Thread-safe for concurrent registration

#### Task 1.4: Create CompositeDeepLinkRegistry
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/CompositeDeepLinkRegistry.kt`

- Wraps generated registry + runtime registry
- Tries generated patterns first, then runtime
- Allows runtime registrations to override generated ones (optional flag)

#### Task 1.5: Update Navigator Interface
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt`

- Rename `getDeepLinkHandler()` to `getDeepLinkRegistry()` (or add alias)
- Add `handleDeepLink(uri: String): Boolean` convenience method
- Update `handleDeepLink(deepLink: DeepLink)` to use new registry

#### Task 1.6: Update TreeNavigator
**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`

- Use `CompositeDeepLinkRegistry` combining config's generated registry + runtime registry
- Implement `getDeepLinkRegistry()` method

#### Task 1.7: Deprecate Old Interfaces
**Files:** Multiple
- Deprecate `DeepLinkHandler` interface (keep for backward compat)
- Deprecate `GeneratedDeepLinkHandler` interface
- Add deprecation messages pointing to `DeepLinkRegistry`

---

### Phase 2: KSP Generator Changes (quo-vadis-ksp)

#### Task 2.1: Update DeepLinkHandlerGenerator to Use @Argument Data
**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt`

- Use `constructorParams` (with `ParamInfo`) instead of just `routeParams`
- Generate proper type conversion code based on `serializerType`
- Handle `@Argument(optional = true)` with default values
- Handle `@Argument(key = "custom")` for parameter name mapping
- Support both path params `{id}` and query params

#### Task 2.2: Generate Type Conversion Code
**Changes in:** `DeepLinkHandlerGenerator.buildRoutePatternInitializer()`

Generate code like:
```kotlin
RoutePattern("profile/{id}", listOf("id")) { params ->
    ProfileDestination.Detail(
        id = params["id"]!!.toInt(),  // Based on SerializerType.INT
        ref = params["ref"]           // Optional, nullable
    )
}
```

Type conversions needed:
- `STRING` → direct use
- `INT` → `.toInt()` or `.toIntOrNull()`
- `LONG` → `.toLong()` or `.toLongOrNull()`
- `FLOAT` → `.toFloat()` or `.toFloatOrNull()`
- `DOUBLE` → `.toDouble()` or `.toDoubleOrNull()`
- `BOOLEAN` → `.toBooleanStrictOrNull()` or custom parsing
- `ENUM` → `enumValueOf<T>()`
- `JSON` → `Json.decodeFromString<T>()`

#### Task 2.3: Generate Code That Uses Both Path and Query Params
**Changes in:** `DeepLinkHandlerGenerator`

- Generate code that merges path params with deep link's query params
- Access optional params from `params.getOrNull("key")` or `params["key"] ?: defaultValue`

#### Task 2.4: Implement DeepLinkRegistry Interface
**Changes in:** Generated handler

- Implement `DeepLinkRegistry` instead of `GeneratedDeepLinkHandler`
- Store runtime registrations
- Implement all registry methods properly

#### Task 2.5: Update Route Pattern Matching
**Changes in:** Generated `RoutePattern` class

- Update `match()` to return both path params extracted AND accept query params
- Ensure pattern matching works with both path segments and query strings

---

### Phase 3: Test Updates

#### Task 3.1: Add DeepLink Parsing Tests
**File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLinkTest.kt` (new)

- Test scheme extraction
- Test path extraction
- Test query param parsing
- Test `allParams` merging
- Test URI reconstruction

#### Task 3.2: Add DeepLinkRegistry Tests
**File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLinkRegistryTest.kt` (new)

- Test runtime registration
- Test pattern matching
- Test type conversions
- Test optional parameters
- Test reverse routing (`createUri`)

#### Task 3.3: Update TreeNavigator Tests
**File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigatorTest.kt`

- Update existing deep link tests
- Add tests for new registry methods
- Test composite registry behavior

---

### Phase 4: Demo App Updates

#### Task 4.1: Update DeepLinkDemoScreen
**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/DeepLinkDemoScreen.kt`

- Update to use new `handleDeepLink(uri)` API
- Add examples with query parameters
- Add example of runtime registration

#### Task 4.2: Add Destinations with Query Params
**Files:** Demo destination files

- Add examples with `@Argument(optional = true)` for query params
- Add examples with type conversions (Int, Boolean)

---

## API Examples (After Refactoring)

### Example 1: Basic Navigation via Deep Link

```kotlin
// Parse and navigate
navigator.handleDeepLink("app://profile/123")

// Or manually
val deepLink = DeepLink.parse("app://profile/123")
val destination = navigator.getDeepLinkRegistry().resolve(deepLink)
destination?.let { navigator.navigate(it) }
```

### Example 2: Deep Link with Query Parameters

```kotlin
// Destination definition
@Destination(route = "search/results")
data class SearchResults(
    @Argument val query: String,
    @Argument(optional = true) val page: Int = 1,
    @Argument(optional = true) val sort: String = "relevance"
) : SearchDestination()

// Deep link
navigator.handleDeepLink("app://search/results?query=kotlin&page=2&sort=date")
// Creates: SearchResults(query = "kotlin", page = 2, sort = "date")
```

### Example 3: Runtime Registration

```kotlin
// Register custom deep link at runtime
val registry = navigator.getDeepLinkRegistry()

// Option 1: With destination factory
registry.register("promo/{code}") { params ->
    PromoDestination(code = params["code"]!!)
}

// Option 2: With custom action
registry.register("logout") { navigator, _ ->
    authService.logout()
    navigator.navigateAndClearAll(LoginDestination)
}
```

### Example 4: Reverse Routing

```kotlin
// Create deep link from destination
val destination = ProfileDestination.Detail(userId = "123")
val uri = navigator.getDeepLinkRegistry().createUri(destination)
// Result: "app://profile/123"

// Share the deep link
shareService.share(uri)
```

### Example 5: Type-Safe Arguments

```kotlin
@Destination(route = "product/{productId}")
data class ProductDetail(
    @Argument val productId: Long,        // Parsed as Long
    @Argument(optional = true) val showReviews: Boolean = false,
    @Argument(optional = true, key = "ref") val referrer: String? = null
) : ProductDestination()

// Deep link: app://product/42?showReviews=true&ref=email
// Creates: ProductDetail(productId = 42L, showReviews = true, referrer = "email")
```

---

## Migration Guide

### For Library Users

**Before:**
```kotlin
// Old API
navigator.handleDeepLink(DeepLink.parse(uri))
```

**After:**
```kotlin
// New API (simpler)
navigator.handleDeepLink(uri)

// Or with more control
val deepLink = DeepLink.parse(uri)
navigator.handleDeepLink(deepLink)
```

### For Runtime Registration

**Before:**
```kotlin
// Old API (didn't work properly)
navigator.getDeepLinkHandler().register(pattern) { nav, params ->
    nav.navigate(SomeDestination(params["id"]!!))
}
```

**After:**
```kotlin
// New API (works correctly)
navigator.getDeepLinkRegistry().register(pattern) { params ->
    SomeDestination(id = params["id"]!!)
}

// Or with custom action
navigator.getDeepLinkRegistry().register(pattern) { nav, params ->
    // Custom navigation logic
}
```

---

## Backward Compatibility

1. **DeepLink class** - Breaking change in properties, but `parse()` API remains same
2. **DeepLinkHandler** - Deprecated but kept, delegates to registry
3. **GeneratedDeepLinkHandler** - Deprecated but kept, implements DeepLinkRegistry
4. **Navigator.handleDeepLink(DeepLink)** - Still works
5. **Navigator.handleDeepLink(uri)** - New convenience method

---

## Success Criteria

- [ ] `@Argument` annotation data is used in code generation
- [ ] Type conversions work for Int, Long, Float, Double, Boolean, Enum
- [ ] Query parameters are properly extracted and passed to destinations
- [ ] Runtime registration works and can override generated patterns
- [ ] `createUri()` generates valid URIs from destinations
- [ ] All existing tests pass
- [ ] New tests cover edge cases
- [ ] Demo app showcases all features
- [ ] Documentation updated

---

## Estimated Effort

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| Phase 1 | Core API Changes | 4-6 hours |
| Phase 2 | KSP Generator | 4-6 hours |
| Phase 3 | Tests | 2-3 hours |
| Phase 4 | Demo App | 1-2 hours |
| **Total** | | **11-17 hours** |

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking changes in DeepLink | Provide migration guide, deprecation warnings |
| Complex type conversion edge cases | Focus on common types first (String, Int, Boolean) |
| Generated code complexity | Use helper functions, keep RoutePattern simple |
| Runtime registration thread safety | Use concurrent collections or synchronization |
