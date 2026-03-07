# Implementation Plan: Compiler Plugin Deep-Link Parity

## Overview

Restore full deep-link parity between the Kotlin compiler plugin and the legacy KSP pipeline.
The fix should preserve the current runtime contract in `quo-vadis-core` and close the gap inside
compiler-plugin metadata collection and IR generation.

## Validated Requirements

- Goal: compiler-plugin users should get the same deep-link behavior as KSP users.
- Scope: targeted parity fix in the compiler plugin, not a broader shared-generation refactor.
- Non-goal: redesign runtime deep-link APIs or platform entry-point handling.

## Problem Statement

Current evidence shows that the compiler plugin generates and wires a `{modulePrefix}DeepLinkHandler`,
but the generated handler is built from an incomplete metadata model.

Observed gaps:

- Routable destinations are currently sourced from `metadata.stacks.flatMap { it.destinations }` only.
- Compiler-plugin metadata tracks route and argument details for stack destinations, but not for flat
  tab items, pane items, or standalone `@Destination` classes.
- Parameterized deep-link resolution in the compiler plugin binds constructor arguments from regex path
  captures only, while KSP resolves from `queryParams + pathParams`.

## Technical Approach

Implement parity in three layers:

1. Expand compiler-plugin metadata so all routable destinations are represented uniformly.
2. Update deep-link IR generation to consume the complete routable destination set.
3. Add regression tests that cover destination kinds and argument-resolution behavior that currently drift
   from KSP.

## Tasks

### Phase 1: Establish the Parity Contract

#### Task 1.1: Freeze expected behavior from KSP
- **Description:** Treat the current KSP-generated deep-link behavior as the compatibility contract for the
  compiler plugin.
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/DeepLinkRegistry.kt`
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLinkRegistryTest.kt`
- **Acceptance Criteria:**
  - Documented checklist exists for route eligibility, `resolve`, `handle`, `createUri`, `canHandle`, and
    `getRegisteredPatterns` parity.
  - Query-plus-path parameter merge semantics are explicitly captured.

#### Task 1.2: Identify unsupported compiler-plugin destination categories
- **Description:** Enumerate destination shapes that KSP includes in deep-link generation but compiler-plugin
  metadata currently misses.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/common/NavigationMetadata.kt`
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/IrMetadataCollector.kt`
- **Acceptance Criteria:**
  - The plan distinguishes stack destinations, flat tab items, pane items, and standalone destinations.
  - Any exclusions are deliberate and backed by tests or validation rules.

### Phase 2: Expand Compiler Metadata

#### Task 2.1: Introduce a complete routable-destination model
- **Description:** Refactor compiler-plugin metadata so deep-link generation does not depend on stack-only
  destination storage.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/common/NavigationMetadata.kt`
- **Implementation Notes:**
  - Add a top-level collection or equivalent normalized representation for all routable destinations.
  - Preserve existing metadata consumers for stacks, tabs, panes, screens, and transitions.
- **Acceptance Criteria:**
  - Route, argument, and transition metadata for deep-linkable destinations are available without traversing
    stack-specific structures.
  - Existing non-deep-link generation paths remain source-compatible or are updated in the same change.

#### Task 2.2: Collect routes and arguments for all destination shapes
- **Description:** Extend metadata collection beyond `@Stack` sealed subclasses.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/IrMetadataCollector.kt`
- **Implementation Notes:**
  - Keep `processStack` behavior for sealed stacks.
  - Add collection for flat `@TabItem` destinations.
  - Add collection for `@PaneItem` destinations.
  - Add collection for standalone `@Destination` declarations that participate in generated config.
  - Reuse existing argument extraction and transition extraction helpers where possible.
- **Acceptance Criteria:**
  - Metadata for every routable destination kind includes class ID, route, arguments, and transition type.
  - Duplicate collection is avoided or deduplicated deterministically.

### Phase 3: Fix Deep-Link IR Generation

#### Task 3.1: Generate handlers from the complete destination set
- **Description:** Replace stack-only deep-link generation inputs with the normalized routable destination set.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`
- **Acceptance Criteria:**
  - `routableDestinations` includes all supported destination kinds.
  - `getRegisteredPatterns`, `canHandle`, `resolve`, and `createUri` operate on the same unified set.

#### Task 3.2: Align argument binding with KSP semantics
- **Description:** Update generated `resolve(deepLink)` logic so constructor arguments can be sourced from query
  parameters in addition to path placeholders.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/DeepLinkHandlerIrGenerator.kt`
