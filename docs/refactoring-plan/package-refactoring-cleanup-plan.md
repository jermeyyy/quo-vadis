# Package Refactoring Cleanup Plan

## Executive Summary

The Developer agent completed a major package restructuring of `quo-vadis-core`, reorganizing navigation, compose, and registry packages. However, significant cleanup work remains:

1. **Duplicate files exist** at both old and new locations
2. **Broken imports** still reference old package paths  
3. **Orphaned legacy code** should be removed (no backward compatibility needed)
4. **Some tests don't compile** due to import issues

This plan provides a systematic approach to complete the cleanup.

---

## Phase 1: Delete Duplicate/Orphaned Files

### 1.1 Navigation Package Root Duplicates

Files that exist in both old location (`navigation/`) and new location (`navigation/node/`, `navigation/internal/`):

| File to Delete | Reason |
|----------------|--------|
| `navigation/NavNode.kt` | Moved to `navigation/node/NavNode.kt` |
| `navigation/ScreenNode.kt` | Moved to `navigation/node/ScreenNode.kt` |
| `navigation/StackNode.kt` | Moved to `navigation/node/StackNode.kt` |
| `navigation/TabNode.kt` | Moved to `navigation/node/TabNode.kt` |
| `navigation/PaneNode.kt` | Moved to `navigation/node/PaneNode.kt` |
| `navigation/NavDestination.kt` | Moved to `navigation/destination/NavDestination.kt` |
| `navigation/DeepLink.kt` | Moved to `navigation/destination/DeepLink.kt` |
| `navigation/DeepLinkResult.kt` | Moved to `navigation/destination/DeepLinkResult.kt` |
| `navigation/Navigator.kt` | Moved to `navigation/navigator/Navigator.kt` |
| `navigation/PaneNavigator.kt` | Moved to `navigation/navigator/PaneNavigator.kt` |
| `navigation/NavigationTransition.kt` | Moved to `navigation/transition/NavigationTransition.kt` |
| `navigation/TransitionState.kt` | Moved to `navigation/transition/TransitionState.kt` |
| `navigation/NavKeyGenerator.kt` | Moved to `navigation/internal/NavKeyGenerator.kt` |

**Command:**
```bash
cd quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core
rm -f navigation/NavNode.kt navigation/ScreenNode.kt navigation/StackNode.kt \
      navigation/TabNode.kt navigation/PaneNode.kt navigation/NavDestination.kt \
      navigation/DeepLink.kt navigation/DeepLinkResult.kt navigation/Navigator.kt \
      navigation/PaneNavigator.kt navigation/NavigationTransition.kt \
      navigation/TransitionState.kt navigation/NavKeyGenerator.kt
```

### 1.2 Delete Entire `navigation/tree/` Directory

The `navigation/tree/` folder is duplicated by `navigation/internal/tree/`:

| Directory to Delete | New Location |
|---------------------|--------------|
| `navigation/tree/operations/` | `navigation/internal/tree/operations/` |
| `navigation/tree/result/` | `navigation/internal/tree/result/` |
| `navigation/tree/config/` | `navigation/internal/tree/config/` |
| `navigation/tree/util/` | `navigation/internal/tree/util/` |
| `navigation/tree/TreeMutator.kt` | `navigation/internal/tree/TreeMutator.kt` |
| `navigation/tree/TreeNavigator.kt` | `navigation/internal/tree/TreeNavigator.kt` |

**Command:**
```bash
rm -rf quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/tree/
```

### 1.3 Delete Duplicate `dsl/registry/` Files

Registry files moved from `dsl/registry/` to `registry/` and `registry/internal/`:

