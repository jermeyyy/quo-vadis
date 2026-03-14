# Compiler Plugin Documentation Plan

**Date**: 8 March 2026  
**Branch context**: `compiler-plugin`  
**Goal**: Plan documentation changes to introduce the compiler plugin as an **experimental alternative** to KSP across repository markdown docs and the docs website, without writing the final user-facing docs yet.

## Problem Statement

Quo Vadis already contains compiler-plugin signals, but the documentation surface is inconsistent:

- The repository has a dedicated migration guide in `docs/MIGRATION.md`, but most high-traffic docs still present KSP as the only or default annotation-processing backend.
- The website is currently wired around KSP-first messaging in `Getting Started`, `Home`, `Annotation API`, `Modular`, `Tabbed Navigation`, `Pane Layouts`, and `DSL Config`.
- Website navigation and routing are explicit, so any new page must be added in multiple places.
- Website search is not yet driven by `searchData.json`; `docs/site/src/contexts/SearchContext.tsx` still uses a small mock array. This changes how much search-index work is actually required for rollout.

The documentation plan therefore needs to do two things safely:

1. Create a canonical place for experimental compiler-plugin guidance.
2. Remove misleading KSP-only framing from entry-point docs without overpromising stability.

## Current State Summary

### Repository docs

Current top-level markdown docs under `docs/`:

- `ANNOTATIONS.md`
- `ARCHITECTURE.md`
- `DSL-CONFIG.md`
- `FLOWMVI-KOIN.md`
- `MIGRATION.md`
- `NAV-NODES.md`
- `NAVIGATOR.md`
- `TRANSITIONS.md`
- `TREE-MUTATOR.md`

Observed behavior:

- `docs/MIGRATION.md` already covers KSP to compiler-plugin migration in detail, including `@NavigationRoot`, mode switching, rollback, and behavioral differences.
- `docs/ANNOTATIONS.md` is still written as if KSP is the sole annotation backend.
- `docs/DSL-CONFIG.md` compares DSL against “Annotations” using KSP-only wording.
- `README.md` has partial compiler-plugin awareness, but installation and setup still remain predominantly KSP-first.

### Website structure

Current website structure is explicit and page-based:

- Routes are declared in `docs/site/src/App.tsx`.
- Sidebar navigation is declared in `docs/site/src/data/navigation.ts`.
- Search is currently mock-driven in `docs/site/src/contexts/SearchContext.tsx`.
- Feature pages live under `docs/site/src/pages/Features/...`.

Relevant current pages:

- `Home`
- `Getting Started`
- `Features/AnnotationAPI`
- `Features/DSLConfig`
- `Features/Modular`
- `Features/TabbedNavigation`
- `Features/PaneLayouts`
- `Features/CoreConcepts`

Observed behavior:

- The site already supports nested navigation groups, as shown by `FlowMVI & Koin`.
- For a feature still marked experimental, a single focused feature page is cheaper and safer than a full multi-page website section.
- Because search is mock-based today, route/nav discoverability matters more than updating `searchData.json` alone.

## Audience And Scope

### Primary audiences

1. **New adopters** deciding whether to start with KSP or try the compiler plugin.
2. **Existing KSP users** evaluating migration risk and rollback path.
3. **Multi-module users** who need to understand config aggregation differences such as `@NavigationRoot`.
4. **Library contributors and early adopters** testing the compiler plugin on the experimental branch.

### Documentation scope for this rollout

In scope:

- Installation and backend selection guidance.
- Clear explanation of **experimental** status.
- Safe mode-switching between KSP and compiler plugin.
- Migration path from KSP.
- Multi-module differences and `@NavigationRoot`.
- Compatibility notes, limitations, testing expectations, and rollback.

Out of scope:

- Full compiler-plugin internals documentation.
- Exhaustive API reference for plugin implementation internals.
- Rewriting all docs to be backend-neutral in one pass.

## Required Documentation Topics

These are the minimum topics required to support an experimental compiler-plugin rollout and backend switching with KSP.

### 1. Positioning and status

- What the compiler plugin is.
- Why it exists alongside KSP.
- Why it is experimental right now.
- Who should adopt it now versus stay on KSP.

### 2. Backend selection model

- Supported backends: KSP and compiler plugin.
- Project-level or module-level switching rules.
- Whether mixed-backend setups are allowed, discouraged, or unsupported.
- What the Gradle plugin defaults are on this branch and what production docs should say once merged.

### 3. Installation and setup

- Recommended setup for trying the compiler plugin.
- KSP setup as the stable/default fallback.
- Required Kotlin version and any K2/compiler constraints.
- Gradle plugin configuration differences.

### 4. What stays the same

