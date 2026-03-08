# Compiler Plugin / KSP Interchangeability Plan

## Overview

This document outlines the work required to make Quo Vadis backend selection switchable through configuration so the compiler plugin can ship as an experimental feature without forcing users onto a different integration model.

The current repository already has a partial switch in the Gradle plugin via `useCompilerPlugin`, but the two backends are not yet interchangeable in the strict sense. They overlap at module-level generated artifacts, but they diverge in root-level aggregation, marker interfaces, Gradle defaults, test coverage, and local development workflow.

## Current State

### KSP responsibilities

- `quo-vadis-ksp` collects `@Stack`, `@Tabs`, `@Pane`, `@Destination`, `@Screen`, wrapper, and transition annotations through extractor classes in `QuoVadisSymbolProcessor`.
- It validates the collected graph through `ValidationEngine`.
- It generates module-prefixed source files in `com.jermey.quo.vadis.generated`.
- The main generated config is `${modulePrefix}NavigationConfig`, produced by `NavigationConfigGenerator`.
- The generated config implements `NavigationConfig`, not `GeneratedNavigationConfig`.
- The generated config delegates most non-composable registrations to a DSL-built `baseConfig`, then supplies custom `screenRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode`, `plus`, and `roots` behavior.
- It also generates `${modulePrefix}DeepLinkHandler`, which implements `DeepLinkRegistry`.
- KSP does not generate an aggregated app-level config, does not support `navigationConfig<T>()`, and does not participate in classpath auto-discovery.

### Compiler plugin responsibilities

- `quo-vadis-compiler-plugin` synthesizes FIR declarations for:
  - `${modulePrefix}NavigationConfig`
  - `${modulePrefix}DeepLinkHandler`
  - `${modulePrefix}ScreenRegistryImpl`
  - `${modulePrefix}__AggregatedConfig`
- `${modulePrefix}NavigationConfig` implements `GeneratedNavigationConfig`.
- `${modulePrefix}__AggregatedConfig` implements `NavigationConfig` and delegates to a composition of discovered module configs.
- IR generators populate bodies for config, deep-link handler, screen registry, container registry, pane role registry, and aggregation.
- `NavigationConfigCallTransformer` rewrites `navigationConfig<T>()` call sites to the generated aggregated config object.
- `MultiModuleDiscovery` scans the current module, dependency descriptors, and classpath roots for `GeneratedNavigationConfig` implementors.

### Gradle/plugin wiring today

- `quo-vadis-gradle-plugin` exposes `modulePrefix`, `useLocalKsp`, and `useCompilerPlugin`.
- `useCompilerPlugin` currently defaults to `true` in the plugin.
- Consumer modules in this repository also default `quoVadis.useCompilerPlugin` to `true` in their build scripts.
- KSP is only wired when `useCompilerPlugin == false`.
- The compiler subplugin always resolves the compiler artifact by Maven coordinates, not by source project dependency.
- `settings.gradle.kts` includes the Gradle plugin as a composite build, but not the compiler plugin artifact itself.

### Consumer integration expectations today

- The demo app currently uses `@NavigationRoot` plus `navigationConfig<AppNavigation>()`.
- That API path is compiler-plugin-specific today because `navigationConfig<T>()` throws when the compiler plugin rewrite is absent.
- Older docs and examples still describe explicit generated config objects and manual `+` composition.

### Test coverage today

- The compiler plugin has substantial compile-testing coverage for config generation, deep links, aggregation, multi-module behavior, and IR verification.
- KSP has extractor-level tests only and lacks comparable end-to-end contract tests for generated output.

## Target State

### Recommended compatibility boundary for the experimental rollout

The experimental rollout should define interchangeability around the **module-level generated contract**, not around every compiler-plugin-only enhancement.

That shared contract should be:

- a module-prefixed generated config object in `com.jermey.quo.vadis.generated`
- a module-prefixed generated deep-link handler in the same package
- matching behavior for `screenRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode`, `plus`, and `roots`
- identical module prefix naming rules
- explicit manual composition via `+` remaining valid in both backends

### Compiler-plugin-only additions during the experimental phase

These features should remain explicitly scoped as compiler-plugin enhancements unless the team decides to fund parity work:

- `@NavigationRoot`
- `${modulePrefix}__AggregatedConfig`
- `navigationConfig<T>()` call-site rewrite and automatic aggregation
- classpath discovery based on `GeneratedNavigationConfig`

### Why this boundary is recommended

