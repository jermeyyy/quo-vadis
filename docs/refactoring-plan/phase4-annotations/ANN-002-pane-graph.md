# Task ANN-002: Create @PaneGraph Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-002 |
| **Name** | Create @PaneGraph Annotation |
| **Phase** | 4 - Annotations Enhancement |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001 (GraphType) |

## Overview

Create dedicated annotations for pane-based (split view) navigation, enabling adaptive layouts for tablets, foldables, and desktop applications.

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/.../annotations/PaneAnnotations.kt

package com.jermey.quo.vadis.annotations

/**
 * Marks a sealed class as a pane-based navigation graph.
 * 
 * Pane graphs display multiple destinations simultaneously,
 * typically in a split-view layout.
 * 
 * @param name The unique name for this pane graph
 * @param primaryPane The key of the primary (main content) pane
 * @param secondaryPane The key of the secondary (detail) pane
 * @param adaptiveBreakpoint Window width at which to switch between
 *        single-pane and multi-pane layouts (dp)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PaneGraph(
    val name: String = "",
    val primaryPane: String = "primary",
    val secondaryPane: String = "secondary",
    val adaptiveBreakpoint: Int = 600
)

/**
 * Marks a class or object as a pane within a PaneGraph.
 * 
 * @param key Unique identifier for this pane
 * @param defaultWeight The relative weight of this pane in the layout (0.0-1.0)
 * @param minWidth Minimum width of this pane in dp (0 = no minimum)
 * @param maxWidth Maximum width of this pane in dp (0 = no maximum)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Pane(
    val key: String,
    val defaultWeight: Float = 0.5f,
    val minWidth: Int = 0,
    val maxWidth: Int = 0
)

/**
 * Specifies adaptive layout behavior for a pane graph.
 */
enum class PaneAdaptiveMode {
    /**
     * Show single pane on compact, both panes on expanded.
     */
    RESPONSIVE,
    
    /**
     * Always show both panes regardless of window size.
     */
    ALWAYS_DUAL,
    
    /**
     * Always show single pane, overlay secondary.
     */
    ALWAYS_SINGLE,
    
    /**
     * Let the system decide based on device type.
     */
    SYSTEM
}
```

## Usage Example

```kotlin
@PaneGraph(
    name = "catalog",
    primaryPane = "list",
    secondaryPane = "detail",
    adaptiveBreakpoint = 840
)
sealed class CatalogGraph {
    
    @Pane(key = "list", defaultWeight = 0.35f, maxWidth = 400)
    sealed class ListPane : CatalogGraph() {
        @Route object ProductList : ListPane()
        @Route data class CategoryList(val category: String) : ListPane()
    }
    
    @Pane(key = "detail", defaultWeight = 0.65f, minWidth = 300)
    sealed class DetailPane : CatalogGraph() {
        @Route object Empty : DetailPane()
        @Route data class ProductDetail(val productId: String) : DetailPane()
    }
}
```

## Generated Code

```kotlin
// Generated: CatalogGraphPaneBuilder.kt

fun buildCatalogGraphPaneNode(parentKey: String? = null): PaneNode {
    return PaneNode(
        key = "pane_catalog",
        parentKey = parentKey,
        panes = listOf(
            StackNode(
                key = "stack_list",
                parentKey = "pane_catalog",
                children = listOf(
                    ScreenNode(
                        key = "screen_ProductList",
                        parentKey = "stack_list",
                        destination = ProductList
                    )
                )
            ),
            StackNode(
                key = "stack_detail",
                parentKey = "pane_catalog", 
                children = listOf(
                    ScreenNode(
                        key = "screen_Empty",
                        parentKey = "stack_detail",
                        destination = Empty
                    )
                )
            )
        ),
        config = PaneConfig(
            primaryPaneKey = "stack_list",
            secondaryPaneKey = "stack_detail",
            adaptiveBreakpoint = 840.dp,
            weights = mapOf("stack_list" to 0.35f, "stack_detail" to 0.65f)
        )
    )
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/PaneAnnotations.kt` | New |

## Acceptance Criteria

- [ ] `@PaneGraph` annotation created with all parameters
- [ ] `@Pane` annotation created for individual pane definitions
- [ ] `PaneAdaptiveMode` enum provides layout options
- [ ] KDoc documentation complete
- [ ] Works across all KMP platforms

## References

- [ANN-001: GraphType](./ANN-001-graph-type.md)
- [CORE-001: NavNode Hierarchy (PaneNode)](../phase1-core/CORE-001-navnode-hierarchy.md)