- **Implementation Notes:**
  - Match KSP semantics of `deepLink.queryParams + pathParams` with path params taking precedence.
  - Preserve current typed conversion behavior for primitives, enums, nullable values, and unsupported cases.
  - Ensure constructor parameters without route placeholders can still be resolved when backed by `@Argument`.
- **Acceptance Criteria:**
  - Deep links with query-backed arguments resolve identically between KSP and compiler plugin.
  - Existing path-parameter deep links continue to work.

#### Task 3.3: Re-verify config exposure and aggregation
- **Description:** Confirm that the normalized handler still flows through generated config and aggregated config
  without behavior changes.
- **Files:**
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/NavigationConfigIrGenerator.kt`
  - `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/ir/generators/AggregatedConfigIrGenerator.kt`
- **Acceptance Criteria:**
  - `deepLinkRegistry` still returns the generated handler.
  - Multi-module aggregation behavior is unchanged apart from fixed destination coverage.

### Phase 4: Add Regression Coverage

#### Task 4.1: Extend compiler-plugin codegen tests
- **Description:** Add focused tests that fail on the current implementation and pass after the fix.
- **Files:**
  - `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/ir/IrCodegenTests.kt`
  - `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/TestSources.kt`
- **Test Matrix:**
  - Flat tab-item route resolution
  - Pane-item route resolution
  - Standalone `@Destination` route resolution
  - Query-backed `@Argument` resolution
  - `createUri` parity for non-stack routable destinations
- **Acceptance Criteria:**
  - New tests reproduce the current bug before the fix.
  - Tests validate actual runtime behavior of generated registries, not just symbol presence.

#### Task 4.2: Add or update parity assertions where gaps are documented
- **Description:** Tighten existing comments and placeholder tests into executable assertions.
- **Files:**
  - `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/ir/IrCodegenTests.kt`
- **Acceptance Criteria:**
  - No deep-link parity claims remain untested in compiler-plugin codegen coverage.

### Phase 5: Documentation and Migration Accuracy

#### Task 5.1: Update migration and compiler-plugin docs
- **Description:** Make compiler-plugin deep-link support claims accurate once the fix lands.
- **Files:**
  - `docs/MIGRATION.md`
  - `README.md`
  - Any compiler-plugin-specific docs that claim feature parity
- **Acceptance Criteria:**
  - Docs describe deep-link parity as supported only when verified by tests.
  - Any temporary caveats are removed once released.

## Sequencing

1. Lock the KSP parity contract and enumerate unsupported destination kinds.
2. Refactor metadata so all routable destinations are available uniformly.
3. Update deep-link IR generation to consume the new metadata and merge query/path params.
4. Add regression tests for each destination category and argument-resolution path.
5. Validate config aggregation behavior.
6. Update docs after tests pass.

## Risks and Mitigations

- **Risk:** Metadata refactor breaks unrelated compiler-plugin generation paths.
  **Mitigation:** Keep the new routable-destination model additive where possible, and verify existing stack,
  tab, and pane generation tests before merging.

- **Risk:** Query-parameter support introduces behavior that KSP validation does not currently allow in all
  cases.
  **Mitigation:** Use current KSP-generated behavior and validation rules as the source of truth; add tests for
  supported cases only.

- **Risk:** Destinations may be collected multiple times through stack/tab/pane traversal.
  **Mitigation:** Normalize on `ClassId` and deduplicate deterministically before IR generation.

- **Risk:** Aggregated configs change deep-link precedence unexpectedly.
  **Mitigation:** Preserve existing config wiring and add at least one multi-module deep-link regression test if
  current coverage is weak.

## Open Questions

- Should standalone `@Destination` deep-link support include every generated destination, or only those already
  surfaced through navigation config generation today?
- If KSP and compiler plugin currently disagree on edge-case query-parameter validation, should parity follow
  shipped KSP behavior exactly or the documented intent?

## Recommended Execution Order

1. Add failing tests for flat tab, pane, standalone, and query-argument deep links.
2. Refactor `NavigationMetadata` and `IrMetadataCollector` to expose complete routable destination metadata.
3. Update `DeepLinkHandlerIrGenerator` to use unified metadata and merged parameter sources.
4. Re-run compiler-plugin and relevant core tests.
5. Update migration and parity docs.