- It allows a property-only backend switch without requiring a cross-platform redesign of `navigationConfig<T>()` for KSP.
- It matches the overlap that already exists between the two codegen paths.
- It prevents the experimental compiler launch from being blocked on full KSP parity for a feature KSP never had.
- It gives the project a clean later milestone: retire KSP, then promote compiler-plugin-only aggregation to the default experience.

## Gap Analysis

### 1. Rollout posture is inverted

Current code already defaults to compiler plugin mode, while the requested rollout posture is “experimental feature”.

Impact:

- The default is too aggressive for a feature that still has parity and workflow gaps.
- Documentation and consumer build scripts currently normalize compiler mode as the baseline.

Required fix:

- Move experimental rollout to opt-in compiler mode with KSP as the release default until parity goals are met.

### 2. Public integration model is not backend-neutral

Current divergence:

- KSP produces module configs and expects explicit composition.
- Compiler plugin supports module configs plus root aggregation through `@NavigationRoot` and `navigationConfig<T>()`.
- The demo app already consumes the compiler-plugin-only path.

Impact:

- A property flip from compiler to KSP is not safe for current app code.
- “Interchangeable backend” is currently false for any consumer using the new root API.

Required fix:

- Decide that the shared experimental contract is explicit generated config objects, not `navigationConfig<T>()`.
- Update samples and tests used for backend switching to consume the shared contract.
- Keep `navigationConfig<T>()` documented as compiler-plugin-only until KSP is removed or parity is explicitly designed.

### 3. Marker interface mismatch blocks shared discovery semantics

Current divergence:

- Compiler plugin module configs implement `GeneratedNavigationConfig`.
- KSP configs implement only `NavigationConfig`.

Impact:

- Shared discovery or aggregation logic cannot be expressed consistently.
- The `GeneratedNavigationConfig` name and docs are compiler-plugin-specific, which is accurate today but incompatible with deeper interchangeability later.

Required fix:

- For phase 1, treat this as acceptable because discovery is outside the interchangeability boundary.
- For phase 2, decide whether to:
  - broaden `GeneratedNavigationConfig` into a backend-neutral generated marker, or
  - introduce a new backend-neutral marker and migrate discovery to that type.

### 4. Duplicate-generation guardrails are incomplete

Current divergence:

- The migration guide says the Gradle plugin errors when both backends are active.
- The actual Gradle plugin does not enforce that in compiler mode.
- Consumer modules still apply the KSP plugin unconditionally.

Impact:

- Users can end up with misleading “both backends active” states.
- Manual KSP dependencies or stale source-dir wiring can reintroduce duplicate classes.

Required fix:

- Make backend validation explicit in the Gradle plugin.
- In compiler mode, detect and fail on Quo Vadis KSP processor dependencies or any Quo Vadis-specific KSP wiring.
- Decide whether inert application of the bare KSP plugin is allowed during the experiment to support property-only switching.

### 5. Property model is too narrow and too scattered

Current divergence:

- The repo uses a Boolean `useCompilerPlugin`.
- The property is repeated in each consumer module.
- The model cannot express backend policy, experimental posture, or compiler artifact resolution cleanly.

Impact:

- Harder to extend.
- Harder to detect mixed-module misconfiguration.
- Harder to document a stable rollout story.

Required fix:

- Replace the Boolean as the primary public switch with a backend enum property.
- Resolve the property centrally and enforce one backend across all Quo Vadis modules in a build.

### 6. Local compiler-plugin development is fragile

Current divergence:

- The Gradle plugin itself is sourced from the local composite build.
- The compiler subplugin artifact is still loaded by Maven coordinates.
- Local source changes to `quo-vadis-compiler-plugin` are ignored until published to `mavenLocal`.

Impact:

- Toggling the experimental backend locally can silently use stale plugin binaries.
- Developers may debug the wrong version.

Required fix:

- Either formalize `mavenLocal` publication as the supported local-dev workflow for the experiment, or
- add a dev-only resolution mode for compiler plugin artifacts.

### 7. Test strategy is not symmetric

Current divergence:

- Compiler plugin behavior is heavily tested.
- KSP output behavior is not verified through the same contract suite.

Impact:

- There is no authoritative parity signal when switching the backend.

Required fix:

- Introduce shared backend contract tests that run against both KSP and compiler plugin modes.

### 8. Documentation and metadata drift

Current divergence:

- Docs still contain KSP-era examples, compiler-plugin-only examples, and mixed messaging about defaults.
- Some comments still describe generated artifacts as `GeneratedNavigationConfig` even where code now uses module-prefixed names.
- The Gradle plugin metadata still describes the plugin as KSP-focused.

