# Quo Vadis Architecture Cleanup Plan

> **Created**: 2025-12-14  
> **Status**: Ready for Implementation  
> **Phase**: Post-Migration Cleanup (after Phase 5)

## Executive Summary

This document outlines the detailed cleanup plan for removing deprecated and legacy code from the Quo Vadis navigation library after the architecture refactoring is complete. The goal is to remove all code and APIs referencing the old architecture (GraphNavHost, BackStack-based navigation) while keeping the new NavNode tree-based architecture as the sole implementation.

### Source of Truth

The **demo app (`composeApp`)** serves as the source of truth for which APIs are actively used and should be retained. Any API not used by the demo app and marked as deprecated is a candidate for removal.

### Key Decisions Made

| API/Component | Decision | Rationale |
|--------------|----------|-----------|
| BackStack/MutableBackStack | **Remove completely** | Breaking change, clean API |
| NavigatorBackStackCompat | **Remove completely** | Part of BackStack compat layer |
| NavigationGraph DSL | **Remove completely** | KSP generates everything now |
| TabDefinition/TabNavigatorConfig | **Remove completely** | @Tab annotations replace them |
| LegacyTransitionState | **Rename to TransitionState** | Not actually legacy, just poorly named |
| StateDrivenDemo | **Rewrite** | Update to use new NavNode APIs |

---

## Phase 1: Core Library Cleanup (quo-vadis-core)

### Task 1.1: Remove BackStack Infrastructure

**Files to delete:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/BackStack.kt`

**Components removed:**
- `BackStack` interface
- `MutableBackStack` class  
- `BackStackEntry` data class
- `EXTRA_SELECTED_TAB_ROUTE` constant
- `getExtra()` extension function
- `setExtra()` extension function

**Dependencies to update:**
- `ComposableCache.kt` - Remove BackStackEntry references, keep only NavNode key-based caching
- `package-info.kt` - Remove BackStack from package documentation

**Estimated effort:** 2-3 hours

---

### Task 1.2: Remove NavigatorCompat Layer

**Files to delete:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigatorCompat.kt`

**Components removed:**
- `NavigatorBackStackCompat` class
- `Navigator.backStack` extension property
- `backStackCompatCache` private cache

**Dependencies to update:**
- `detekt-baseline.xml` - Remove entries referencing NavigatorCompat.kt

**Estimated effort:** 1 hour

---

### Task 1.3: Remove NavigationGraph DSL Infrastructure

**Files to delete:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationGraph.kt`

**Components removed:**
- `NavigationGraph` interface
- `DestinationConfig` data class
- `NavigationGraphBuilder` class
- `navigationGraph()` DSL function
- `ModuleNavigation` interface
- `BaseModuleNavigation` abstract class

**Dependencies to update:**
- Remove all imports referencing NavigationGraph in documentation
- Update `package-info.kt`

**Estimated effort:** 1-2 hours

---

### Task 1.4: Remove TabDefinition Infrastructure

**Files to delete:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt`

**Components removed:**
- `TabDefinition` interface
- `TabNavigatorConfig` data class

**Dependencies to update:**
- None identified (already fully deprecated)

**Estimated effort:** 30 minutes

---

### Task 1.5: Rename LegacyTransitionState to TransitionState

**File to modify:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/LegacyTransitionState.kt`

**Changes:**
1. Rename file to `TransitionState.kt`
2. Rename `LegacyTransitionState` sealed interface to `TransitionState`
3. Update all subclasses:
   - `LegacyTransitionState.Idle` → `TransitionState.Idle`
   - `LegacyTransitionState.InProgress` → `TransitionState.InProgress`
   - `LegacyTransitionState.PredictiveBack` → `TransitionState.PredictiveBack`
   - `LegacyTransitionState.Seeking` → `TransitionState.Seeking`
4. Update extension properties:
   - `LegacyTransitionState.isAnimating` → `TransitionState.isAnimating`
   - `LegacyTransitionState.progress` → `TransitionState.progress`

**Files requiring updates:**
- `Navigator.kt` - Update type reference in interface
- `TreeNavigator.kt` - Update all usages (15+ locations)
- `FakeNavigator.kt` - Update all usages
- All test files referencing `LegacyTransitionState`

**Estimated effort:** 2-3 hours

---

### Task 1.6: Clean Up Navigator Interface

**File to modify:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt`

