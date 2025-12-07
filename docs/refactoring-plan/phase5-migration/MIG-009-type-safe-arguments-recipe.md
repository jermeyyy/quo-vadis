# MIG-009: Type-Safe Arguments Recipe

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-009 |
| **Complexity** | Medium |
| **Estimated Time** | 0.5 days |
| **Dependencies** | PREP-001 (recipes module) |
| **Output** | `quo-vadis-recipes/src/commonMain/kotlin/.../arguments/TypeSafeArgumentsRecipe.kt` |
| **Status** | 🟢 Completed |
| **Completed** | 2025-12-07 |

## Objective

Create a comprehensive recipe file demonstrating all features of the `@Argument` annotation for type-safe navigation arguments.

## Background

The `@Argument` annotation was added in Phase 4 to provide explicit metadata for navigation parameters:

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(
    val key: String = "",      // Custom URL parameter key
    val optional: Boolean = false  // Whether parameter can be omitted
)
```

### Why @Argument?

1. **Explicit Intent**: Clearly documents which parameters are navigation arguments
2. **Custom Key Mapping**: URL parameter names can differ from property names
3. **Optional Parameters**: Support query parameters with defaults
4. **KSP Processing**: Provides richer metadata for code generation
5. **Future Extensibility**: Foundation for validation, serialization config, etc.

## Scope

### Recipe Demonstrates

1. **Basic @Argument Usage**
   - Single path parameter
   - Multiple path parameters
   
2. **Custom Key Mapping**
   - `@Argument(key = "q")` for API compatibility
   - URL parameter name differs from Kotlin property name
   
3. **Optional Arguments**
   - `@Argument(optional = true)` for query parameters
   - Default values for optional params
   - Mix of required path + optional query params
   
4. **Enum Type Arguments**
   - Type-safe enum serialization
   - Enum name mapping in URLs
   
5. **Path vs Query Parameters**
   - Best practices for when to use each
   - Resource identifiers (path) vs filters (query)
   
6. **Complex Types (Conceptual)**
   - Pattern for `@Serializable` types
   - JSON serialization in deep links

### Files Created

```
quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/arguments/
├── package-info.kt              # Package documentation
└── TypeSafeArgumentsRecipe.kt   # Comprehensive recipe (~1260 lines)
```

### Existing Files Updated

| File | Changes |
|------|---------|
| `ListDetailRecipe.kt` | Added `@Argument` to `ProductDetail.productId` |
| `BottomTabsRecipe.kt` | Added `@Argument` to `ArticleDetail.articleId`, `SearchResults.query` |
| `BranchingWizardRecipe.kt` | Added `@Argument` to `Confirmation.orderId` |
| `DeepLinkDestinations.kt` | Added `@Argument` as comments (KSP disabled) |

## Recipe Structure

### Sealed Destination Classes

```kotlin
// 1. Basic Arguments
@Stack(name = "basicArgs", startDestination = "UserList")
sealed class BasicArgumentsDestination : DestinationInterface {
    @Destination(route = "users/{userId}")
    data class UserProfile(@Argument val userId: String) : BasicArgumentsDestination()
    
    @Destination(route = "users/{userId}/posts/{postId}")
    data class UserPost(
        @Argument val userId: String,
        @Argument val postId: String
    ) : BasicArgumentsDestination()
}

// 2. Custom Key Mapping
@Destination(route = "search?q={query}")
data class SearchResults(
    @Argument(key = "query") val searchQuery: String  // URL uses "q", code uses "searchQuery"
) : CustomKeyDestination()

// 3. Optional Arguments
@Destination(route = "products/{productId}")
data class ProductDetail(
    @Argument val productId: String,                           // Required
    @Argument(optional = true) val referrer: String? = null,   // Optional
    @Argument(optional = true) val showReviews: Boolean = false
) : OptionalArgumentsDestination()

// 4. Enum Types
@Destination(route = "products/browse")
data class BrowseProducts(
    @Argument(optional = true) val category: ProductCategory? = null,
    @Argument(optional = true) val sortDirection: SortDirection = SortDirection.ASC
) : EnumArgumentsDestination()
```

### Screen Composables

```kotlin
@Screen(BasicArgumentsDestination.UserProfile::class)
@Composable
fun UserProfileScreen(
    destination: BasicArgumentsDestination.UserProfile,
    navigator: Navigator
) {
    val userId = destination.userId  // Direct property access
    // ...
}
```

### Migration Checklist

The recipe includes a `TypeSafeArgumentsMigrationChecklist` object documenting:
- When to add `@Argument` annotation
- `key` vs default key behavior  
- `optional` parameter rules
- Enum and complex type handling
- Common mistakes to avoid

## Acceptance Criteria

- [x] Recipe demonstrates all 6 `@Argument` patterns
- [x] Deep link examples in KDoc for each pattern
- [x] Before/after migration examples where applicable
- [x] `@Screen` composables showing argument access
- [x] `TypeSafeArgumentsApp` entry point composable
- [x] `TypeSafeArgumentsMigrationChecklist` object
- [x] Existing recipes updated with `@Argument`
- [x] Build passes: `:quo-vadis-recipes:compileKotlinMetadata` ✓

## Related Tasks

- [PREP-001](./PREP-001-recipes-module.md) - Recipes module setup
- [MIG-002](./MIG-002-master-detail-example.md) - Master-detail (updated with @Argument)
- [MIG-003](./MIG-003-tabbed-navigation-example.md) - Tabs (updated with @Argument)
- [MIG-004](./MIG-004-process-flow-example.md) - Wizard (updated with @Argument)
- [MIG-006](./MIG-006-deep-linking-recipe.md) - Deep linking (updated with @Argument)

## Notes

- The `@Argument` annotation works with route templates (`{param}`)
- KSP uses `@Argument` metadata for deep link parsing
- Optional arguments must have default values in the data class
- Enum types are serialized/deserialized by name
- Complex types require `@Serializable` and JSON encoding

## Verification

```bash
# Compile recipes module
./gradlew :quo-vadis-recipes:compileKotlinMetadata

# Verify no detekt issues (optional)
./gradlew :quo-vadis-recipes:detekt
```
