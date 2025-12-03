# KSP-002: Add NavNode Class References to QuoVadisClassNames

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-002 |
| **Task Name** | Add NavNode Class References to QuoVadisClassNames |
| **Phase** | Phase 3: KSP Processor Updates |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | Phase 1 (CORE-001, CORE-002) |
| **Blocked By** | CORE-001 |
| **Blocks** | KSP-004 |

---

## Overview

This task extends the `QuoVadisClassNames` object in the KSP processor to include type-safe references to the new `NavNode` hierarchy and `TreeMutator` classes. These references are essential for generating code that creates and manipulates `NavNode` structures.

### Why Type-Safe References?

The `QuoVadisClassNames` object provides compile-time safe references to core library classes using KotlinPoet's `ClassName`. This approach:

1. **Prevents typos**: Class names are validated at compile time
2. **Enables refactoring**: Renaming classes in `quo-vadis-core` automatically propagates to KSP
3. **Improves maintainability**: Single source of truth for all class references

---

## File Location

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisClassNames.kt
```

---

## Implementation

### Updated QuoVadisClassNames

```kotlin
package com.jermey.quo.vadis.ksp

import com.jermey.quo.vadis.core.navigation.compose.TransitionScope
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationGraphBuilder
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.RouteRegistry
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.TreeMutator
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

/**
 * Type-safe references to Quo Vadis Core API classes.
 *
 * This object provides compile-time safe references to core navigation classes,
 * ensuring that refactoring in quo-vadis-core is automatically reflected in the KSP processor.
 *
 * ## Usage
 *
 * ```kotlin
 * // In generator code
 * val returnType = QuoVadisClassNames.NAV_NODE
 * val stackType = QuoVadisClassNames.STACK_NODE
 *
 * FunSpec.builder("buildGraph")
 *     .returns(returnType)
 *     .addStatement("return %T(...)", stackType)
 *     .build()
 * ```
 */
internal object QuoVadisClassNames {

    // =========================================================================
    // Core Navigation Classes
    // =========================================================================

    val NAVIGATOR: ClassName = Navigator::class.toClassName()
    val NAVIGATION_GRAPH: ClassName = NavigationGraph::class.toClassName()
    val NAVIGATION_GRAPH_BUILDER: ClassName = NavigationGraphBuilder::class.toClassName()
    val NAVIGATION_TRANSITION: ClassName = NavigationTransition::class.toClassName()
    val ROUTE_REGISTRY: ClassName = RouteRegistry::class.toClassName()
    val DESTINATION: ClassName = Destination::class.toClassName()

    // =========================================================================
    // NavNode Hierarchy (NEW)
    // =========================================================================

    /**
     * Base sealed interface for navigation tree nodes.
     * @see NavNode
     */
    val NAV_NODE: ClassName = NavNode::class.toClassName()

    /**
     * Leaf node representing a single screen/destination.
     * @see ScreenNode
     */
    val SCREEN_NODE: ClassName = ScreenNode::class.toClassName()

    /**
     * Container node representing a linear navigation stack.
     * @see StackNode
     */
    val STACK_NODE: ClassName = StackNode::class.toClassName()

    /**
     * Container node representing tabbed navigation with parallel stacks.
     * @see TabNode
     */
    val TAB_NODE: ClassName = TabNode::class.toClassName()

    /**
     * Container node for adaptive pane layouts.
     * @see PaneNode
     */
    val PANE_NODE: ClassName = PaneNode::class.toClassName()

    // =========================================================================
    // Tree Operations (NEW)
    // =========================================================================

    /**
     * Pure functional operations for manipulating the NavNode tree.
     * @see TreeMutator
     */
    val TREE_MUTATOR: ClassName = TreeMutator::class.toClassName()

    // =========================================================================
    // Compose Classes
    // =========================================================================

    val TRANSITION_SCOPE: ClassName = TransitionScope::class.toClassName()

    // =========================================================================
    // Utility Functions
    // =========================================================================

    /**
     * Convert KClass to KotlinPoet ClassName.
     *
     * Handles nested classes by splitting the qualified name appropriately.
     */
    private fun KClass<*>.toClassName(): ClassName {
        val qualifiedName = this.qualifiedName
            ?: throw IllegalArgumentException("Cannot get qualified name for $this")
        val packageName = qualifiedName.substringBeforeLast('.', "")
        val simpleNames = qualifiedName.substringAfterLast('.').split('.')
        return ClassName(packageName, simpleNames)
    }
}
```

---

## Implementation Steps

### Step 1: Add Import Statements (10 minutes)

Add imports for the new NavNode classes at the top of the file:

```kotlin
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.TreeMutator
```

> **Note**: These imports will initially fail until CORE-001 is completed. Use `ClassName("com.jermey.quo.vadis.core.navigation.core", "NavNode")` as a temporary fallback if needed.

### Step 2: Add NavNode Class References (20 minutes)

Add the new class references grouped under a clear section:

```kotlin
// =========================================================================
// NavNode Hierarchy (NEW)
// =========================================================================

