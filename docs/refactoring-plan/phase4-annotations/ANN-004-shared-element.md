# Task ANN-004: Define @Pane and @PaneItem Annotations

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-004 |
| **Name** | Define @Pane and @PaneItem Annotations |
| **Phase** | 3 - Annotations |
| **Complexity** | Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001, ANN-002 |

## Overview

Define the `@Pane` and `@PaneItem` annotations for adaptive layout containers that support split-view and responsive layouts. A `@Pane`-annotated sealed class becomes a `PaneNode` in the navigation tree, enabling list-detail patterns, multi-column layouts, and adaptive UI that responds to screen size changes.

The `@Pane` annotation marks the container with back navigation behavior configuration, while `@PaneItem` defines each pane's role, adaptation strategy, and root graph reference.

## Implementation

### File Location

`quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt`

### Annotation and Enum Definitions

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Defines back navigation behavior within a pane container.
 *
 * Controls how back gestures and navigation actions are handled
 * when multiple panes are visible in an expanded layout.
 */
enum class PaneBackBehavior {
    /**
     * Pop until the scaffold visibility state changes.
     * Back navigation continues until a pane becomes hidden or shown.
     * Best for: List-detail patterns where back should return to list-only view.
     */
    PopUntilScaffoldValueChange,

    /**
     * Pop until the content in any pane changes.
     * Back navigation stops when any visible pane's content updates.
     * Best for: Complex layouts where content changes are significant.
     */
    PopUntilContentChange,

    /**
     * Simple pop from the currently active stack.
     * Standard back behavior without pane-aware logic.
     * Best for: Simple layouts or when fine-grained control is needed.
     */
    PopLatest
}

/**
 * Defines the role of a pane within the adaptive layout.
 *
 * Roles determine layout positioning and default behavior
 * when screen space is limited.
 */
enum class PaneRole {
    /**
     * Primary content pane, always visible.
     * Typically contains the main navigation list or content.
     * Examples: Email list, file browser, settings menu.
     */
    PRIMARY,

    /**
     * Secondary/supporting content pane.
     * Shows detail or supplementary content related to primary selection.
     * Examples: Email detail, file preview, setting details.
     */
    SECONDARY,

    /**
     * Optional extra content pane for three-column layouts.
     * Additional information or tertiary navigation.
     * Examples: Properties panel, comments sidebar, quick actions.
     */
    EXTRA
}

/**
 * Defines how a pane adapts when screen space is limited.
 *
 * Controls the pane's behavior during layout transitions
 * between expanded (multi-pane) and compact (single-pane) modes.
 */
enum class AdaptStrategy {
    /**
     * Hide the pane completely on compact screens.
     * Content becomes inaccessible until screen expands.
     * Best for: Optional content that isn't essential.
     */
    HIDE,

    /**
     * Collapse the pane to a minimal representation.
     * May show icons only or a narrow summary strip.
     * Best for: Navigation rails or tool panels.
     */
    COLLAPSE,

    /**
     * Show the pane as an overlay/modal on compact screens.
     * Pane appears above primary content when activated.
     * Best for: Detail views that need full attention.
     */
    OVERLAY,

    /**
     * Reflow the pane below/beside the primary content.
     * Panes stack vertically or adapt to available space.
     * Best for: Content that should remain visible but can reposition.
     */
    REFLOW
}

/**
 * Marks a sealed class/interface as an adaptive pane container.
 *
 * A pane container defines a responsive layout with multiple content areas
 * that adapt to screen size. On large screens, multiple panes are visible
 * simultaneously (list-detail, three-column). On compact screens, panes
 * are shown individually with navigation between them.
 *
 * The container is transformed into a [PaneNode] in the navigation tree,
 * coordinating navigation state across all panes.
 *
 * Panes support:
 * - Adaptive layouts responding to window size
 * - Coordinated back navigation across panes
 * - Independent navigation stacks per pane
 * - Configurable adaptation strategies
 *
 * @param name Unique identifier for this pane container. Used in route generation
 *             and navigation tree construction (e.g., "catalog", "mailbox").
 * @param backBehavior Controls how back navigation operates when multiple panes
 *                     are visible. See [PaneBackBehavior] for options.
 *
 * @sample List-detail pattern
 * ```kotlin
 * @Pane(name = "catalog", backBehavior = PaneBackBehavior.PopUntilContentChange)
 * sealed class CatalogPane : Destination {
 *
 *     @PaneItem(role = PaneRole.PRIMARY, rootGraph = ListDestination::class)
 *     @Destination(route = "catalog/list")
 *     data object List : CatalogPane()
 *
 *     @PaneItem(
 *         role = PaneRole.SECONDARY,
 *         adaptStrategy = AdaptStrategy.OVERLAY,
 *         rootGraph = DetailDestination::class
 *     )
 *     @Destination(route = "catalog/detail/{id}")
 *     data class Detail(val id: String) : CatalogPane()
 * }
 * ```
 *
 * @sample Three-column layout
 * ```kotlin
 * @Pane(name = "mail", backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange)
 * sealed class MailPane : Destination {
 *
 *     @PaneItem(role = PaneRole.PRIMARY, rootGraph = FoldersGraph::class)
 *     @Destination(route = "mail/folders")
 *     data object Folders : MailPane()
 *
 *     @PaneItem(role = PaneRole.SECONDARY, rootGraph = InboxGraph::class)
 *     @Destination(route = "mail/inbox")
 *     data object Inbox : MailPane()
 *
 *     @PaneItem(
 *         role = PaneRole.EXTRA,
 *         adaptStrategy = AdaptStrategy.OVERLAY,
 *         rootGraph = MessageGraph::class
 *     )
 *     @Destination(route = "mail/message/{id}")
 *     data class Message(val id: String) : MailPane()
 * }
 * ```
 *
 * @see PaneItem
 * @see PaneRole
 * @see PaneBackBehavior
 * @see AdaptStrategy
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Pane(
    /**
     * Unique name for this pane container.
     */
    val name: String,

    /**
     * Back navigation behavior within panes.
     */
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
)

