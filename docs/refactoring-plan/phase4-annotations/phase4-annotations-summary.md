# Phase 4: Annotations - Summary

## Phase Overview

Phase 4 focuses on defining the annotation system for the Quo Vadis navigation library. These annotations provide a declarative way to define navigation graphs, containers, and screen bindings that are processed by KSP (Kotlin Symbol Processing) to generate the navigation infrastructure at compile time.

**Phase Number in Tasks**: Listed as "Phase 3 - Annotations" in task metadata (note: file organization uses phase4)

## Objectives

1. Create foundational annotations for navigation destination declaration
2. Define container annotations for different navigation patterns (stack, tabs, panes)
3. Enable compile-time code generation through KSP-compatible annotations
4. Support deep linking through route configuration
5. Provide screen content binding for Composable functions
6. Enable shared element transitions through annotation parameters

---

## Task Summaries

### ANN-001: Define @Destination Annotation

| Field | Value |
|-------|-------|
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |

**Purpose**: Create the foundational `@Destination` annotation that marks classes/objects as navigation targets.

**Key Features**:
- Marks data objects (parameterless) and data classes (with parameters) as navigable destinations
- `route` parameter for deep linking support (path parameters, query parameters)
- Empty route means destination is not deep-linkable
- Maps to `ScreenNode` in NavNode hierarchy

**Parameters**:
- `route: String = ""` - Route path for deep linking

---

### ANN-002: Define @Stack Container Annotation

| Field | Value |
|-------|-------|
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | ANN-001 |

**Purpose**: Create the `@Stack` annotation for stack-based (push/pop) navigation containers.

**Key Features**:
- Marks sealed classes/interfaces as navigation containers
- Linear push/pop navigation behavior
- All sealed subclasses become destinations within the stack
- Automatic start destination resolution from sealed subclass names
- Maps to `StackNode` in NavNode hierarchy

**Parameters**:
- `name: String` (required) - Unique name for the stack
- `startDestination: String = ""` - Simple name of initial destination

---

### ANN-003: Define @Tab and @TabItem Annotations

| Field | Value |
|-------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001, ANN-002 |

**Purpose**: Define annotations for tabbed navigation with parallel independent stacks.

**Key Features**:
- `@Tab` marks container, `@TabItem` provides metadata per tab
- Independent back stacks per tab
- State preservation when switching tabs
- Deep linking to specific tabs
- Configurable initial tab selection
- Maps to `TabNode` in NavNode hierarchy

**@Tab Parameters**:
- `name: String` - Unique identifier for tab container
- `initialTab: String = ""` - Initially selected tab name

**@TabItem Parameters**:
- `label: String` - Display label for tab
- `icon: String = ""` - Platform-specific icon identifier
- `rootGraph: KClass<*>` - Reference to @Stack-annotated sealed class

---

### ANN-004: Define @Pane and @PaneItem Annotations

| Field | Value |
|-------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001, ANN-002 |

**Purpose**: Define annotations for adaptive layouts supporting split-view and responsive patterns.

**Key Features**:
- `@Pane` marks container, `@PaneItem` defines each pane's role
- Supports list-detail patterns, multi-column layouts
- Responsive adaptation to screen size
- Three pane roles: PRIMARY, SECONDARY, EXTRA
- Configurable back navigation behavior
- Maps to `PaneNode` in NavNode hierarchy

**Supporting Enums**:
- `PaneBackBehavior`: `PopUntilScaffoldValueChange`, `PopUntilContentChange`, `PopLatest`
- `PaneRole`: `PRIMARY`, `SECONDARY`, `EXTRA`
- `AdaptStrategy`: `HIDE`, `COLLAPSE`, `OVERLAY`, `REFLOW`

**@Pane Parameters**:
- `name: String` - Unique identifier for pane container
- `backBehavior: PaneBackBehavior = PopUntilScaffoldValueChange`

**@PaneItem Parameters**:
- `role: PaneRole` - Layout role (PRIMARY/SECONDARY/EXTRA)
- `adaptStrategy: AdaptStrategy = HIDE` - Adaptation behavior
- `rootGraph: KClass<*>` - Reference to @Stack-annotated sealed class

---

### ANN-005: Define @Screen Content Binding Annotation

| Field | Value |
|-------|-------|
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | ANN-001 |

**Purpose**: Create the `@Screen` annotation that binds Composable functions to destinations.

**Key Features**:
- Targets functions only (specifically @Composable functions)
- Links Composable to specific @Destination-annotated class
- Three function signature patterns supported
- Enables shared element transition parameters
- Generates entries in `GeneratedScreenRegistry`

