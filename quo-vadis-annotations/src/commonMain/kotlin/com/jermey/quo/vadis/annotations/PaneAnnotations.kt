package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Defines back navigation behavior within a pane container.
 *
 * Controls how back gestures and navigation actions are handled
 * when multiple panes are visible in an expanded layout.
 *
 * ## Behavior Details
 *
 * - **PopUntilScaffoldValueChange**: Back navigation continues until the scaffold's
 *   visibility state changes (e.g., a pane becomes hidden or shown). Ideal for
 *   list-detail patterns where back should return to a list-only view.
 *
 * - **PopUntilContentChange**: Back navigation stops when any visible pane's content
 *   updates. Best for complex layouts where content changes are significant
 *   navigation events.
 *
 * - **PopLatest**: Standard back behavior without pane-aware logic. Simply pops
 *   from the currently active stack. Best for simple layouts or when fine-grained
 *   control is needed.
 *
 * @see Pane
 */
enum class PaneBackBehavior {
    /**
     * Pop until the scaffold visibility state changes.
     *
     * Back navigation continues until a pane becomes hidden or shown.
     * Best for: List-detail patterns where back should return to list-only view.
     */
    PopUntilScaffoldValueChange,

    /**
     * Pop until the content in any pane changes.
     *
     * Back navigation stops when any visible pane's content updates.
     * Best for: Complex layouts where content changes are significant.
     */
    PopUntilContentChange,

    /**
     * Simple pop from the currently active stack.
     *
     * Standard back behavior without pane-aware logic.
     * Best for: Simple layouts or when fine-grained control is needed.
     */
    PopLatest
}

/**
 * Defines the role of a pane within the adaptive layout.
 *
 * Roles determine layout positioning and default behavior when screen space
 * is limited. The layout system uses these roles to determine pane placement
 * and priority during screen size transitions.
 *
 * ## Layout Behavior
 *
 * - **PRIMARY**: Always visible, typically on the left or top. Contains main
 *   navigation content (lists, menus, folders).
 *
 * - **SECONDARY**: Shows when space allows, typically in the center or right.
 *   Contains detail content related to the primary selection.
 *
 * - **EXTRA**: Shows only on large screens or as overlay. Contains supplementary
 *   content like properties, comments, or additional actions.
 *
 * @see PaneItem
 * @see Pane
 */
enum class PaneRole {
    /**
     * Primary content pane, always visible.
     *
     * Typically contains the main navigation list or content.
     * Examples: Email list, file browser, settings menu.
     */
    PRIMARY,

    /**
     * Secondary/supporting content pane.
     *
     * Shows detail or supplementary content related to primary selection.
     * Examples: Email detail, file preview, setting details.
     */
    SECONDARY,

    /**
     * Optional extra content pane for three-column layouts.
     *
     * Additional information or tertiary navigation.
     * Examples: Properties panel, comments sidebar, quick actions.
     */
    EXTRA
}

/**
 * Defines how a pane adapts when screen space is limited.
 *
 * Controls the pane's behavior during layout transitions between expanded
 * (multi-pane) and compact (single-pane) modes. The renderer uses this
 * strategy to determine how to present the pane when there isn't enough
 * space for all panes.
 *
 * ## Strategy Comparison
 *
 * | Strategy | Visible | Interactive | Screen Space |
 * |----------|---------|-------------|--------------|
 * | HIDE     | No      | No          | None         |
 * | COLLAPSE | Partial | Limited     | Minimal      |
 * | OVERLAY  | Full    | Full        | Shared       |
 * | REFLOW   | Full    | Full        | Stacked      |
 *
 * @see PaneItem
 * @see Pane
 */
enum class AdaptStrategy {
    /**
     * Hide the pane completely on compact screens.
     *
     * Content becomes inaccessible until screen expands.
     * Best for: Optional content that isn't essential.
     */
    HIDE,

    /**
     * Collapse the pane to a minimal representation.
     *
     * May show icons only or a narrow summary strip.
     * Best for: Navigation rails or tool panels.
     */
    COLLAPSE,

