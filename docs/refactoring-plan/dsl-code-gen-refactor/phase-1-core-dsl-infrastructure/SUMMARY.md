# Phase 1: Core DSL Infrastructure

> **Phase Status**: ⬜ Not Started  
> **Duration**: 2 weeks  
> **Tasks**: 3 (1.1, 1.2, 1.3)  
> **Full Plan**: [DSL_CODE_GENERATION_REFACTORING.md](../../DSL_CODE_GENERATION_REFACTORING.md)

---

## Phase Objectives

This phase establishes the foundational infrastructure for the DSL-based navigation configuration system. The goal is to create a type-safe, composable configuration API that will:

1. **Define the `NavigationConfig` interface** - A unified contract for all navigation registries
2. **Create DSL builder classes** - Type-safe builders for screens, containers, scopes, and more
3. **Implement the runtime configuration** - Convert DSL definitions into runtime-usable registries

---

## Key Deliverables

### 1. NavigationConfig Interface (Task 1.1)
- `NavigationConfig.kt` - Core interface with all registry properties
- `EmptyNavigationConfig.kt` - Default no-op implementation
- `CompositeNavigationConfig.kt` - Support for combining multiple configs

### 2. DSL Builder Infrastructure (Task 1.2)
- `NavigationConfigBuilder.kt` - Main entry point for DSL configuration
- `StackBuilder.kt` - Builder for stack containers
- `TabsBuilder.kt` - Builder for tab containers  
- `PanesBuilder.kt` - Builder for pane containers
- `NavigationConfigDsl.kt` - DSL marker annotation
- Supporting data classes and sealed types

### 3. DslNavigationConfig Implementation (Task 1.3)
- `DslNavigationConfig.kt` - Runtime implementation of `NavigationConfig`
- `DslScreenRegistry.kt` - DSL-based screen registry implementation
- `DslContainerRegistry.kt` - DSL-based container registry implementation
- `DslScopeRegistry.kt` - DSL-based scope registry implementation
- Supporting runtime structures

---

## Dependencies

### This Phase Depends On

| Dependency | Description | Status |
|------------|-------------|--------|
| Existing Registry Interfaces | `ScreenRegistry`, `ScopeRegistry`, `ContainerRegistry`, etc. | ✅ Exists |
| NavNode Types | `StackNode`, `TabNode`, `PaneNode`, `ScreenNode` | ✅ Exists |
| Destination Interface | Base `Destination` type | ✅ Exists |
| Compose Dependencies | Compose runtime, annotations | ✅ Exists |

**Location of existing registries**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/`

### External Dependencies
- None - this phase uses only existing Quo Vadis core types

---

## Dependents

### What Depends on This Phase

| Phase/Task | Description | Dependency Type |
|------------|-------------|-----------------|
| Phase 2 (Convenience Composables) | Uses `NavigationConfig` interface | **Hard** |
| Phase 3 (KSP Generator) | Generates code that uses DSL builders | **Hard** |
| Phase 4 (Migration) | Deprecation points to new APIs | **Hard** |
| Phase 5 (Documentation) | Documents new APIs | **Soft** |

**Impact**: This phase is the critical foundation. All subsequent phases cannot begin until Phase 1 is complete.

---

## Success Criteria

### Technical Criteria

- [ ] `NavigationConfig` interface compiles and provides access to all 6 registry types
- [ ] DSL builders support all container types: `stack`, `tabs`, `panes`
- [ ] DSL marker annotation prevents scope leakage in nested builders
- [ ] `DslNavigationConfig` correctly converts builder state to runtime registries
- [ ] `buildNavNode()` correctly creates NavNode trees from container definitions
- [ ] `plus` operator correctly combines two `NavigationConfig` instances
- [ ] All new code is properly documented with KDoc

### Quality Criteria

- [ ] Unit tests exist for all builder permutations
- [ ] Unit tests exist for config composition logic
- [ ] Unit tests exist for `buildNavNode()` scenarios
- [ ] No compilation warnings in new code
- [ ] Code follows existing project style conventions

### Integration Criteria

- [ ] New APIs integrate with existing registry interfaces without modification
- [ ] Existing navigation code continues to work unchanged
- [ ] Demo app can use new DSL manually (before code generation)

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| API Design Issues | Medium | High | Review DSL ergonomics early, get feedback before full implementation |
| Type System Complexity | Medium | Medium | Use established Kotlin DSL patterns, leverage reified type parameters |
| Performance Overhead | Low | Medium | Use lazy initialization, benchmark against current approach |
| Scope Leakage in DSL | Medium | Low | Implement `@DslMarker` annotation correctly |
| Missing Container Features | Low | High | Audit all existing annotation features before finalizing builders |

---

## Task Dependencies Within Phase

```
┌─────────────────────────────────────────────────────────────┐
│                         Phase 1                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────────┐                                      │
│   │ Task 1.1         │                                      │
│   │ NavigationConfig │                                      │
│   │ Interface        │                                      │
│   └────────┬─────────┘                                      │
│            │                                                │
│            ▼                                                │
│   ┌──────────────────┐                                      │
│   │ Task 1.2         │                                      │
│   │ DSL Builder      │                                      │
│   │ Infrastructure   │                                      │
│   └────────┬─────────┘                                      │
│            │                                                │
│            ▼                                                │
│   ┌──────────────────┐                                      │
│   │ Task 1.3         │                                      │
│   │ DslNavigationConfig                                     │
│   │ Implementation   │                                      │
│   └──────────────────┘                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Execution Order**: Tasks must be completed sequentially (1.1 → 1.2 → 1.3)

---

## Files Overview

| Task | File | Location | Type |
|------|------|----------|------|
| 1.1 | `NavigationConfig.kt` | `quo-vadis-core/.../navigation/` | Interface |
| 1.1 | `EmptyNavigationConfig.kt` | `quo-vadis-core/.../navigation/` | Object |
| 1.1 | `CompositeNavigationConfig.kt` | `quo-vadis-core/.../navigation/` | Class |
| 1.2 | `NavigationConfigDsl.kt` | `quo-vadis-core/.../navigation/dsl/` | Annotation |
| 1.2 | `NavigationConfigBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.2 | `StackBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.2 | `TabsBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.2 | `PanesBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.2 | `ContainerBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Sealed Class |
| 1.2 | `BuilderDataClasses.kt` | `quo-vadis-core/.../navigation/dsl/` | Data Classes |
| 1.3 | `DslNavigationConfig.kt` | `quo-vadis-core/.../navigation/` | Class |
| 1.3 | `DslScreenRegistry.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.3 | `DslContainerRegistry.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |
| 1.3 | `DslScopeRegistry.kt` | `quo-vadis-core/.../navigation/dsl/` | Class |

**Base Path**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/`

---

## Estimated Effort

| Task | Effort | Notes |
|------|--------|-------|
| 1.1 | 2-3 days | Interface design, composition logic |
| 1.2 | 4-5 days | Multiple builders, type safety, DSL patterns |
| 1.3 | 3-4 days | Runtime conversion, buildNavNode logic |

**Total**: ~10-12 days (fits within 2-week timeline)

---

## Review Checklist

Before marking Phase 1 complete:

- [ ] All 3 tasks marked as completed
- [ ] All acceptance criteria for each task verified
- [ ] Code review completed for all new files
- [ ] Unit tests passing
- [ ] Documentation updated
- [ ] No regressions in existing functionality
- [ ] Manual testing with DSL configuration in demo app
