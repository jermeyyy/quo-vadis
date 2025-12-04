# KSP-004: Create Deep Link Handler Generator

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-004 |
| **Task Name** | Create Deep Link Handler Generator |
| **Phase** | Phase 4: KSP Processor Rewrite |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | KSP-001 (Annotation Extractors) |
| **Blocked By** | KSP-001 |
| **Blocks** | None |

---

## Overview

This task creates a KSP generator that produces `GeneratedDeepLinkHandler.kt` - a handler for parsing deep link URIs into destination instances. The handler matches URI paths against route patterns defined in `@Destination` annotations and creates the corresponding destination objects.

### Purpose

Enable deep linking support by:
1. **Matching URIs** to route patterns (e.g., `myapp://home/detail/123`)
2. **Extracting parameters** from URI paths (e.g., `{id}` → `"123"`)
3. **Creating destinations** with extracted parameter values
4. **Generating reverse URIs** from destination instances

### Data Flow

```
@Destination(route = "...")  →  DestinationExtractor  →  DeepLinkHandlerGenerator
                                                              ↓
                                              GeneratedDeepLinkHandler.kt
```

---

## Generated Output

### File Location

```
build/generated/ksp/commonMain/kotlin/{package}/generated/GeneratedDeepLinkHandler.kt
```

### Generated Structure

```kotlin
// File: GeneratedDeepLinkHandler.kt
package com.example.generated

import com.jermey.quo.vadis.core.navigation.core.*

/**
 * KSP-generated deep link handler.
 * Parses deep link URIs and creates corresponding destination instances.
 */
object GeneratedDeepLinkHandler : DeepLinkHandler {
    
    private val routes = listOf(
        RoutePattern("home/feed", emptyList()) { HomeDestination.Feed },
        RoutePattern("home/detail/{id}", listOf("id")) { params ->
            HomeDestination.Detail(id = params["id"]!!)
        },
        RoutePattern("profile/overview", emptyList()) { ProfileDestination.Overview },
        RoutePattern("profile/edit/{section}", listOf("section")) { params ->
            ProfileDestination.Edit(section = params["section"]!!)
        }
    )
    
    override fun handleDeepLink(uri: Uri): DeepLinkResult {
        val path = uri.path?.trimStart('/') ?: return DeepLinkResult.NotMatched
        
        for (route in routes) {
            val params = route.match(path)
            if (params != null) {
                return DeepLinkResult.Matched(route.createDestination(params))
            }
        }
        
        return DeepLinkResult.NotMatched
    }
    
    override fun createDeepLinkUri(destination: Destination, scheme: String): Uri? {
        return when (destination) {
            HomeDestination.Feed -> Uri.parse("$scheme://home/feed")
            is HomeDestination.Detail -> Uri.parse("$scheme://home/detail/${destination.id}")
            ProfileDestination.Overview -> Uri.parse("$scheme://profile/overview")
            is ProfileDestination.Edit -> Uri.parse("$scheme://profile/edit/${destination.section}")
            else -> null
        }
    }
}

private data class RoutePattern(
    val pattern: String,
    val paramNames: List<String>,
    val createDestination: (Map<String, String>) -> Destination
) {
    private val regex: Regex = buildRegex()
    
    private fun buildRegex(): Regex {
        var regexPattern = Regex.escape(pattern)
        for (param in paramNames) {
            regexPattern = regexPattern.replace("\\{$param\\}", "([^/]+)")
        }
        return Regex("^$regexPattern$")
    }
    
    fun match(path: String): Map<String, String>? {
        val matchResult = regex.matchEntire(path) ?: return null
        return paramNames.zip(matchResult.groupValues.drop(1)).toMap()
    }
}

sealed class DeepLinkResult {
    data class Matched(val destination: Destination) : DeepLinkResult()
    data object NotMatched : DeepLinkResult()
}
```

---

## Implementation

### DeepLinkHandlerGenerator Class