**Changes:**
1. Remove deprecated methods (marked with @Deprecated):
   - Line 169: Deprecated navigation method
   - Line 200: Deprecated navigation method  
   - Line 223: Deprecated navigation method
   - Line 282: Deprecated method
   - Line 295: Deprecated method
2. Clean up any leftover references to BackStack

**Dependencies:**
- Review and update all Navigator usages to use new APIs

**Estimated effort:** 2-3 hours

---

### Task 1.7: Clean Up TreeNavigator

**File to modify:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`

**Changes:**
1. Remove deprecated methods (lines 375, 390, 595)
2. Update TransitionState references after rename

**Estimated effort:** 1-2 hours

---

### Task 1.8: Update Destination.kt

**File to modify:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Destination.kt`

**Changes:**
1. Remove deprecated code at line 51
2. Ensure `TypedDestination` is the primary pattern

**Estimated effort:** 30 minutes

---

### Task 1.9: Clean Up Testing Module

**File to modify:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt`

**Changes:**
1. Remove deprecated method (line 201)
2. Update TransitionState references after rename
3. Remove any BackStack-related compatibility code

**Estimated effort:** 1-2 hours

---

### Task 1.10: Update Detekt Baselines

**Files to modify:**
- `quo-vadis-core/detekt-baseline.xml`

**Changes:**
1. Remove all entries referencing deleted files:
   - GraphNavHost.kt references
   - BackStack.kt references  
   - NavigatorCompat.kt references
2. Update any remaining entries

**Estimated effort:** 30 minutes

---

## Phase 2: Demo App Cleanup (composeApp)

### Task 2.1: Rewrite StateDrivenDemoScreen

**Files to modify:**
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/StateDrivenDemoScreen.kt`
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/BackstackEditorPanel.kt`
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/StateDrivenDestinations.kt`

**Changes:**
1. Replace `BackStack` / `MutableBackStack` usage with `TreeNavigator` and `NavNode`
2. Update the demo to showcase state observation via `StateFlow<NavNode>`
3. Update backstack manipulation to use `TreeMutator` operations
4. Ensure demo demonstrates the new architecture's power, not legacy patterns

**Key architecture changes:**
```kotlin
// OLD (BackStack-based)
val backStack = MutableBackStack().apply { push(destination) }
backStack.entries.lastOrNull()

// NEW (NavNode-based)  
val navigator = TreeNavigator(initialState = buildDemoNavNode())
navigator.state.value.activeStack()
```

**Estimated effort:** 4-6 hours

---

### Task 2.2: Update Demo README

