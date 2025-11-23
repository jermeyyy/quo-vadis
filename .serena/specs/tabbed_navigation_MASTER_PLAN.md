# Tabbed Navigation Implementation - Master Plan

## Executive Summary

This document outlines the complete implementation plan for adding **hierarchical tabbed navigation** support to the `quo-vadis-core` navigation library. The feature enables apps to have multiple, independent navigation stacks (tabs) within a parent navigation context, with proper state preservation, back press delegation, and type-safe API.

**Goal**: Transform quo-vadis from a single-backstack library into a hierarchical navigation system supporting nested, parallel navigation stacks (tabs) while maintaining all existing principles: type safety, reactivity, MVI support, multiplatform compatibility, and testability.

## Core Problem Statement

The current quo-vadis library manages a single, linear navigation backstack. When implementing tabbed interfaces (like bottom navigation):

### Current Limitations
1. **State Loss**: Switching tabs via `navigator.navigateAndReplace()` destroys previous screens
2. **Single Stack**: Cannot maintain parallel navigation states for each tab
3. **Complex Back Handling**: No built-in delegation for hierarchical back press
4. **Manual Management**: Developers must implement state preservation manually (as seen in the demo app workaround)

### Desired Solution
1. **Parallel Stacks**: Each tab maintains its own navigation stack independently
2. **State Preservation**: Switching tabs preserves all screen state (scroll, inputs, etc.)
3. **Hierarchical Back**: Back press intelligently delegates through: tab stack → tab switch → parent navigator
4. **Type-Safe API**: Maintain quo-vadis principles with compile-time safety
5. **KSP Integration**: Simplify implementation with annotations

## Architectural Vision

### Nested Navigation Hierarchy
```
MainNavigator (App-level)
  ├─ LoginFlow
  ├─ MainScreen (TabNavigator Container)
  │   └─ TabNavigator (Tab-level)
  │       ├─ HomeTab
  │       │   └─ Stack: [HomeRoot, Detail(1), Detail(2)]
  │       ├─ ExploreTab
  │       │   └─ Stack: [ExploreRoot]
  │       ├─ ProfileTab
  │       │   └─ Stack: [ProfileRoot, EditProfile]
  │       └─ SettingsTab
  │           └─ Stack: [SettingsRoot]
  └─ SettingsFlow
```

### Back Press Flow
```
User presses back
  ↓
Main Navigator receives event
  ↓
Delegates to active child (TabNavigator)
  ↓
TabNavigator.onBack() logic:
  ├─ Current tab stack > 1? → Pop from tab stack (CONSUMED)
  ├─ Not on primary tab? → Switch to primary tab (CONSUMED)
  └─ At primary tab root? → Return false (NOT CONSUMED)
        ↓
Main Navigator pops TabNavigator container (exit to previous screen)
```

## Implementation Phases

This implementation is divided into **5 phases**, each with a detailed SPEC document:

### Phase 1: Core Library - Nested Navigator Foundation
**File**: `tabbed_navigation_PHASE1_core_foundation_SPEC.md`

**Scope**: Core library components for nested navigation
- `TabNavigatorState` - Manages parallel tab stacks
- `TabDefinition` - Type-safe tab configuration
- `ChildNavigator` interface - Delegation protocol
- `BackPressHandler` interface - Hierarchical back handling
- Enhanced `Navigator` with child delegation

**Deliverables**:
- New interfaces in `quo-vadis-core/src/commonMain/.../core/`
- Updated `Navigator` with child navigation support
- Unit tests with `FakeNavigator`

**Dependencies**: None (foundational)

---

### Phase 2: Compose UI Integration
**File**: `tabbed_navigation_PHASE2_compose_integration_SPEC.md`

**Scope**: Composable components for tab navigation UI
- `TabbedNavHost` - Container for tab navigation
- `rememberTabNavigatorState()` - State management
- `TabNavigationContainer` - Visibility management with animations
- Predictive back gesture support for tabs
- Shared element transitions across tab switches

**Deliverables**:
- New composables in `quo-vadis-core/src/commonMain/.../compose/`
- Animation support for tab switching
- Integration with existing `GraphNavHost`

**Dependencies**: Phase 1 (core foundation)

---

### Phase 3: KSP Annotations & Code Generation
**File**: `tabbed_navigation_PHASE3_ksp_annotations_SPEC.md`