```kotlin
package com.jermey.quo.vadis.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.squareup.kotlinpoet.*

/**
 * Generates [GeneratedDeepLinkHandler] for deep link URI parsing.
 *
 * Transforms @Destination route patterns into:
 * - RoutePattern instances for URI matching
 * - handleDeepLink() implementation
 * - createDeepLinkUri() for reverse URI generation
 */
class DeepLinkHandlerGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    
    /**
     * Generate the deep link handler from all destinations with routes.
     *
     * @param destinations All extracted DestinationInfo with non-empty routes
     * @param packageName Target package for generated code
     */
    fun generate(destinations: List<DestinationInfo>, packageName: String) {
        // Filter destinations with routes
        val routeableDestinations = destinations.filter { it.route.isNotEmpty() }
        
        if (routeableDestinations.isEmpty()) {
            logger.warn("No @Destination with routes found, skipping deep link handler generation")
            return
        }
        
        val fileSpec = FileSpec.builder(packageName, "GeneratedDeepLinkHandler")
            .addFileComment("Generated by Quo Vadis KSP Processor. Do not modify.")
            .addType(generateHandlerObject(routeableDestinations))
            .addType(generateRoutePatternClass())
            .addType(generateDeepLinkResultClass())
            .build()
        
        codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = "GeneratedDeepLinkHandler"
        ).bufferedWriter().use { fileSpec.writeTo(it) }
        
        logger.info("Generated GeneratedDeepLinkHandler.kt with ${routeableDestinations.size} routes")
    }
    
    private fun generateHandlerObject(destinations: List<DestinationInfo>): TypeSpec {
        return TypeSpec.objectBuilder("GeneratedDeepLinkHandler")
            .addSuperinterface(ClassName("com.jermey.quo.vadis.core.navigation.core", "DeepLinkHandler"))
            .addKdoc("""
                |KSP-generated deep link handler.
                |
                |Parses deep link URIs and creates corresponding destination instances.
                |Supports ${destinations.size} route patterns.
            """.trimMargin())
            .addProperty(generateRoutesProperty(destinations))
            .addFunction(generateHandleDeepLinkFunction())
            .addFunction(generateCreateDeepLinkUriFunction(destinations))
            .build()
    }
    
    private fun generateRoutesProperty(destinations: List<DestinationInfo>): PropertySpec {
        val routePatterns = destinations.map { dest ->
            val params = extractRouteParams(dest.route)
            val paramList = if (params.isEmpty()) {
                "emptyList()"
            } else {
                "listOf(${params.joinToString { "\"$it\"" }})"
            }
            
            val creator = if (params.isEmpty()) {
                // Data object - direct reference
                "{ ${dest.qualifiedName} }"
            } else {
                // Data class - extract params
                val paramAssignments = params.joinToString { p -> "$p = params[\"$p\"]!!" }
                "{ params -> ${dest.qualifiedName}($paramAssignments) }"
            }
            
            "RoutePattern(\"${dest.route}\", $paramList, $creator)"
        }
        
        return PropertySpec.builder("routes", LIST.parameterizedBy(ClassName("", "RoutePattern")))
            .addModifiers(KModifier.PRIVATE)
            .initializer("listOf(\n${routePatterns.joinToString(",\n").prependIndent("    ")}\n)")
            .build()
    }
    
    private fun generateHandleDeepLinkFunction(): FunSpec {
        return FunSpec.builder("handleDeepLink")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("uri", ClassName("android.net", "Uri"))
            .returns(ClassName("", "DeepLinkResult"))
            .addCode("""
                |val path = uri.path?.trimStart('/') ?: return DeepLinkResult.NotMatched
                |
                |for (route in routes) {
                |    val params = route.match(path)
                |    if (params != null) {
                |        return DeepLinkResult.Matched(route.createDestination(params))
                |    }
                |}
                |
                |return DeepLinkResult.NotMatched
            """.trimMargin())
            .build()
    }
    
    private fun generateCreateDeepLinkUriFunction(destinations: List<DestinationInfo>): FunSpec {
        val whenClauses = destinations.map { dest ->
            val params = extractRouteParams(dest.route)
            if (params.isEmpty()) {
                "${dest.qualifiedName} -> Uri.parse(\"\$scheme://${dest.route}\")"
            } else {
                // Build URI with interpolated params
                val uriPath = dest.route.replace(Regex("\\{(\\w+)\\}")) { "\${destination.${it.groupValues[1]}}" }
                "is ${dest.qualifiedName} -> Uri.parse(\"\$scheme://$uriPath\")"
            }
        }
        
        return FunSpec.builder("createDeepLinkUri")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destination", ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination"))
            .addParameter(ParameterSpec.builder("scheme", STRING).defaultValue("\"myapp\"").build())
            .returns(ClassName("android.net", "Uri").copy(nullable = true))
            .addCode("""
                |return when (destination) {
                |${whenClauses.joinToString("\n").prependIndent("    ")}
                |    else -> null
                |}
            """.trimMargin())
            .build()
    }
    
    private fun generateRoutePatternClass(): TypeSpec {
        return TypeSpec.classBuilder("RoutePattern")
            .addModifiers(KModifier.PRIVATE, KModifier.DATA)
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("pattern", STRING)
                .addParameter("paramNames", LIST.parameterizedBy(STRING))
                .addParameter("createDestination", LambdaTypeName.get(
                    parameters = listOf(ParameterSpec.unnamed(MAP.parameterizedBy(STRING, STRING))),
                    returnType = ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination")
                ))
                .build())
            .addProperty(PropertySpec.builder("pattern", STRING).initializer("pattern").build())
            .addProperty(PropertySpec.builder("paramNames", LIST.parameterizedBy(STRING)).initializer("paramNames").build())
            .addProperty(PropertySpec.builder("createDestination", LambdaTypeName.get(
                parameters = listOf(ParameterSpec.unnamed(MAP.parameterizedBy(STRING, STRING))),
                returnType = ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination")
            )).initializer("createDestination").build())
            .addProperty(PropertySpec.builder("regex", ClassName("kotlin.text", "Regex"))
                .addModifiers(KModifier.PRIVATE)
                .initializer("buildRegex()")
                .build())
            .addFunction(FunSpec.builder("buildRegex")
                .addModifiers(KModifier.PRIVATE)
                .returns(ClassName("kotlin.text", "Regex"))
                .addCode("""
                    |var regexPattern = Regex.escape(pattern)
                    |for (param in paramNames) {
                    |    regexPattern = regexPattern.replace("\\{${'$'}param\\}", "([^/]+)")
                    |}
                    |return Regex("^${'$'}regexPattern${'$'}")
                """.trimMargin())
                .build())
            .addFunction(FunSpec.builder("match")
                .addParameter("path", STRING)
                .returns(MAP.parameterizedBy(STRING, STRING).copy(nullable = true))
                .addCode("""
                    |val matchResult = regex.matchEntire(path) ?: return null
                    |return paramNames.zip(matchResult.groupValues.drop(1)).toMap()
                """.trimMargin())
                .build())
            .build()
    }
    
    private fun generateDeepLinkResultClass(): TypeSpec {
        return TypeSpec.classBuilder("DeepLinkResult")
            .addModifiers(KModifier.SEALED)
            .addType(TypeSpec.classBuilder("Matched")
                .addModifiers(KModifier.DATA)
                .superclass(ClassName("", "DeepLinkResult"))
                .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameter("destination", ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination"))
                    .build())
                .addProperty(PropertySpec.builder("destination", ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination"))
                    .initializer("destination")
                    .build())
                .build())
            .addType(TypeSpec.objectBuilder("NotMatched")
                .addModifiers(KModifier.DATA)
                .superclass(ClassName("", "DeepLinkResult"))
                .build())
            .build()
    }
    
    /**
     * Extract parameter names from a route pattern.
     * e.g., "home/detail/{id}/{name}" → ["id", "name"]
     */
    private fun extractRouteParams(route: String): List<String> {
        return Regex("\\{(\\w+)\\}").findAll(route).map { it.groupValues[1] }.toList()
    }
}
```