**Supported Function Signatures**:
1. **Simple destinations**: `fun Screen(navigator: Navigator)`
2. **With data**: `fun Screen(destination: DestType, navigator: Navigator)`
3. **With shared elements**: `fun Screen(destination: DestType, navigator: Navigator, sharedTransitionScope: SharedTransitionScope?, animatedVisibilityScope: AnimatedVisibilityScope?)`

**Parameters**:
- `destination: KClass<*>` - The destination class this Composable renders

---

## Key Components/Features to Implement

### Annotations

| Annotation | Target | File Location |
|------------|--------|---------------|
| `@Destination` | CLASS | `quo-vadis-annotations/.../Destination.kt` |
| `@Stack` | CLASS | `quo-vadis-annotations/.../Stack.kt` |
| `@Tab` | CLASS | `quo-vadis-annotations/.../TabAnnotations.kt` |
| `@TabItem` | CLASS | `quo-vadis-annotations/.../TabAnnotations.kt` |
| `@Pane` | CLASS | `quo-vadis-annotations/.../PaneAnnotations.kt` |
| `@PaneItem` | CLASS | `quo-vadis-annotations/.../PaneAnnotations.kt` |
| `@Screen` | FUNCTION | `quo-vadis-annotations/.../Screen.kt` |

### Supporting Types

| Type | Kind | Description |
|------|------|-------------|
| `PaneBackBehavior` | enum | Back navigation behavior in panes |
| `PaneRole` | enum | Pane layout roles |
| `AdaptStrategy` | enum | Pane adaptation strategies |
| `TabMetadata` | data class | Generated tab metadata |
| `PaneMetadata` | data class | Generated pane metadata |
| `ScreenContext` | data class | Context provided to screen composables |
| `GeneratedScreenRegistry` | object | KSP-generated destination-to-screen mapping |

---

## Dependencies on Other Phases

### Dependencies FROM Phase 4 (what Phase 4 needs):
- None for annotation definitions (they are self-contained)

### Dependencies TO Phase 4 (what uses Phase 4):

| Phase | Task | Dependency Description |
|-------|------|------------------------|
| Phase 1 (Core) | CORE-001 | NavNode hierarchy (StackNode, TabNode, PaneNode, ScreenNode) - annotations map to these |
| Phase 2 (Renderer) | RENDER-004 | QuoVadisHost uses GeneratedScreenRegistry for rendering |
| Phase 3 (KSP) | KSP-002 | Class References Generator processes these annotations |
| Phase 3 (KSP) | KSP-003 | Graph Extractor generates code from annotations |

---

## File References

### Files to Create

| File Path | Description |
|-----------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt` | @Destination annotation |
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt` | @Stack annotation |
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt` | @Tab and @TabItem annotations |
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt` | @Pane, @PaneItem annotations and enums |
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt` | @Screen annotation |

### Referenced Documentation

- [INDEX.md](../INDEX.md) - Refactoring Plan Index
- [CORE-001: NavNode Hierarchy](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [RENDER-004: QuoVadisHost](../phase2-renderer/RENDER-004-quovadis-host.md) - Adaptive rendering
- [KSP-002: Class References Generator](../phase3-ksp/KSP-002-class-references.md) - Code generation
- [KSP-003: Graph Extractor](../phase3-ksp/KSP-003-graph-extractor.md) - Screen registry generation

---

## Estimated Complexity/Effort

| Task | Complexity | Time |
|------|------------|------|
| ANN-001: @Destination | Low | 0.5 days |
| ANN-002: @Stack | Low | 0.5 days |
| ANN-003: @Tab/@TabItem | Medium | 1 day |
| ANN-004: @Pane/@PaneItem | Medium | 1 day |
| ANN-005: @Screen | Low | 0.5 days |
| **Total** | **Low-Medium** | **3.5 days** |

---

## Implementation Notes

### Common Annotation Properties
- All annotations use `@Target(AnnotationTarget.CLASS)` except `@Screen` which uses `AnnotationTarget.FUNCTION`
- All annotations use `@Retention(AnnotationRetention.SOURCE)` for KSP processing
- All annotations are in `commonMain` for KMP compatibility

### Sealed Class Pattern
- `@Stack`, `@Tab`, and `@Pane` annotations leverage Kotlin's sealed class hierarchy
- Sealed subclasses automatically define navigation boundaries
- Each subclass must be annotated with `@Destination`

### Type Safety
- `rootGraph` parameters use `KClass<*>` for compile-time type reference
- KSP validates that referenced classes have proper annotations

### Platform Considerations
- Icon identifiers in `@TabItem` are platform-specific (Material icons, SF Symbols, etc.)
- All annotations accessible from all KMP platforms through commonMain