- Annotation model remains the same for `@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`, and related annotations.
- Runtime navigation behavior expected to remain compatible.
- DSL configuration remains a backend-independent alternative.

### 5. What changes with the compiler plugin

- No visible generated source files.
- IDE/autocomplete expectations.
- Multi-module aggregation via `@NavigationRoot`.
- Build/debug workflow changes.
- Local development caveats for plugin artifact resolution.

### 6. Migration and rollback

- KSP to compiler-plugin migration steps.
- Rollback path back to KSP.
- How to verify parity after switching.
- How to reason about generated config naming and discovery changes.

### 7. Compatibility and limitations

- Kotlin and Compose compatibility constraints.
- Experimental compiler API caveat.
- Known limitations versus KSP.
- Any missing parity areas, if applicable.
- Local development caveat: compiler plugin changes may require publishing locally before downstream demo verification.

### 8. Examples and verification

- Single-module setup example.
- Multi-module setup example.
- `@NavigationRoot` example.
- Mode-switch example in Gradle config.
- Verification commands and smoke-test checklist.

### 9. Warnings and adoption guidance

- Experimental warning callout.
- Do not use both backends in the same module.
- Prefer KSP for production stability until the plugin graduates.
- Expect docs and APIs to change during the experiment.

## Proposed Information Architecture

### Recommendation for `docs/`

Use a **new canonical markdown guide** for the experimental backend, while keeping `MIGRATION.md` focused on cutover.

#### Primary new doc

- **Add** `docs/COMPILER-PLUGIN.md`

Why:

- `MIGRATION.md` is already migration-shaped and assumes an existing KSP user.
- Experimental rollout needs a broader document covering evaluation, installation, backend choice, limitations, and rollback.
- A dedicated document gives README and website pages a stable canonical target.

#### Existing docs to update

- **Update** `docs/MIGRATION.md`
  - Reframe as the focused migration companion.
  - Link to `COMPILER-PLUGIN.md` for backend overview and adoption guidance.
- **Update** `docs/ANNOTATIONS.md`
  - Replace KSP-only framing with “annotation-based configuration supported by KSP and the compiler plugin”.
  - Keep annotation semantics in one place; avoid duplicating migration content.
- **Update** `docs/DSL-CONFIG.md`
  - Clarify that DSL is the backend-independent/manual option, while annotations can be powered by KSP or the compiler plugin.
- **Update** `README.md`
  - Improve discoverability from the root entry point.
  - Point new users to backend choice docs before they fall into KSP-only setup.

### Recommendation for `docs/site`

Add **one new feature page** instead of a large section for the first rollout.

#### Primary website page

- **Add** `docs/site/src/pages/Features/CompilerPlugin/CompilerPlugin.tsx`

Route:

- `/features/compiler-plugin`

Navigation label:

- `Compiler Plugin (Experimental)`

Why this is the best integration point:

- The site’s existing IA groups conceptual guides under `Features`.
- Compiler plugin guidance is closer to “how annotation-based navigation is generated” than to core architecture or demo content.
- A single page limits maintenance cost while the backend is still experimental.
- It can link outward to repo markdown docs for deeper migration detail instead of duplicating the whole migration guide in React.

#### Existing website pages to update

- **Update** `docs/site/src/App.tsx`
  - Register the new route.
- **Update** `docs/site/src/data/navigation.ts`
  - Add the page to the Features tree near `Annotation-Based API`, `DSL Configuration`, and `Modular Architecture`.
- **Update** `docs/site/src/pages/GettingStarted/GettingStarted.tsx`
  - Change installation from KSP-first to backend-choice framing.
  - Add a link to the compiler plugin page.
- **Update** `docs/site/src/pages/Features/AnnotationAPI/AnnotationAPI.tsx`
  - Replace KSP-only statements with backend-neutral wording plus a link to the compiler-plugin page.
- **Update** `docs/site/src/pages/Features/DSLConfig/DSLConfig.tsx`
  - Clarify “Annotations” means annotation-based config with either backend, not only KSP.
- **Update** `docs/site/src/pages/Features/Modular/Modular.tsx`
  - Add a note explaining KSP manual config composition versus compiler-plugin `@NavigationRoot` aggregation.
- **Update** `docs/site/src/pages/Home/Home.tsx`
  - Remove hard KSP-only marketing statements from hero and quick-start copy.

#### Search/discoverability recommendation

- **Update** `docs/site/src/contexts/SearchContext.tsx`
  - Add a temporary search entry for the compiler-plugin page because current search is mock-based.
- **Do not prioritize** `docs/site/src/data/searchData.json` for this rollout unless search implementation changes.

Why:

- The site does not currently consume `searchData.json`.
- Updating only the JSON file would create maintenance work with no user-facing effect.
- If search is upgraded later, the compiler-plugin page should be added to the real indexing pipeline then.

## Specific File Recommendations

### Repository markdown docs

**Add**

- `docs/COMPILER-PLUGIN.md`

**Update**

- `README.md`
- `docs/MIGRATION.md`
- `docs/ANNOTATIONS.md`
- `docs/DSL-CONFIG.md`

### Website

**Add**

- `docs/site/src/pages/Features/CompilerPlugin/CompilerPlugin.tsx`

**Update**

- `docs/site/src/App.tsx`
- `docs/site/src/data/navigation.ts`
- `docs/site/src/pages/Home/Home.tsx`
- `docs/site/src/pages/GettingStarted/GettingStarted.tsx`
- `docs/site/src/pages/Features/AnnotationAPI/AnnotationAPI.tsx`
- `docs/site/src/pages/Features/DSLConfig/DSLConfig.tsx`
- `docs/site/src/pages/Features/Modular/Modular.tsx`
- `docs/site/src/contexts/SearchContext.tsx`

### Optional later-phase website updates

Defer until the compiler plugin is less experimental or search is real:

- `docs/site/src/data/searchData.json`
- `docs/site/src/data/codeExamples.ts`
- `docs/site/src/data/constants.ts`
- `docs/site/src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`
- `docs/site/src/pages/Features/PaneLayouts/PaneLayouts.tsx`

These should eventually be made backend-neutral, but they are lower-leverage than the entry points above.

## Content Outline For `docs/COMPILER-PLUGIN.md`

Recommended outline:

### 1. Title and status banner

- `Compiler Plugin Guide (Experimental)`
- Short warning block: experimental, parity target, fallback available.

### 2. When to use it

- Who should evaluate it now.
- Who should remain on KSP.

### 3. Backend overview

- KSP versus compiler plugin.
- What changes and what stays the same.

### 4. Installation

- Single-module setup.
- Multi-module setup.
- Required versions.

### 5. Switching backends

- From stable KSP to experimental compiler plugin.
- How to opt back out.
- Constraints on mixing backends.

### 6. Multi-module behavior

- Manual config composition with KSP.
- `@NavigationRoot` aggregation with the compiler plugin.

### 7. Generated output model

- No generated `.kt` files.
- IDE and debugging implications.

### 8. Compatibility and limitations

- Kotlin/K2/compiler API caveats.
- Current gaps or known risks.
- Local development note about publishing plugin artifacts if applicable.

### 9. Verification checklist

- Build command(s).
- Smoke-test flow.
- Suggested parity checks.

### 10. Rollback

- Exact steps to return to KSP.

### 11. Related docs

- Link to `MIGRATION.md`.
- Link to `ANNOTATIONS.md`.
- Link to `DSL-CONFIG.md`.

## Content Outline For Website Page

Recommended single-page outline for `/features/compiler-plugin`:

### 1. Intro

- What the compiler plugin is.
- Experimental badge/callout.

### 2. Why it exists

- Faster build path.
- Better IDE story.
- Cleaner multi-module discovery.

### 3. KSP vs compiler plugin

- Short comparison table.
- Clear default recommendation.

### 4. Getting started

- Minimal compiler-plugin setup example.
- Link to repo doc for full details.

### 5. Switching and rollback

- Short mode-switch checklist.
- Link to migration guide.

### 6. Multi-module support

- `@NavigationRoot` example.
- Comparison with manual `+` config composition.

### 7. Limitations and warnings

- Experimental API caveat.
- Not both backends in one module.
- Version sensitivity.

### 8. Next steps

- Links to `Getting Started`, `Annotation API`, `Modular Architecture`, and repo migration guide.

## Recommended Messaging Strategy

### Default stance for rollout

Use this hierarchy consistently:

1. **KSP remains the stable/default recommendation** unless product direction changes before release.
2. **Compiler plugin is experimental but first-class enough to document.**
3. **Annotations are a frontend API; KSP and compiler plugin are backends.**
4. **DSL remains the manual/backend-independent alternative.**

### Copy direction to apply across docs

Replace wording like:

- “The KSP processor generates...”

With wording like:

- “Quo Vadis can generate annotation-based navigation infrastructure via KSP or the experimental compiler plugin.”
- “In KSP mode...”
- “In compiler-plugin mode...”

This prevents widespread doc drift the next time the default backend changes.

## Rollout Sequencing

Use this sequence so docs track the feature safely and do not overstate readiness.

### Phase 1. Canonical experimental docs

- Add `docs/COMPILER-PLUGIN.md`.
- Update `docs/MIGRATION.md` to reference it.

Reason:

- Establish one canonical source before editing every entry point.