---

## Route Pattern Matching

### Pattern Syntax

| Pattern | Example URI | Extracted Params |
|---------|-------------|------------------|
| `home/feed` | `myapp://home/feed` | (none) |
| `home/detail/{id}` | `myapp://home/detail/123` | `id="123"` |
| `user/{userId}/post/{postId}` | `myapp://user/42/post/99` | `userId="42"`, `postId="99"` |

### Regex Conversion

```
Input:  "home/detail/{id}"
Escape: "home/detail/\{id\}"
Replace: "home/detail/([^/]+)"
Final:  ^home/detail/([^/]+)$
```

### Matching Process

1. Extract path from URI (strip leading `/`)
2. Iterate through route patterns
3. Match regex against path
4. Extract capture groups as parameter values
5. Create destination with parameters

---

## Implementation Steps

### Step 1: Create Generator Class (0.5 days)

Create `DeepLinkHandlerGenerator` with:
- Constructor taking `CodeGenerator` and `KSPLogger`
- `generate()` method accepting `List<DestinationInfo>`

### Step 2: Implement Route Pattern Generation (1 day)

- Parse route strings to extract parameters
- Generate `RoutePattern` instances
- Build regex patterns with capture groups

### Step 3: Implement handleDeepLink (0.5 days)

- Generate URI path extraction
- Generate pattern matching loop
- Return `DeepLinkResult.Matched` or `NotMatched`