    /**
     * Show the pane as an overlay/modal on compact screens.
     *
     * Pane appears above primary content when activated.
     * Best for: Detail views that need full attention.
     */
    OVERLAY,

    /**
     * Reflow the pane below/beside the primary content.
     *
     * Panes stack vertically or adapt to available space.
     * Best for: Content that should remain visible but can reposition.
     */
    REFLOW
}

/**
 * Marks a sealed class or interface as an adaptive pane container.
 *
 * A pane container defines a responsive layout with multiple content areas
 * that adapt to screen size. On large screens, multiple panes are visible
 * simultaneously (list-detail, three-column). On compact screens, panes
 * are shown individually with navigation between them.
 *
 * The container is transformed into a [PaneNode] in the navigation tree,
 * coordinating navigation state across all panes.
 *
 * ## Features
 *
 * Panes support:
 * - Adaptive layouts responding to window size
 * - Coordinated back navigation across panes
 * - Independent navigation stacks per pane
 * - Configurable adaptation strategies
 *
 * ## Usage
 *
 * Apply to a sealed class where each subclass represents a pane:
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
 * ## Sealed Class Requirements
 *
 * - Must be a `sealed class` or `sealed interface`
 * - Each direct subclass must be annotated with [@PaneItem] and [@Destination]
 * - Subclasses can be `data object` or `data class` with parameters
 *
 * ## NavNode Mapping
 *
 * This annotation maps to [PaneNode] in the NavNode hierarchy:
 * ```
 * @Pane → PaneNode(
 *     key = "{name}",
 *     panes = {PaneRole → StackNode for each @PaneItem subclass},
 *     backBehavior = {backBehavior}
 * )
 * ```
 *
 * ## Generated Code
 *
 * KSP generates a builder function for the PaneNode tree:
 * ```kotlin
 * // KSP generates: buildCatalogPaneNavNode()
 * PaneNode(
 *     key = "catalog",
 *     parentKey = null,
 *     panes = mapOf(
 *         PaneRole.PRIMARY to buildListDestinationNavNode().copy(parentKey = "catalog"),
 *         PaneRole.SECONDARY to buildDetailDestinationNavNode().copy(parentKey = "catalog")
 *     ),
 *     backBehavior = PaneBackBehavior.PopUntilContentChange,
 *     paneMetadata = mapOf(
 *         PaneRole.PRIMARY to PaneMetadata(adaptStrategy = AdaptStrategy.HIDE),
 *         PaneRole.SECONDARY to PaneMetadata(adaptStrategy = AdaptStrategy.OVERLAY)
 *     )
 * )
 * ```
 *
 * ## Examples
 *
 * ### List-Detail Pattern (Two-Pane)
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
 * ### Email App Pattern (Three-Pane)
 * ```kotlin
 * @Pane(name = "mail", backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange)
 * sealed class MailPane : Destination {
 *
 *     @PaneItem(
 *         role = PaneRole.PRIMARY,
 *         adaptStrategy = AdaptStrategy.COLLAPSE,
 *         rootGraph = FoldersGraph::class
 *     )
 *     @Destination(route = "mail/folders")
 *     data object Folders : MailPane()
 *
 *     @PaneItem(
 *         role = PaneRole.SECONDARY,
 *         adaptStrategy = AdaptStrategy.REFLOW,
 *         rootGraph = MessageListGraph::class
 *     )
 *     @Destination(route = "mail/messages")
 *     data object Messages : MailPane()
 *
 *     @PaneItem(
 *         role = PaneRole.EXTRA,
 *         adaptStrategy = AdaptStrategy.OVERLAY,
 *         rootGraph = MessageDetailGraph::class
 *     )
 *     @Destination(route = "mail/message/{messageId}")
 *     data class MessageDetail(val messageId: String) : MailPane()
 * }
 * ```
 *
 * @property name Unique identifier for this pane container. Used in route
 *   generation, key construction, and navigation tree identification
 *   (e.g., "catalog", "mailbox", "settings").
 * @property backBehavior Controls how back navigation operates when multiple
 *   panes are visible. See [PaneBackBehavior] for options. Defaults to
 *   [PaneBackBehavior.PopUntilScaffoldValueChange].
 *
 * @see PaneItem
 * @see PaneRole
 * @see PaneBackBehavior
 * @see AdaptStrategy
 * @see Destination
 * @see Stack
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
 * ## Usage
 *
 * Apply to each sealed subclass within a [@Pane] container:
 * ```kotlin
 * @Pane(name = "shop", backBehavior = PaneBackBehavior.PopUntilContentChange)
 * sealed class ShopPane : Destination {
 *
 *     @PaneItem(role = PaneRole.PRIMARY, rootGraph = ProductListGraph::class)
 *     @Destination(route = "shop/products")
 *     data object Products : ShopPane()
 *
 *     @PaneItem(
 *         role = PaneRole.SECONDARY,
 *         adaptStrategy = AdaptStrategy.OVERLAY,
 *     )
 *     @Destination(route = "shop/product/{productId}")
 *     data class ProductDetail(val productId: String) : ShopPane()
 * }
 * ```
 *
 * ## Root Graph Connection
 *
 * Each pane references a root graph that defines the navigation destinations
 * available within that pane. The root graph must be a sealed class annotated
 * with [@Stack]:
 * ```kotlin
 * // Referenced stack graph
 * @Stack(name = "productList", startDestination = ProductListGraph.Products::class)
 * sealed class ProductListGraph : Destination {
 *
 *     @Destination(route = "products")
 *     data object Products : ProductListGraph()
 *
 *     @Destination(route = "products/category/{categoryId}")
 *     data class Category(val categoryId: String) : ProductListGraph()
 * }
 * ```
 *
 * ## Adaptation Strategies
 *
 * The [adaptStrategy] controls how the pane behaves when screen space is limited:
 *
 * - **HIDE**: Pane is completely hidden and inaccessible
 * - **COLLAPSE**: Pane shows a minimal representation (e.g., icons only)
 * - **OVERLAY**: Pane shows as a modal/sheet over primary content
 * - **REFLOW**: Pane repositions (e.g., below primary content)
 *
 * ## Generated Metadata
 *
 * KSP generates [PaneMetadata] for each pane item, used for layout rendering:
 * ```kotlin
 * data class PaneMetadata(
 *     val adaptStrategy: AdaptStrategy
 * )
 * ```
 *
 * ## Examples
 *
 * ### Primary Pane (Always Visible)
 * ```kotlin
 * @PaneItem(role = PaneRole.PRIMARY, rootGraph = ProductListGraph::class)
 * @Destination(route = "shop/products")
 * data object Products : ShopPane()
 * ```
 *
 * ### Secondary Pane with Overlay Adaptation
 * ```kotlin
 * @PaneItem(
 *     role = PaneRole.SECONDARY,
 *     adaptStrategy = AdaptStrategy.OVERLAY,
 * )
 * @Destination(route = "shop/product/{productId}")
 * data class ProductDetail(val productId: String) : ShopPane()
 * ```
 *
 * ### Extra Pane with Collapse Adaptation
 * ```kotlin
 * @PaneItem(
 *     role = PaneRole.EXTRA,
 *     adaptStrategy = AdaptStrategy.COLLAPSE,
 * )
 * @Destination(route = "shop/cart")
 * data object Cart : ShopPane()
 * ```
 *
 * @property role The pane's role in the layout hierarchy. See [PaneRole].
 *   Determines positioning and default visibility.
 * @property adaptStrategy How the pane adapts when screen space is limited.
 *   See [AdaptStrategy]. Defaults to [AdaptStrategy.HIDE].
 *
 * @see Pane
 * @see PaneRole
 * @see AdaptStrategy
 * @see Stack
 * @see Destination
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
)