| File to Delete | New Location |
|----------------|--------------|
| `dsl/registry/CompositeContainerRegistry.kt` | `registry/internal/CompositeContainerRegistry.kt` |
| `dsl/registry/CompositeDeepLinkRegistry.kt` | `registry/internal/CompositeDeepLinkRegistry.kt` |
| `dsl/registry/CompositeScopeRegistry.kt` | `registry/internal/CompositeScopeRegistry.kt` |
| `dsl/registry/CompositeScreenRegistry.kt` | `registry/internal/CompositeScreenRegistry.kt` |
| `dsl/registry/CompositeTransitionRegistry.kt` | `registry/internal/CompositeTransitionRegistry.kt` |
| `dsl/registry/RuntimeDeepLinkRegistry.kt` | `registry/internal/RuntimeDeepLinkRegistry.kt` |
| `dsl/registry/ScopeRegistry.kt` | `registry/ScopeRegistry.kt` |
| `dsl/registry/ScreenRegistry.kt` | `registry/ScreenRegistry.kt` |
| `dsl/registry/ContainerRegistry.kt` | `registry/ContainerRegistry.kt` |
| `dsl/registry/ContainerInfo.kt` | Part of `registry/ContainerRegistry.kt` |
| `dsl/registry/TransitionRegistry.kt` | `registry/TransitionRegistry.kt` |
| `dsl/registry/DeepLinkRegistry.kt` | `registry/DeepLinkRegistry.kt` |
| `dsl/registry/PaneRoleRegistry.kt` | `registry/PaneRoleRegistry.kt` |
| `dsl/registry/BackHandlerRegistry.kt` | `registry/BackHandlerRegistry.kt` |
| `dsl/registry/RouteRegistry.kt` | `registry/RouteRegistry.kt` |

**Command:**
```bash
rm -rf quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/registry/
```

### 1.4 Delete Old `compose/` Subdirectories

| Directory to Delete | New Location |
|---------------------|--------------|
| `compose/render/` | `compose/internal/render/` |
| `compose/navback/` | `compose/internal/navback/` |
| `compose/animation/` | `compose/internal/` and `compose/transition/` |
| `compose/wrapper/` | `compose/scope/` |

**Command:**
```bash
cd quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose
rm -rf render/ navback/ animation/ wrapper/
```

### 1.5 Delete Platform-Specific Duplicates

Check and remove duplicates in platform source sets:

```bash
# Android
rm -f quo-vadis-core/src/androidMain/kotlin/.../compose/navback/*.kt
rm -f quo-vadis-core/src/androidMain/kotlin/.../compose/wrapper/*.kt

# iOS  
rm -f quo-vadis-core/src/iosMain/kotlin/.../compose/navback/IOSEdgeSwipeInput.kt

# Desktop, JS, WASM - verify no duplicates remain
```

---

## Phase 2: Fix Broken Imports

### 2.1 Old Import Paths → New Import Paths

| Old Import | New Import |
|------------|------------|
| `com.jermey.quo.vadis.core.navigation.NavNode` | `com.jermey.quo.vadis.core.navigation.node.NavNode` |
| `com.jermey.quo.vadis.core.navigation.ScreenNode` | `com.jermey.quo.vadis.core.navigation.node.ScreenNode` |
| `com.jermey.quo.vadis.core.navigation.StackNode` | `com.jermey.quo.vadis.core.navigation.node.StackNode` |
| `com.jermey.quo.vadis.core.navigation.TabNode` | `com.jermey.quo.vadis.core.navigation.node.TabNode` |
| `com.jermey.quo.vadis.core.navigation.PaneNode` | `com.jermey.quo.vadis.core.navigation.node.PaneNode` |
| `com.jermey.quo.vadis.core.navigation.NavDestination` | `com.jermey.quo.vadis.core.navigation.destination.NavDestination` |
| `com.jermey.quo.vadis.core.navigation.DeepLink` | `com.jermey.quo.vadis.core.navigation.destination.DeepLink` |
| `com.jermey.quo.vadis.core.navigation.Navigator` | `com.jermey.quo.vadis.core.navigation.navigator.Navigator` |
| `com.jermey.quo.vadis.core.navigation.NavigationTransition` | `com.jermey.quo.vadis.core.navigation.transition.NavigationTransition` |
| `com.jermey.quo.vadis.core.navigation.TransitionState` | `com.jermey.quo.vadis.core.navigation.transition.TransitionState` |
| `com.jermey.quo.vadis.core.navigation.NavKeyGenerator` | `com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator` |
| `com.jermey.quo.vadis.core.navigation.tree.*` | `com.jermey.quo.vadis.core.navigation.internal.tree.*` |
| `com.jermey.quo.vadis.core.dsl.registry.*` | `com.jermey.quo.vadis.core.registry.*` |
| `com.jermey.quo.vadis.core.compose.wrapper.*` | `com.jermey.quo.vadis.core.compose.scope.*` |
| `com.jermey.quo.vadis.core.compose.render.*` | `com.jermey.quo.vadis.core.compose.internal.render.*` |
| `com.jermey.quo.vadis.core.compose.navback.*` | `com.jermey.quo.vadis.core.compose.internal.navback.*` |
| `com.jermey.quo.vadis.core.compose.animation.*` | `com.jermey.quo.vadis.core.compose.internal.*` or `compose.transition.*` |