### Step 4: Implement createDeepLinkUri (0.5 days)

- Generate `when` expression over all destinations
- Build URI strings with parameter interpolation

### Step 5: Integration and Testing (1 day)

- Integrate with `QuoVadisSymbolProcessor`
- Write unit tests for all patterns

---

## Edge Cases

### Query Parameters (Future)

```kotlin
// Route: "search?q={query}&page={page}"
// Currently not supported - only path parameters
```

### Optional Parameters

```kotlin
// Route: "profile/{userId?}"
// Generate nullable parameter handling
ProfileDestination.Profile(userId = params["userId"])
```

### Type Conversion

```kotlin
// Route: "item/{id}" where id: Int
// Generate: id = params["id"]!!.toInt()
```

### Duplicate Routes

```kotlin
// Error: Duplicate route "detail/{id}" on:
//   - HomeDestination.Detail
//   - ProfileDestination.Detail
// KSP should report error during validation
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/.../generators/DeepLinkHandlerGenerator.kt` | Create | Generator class |
| `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | Modify | Invoke generator |
| `quo-vadis-core/.../core/DeepLinkHandler.kt` | Create | Interface definition |
| `build/generated/.../GeneratedDeepLinkHandler.kt` | Generated | Output file |

---

## Acceptance Criteria

- [ ] `DeepLinkHandlerGenerator` class created
- [ ] Generates `GeneratedDeepLinkHandler` object implementing `DeepLinkHandler`
- [ ] Generates `RoutePattern` data class with regex matching
- [ ] Generates `DeepLinkResult` sealed class
- [ ] Handles static routes (no parameters)
- [ ] Handles dynamic routes with path parameters
- [ ] Handles multiple parameters in single route
- [ ] `handleDeepLink()` correctly matches URIs to destinations
- [ ] `createDeepLinkUri()` generates URIs from destinations
- [ ] Reports error on duplicate routes
- [ ] Skips destinations without routes
- [ ] Unit tests for pattern matching
- [ ] Integration tests with real URIs

---

## Testing Notes

```kotlin
@Test
fun `matches static route`() {
    val result = GeneratedDeepLinkHandler.handleDeepLink(
        Uri.parse("myapp://home/feed")
    )
    assertEquals(DeepLinkResult.Matched(HomeDestination.Feed), result)
}

@Test
fun `matches dynamic route with parameter`() {
    val result = GeneratedDeepLinkHandler.handleDeepLink(
        Uri.parse("myapp://home/detail/123")
    )
    assertEquals(
        DeepLinkResult.Matched(HomeDestination.Detail(id = "123")),
        result
    )
}

@Test
fun `returns NotMatched for unknown route`() {
    val result = GeneratedDeepLinkHandler.handleDeepLink(
        Uri.parse("myapp://unknown/path")
    )
    assertEquals(DeepLinkResult.NotMatched, result)
}

@Test
fun `creates URI from destination`() {
    val uri = GeneratedDeepLinkHandler.createDeepLinkUri(
        HomeDestination.Detail(id = "456"),
        scheme = "myapp"
    )
    assertEquals("myapp://home/detail/456", uri?.toString())
}

@Test
fun `extracts multiple parameters`() {
    val result = GeneratedDeepLinkHandler.handleDeepLink(
        Uri.parse("myapp://user/42/post/99")
    )
    assertEquals(
        DeepLinkResult.Matched(UserDestination.Post(userId = "42", postId = "99")),
        result
    )
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 4 Overview
- [KSP-001](./KSP-001-graph-type-enum.md) - Annotation Extractors (provides DestinationInfo)
- [ANN-001](../phase4-annotations/ANN-001-graph-type.md) - @Destination annotation and route format
