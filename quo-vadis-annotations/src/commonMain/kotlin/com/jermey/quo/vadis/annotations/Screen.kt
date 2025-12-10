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
 * For destinations without data, only `Navigator` is required:
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
 * For destinations carrying parameters, include the destination instance as the
 * first parameter, followed by `Navigator`:
 *
 * ```kotlin
 * @Screen(HomeDestination.Article::class)
 * @Composable
 * fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
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
 * @see Tabs
 * @see Pane
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class Screen(
    /**
     * The destination class this composable renders.
     * Must be a class annotated with @Destination.
     */
    val destination: KClass<*>,
)
