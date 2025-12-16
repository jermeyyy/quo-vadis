# Phase 3: KSP Generator Refactoring

> **Phase Status**: ⬜ Not Started  
> **Duration**: 2 weeks  
> **Tasks**: 4 (3.1, 3.2, 3.3, 3.4)  
> **Full Plan**: [DSL_CODE_GENERATION_REFACTORING.md](../../DSL_CODE_GENERATION_REFACTORING.md)

---

## Phase Objectives

This phase transforms the KSP code generators to produce unified, DSL-based output instead of the current scattered, imperative registry files. This is the **most complex phase** of the refactoring effort, requiring careful coordination of all existing generators and introduction of new base classes.

### Primary Goals

1. **Eliminate duplicate code** - Create shared base classes and utilities across all generators
2. **Consolidate output** - Generate single `GeneratedNavigationConfig.kt` instead of 6+ files
3. **Produce DSL-style code** - Output uses the new `navigationConfig { }` DSL from Phase 1/2
4. **Maintain annotation compatibility** - All existing annotations (`@Screen`, `@Tabs`, `@Stack`, etc.) continue to work
5. **Improve error messages** - Better diagnostics for misconfiguration

---

## Key Deliverables

### 1. Generator Base Classes (Task 3.1)
- `DslCodeGenerator.kt` - Abstract base class for all DSL generators
- `CodeBlockBuilders.kt` - Reusable KotlinPoet code block builders
- `StringTemplates.kt` - Shared string templates for common patterns

### 2. NavigationConfigGenerator (Task 3.2)
- `NavigationConfigGenerator.kt` - Main orchestrating generator
- `ScreenBlockGenerator.kt` - Generates `screen<T>` DSL blocks
- `ContainerBlockGenerator.kt` - Generates `tabs`, `stack`, `panes` DSL blocks
- `ScopeBlockGenerator.kt` - Generates `scope()` DSL blocks
- `TransitionBlockGenerator.kt` - Generates `transition<T>` DSL blocks
- `WrapperBlockGenerator.kt` - Generates `tabWrapper`/`paneWrapper` DSL blocks

### 3. Refactored Existing Generators (Task 3.3)
- Convert 7 existing generators to produce DSL blocks
- Deprecate `NavNodeBuilderGenerator` (keep for compatibility)
- Integrate all output into single config file

### 4. Processor Orchestration (Task 3.4)
- Updated `QuoVadisSymbolProcessor.kt`
- Single-file generation pipeline
- Improved incremental processing
- Enhanced error reporting

---

## Current vs Target Architecture

### Current Architecture (Before)

```
quo-vadis-ksp/
├── QuoVadisSymbolProcessor.kt          # Orchestrates all generators
├── generators/
│   ├── ScreenRegistryGenerator.kt      # → GeneratedScreenRegistry.kt
│   ├── ContainerRegistryGenerator.kt   # → GeneratedContainerRegistry.kt
│   ├── ScopeRegistryGenerator.kt       # → GeneratedScopeRegistry.kt
│   ├── TransitionRegistryGenerator.kt  # → GeneratedTransitionRegistry.kt
│   ├── WrapperRegistryGenerator.kt     # → GeneratedWrapperRegistry.kt
│   ├── NavNodeBuilderGenerator.kt      # → *NavNodeBuilder.kt (per container)
│   ├── DeepLinkHandlerGenerator.kt     # → GeneratedDeepLinkHandler.kt
│   └── NavigatorExtGenerator.kt        # → NavigatorExtensions.kt
├── extractors/
│   └── ... (unchanged)
└── models/
    └── ... (unchanged)
```

**Output**: 6-10+ generated files per project

### Target Architecture (After)

```
quo-vadis-ksp/
├── QuoVadisSymbolProcessor.kt          # Simplified orchestration
├── generators/
│   ├── base/
│   │   ├── DslCodeGenerator.kt         # NEW: Abstract base class
│   │   ├── CodeBlockBuilders.kt        # NEW: Shared builders
│   │   └── StringTemplates.kt          # NEW: Reusable templates
│   ├── dsl/
│   │   ├── NavigationConfigGenerator.kt    # NEW: Main DSL generator
│   │   ├── ScreenBlockGenerator.kt         # NEW: Screen DSL blocks
│   │   ├── ContainerBlockGenerator.kt      # NEW: Container DSL blocks
│   │   ├── ScopeBlockGenerator.kt          # NEW: Scope DSL blocks
│   │   ├── TransitionBlockGenerator.kt     # NEW: Transition DSL blocks
│   │   ├── WrapperBlockGenerator.kt        # NEW: Wrapper DSL blocks
│   │   └── DeepLinkBlockGenerator.kt       # NEW: Deep link DSL blocks
│   └── legacy/
│       ├── NavNodeBuilderGenerator.kt  # DEPRECATED but kept
│       └── NavigatorExtGenerator.kt    # May keep for extensions
├── extractors/
│   └── ... (unchanged)
└── models/
    └── ... (unchanged)
```

