```markdown
# Task ANN-005: Define @Screen Content Binding Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-005 |
| **Name** | Define @Screen Content Binding Annotation |
| **Phase** | 3 - Annotations |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | ANN-001 (depends on @Destination) |

## Overview

Create the `@Screen` annotation that binds a Composable function to render a specific navigation destination. When processed by KSP, each `@Screen`-annotated function is registered in the `GeneratedScreenRegistry`, enabling the `QuoVadisHost` to look up and invoke the correct Composable for any destination.

The `@Screen` annotation:
- Targets **functions only** (specifically `@Composable` functions)
- Links a Composable to a specific `@Destination`-annotated class
- Supports different function signatures based on destination type
- Enables optional shared element transition parameters

## Implementation

### File Location

`quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt`

### Annotation Definition

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Binds a Composable function to render a specific navigation destination.
 *
 * Apply this annotation to `@Composable` functions to register them as the
 * renderer for a particular destination. When the navigation state changes
 * to show a destination, `QuoVadisHost` uses the `GeneratedScreenRegistry`
 * (produced by KSP) to find and invoke the matching Composable.
 *
 * ## Function Signature Requirements
 *
 * The annotated function's parameters are detected by KSP based on their types.
 * Parameter order matters for proper detection.
 *
 * ### Simple Destinations (data objects)
 *
 * For destinations without data, the function receives only the [Navigator]:
 *
 * ```kotlin
 * @Screen(HomeDestination.Feed::class)
 * @Composable
 * fun FeedScreen(navigator: Navigator) {
 *     // Render feed content
 * }
 * ```
 *
 * ### Destinations with Data (data classes)
 *
 * For destinations carrying data, include the destination instance as the
 * first parameter, followed by [Navigator]:
 *
 * ```kotlin
 * @Screen(HomeDestination.Detail::class)
 * @Composable
 * fun DetailScreen(destination: HomeDestination.Detail, navigator: Navigator) {
 *     // Access destination.articleId, destination.title, etc.
 * }
 * ```
 *
 * ### With Shared Element Scopes (optional)
 *
 * To participate in shared element transitions, add optional scope parameters.
 * These are nullable and provided by `QuoVadisHost` when transitions are active:
 *
 * ```kotlin
 * @Screen(HomeDestination.Detail::class)
 * @Composable
 * fun DetailScreen(
 *     destination: HomeDestination.Detail,
 *     navigator: Navigator,
 *     sharedTransitionScope: SharedTransitionScope?,
 *     animatedVisibilityScope: AnimatedVisibilityScope?
 * ) {
 *     // Use scopes for shared element modifiers
 * }
 * ```
 *
 * ## KSP Processing
 *
 * KSP generates entries in `GeneratedScreenRegistry` mapping each destination
 * class to its Composable renderer. The registry is used by `QuoVadisHost`
 * at runtime to resolve which Composable to display.
 *
 * ## NavNode Mapping
 *
 * ```
 * @Screen(Destination::class) → GeneratedScreenRegistry entry
 *                             → QuoVadisHost renders via ScreenNode
 * ```
 *
 * @property destination The destination class this Composable renders.
 *                       Must be a class annotated with [@Destination].
 *
 * @see Destination
 * @see Stack
 * @see Tab
 * @see Pane
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Screen(
    /**
     * The destination class this composable renders.
     * Must be a class annotated with @Destination.
     */
    val destination: KClass<*>
)
```

## Function Signature Patterns

KSP detects parameters by type. The following patterns are supported:

### Pattern 1: Simple Destinations (data objects)

For destinations without parameters, only `Navigator` is required:

```kotlin
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        Text("Feed Screen")
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }
    }
}
```

**Parameter Requirements:**
| Parameter | Type | Required |
|-----------|------|----------|
| `navigator` | `Navigator` | ✅ Yes |

### Pattern 2: Destinations with Data (data classes)

For destinations carrying parameters, the destination instance comes first:

```kotlin
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Column {
        Text("Article: ${destination.articleId}")
        Button(onClick = { navigator.pop() }) {
            Text("Go Back")
        }
    }
}
```

**Parameter Requirements:**
| Parameter | Type | Required |
|-----------|------|----------|
| `destination` | Destination type | ✅ Yes |
| `navigator` | `Navigator` | ✅ Yes |

### Pattern 3: With Shared Element Scopes

For shared element transitions, add optional scope parameters:

```kotlin
@Screen(HomeDestination.Detail::class)
@Composable
fun DetailScreen(
    destination: HomeDestination.Detail,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Column {
        // Use shared element modifier when scopes are available
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Image(
                    painter = painterResource(destination.imageRes),
                    contentDescription = null,
                    modifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "image-${destination.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                )
            }
        } else {
            Image(
                painter = painterResource(destination.imageRes),
                contentDescription = null
            )
        }
        
        Text("Detail: ${destination.title}")
    }
}
```

**Parameter Requirements:**
| Parameter | Type | Required |
|-----------|------|----------|
| `destination` | Destination type | ✅ Yes (if data class) |
| `navigator` | `Navigator` | ✅ Yes |
| `sharedTransitionScope` | `SharedTransitionScope?` | ❌ Optional |
| `animatedVisibilityScope` | `AnimatedVisibilityScope?` | ❌ Optional |

