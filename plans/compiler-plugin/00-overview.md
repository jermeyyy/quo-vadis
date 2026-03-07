# Compiler Plugin Migration: Overview

**Status**: Planning  
**Created**: 2 March 2026  
**Scope**: Migrate Quo-Vadis code generation from KSP to native Kotlin K2 compiler plugin

---

## Executive Summary

Migrate the Quo-Vadis navigation library's metaprogramming pipeline from Kotlin Symbol Processing (KSP) to a native K2 compiler plugin. This eliminates physical file generation, provides instant IDE feedback via FIR synthetic declarations, and drastically reduces build configuration overhead for consumers.

## Current Focus Update

The migration plan remains valid at a high level, but the immediate priority has shifted to restoring KSP parity for container wrapper generation before broader expansion.

New analysis artifacts:

- [Koin pattern analysis](06-koin-pattern-analysis.md)
- [Current-state comparison](07-current-state-comparison.md)
- [Container registry refactor implementation plan](08-implementation-plan.md)

The key conclusion from the March 2026 investigation is that the current compiler plugin is collecting tabs-container metadata, but it is not reliably preserving the working KSP wrapper-dispatch contract in generated IR. The first implementation priority is therefore deterministic container-registry generation, not runtime redesign.

### Current State (KSP)

- `quo-vadis-ksp` module processes 11 annotations via KSP
- Generates 2 files per module: `{Prefix}NavigationConfig.kt` and `{Prefix}DeepLinkHandler.kt`
- Generated config implements `NavigationConfig` (6 sub-registries: Screen, Scope, Transition, Container, DeepLink, PaneRole)
- Multi-module merging via manual `+` operator chaining
- Gradle plugin (`quo-vadis-gradle-plugin`) configures KSP, passes `modulePrefix`, wires source directories

### Target State (Compiler Plugin)

- `quo-vadis-compiler-plugin` + `quo-vadis-compiler-plugin-native` modules
- FIR phase: synthetic `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler` declarations visible in IDE
- IR phase: full implementation logic woven into compiled binaries
- Real-time diagnostics (route collisions, argument mismatches) as IDE errors/warnings
- Multi-module auto-discovery via `@NavigationRoot` — no manual `+` chaining
- Zero Gradle configuration beyond `plugins { id("com.jermey.quo-vadis") }`

---

## Phase Overview

| Phase | Name | Dependencies | Summary |
|-------|------|-------------|---------|
| 1 | [Infrastructure & Gradle Plugin](01-infrastructure.md) | None | New modules, SPI registration, Gradle plugin migration |
| 2 | [FIR Frontend](02-fir-frontend.md) | Phase 1 | Annotation discovery, synthetic declarations, diagnostics |
| 3 | [IR Backend](03-ir-backend.md) | Phase 2 | Implementation logic, NavNode tree, screen/container registries, deep links |
| 4 | [Multi-Module Auto-Discovery](04-multi-module.md) | Phase 3 | `@NavigationRoot`, metadata-driven aggregation |
| 5 | [Testing, Migration & Deprecation](05-testing-migration.md) | Phase 3 (partial), Phase 4 | Test framework, KSP deprecation, migration guide |

### Phase Dependency Graph

```
Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4
                │                       │
                └──→ Phase 5 (testing infrastructure starts with Phase 2)
                            ↑
                     Phase 3 + Phase 4 complete → KSP deprecation
```

---

## Module Topology (Target)

| Module | Purpose | Platform | Key Artifact |
|--------|---------|----------|-------------|
| `quo-vadis-core` | Runtime API, NavNode tree, annotations | All KMP | Unchanged |
| `quo-vadis-annotations` | Annotation definitions | All KMP | Unchanged (retention may change from SOURCE to BINARY) |
| `quo-vadis-gradle-plugin` | Build integration, `KotlinCompilerPluginSupportPlugin` | Gradle JVM | Migrated |
| `quo-vadis-compiler-plugin` | FIR + IR extensions | JVM, JS, Wasm | **New** |
| `quo-vadis-compiler-plugin-native` | Native-specific plugin wrapper | Kotlin/Native | **New** |
| `quo-vadis-ksp` | Legacy KSP processor | Gradle JVM | **Deprecated** (parallel support) |
| `quo-vadis-core-flow-mvi` | FlowMVI integration | All KMP | Unchanged |

---

## What the Compiler Plugin Must Generate

The compiler plugin must replicate everything currently produced by KSP:

### 1. `{Prefix}NavigationConfig` (object)

Implements `NavigationConfig` with 6 sub-registries:

| Registry | Generation Approach | Complexity |
|----------|-------------------|------------|
| `ScreenRegistry` | `when`-based dispatch to `@Screen` composables. Must handle destination param, `SharedTransitionScope?`, `AnimatedVisibilityScope?` | High — involves Compose compiler interop |
| `ScopeRegistry` | Scope key → destination class mappings | Low |
| `TransitionRegistry` | Destination class → `NavTransition` mappings from `@Transition` | Low |
| `ContainerRegistry` | Dual: DSL-based building (`tabs<T>{}`, `panes<T>{}`) + `when`-dispatch to `@TabsContainer`/`@PaneContainer` wrappers | High |
| `DeepLinkRegistry` | Generated via `{Prefix}DeepLinkHandler` reference | Medium (see below) |
| `PaneRoleRegistry` | Scope key + destination → `PaneRole` mapping from `@PaneItem` | Low |

