# HIER-029: Deprecate Runtime Wrappers

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-029 |
| **Task Name** | Deprecate Runtime Wrapper Registration |
| **Phase** | Phase 5: Migration |
| **Complexity** | Small |
| **Estimated Time** | 0.5 day |
| **Dependencies** | HIER-028 |
| **Blocked By** | HIER-028 |
| **Blocks** | HIER-031 |

---

## Overview

Add `@Deprecated` annotations to the runtime wrapper registration APIs, pointing users to the new annotation-based approach.

---

## Files to Modify

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/
├── TabWrapper.kt      # Add @Deprecated
└── PaneWrapper.kt     # Add @Deprecated
```

---

## Implementation

```kotlin
// TabWrapper.kt
/**
 * Runtime tab wrapper registration.
 *
 * @deprecated Use `@TabWrapper` annotation instead for compile-time safety.
 * ```kotlin
 * // Old:
 * TabWrapper(
 *     node = tabNode,
 *     content = { MyTabBar(it) }
 * )
 *
 * // New:
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
 *     // ...
 * }
 * ```
 */
@Deprecated(
    message = "Use @TabWrapper annotation instead",
    replaceWith = ReplaceWith(
        "@TabWrapper(YourTabNode::class)",
        "com.jermey.quo.vadis.annotations.TabWrapper"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun TabWrapper(
    node: TabNode,
    content: @Composable (TabWrapperScope) -> Unit
) {
    // Existing implementation...
}

// PaneWrapper.kt
@Deprecated(
    message = "Use @PaneWrapper annotation instead",
    replaceWith = ReplaceWith(
        "@PaneWrapper(YourPaneNode::class)",
        "com.jermey.quo.vadis.annotations.PaneWrapper"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun PaneWrapper(
    node: PaneNode,
    content: @Composable (PaneWrapperScope) -> Unit
) {
    // Existing implementation...
}
```

---

## Acceptance Criteria

- [ ] `@Deprecated` annotation on `TabWrapper` composable
- [ ] `@Deprecated` annotation on `PaneWrapper` composable
- [ ] `ReplaceWith` pointing to new annotations
- [ ] `DeprecationLevel.WARNING` (not ERROR yet)
- [ ] KDoc with migration example