### 2.2 Files Requiring Import Fixes

#### DSL Module Files
- [ ] `dsl/DslNavigationConfig.kt`
- [ ] `dsl/DslContainerRegistry.kt`
- [ ] `dsl/DslScreenRegistry.kt`
- [ ] `dsl/DslTransitionRegistry.kt`
- [ ] `dsl/DslScopeRegistry.kt`
- [ ] `dsl/DslPaneRoleRegistry.kt`
- [ ] `dsl/DslDeepLinkRegistry.kt`

#### Navigation Config Files
- [ ] `navigation/config/CompositeNavigationConfig.kt`
- [ ] `navigation/config/NavigationConfig.kt`

#### Compose Internal Files
- [ ] `compose/internal/render/NavTreeRenderer.kt`
- [ ] `compose/internal/render/AnimatedNavContent.kt`
- [ ] `compose/internal/render/PaneRenderer.kt`
- [ ] `compose/internal/render/TabRenderer.kt`
- [ ] `compose/internal/render/StackRenderer.kt`
- [ ] `compose/internal/render/ScreenRenderer.kt`
- [ ] `compose/internal/navback/NavigateBackHandler.kt`
- [ ] `compose/internal/navback/CascadeBackState.kt`
- [ ] `compose/NavigationHost.kt`

---

## Phase 3: Fix Test Files

### 3.1 Test Files with Import Issues

All test files in `quo-vadis-core/src/commonTest/kotlin/` need import updates:

#### Core Navigation Tests
- [ ] `navigation/core/NavNodeTest.kt`
- [ ] `navigation/core/TreeMutatorPushTest.kt`
- [ ] `navigation/core/TreeMutatorPopTest.kt`
- [ ] `navigation/core/TreeMutatorTabTest.kt`
- [ ] `navigation/core/TreeMutatorPaneTest.kt`
- [ ] `navigation/core/TreeMutatorScopeTest.kt`
- [ ] `navigation/core/TreeMutatorStackScopeTest.kt`
- [ ] `navigation/core/TreeMutatorEdgeCasesTest.kt`
- [ ] `navigation/core/TreeMutatorBackHandlingTest.kt`
- [ ] `navigation/core/TreeNavigatorTest.kt`
- [ ] `navigation/core/ScopeRegistryTest.kt`
- [ ] `navigation/core/DeepLinkTest.kt`
- [ ] `navigation/core/DeepLinkRegistryTest.kt`

#### Compose Tests
- [ ] `navigation/compose/hierarchical/NavTreeRendererTest.kt`
- [ ] `navigation/compose/hierarchical/AnimatedNavContentTest.kt`
- [ ] `navigation/compose/hierarchical/PaneRendererTest.kt`
- [ ] `navigation/compose/hierarchical/TabRendererTest.kt`
- [ ] `navigation/compose/hierarchical/StackRendererTest.kt`
- [ ] `navigation/compose/hierarchical/ScreenRendererTest.kt`
- [ ] `navigation/compose/hierarchical/PredictiveBackContentTest.kt`
- [ ] `navigation/compose/hierarchical/BackAnimationControllerTest.kt`
- [ ] `navigation/compose/navback/BackNavigationEventTest.kt`
- [ ] `navigation/compose/navback/ScreenNavigationInfoTest.kt`
- [ ] `navigation/compose/gesture/CascadeBackStateTest.kt`
- [ ] `navigation/compose/registry/ContainerRegistryTest.kt`
- [ ] `navigation/compose/BackHandlerRegistryTest.kt`

