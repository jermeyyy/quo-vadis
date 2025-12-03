# Quo Vadis Architecture Refactoring - Execution Plan

## Executive Summary

This document outlines the comprehensive execution plan for refactoring the **Quo Vadis** navigation library from a linear backstack model to a **tree-based NavNode architecture** with a **Single Rendering Component (QuoVadisHost)**.

### Vision

Transform Quo Vadis from a "Direct BackStack Manager" into a holistic **"Navigation Rendering Engine"** capable of:

- **Unified rendering** of linear stacks, tabbed navigators, and adaptive panes
- **Seamless Shared Element Transitions** across all navigation types
- **Physics-based Predictive Back** gestures with speculative pop
- **Deep linking** with automatic path reconstruction
- **Adaptive layouts** that morph between compact and expanded modes

### Current State vs Target State

| Aspect | Current State | Target State |
|--------|---------------|--------------|
| **State Model** | Linear `List<Destination>` | Recursive `NavNode` Tree |
| **Navigation Hosts** | Multiple nested (`NavHost`, `GraphNavHost`, `TabbedNavHost`) | Single `QuoVadisHost` |
| **Shared Elements** | Limited to single host boundaries | Seamless across all navigation |
| **Predictive Back** | Per-host handling | Unified speculative pop |
| **Layout Types** | Stack-only (tabs via separate hosts) | Stack, Tab, Pane (unified) |
| **Deep Linking** | Flat route matching | Tree path reconstruction |

---

## Task Summary

### Phase 1: Core State Refactoring (6 Tasks)
Replaces the linear backstack with a recursive tree structure.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [CORE-001](./phase1-core/CORE-001-navnode-hierarchy.md) | Define NavNode Sealed Hierarchy | Medium | 2-3 days |
| [CORE-002](./phase1-core/CORE-002-tree-mutator.md) | Implement TreeMutator Operations | High | 3-4 days |
| [CORE-003](./phase1-core/CORE-003-navigator-refactor.md) | Refactor Navigator to StateFlow<NavNode> | High | 4-5 days |
| [CORE-004](./phase1-core/CORE-004-state-serialization.md) | Implement NavNode Serialization | Medium | 2-3 days |
| [CORE-005](./phase1-core/CORE-005-backward-compat.md) | Create Backward Compatibility Layer | Medium | 2-3 days |
| [CORE-006](./phase1-core/CORE-006-unit-tests.md) | Comprehensive Unit Tests | Medium | 3-4 days |

**Phase 1 Total: ~16-22 days**

---

### Phase 2: Unified Renderer (8 Tasks)
Implements the single rendering component that projects the NavNode tree.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [RENDER-001](./phase2-renderer/RENDER-001-renderable-surface.md) | Define RenderableSurface Data Class | Low | 1 day |
| [RENDER-002](./phase2-renderer/RENDER-002-flatten-algorithm.md) | Implement flattenState Algorithm | High | 3-4 days |
| [RENDER-003](./phase2-renderer/RENDER-003-transition-state.md) | Create TransitionState Sealed Class | Medium | 2 days |
| [RENDER-004](./phase2-renderer/RENDER-004-quovadis-host.md) | Build QuoVadisHost Composable | High | 5-7 days |
| [RENDER-005](./phase2-renderer/RENDER-005-predictive-back.md) | Integrate Predictive Back with Speculative Pop | High | 4-5 days |
| [RENDER-006](./phase2-renderer/RENDER-006-animation-registry.md) | Create AnimationRegistry | Medium | 2-3 days |
| [RENDER-007](./phase2-renderer/RENDER-007-saveable-state.md) | SaveableStateHolder Integration | Medium | 2-3 days |
| [RENDER-008](./phase2-renderer/RENDER-008-legacy-migration.md) | Legacy Host Migration Wrappers | Medium | 2-3 days |

**Phase 2 Total: ~21-28 days**

---

