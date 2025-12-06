# KSP-007: Remove Legacy TabGraphExtractor

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-007 |
| **Task Name** | Remove Legacy TabGraphExtractor |
| **Phase** | Phase 3: KSP Processor Rewrite |
| **Complexity** | Low |
| **Estimated Time** | 1-2 hours |
| **Dependencies** | None |
| **Blocked By** | None |
| **Blocks** | Phase 5 Migration |

---

## Overview

This task removes the legacy `TabGraphExtractor` and related legacy code that causes build failures when processing the demo app. The legacy code uses unsafe `.first { }` calls that throw `NoSuchElementException` when annotation properties are missing.

### Problem Statement

The current build fails with:
```
e: [ksp] java.util.NoSuchElementException: Sequence contains no element matching the predicate.
    at com.jermey.quo.vadis.ksp.TabGraphExtractor.extract(TabGraphExtractor.kt:180)
    at com.jermey.quo.vadis.ksp.QuoVadisSymbolProcessor.processTabGraphClass(QuoVadisSymbolProcessor.kt:181)
```

This occurs because:
1. Legacy `TabGraphExtractor` uses unsafe `.first { }` calls without null safety
2. The demo app has `@Tab` annotations that don't match the legacy annotation schema
3. New `TabExtractor` (in `extractors/`) handles the new annotation schema correctly

### Rationale

1. **Unblocks development**: Full app build currently fails
2. **Removes dead code**: Legacy extractors/generators are superseded by KSP-001 through KSP-006
3. **Prevents confusion**: Two parallel code paths for the same annotations
4. **Simplifies maintenance**: Single source of truth for annotation processing

---

## Legacy Files to Remove

### Primary Targets

| File | Purpose | Replacement |
|------|---------|-------------|
| `TabGraphExtractor.kt` | Legacy @Tab/@TabItem extraction | `extractors/TabExtractor.kt` |
| `TabGraphInfo.kt` | Legacy tab data model | `models/TabInfo.kt` |
| `TabGraphGenerator.kt` | Legacy tab code generation | `generators/NavNodeBuilderGenerator.kt` |
| `GraphInfoExtractor.kt` | Legacy @Graph extraction | `extractors/StackExtractor.kt` |
| `GraphInfo.kt` | Legacy graph data model | `models/StackInfo.kt` |
| `GraphGenerator.kt` | Legacy graph code generation | `generators/NavNodeBuilderGenerator.kt` |
| `GraphBuilderGenerator.kt` | Legacy builder generation | `generators/NavNodeBuilderGenerator.kt` |
| `RouteConstantsGenerator.kt` | Legacy route constants | `generators/DeepLinkHandlerGenerator.kt` |
| `RouteInitializationGenerator.kt` | Legacy route init | `generators/DeepLinkHandlerGenerator.kt` |
| `DestinationExtensionsGenerator.kt` | Legacy destination extensions | `generators/NavigatorExtGenerator.kt` |

### Processor Methods to Remove

In `QuoVadisSymbolProcessor.kt`:
- `processGraphClass()` - Legacy @Graph processing
- `processTabGraphClass()` - Legacy @TabGraph/@Tab processing
- `processContentFunction()` - Legacy @Content processing
- Related fields: `contentMappings`, `allGraphInfos`

### Processor Passes to Remove

In `process()` method:
- First pass: `@Content` functions collection
- Second pass: `@Graph` classes processing
- Third pass: `@Tab` classes processing (legacy)

---

## Implementation Steps

### Step 1: Remove Legacy Files (30 min)

Delete the following files from `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`:
- `TabGraphExtractor.kt`
- `TabGraphInfo.kt`
- `TabGraphGenerator.kt`
- `GraphInfoExtractor.kt`
- `GraphInfo.kt`
- `GraphGenerator.kt`
- `GraphBuilderGenerator.kt`
- `RouteConstantsGenerator.kt`
- `RouteInitializationGenerator.kt`
- `DestinationExtensionsGenerator.kt`

### Step 2: Update QuoVadisSymbolProcessor (30 min)

1. Remove legacy imports
2. Remove legacy fields (`contentMappings`, `allGraphInfos`)
3. Remove legacy methods (`processContentFunction`, `processGraphClass`, `processTabGraphClass`)
4. Remove legacy passes from `process()` method
5. Remove `finish()` method (only used for legacy route initialization)
6. Keep only the new NavNode-based processing pipeline

### Step 3: Update QuoVadisClassNames (15 min)

Remove any class name references only used by legacy generators.

### Step 4: Clean Up Annotations Module (15 min)

Review `quo-vadis-annotations` for legacy annotations that can be deprecated:
- `@Graph` - Superseded by `@Stack`
- `@Content` - Superseded by `@Screen`
- Any other legacy annotations

### Step 5: Verify Build (15 min)

1. Run `:quo-vadis-ksp:build -x detekt`
2. Run `:composeApp:assembleDebug` (should now pass or show only new validation errors)
3. Fix any remaining issues

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `TabGraphExtractor.kt` | Delete | Legacy extractor |
| `TabGraphInfo.kt` | Delete | Legacy model |
| `TabGraphGenerator.kt` | Delete | Legacy generator |
| `GraphInfoExtractor.kt` | Delete | Legacy extractor |
| `GraphInfo.kt` | Delete | Legacy model |
| `GraphGenerator.kt` | Delete | Legacy generator |
| `GraphBuilderGenerator.kt` | Delete | Legacy generator |
| `RouteConstantsGenerator.kt` | Delete | Legacy generator |
| `RouteInitializationGenerator.kt` | Delete | Legacy generator |
| `DestinationExtensionsGenerator.kt` | Delete | Legacy generator |
| `QuoVadisSymbolProcessor.kt` | Modify | Remove legacy code |
| `QuoVadisClassNames.kt` | Modify | Remove unused references |

---

## Acceptance Criteria

- [ ] All legacy extractor files deleted
- [ ] All legacy generator files deleted
- [ ] All legacy model files deleted
- [ ] `QuoVadisSymbolProcessor` cleaned up (legacy methods/fields removed)
- [ ] `:quo-vadis-ksp:build` passes
- [ ] `:composeApp:kspCommonMainKotlinMetadata` no longer throws `NoSuchElementException`
- [ ] Only new validation errors (from `ValidationEngine`) appear, if any
- [ ] No regression in new KSP-001 through KSP-006 functionality

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Break existing demo app | Low | Medium | Demo app should use new annotations; migration in Phase 5 |
| Remove code still in use | Low | High | Verify all usages before deletion |
| Compilation errors after removal | Medium | Low | Incremental removal with build verification |

---

## Notes

- This is a cleanup task to remove technical debt
- The demo app will need migration to new annotations (Phase 5)
- After this task, only the new NavNode-based pipeline remains
- Legacy annotations (`@Graph`, `@Content`) can be deprecated but not removed until Phase 5

---

## References

- [KSP-001](./KSP-001-graph-type-enum.md) - New annotation extractors
- [KSP-002](./KSP-002-class-references.md) - NavNode builder generator
- [KSP-006](./KSP-006-validation.md) - Validation engine
- [Phase 5: Migration](../phase5-migration/) - Demo app migration plan