#### Test Utilities
- [ ] `navigation/FakeNavRenderScope.kt`
- [ ] `navigation/FakeNavigator.kt`
- [ ] `navigation/FakeSaveableStateHolder.kt`
- [ ] `navigation/EmptyScreenRegistry.kt`
- [ ] `navigation/testing/NavigatorTestHelpers.kt`
- [ ] `navigation/testing/FakeNavRenderScopeTest.kt`

#### Composite Registry Tests
- [ ] `navigation/CompositeContainerRegistryTest.kt`
- [ ] `navigation/CompositeScreenRegistryTest.kt`

### 3.2 Test Import Fix Pattern

For each test file, apply these import replacements:

```kotlin
// Old imports
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry

// New imports
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.registry.ScopeRegistry
```

---

## Phase 4: Update KDoc References

Update KDoc comments that reference old package paths:

### Files with KDoc References to Update
- [ ] `registry/ScopeRegistry.kt` - References `com.jermey.quo.vadis.core.dsl.registry.*`
- [ ] `registry/ContainerRegistry.kt` - References `dsl.registry.*`
- [ ] `navigation/navigator/Navigator.kt` - References `tree.TreeMutator`
- [ ] `compose/scope/NavRenderScope.kt` - References old compose paths

---

## Phase 5: Verification

### 5.1 Build Verification
```bash
# Clean build
./gradlew clean

# Build core module
./gradlew :quo-vadis-core:build

# Build KSP module
./gradlew :quo-vadis-ksp:build

# Build all
./gradlew build
```

### 5.2 Test Verification
```bash
# Run all tests
./gradlew :quo-vadis-core:allTests

# Run desktop tests specifically
./gradlew :quo-vadis-core:desktopTest
```

### 5.3 Static Analysis
```bash
# Detekt
./gradlew detekt

# Regenerate baselines if needed
./gradlew detektBaseline
```

### 5.4 Import Verification

Search for any remaining old imports:
```bash
# Check for old navigation.* imports (should be navigation.node.*, etc.)
grep -r "import com.jermey.quo.vadis.core.navigation.NavNode" --include="*.kt"
grep -r "import com.jermey.quo.vadis.core.navigation.ScreenNode" --include="*.kt"

# Check for old dsl.registry.* imports
grep -r "import com.jermey.quo.vadis.core.dsl.registry" --include="*.kt"

# Check for old compose.wrapper.* imports
grep -r "import com.jermey.quo.vadis.core.compose.wrapper" --include="*.kt"

# Check for old compose.render.* imports
grep -r "import com.jermey.quo.vadis.core.compose.render" --include="*.kt"
```

---

## Execution Order

1. **Phase 1** - Delete all duplicate/orphaned files first (prevents confusion)
2. **Phase 2** - Fix imports in source files
3. **Phase 3** - Fix imports in test files
4. **Phase 4** - Update KDoc references
5. **Phase 5** - Verify everything builds and tests pass

---

## Estimated Effort

| Phase | Files | Estimated Time |
|-------|-------|----------------|
| Phase 1: Delete duplicates | ~40 files | 15 minutes |
| Phase 2: Fix source imports | ~15 files | 30 minutes |
| Phase 3: Fix test imports | ~30 files | 45 minutes |
| Phase 4: Update KDoc | ~10 files | 15 minutes |
| Phase 5: Verification | - | 15 minutes |
| **Total** | ~95 files | **~2 hours** |

---

## Success Criteria

- [ ] `./gradlew build` completes without errors
- [ ] `./gradlew :quo-vadis-core:allTests` passes all tests
- [ ] No files exist in old package locations
- [ ] No imports reference old package paths
- [ ] `grep` verification commands return no results
- [ ] Detekt runs clean (with updated baseline if needed)