### Phase 3: KSP Processor Updates (8 Tasks)
Updates code generation to produce NavNode tree structures.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [KSP-001](./phase3-ksp/KSP-001-graph-type-enum.md) | Enhance @Graph with GraphType Support | Low | 1 day |
| [KSP-002](./phase3-ksp/KSP-002-class-references.md) | Add NavNode Class References | Low | 0.5 days |
| [KSP-003](./phase3-ksp/KSP-003-graph-extractor.md) | Update GraphInfoExtractor | Low-Medium | 1-2 days |
| [KSP-004](./phase3-ksp/KSP-004-navnode-generator.md) | Create NavNode Builder Generator | High | 4-5 days |
| [KSP-005](./phase3-ksp/KSP-005-nested-graphs.md) | Support Nested Graph Definitions | High | 3-4 days |
| [KSP-006](./phase3-ksp/KSP-006-path-reconstructor.md) | Generate Path Reconstructors for Deep Links | High | 4-5 days |
| [KSP-007](./phase3-ksp/KSP-007-tabgraph-migration.md) | Update TabGraph Generator | Medium | 2-3 days |
| [KSP-008](./phase3-ksp/KSP-008-compat-generator.md) | Generate Backward Compatibility Extensions | Medium | 2-3 days |

**Phase 3 Total: ~17-24 days**

---

### Phase 4: Annotations Enhancement (4 Tasks)
Extends annotation capabilities for new features.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [ANN-001](./phase4-annotations/ANN-001-graph-type.md) | Define GraphType Enumeration | Low | 0.5 days |
| [ANN-002](./phase4-annotations/ANN-002-pane-graph.md) | Create @PaneGraph Annotation | Low | 1 day |
| [ANN-003](./phase4-annotations/ANN-003-route-transitions.md) | Add Transition Metadata to @Route | Low | 1 day |
| [ANN-004](./phase4-annotations/ANN-004-shared-element.md) | Create @SharedElement Annotation | Low | 0.5 days |

**Phase 4 Total: ~3 days**

---

### Phase 5: Migration Utilities (5 Tasks)
Provides tools for gradual adoption of the new architecture.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [MIG-001](./phase5-migration/MIG-001-backstack-converter.md) | BackStack-to-NavNode Converter | Medium | 2 days |
| [MIG-002](./phase5-migration/MIG-002-state-adapter.md) | Navigator State Adapter | Medium | 2 days |
| [MIG-003](./phase5-migration/MIG-003-host-adapter.md) | GraphNavHost to QuoVadisHost Adapter | Medium | 2 days |
| [MIG-004](./phase5-migration/MIG-004-transition-migrator.md) | Transition to AnimationRegistry Migrator | Medium | 2 days |
| [MIG-005](./phase5-migration/MIG-005-deprecation-warnings.md) | Type-Safe Deprecation Warnings | Low | 1 day |

**Phase 5 Total: ~9 days**

---

### Phase 6: Risk Mitigation (6 Tasks)
Implements safeguards against identified risks.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [RISK-001](./phase6-risks/RISK-001-memoized-flatten.md) | Memoized Tree Flattening | Medium | 2 days |
| [RISK-002](./phase6-risks/RISK-002-gesture-exclusion.md) | Gesture Exclusion Modifier | Medium | 2 days |
| [RISK-003](./phase6-risks/RISK-003-deeplink-validator.md) | Deep Link Tree Validator | Medium | 2 days |
| [RISK-004](./phase6-risks/RISK-004-api-checker.md) | API Compatibility Checker | Medium | 2 days |
| [RISK-005](./phase6-risks/RISK-005-state-restoration.md) | State Restoration Validation | Medium | 2 days |
| [RISK-006](./phase6-risks/RISK-006-benchmarks.md) | Performance Benchmarks | Medium | 3 days |

**Phase 6 Total: ~13 days**

---

### Phase 7: Documentation (5 Tasks)
Updates all documentation for the new architecture.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [DOC-001](./phase7-docs/DOC-001-annotation-kdoc.md) | Update KSP Annotation Documentation | Low | 1 day |
| [DOC-002](./phase7-docs/DOC-002-migration-guide.md) | Create Migration Guide | Medium | 3 days |
| [DOC-003](./phase7-docs/DOC-003-readme-update.md) | Update README with New Architecture | Low | 1 day |
| [DOC-004](./phase7-docs/DOC-004-deep-linking.md) | Create Deep Linking Documentation | Medium | 2 days |
| [DOC-005](./phase7-docs/DOC-005-pane-navigation.md) | Create Pane Navigation Guide | Medium | 2 days |

**Phase 7 Total: ~9 days**

