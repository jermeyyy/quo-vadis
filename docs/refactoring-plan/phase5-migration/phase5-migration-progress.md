# Phase 5: Migration Examples - Progress

> **Last Updated**: 2025-12-06  
> **Phase Status**: ⚪ Not Started  
> **Progress**: 0/11 tasks (0%)

## Overview

This phase creates the `quo-vadis-recipes` module with LLM-optimized navigation examples, marks all legacy APIs with `@Deprecated` annotations, and rewrites the demo app to showcase all patterns.

> **Note**: No backward compatibility adapters are needed. The library is in development stage and breaking changes are acceptable.

---

## Task Progress

### Preparatory Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [PREP-001](./PREP-001-recipes-module.md) | Create quo-vadis-recipes Module | ⚪ Not Started | - | Can start immediately |
| [PREP-002](./PREP-002-deprecated-annotations.md) | Add @Deprecated Annotations | ⚪ Not Started | - | Depends on Phase 4 |
| [PREP-003](./PREP-003-permalink-reference.md) | GitHub Permalink Reference Doc | ⚪ Not Started | - | Can start immediately |

### Recipe Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [MIG-001](./MIG-001-simple-stack-example.md) | Simple Stack Navigation Recipe | ⚪ Not Started | - | Depends on PREP-001, Phase 1-4 |
| [MIG-002](./MIG-002-master-detail-example.md) | Master-Detail Pattern Recipe | ⚪ Not Started | - | Depends on MIG-001 |
| [MIG-003](./MIG-003-tabbed-navigation-example.md) | Tabbed Navigation Recipe | ⚪ Not Started | - | Depends on MIG-001 |
| [MIG-004](./MIG-004-process-flow-example.md) | Process/Wizard Flow Recipe | ⚪ Not Started | - | Depends on MIG-001 |
| [MIG-005](./MIG-005-nested-tabs-detail-example.md) | Nested Tabs + Detail Recipe | ⚪ Not Started | - | Depends on MIG-003 |
| [MIG-006](./MIG-006-deep-linking-recipe.md) | Deep Linking Recipe | ⚪ Not Started | - | Depends on MIG-001, MIG-002 |

### Migration Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [MIG-007](./MIG-007-demo-app-rewrite.md) | Demo App Rewrite | ⚪ Not Started | - | Depends on MIG-001..006 |
| [MIG-008](./MIG-008-api-change-summary.md) | API Change Summary Document | ⚪ Not Started | - | Depends on PREP-002 |

---

## Completed Tasks

_None yet._

---

## In Progress Tasks

_None currently in progress._

---

## Blocked Tasks

| Task | Blocked By | Status |
|------|------------|--------|
| MIG-001 through MIG-006 | Phase 1-4 completion | Waiting for core implementation |
| MIG-007 | MIG-001 through MIG-006 | Waiting for recipes |
| PREP-002 | Phase 4 annotations | Waiting for new annotations |

---

## Ready to Start

| Task | Notes |
|------|-------|
| PREP-001 | Can start immediately - module setup |
| PREP-003 | Can start immediately - documentation |

---

## Dependencies

```
                            ┌─────────────────────────────────┐
                            │       PREP-001 (recipes)        │
                            │      (can start anytime)        │
                            └────────────────┬────────────────┘
                                             │
Phase 1-4 ───────────────────────────────────┼───┐
            │                                │   │
            ▼                                │   │
       PREP-002 ──────────────────┐          │   │
    (@Deprecated)                 │          │   │
            │                     │          ▼   │
            │                     │      MIG-001 (stack)
            ▼                     │          │
       MIG-008 (summary)          │    ┌─────┼─────┬───────┐
                                  │    │     │     │       │
                                  │    ▼     ▼     ▼       ▼
                                  │ MIG-002 MIG-003 MIG-004 │
                                  │ (detail) (tabs) (wizard)│
                                  │    │     │             │
                                  │    │     ▼             │
                                  │    │  MIG-005          │
                                  │    │  (nested)         │
                                  │    │     │             │
                                  │    ├─────┴─────────────┤
                                  │    │                   │
                                  │    ▼                   │
                                  │ MIG-006 (deeplink)     │
                                  │    │                   │
                                  │    └───────┬───────────┘
                                  │            │
                                  │            ▼
                                  └──────► MIG-007 (demo app)

PREP-003 (permalinks) ─────────── (can start anytime)
```

---

## Recipe Module Coverage

| Package | Recipe | Key Patterns |
|---------|--------|--------------|
| `stack/` | MIG-001 | `@Stack`, `@Destination`, `@Screen`, basic navigation |
| `masterdetail/` | MIG-002 | Route templates, typed arguments, shared elements |
| `tabs/` | MIG-003, MIG-005 | `@Tab`, `@TabItem`, `tabWrapper`, nested stacks |
| `wizard/` | MIG-004 | Sequential flow, branching, stack clearing |
| `deeplink/` | MIG-006 | URI handling, path parameters, reconstruction |
| `pane/` | (Future) | Adaptive layouts (if time permits) |

---

## Notes

- **Estimated**: 14-17 days total
- **New module**: `quo-vadis-recipes` for LLM-optimized examples
- **Deprecation**: All legacy APIs marked `@Deprecated` with `replaceWith`
- **References**: GitHub permalinks to main branch for "migrating from" code
- **No backwards compatibility**: Library in development stage

---

## Related Documents

- [Phase 5 Summary](./phase5-migration-summary.md)
- [PREP-001: Recipes Module](./PREP-001-recipes-module.md)
- [PREP-002: Deprecated Annotations](./PREP-002-deprecated-annotations.md)
- [PREP-003: Permalink Reference](./PREP-003-permalink-reference.md)
