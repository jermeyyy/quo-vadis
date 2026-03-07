# Compiler Plugin Current-State Comparison

**Status**: Analysis complete  
**Created**: 7 March 2026  
**Purpose**: Compare the working KSP contract, the current compiler-plugin implementation, and the Koin-inspired target architecture

---

## Validated Problem Statement

The current compiler plugin fails to preserve the previously working KSP contract for tab container wrappers.

Observed symptoms:

- `@TabsContainer` wrappers are not reliably invoked for `TabNode` rendering
- generic content rendering is used instead
- `DemoTabsStore` is never provided because the wrapper does not run
- Demo tab screens then crash with `IllegalStateException: No DemoTabsStore provided`

Runtime is not the primary fault domain. The KSP path previously exercised the same runtime successfully.

---

## Working KSP Contract

The KSP path had a coherent contract across config generation, rendering, and FlowMVI integration.

### Container Binding Contract

1. Generated base config built `TabNode` instances with `scopeKey` and `wrapperKey` aligned.
2. Generated `containerRegistry` delegated structural container info to the base config.
3. The same registry implemented composable wrapper dispatch through a deterministic `when` on tab scope keys.
4. `TabRenderer` called `containerRegistry.TabsContainer(node.wrapperKey ?: node.key.value, ...)`.
5. The selected wrapper ran under `LocalContainerNode`, so `rememberSharedContainer` could resolve a shared container scope.

That contract is what made `DemoTabsWrapper` and `DemoTabsStore` work.

---

## Current Compiler-Plugin Behavior

### What Is Working

- FIR and IR metadata collection for tabs and wrappers exists.
- `NavigationConfig` synthesis delegates structural graph creation to the base config.
- Container registry generation attempts to overlay custom wrapper dispatch on top of the base registry.

### What Is Broken

The failure is in the generated container wrapper dispatch path.

Current behavior indicates:

- wrapper metadata is collected
- `hasTabsContainer` can return `true` from metadata-only key checks
- wrapper invocation generation is best-effort
- if the plugin cannot safely resolve or emit the wrapper branch, it silently falls back to `content()`

That fallback causes the generic tab content path to run without the `@TabsContainer` provider wrapper.

---

## Root Cause Assessment

### Primary Root Cause

The compiler plugin treats wrapper dispatch generation as an optional enhancement instead of a required part of the navigation contract.

This shows up in three architectural problems:

1. **Silent fallback**
   Missing or unresolved wrapper branches degrade to generic content instead of failing loudly.

2. **Overloaded key semantics**
   Container display name, scope key, and wrapper lookup key are too tightly coupled and not represented explicitly in metadata.

3. **Fragile IR emission strategy**
   The container registry is emitted as an anonymous object with manual composable ABI handling, making it much easier for branch generation to drift from the screen-registry path.

### Secondary Architectural Problem

Containers do not have the same quality of synthesis model as screens.

Screens already trend toward a dedicated generated implementation. Containers are still handled by a more fragile overlay generator.

---

## Comparison With Koin

| Dimension | KSP path | Current compiler plugin | Koin-inspired target |
|-----------|----------|-------------------------|----------------------|
| Collection | coherent enough for current feature set | collection exists | keep collection, but normalize it |
| Semantic model | implicit in generator logic | partial and leaky | explicit canonical model |
| Validation | mostly implicit | weak for container dispatch | compile-time validation before emission |
| Runtime target | stable existing runtime contract | partly preserved, partly bypassed by fallback | preserve runtime contract, emit deterministically |
| Failure mode | deterministic wrapper dispatch | hidden generic fallback | loud compile failure or guaranteed branch emission |
| Container synthesis | generated `when` dispatch tied to scope keys | anonymous overlay with best-effort wrapper emission | explicit generated registry class or deterministic overlay from normalized bindings |

---

## Refactor Direction

### Preserve

- existing runtime renderer contract
- existing FlowMVI shared-container behavior
- KSP-compatible wrapper-key semantics
- base DSL/navigation graph building where it already works

### Replace

- metadata-only wrapper checks that are disconnected from real dispatch generation
- silent fallback on wrapper resolution failure
- overloaded container identity semantics
- ad hoc anonymous-object container codegen as the semantic center of container behavior

---

## Proposed Compiler-Side Model

The compiler plugin should introduce a canonical container binding model.

Suggested shape:

| Field | Purpose |
|-------|---------|
| `containerKind` | tabs or pane |
| `containerClassId` | class annotated with `@Tabs` or `@Pane` |
| `scopeKey` | logical navigation/container scope identity |
| `wrapperKey` | runtime wrapper dispatch key; defaults to `scopeKey` for parity |
| `wrapperFunctionId` | resolved `@TabsContainer` or `@PaneContainer` function |
| `wrapperSignature` | validated parameter model used by IR emitter |

This model should be produced once, validated once, and then used by both base-config generation and container-registry emission.

---

## Immediate Implications

The reported bugs should be solved by compiler-plugin refactoring, not by runtime workarounds.

Immediate engineering priorities:

1. Reproduce the wrapper-dispatch failure with a compiler integration test.
2. Make wrapper resolution failure explicit.
3. Rebuild container-registry emission around normalized bindings.
4. Reassert KSP parity for wrapper key semantics and shared-container provisioning.