**Output**: 1 primary generated file (`GeneratedNavigationConfig.kt`) + optional legacy files

---

## Generated Code Comparison

### Before (Current) - Multiple Files

```kotlin
// File 1: GeneratedScreenRegistry.kt
object GeneratedScreenRegistry : ScreenRegistry {
    @Composable
    override fun Content(destination: Destination, navigator: Navigator, ...) {
        when (destination) {
            is HomeDestination.Feed -> FeedScreen(navigator)
            is HomeDestination.Detail -> DetailScreen(destination, navigator)
            // ... 50+ cases
            else -> error("No screen registered")
        }
    }
}

// File 2: GeneratedScopeRegistry.kt
object GeneratedScopeRegistry : ScopeRegistry {
    private val scopeMap = mapOf(
        "MainTabs" to setOf(MainTabs.HomeTab::class, ...),
        // ... more scopes
    )
}

// File 3: GeneratedContainerRegistry.kt
// File 4: GeneratedTransitionRegistry.kt
// File 5: GeneratedWrapperRegistry.kt
// File 6: MainTabsNavNodeBuilder.kt
// File 7: ProfileStackNavNodeBuilder.kt
// ... potentially more files
```

### After (Target) - Single Unified File

```kotlin
// GeneratedNavigationConfig.kt
object GeneratedNavigationConfig : NavigationConfig {
    
    private val config = navigationConfig {
        // ═══════════════════════════════════════════════
        // SCREENS
        // ═══════════════════════════════════════════════
        
        screen<HomeDestination.Feed> { FeedScreen(navigator) }
        screen<HomeDestination.Detail> { DetailScreen(destination = it, navigator) }
        screen<ProfileDestination> { ProfileScreen(userId = it.userId, navigator) }
        
        // ═══════════════════════════════════════════════
        // CONTAINERS
        // ═══════════════════════════════════════════════
        
        tabs<MainTabs>(scopeKey = "MainTabs") {
            initialTab = 0
            tab(MainTabs.HomeTab, title = "Home", icon = Icons.Home)
            tab(MainTabs.ExploreTab, title = "Explore") {
                screen(ExploreDestination.List)
            }
        }
        
        stack<ProfileStack>(scopeKey = "ProfileStack") {
            screen(ProfileDestination.Main)
        }
        
        // ═══════════════════════════════════════════════
        // SCOPES
        // ═══════════════════════════════════════════════
        
        scope("MainTabs", MainTabs.HomeTab::class, MainTabs.ExploreTab::class)
        
        // ═══════════════════════════════════════════════
        // TRANSITIONS
        // ═══════════════════════════════════════════════
        
        transition<HomeDestination.Detail>(NavTransitions.SharedElement)
        
        // ═══════════════════════════════════════════════
        // WRAPPERS
        // ═══════════════════════════════════════════════
        
        tabWrapper("mainTabsWrapper") {
            CustomTabBar(tabs, activeTabIndex, ::switchToTab) { content() }
        }
    }
    
    // Delegate to DSL-built config
    override val screenRegistry = config.screenRegistry
    override val wrapperRegistry = config.wrapperRegistry
    override val scopeRegistry = config.scopeRegistry
    override val transitionRegistry = config.transitionRegistry
    override val containerRegistry = config.containerRegistry
    override val deepLinkHandler = config.deepLinkHandler
    
    override fun buildNavNode(...) = config.buildNavNode(...)
    override fun plus(other: NavigationConfig) = CompositeNavigationConfig(this, other)
}
```

---

## Dependencies

### This Phase Depends On

| Dependency | Description | Status | Critical |
|------------|-------------|--------|----------|
| Phase 1 Complete | `NavigationConfig` interface and DSL builders | ⬜ | **Yes** |
| Phase 2 Complete | `navigationConfig { }` DSL function available | ⬜ | **Yes** |
| Existing Extractors | `ScreenExtractor`, `TabExtractor`, etc. | ✅ Exists | Yes |
| Existing Models | `ScreenInfo`, `TabInfo`, `PaneInfo`, etc. | ✅ Exists | Yes |
| KotlinPoet | Code generation library | ✅ Exists | Yes |