Plus: `buildNavNode()`, `plus()` operator, `roots` property.

### 2. `{Prefix}DeepLinkHandler` (object)

Implements `DeepLinkRegistry`:
- Route pattern list with regex matchers
- Type-safe argument extraction (String, Int, Long, Float, Double, Boolean, Enum)
- URI creation from destinations
- Pattern matching with `{param}` substitution

---

## Annotations to Process

| Annotation | Target | Retention Change | Notes |
|------------|--------|-----------------|-------|
| `@Stack` | CLASS | SOURCE → BINARY | Needed in FIR metadata for cross-module resolution |
| `@Destination` | CLASS | SOURCE → BINARY | Same |
| `@Screen` | FUNCTION | SOURCE → BINARY | Must be visible to IR for `when`-dispatch generation |
| `@Argument` | VALUE_PARAMETER | SOURCE (keep) | Only needed within same-module processing |
| `@Tabs` | CLASS | SOURCE → BINARY | Cross-module container |
| `@TabItem` | CLASS | SOURCE → BINARY | Cross-module container |
| `@Pane` | CLASS | SOURCE → BINARY | Cross-module container |
| `@PaneItem` | CLASS | SOURCE → BINARY | Cross-module container |
| `@TabsContainer` | FUNCTION | RUNTIME (keep) | Already RUNTIME |
| `@PaneContainer` | FUNCTION | RUNTIME (keep) | Already RUNTIME |
| `@Transition` | CLASS | RUNTIME (keep) | Already RUNTIME |

**Key change**: Most annotations move from `SOURCE` to `BINARY` retention so FIR metadata preserves them across modules.

---

## Key Technical Decisions

### 1. DSL Reuse vs Pure IR Generation

**Decision**: Hybrid approach — generate IR that calls the existing `navigationConfig { }` DSL for non-composable registries (scope, transition, container building), but generate `when`-dispatch directly in IR for composable registries (screen, wrapper).

**Rationale**: The DSL already handles the complex NavNode tree construction correctly. Generating IR calls to the DSL is simpler and more maintainable than replicating all tree-building logic in raw IR. Only the composable registries require direct IR generation due to Compose compiler constraints.

### 2. Annotation Retention

**Decision**: Change most annotations from `SOURCE` to `BINARY` retention.

**Rationale**: Compiler plugins can only read annotations from dependency modules if they survive compilation into `.klib`/`.jar` metadata. This is essential for Phase 4 multi-module discovery.

### 3. KSP Parallel Support

**Decision**: Maintain KSP module during transition, deprecated but functional.

**Rationale**: Users need migration time. The Gradle plugin will support both modes with a configuration flag.

### 4. Multi-Module Discovery

**Decision**: Introduce `@NavigationRoot` annotation + classpath scanning instead of manual `+` chaining.

**Rationale**: Eliminates boilerplate, mirrors Koin's successful migration pattern.

---

## Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|------------|------------|
| K2 compiler plugin API instability | High | Medium | Pin to specific Kotlin version, abstract behind internal interfaces |
| Compose compiler interop issues | High | Medium | Maintain `when`-dispatch pattern (proven in KSP), test extensively |
| Native platform divergence | Medium | Low | Dedicated `compiler-plugin-native` module, automated source sync |
| Multi-module metadata resolution | Medium | Medium | Phase 4 treated as separate concern, fallback to manual `+` always available |
| IDE support varies across Android Studio versions | Medium | Medium | Document minimum IDE versions, test against multiple AS/IJ versions |
| Build time regression during transition (both KSP and plugin) | Low | Medium | Make modes mutually exclusive per module in Gradle config |

---

## Success Criteria

### Phase 1 Complete
- [ ] New modules compile and load into Kotlin compiler
- [ ] Gradle plugin passes `modulePrefix` to compiler plugin
- [ ] `./gradlew build` succeeds with plugin applied (no-op transformations)

### Phase 2 Complete
- [ ] IDE shows synthetic `{Prefix}NavigationConfig` and `{Prefix}DeepLinkHandler` in autocomplete
- [ ] Diagnostic errors appear in real-time for route collisions, argument mismatches
- [ ] No physical files generated

### Phase 3 Complete
- [ ] Single-module `@Stack` + `@Destination` + `@Screen` navigation works end-to-end
- [ ] `@Tabs` and `@Pane` containers build and render correctly
- [ ] `@TabsContainer` and `@PaneContainer` wrappers resolve with KSP-parity key semantics
- [ ] Shared container providers in wrapper composables execute correctly for tab and pane content
- [ ] Deep links resolve with type-safe argument extraction
- [ ] All transitions apply correctly
- [ ] Demo app (`composeApp`) runs on all platforms using compiler plugin

### Phase 4 Complete
- [ ] `@NavigationRoot` auto-discovers configs from dependent feature modules
- [ ] `feature1` + `feature2` modules work without manual `+` chaining
- [ ] Manual `+` operator still works as fallback

### Phase 5 Complete
- [ ] Integration test suite covers all annotation combinations
- [ ] IR verification passes (`-Xverify-ir`)
- [ ] Migration guide published
- [ ] KSP module marked `@Deprecated` with clear migration path

---

## Detailed Phase Plans

Each phase has its own document with:
- Detailed task breakdown with acceptance criteria
- Files to create/modify
- Dependencies between tasks
- Technical approach and design decisions
- Sequencing diagram

See the individual phase documents linked in the Phase Overview table above.
