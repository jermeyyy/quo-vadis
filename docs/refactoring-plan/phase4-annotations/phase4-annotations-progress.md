# Phase 4: Annotations Redesign - Progress

> **Last Updated**: 2025-12-06  
> **Phase Status**: 🟡 In Progress  
> **Progress**: 4/5 tasks (80%)

## Overview

This phase introduces the new annotation system that maps directly to NavNode types, providing a cleaner API for defining navigation graphs.

---

## Task Progress

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [ANN-001](./ANN-001-graph-type.md) | Define `@Destination` Annotation | 🟢 Completed | 2025-12-06 | Created `Destination.kt` with route parameter |
| [ANN-002](./ANN-002-pane-graph.md) | Define `@Stack` Container Annotation | 🟢 Completed | 2025-12-06 | Created `Stack.kt` with name and startDestination |
| [ANN-003](./ANN-003-route-transitions.md) | Define `@Tab` and `@TabItem` Annotations | 🟢 Completed | 2025-12-06 | Created `TabAnnotations.kt`, replaced old @TabGraph/@Tab |
| [ANN-004](./ANN-004-shared-element.md) | Define `@Pane` and `@PaneItem` Annotations | 🟢 Completed | 2025-12-06 | Created `PaneAnnotations.kt` with enums and annotations |
| [ANN-005](./ANN-005-screen.md) | Define `@Screen` Content Binding Annotation | ⚪ Not Started | - | No dependencies |

---

## Completed Tasks

### ANN-001: Define @Destination Annotation ✅
- **Completed**: 2025-12-06
- Created `Destination.kt` in `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/`
- `@Destination(route: String = "")` annotation with:
  - `@Target(AnnotationTarget.CLASS)` - applies to classes/objects
  - `@Retention(AnnotationRetention.SOURCE)` - compile-time only
  - Default empty route (not deep-linkable)
  - Comprehensive KDoc with examples for path params, query params, and non-deep-linkable
- Build verified: `:quo-vadis-annotations:build` ✓

### ANN-002: Define @Stack Container Annotation ✅
- **Completed**: 2025-12-06
- Created `Stack.kt` in `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/`
- `@Stack(name: String, startDestination: String = "")` annotation with:
  - `name` - required unique name for the stack
  - `startDestination` - optional, defaults to first declared subclass
  - `@Target(AnnotationTarget.CLASS)` - applies to sealed classes/interfaces
  - `@Retention(AnnotationRetention.SOURCE)` - compile-time only
  - Comprehensive KDoc with examples for basic, default start, and complex destinations
- Build verified: `:quo-vadis-annotations:build` ✓

### ANN-003: Define @Tab and @TabItem Annotations ✅
- **Completed**: 2025-12-06
- Created `TabAnnotations.kt` in `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/`
- Removed old `@TabGraph`, `@Tab`, `@TabContent` from `Annotations.kt` (no usages found)
- `@Tab(name: String, initialTab: String = "")` annotation with:
  - `name` - unique identifier for the tab container
  - `initialTab` - optional, defaults to first declared subclass
  - Maps to `TabNode` in NavNode hierarchy
- `@TabItem(label: String, icon: String = "", rootGraph: KClass<*>)` annotation with:
  - `label` - display label for the tab
  - `icon` - platform-specific icon identifier (optional)
  - `rootGraph` - root navigation graph class (must be @Stack annotated)
- Both annotations have `@Target(AnnotationTarget.CLASS)`, `@Retention(AnnotationRetention.SOURCE)`
- Comprehensive KDoc with examples for basic tabs, deep linking, and nested tabs
- Build verified: `:quo-vadis-annotations:build` ✓

### ANN-004: Define @Pane and @PaneItem Annotations ✅
- **Completed**: 2025-12-06
- Created `PaneAnnotations.kt` in `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/`
- `PaneBackBehavior` enum with `PopUntilScaffoldValueChange`, `PopUntilContentChange`, `PopLatest`
- `PaneRole` enum with `PRIMARY`, `SECONDARY`, `EXTRA`
- `AdaptStrategy` enum with `HIDE`, `COLLAPSE`, `OVERLAY`, `REFLOW`
- `@Pane(name: String, backBehavior: PaneBackBehavior = PopUntilScaffoldValueChange)` annotation with:
  - `name` - unique identifier for the pane container
  - `backBehavior` - configures back navigation in multi-pane layouts
  - Maps to `PaneNode` in NavNode hierarchy
- `@PaneItem(role: PaneRole, adaptStrategy: AdaptStrategy = HIDE, rootGraph: KClass<*>)` annotation with:
  - `role` - layout role (PRIMARY/SECONDARY/EXTRA)
  - `adaptStrategy` - adaptation behavior on compact screens
  - `rootGraph` - root navigation graph class (must be @Stack annotated)
- Both annotations have `@Target(AnnotationTarget.CLASS)`, `@Retention(AnnotationRetention.SOURCE)`
- Comprehensive KDoc with examples for list-detail and three-pane patterns
- Build verified: `:quo-vadis-annotations:build` ✓

---

## In Progress Tasks

_None currently in progress._

---

## Blocked Tasks

_None - all tasks can start immediately._

---

## Ready to Start

All remaining annotation tasks can be started immediately as they have no dependencies:

1. ~~**ANN-001**: Define `@Destination` Annotation (0.5 days)~~ ✅ Completed
2. ~~**ANN-002**: Define `@Stack` Container Annotation (0.5 days)~~ ✅ Completed
3. ~~**ANN-003**: Define `@Tab` and `@TabItem` Annotations (1 day)~~ ✅ Completed
4. ~~**ANN-004**: Define `@Pane` and `@PaneItem` Annotations (1 day)~~ ✅ Completed
5. **ANN-005**: Define `@Screen` Content Binding Annotation (0.5 days)

---

## Dependencies

```
(No dependencies - all tasks can be parallelized)

ANN-001 ─┐
ANN-002 ─┼──► Phase 3 (KSP)
ANN-003 ─┤
ANN-004 ─┤
ANN-005 ─┘
```

---

## New Annotation Summary

| Annotation | Maps To | Purpose |
|------------|---------|---------|
| `@Destination(route)` | `ScreenNode` | Navigation target with deep link route |
| `@Stack(name, startDestination)` | `StackNode` | Linear navigation container |
| `@Tab(name, initialTab)` | `TabNode` | Tabbed navigation container |
| `@TabItem(label, icon, rootGraph)` | Tab metadata | Tab UI configuration |
| `@Pane(name, backBehavior)` | `PaneNode` | Adaptive layout container |
| `@PaneItem(role, adaptStrategy)` | Pane metadata | Pane behavior configuration |
| `@Screen(destination)` | Registry entry | Composable-to-destination binding |

---

## Notes

- Estimated 3.5 days total
- Can be done in parallel with Phase 1 and Phase 2
- Annotations are defined in `quo-vadis-annotations` module
- Must be completed before Phase 3 (KSP)

---

## Related Documents

- [Phase 4 Summary](./phase4-annotations-summary.md)
