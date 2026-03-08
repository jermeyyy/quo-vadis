# Compiler Plugin Audit: quo-vadis-core and composeApp

Date: 8 March 2026
Scope: compare `compiler-plugin` against `main`, limited to `quo-vadis-core` and `composeApp` with other files inspected only for causality.

## Executive Summary

The branch introduces a mix of:

- necessary compiler-plugin enablement in `quo-vadis-core` and `composeApp`
- broader runtime behavior changes in `quo-vadis-core`
- a full demo bootstrap migration that makes `composeApp` depend on compiler-plugin aggregation by default

The additive API work in `quo-vadis-core` is mostly reasonable for experimental compiler-plugin support. The main concern is not the added APIs themselves, but that two runtime behavior changes escaped into core while the branch was still chasing compiler-plugin parity:

1. `TabRenderer` now prefers `scopeKey` over `wrapperKey` for tab wrapper lookup.
2. Pane back-navigation behavior was materially changed and covered with a new dedicated test suite.

Those are broader than “compiler plugin support” and should be treated as runtime changes, not just tooling work.

The highest shipping risk is in `composeApp`: the demo no longer composes configs manually and now defaults to compiler-plugin mode. That is a large blast-radius change for an experimental backend, especially because branch artifacts and plans on this branch still document recent parity issues around wrapper dispatch and deep links.

Bottom line:

- `quo-vadis-core` contains mostly additive support work plus two runtime changes that deserve isolation.
- `composeApp` was converted from a demo of the stable path into a demo of the experimental path.
- For an experimental compiler-plugin release, keeping the demo and runtime changes narrower would reduce migration and rollback risk.

## Changed Areas

### quo-vadis-core

1. New compiler-plugin-facing API surface in `navigation.config`
   - Added `navigationConfig<T>()` placeholder API in `NavigationConfig.kt`.
   - Added `GeneratedNavigationConfig` marker interface.
   - Intent: enable compiler-plugin-generated aggregated config discovery and call-site rewriting.

2. New compiler-plugin-facing builder hooks in `dsl`
   - `NavigationConfigBuilder` gained `registerTabsContainer`, `registerStackContainer`, `registerScope`, `registerTransition`, and `registerPaneContainer`.
   - These are marked `@InternalQuoVadisApi` and are clearly intended as stable IR generation targets.

3. Runtime tab wrapper lookup behavior changed
   - `TabRenderer` now resolves wrapper lookup keys via `scopeKey ?: wrapperKey ?: nodeKey` instead of the older wrapper-key-first behavior.
   - A new `TabRendererLookupTest` locks that behavior in.

4. Runtime pane back-navigation behavior changed
   - `BackOperations` and `PaneOperations` changed compact/expanded pane back handling and pane removal behavior.
   - A large new `PaneBackNavigationTest` suite and updates to `TreeMutatorPaneTest` were added.

5. Small public API additions to registry interfaces
   - `PaneRoleRegistry` gained a `KClass` overload.
   - `ScreenRegistry` gained `ScreenRegistry.Empty`.

6. Minor DSL adjustment in `PanesBuilder`
   - Small alias/wording alignment around secondary/tertiary pane semantics.

### composeApp

1. Demo bootstrap migrated to compiler-plugin aggregation
   - `DI.kt` removed manual `ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig` composition.
   - Added `@NavigationRoot object AppNavigation`.
   - `Navigator` now resolves config via `navigationConfig<AppNavigation>()`.

2. Demo build now defaults to compiler-plugin mode
   - `composeApp/build.gradle.kts` switched from local-KSP development config to `useCompilerPlugin = ... getOrElse(true)`.

3. Demo destination classes were updated to use explicit `@Argument`
   - `MessagesPaneDestination.kt`
   - `StateDrivenDemoDestination.kt`
   - These align the demo with compiler-plugin argument collection and diagnostics.

4. Demo UI adjusted for new destination/runtime expectations
   - `ProfileScreen.kt` now accepts the destination parameter and uses a more explicit `modifier`/`store` signature.
   - `MainTabsUI.kt` introduced `resolveMainTabKind` so nested stack destinations map back to their parent tab presentation.
   - `MainTabsUiTest.kt` verifies that mapping.

## Potential Breaking or Risky Changes

### High