## Usage Examples

### Complete Navigation Graph with Screens

```kotlin
// Define destinations
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/article/{articleId}")
    data class Article(val articleId: String) : HomeDestination()
    
    @Destination(route = "home/profile/{userId}")
    data class Profile(val userId: String) : HomeDestination()
}

// Bind screens to destinations
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    LazyColumn {
        items(articles) { article ->
            ArticleCard(
                article = article,
                onClick = { navigator.navigate(HomeDestination.Article(article.id)) }
            )
        }
    }
}

@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    val article = remember { getArticle(destination.articleId) }
    
    Column {
        Text(article.title, style = MaterialTheme.typography.headlineLarge)
        Text(article.content)
        
        Button(onClick = { navigator.navigate(HomeDestination.Profile(article.authorId)) }) {
            Text("View Author")
        }
    }
}

@Screen(HomeDestination.Profile::class)
@Composable
fun ProfileScreen(destination: HomeDestination.Profile, navigator: Navigator) {
    val user = remember { getUser(destination.userId) }
    
    Column {
        Text("Profile: ${user.name}")
        Text("Posts: ${user.postCount}")
    }
}
```

### Screens with Shared Element Transitions

```kotlin
@Screen(GalleryDestination.Grid::class)
@Composable
fun GalleryGridScreen(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(images) { image ->
            val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        state = rememberSharedContentState(key = "image-${image.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }
            
            Image(
                painter = rememberAsyncImagePainter(image.url),
                contentDescription = null,
                modifier = imageModifier
                    .clickable { navigator.navigate(GalleryDestination.Detail(image.id)) }
            )
        }
    }
}

@Screen(GalleryDestination.Detail::class)
@Composable
fun GalleryDetailScreen(
    destination: GalleryDestination.Detail,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                state = rememberSharedContentState(key = "image-${destination.imageId}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = rememberAsyncImagePainter(getImageUrl(destination.imageId)),
            contentDescription = null,
            modifier = imageModifier.fillMaxSize()
        )
    }
}
```

## Generated Code

KSP generates the `GeneratedScreenRegistry` from all `@Screen` annotations:

```kotlin
// Generated: GeneratedScreenRegistry.kt

package com.example.app.navigation.generated

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.Navigator

/**
 * Registry mapping destinations to their screen composables.
 * Generated by KSP from @Screen annotations.
 */
object GeneratedScreenRegistry : ScreenRegistry {
    
    override fun getScreen(destination: Any): @Composable (ScreenContext) -> Unit {
        return when (destination) {
            is HomeDestination.Feed -> { context ->
                FeedScreen(navigator = context.navigator)
            }
            is HomeDestination.Article -> { context ->
                ArticleScreen(
                    destination = destination,
                    navigator = context.navigator
                )
            }
            is HomeDestination.Profile -> { context ->
                ProfileScreen(
                    destination = destination,
                    navigator = context.navigator
                )
            }
            is GalleryDestination.Grid -> { context ->
                GalleryGridScreen(
                    navigator = context.navigator,
                    sharedTransitionScope = context.sharedTransitionScope,
                    animatedVisibilityScope = context.animatedVisibilityScope
                )
            }
            is GalleryDestination.Detail -> { context ->
                GalleryDetailScreen(
                    destination = destination,
                    navigator = context.navigator,
                    sharedTransitionScope = context.sharedTransitionScope,
                    animatedVisibilityScope = context.animatedVisibilityScope
                )
            }
            else -> throw IllegalArgumentException("No screen registered for: $destination")
        }
    }
}

/**
 * Context provided to screen composables by QuoVadisHost.
 */
data class ScreenContext(
    val navigator: Navigator,
    val sharedTransitionScope: SharedTransitionScope? = null,
    val animatedVisibilityScope: AnimatedVisibilityScope? = null
)
```

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt` | **New** | Create @Screen annotation class |

## Acceptance Criteria

- [ ] `@Screen` annotation created with `destination` parameter of type `KClass<*>`
- [ ] `@Target` is set to `AnnotationTarget.FUNCTION`
- [ ] `@Retention` is set to `AnnotationRetention.SOURCE`
- [ ] Comprehensive KDoc documentation with examples
- [ ] Documentation covers all three function signature patterns:
  - [ ] Simple destinations (data objects) - Navigator only
  - [ ] Destinations with data (data classes) - Destination + Navigator
  - [ ] With shared element scopes (optional parameters)
- [ ] Parameter order requirements clearly documented
- [ ] Annotation is accessible from all KMP platforms (commonMain)
- [ ] Notes that Navigator is always a required parameter

## References

- [INDEX.md](../INDEX.md) - Refactoring Plan Index
- [ANN-001: @Destination Annotation](./ANN-001-graph-type.md) - Destination definition (dependency)
- [KSP-003: Graph Extractor](../phase3-ksp/KSP-003-graph-extractor.md) - Code generation for screen registry
- [RENDER-004: QuoVadisHost](../phase2-renderer/RENDER-004-quovadis-host.md) - How screens are rendered

```
