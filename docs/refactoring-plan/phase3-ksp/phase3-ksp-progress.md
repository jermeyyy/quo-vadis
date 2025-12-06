# Phase 3: KSP Processor Rewrite - Progress

> **Last Updated**: 2025-12-06  
> **Phase Status**: 🟡 In Progress  
> **Progress**: 1/6 tasks (17%)

## Overview

This phase implements a complete rewrite of the KSP code generation for the new annotation system, producing NavNode builders, screen registries, and deep link handlers.

---

## Task Progress

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [KSP-001](./KSP-001-graph-type-enum.md) | Create Annotation Extractors | 🟢 Completed | 2025-12-06 | 11 files created |
| [KSP-002](./KSP-002-class-references.md) | Create NavNode Builder Generator | ⚪ Not Started | - | Depends on KSP-001 |
| [KSP-003](./KSP-003-graph-extractor.md) | Create Screen Registry Generator | ⚪ Not Started | - | Depends on KSP-001 |
| [KSP-004](./KSP-004-deep-link-handler.md) | Create Deep Link Handler Generator | ⚪ Not Started | - | Depends on KSP-002, KSP-003 |
| [KSP-005](./KSP-005-navigator-extensions.md) | Create Navigator Extensions Generator | ⚪ Not Started | - | Depends on KSP-002 |
| [KSP-006](./KSP-006-validation.md) | Validation and Error Reporting | ⚪ Not Started | - | Depends on KSP-001 |

---

## Completed Tasks

### KSP-001: Create Annotation Extractors (2025-12-06)

Created the extraction layer for parsing annotations into strongly-typed intermediate models.

**Model Classes Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/`):
- `ParamInfo.kt` - Constructor parameter metadata
- `DestinationInfo.kt` - @Destination annotation metadata
- `StackInfo.kt` - @Stack annotation metadata
- `TabInfo.kt` - @Tab/@TabItem annotation metadata
- `PaneInfo.kt` - @Pane/@PaneItem metadata + enums (PaneRole, AdaptStrategy, PaneBackBehavior)
- `ScreenInfo.kt` - @Screen annotation metadata

**Extractor Classes Created** (`quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/`):
- `DestinationExtractor.kt` - Extracts @Destination, route params, constructor params
- `StackExtractor.kt` - Extracts @Stack with start destination resolution
- `TabExtractor.kt` - Extracts @Tab/@TabItem with rootGraph resolution
- `PaneExtractor.kt` - Extracts @Pane/@PaneItem with role/strategy parsing
- `ScreenExtractor.kt` - Extracts @Screen with scope parameter detection

**Bug Fix**: Fixed pre-existing compilation error in `QuoVadisSymbolProcessor.kt` - changed `TabGraph` import to `Tab`.

**Verified**: `:quo-vadis-ksp:compileKotlin` ✓

---

## In Progress Tasks

_None currently in progress._

---

## Blocked Tasks

_None - KSP-001 completed, unlocks remaining tasks._

---

## Ready to Start

- **KSP-002**: Create NavNode Builder Generator
- **KSP-003**: Create Screen Registry Generator
- **KSP-006**: Validation and Error Reporting

---

## Dependencies

```
Phase 4 (Annotations) ─► KSP-001 ─┬─► KSP-002 ─┬─► KSP-004
                                  │            └─► KSP-005
                                  │
                                  ├─► KSP-003 ───► KSP-004
                                  │
                                  └─► KSP-006
```

---

## Generated Artifacts

| Input | Output | Purpose |
|-------|--------|---------|
| `@Stack` class | `build{Name}NavNode()` | Initial StackNode tree |
| `@Tab` class | `build{Name}NavNode()` | Initial TabNode tree |
| `@Pane` class | `build{Name}NavNode()` | Initial PaneNode tree |
| All `@Screen` | `GeneratedScreenRegistry` | Destination → Composable mapping |
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing |

---

## Notes

- Estimated 14-19 days total
- Can be started in parallel with Phase 2 (after Phase 4)
- Focus on compile-time safety and helpful error messages

---

## Related Documents

- [Phase 3 Summary](./phase3-ksp-summary.md)