1. `composeApp` now depends on compiler-plugin aggregation by default
   - Evidence:
     - `composeApp/build.gradle.kts` now defaults `useCompilerPlugin` to `true`.
     - `composeApp/src/commonMain/.../DI.kt` removed manual composition and now calls `navigationConfig<AppNavigation>()`.
   - Why risky:
     - This changes the demo from “consumer of the proven KSP/manual path” into “consumer of the experimental path”.
     - If aggregation, wrapper dispatch, or plugin artifact resolution breaks, the demo stops being a reliable smoke test for core runtime behavior.
     - Branch-local evidence shows this backend was still being stabilized late in the branch: the compare includes follow-up fixes for navigation generation and deep-link parity, and internal notes on this branch state that local compiler-plugin changes are not automatically picked up by `composeApp` without publishing to `mavenLocal` first.
   - Migration impact:
     - High for internal development workflow.
     - Medium-to-high for any user treating `composeApp` as the reference integration.
   - Recommendation:
     - Isolate or defer the default-demo switch before shipping the compiler plugin experimentally.
     - Prefer keeping the manual/KSP bootstrap available as the default demo path until parity is proven.

2. `TabRenderer` behavior changed to compensate for compiler-plugin wrapper key normalization
   - Evidence:
     - `quo-vadis-core/src/commonMain/.../TabRenderer.kt` now prefers `scopeKey` over `wrapperKey`.
     - `TabRendererLookupTest` explicitly asserts that new priority.
     - Branch plans explicitly describe container-wrapper dispatch as the active parity problem and recommend avoiding runtime redesign.
   - Why risky:
     - This is a runtime semantic change in `quo-vadis-core`, not just compiler-plugin plumbing.
     - Any existing or manual config that intentionally distinguishes `wrapperKey` from `scopeKey` may now resolve wrappers differently.
     - The branch’s own architecture notes argue the primary fault domain is compiler-plugin emission, not runtime renderer logic.
   - Migration impact:
     - Potentially breaking for advanced/manual container configs.
     - Low for typical generated configs if KSP and plugin both keep wrapper key equal to scope key.
   - Recommendation:
     - Prefer isolating this to plugin-generated configs, or at minimum add explicit backward-compat tests for manual configs where `wrapperKey != scopeKey`.

### Medium

3. Pane back-navigation semantics changed in core with no obvious compiler-plugin dependency
   - Evidence:
     - `BackOperations.kt` and `PaneOperations.kt` changed.
     - `PaneBackNavigationTest.kt` was added with substantial new expectations around removing an entire `PaneNode`, compact vs expanded behavior, and pane clearing/switching.
   - Why risky:
     - This affects all pane consumers, not just compiler-plugin users.
     - It looks like a legitimate runtime bugfix/regression fix, but it is broader than the compiler-plugin implementation itself.
     - If shipped together with the compiler plugin, it becomes harder to attribute regressions.
   - Migration impact:
     - Medium: behavior changes are intentional and now tested, but they are still a runtime contract change.
   - Recommendation:
     - Split, document, or at least call this out as a separate runtime bugfix stream rather than bundling it invisibly into compiler-plugin work.

4. New public bootstrap API can fail hard without plugin rewriting
   - Evidence:
     - `navigationConfig<T>()` is a public function in `quo-vadis-core` whose body is just `error(...)`.
   - Why risky:
     - The API only works when the compiler plugin is active and the IR call-site rewrite succeeds.
     - That is acceptable for an experimental feature, but it is a different failure mode than typical runtime APIs.
     - The branch does include FIR checks requiring `@NavigationRoot`, but there is no core-level runtime test in scope proving end-to-end behavior from the library side.
   - Migration impact:
     - Mostly documentation/support risk rather than binary compatibility risk.
   - Recommendation:
     - Keep it clearly experimental in release messaging and avoid making it the only demo path until coverage is stronger.

### Low

5. `PaneRoleRegistry` and `ScreenRegistry` additions are broad but mostly safe
   - Evidence:
     - `PaneRoleRegistry` gained a `KClass` overload.
     - `ScreenRegistry` gained `Empty`.
   - Why low risk:
     - Both are additive changes with backward-compatible defaults.
     - They look like support APIs for generated implementations and safer fallback behavior.
   - Recommendation:
     - Safe to keep.

6. Demo destination `@Argument` additions and tab-label refactor are safe and likely necessary
   - Evidence:
     - `MessagesPane.ConversationDetail`, `StateDrivenDemoDestination.Profile`, and `Detail` now use `@Argument`.
     - `MainTabsUI.kt` now maps nested stack destinations back to their owning tab via `resolveMainTabKind`, with tests.
   - Why low risk:
     - These are demo-level correctness changes that align the demo with compiler-plugin metadata collection and diagnostics.
     - They do not widen core public API.

## Safe/Internal vs Broad Changes

### Likely Safe / Internal

- `GeneratedNavigationConfig` marker interface.
- `NavigationConfigBuilder.register*` methods, because they are internal API hooks for generated IR and are explicitly marked `@InternalQuoVadisApi`.
- `ScreenRegistry.Empty` additive fallback.
- `PaneRoleRegistry` `KClass` overload.
- Demo `@Argument` annotations.
- Demo `MainTabsUI` presentation refactor plus tests.
- `ProfileScreen` signature changes, which appear to satisfy generated screen-dispatch expectations.