Impact:

- Users will not understand what is safe to toggle by property.

Required fix:

- Document the experimental contract explicitly and separate shared behavior from compiler-only extras.

## Recommended Configuration Model

### Primary recommendation

Use a backend enum property as the public switch:

```kotlin
quoVadis {
    backend = providers.gradleProperty("quoVadis.backend")
        .map(QuoVadisBackend::valueOfNormalized)
        .getOrElse(QuoVadisBackend.KSP)
}
```

Recommended values:

- `ksp`
- `compiler`

### Backward compatibility

- Keep `useCompilerPlugin` as a deprecated compatibility alias for one transition window.
- Map `true -> compiler` and `false -> ksp`.
- If both `backend` and `useCompilerPlugin` are set, `backend` wins and the plugin should warn.

### Backend policy rules

- The backend property must be set once at the root build level, typically via `gradle.properties`.
- Mixed backend selection across Quo Vadis modules should be treated as unsupported during the experiment.
- The Gradle plugin should fail fast when it detects mixed mode or incompatible manual wiring.

### KSP plugin handling during the experiment

To preserve a **property-only switch**, the least disruptive rule is:

- allow the bare KSP Gradle plugin to remain applied in consumer modules during the experiment
- in compiler mode, ensure the Quo Vadis KSP processor dependency is not added
- in compiler mode, fail if the build already contains a manual dependency on `quo-vadis-ksp` or Quo Vadis-specific KSP source-dir/task wiring
- after the experiment, simplify the public guidance to remove the KSP plugin entirely

### Separate local-development control

Backend selection and artifact resolution should not be conflated.

Recommended additional dev-only property:

- `quoVadis.compilerPluginResolution=published|mavenLocal|project`

If implementing `project` resolution is too expensive now, keep only:

- `published`
- `mavenLocal`

and document that local compiler-plugin changes require republishing.

## Task Breakdown

### Phase 0: Decision checkpoint

Goal: lock the compatibility boundary before implementation.

Tasks:

1. Confirm that experimental interchangeability is defined around module-level generated objects, not `navigationConfig<T>()`.
2. Confirm that `@NavigationRoot` remains compiler-plugin-only during the experimental phase.
3. Confirm that compiler mode is opt-in until parity and workflow gaps are closed.

### Phase 1: Gradle property and plugin model

Dependencies: Phase 0

Tasks:

1. Add a typed backend enum to `QuoVadisExtension`.
2. Deprecate `useCompilerPlugin` in favor of the enum.
3. Update `QuoVadisPlugin` to branch on backend enum, not Boolean semantics.
4. Add validation for unsupported mixed/manual wiring.
5. Decide and implement the dev-only compiler artifact resolution mode.
6. Update plugin metadata and messages to describe compiler backend as experimental rather than default.

### Phase 2: Shared generated contract alignment

Dependencies: Phase 1

Tasks:

1. Define the exact shared contract document for both backends:
   - generated package
   - config object naming
   - deep-link handler naming
   - required behavior of registries and helper members
2. Verify compiler plugin output matches that contract for module-level artifacts.
3. Verify KSP output matches that contract for module-level artifacts.
4. Fix any naming, package, or behavioral mismatches that break explicit object consumption.
5. Decide whether `GeneratedNavigationConfig` remains compiler-only or becomes backend-neutral in a later phase.

### Phase 3: Sample and integration cleanup

Dependencies: Phase 2

Tasks:

1. Update `composeApp` and any rollout-facing samples to consume the shared contract when demonstrating backend switching.
2. Isolate compiler-plugin-only `@NavigationRoot` examples so they are not presented as interchangeable with KSP.
3. Ensure feature modules still build correctly with the same property flip.

### Phase 4: Test matrix and parity validation

Dependencies: Phase 2

Tasks:

1. Create a shared codegen contract test suite that can run against both backends.
2. Cover at minimum:
   - stacks
   - tabs with flat and stack-backed items
   - panes
   - screen bindings
   - transitions
   - deep links with path and query args
   - `plus` composition
   - `roots`
3. Add Gradle functional tests for backend selection and plugin wiring.
4. Add backend-flip tests for `clean -> build` and `backend flip -> clean -> build`.
5. Add local-dev smoke tests for compiler artifact resolution if a new resolution mode is introduced.

### Phase 5: Documentation and rollout

Dependencies: Phases 1 through 4

Tasks:

1. Update migration docs to distinguish:
   - backend-interchangeable APIs
   - compiler-plugin-only APIs