val NAV_NODE: ClassName = NavNode::class.toClassName()
val SCREEN_NODE: ClassName = ScreenNode::class.toClassName()
val STACK_NODE: ClassName = StackNode::class.toClassName()
val TAB_NODE: ClassName = TabNode::class.toClassName()
val PANE_NODE: ClassName = PaneNode::class.toClassName()
```

### Step 3: Add TreeMutator Reference (10 minutes)

Add the TreeMutator reference:

```kotlin
// =========================================================================
// Tree Operations (NEW)
// =========================================================================

val TREE_MUTATOR: ClassName = TreeMutator::class.toClassName()
```

### Step 4: Add KDoc Documentation (20 minutes)

Document each new reference with:
- Brief description
- `@see` reference to the actual class

### Step 5: Verify Compilation (30 minutes)

After CORE-001 is complete, verify the KSP module compiles:

```bash
./gradlew :quo-vadis-ksp:build
```

---

## Temporary Implementation (Before Phase 1)

If this task needs to proceed before Phase 1 completion, use string-based class names temporarily:

```kotlin
// Temporary fallback until CORE-001 is complete
val NAV_NODE: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "NavNode"
)

val SCREEN_NODE: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "ScreenNode"
)

val STACK_NODE: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "StackNode"
)

val TAB_NODE: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "TabNode"
)

val PANE_NODE: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "PaneNode"
)

val TREE_MUTATOR: ClassName = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "TreeMutator"
)
```

Replace with type-safe imports once Phase 1 is available.

---

## Usage Examples

### In NavNodeGenerator (KSP-004)

```kotlin
// Generating a StackNode creation
val stackNodeSpec = FunSpec.builder("build${graphName}NavNode")
    .returns(QuoVadisClassNames.NAV_NODE)
    .addStatement(
        "return %T(key = %S, parentKey = null, children = listOf(...))",
        QuoVadisClassNames.STACK_NODE,
        "root"
    )
    .build()
```

### In Generated Code

```kotlin
// Generated by KSP
fun buildMainDestinationNavNode(): NavNode {
    return StackNode(
        key = "main-root",
        parentKey = null,
        children = listOf(
            ScreenNode(
                key = "home-screen",
                parentKey = "main-root",
                destination = MainDestination.Home
            )
        )
    )
}
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/.../QuoVadisClassNames.kt` | Modify | Add NavNode hierarchy references |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Required for type-safe imports |
| CORE-002 (TreeMutator) | Hard | Required for TreeMutator reference |

---

## Acceptance Criteria

- [ ] `NAV_NODE` class reference added to `QuoVadisClassNames`
- [ ] `SCREEN_NODE` class reference added
- [ ] `STACK_NODE` class reference added
- [ ] `TAB_NODE` class reference added
- [ ] `PANE_NODE` class reference added
- [ ] `TREE_MUTATOR` class reference added
- [ ] All references use type-safe `KClass::toClassName()` pattern
- [ ] KDoc documentation on all new references
- [ ] KSP module compiles successfully
- [ ] No breaking changes to existing generators

---

## Testing Notes

Unit tests for the class names:

```kotlin
@Test
fun `NavNode class references resolve correctly`() {
    assertEquals(
        "com.jermey.quo.vadis.core.navigation.core",
        QuoVadisClassNames.NAV_NODE.packageName
    )
    assertEquals("NavNode", QuoVadisClassNames.NAV_NODE.simpleName)
}

@Test
fun `StackNode class reference is correct`() {
    assertEquals("StackNode", QuoVadisClassNames.STACK_NODE.simpleName)
}

@Test
fun `TabNode class reference is correct`() {
    assertEquals("TabNode", QuoVadisClassNames.TAB_NODE.simpleName)
}

@Test
fun `PaneNode class reference is correct`() {
    assertEquals("PaneNode", QuoVadisClassNames.PANE_NODE.simpleName)
}

@Test
fun `TreeMutator class reference is correct`() {
    assertEquals("TreeMutator", QuoVadisClassNames.TREE_MUTATOR.simpleName)
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 3 Overview
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-002](../phase1-core/CORE-002-tree-mutator.md) - TreeMutator implementation
- [Current QuoVadisClassNames](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisClassNames.kt)