### Potentially Too Broad For Compiler-Plugin Scope

- `TabRenderer` wrapper lookup priority change.
- Pane back-navigation behavior changes.
- Making `composeApp` default to compiler-plugin mode and compiler-plugin aggregation.

## Changes That Should Be Isolated, Reverted, or Deferred

1. Defer or isolate the `composeApp` default bootstrap switch
   - Best candidate for deferment before an experimental release.
   - It increases the blast radius of plugin instability and weakens the demo as a stable comparison target.

2. Isolate the `TabRenderer` runtime key-priority change unless backward compatibility is proven
   - If kept, add explicit tests for:
     - manual `NavigationConfig` implementations
     - wrapper configs where `wrapperKey != scopeKey`
     - KSP-generated configs still behaving identically

3. Treat pane back-navigation changes as a separate runtime bugfix track
   - The tests suggest the behavior is deliberate and likely useful.
   - It still does not look necessary for compiler-plugin support itself.

4. Keep the additive compiler-plugin support in core
   - `navigationConfig<T>()`, `GeneratedNavigationConfig`, and the builder registration hooks appear necessary for the new backend and are reasonable to keep.

## Confidence and Testing Notes

### What the changed tests cover well

- `quo-vadis-core` now has targeted tests for the new tab wrapper lookup contract.
- `quo-vadis-core` has strong new pane back-navigation tests that exercise compact/expanded and sibling/root cases.
- `composeApp` has unit coverage for nested tab destination → tab presentation mapping.

### What is under-tested in the scoped modules

1. No scoped tests were found for `navigationConfig<T>()` as a core API contract
   - There is no `quo-vadis-core` test in scope proving successful call-site rewriting, fallback behavior, or misuse cases from the library side.

2. No scoped compatibility tests were found for manual config composition after the core runtime changes
   - Missing coverage for old-style `NavigationConfig` manual composition plus tab wrappers.

3. No scoped integration tests were found for `composeApp` startup/bootstrap in both modes
   - No app-level test in scope proves:
     - compiler-plugin mode boots cleanly
     - KSP/manual fallback still works cleanly
     - aggregated feature-module discovery is correct in the real demo wiring

4. No scoped tests were found for the local-development artifact resolution trap
   - Branch notes show local compiler-plugin edits require publishing to `mavenLocal` before `composeApp` sees them.
   - That is a real workflow risk not covered by the scoped tests.

### Broader confidence signal outside the scoped modules

- The branch adds substantial compiler-plugin tests and a CI workflow that builds `composeApp` with `-PquoVadis.useCompilerPlugin=true`.
- That improves overall confidence in the plugin effort.
- It does not fully offset the scoped concerns above because the risky changes here are specifically about runtime breadth and demo-default migration.

### Net confidence

- Core additive API support: medium confidence.
- Core runtime behavior changes: medium confidence for intended behavior, lower confidence for backward compatibility.
- Demo migration to compiler-plugin-default bootstrap: low-to-medium confidence until the experimental path is proven as stable as the manual/KSP path.

Note: this audit did not execute the test suite locally; confidence is based on the branch diff, current source, and changed tests.

## Recommended Follow-Up Checks

1. Run `composeApp` end-to-end in both modes before shipping experimentally
   - `quoVadis.useCompilerPlugin=true`
   - `quoVadis.useCompilerPlugin=false`
   - Verify startup, tabs wrapper rendering, pane flows, and deep links.

2. Add backward-compat coverage for `TabRenderer`
   - Especially manual configs and any case where `wrapperKey` and `scopeKey` differ.

3. Decide explicitly whether pane back-navigation changes are part of the compiler-plugin release
   - If yes, document them as runtime behavior changes.
   - If no, split or defer them.

4. Keep the demo’s stable comparison path available
   - Even if compiler-plugin mode remains supported, preserving a known-good manual/KSP bootstrap path would make regressions easier to detect.

5. Validate the local-development workflow
   - Ensure the experimental release instructions mention that local plugin changes may require publishing plugin artifacts before the demo consumes them.

## Final Assessment

The branch did introduce changes in `quo-vadis-core` and `composeApp` that are bigger than the minimum strictly required for compiler-plugin support.

The additive core API work is acceptable. The broader concerns are:

- runtime renderer behavior was changed to compensate for plugin wrapper generation
- pane navigation behavior changed in core during the same branch
- the demo was converted to the experimental bootstrap path by default

For an experimental compiler-plugin release, the safest pre-ship shape would be:

- keep the additive core/compiler support
- isolate or justify the runtime behavior changes explicitly
- avoid forcing `composeApp` to be compiler-plugin-first until parity is firmly demonstrated