2. Update README and demo references to reflect the experimental status and the root property.
3. Document supported local workflows for experimental compiler development.
4. Document duplicate-generation failure modes and recovery steps.
5. Define exit criteria for flipping the default backend from KSP to compiler.

## Recommended Sequencing

1. Make the rollout decision explicit.
2. Introduce the backend enum and validation guardrails.
3. Stabilize the shared module-level generated contract.
4. Move samples and docs back onto the shared contract for backend-switch scenarios.
5. Build the parity test suite.
6. Launch compiler backend as opt-in experimental.
7. Revisit root aggregation parity only after the module-level swap is stable.

## Risks and Mitigations

### Risk: duplicate generation or duplicate classes

Why it matters:

- Both backends generate the same package and similarly named artifacts.

Mitigation:

- Fail fast on incompatible simultaneous Quo Vadis KSP wiring and compiler mode.
- Require `clean` on backend flips in docs and CI.
- Add Gradle functional tests that exercise backend transitions.

### Risk: users interpret `navigationConfig<T>()` as backend-neutral

Why it matters:

- That API is compiler-plugin-only today.

Mitigation:

- Keep it documented as compiler-plugin-only during the experimental window.
- Do not use it in backend-switching samples.

### Risk: mixed backend mode across modules

Why it matters:

- Aggregation, naming assumptions, and testing all become ambiguous.

Mitigation:

- Require one backend for all Quo Vadis modules in a build.
- Validate centrally from the Gradle plugin.

### Risk: binary/source compatibility drift between generated artifacts

Why it matters:

- Even small differences in supertypes or helper members can break consumer code when switching backends.

Mitigation:

- Define and test a shared artifact contract.
- Add explicit reflection-based or compile-based assertions for package, object names, supertypes, and public members.

### Risk: stale compiler plugin artifact in local development

Why it matters:

- Developers can unknowingly test an old compiler plugin binary.

Mitigation:

- Provide an explicit dev resolution mode or document `mavenLocal` publication as mandatory.
- Add warnings when local source is present but the plugin still resolves from published coordinates.

### Risk: Kotlin compiler API churn

Why it matters:

- The compiler plugin relies on experimental FIR/IR APIs.

Mitigation:

- Keep KSP as the default during the experiment.
- Retain a rollback path until the compiler backend is proven across supported Kotlin versions.

## Validation Strategy

### Build and functional validation

Run a backend matrix against representative modules:

- `composeApp` in KSP mode
- `composeApp` in compiler mode
- `feature1` and `feature2` in both modes
- clean builds after flipping the backend property

### Contract validation

For both backends, assert:

- generated config object exists in `com.jermey.quo.vadis.generated`
- generated deep-link handler exists with the expected module-prefixed name
- `NavigationConfig` behavior is equivalent for `buildNavNode`, registries, `plus`, and `roots`
- deep-link resolution and URI creation are equivalent

### Multi-module validation

During the experiment, validate two separate scenarios:

- backend-neutral scenario: manual `+` composition works identically in both modes
- compiler-plugin-only scenario: `@NavigationRoot` plus `navigationConfig<T>()` works in compiler mode only

### Workflow validation

Validate:

- fresh clone experience
- clean backend flip
- local compiler-plugin change plus republish flow
- failure messaging when incompatible backend wiring is present

## Explicit Open Questions

1. Is true source-level interchangeability for `navigationConfig<T>()` a real requirement for this milestone, or is shared explicit generated-object consumption sufficient for the experimental launch?
2. Should `GeneratedNavigationConfig` remain compiler-only, or should the project introduce a backend-neutral marker in a follow-up phase?
3. Is a dev-only `project` resolution mode for the compiler subplugin required now, or is a documented `mavenLocal` workflow acceptable for the experiment?
4. Should the Gradle plugin tolerate the bare KSP plugin being applied in compiler mode to preserve a property-only switch, or should it fail on any KSP plugin presence at all?
5. What exact exit criteria will justify flipping the default backend from KSP to compiler?

## Recommendation

Proceed with an **opt-in compiler backend experiment** built around a new backend enum property and a clearly documented shared contract of module-level generated artifacts.

Do **not** define the initial interchangeability promise around `@NavigationRoot` or `navigationConfig<T>()`. Those are compiler-plugin enhancements today, and forcing KSP parity there would turn a contained rollout into a larger cross-platform API redesign.

Once the backend switch is validated through shared contract tests, Gradle guardrails, and a documented local workflow, the project can safely run the compiler plugin as an experimental feature while keeping KSP as the default fallback.