### Phase 2. Discoverability fixes

- Update `README.md`.
- Update website `Home` and `Getting Started`.
- Add `/features/compiler-plugin` page and sidebar route.

Reason:

- Prevent new users from seeing outdated KSP-only setup after the feature lands.

### Phase 3. Conceptual consistency

- Update `ANNOTATIONS.md`.
- Update `DSL-CONFIG.md`.
- Update website `Annotation API`, `DSL Config`, and `Modular` pages.

Reason:

- These pages explain backend-related concepts and currently encode incorrect assumptions.

### Phase 4. Breadth cleanup

- Sweep other website feature pages that say “KSP generates...”
- Normalize code examples and constants if the compiler plugin becomes the public default.

Reason:

- This is lower priority than preventing onboarding mistakes.

### Phase 5. Search/index modernization

- If site search is upgraded from mock data, include the compiler-plugin page in the real indexing pipeline.

Reason:

- Current mock search means search-index work is mostly cosmetic right now.

## Dependencies On Implementation

These docs should not be finalized until the corresponding implementation details are confirmed.

### Must be confirmed before publishing user-facing docs

- Exact Gradle plugin flags and defaults, especially whether `useCompilerPlugin` defaults to true on the target branch or only in local/experimental contexts.
- Official support stance on mixed backend usage across modules.
- Required Kotlin version and minimum supported toolchain.
- Whether `@NavigationRoot` naming, behavior, and customization are finalized.
- Canonical verification command(s) for compiler-plugin users.

### Should be confirmed if mentioned in docs

- Known parity status for tabs, panes, deep links, and multi-module aggregation.
- Any local development workflow caveat involving publishing compiler-plugin artifacts before testing downstream modules.
- Expected debugging story for generated output or IR dumps.

## Required Examples, Warnings, Migration Notes, And Compatibility Notes

### Examples that must be included

1. Single-module compiler-plugin setup.
2. Single-module KSP setup as fallback/reference.
3. Backend switching example in Gradle config.
4. Multi-module `@NavigationRoot` example.
5. Rollback example returning to KSP.

### Warnings that must be included

1. Compiler plugin is experimental.
2. Do not enable both backends in the same module.
3. Toolchain upgrades may affect compiler-plugin behavior sooner than KSP.
4. Generated source inspection differs from KSP because `.kt` files are not emitted.

### Migration notes that must be included

1. Annotation source code generally does not need to change.
2. Build configuration changes are the primary migration surface.
3. Multi-module composition may change from manual `+` composition to `@NavigationRoot` aggregation.
4. Rollback path must be explicit and low-risk.

### Compatibility notes that must be included

1. Required Kotlin/K2 version.
2. Current stability expectations for the compiler plugin.
3. Any known parity limitations or caveats.
4. Relationship between annotations, DSL, KSP, and compiler plugin.

## Risks And Open Questions

### Risks

1. **Mixed messaging risk**: Existing pages still present KSP as the only backend, which can make the experimental rollout look incomplete or contradictory.
2. **Over-documenting too early**: If public docs imply the compiler plugin is production-ready before defaults and compatibility are stable, users will adopt it under the wrong assumptions.
3. **Duplication risk**: Copying migration content into both markdown docs and website pages will create maintenance drift.
4. **Search drift risk**: Updating `searchData.json` without fixing actual search behavior gives a false sense of discoverability coverage.
5. **Example drift risk**: Centralized website examples in `codeExamples.ts` are still KSP-focused and can easily contradict new overview pages if not scheduled for a later sweep.

### Open questions

1. Is the intended public recommendation still “KSP first, compiler plugin experimental”, or is the branch moving toward “compiler plugin default, KSP fallback”?
2. Should `docs/MIGRATION.md` remain named specifically as KSP to compiler-plugin migration, or should it eventually become a broader backend migration guide?
3. Is `@NavigationRoot` finalized enough to feature prominently in onboarding docs, or should it stay framed as an advanced/multi-module feature?
4. Are there any known unsupported edge cases where docs must explicitly tell users to stay on KSP?
5. Should the docs website eventually get a real search index as part of this rollout, or is that intentionally deferred?

## Recommended Outcome

For this rollout, the safest and clearest documentation shape is:

- one new canonical markdown guide: `docs/COMPILER-PLUGIN.md`
- one new website feature page: `/features/compiler-plugin`
- targeted entry-point updates to `README`, `Getting Started`, `Home`, `Annotation API`, `DSL Config`, `Modular`, and `MIGRATION`
- explicit experimental messaging and backend-choice framing
- deferred full-site wording cleanup and real search-index work until implementation and default-backend decisions settle

This gives Quo Vadis a coherent experimental rollout story without forcing a premature full documentation rewrite.