**Scope**: Simplify tab navigation setup with annotations
- `@TabGraph` annotation - Marks sealed class as tab container
- `@Tab` annotation - Defines individual tabs with metadata
- `@TabContent` annotation - Links Composables to tabs
- Enhanced KSP processor for tab graph generation
- Generate `buildTabGraph()` functions

**Deliverables**:
- New annotations in `quo-vadis-annotations`
- Updated KSP processor in `quo-vadis-ksp`
- Code generation for tab scaffolding

**Dependencies**: Phase 1, Phase 2

---

### Phase 4: Demo App Refactoring
**File**: `tabbed_navigation_PHASE4_demo_app_SPEC.md`

**Scope**: Migrate demo app to use new tab navigation API
- Remove manual `BottomNavigationContainer` workaround
- Replace with `TabbedNavHost` + annotations
- Demonstrate best practices
- Add new navigation patterns:
  - Nested tabs within tabs
  - Deep linking to tab content
  - Tab restoration from saved state

**Deliverables**:
- Refactored `composeApp` demo
- New `TabsDestination` using `@TabGraph`
- Documentation updates
- Test coverage for tab scenarios

**Dependencies**: Phase 1, Phase 2, Phase 3

---

### Phase 5: Documentation & Testing
**File**: `tabbed_navigation_PHASE5_documentation_SPEC.md`

**Scope**: Complete documentation and comprehensive testing
- Architecture documentation (`TABBED_NAVIGATION.md`)
- API reference updates
- Migration guide for existing apps
- Test suite:
  - Unit tests for all components
  - Integration tests for nested navigation
  - UI tests for tab switching and back press
  - Multiplatform tests (Android, iOS, Desktop, Web)

**Deliverables**:
- Documentation in `quo-vadis-core/docs/`
- Test coverage ≥ 85%
- Migration examples
- Best practices guide

**Dependencies**: Phase 1, Phase 2, Phase 3, Phase 4

## Technical Principles

### 1. Type Safety (Maintained)
```kotlin
@TabGraph("main_tabs")
sealed class MainTabs : TabDestination {
    @Tab(
        route = "home",
        label = "Home",
        icon = "home"
    )
    data object HomeTab : MainTabs() {
        override val rootDestination = HomeDestination.Root
    }
    
    @Tab(route = "profile", label = "Profile", icon = "person")
    data object ProfileTab : MainTabs() {
        override val rootDestination = ProfileDestination.Root
    }
}
```

### 2. Reactive State (Enhanced)
- `TabNavigatorState.selectedTab: StateFlow<TabDefinition>`
- `TabNavigatorState.tabStacks: StateFlow<Map<TabDefinition, List<Route>>>`
- Each tab's stack independently observable
- Tab switch events via `SharedFlow`

### 3. Modularization (Preserved)
- Tab graphs are `NavigationGraph` instances
- Each tab can be a complete feature module
- Clear boundaries via `TabDefinition` interface

### 4. MVI Support (Extended)
```kotlin
sealed interface TabNavigationIntent : NavigationIntent {
    data class SelectTab(val tab: TabDefinition) : TabNavigationIntent
    data class NavigateInTab(val route: Route) : TabNavigationIntent
}

sealed interface TabNavigationEffect : NavigationEffect {
    data class TabSwitched(val from: TabDefinition, val to: TabDefinition) : TabNavigationEffect
}
```

### 5. Testability (Maintained)
```kotlin
val fakeTabNavigator = FakeTabNavigator()
fakeTabNavigator.selectTab(ProfileTab)
fakeTabNavigator.navigateInTab(ProfileEdit)
assertTrue(fakeTabNavigator.currentTab == ProfileTab)
assertEquals(2, fakeTabNavigator.getTabStack(ProfileTab).size)
```

### 6. Platform Agnostic (Maintained)
- Core logic in `commonMain`
- Platform-specific predictive back in `androidMain`/`iosMain`
- Shared element transitions work across tabs on all platforms

## Success Criteria

### Functional Requirements
- ✅ Multiple independent tab stacks maintained simultaneously
- ✅ State preservation across tab switches (scroll, inputs, etc.)
- ✅ Intelligent back press delegation (tab → tab switch → parent)
- ✅ Type-safe tab definitions and navigation
- ✅ Smooth animations between tabs (fade, slide, etc.)
- ✅ Deep linking directly to tab content
- ✅ Saved state restoration (survive process death)
- ✅ Nested tabs support (tabs within tabs)

