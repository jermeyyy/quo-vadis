# HIER-033: Cleanup Legacy Code

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-033 |
| **Task Name** | Cleanup Legacy Rendering Code |
| **Phase** | Phase 5: Migration |
| **Complexity** | Small |
| **Estimated Time** | 1 day |
| **Dependencies** | HIER-032 |
| **Blocked By** | HIER-032 |
| **Blocks** | None |

---

## Overview

Final cleanup task to mark legacy code for removal and ensure the codebase is ready for the next major version where deprecated code will be removed.

---

## Tasks

### 1. Mark for Removal in v2.0

Update deprecation levels for next major version:

```kotlin
// Will be removed in v2.0
@Deprecated(
    message = "...",
    level = DeprecationLevel.ERROR  // Change from WARNING
)
```

### 2. Add Removal TODOs

```kotlin
// TODO(v2.0): Remove this file entirely
// Replaced by HierarchicalQuoVadisHost
@Deprecated(...)
@Composable
fun QuoVadisHost(...) { ... }
```

### 3. Update CHANGELOG

```markdown
## [Unreleased]

### Deprecated (Will be removed in v2.0)
- `QuoVadisHost` - Use `HierarchicalQuoVadisHost` instead
- `RenderableSurface` - No longer needed
- Runtime `TabWrapper` and `PaneWrapper` - Use annotations
- `FlattenedNavTree` - Superseded by `NavTreeRenderer`

### Migration Required Before v2.0
See [Migration Guide](docs/MIGRATION_HIERARCHICAL_RENDERING.md)
```

### 4. Create Removal Tracking Issue

GitHub issue tracking what to remove in v2.0:
- [ ] `RenderableSurface.kt`
- [ ] `FlattenedNavTree.kt`
- [ ] `SurfaceRenderer.kt`
- [ ] Legacy `QuoVadisHost`
- [ ] Runtime `TabWrapper`/`PaneWrapper` composables
- [ ] `RenderingMode.Flattened` option

### 5. Add Proguard/R8 Keep Rules

For binary compatibility during transition:

```proguard
# Keep deprecated APIs for binary compatibility
-keep class com.jermey.quo.vadis.core.navigation.compose.RenderableSurface { *; }
-keep class com.jermey.quo.vadis.core.navigation.compose.QuoVadisHostKt { *; }
```

---

## Files to Modify

```
quo-vadis-core/
├── CHANGELOG.md                     # Document deprecations
├── consumer-rules.pro               # Add keep rules
└── src/commonMain/kotlin/...        # Update @Deprecated levels
```

---

## Acceptance Criteria

- [ ] All deprecated APIs have removal timeline comments
- [ ] CHANGELOG updated with deprecation notice
- [ ] GitHub issue created for v2.0 removal tracking
- [ ] Proguard rules added for binary compatibility
- [ ] No compiler warnings in library code (deprecations are intentional)
- [ ] CI passes with all deprecations

---

## Post-Completion

After this task, the hierarchical rendering migration is complete. The remaining work is:

1. **v1.x releases**: Maintain both systems, encourage migration
2. **v2.0 release**: Remove deprecated code per tracking issue
3. **Ongoing**: Enhance hierarchical system based on feedback
