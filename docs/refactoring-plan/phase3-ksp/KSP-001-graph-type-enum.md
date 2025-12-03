# KSP-001: Enhance @Graph with GraphType Support

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-001 |
| **Task Name** | Enhance @Graph with GraphType Support |
| **Phase** | Phase 3: KSP Processor Updates |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | KSP-003 |

---

## Overview

This task introduces a `GraphType` enumeration to the `@Graph` annotation, enabling developers to specify the type of navigation structure a graph represents. This is foundational for the new tree-based navigation architecture, where graphs can represent:

- **Stack**: Traditional linear push/pop navigation (default)
- **Tab**: Parallel stacks with tab switching
- **Pane**: Adaptive multi-pane layouts

### Rationale

The current `@Graph` annotation assumes all graphs are linear stacks. The new `NavNode` architecture (Phase 1) supports multiple container types. By adding `GraphType` to the annotation, the KSP processor can generate appropriate `NavNode` structures (e.g., `StackNode`, `TabNode`, `PaneNode`) based on the declared type.

---

## File Location

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Annotations.kt
```

---

## Implementation

### GraphType Enumeration

```kotlin
/**
 * Defines the structural type of a navigation graph.
 *
 * The graph type determines how the generated NavNode tree is structured:
 * - [STACK]: Linear navigation with push/pop semantics
 * - [TAB]: Parallel stacks accessible via tab switching
 * - [PANE]: Adaptive layout with simultaneously visible panes
 *
 * @sample Stack graph (default behavior)
 * ```kotlin
 * @Graph("main", type = GraphType.STACK)
 * sealed class MainDestination : Destination {
 *     @Route("home") data object Home : MainDestination()
 *     @Route("detail") data class Detail(val id: String) : MainDestination()
 * }
 * // Generates: StackNode with push/pop navigation
 * ```
 *
 * @sample Tab graph
 * ```kotlin
 * @Graph("tabs", type = GraphType.TAB)
 * sealed class MainTabs : Destination {
 *     @Route("home") data object Home : MainTabs()
 *     @Route("search") data object Search : MainTabs()
 *     @Route("profile") data object Profile : MainTabs()
 * }
 * // Generates: TabNode with parallel stacks per tab
 * ```
 *
 * @sample Pane graph
 * ```kotlin
 * @Graph("master-detail", type = GraphType.PANE)
 * sealed class MasterDetailDestination : Destination {
 *     @Route("list") data object List : MasterDetailDestination()
 *     @Route("detail") data class Detail(val id: String) : MasterDetailDestination()
 * }
 * // Generates: PaneNode for adaptive layouts
 * ```
 */
enum class GraphType {
    /**
     * Linear navigation stack with push/pop semantics.
     *
     * This is the default type. Destinations are arranged in a linear history
     * where navigating forward pushes to the stack and back pops from it.
     *
     * Maps to: [StackNode]
     */
    STACK,

    /**
     * Tabbed navigation with parallel stacks.
     *
     * Each destination in the graph becomes a tab, with its own independent
     * navigation stack. Tab switching preserves per-tab history.
     *
     * Maps to: [TabNode]
     */
    TAB,