---

### Phase 8: Testing Infrastructure (6 Tasks)
Ensures comprehensive test coverage.

| ID | Task | Complexity | Est. Time |
|----|------|------------|-----------|
| [TEST-001](./phase8-testing/TEST-001-ksp-generator.md) | KSP Generator Unit Tests | Medium | 3 days |
| [TEST-002](./phase8-testing/TEST-002-path-reconstructor.md) | Path Reconstructor Tests | High | 3 days |
| [TEST-003](./phase8-testing/TEST-003-migration-utils.md) | Migration Utility Tests | Medium | 2 days |
| [TEST-004](./phase8-testing/TEST-004-integration.md) | Integration Tests with Demo App | High | 4 days |
| [TEST-005](./phase8-testing/TEST-005-performance.md) | Performance Regression Tests | Medium | 2 days |
| [TEST-006](./phase8-testing/TEST-006-multiplatform.md) | Multiplatform Compatibility Tests | High | 4 days |

**Phase 8 Total: ~18 days**

---

## Dependency Graph

```
Phase 1 (Core) ──────────────────────────────────────────────┐
    │                                                         │
    ├─► Phase 2 (Renderer) ─────────────────────────────────┬─┴──► Phase 7 (Docs)
    │       │                                                │          │
    │       └─► Phase 5 (Migration) ────────────────────────┤          │
    │                                                        │          ▼
    ├─► Phase 4 (Annotations) ───► Phase 3 (KSP) ───────────┤      Phase 8 (Testing)
    │                                                        │
    └─► Phase 6 (Risks) ────────────────────────────────────┘
```

### Critical Path

1. **CORE-001** → **CORE-002** → **CORE-003** (Foundation)
2. **RENDER-002** → **RENDER-004** (Renderer Core)
3. **KSP-004** → **KSP-006** (Code Generation)

---

## Total Estimated Effort

| Phase | Tasks | Estimated Days |
|-------|-------|----------------|
| Phase 1: Core State | 6 | 16-22 |
| Phase 2: Renderer | 8 | 21-28 |
| Phase 3: KSP | 8 | 17-24 |
| Phase 4: Annotations | 4 | 3 |
| Phase 5: Migration | 5 | 9 |
| Phase 6: Risks | 6 | 13 |
| Phase 7: Documentation | 5 | 9 |
| Phase 8: Testing | 6 | 18 |
| **TOTAL** | **48** | **106-126 days** |

> **Note**: Many tasks can be parallelized. With 2-3 developers, the timeline can be reduced to **8-12 weeks**.

---

## Quick Links

- [Phase 1: Core State Refactoring](./phase1-core/)
- [Phase 2: Unified Renderer](./phase2-renderer/)
- [Phase 3: KSP Processor Updates](./phase3-ksp/)
- [Phase 4: Annotations Enhancement](./phase4-annotations/)
- [Phase 5: Migration Utilities](./phase5-migration/)
- [Phase 6: Risk Mitigation](./phase6-risks/)
- [Phase 7: Documentation](./phase7-docs/)
- [Phase 8: Testing Infrastructure](./phase8-testing/)

---

## Key Decisions Required

Before starting implementation, the following decisions should be made:

1. **Node ID Strategy**: UUID vs structured path (e.g., `root/stack0/screen1`)
   - *Recommendation*: UUID with optional debug labels

2. **Cascading Pop Behavior**: When `StackNode` becomes empty after pop
   - *Recommendation*: Configurable via `PopBehavior.CASCADE` vs `PopBehavior.PRESERVE_EMPTY`

3. **TabNode Initialization**: Pre-create all stacks or lazy-initialize
   - *Recommendation*: Lazy for memory efficiency

4. **Backward Compatibility Duration**: How long to maintain deprecated APIs
   - *Recommendation*: 2 major versions with deprecation warnings

5. **Path Reconstructor Approach**: Compile-time (KSP) vs runtime resolution
   - *Recommendation*: Hybrid - KSP generates structure, runtime resolves parameters

---

## References

- [Original Refactoring Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
- [Current Architecture Documentation](../../quo-vadis-core/docs/ARCHITECTURE.md)
- [API Reference](../../quo-vadis-core/docs/API_REFERENCE.md)
- [Shared Element Transitions](../../quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md)
