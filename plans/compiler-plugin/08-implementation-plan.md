# Implementation Plan: Compiler Plugin Container Registry Refactor

**Status**: Ready for implementation  
**Created**: 7 March 2026  
**Scope**: Fix compiler-plugin-generated IR for container wrappers and refactor the plugin toward a stronger, Koin-inspired architecture while keeping runtime changes minimal

---

## Overview

This plan focuses on the compiler plugin, not a broad runtime redesign. The runtime contract that worked with KSP should remain the default target.

Primary outcomes:

- `@TabsContainer` wrappers are deterministically used for tab nodes
- `DemoTabsWrapper` runs, so `DemoTabsStore` is provided correctly
- container registry generation becomes explicit, testable, and symmetric with the rest of the plugin
- future IR work follows a collect -> normalize -> validate -> emit structure

---

## Validated Requirements

- Fix current compiler-plugin IR generation for tabs container wrappers.
- Preserve behavior previously delivered by KSP-generated code.
- Keep runtime changes minimal because runtime behavior was already correct.
- Use Koin only as an architectural pattern reference, not as a design to clone literally.
- Store analysis and plan artifacts under `plans/compiler-plugin`.

---

## Technical Direction

### Architectural Decision 1: Introduce A Canonical Compiler Model

The plugin should stop emitting container logic directly from loosely coupled raw metadata. Introduce an intermediate semantic model for the entire module, with a dedicated normalized container binding subset.

Recommended compiler-side model layers:

1. `RawNavigationMetadata`
   Existing collected annotation facts.
2. `NormalizedNavigationModel`
   Canonical graph, destination, transition, deep-link, and container bindings.
3. `ValidatedNavigationModel`
   Same model after semantic checks pass.
4. IR emitters
   Screen registry, container registry, deep-link registry, and config wiring generated from validated inputs.

### Architectural Decision 2: Preserve The Existing Runtime Contract

Do not redesign `TabRenderer`, `ContainerRegistry`, or FlowMVI shared-container behavior as part of this fix. The compiler plugin should generate code that matches the established KSP/runtime contract.

### Architectural Decision 3: Remove Silent Fallback For Broken Wrapper Bindings

If the plugin cannot resolve an annotated wrapper function or cannot map it to a valid container binding, that must surface as a compiler diagnostic or a hard generation failure. Hidden passthrough behavior is not acceptable for container wrappers.

### Architectural Decision 4: Make Container Synthesis Symmetric With Screen Synthesis

Container registry generation should be structured like a first-class generated component, not as a fragile anonymous overlay that also acts as the semantic source of truth.

---

## Work Breakdown

### Phase 1: Reproduction And Guard Rails

#### Task 1.1: Add failing integration coverage

- **Description:** Add compiler-plugin integration tests that compile a real `@TabsContainer` wrapper and verify wrapper invocation behavior.
- **Files:** `quo-vadis-compiler-plugin/src/test/kotlin/.../IrCodegenTests.kt`, test fixtures under `.../testing/TestSources.kt`, golden files if needed.
- **Acceptance Criteria:**
  - a test proves the generated `TabsContainer` path invokes the custom wrapper
  - a test reproduces the current failure for the demo-like shared-container case before the fix

#### Task 1.2: Add failure visibility

- **Description:** Remove silent generation-time fallback for unresolved wrapper functions. Emit a compiler diagnostic or fail generation with actionable context.
- **Files:** container registry generator, FIR diagnostics if needed.
- **Acceptance Criteria:**
  - wrapper resolution failures are explicit
  - generic passthrough is only used when no wrapper is declared, not when one is declared but generation fails

### Phase 2: Canonical Model And Key Normalization

#### Task 2.1: Introduce normalized container binding model

- **Description:** Add normalized types for tabs/pane bindings with explicit `scopeKey`, `wrapperKey`, `wrapperFunctionId`, and signature information.
- **Files:** `quo-vadis-compiler-plugin/src/main/kotlin/.../common/NavigationMetadata.kt` or a new normalization package.
- **Acceptance Criteria:**
  - binding model separates display name, scope identity, and wrapper dispatch identity
  - both base config generation and container registry emission consume the same binding model

#### Task 2.2: Rebuild wrapper-key derivation for KSP parity

- **Description:** Recreate the old KSP rule that maps wrappers to the container scope key, including companion normalization where needed.
- **Files:** normalization layer and container registry generator.
- **Acceptance Criteria:**
  - `wrapperKey == scopeKey` for tabs/panes unless an explicit future contract says otherwise
  - generated config and generated dispatch use the same derived key

