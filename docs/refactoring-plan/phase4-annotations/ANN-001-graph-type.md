# Task ANN-001: Define @Destination Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-001 |
| **Name** | Define @Destination Annotation |
| **Phase** | 3 - Annotations |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |

## Overview

Create the `@Destination` annotation that marks a class or object as a navigation target. When processed by KSP, destinations become `ScreenNode` instances in the navigation tree. This is the foundational annotation for defining navigable screens in the Quo Vadis navigation system.

The `@Destination` annotation is used on:
- **Data objects** for simple screens without parameters
- **Data classes** for screens that require navigation arguments

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt

package com.jermey.quo.vadis.annotations

/**
 * Marks a class or object as a navigation destination.
 *
 * Destinations represent individual screens or views in the navigation graph.
 * When processed by KSP, each destination becomes a [ScreenNode] in the
 * navigation tree.
 *
 * ## Usage
 *
 * Apply to data objects for parameter-less destinations:
 * ```kotlin
 * @Destination(route = "home")
 * data object Home : HomeDestination()
 * ```
 *
 * Apply to data classes for destinations with parameters:
 * ```kotlin
 * @Destination(route = "profile/{userId}")
 * data class Profile(val userId: String) : HomeDestination()
 * ```
 *
 * ## Deep Linking
 *
 * The [route] parameter enables deep linking support:
 * - Path parameters: `"profile/{userId}"` extracts `userId` from the URI
 * - Query parameters: `"search?query={q}"` extracts `q` from query string
 * - Empty route means the destination is not deep-linkable
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [ScreenNode] in the NavNode hierarchy:
 * ```
 * @Destination → ScreenNode(destination = <instance>)
 * ```
 *
 * @property route Route path for deep linking. Supports path parameters
 *   (`{param}`) and query parameters (`?key={value}`). If empty, the
 *   destination is not accessible via deep links.
 *
 * @see Stack
 * @see Tab
 * @see Pane
 * @see Screen
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Destination(
    val route: String = ""
)
```

## Usage Examples

### Basic Destinations (Data Objects)

```kotlin
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/settings")
    data object Settings : HomeDestination()
    
    @Destination  // No route = not deep-linkable
    data object Onboarding : HomeDestination()
}
```

### Destinations with Parameters (Data Classes)

```kotlin
@Stack(name = "profile", startDestination = "Overview")
sealed class ProfileDestination {

    @Destination(route = "profile/overview")
    data object Overview : ProfileDestination()

    @Destination(route = "profile/user/{userId}")
    data class UserDetail(val userId: String) : ProfileDestination()

    @Destination(route = "profile/settings/{section}?highlight={field}")
    data class SettingsSection(
        val section: String,
        val field: String? = null
    ) : ProfileDestination()
}
```

### Deep Link Route Patterns

```kotlin
// Simple route
@Destination(route = "home")
data object Home : AppDestination()

// Path parameter
@Destination(route = "article/{articleId}")
data class Article(val articleId: String) : AppDestination()

// Multiple path parameters
@Destination(route = "user/{userId}/post/{postId}")
data class UserPost(val userId: String, val postId: String) : AppDestination()

// Query parameters
@Destination(route = "search?query={q}&filter={f}")
data class Search(val q: String, val f: String? = null) : AppDestination()

// Not deep-linkable (empty route)
@Destination
data object InternalScreen : AppDestination()
```

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt` | **New** | Create annotation class |

## Acceptance Criteria

- [ ] `@Destination` annotation created with `route` parameter
- [ ] Default value for `route` is empty string (not deep-linkable)
- [ ] `@Target` is set to `AnnotationTarget.CLASS`
- [ ] `@Retention` is set to `AnnotationRetention.SOURCE`
- [ ] Comprehensive KDoc documentation with examples
- [ ] Annotation is accessible from all KMP platforms (commonMain)
- [ ] Unit tests verify annotation retention and target

## References

- [INDEX.md](../INDEX.md) - Refactoring Plan Index