/**
 * Provides metadata for a pane within a [@Pane] container.
 *
 * Applied to sealed subclasses of a [@Pane]-annotated class to configure
 * the pane's layout role, adaptation behavior, and navigation content.
 * Each pane item references a root graph (a [@Stack]-annotated sealed class)
 * that defines the navigation destinations available within that pane.
 *
 * @param role The pane's role in the layout hierarchy. See [PaneRole].
 *             Determines positioning and default visibility.
 * @param adaptStrategy How the pane adapts when screen space is limited.
 *                      See [AdaptStrategy]. Defaults to [AdaptStrategy.HIDE].
 * @param rootGraph The root navigation graph class for this pane's content.
 *                  Must be a sealed class annotated with [@Stack].
 *
 * @sample Primary pane (always visible)
 * ```kotlin
 * @PaneItem(role = PaneRole.PRIMARY, rootGraph = ProductListGraph::class)
 * @Destination(route = "shop/products")
 * data object Products : ShopPane()
 * ```
 *
 * @sample Secondary pane with overlay adaptation
 * ```kotlin
 * @PaneItem(
 *     role = PaneRole.SECONDARY,
 *     adaptStrategy = AdaptStrategy.OVERLAY,
 *     rootGraph = ProductDetailGraph::class
 * )
 * @Destination(route = "shop/product/{productId}")
 * data class ProductDetail(val productId: String) : ShopPane()
 * ```
 *
 * @sample Extra pane with collapse adaptation
 * ```kotlin
 * @PaneItem(
 *     role = PaneRole.EXTRA,
 *     adaptStrategy = AdaptStrategy.COLLAPSE,
 *     rootGraph = CartGraph::class
 * )
 * @Destination(route = "shop/cart")
 * data object Cart : ShopPane()
 * ```
 *
 * @see Pane
 * @see PaneRole
 * @see AdaptStrategy
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PaneItem(
    /**
     * Role of this pane in the layout.
     */
    val role: PaneRole,

    /**
     * Adaptation strategy when space is limited.
     */
    val adaptStrategy: AdaptStrategy = AdaptStrategy.HIDE,

    /**
     * Root graph for this pane's content.
     * Must be a sealed class annotated with @Stack.
     */
    val rootGraph: KClass<*>
)
```

## Usage Examples

### List-Detail Pattern (Two-Pane)

```kotlin
// Define the pane container
@Pane(name = "catalog", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class CatalogPane : Destination {

    @PaneItem(role = PaneRole.PRIMARY, rootGraph = ListDestination::class)
    @Destination(route = "catalog/list")
    data object List : CatalogPane()

    @PaneItem(
        role = PaneRole.SECONDARY,
        adaptStrategy = AdaptStrategy.OVERLAY,
        rootGraph = DetailDestination::class
    )
    @Destination(route = "catalog/detail/{id}")
    data class Detail(val id: String) : CatalogPane()
}

// Define each pane's stack
@Stack(name = "catalogList", startDestination = "ProductList")
sealed class ListDestination : Destination {
    @Destination(route = "catalog/products")
    data object ProductList : ListDestination()

    @Destination(route = "catalog/category/{categoryId}")
    data class Category(val categoryId: String) : ListDestination()
}

@Stack(name = "catalogDetail", startDestination = "ProductDetail")
sealed class DetailDestination : Destination {
    @Destination(route = "catalog/product/{productId}")
    data class ProductDetail(val productId: String) : DetailDestination()

    @Destination(route = "catalog/product/{productId}/reviews")
    data class Reviews(val productId: String) : DetailDestination()
}
```

### Email App Pattern (Three-Pane)

```kotlin
@Pane(name = "mail", backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange)
sealed class MailPane : Destination {

    @PaneItem(
        role = PaneRole.PRIMARY,
        adaptStrategy = AdaptStrategy.COLLAPSE,  // Shows as rail on medium screens
        rootGraph = FoldersGraph::class
    )
    @Destination(route = "mail/folders")
    data object Folders : MailPane()

    @PaneItem(
        role = PaneRole.SECONDARY,
        adaptStrategy = AdaptStrategy.REFLOW,
        rootGraph = MessageListGraph::class
    )
    @Destination(route = "mail/messages")
    data object Messages : MailPane()

    @PaneItem(
        role = PaneRole.EXTRA,
        adaptStrategy = AdaptStrategy.OVERLAY,
        rootGraph = MessageDetailGraph::class
    )
    @Destination(route = "mail/message/{messageId}")
    data class MessageDetail(val messageId: String) : MailPane()
}
```

### App Entry Point with Adaptive Layout

```kotlin
@Composable
fun App() {
    // KSP-generated function builds the PaneNode tree
    val navTree = remember { buildCatalogPaneNavNode() }
    val navigator = rememberNavigator(navTree)

    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry
    ) { paneState ->
        // Optional: Custom pane scaffold rendering
        AdaptiveScaffold(
            paneState = paneState,
            primaryPane = { PaneContent(paneState.primary) },
            secondaryPane = { PaneContent(paneState.secondary) }
        )
    }
}
```

### Navigation Within Panes

```kotlin
@Screen(ListDestination.ProductList::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // Navigate in secondary pane (or overlay on compact)
                    navigator.navigateToPane(
                        CatalogPane.Detail(product.id),
                        PaneRole.SECONDARY
                    )
                }
            )
        }
    }
}
```

## Generated Code

KSP generates the following from `@Pane` annotations:

```kotlin
// Generated: CatalogPaneNavNode.kt

