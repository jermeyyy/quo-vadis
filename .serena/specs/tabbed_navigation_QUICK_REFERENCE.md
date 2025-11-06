# Tabbed Navigation Implementation - Quick Reference

## 📋 Planning Complete

Comprehensive 5-phase implementation plan for hierarchical tabbed navigation in quo-vadis library.

## 📚 Specification Documents

All specifications are in `.serena/specs/`:

| Document | Purpose | Size |
|----------|---------|------|
| `tabbed_navigation_MASTER_PLAN.md` | Overall plan, phases, timeline | Master |
| `tabbed_navigation_PHASE1_core_foundation_SPEC.md` | Core library components | ~1,160 LOC |
| `tabbed_navigation_PHASE2_compose_integration_SPEC.md` | Compose UI integration | ~2,010 LOC |
| `tabbed_navigation_PHASE3_ksp_annotations_SPEC.md` | KSP code generation | ~870 LOC |
| `tabbed_navigation_PHASE4_demo_app_SPEC.md` | Demo app refactoring | -120 LOC |
| `tabbed_navigation_PHASE5_documentation_SPEC.md` | Documentation & testing | ~8,400 LOC |

## ⏱️ Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Core Foundation | 3-4 days | 🔴 Not Started |
| Phase 2: Compose Integration | 3-4 days | 🔴 Not Started |
| Phase 3: KSP Annotations | 2-3 days | 🔴 Not Started |
| Phase 4: Demo App | 2-3 days | 🔴 Not Started |
| Phase 5: Documentation | 2-3 days | 🔴 Not Started |
| **Total** | **12-17 days** | 🟡 **Planning Complete** |

## 🎯 Key Features

### User Benefits
- ✅ **87% less boilerplate** - Annotations replace manual code
- ✅ **State preservation** - No more lost scroll positions
- ✅ **Type-safe** - Compile-time navigation safety
- ✅ **Smart back press** - Intuitive hierarchical behavior
- ✅ **All platforms** - Android, iOS, Desktop, Web

### Technical Benefits
- ✅ **No breaking changes** - Fully backward compatible
- ✅ **Reactive state** - StateFlow-based
- ✅ **Testable** - FakeTabNavigator for unit tests
- ✅ **Performant** - <16ms tab switching
- ✅ **Documented** - Complete guides and examples

## 🏗️ Architecture Highlight

### Before (Current Demo - Manual Workaround)
```kotlin
// ~150 lines of manual state management
class BottomNavigationContainer { ... }
class MainContainer { ... }
// State lost on tab switch via navigateAndReplace()
```

### After (New Annotation-Based API)
```kotlin
// ~20 lines with full state preservation
@TabGraph("main_tabs")
sealed class MainTabs : TabDefinition {
    @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeDestination::class)
    data object Home : MainTabs()
    
    @Tab(route = "profile", label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    data object Profile : MainTabs()
}

// Generated automatically:
// - MainTabsConfig: TabNavigatorConfig
// - MainTabsContainer: @Composable
// - buildMainTabsGraph(): NavigationGraph
```

## 🔍 Phase Details

### Phase 1: Core Foundation (Platform-Agnostic)
**What**: Core library interfaces and state management  
**Where**: `quo-vadis-core/src/commonMain/.../core/`  
**New Components**:
- `TabDefinition` interface
- `TabNavigatorState` class
- `BackPressHandler` interface
- `TabNavigatorConfig` data class
- Enhanced `Navigator` with child delegation

**Tests**: ~600 lines unit tests, ≥90% coverage

---

### Phase 2: Compose Integration
**What**: Compose UI components  
**Where**: `quo-vadis-core/src/commonMain/.../compose/`  
**New Components**:
- `TabbedNavHost` composable
- `TabNavigationContainer` composable
- `rememberTabNavigator()` functions
- `TabPredictiveBack` (Android/iOS)
- `TabScopedNavigator` wrapper

**Tests**: ~850 lines integration/UI tests, ≥85% coverage

---

### Phase 3: KSP Annotations
**What**: Code generation for minimal boilerplate  
**Where**: `quo-vadis-annotations/`, `quo-vadis-ksp/`  
**New Annotations**:
- `@TabGraph(name)` - Marks tab container
- `@Tab(route, label, icon, rootGraph)` - Defines tab
- `@TabContent(tabClass)` - Custom content (optional)

**Generated**:
- Configuration objects
- Container composables
- Graph builder functions

**Tests**: ~950 lines KSP processor tests

---

### Phase 4: Demo App Refactoring
**What**: Showcase new API in demo app  
**Where**: `composeApp/src/commonMain/.../demo/`  
**Changes**:
- ❌ Remove `BottomNavigationContainer.kt` (manual workaround)
- ❌ Remove `MainContainer.kt` (manual container)
- ➕ Add `tabs/MainTabs.kt` (annotation-based)
- ➕ Add `tabs/NestedTabsExample.kt` (nested demo)
- ✏️ Simplify all screen files (remove dual-mode)

