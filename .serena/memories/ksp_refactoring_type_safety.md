# KSP Module Refactoring - Type-Safe Class References

## Overview
Refactored the `quo-vadis-ksp` module to use type-safe class references from `quo-vadis-core` instead of hardcoded string-based imports and class names in KotlinPoet API invocations.

## Changes Made

### 1. Added Dependency
**File**: `quo-vadis-ksp/build.gradle.kts`
- Added `implementation(projects.quoVadisCore)` dependency
- Initially tried `compileOnly` but KSP processor needs runtime access to classes for reflection
- Using `implementation` ensures classes are available during KSP processing

### 2. Created QuoVadisClassNames Helper
**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisClassNames.kt`
- Central object providing type-safe ClassName references
- Uses Kotlin reflection (`::class.toClassName()`) to derive class names
- Provides constants for:
  - `NAVIGATOR` - Navigator interface
  - `NAVIGATION_GRAPH` - NavigationGraph class
  - `NAVIGATION_GRAPH_BUILDER` - NavigationGraphBuilder class
  - `NAVIGATION_TRANSITION` - NavigationTransition interface
  - `ROUTE_REGISTRY` - RouteRegistry object
  - `TRANSITION_SCOPE` - TransitionScope class (from compose module)

### 3. Refactored Generators
**Files Updated**:
- `DestinationExtensionsGenerator.kt`
- `GraphBuilderGenerator.kt`
- `GraphGenerator.kt`

**Changes**:
- Replaced all hardcoded string references like `ClassName("com.jermey.quo.vadis.core.navigation.core", "Navigator")`
- Now use `QuoVadisClassNames.NAVIGATOR` instead
- Fixed line length issues to comply with detekt rules

## Benefits

1. **Refactoring Safety**: If any class in `quo-vadis-core` is renamed or moved, the KSP module will fail to compile, alerting developers to update the processor
2. **IDE Support**: Proper Kotlin references enable IDE navigation and refactoring tools
3. **Type Safety**: Compile-time verification of class existence and package structure
4. **Maintainability**: Single source of truth for class references in `QuoVadisClassNames`
5. **DRY Principle**: No duplication of package/class name strings throughout codebase

## Verification
- ✅ `./gradlew :quo-vadis-ksp:build` passes
- ✅ `./gradlew :composeApp:assembleDebug` passes
- ✅ `./gradlew test` passes
- ✅ Generated code uses proper imports (verified in generated Registry files)

## Technical Notes

### Why `implementation` instead of `compileOnly`?
The KSP processor uses Kotlin reflection (`::class.toClassName()`) at runtime to extract package and class names from the core module classes. This requires the classes to be on the classpath during KSP execution, not just at compile time.

### Extension Pattern
The `toClassName()` extension function converts `KClass<*>` to KotlinPoet's `ClassName`:
```kotlin
private fun KClass<*>.toClassName(): ClassName {
    val qualifiedName = this.qualifiedName 
        ?: throw IllegalArgumentException("Cannot get qualified name for $this")
    val packageName = qualifiedName.substringBeforeLast('.', "")
    val simpleNames = qualifiedName.substringAfterLast('.').split('.')
    return ClassName(packageName, simpleNames)
}
```

## Future Considerations
- This pattern can be extended to other generated code (annotations, etc.)
- Consider creating similar helper objects for frequently referenced external classes (Compose, Kotlinx, etc.)
- Could add validation tests to ensure generated class references match actual core module structure
