# Task ANN-001: Define GraphType Enumeration

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-001 |
| **Name** | Define GraphType Enumeration |
| **Phase** | 4 - Annotations Enhancement |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |

## Overview

Create the `GraphType` enum that defines the structural type of a navigation graph. This enum is used with the `@Graph` annotation to specify whether the graph represents a linear stack, tabbed navigation, or pane-based (split view) layout.

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/.../annotations/GraphType.kt

package com.jermey.quo.vadis.annotations

/**
 * Defines the structural type of a navigation graph.
 * 
 * Used with [@Graph] to specify how destinations within the graph
 * are organized and how navigation between them behaves.
 */
enum class GraphType {
    /**
     * Linear stack navigation.
     * 
     * Destinations are pushed onto and popped from a stack.
     * Back navigation removes the top destination.
     * 
     * Example use cases:
     * - Master-detail flows
     * - Wizard/process flows
     * - Standard screen-to-screen navigation
     */
    STACK,
    
    /**
     * Tabbed navigation with parallel stacks.
     * 
     * Each tab maintains its own navigation stack.
     * Switching tabs preserves the stack state of inactive tabs.
     * 
     * Example use cases:
     * - Bottom navigation bars
     * - Top tab bars
     * - Navigation rails
     */
    TAB,
    
    /**
     * Pane-based navigation for adaptive layouts.
     * 
     * Multiple destinations are displayed simultaneously.
     * Typically used for split-view on tablets/desktop.
     * 
     * Example use cases:
     * - List-detail split view
     * - Multi-column layouts
     * - Responsive layouts that adapt to screen size
     */
    PANE,
    
    /**
     * Custom graph type for advanced use cases.
     * 
     * Allows implementing custom navigation patterns
     * not covered by the standard types.
     * 
     * Note: Custom types require manual implementation
     * of the corresponding NavNode structure.
     */
    CUSTOM
}
```

### Update @Graph Annotation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/.../annotations/Annotations.kt

/**
 * Marks a sealed class as a navigation graph.
 * 
 * @param name The unique name for this graph
 * @param type The structural type of the graph (default: STACK)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(
    val name: String = "",
    val type: GraphType = GraphType.STACK
)
```

## Usage Examples

```kotlin
// Linear stack graph (default)
@Graph(name = "settings")
sealed class SettingsGraph {
    @Route object Main : SettingsGraph()
    @Route object Privacy : SettingsGraph()
    @Route object Notifications : SettingsGraph()
}

// Tabbed graph
@Graph(name = "main", type = GraphType.TAB)
sealed class MainGraph {
    @Route object Home : MainGraph()
    @Route object Search : MainGraph()
    @Route object Profile : MainGraph()
}

// Pane graph for adaptive layouts
@Graph(name = "catalog", type = GraphType.PANE)
sealed class CatalogGraph {
    @Route object List : CatalogGraph()
    @Route data class Detail(val id: String) : CatalogGraph()
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/GraphType.kt` | New |
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/Annotations.kt` | Modify |

## Acceptance Criteria

- [ ] `GraphType` enum created with STACK, TAB, PANE, CUSTOM values
- [ ] Each value has comprehensive KDoc documentation
- [ ] `@Graph` annotation updated with `type` parameter
- [ ] Default value is `GraphType.STACK` for backward compatibility
- [ ] Enum is accessible from all KMP platforms

## References

- [KSP-001: Enhance @Graph Annotation](../phase3-ksp/KSP-001-graph-type-enum.md)
- [Original Refactoring Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