**Phase 1 & 2 Must Be Complete Before Starting Phase 3**

### Files to Reference

| Module | Path | Purpose |
|--------|------|---------|
| quo-vadis-ksp | `generators/ScreenRegistryGenerator.kt` | Existing screen generation patterns |
| quo-vadis-ksp | `generators/ContainerRegistryGenerator.kt` | Existing container patterns |
| quo-vadis-ksp | `generators/NavNodeBuilderGenerator.kt` | Node building logic to preserve |
| quo-vadis-ksp | `QuoVadisSymbolProcessor.kt` | Current orchestration flow |
| quo-vadis-ksp | `extractors/*.kt` | Data extraction (unchanged) |
| quo-vadis-ksp | `models/*.kt` | Info classes (unchanged) |
| quo-vadis-core | `navigation/dsl/*.kt` | Target DSL APIs (from Phase 1) |

---

## Dependents

### What Depends on This Phase

| Phase/Task | Description | Dependency Type |
|------------|-------------|-----------------|
| Phase 4 (Migration) | Deprecation warnings reference new generated code | **Hard** |
| Phase 5 (Documentation) | Documents new generated output format | **Hard** |
| Demo App | Uses new one-liner integration | **Soft** |
| User Projects | Gradual migration to new APIs | **Soft** |

**Impact**: This phase produces the new generated code that all downstream phases and user projects will consume.

---

## Success Criteria

### Technical Criteria

- [ ] Single `GeneratedNavigationConfig.kt` file generated instead of 6+ files
- [ ] Generated code uses DSL syntax (`navigationConfig { }`, `screen<T>`, etc.)
- [ ] All existing annotation types supported (`@Screen`, `@Tabs`, `@Stack`, `@Pane`, `@Transition`, `@TabWrapper`, `@PaneWrapper`)
- [ ] Generated code compiles without errors
- [ ] Generated config properly implements `NavigationConfig` interface
- [ ] `buildNavNode()` delegation works correctly
- [ ] Multi-module composition via `+` operator works

### Code Quality Criteria

- [ ] No code duplication across generators (shared base classes used)
- [ ] KotlinPoet best practices followed
- [ ] Generated code is readable and well-formatted
- [ ] Proper imports (no star imports, minimal import count)
- [ ] Generated KDoc comments where appropriate

### Compatibility Criteria

- [ ] All existing annotations continue to work unchanged
- [ ] No breaking changes to annotation API
- [ ] Legacy generators still available (deprecated) for gradual migration
- [ ] Incremental processing still functional

### Performance Criteria

- [ ] Build time impact < 5% compared to current generators
- [ ] Generated file size comparable or smaller than combined current files
- [ ] Memory usage during KSP processing acceptable

---

## Risk Assessment

| Risk | Probability | Impact | Severity | Mitigation |
|------|-------------|--------|----------|------------|
| Complex refactoring causes bugs | **High** | High | **Critical** | Extensive testing, incremental changes, feature flags |
| Generated code doesn't compile | Medium | High | High | Compile-time verification, integration tests |
| Incremental processing breaks | Medium | Medium | Medium | Careful dependency tracking, test incremental scenarios |
| Performance regression | Medium | Medium | Medium | Benchmark before/after, optimize hot paths |
| Missing edge cases in conversion | **High** | Medium | High | Audit all existing generator code paths |
| DSL API changes during Phase 1/2 | Medium | High | High | Coordinate phases, integration testing |
| Multi-module scenarios fail | Medium | High | High | Test with multi-module demo project |

### Mitigation Strategies

1. **Incremental Development**: Implement one generator conversion at a time
2. **Feature Flag**: Add option to use legacy vs new generators during transition
3. **Comprehensive Tests**: Unit tests for each generator, integration tests for full pipeline
4. **Before/After Comparison**: Generate both old and new output, compare functionality
5. **Code Review**: Each task requires code review before merging

---