### Non-Functional Requirements
- ✅ No breaking changes to existing quo-vadis API
- ✅ Performance: Tab switching < 16ms (60fps)
- ✅ Memory: Reasonable with 4-8 tabs in memory
- ✅ All platforms: Android, iOS, Desktop, Web
- ✅ Test coverage ≥ 85%
- ✅ Documentation complete and clear
- ✅ KSP code generation reduces boilerplate by ≥ 70%

### Demo App Requirements
- ✅ Bottom navigation with 4 tabs
- ✅ Each tab with deep navigation stacks
- ✅ Nested tabs demonstration
- ✅ Deep link examples
- ✅ Predictive back gestures
- ✅ Shared element transitions between tab screens

## Timeline Estimates

| Phase | Complexity | Estimated Effort | Dependencies |
|-------|-----------|------------------|--------------|
| Phase 1 | High | 3-4 days | None |
| Phase 2 | High | 3-4 days | Phase 1 |
| Phase 3 | Medium | 2-3 days | Phase 1, 2 |
| Phase 4 | Medium | 2-3 days | Phase 1, 2, 3 |
| Phase 5 | Medium | 2-3 days | All phases |
| **Total** | | **12-17 days** | Sequential |

## Risk Assessment

### High Risk
1. **Back Press Complexity**: Hierarchical delegation can be tricky
   - *Mitigation*: Extensive testing, clear state machine documentation
2. **State Management**: Memory pressure with many tabs
   - *Mitigation*: Lazy loading, configurable stack depth limits

### Medium Risk
3. **KSP Generation**: Complex code generation for nested structures
   - *Mitigation*: Start with manual implementation, then generate equivalent code
4. **Animation Conflicts**: Tab transitions vs predictive back
   - *Mitigation*: Separate animation coordinators, clear precedence rules

### Low Risk
5. **API Design**: Maintaining consistency with existing patterns
   - *Mitigation*: Follow established quo-vadis conventions
6. **Multiplatform**: Platform-specific gesture handling
   - *Mitigation*: Use existing predictive back infrastructure

## Breaking Changes Assessment

**None expected**. This is an **additive feature**:
- Existing `Navigator` API remains unchanged
- New `TabNavigator` is opt-in
- Existing apps continue working without modification
- Migration is gradual and optional

## Future Enhancements (Post-Implementation)

1. **Tab Lazy Loading**: Load tab content on-demand
2. **Tab Offloading**: Remove inactive tabs from memory after timeout
3. **Tab Reordering**: User-customizable tab order
4. **Dynamic Tabs**: Add/remove tabs at runtime
5. **Tab Badges**: Notification counts on tab icons
6. **Tab Persistence**: Save/restore tab state to disk
7. **Tab Animations**: Custom enter/exit animations per tab
8. **Tab Gestures**: Swipe between tabs (iOS/Android native feel)

## Related Documentation

- **Spec Attachment**: `tabbed_navigation_SPEC.md` (architectural foundation)
- **Phase 1 Spec**: `tabbed_navigation_PHASE1_core_foundation_SPEC.md`
- **Phase 2 Spec**: `tabbed_navigation_PHASE2_compose_integration_SPEC.md`
- **Phase 3 Spec**: `tabbed_navigation_PHASE3_ksp_annotations_SPEC.md`
- **Phase 4 Spec**: `tabbed_navigation_PHASE4_demo_app_SPEC.md`
- **Phase 5 Spec**: `tabbed_navigation_PHASE5_documentation_SPEC.md`
- **Memory**: `bottom_navigation_state_retention` (current workaround)
- **Memory**: `architecture_patterns` (quo-vadis design principles)

## Sign-off Requirements

Before proceeding to implementation:
1. ✅ User approval of this master plan
2. ✅ Confirmation of phased approach
3. ✅ Agreement on success criteria
4. ✅ Review of each phase SPEC document

---

**Status**: 🟡 Planning Complete - Awaiting User Approval

**Next Steps**: 
1. User reviews this master plan
2. User reviews individual phase SPECs
3. Upon approval, begin Phase 1 implementation
