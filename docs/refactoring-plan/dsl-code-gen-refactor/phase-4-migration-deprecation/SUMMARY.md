# Phase 4: Migration & Deprecation

> **Phase Status**: ⬜ Not Started  
> **Duration**: 1 week  
> **Tasks**: 3 (4.1, 4.2, 4.3)  
> **Full Plan**: [DSL_CODE_GENERATION_REFACTORING.md](../../DSL_CODE_GENERATION_REFACTORING.md)

---

## Phase Objectives

This phase provides the migration path from old APIs to the new DSL-based architecture. The goal is to ensure a **smooth, non-breaking transition** for existing users while guiding them toward the improved APIs.

### Primary Goals

1. **Add deprecation warnings** - Mark old APIs as deprecated with clear migration guidance
2. **Update demo application** - Showcase new patterns as reference implementation
3. **Create migration documentation** - Step-by-step guides for common migration scenarios

---

## Key Deliverables

### 1. Deprecation Warnings (Task 4.1)
- Deprecated `build*NavNode()` functions with `@Deprecated` and `ReplaceWith`
- Deprecated individual registry objects pointing to unified config
- Documentation of direct registry parameter deprecation
- Clear migration messages in deprecation annotations

### 2. Updated Demo Application (Task 4.2)
- One-liner integration example (`QuoVadisNavigation`)
- Standard pattern example (`rememberQuoVadisNavigator` + `NavigationHost`)
- Advanced configuration example (multi-module composition, custom options)
- Before/after code comparisons

### 3. Migration Guide Documentation (Task 4.3)
- `docs/MIGRATION_DSL.md` comprehensive guide
- Step-by-step migration patterns
- Common scenarios and solutions
- Troubleshooting section and FAQ

---

## Dependencies

### This Phase Depends On

| Dependency | Description | Status | Critical |
|------------|-------------|--------|----------|
| Phase 1 Complete | `NavigationConfig` interface and DSL builders | ⬜ | **Yes** |
| Phase 2 Complete | Convenience composables (`rememberQuoVadisNavigator`, etc.) | ⬜ | **Yes** |
| Phase 3 Complete | KSP generates new `GeneratedNavigationConfig` | ⬜ | **Yes** |
| Existing Demo App | Current demo application to update | ✅ Exists | Yes |

**All Previous Phases Must Be Complete Before Starting Phase 4**

### Files to Reference

| Module | Path | Purpose |
|--------|------|---------|
| quo-vadis-ksp | `generators/NavNodeBuilderGenerator.kt` | Functions to deprecate |
| quo-vadis-ksp | Generated `*Registry.kt` files | Objects to deprecate |
| quo-vadis-core | `navigation/NavigationConfig.kt` | New API (from Phase 1) |
| quo-vadis-core | `compose/QuoVadisComposables.kt` | New composables (from Phase 2) |
| composeApp | `src/commonMain/` | Demo app to update |

---

## Dependents

### What Depends on This Phase

| Phase/Task | Description | Dependency Type |
|------------|-------------|-----------------|
| Phase 5 (Documentation) | References deprecation and migration guide | **Hard** |
| User Projects | Follow migration guide for upgrades | **Soft** |
| Library Releases | Deprecation warnings appear in releases | **Soft** |

**Impact**: This phase is critical for user experience during the transition period. Clear deprecation and migration guidance ensures adoption of new APIs.

---

## Success Criteria

### Technical Criteria

- [ ] All old `build*NavNode()` functions marked `@Deprecated`
- [ ] All individual registry objects marked `@Deprecated`  
- [ ] Deprecation messages include `ReplaceWith` where applicable
- [ ] Deprecation levels appropriate (`WARNING` initially, not `ERROR`)
- [ ] No breaking changes - all old code still compiles and runs
- [ ] Demo app demonstrates all three usage patterns (one-liner, standard, advanced)

### Documentation Criteria

- [ ] `MIGRATION_DSL.md` exists with complete content
- [ ] Before/after examples for each migration scenario
- [ ] Troubleshooting section covers common issues
- [ ] FAQ addresses anticipated questions
- [ ] All code examples compile and work

### Quality Criteria

- [ ] Deprecation messages are clear and actionable
- [ ] Demo app code is clean and well-commented
- [ ] Migration guide is easy to follow
- [ ] No warnings in demo app after migration
- [ ] All demo functionality preserved after migration