**Result**: Net -120 lines (cleaner code!)

---

### Phase 5: Documentation & Testing
**What**: Production-ready quality assurance  
**Where**: `quo-vadis-core/docs/`  
**Deliverables**:
- `TABBED_NAVIGATION.md` (~2,500 lines)
- `MIGRATION_TO_TABBED_NAVIGATION.md` (~1,000 lines)
- Updated `API_REFERENCE.md` (+500 lines)
- Demo `tabs/README.md` (~400 lines)
- Comprehensive test suite (≥85% overall coverage)
- Performance benchmarks (<16ms, <50MB)

## ✅ Success Criteria

### Must Have
- ✅ Multiple independent tab stacks
- ✅ State preserved across tab switches
- ✅ Hierarchical back press working
- ✅ Type-safe tab definitions
- ✅ KSP code generation functional
- ✅ All platforms supported
- ✅ Test coverage ≥85%
- ✅ Complete documentation

### Performance Targets
- ✅ Tab switching: <16ms (60fps)
- ✅ Memory usage: <50MB for 8 tabs
- ✅ No dropped frames
- ✅ Smooth animations

## 🚀 Next Steps

1. **Review Phase**: User reviews all specifications
2. **Approval**: User approves implementation plan
3. **Phase 1 Start**: Begin core foundation implementation
4. **Sequential Execution**: Complete phases 1→2→3→4→5
5. **Release**: Final release after Phase 5 completion

## 📖 Related Documentation

### Existing Memories
- `architecture_patterns` - Quo-vadis design principles
- `bottom_navigation_state_retention` - Current manual workaround
- `project_overview` - Overall project structure
- `codebase_structure` - File organization
- `ksp_refactoring_type_safety` - Existing KSP infrastructure

### Reference Materials
- `tabbed_navigation_SPEC.md` (user-provided architectural concept)
- `.github/instructions/copilot.instructions.md` (development guidelines)

## 💡 Key Design Decisions

1. **No Breaking Changes**: Fully backward compatible with existing API
2. **Platform-Agnostic Core**: All logic in `commonMain`, platform code only for gestures
3. **Reactive State**: StateFlow throughout, no callbacks
4. **Hierarchical Delegation**: Clean back press delegation through navigation tree
5. **Type Safety**: Sealed classes for tab definitions, compile-time safety
6. **Code Generation**: KSP for 87% boilerplate reduction
7. **Testing First**: Comprehensive test suite with ≥85% coverage

## 🎨 Example Use Case (Demo App)

### Current Problem
Bottom navigation using `navigateAndReplace()` loses state:
- Scroll position lost when switching tabs
- Form inputs cleared
- Navigation stack destroyed

### New Solution
Annotation-based tabs with automatic state preservation:
```kotlin
@TabGraph("main_tabs")
sealed class MainTabs : TabDefinition {
    @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeGraphDestination::class)
    data object Home : MainTabs()
    // ... more tabs
}

// Usage (generated composable)
MainTabsContainer(parentNavigator = navigator)
```

**Result**:
- ✅ State preserved automatically
- ✅ Each tab maintains independent navigation stack
- ✅ Smooth transitions with animations
- ✅ Intelligent back press behavior

## 📊 Project Scope

| Category | Count |
|----------|-------|
| New Files | ~25 |
| Modified Files | ~15 |
| New Code | ~12,450 lines |
| Documentation | ~4,400 lines |
| Tests | ~4,000 lines |
| **Total New Content** | **~20,850 lines** |

## ⚠️ Risks & Mitigations

| Risk | Level | Mitigation |
|------|-------|------------|
| Back press complexity | High | Extensive testing, clear state machine docs |
| State memory pressure | High | Lazy loading, configurable limits |
| KSP generation | Medium | Manual implementation first, then generate |
| Animation conflicts | Medium | Separate coordinators, precedence rules |
| API consistency | Low | Follow quo-vadis conventions |
| Platform gestures | Low | Reuse existing infrastructure |

## 🔧 Verification Commands

```bash
# Phase 1 Verification
./gradlew :quo-vadis-core:build
./gradlew :quo-vadis-core:test

# Phase 2 Verification
./gradlew :quo-vadis-core:test
./gradlew :quo-vadis-core:connectedAndroidTest

# Phase 3 Verification
./gradlew :quo-vadis-ksp:test
# Check generated code in build/generated/ksp/

# Phase 4 Verification
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:run

# Phase 5 Verification
./gradlew :quo-vadis-core:koverHtmlReport
# Open build/reports/kover/html/index.html
```

## 📝 Status

**Current**: 🟡 Planning Complete - Awaiting User Approval  
**Created**: November 6, 2025  
**Estimated Completion**: 12-17 days after approval  
**Memory Saved**: `tabbed_navigation_implementation_plan`

---

**Ready to begin implementation upon user approval!** 🚀