## Task Dependencies Within Phase

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Phase 3                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │  Task 3.1: Generator Base Classes                                    │  │
│   │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │  │
│   │  │DslCodeGenerator│  │CodeBlockBuilders│  │StringTemplates │         │  │
│   │  └────────────────┘  └────────────────┘  └────────────────┘         │  │
│   └───────────────────────────────┬──────────────────────────────────────┘  │
│                                   │                                         │
│                                   ▼                                         │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │  Task 3.2: NavigationConfigGenerator                                 │  │
│   │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │  │
│   │  │NavigationConfig│  │ScreenBlock     │  │ContainerBlock  │         │  │
│   │  │Generator       │  │Generator       │  │Generator       │         │  │
│   │  └────────────────┘  └────────────────┘  └────────────────┘         │  │
│   │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │  │
│   │  │ScopeBlock      │  │TransitionBlock │  │WrapperBlock    │         │  │
│   │  │Generator       │  │Generator       │  │Generator       │         │  │
│   │  └────────────────┘  └────────────────┘  └────────────────┘         │  │
│   └───────────────────────────────┬──────────────────────────────────────┘  │
│                                   │                                         │
│          ┌────────────────────────┼────────────────────────┐                │
│          │                        │                        │                │
│          ▼                        ▼                        ▼                │
│   ┌─────────────┐          ┌─────────────┐          ┌─────────────┐        │
│   │Task 3.3:    │          │Task 3.4:    │          │             │        │
│   │Refactor     │◄────────►│Processor    │          │             │        │
│   │Existing Gen │          │Orchestration│          │             │        │
│   └─────────────┘          └─────────────┘          │             │        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Execution Order**:
1. Task 3.1 first (base classes needed by all)
2. Task 3.2 next (main generator structure)
3. Tasks 3.3 and 3.4 can be done in parallel after 3.2

---

## Files Overview

| Task | File | Location | Type |
|------|------|----------|------|
| 3.1 | `DslCodeGenerator.kt` | `generators/base/` | Abstract Class |
| 3.1 | `CodeBlockBuilders.kt` | `generators/base/` | Object |
| 3.1 | `StringTemplates.kt` | `generators/base/` | Object |
| 3.2 | `NavigationConfigGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `ScreenBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `ContainerBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `ScopeBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `TransitionBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `WrapperBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.2 | `DeepLinkBlockGenerator.kt` | `generators/dsl/` | Class |
| 3.3 | Various existing generators | `generators/` | Refactored |
| 3.4 | `QuoVadisSymbolProcessor.kt` | Root | Modified |

**Base Path**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`

---

## Estimated Effort

| Task | Effort | Notes |
|------|--------|-------|
| 3.1 Generator Base Classes | 2-3 days | Foundation for all other tasks |
| 3.2 NavigationConfigGenerator | 4-5 days | Most complex, multiple sub-generators |
| 3.3 Refactor Existing Generators | 3-4 days | 7 generators to convert/deprecate |
| 3.4 Processor Orchestration | 2-3 days | Pipeline changes, testing |

**Total**: ~12-15 days (fits within 2-week timeline with buffer)

---

## Testing Strategy

### Unit Tests

- Each sub-generator tested in isolation
- Input: Model objects (ScreenInfo, TabInfo, etc.)
- Output: KotlinPoet CodeBlock verification

### Integration Tests

- Full pipeline test with sample annotated classes
- Compile generated code and verify it works
- Compare behavior with legacy generators

### Regression Tests

- Ensure existing test cases still pass
- No behavior changes for annotation processing

### Multi-Module Tests

- Test config composition across modules
- Verify no conflicts in generated code

---

## Review Checklist

Before marking Phase 3 complete:

- [ ] All 4 tasks marked as completed
- [ ] All acceptance criteria for each task verified
- [ ] Code review completed for all new/modified files
- [ ] Unit tests passing for all generators
- [ ] Integration tests passing for full pipeline
- [ ] No regressions in existing functionality
- [ ] Demo app updated to use new generated code
- [ ] Performance benchmarks acceptable
- [ ] Legacy generators properly deprecated

---

## Related Files

- [Task 3.1 - Generator Base Classes](./TASK-3.1-generator-base-classes.md)
- [Task 3.2 - NavigationConfigGenerator](./TASK-3.2-navigation-config-generator.md)
- [Task 3.3 - Refactor Existing Generators](./TASK-3.3-refactor-existing-generators.md)
- [Task 3.4 - Processor Orchestration](./TASK-3.4-processor-orchestration.md)
- [Phase 1 Summary](../phase-1-core-dsl-infrastructure/SUMMARY.md)
- [Phase 2 Summary](../phase-2-convenience-composables/SUMMARY.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