### Phase 3: Container Registry Refactor

#### Task 3.1: Extract container binding validation

- **Description:** Validate wrapper signatures and container-target mapping before IR emission.
- **Files:** new validator package or FIR/IR shared validation helpers.
- **Acceptance Criteria:**
  - invalid wrapper signatures fail before dispatch generation
  - no best-effort branch skipping remains in the emitter

#### Task 3.2: Refactor container registry emission into dedicated component

- **Description:** Replace the current anonymous-object-heavy generation flow with a dedicated emitter that consumes validated bindings and produces deterministic registry methods.
- **Files:** `ContainerRegistryIrGenerator.kt`, potentially new helper/emitter classes.
- **Acceptance Criteria:**
  - `hasTabsContainer` and `TabsContainer` derive from the same validated binding set
  - `hasPaneContainer` and `PaneContainer` derive from the same validated binding set
  - there is a single source of truth for dispatch branches

#### Task 3.3: Keep base-config delegation limited to structure

- **Description:** Continue delegating structural container info to the base config, but keep wrapper dispatch generation explicit and deterministic.
- **Files:** `BaseConfigIrGenerator.kt`, `NavigationConfigIrGenerator.kt`, container registry emitter.
- **Acceptance Criteria:**
  - container structure continues to come from the base config
  - wrapper rendering no longer depends on metadata-only checks disconnected from actual generated branches

### Phase 4: Plugin Architecture Cleanup

#### Task 4.1: Introduce collect -> normalize -> emit orchestration

- **Description:** Make the IR entry point explicitly stage metadata collection, normalization, validation, and emission.
- **Files:** `QuoVadisIrGenerationExtension.kt`, `IrMetadataCollector.kt`, new normalization/validation packages.
- **Acceptance Criteria:**
  - emitters no longer pull ad hoc semantics directly from raw metadata
  - normalization and validation are individually testable

#### Task 4.2: Align screen and container generation patterns

- **Description:** Document and, where practical, enforce symmetry between screen registry and container registry generation.
- **Files:** IR generator package and tests.
- **Acceptance Criteria:**
  - registry generators follow similar conventions for symbol resolution, branch generation, and diagnostics
  - new container fixes do not introduce a one-off architectural island

### Phase 5: Hardening And Regression Coverage

#### Task 5.1: Demo parity validation

- **Description:** Validate the `composeApp` tabs demo against the compiler plugin after refactor.
- **Files:** test coverage plus manual verification notes if needed.
- **Acceptance Criteria:**
  - Demo tabs render through the custom wrapper
  - `DemoTabsStore` is available in child tab screens
  - no regression in pane wrappers or generic tabs behavior

#### Task 5.2: Golden and IR verification coverage

- **Description:** Expand textual IR/golden coverage around wrapper dispatch.
- **Files:** IR golden fixtures and tests.
- **Acceptance Criteria:**
  - generated dispatch branches are visible in golden outputs
  - `-Xverify-ir` stays clean

---

## Suggested File-Level Refactor Boundaries

Primary implementation area:

- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/IrMetadataCollector.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/common/NavigationMetadata.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/ContainerRegistryIrGenerator.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/BaseConfigIrGenerator.kt`
- `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/ir/IrCodegenTests.kt`
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/TestSources.kt`

Potential minimal runtime touchpoints only if required:

- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ContainerRegistry.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/TabRenderer.kt`

Runtime changes should be avoided unless the compiler plugin cannot express the required contract cleanly.

---

## Risks And Mitigations

| Risk | Mitigation |
|------|------------|
| Compose ABI details make container wrapper emission brittle | validate wrapper signatures before emission and keep dispatch generation simple |
| key derivation changes break existing runtime resolution | lock KSP parity with explicit tests before refactor expansion |
| refactor scope spreads into runtime redesign | keep runtime changes behind explicit justification and separate tasks |
| container fix becomes another one-off patch | require normalized model and staged emitter pipeline before closing the work |

---

## Recommended Implementation Sequence

1. Add failing wrapper-dispatch tests.
2. Remove silent fallback and make the current failure explicit.
3. Introduce normalized container bindings and KSP-parity key derivation.
4. Rebuild container-registry emission from validated bindings.
5. Align IR orchestration around collect -> normalize -> validate -> emit.
6. Validate demo behavior and harden golden coverage.

This sequence fixes the user-visible crash early while still steering the plugin toward a maintainable architecture.