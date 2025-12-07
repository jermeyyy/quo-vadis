# KSP-008: Fix Deep Link Handler Generator Imports

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-008 |
| **Task Name** | Fix Deep Link Handler Generator Imports |
| **Phase** | Phase 3: KSP Processor Rewrite |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | KSP-004 (Create Deep Link Handler Generator) |
| **Blocked By** | None |
| **Blocks** | MIG-006 (Deep Linking Recipe - partially) |

---

## Overview

The `DeepLinkHandlerGenerator` (created in KSP-004) generates `GeneratedDeepLinkHandlerImpl.kt` but does **not import destination classes**, causing compilation errors when the generated code references destinations from other packages.

### Problem

When KSP processes `@Destination` annotations in a module (e.g., `quo-vadis-recipes`), the generated `GeneratedDeepLinkHandlerImpl.kt` references destination classes like `ProductsDestination.List` without importing them:

```kotlin
// Generated file: com/jermey/quo/vadis/generated/GeneratedDeepLinkHandlerImpl.kt
package com.jermey.quo.vadis.generated

// ❌ Missing import:
// import com.jermey.quo.vadis.recipes.deeplink.ProductsDestination
// import com.jermey.quo.vadis.recipes.deeplink.CategoryDestination

object GeneratedDeepLinkHandlerImpl : GeneratedDeepLinkHandler {
    private val routes = listOf(
        RoutePattern("products", emptyList()) { ProductsDestination.List },  // ❌ Unresolved reference
        // ...
    )
}
```

### Root Cause

In `DeepLinkHandlerGenerator.kt`, the `buildDestinationClassName()` method returns only the simple class name (e.g., `ProductsDestination.List`) without the package:

```kotlin
private fun buildDestinationClassName(dest: DestinationInfo): String {
    return if (dest.parentSealedClass != null) {
        "${dest.parentSealedClass}.${dest.className}"
    } else {
        dest.className
    }
}
```

The generated `FileSpec` does not add imports for these destination classes.

---

## Solution

### Option A: Use Fully Qualified Names (Simple)

Modify `buildDestinationClassName()` to return fully qualified names including the package:

```kotlin
private fun buildDestinationClassName(dest: DestinationInfo): String {
    val packagePrefix = if (dest.packageName.isNotEmpty()) "${dest.packageName}." else ""
    return if (dest.parentSealedClass != null) {
        "$packagePrefix${dest.parentSealedClass}.${dest.className}"
    } else {
        "$packagePrefix${dest.className}"
    }
}
```

**Pros**: Simple fix, no import management needed
**Cons**: Generated code is verbose with long class names

### Option B: Add Imports via KotlinPoet (Recommended)

Use KotlinPoet's `ClassName` to properly reference destination classes, allowing automatic import generation:

```kotlin
private fun buildDestinationClassName(dest: DestinationInfo): ClassName {
    return if (dest.parentSealedClass != null) {
        ClassName(dest.packageName, dest.parentSealedClass, dest.className)
    } else {
        ClassName(dest.packageName, dest.className)
    }
}
```

Then use `%T` format specifier in CodeBlock generation:

```kotlin
CodeBlock.of(
    "RoutePattern(%S, emptyList()) { %T }",
    route,
    destinationClassName  // KotlinPoet adds import automatically
)
```

**Pros**: Clean generated code with proper imports
**Cons**: More refactoring required in generator

---

## Implementation Steps

### Step 1: Update DestinationInfo (if needed)

Ensure `DestinationInfo` contains the package name:

```kotlin
data class DestinationInfo(
    val className: String,
    val packageName: String,  // ← Ensure this is populated
    val parentSealedClass: String?,
    val route: String?,
    val routeParams: List<String>,
    val constructorParams: List<ParamInfo>,
    val isDataClass: Boolean,
    val isDataObject: Boolean,
    val declaration: KSClassDeclaration
)
```

### Step 2: Update DestinationExtractor

Ensure the extractor captures the full package name:

```kotlin
fun extractDestinationInfo(declaration: KSClassDeclaration): DestinationInfo {
    val packageName = declaration.packageName.asString()
    // ... rest of extraction
    return DestinationInfo(
        className = declaration.simpleName.asString(),
        packageName = packageName,
        // ...
    )
}
```

### Step 3: Update DeepLinkHandlerGenerator

#### Option A Implementation

```kotlin
private fun buildDestinationClassName(dest: DestinationInfo): String {
    val fqn = if (dest.packageName.isNotEmpty()) {
        "${dest.packageName}."
    } else ""
    
    return if (dest.parentSealedClass != null) {
        "$fqn${dest.parentSealedClass}.${dest.className}"
    } else {
        "$fqn${dest.className}"
    }
}
```

#### Option B Implementation (Recommended)

1. Change return type of helper to `ClassName`:

```kotlin
private fun buildDestinationClassName(dest: DestinationInfo): ClassName {
    return if (dest.parentSealedClass != null) {
        ClassName(dest.packageName, dest.parentSealedClass).nestedClass(dest.className)
    } else {
        ClassName(dest.packageName, dest.className)
    }
}
```

2. Update `buildRoutePatternInitializer()` to use `%T`:

```kotlin
private fun buildRoutePatternInitializer(dest: DestinationInfo): CodeBlock {
    val route = dest.route ?: return CodeBlock.of("")
    val params = dest.routeParams
    val destClassName = buildDestinationClassName(dest)

    return if (params.isEmpty()) {
        CodeBlock.of(
            "RoutePattern(%S, emptyList()) { %T }",
            route,
            destClassName
        )
    } else {
        val paramAssignments = params.joinToString(", ") { p ->
            "$p = params[\"$p\"]!!"
        }
        CodeBlock.of(
            "RoutePattern(%S, listOf(%L)) { params ->\n    %T(%L)\n}",
            route,
            params.joinToString(", ") { "\"$it\"" },
            destClassName,
            paramAssignments
        )
    }
}
```

3. Update `buildWhenCases()` similarly for `createDeepLinkUri()`:

```kotlin
private fun buildWhenCases(destinations: List<DestinationInfo>): CodeBlock {
    val builder = CodeBlock.builder()
    
    destinations.forEach { dest ->
        val destClassName = buildDestinationClassName(dest)
        val route = dest.route ?: return@forEach
        val params = dest.routeParams
        
        if (params.isEmpty()) {
            builder.addStatement("%T -> %S", destClassName, "\$scheme://$route")
        } else {
            val uriPath = buildUriPathWithParams(route, params)
            builder.addStatement("is %T -> %P", destClassName, "\$scheme://$uriPath")
        }
    }
    
    builder.addStatement("else -> null")
    return builder.build()
}
```

### Step 4: Test

1. Clean the build:
   ```bash
   ./gradlew :quo-vadis-recipes:clean
   ```

2. Rebuild with KSP:
   ```bash
   ./gradlew :quo-vadis-recipes:compileKotlinDesktop
   ```

3. Verify generated file has proper imports:
   ```kotlin
   // Expected: GeneratedDeepLinkHandlerImpl.kt
   package com.jermey.quo.vadis.generated
   
   import com.jermey.quo.vadis.recipes.deeplink.ProductsDestination
   import com.jermey.quo.vadis.recipes.deeplink.CategoryDestination
   // ...
   ```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/.../models/DestinationInfo.kt` | Verify | Ensure `packageName` is present |
| `quo-vadis-ksp/.../extractors/DestinationExtractor.kt` | Verify/Modify | Ensure package extraction |
| `quo-vadis-ksp/.../generators/DeepLinkHandlerGenerator.kt` | Modify | Fix import generation |

---

## Acceptance Criteria

- [ ] `DestinationInfo` contains `packageName` field
- [ ] `DestinationExtractor` populates `packageName` correctly
- [ ] `DeepLinkHandlerGenerator` generates proper imports for all destination classes
- [ ] Generated `GeneratedDeepLinkHandlerImpl.kt` compiles without "Unresolved reference" errors
- [ ] Works for destinations in any package (not just the generated package)
- [ ] Works for nested sealed class members (e.g., `ProductsDestination.Detail`)
- [ ] `:quo-vadis-recipes:compileKotlinDesktop` passes after enabling `@Destination` annotations

---

## Testing Notes

### Before Fix

```
e: GeneratedDeepLinkHandlerImpl.kt:22:49 Unresolved reference 'ProductsDestination'
e: GeneratedDeepLinkHandlerImpl.kt:23:58 Unresolved reference 'ProductsDestination'
...
```

### After Fix

```bash
./gradlew :quo-vadis-recipes:compileKotlinDesktop
# BUILD SUCCESSFUL
```

### Manual Verification

Check generated file at:
```
quo-vadis-recipes/build/generated/ksp/metadata/commonMain/kotlin/com/jermey/quo/vadis/generated/GeneratedDeepLinkHandlerImpl.kt
```

Expected imports:
```kotlin
import com.jermey.quo.vadis.recipes.deeplink.ProductsDestination
import com.jermey.quo.vadis.recipes.deeplink.CategoryDestination
```

---

## Related Issues

This bug was discovered when implementing MIG-006 (Deep Linking Recipe). The recipe files had to comment out `@Destination` annotations as a workaround:

```kotlin
// @Destination(route = "products") - commented to avoid KSP processing in recipes module
data object List : ProductsDestination()
```

Once this fix is applied, the annotations can be uncommented in the recipes module.

---

## References

- [KSP-004](./KSP-004-deep-link-handler.md) - Original Deep Link Handler Generator task
- [MIG-006](../phase5-migration/MIG-006-deep-linking-recipe.md) - Deep Linking Recipe (affected by this bug)
- [KotlinPoet ClassName docs](https://square.github.io/kotlinpoet/1.x/kotlinpoet/com.squareup.kotlinpoet/-class-name/)