/**
 * Builds the navigation tree for CatalogPane.
 */
fun buildCatalogPaneNavNode(): PaneNode {
    return PaneNode(
        key = "catalog",
        parentKey = null,
        panes = mapOf(
            PaneRole.PRIMARY to buildListDestinationNavNode().copy(parentKey = "catalog"),
            PaneRole.SECONDARY to buildDetailDestinationNavNode().copy(parentKey = "catalog")
        ),
        backBehavior = PaneBackBehavior.PopUntilContentChange,
        paneMetadata = mapOf(
            PaneRole.PRIMARY to PaneMetadata(
                adaptStrategy = AdaptStrategy.HIDE
            ),
            PaneRole.SECONDARY to PaneMetadata(
                adaptStrategy = AdaptStrategy.OVERLAY
            )
        )
    )
}

/**
 * Pane metadata for layout rendering.
 */
data class PaneMetadata(
    val adaptStrategy: AdaptStrategy
)
```

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt` | New | @Pane, @PaneItem annotations and related enums |

## Acceptance Criteria

- [ ] `@Pane` annotation created with `name` and `backBehavior` parameters
- [ ] `@PaneItem` annotation created with `role`, `adaptStrategy`, and `rootGraph` parameters
- [ ] `PaneBackBehavior` enum created with `PopUntilScaffoldValueChange`, `PopUntilContentChange`, `PopLatest`
- [ ] `PaneRole` enum created with `PRIMARY`, `SECONDARY`, `EXTRA`
- [ ] `AdaptStrategy` enum created with `HIDE`, `COLLAPSE`, `OVERLAY`, `REFLOW`
- [ ] Both annotations have `@Target(AnnotationTarget.CLASS)`
- [ ] Both annotations have `@Retention(AnnotationRetention.SOURCE)`
- [ ] Comprehensive KDoc documentation with examples for all types
- [ ] `backBehavior` defaults to `PopUntilScaffoldValueChange`
- [ ] `adaptStrategy` defaults to `HIDE`
- [ ] `rootGraph` parameter accepts `KClass<*>` for stack references

## References

- [INDEX.md](../INDEX.md) - Overall refactoring plan
- [CORE-001: NavNode Hierarchy](../phase1-core/CORE-001-navnode-hierarchy.md) - PaneNode definition
- [RENDER-004: QuoVadisHost](../phase2-renderer/RENDER-004-quovadis-host.md) - Adaptive rendering
- [KSP-002: Class References Generator](../phase3-ksp/KSP-002-class-references.md) - Code generation details
