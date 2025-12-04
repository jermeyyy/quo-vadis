# Task ANN-002: Define @Stack Container Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-002 |
| **Name** | Define @Stack Container Annotation |
| **Phase** | 3 - Annotations |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | ANN-001 |

## Overview

Create the `@Stack` annotation that marks a sealed class or interface as a stack-based navigation container. When processed by KSP, stacks become `StackNode` instances in the navigation tree, providing linear push/pop navigation behavior.

The `@Stack` annotation leverages Kotlin's sealed class hierarchy:
- The annotated sealed class defines the navigation graph boundary
- All sealed subclasses automatically become destinations within the stack
- Each subclass must be annotated with `@Destination`

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt

package com.jermey.quo.vadis.annotations

/**
 * Marks a sealed class or interface as a stack-based navigation container.
 *
 * Stack navigation provides linear, push/pop behavior where destinations
 * are added to and removed from a stack. Back navigation pops the top
 * destination, revealing the previous one.
 *
 * ## Usage
 *
 * Apply to a sealed class containing destination subclasses:
 * ```kotlin
 * @Stack(name = "home", startDestination = "Feed")
 * sealed class HomeDestination {
 *
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 *
 *     @Destination(route = "home/detail/{id}")
 *     data class Detail(val id: String) : HomeDestination()
 * }
 * ```
 *
 * ## Sealed Class Requirements
 *
 * - Must be a `sealed class` or `sealed interface`
 * - All direct subclasses must be annotated with [@Destination]
 * - Subclasses can be `data object` (no params) or `data class` (with params)
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [StackNode] in the NavNode hierarchy:
 * ```
 * @Stack → StackNode(
 *     key = "{name}-stack",
 *     children = [ScreenNode for each @Destination subclass]
 * )
 * ```
 *
 * ## Start Destination Resolution
 *
 * The [startDestination] is resolved by matching against sealed subclass
 * simple names:
 * - `"Feed"` matches `data object Feed : HomeDestination()`
 * - `"Detail"` matches `data class Detail(...) : HomeDestination()`
 *
 * If [startDestination] is empty, the first destination in declaration
 * order is used as the initial screen.
 *
 * @property name Unique name for this navigation stack. Used for key
 *   generation, debugging, and identification in the navigation tree.
 * @property startDestination Simple name of the initial destination subclass.
 *   Must match one of the sealed subclass names exactly. If empty, the
 *   first declared subclass is used.
 *
 * @see Destination
 * @see Tab
 * @see Pane
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Stack(
    val name: String,
    val startDestination: String = ""
)
```

## Usage Examples

### Basic Stack Navigation

```kotlin
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/detail/{id}")
    data class Detail(val id: String) : HomeDestination()

    @Destination(route = "home/settings")
    data object Settings : HomeDestination()
}
```

### Stack with Default Start Destination

```kotlin
// First destination (Overview) is used as start when startDestination is empty
@Stack(name = "profile")
sealed class ProfileDestination {

    @Destination(route = "profile/overview")
    data object Overview : ProfileDestination()  // ← Default start

    @Destination(route = "profile/edit")
    data object Edit : ProfileDestination()

    @Destination(route = "profile/settings")
    data object Settings : ProfileDestination()
}
```

### Stack with Complex Destinations

```kotlin
@Stack(name = "product", startDestination = "List")
sealed class ProductDestination {

    @Destination(route = "products")
    data object List : ProductDestination()

    @Destination(route = "products/{productId}")
    data class Detail(val productId: String) : ProductDestination()

    @Destination(route = "products/{productId}/reviews")
    data class Reviews(val productId: String) : ProductDestination()

    @Destination(route = "products/compare?ids={productIds}")
    data class Compare(val productIds: List<String>) : ProductDestination()
}
```

### Generated NavNode Structure

For the `HomeDestination` stack above, KSP generates:

```kotlin
fun buildHomeDestinationNavNode(
    key: String = "home-stack",
    parentKey: String? = null
): StackNode {
    return StackNode(
        key = key,
        parentKey = parentKey,
        children = listOf(
            ScreenNode(
                key = "$key/feed",
                parentKey = key,
                destination = HomeDestination.Feed
            )
        )
    )
}
```

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt` | **New** | Create annotation class |

## Acceptance Criteria

- [ ] `@Stack` annotation created with `name` and `startDestination` parameters
- [ ] `name` parameter is required (no default value)
- [ ] `startDestination` parameter defaults to empty string
- [ ] `@Target` is set to `AnnotationTarget.CLASS`
- [ ] `@Retention` is set to `AnnotationRetention.SOURCE`
- [ ] Comprehensive KDoc documentation with examples
- [ ] Documentation explains sealed class requirements
- [ ] Documentation explains start destination resolution
- [ ] Annotation is accessible from all KMP platforms (commonMain)
- [ ] Unit tests verify annotation retention and target

## References

- [INDEX.md](../INDEX.md) - Refactoring Plan Index
