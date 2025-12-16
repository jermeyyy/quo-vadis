```markdown
# Phase 5: Documentation & Testing

> **Phase Status**: ⬜ Not Started  
> **Duration**: 1 week  
> **Tasks**: 2 (5.1, 5.2)  
> **Dependencies**: Phase 1, Phase 2, Phase 3, Phase 4  
> **Full Plan**: [DSL_CODE_GENERATION_REFACTORING.md](../../DSL_CODE_GENERATION_REFACTORING.md)

---

## Phase Objectives

This phase finalizes the DSL refactoring by ensuring comprehensive documentation and thorough test coverage. The goal is to:

1. **Document all new APIs** - KDoc, website documentation, code samples
2. **Achieve comprehensive test coverage** - Unit tests, integration tests, regression tests

---

## Key Deliverables

### 1. API Documentation (Task 5.1)
- KDoc documentation for all new public APIs
- Website documentation updates in `docs/site/`
- Updated code samples and usage guides
- API reference reflecting new patterns

### 2. Comprehensive Testing (Task 5.2)
- DSL Builder unit tests (all permutations)
- NavigationConfig unit tests (composition, lookup, building)
- KSP output validation tests
- Integration tests for end-to-end navigation flows
- Migration tests (old patterns continue working)
- 90%+ code coverage target

---

## Dependencies

### This Phase Depends On

| Dependency | Description | Status |
|------------|-------------|--------|
| Phase 1 | Core DSL Infrastructure | ⬜ Not Started |
| Phase 2 | Convenience Composables | ⬜ Not Started |
| Phase 3 | KSP Generator Refactoring | ⬜ Not Started |
| Phase 4 | Migration & Deprecation | ⬜ Not Started |

**Note**: This is the final phase. All prior phases must be complete before documentation and testing can be finalized.

### External Dependencies
- Documentation site framework in `docs/site/`

---

## Dependents

### What Depends on This Phase

| Item | Description | Dependency Type |
|------|-------------|-----------------|
| Release | Final release of DSL refactoring | **Hard** |
| External Users | API documentation for adoption | **Soft** |

---

## Success Criteria

### Documentation Criteria

- [ ] All new public APIs have comprehensive KDoc
- [ ] Website documentation updated with new patterns
- [ ] Code samples demonstrate one-liner, standard, and advanced usage
- [ ] Migration guide linked from relevant documentation
- [ ] API reference is complete and accurate

### Testing Criteria

- [ ] 90%+ code coverage on new code
- [ ] All DSL builder permutations tested
- [ ] Config composition logic tested
- [ ] KSP generated code validates correctly
- [ ] End-to-end navigation flows work
- [ ] Old APIs continue to work (regression tests)
- [ ] Multi-module scenarios tested

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Documentation gaps | Medium | Medium | Review checklist, multiple reviewers |
| Insufficient test coverage | Low | High | Coverage tools, CI enforcement |
| Missing edge cases | Medium | Medium | Property-based testing where applicable |
| Stale documentation | Low | Low | Keep docs close to code, review process |

---

## Task Dependencies Within Phase

```
┌─────────────────────────────────────────────────────────────┐
│                         Phase 5                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────────┐     ┌──────────────────┐            │
│   │ Task 5.1         │     │ Task 5.2         │            │
│   │ API              │     │ Comprehensive    │            │
│   │ Documentation    │     │ Testing          │            │
│   └──────────────────┘     └──────────────────┘            │
│         │                         │                        │
│         └───────────┬─────────────┘                        │
│                     │                                      │
│                     ▼                                      │
│            ┌────────────────┐                              │
│            │ Phase Complete │                              │
│            └────────────────┘                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Execution Order**: Tasks 5.1 and 5.2 can be worked on in parallel.

---

## Files Overview

| Task | File/Directory | Location | Type |
|------|----------------|----------|------|
| 5.1 | KDoc comments | `quo-vadis-core/src/commonMain/...` | Code docs |
| 5.1 | Website updates | `docs/site/src/` | Documentation |
| 5.1 | Code samples | `docs/site/`, README | Examples |
| 5.2 | DSL builder tests | `quo-vadis-core/src/commonTest/.../dsl/` | Unit tests |
| 5.2 | Config tests | `quo-vadis-core/src/commonTest/.../navigation/` | Unit tests |
| 5.2 | KSP output tests | `quo-vadis-ksp/src/test/...` | Unit tests |
| 5.2 | Integration tests | `composeApp/src/commonTest/...` | Integration tests |

---

## Estimated Effort

| Task | Effort | Notes |
|------|--------|-------|
| 5.1 | 2-3 days | KDoc, website, samples |
| 5.2 | 3-4 days | Comprehensive test suite |

**Total**: ~5-7 days (fits within 1-week timeline)

---

## Review Checklist

Before marking Phase 5 complete:

- [ ] All 2 tasks marked as completed
- [ ] All acceptance criteria for each task verified
- [ ] Documentation reviewed for accuracy
- [ ] Test coverage meets 90% target
- [ ] CI pipeline passing
- [ ] Final regression testing completed
- [ ] Ready for release

```
