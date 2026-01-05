# Package Refactoring Cleanup Plan

## Status: Created (January 5, 2025)

## Plan Location
`docs/refactoring-plan/package-refactoring-cleanup-plan.md`

## Summary
Created comprehensive plan to complete package refactoring cleanup:

### Requirements Addressed:
1. **Fix tests that don't build/pass** - Plan includes Phase 3 with 30+ test files to fix
2. **Remove old files/duplicate code** - Plan includes Phase 1 with ~40 files to delete
3. **No backward compatibility** - Plan explicitly removes all old files

### Plan Phases:
1. **Delete duplicates** (~40 files) - Remove files at old locations
2. **Fix source imports** (~15 files) - Update imports in source files
3. **Fix test imports** (~30 files) - Update imports in test files  
4. **Update KDoc** (~10 files) - Fix documentation references
5. **Verification** - Build, test, grep verification

### Key Directories to Remove:
- `navigation/tree/` (duplicates `navigation/internal/tree/`)
- `dsl/registry/` (duplicates `registry/` and `registry/internal/`)
- `compose/render/` (duplicates `compose/internal/render/`)
- `compose/navback/` (duplicates `compose/internal/navback/`)
- `compose/animation/` (duplicates `compose/internal/`)
- `compose/wrapper/` (duplicates `compose/scope/`)

### Import Migration Patterns:
- `navigation.NavNode` → `navigation.node.NavNode`
- `navigation.NavDestination` → `navigation.destination.NavDestination`
- `navigation.Navigator` → `navigation.navigator.Navigator`
- `navigation.tree.*` → `navigation.internal.tree.*`
- `dsl.registry.*` → `registry.*`
- `compose.wrapper.*` → `compose.scope.*`
- `compose.render.*` → `compose.internal.render.*`

### Estimated Effort: ~2 hours

### Success Criteria:
- Build completes without errors
- All tests pass
- No files at old locations
- No old imports remain