**File to modify:**
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/README.md`

**Changes:**
1. Remove references to `navigator.backStack`
2. Update code examples to use new APIs
3. Document the new NavNode-based patterns

**Estimated effort:** 1-2 hours

---

## Phase 3: FlowMVI Module Cleanup (quo-vadis-core-flow-mvi)

### Task 3.1: Update StateManager

**File to modify:**
- `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/utils/StateManager.kt`

**Changes:**
1. Remove `BackStack.toNavigationState()` extension function
2. Replace with NavNode-based state conversion if needed
3. Update any other BackStack references

**Estimated effort:** 1-2 hours

---

## Phase 4: Documentation Cleanup

### Task 4.1: Update Core Documentation

**Files to modify:**
- `quo-vadis-core/docs/ARCHITECTURE.md`
- `quo-vadis-core/docs/API_REFERENCE.md`
- `quo-vadis-core/docs/MIGRATION.md`
- `quo-vadis-core/docs/NAVIGATION_IMPLEMENTATION.md`
- `quo-vadis-core/docs/STATE_DRIVEN_NAVIGATION.md`
- `quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md`
- `quo-vadis-core/docs/MULTIPLATFORM_PREDICTIVE_BACK.md`
- `quo-vadis-core/docs/ANNOTATION_API.md`

**Changes per file:**

#### ARCHITECTURE.md
- Remove GraphNavHost from component diagram (line 40)
- Remove BackStack from state management section (line 47, 110)
- Update flow diagrams to show NavNode instead of BackStack
- Update all references to use new terminology

#### API_REFERENCE.md
- Remove entire BackStack section (lines 90-116)
- Remove GraphNavHost section (lines 556-694)
- Remove BackStack Extensions section (lines 1069-1077)
- Update code examples throughout

#### MIGRATION.md
- Update to show migration from legacy APIs to new NavNode APIs
- Remove old GraphNavHost examples (lines 184, 200)

#### NAVIGATION_IMPLEMENTATION.md
- Remove BackStack.kt reference (line 128)
- Remove BackStack Interface reference (line 192)
- Update all code examples

#### STATE_DRIVEN_NAVIGATION.md
- Major rewrite needed - this document heavily references deprecated APIs
- Update to show new NavNode-based state observation
- Remove StateBackStack, StateNavigator, StateNavHost sections
- Keep conceptual content but update all examples

#### SHARED_ELEMENT_TRANSITIONS.md
- Update GraphNavHost references to NavigationHost
- Update any SharedElementConfig deprecated sections

#### MULTIPLATFORM_PREDICTIVE_BACK.md
- Remove Legacy API section (line 7 and section starting at 334)
- Update GraphNavHost references to NavigationHost
- Remove decision matrix for legacy vs new

#### ANNOTATION_API.md
- Update GraphNavHost example at line 491
- Ensure all examples use current patterns

**Estimated effort:** 6-8 hours

---

### Task 4.2: Update Documentation Website

**Files to modify:**
- `docs/site/src/pages/Demo/Demo.tsx`
- `docs/site/src/pages/Home/Home.tsx`
- `docs/site/src/pages/Features/Features.tsx`
- `docs/site/src/pages/Features/BackStack/BackStack.tsx`
- `docs/site/src/pages/Features/PredictiveBack/PredictiveBack.tsx`
- `docs/site/src/pages/GettingStarted/GettingStarted.tsx`
- `docs/site/src/data/navigation.ts`
- `docs/site/src/data/searchData.json`

**Changes:**

#### All code examples
- Replace `GraphNavHost` with `NavigationHost`
- Update navigation patterns to use new APIs

#### Features/BackStack/
- Either remove this page entirely OR
- Rename to "Stack Management" and update to show NavNode stack operations

#### Navigation data
- Update navigation.ts to reflect renamed/removed sections
- Regenerate searchData.json after content updates

**Estimated effort:** 4-6 hours

---

### Task 4.3: Update Project README

**File to modify:**
- `README.md`

**Changes:**
1. Remove "Direct BackStack Access" feature mention (line 28)
2. Update "BackStack Manipulation" section (line 366)
3. Remove "Direct BackStack Access" section (line 430)
4. Replace GraphNavHost examples (lines 193, 253) with NavigationHost
5. Update all code examples to reflect new architecture

**Estimated effort:** 2-3 hours

---

### Task 4.4: Update Contributing Guide

**File to modify:**
- `CONTRIBUTING.md`

**Changes:**
- Remove BackStack from naming convention examples (line 96)
- Update any architecture references

**Estimated effort:** 30 minutes

---

## Phase 5: Memory Files Cleanup

### Task 5.1: Update Serena Memories

**Files to modify:**
- `.serena/memories/project_overview.md`
- `.serena/memories/navigation_animation_implementation.md`
- `.serena/memories/shared_element_transitions.md`
- `.serena/memories/architecture_patterns.md`
- `.serena/memories/codebase_structure.md`
- `.serena/memories/code_style_and_conventions.md`
- `.serena/memories/flowmvi_module_implementation.md`

**Changes:**
- Remove GraphNavHost.kt file references
- Remove BackStack.kt file references
- Update architecture diagrams
- Update component lists

**Estimated effort:** 1-2 hours

---

## Phase 6: GitHub & CI Cleanup

### Task 6.1: Update GitHub Agent Instructions

**Files to modify:**
- `.github/instructions/copilot.instructions.md`
- `.github/agents/docs-website.agent.md`
- `.github/agents/Simple-Developer.agent.md`
- `.github/agents/Developer.agent.md`

**Changes:**
- Remove legacy API references from architecture tables
- Update to reflect only new NavNode-based APIs
- Remove BackStack class references from developer agents

**Estimated effort:** 1 hour

---

## Phase 7: Test Cleanup

### Task 7.1: Update Unit Tests

**Files to modify:**
- `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigatorTest.kt`
- `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutatorScopeTest.kt`
- Any other test files with deprecated API references

**Changes:**
1. Update `LegacyTransitionState` references to `TransitionState`
2. Remove any tests for removed APIs
3. Ensure all tests pass with cleaned code

**Estimated effort:** 2-3 hours

---

## Implementation Order

### Critical Path (Must Complete First)
1. **Task 1.5**: Rename LegacyTransitionState (many dependencies)
2. **Task 1.1**: Remove BackStack (core change)
3. **Task 1.2**: Remove NavigatorCompat
4. **Task 1.6**: Clean Navigator interface
5. **Task 1.7**: Clean TreeNavigator

### Secondary (Can Parallelize)
6. **Task 1.3**: Remove NavigationGraph DSL
7. **Task 1.4**: Remove TabDefinition
8. **Task 1.8**: Update Destination.kt
9. **Task 1.9**: Clean FakeNavigator
10. **Task 1.10**: Update Detekt baselines

### Demo App (After Core)
11. **Task 2.1**: Rewrite StateDrivenDemoScreen
12. **Task 2.2**: Update Demo README
13. **Task 3.1**: Update FlowMVI StateManager

### Documentation (After Code)
14. **Task 4.1**: Core documentation
15. **Task 4.2**: Documentation website
16. **Task 4.3**: Project README
17. **Task 4.4**: Contributing guide
18. **Task 5.1**: Memory files
19. **Task 6.1**: GitHub agents

### Final Verification
20. **Task 7.1**: Update and run all tests
21. Build verification on all platforms
22. Manual testing of demo app

---

## Risk Assessment

### High Risk
- **Breaking changes for external users** - Anyone using BackStack APIs will need to migrate
- **StateDrivenDemo rewrite** - Complex UI that needs careful reimplementation

### Medium Risk  
- **TransitionState rename** - Many files to update, potential for missed references
- **Documentation inconsistencies** - Large surface area to update

### Low Risk
- **TabDefinition removal** - Already deprecated, minimal usage
- **NavigationGraph DSL removal** - Already replaced by KSP

---

## Verification Checklist

After cleanup completion:

- [ ] `./gradlew build` succeeds on all modules
- [ ] All unit tests pass
- [ ] Demo app runs on Android, iOS, Desktop, Web
- [ ] No `BackStack` imports remain in non-test code
- [ ] No `GraphNavHost` references remain
- [ ] No `LegacyTransitionState` references remain (all renamed to `TransitionState`)
- [ ] No compiler warnings about deprecated APIs
- [ ] Documentation website builds and deploys
- [ ] Search in codebase for deprecated terms returns no production code hits

---

## Estimated Total Effort

| Phase | Hours |
|-------|-------|
| Phase 1: Core Library | 12-16 |
| Phase 2: Demo App | 5-8 |
| Phase 3: FlowMVI | 1-2 |
| Phase 4: Documentation | 12-17 |
| Phase 5: Memories | 1-2 |
| Phase 6: GitHub/CI | 1 |
| Phase 7: Testing | 2-3 |
| **Total** | **34-49 hours** |

Estimated calendar time: **1-2 weeks** (single developer, part-time)

---

## Appendix: Files Summary

### Files to DELETE
```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/BackStack.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigatorCompat.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationGraph.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt
```

### Files to RENAME
```
LegacyTransitionState.kt → TransitionState.kt
```

### Files to HEAVILY MODIFY
```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/StateDrivenDemoScreen.kt
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/BackstackEditorPanel.kt
quo-vadis-core/docs/STATE_DRIVEN_NAVIGATION.md
quo-vadis-core/docs/API_REFERENCE.md
```

### Documentation Pages to UPDATE
```
quo-vadis-core/docs/*.md (8 files)
docs/site/src/pages/**/*.tsx (7+ files)
README.md
CONTRIBUTING.md
```
