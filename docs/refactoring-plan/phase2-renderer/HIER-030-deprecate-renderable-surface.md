# HIER-030: Deprecate RenderableSurface

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-030 |
| **Task Name** | Deprecate RenderableSurface System |
| **Phase** | Phase 5: Migration |
| **Complexity** | Small |
| **Estimated Time** | 0.5 day |
| **Dependencies** | HIER-028 |
| **Blocked By** | HIER-028 |
| **Blocks** | HIER-031 |

---

## Overview

Add `@Deprecated` annotations to the `RenderableSurface` flattening system, pointing users to `HierarchicalQuoVadisHost`.

---

## Files to Modify

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/
├── RenderableSurface.kt           # Add @Deprecated
├── QuoVadisHost.kt                # Add @Deprecated
└── flattening/
    ├── FlattenedNavTree.kt        # Add @Deprecated
    └── SurfaceRenderer.kt         # Add @Deprecated
```

---

## Implementation

```kotlin
// RenderableSurface.kt
/**
 * Surface for rendering navigation destinations.
 *
 * @deprecated The flattened rendering model has been replaced by hierarchical rendering.
 * Use `HierarchicalQuoVadisHost` instead, which properly preserves wrapper/content relationships.
 *
 * Migration guide: https://quo-vadis.dev/migration/rendering
 */
@Deprecated(
    message = "Use HierarchicalQuoVadisHost instead of QuoVadisHost",
    replaceWith = ReplaceWith(
        "HierarchicalQuoVadisHost(navigator, modifier)",
        "com.jermey.quo.vadis.core.navigation.compose.HierarchicalQuoVadisHost"
    ),
    level = DeprecationLevel.WARNING
)
data class RenderableSurface(
    // ...
)

// QuoVadisHost.kt
/**
 * Legacy navigation host using flattened rendering.
 *
 * @deprecated Use `HierarchicalQuoVadisHost` for proper wrapper/content relationships.
 */
@Deprecated(
    message = "Use HierarchicalQuoVadisHost for better animation support",
    replaceWith = ReplaceWith(
        "HierarchicalQuoVadisHost(navigator, modifier)",
        "com.jermey.quo.vadis.core.navigation.compose.HierarchicalQuoVadisHost"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    // Existing implementation...
}
```

---

## Deprecation Timeline

| Version | Level | Notes |
|---------|-------|-------|
| 1.0.0 | WARNING | Initial deprecation |
| 1.1.0 | ERROR | Compile error, require migration |
| 2.0.0 | HIDDEN | Remove from API, keep for binary compat |

---

## Acceptance Criteria

- [ ] `@Deprecated` on `RenderableSurface`
- [ ] `@Deprecated` on legacy `QuoVadisHost`
- [ ] `@Deprecated` on `FlattenedNavTree`
- [ ] All `@Deprecated` have `ReplaceWith`
- [ ] `DeprecationLevel.WARNING`
- [ ] Migration URL in KDoc