---

## Risk Assessment

| Risk | Probability | Impact | Severity | Mitigation |
|------|-------------|--------|----------|------------|
| Users ignore deprecation warnings | Medium | Low | Low | Clear messaging, prominent docs |
| Migration guide incomplete | Medium | Medium | Medium | Review common usage patterns |
| Demo app migration reveals issues | Low | High | Medium | Thorough testing, Phase 3 validation |
| Deprecation breaks CI/builds | Low | High | Medium | Use `WARNING` level, not `ERROR` |
| Multi-module migration complexity | Medium | Medium | Medium | Dedicated section in migration guide |

---

## Task Dependencies Within Phase

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Phase 4                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐       │
│   │ Task 4.1         │   │ Task 4.2         │   │ Task 4.3         │       │
│   │ Deprecation      │   │ Update Demo      │   │ Migration        │       │
│   │ Warnings         │   │ Application      │   │ Guide            │       │
│   └────────┬─────────┘   └────────┬─────────┘   └────────┬─────────┘       │
│            │                      │                      │                  │
│            └──────────────────────┼──────────────────────┘                  │
│                                   │                                         │
│                                   ▼                                         │
│                        ┌──────────────────┐                                 │
│                        │ All tasks can    │                                 │
│                        │ proceed in       │                                 │
│                        │ parallel         │                                 │
│                        └──────────────────┘                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Execution Order**: All tasks can be done in **parallel** since they don't depend on each other.

---

## Files Overview

| Task | File | Location | Type |
|------|------|----------|------|
| 4.1 | Various generated files | `quo-vadis-ksp/generators/` | Modified |
| 4.1 | Generated output templates | `quo-vadis-ksp/` | Modified |
| 4.2 | `App.kt` (or similar) | `composeApp/src/commonMain/` | Modified |
| 4.2 | Navigation setup files | `composeApp/src/commonMain/` | Modified |
| 4.3 | `MIGRATION_DSL.md` | `docs/` | New |

**Primary Output**: Updated generated code with deprecations + `docs/MIGRATION_DSL.md`

---

## Estimated Effort

| Task | Effort | Notes |
|------|--------|-------|
| 4.1 Deprecation Warnings | 1-2 days | Add annotations to generators |
| 4.2 Update Demo Application | 2 days | Migrate and document patterns |
| 4.3 Migration Guide | 2 days | Comprehensive documentation |

**Total**: ~5-6 days (fits within 1-week timeline)

---

## Deprecation Strategy

### Phase 1: Warning Level (This Phase)
- Use `DeprecationLevel.WARNING`
- All old code continues to compile
- IDE shows warnings but doesn't block

### Phase 2: Future Release (Not This Phase)
- Upgrade to `DeprecationLevel.ERROR` 
- Old APIs still present but unusable
- Forces migration before major version

### Phase 3: Removal (Future Major Version)
- Remove deprecated APIs entirely
- Only new APIs available
- Clean codebase

---

## Migration Path Overview

### Old Pattern → New Pattern

| Old API | New API | Complexity |
|---------|---------|------------|
| `buildMainTabsNavNode()` | `config.buildNavNode(MainTabs::class)` | Low |
| `GeneratedScreenRegistry` | `GeneratedNavigationConfig.screenRegistry` | Low |
| Multiple registry params | Single `config` param | Medium |
| Manual `TreeNavigator` setup | `rememberQuoVadisNavigator()` | Low |
| Full manual setup | `QuoVadisNavigation()` one-liner | Low |

---

## Review Checklist

Before marking Phase 4 complete:

- [ ] All 3 tasks marked as completed
- [ ] All acceptance criteria for each task verified
- [ ] Deprecation warnings verified in IDE
- [ ] Demo app runs without issues
- [ ] Migration guide reviewed for completeness
- [ ] All code examples in guide tested
- [ ] No regressions in existing functionality

---

## Related Files

- [Task 4.1 - Deprecation Warnings](./TASK-4.1-deprecation-warnings.md)
- [Task 4.2 - Update Demo Application](./TASK-4.2-update-demo-app.md)
- [Task 4.3 - Migration Guide](./TASK-4.3-migration-guide.md)
- [Phase 3 Summary](../phase-3-ksp-generator-refactoring/SUMMARY.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
