# Koin Compiler Plugin Pattern Analysis

**Status**: Analysis complete  
**Created**: 7 March 2026  
**Purpose**: Extract reusable compiler-plugin architecture patterns from Koin and identify what Quo-Vadis should and should not copy

---

## Why Koin Is A Useful Reference

Koin solves a similar class of problem: collect annotation-driven definitions at compile time, expose compiler-visible generated artifacts, and populate runtime registries through generated code instead of runtime scanning.

For Quo-Vadis, the most valuable part of Koin is not its exact DI behavior. The value is its phase separation:

- FIR discovers and declares
- IR normalizes and emits executable bodies
- Runtime consumes a small, stable builder API

---

## Koin Architecture Summary

### Phase Split

| Concern | Koin ownership | Notes for Quo-Vadis |
|---------|----------------|---------------------|
| Plugin registration | `KoinPluginComponentRegistrar`, `KoinPluginRegistrar`, `KoinIrExtension` | Clear entry points and phase ordering are worth copying |
| FIR declaration discovery | `KoinModuleFirGenerator` | Predicate-driven discovery is the right model |
| Cross-module metadata visibility | generated hint declarations | Compiler-visible metadata is preferable to source scanning |
| IR collection + normalization | `KoinAnnotationProcessor` | Useful direction, but too monolithic to copy directly |
| IR emission | `KoinHintTransformer`, `KoinDSLTransformer`, `KoinStartTransformer` | Focused emitters are easier to reason about and test |
| Runtime integration | standard Koin `Module`, `InstanceRegistry`, `ScopeRegistry` APIs | Strongest reusable idea: emit against stable runtime APIs |

### What Koin Actually Generates

Koin does **not** generate a separate runtime resolver format. It generates code that feeds Koin's existing runtime infrastructure.

Core generated artifacts:

- FIR-visible declarations for modules and hints
- IR-filled module bodies that call standard Koin builders
- IR rewrites for Koin DSL calls and startup wiring

The runtime still resolves everything through Koin's normal registries and resolvers.

---

## Patterns Quo-Vadis Should Reuse

### 1. Collect, Normalize, Emit

Quo-Vadis should stop treating metadata collection and IR emission as the same responsibility.

Recommended pipeline:

1. Collect raw annotation data in FIR and IR
2. Normalize it into a canonical semantic model
3. Validate the model
4. Emit runtime registries and config bodies from that model

### 2. Target A Small Runtime API Surface

Koin's plugin succeeds because it emits against a small set of runtime builder APIs rather than open-coded runtime internals.

Quo-Vadis should do the same. The compiler plugin should target a narrow runtime contract such as:

- base navigation graph construction
- screen registry entries
- container registry entries
- deep-link entries
- pane-role entries

This keeps IR generation deterministic and makes runtime behavior easier to preserve.

### 3. Use Compiler-Visible Metadata For Cross-Module Discovery

If Quo-Vadis needs later multi-module aggregation, the correct pattern is compiler-visible generated metadata, not ad hoc process state.

### 4. Centralize Names And Contracts

Koin centralizes FQNs and generated names. Quo-Vadis should centralize:

- annotation FQNs
- runtime symbol lookups
- generated declaration names
- container signature contracts
- wrapper key derivation rules

### 5. Split Emitters By Domain

Koin's separate IR passes are easier to test than a single all-knowing generator. Quo-Vadis should separate emitters for:

- screen registry
- container registry
- deep-link registry
- navigation config wiring

---

## Patterns Quo-Vadis Should Not Reuse

### 1. Cross-Phase Global State

Koin uses a JVM system-property bridge for some cross-phase coordination. Quo-Vadis should avoid this. It is brittle, hard to reason about, and unnecessary if the plugin uses a proper intermediate model.

### 2. Monolithic IR Semantics

Koin's annotation processor carries too many responsibilities at once. Quo-Vadis should not repeat that shape. Navigation graph correctness benefits from a dedicated validation and normalization step.

### 3. Best-Effort Fallbacks For Semantic Failures

Koin can tolerate more runtime dynamism because DI resolution is intentionally flexible. Quo-Vadis should not silently fall back when a graph binding is invalid. If a wrapper cannot be resolved, that should be a compile-time failure or a loud generated error path, not a hidden passthrough.

### 4. Heuristic Matching As A Primary Contract

Quo-Vadis should avoid package-name or loose signature heuristics as a source of truth for container binding. It should bind wrappers through normalized symbols and explicit keys.

---

## Direct Mapping To Quo-Vadis

| Koin pattern | Quo-Vadis equivalent |
|--------------|----------------------|
| FIR-discovered module contributors | FIR/IR-discovered destinations, tabs, panes, wrappers |
| Generated hints visible to downstream compilations | generated navigation contributor metadata for future multi-module discovery |
| Standard runtime `Module` API | stable `NavigationConfig` and registry-builder/runtime contracts |
| IR transformers per responsibility | dedicated emitters for config, screen registry, container registry, deep links |
| Runtime owns resolution | runtime continues to own rendering and navigation, plugin only supplies deterministic registries |

---

## Recommendation For Quo-Vadis

The Koin-inspired design Quo-Vadis should adopt is:

1. Keep FIR focused on declaration availability and diagnostics.
2. Build a canonical compiler-side navigation model before emitting IR.
3. Generate explicit registry implementations from that model.
4. Preserve existing runtime contracts where they are already correct.
5. Defer broader runtime redesign unless the compiler plugin cannot express a stable contract without it.

For the current reported failures, this means the compiler plugin should be refactored around a stronger container binding model rather than patching runtime behavior.