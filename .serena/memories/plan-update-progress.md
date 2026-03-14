# Plan Update Progress

## Task
Updating `plans/compiler-plugin-ksp-interchangeability-plan.md` to support:
- True zero-code-change backend switching (KSP ↔ compiler plugin)
- KSP support for `navigationConfig<T>()` via runtime registry
- KSP support for `@NavigationRoot` processing and aggregated config generation

## User Decisions
1. Drop `GeneratedNavigationConfig` marker interface - use `NavigationConfig` directly
2. Add `@GeneratedConfig` annotation (lightweight, on generated config objects, for discovery)
3. Add `NavigationConfigRegistry` internal singleton to quo-vadis-core
4. KSP classpath scanning for multi-module discovery (scan for `@GeneratedConfig`)
5. Both backends implement full API parity
6. KSP processor `@Deprecated` should be removed

## Current State
- Lines 1-314 of the plan file contain CORRECT new content (Overview, Current State, Target State, Architecture Changes sections 1-9)
- Lines 315-723 contain OLD/stale content from the original plan (old Gap Analysis, old Task Breakdown, old Risks, etc.)
- The good part is saved at `/tmp/plan-part1.md` (314 lines)
- The original backup is at `.bak` file

## What Remains
Need to replace lines 315-723 with new Gap Analysis, Configuration Model, Task Breakdown (5 phases), Recommended Sequencing, Risks and Mitigations, Validation Strategy, Open Questions, and Recommendation sections.

Key new sections needed from line 315 onwards:
- Gap Analysis (8 gaps: annotation missing, registry missing, KSP no @NavigationRoot, KSP no multi-module, GeneratedNavigationConfig still used, duplicate guards, test symmetry, local dev fragile)
- Configuration Model (already implemented backend enum)
- Task Breakdown: Phase 1 (core infra), Phase 2 (compiler plugin migration), Phase 3 (KSP parity), Phase 4 (shared tests), Phase 5 (docs)
- Sequencing: 9 ordered steps
- Risks: KSP classpath scanning, registry timing, duplicates, mixed mode, drift, stale artifacts, Kotlin API churn
- Validation: Build matrix, contract, workflow
- Open Questions: 4 items about thread safety, dev resolution, KSP plugin tolerance, discovery mechanism
- Recommendation: Full API parity with dual-path resolution strategy