    /**
     * Adaptive pane layout with simultaneous visibility.
     *
     * Multiple destinations can be displayed simultaneously in a split-view
     * or master-detail configuration. Responsive to screen size changes.
     *
     * Maps to: [PaneNode]
     */
    PANE
}
```

### Updated @Graph Annotation

```kotlin
/**
 * Marks a sealed class as a navigation graph.
 *
 * The sealed class should extend [Destination] and contain destination objects/classes
 * representing the screens in this graph. KSP will generate:
 * - Route registration code (`{ClassName}RouteInitializer`)
 * - Graph builder function (`build{ClassName}Graph()`)
 * - NavNode builder function (`build{ClassName}NavNode()`) - NEW
 * - Typed destination extensions (for destinations with [@Argument])
 *
 * @param name The unique identifier for this navigation graph. Used in generated function names.
 * @param startDestination The simple name of the destination to use as the start destination.
 *                         If not specified, the first destination in the sealed class will be used.
 * @param type The structural type of this graph. Determines the generated NavNode container type.
 *             Defaults to [GraphType.STACK] for backward compatibility.
 *
 * @sample Basic stack graph (default)
 * ```kotlin
 * @Graph("main", startDestination = "Home")
 * sealed class MainDestination : Destination {
 *     @Route("main/home")
 *     data object Home : MainDestination()
 *
 *     @Route("main/settings")
 *     data object Settings : MainDestination()
 * }
 * ```
 *
 * @sample Tab graph
 * ```kotlin
 * @Graph("bottomNav", type = GraphType.TAB)
 * sealed class BottomNavDestination : Destination {
 *     @Route("home") data object Home : BottomNavDestination()
 *     @Route("search") data object Search : BottomNavDestination()
 *     @Route("profile") data object Profile : BottomNavDestination()
 * }
 * ```
 *
 * @sample Pane graph for adaptive layouts
 * ```kotlin
 * @Graph("adaptive", type = GraphType.PANE)
 * sealed class AdaptiveDestination : Destination {
 *     @Route("list") data object List : AdaptiveDestination()
 *     @Route("detail") data class Detail(val id: String) : AdaptiveDestination()
 * }
 * ```
 *
 * @see Route
 * @see Argument
 * @see Content
 * @see GraphType
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(
    val name: String,
    val startDestination: String = "",
    val type: GraphType = GraphType.STACK
)
```

---

## Implementation Steps

### Step 1: Create GraphType Enum (30 minutes)

Add the `GraphType` enum to `Annotations.kt`:

```kotlin
// Add before @Graph annotation
enum class GraphType {
    STACK,
    TAB,
    PANE
}
```

### Step 2: Update @Graph Annotation (15 minutes)

Add the `type` parameter with default value:

```kotlin
annotation class Graph(
    val name: String,
    val startDestination: String = "",
    val type: GraphType = GraphType.STACK  // New parameter
)
```

### Step 3: Add KDoc Documentation (30 minutes)

Document both `GraphType` and the updated `@Graph` with:
- Enum value descriptions
- Code samples for each graph type
- Cross-references to NavNode types

### Step 4: Verify Compilation (15 minutes)

Ensure the annotation module compiles on all platforms:

```bash
./gradlew :quo-vadis-annotations:build
```

### Step 5: Update Unit Tests (30 minutes)

If annotation tests exist, add tests for the new parameter:

```kotlin
@Test
fun `Graph annotation accepts type parameter`() {
    @Graph("test", type = GraphType.TAB)
    sealed class TestGraph : Destination {
        @Route("home") data object Home : TestGraph()
    }
    // Verify annotation is correctly applied
}
```

---

## Backward Compatibility

The `type` parameter defaults to `GraphType.STACK`, ensuring:

1. **Existing code continues to work** without modification
2. **Generated code remains identical** for existing `@Graph` usages
3. **Gradual adoption** of new graph types

```kotlin
// These are equivalent:
@Graph("main")
sealed class MainDest : Destination { /* ... */ }

@Graph("main", type = GraphType.STACK)
sealed class MainDest : Destination { /* ... */ }
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-annotations/.../Annotations.kt` | Modify | Add `GraphType` enum and update `@Graph` |

---

## Dependencies

This task has **no dependencies** and can be started immediately. It is a prerequisite for KSP-003.

---

## Acceptance Criteria

- [ ] `GraphType` enum defined with `STACK`, `TAB`, `PANE` values
- [ ] `@Graph` annotation includes `type: GraphType = GraphType.STACK` parameter
- [ ] KDoc documentation for all new APIs
- [ ] Code samples demonstrating each `GraphType`
- [ ] Module compiles on all platforms (Android, iOS, Desktop, Web)
- [ ] Existing `@Graph` usages continue to work without changes
- [ ] No breaking changes to the annotation API

---

## Testing Notes

Basic validation during development:

```kotlin
@Test
fun `GraphType enum has expected values`() {
    val types = GraphType.values()
    assertEquals(3, types.size)
    assertTrue(GraphType.STACK in types)
    assertTrue(GraphType.TAB in types)
    assertTrue(GraphType.PANE in types)
}

@Test
fun `Graph annotation with default type compiles`() {
    @Graph("test")
    sealed class TestGraph : Destination {
        @Route("home") data object Home : TestGraph()
    }
    // Should compile without errors
}

@Test
fun `Graph annotation with explicit type compiles`() {
    @Graph("test", type = GraphType.TAB)
    sealed class TestGraph : Destination {
        @Route("home") data object Home : TestGraph()
    }
    // Should compile without errors
}
```

---

## Future Considerations

### Additional Graph Types

Future versions may introduce additional types:

```kotlin
enum class GraphType {
    STACK,
    TAB,
    PANE,
    DIALOG,    // Future: Modal dialog navigation
    DRAWER,    // Future: Drawer-based navigation
    BOTTOM_SHEET  // Future: Bottom sheet navigation
}
```

### Type-Specific Parameters

Consider extending `@Graph` with type-specific configuration:

```kotlin
// Potential future enhancement
@Graph(
    name = "tabs",
    type = GraphType.TAB,
    tabConfig = TabConfig(
        persistBackStack = true,
        resetOnReselect = false
    )
)
```

---

## References

- [INDEX](../INDEX.md) - Phase 3 Overview
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [Current Annotations](../../../quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Annotations.kt)
