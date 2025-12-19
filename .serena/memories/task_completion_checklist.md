# Task Completion Checklist

## Before Marking a Task Complete

Run through this checklist after completing any code changes:

### 1. Code Quality

```bash
# Run detekt static analysis
./gradlew detekt
```

- [ ] No detekt violations (or justified suppressions)
- [ ] Code follows project naming conventions
- [ ] KDoc comments on all new public APIs
- [ ] No TODO/FIXME markers left behind

### 2. Testing

```bash
# Run android tests (fastest)
./gradlew :quo-vadis-core:androidTest

# Run all tests for the affected module
./gradlew :quo-vadis-core:test
./gradlew :quo-vadis-annotations:test
./gradlew :quo-vadis-ksp:test
./gradlew :composeApp:test
```

- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] Tests cover edge cases
- [ ] Use `FakeNavigator` for navigation testing

### 3. Build Verification

```bash
# Build all platforms
./gradlew build

# Or build specific module
./gradlew :quo-vadis-core:build
```

- [ ] Project compiles without errors
- [ ] No new compile warnings
- [ ] All platform targets build successfully

### 4. Documentation

- [ ] README updated if needed
- [ ] API documentation (KDoc) added
- [ ] Example code updated if API changed
- [ ] CHANGELOG.md updated for notable changes

### 5. Platform Testing (if applicable)

For UI or platform-specific changes:

```bash
# Desktop
./gradlew :composeApp:run

# Android
./gradlew :composeApp:installDebug

```

- [ ] Feature works on Android
- [ ] Feature works on iOS (if applicable)
- [ ] Feature works on Desktop
- [ ] Feature works on Web (JS/WASM)

### 6. Git Hygiene

```bash
git status
git diff --staged
```

- [ ] Only relevant files included in commit
- [ ] No generated files committed (build/, .gradle/)
- [ ] Commit message follows conventional format
- [ ] No merge conflicts

## Quick Verification Commands

```bash
# Full verification (run all checks)
./gradlew clean build detekt test

# Quick verification (desktop only)
./gradlew :quo-vadis-core:desktopTest detekt

# Check for compilation issues only
./gradlew compileKotlin
```

## Common Issues to Check

1. **Multiplatform compatibility**: Ensure code works on all targets
2. **Compose stability**: Verify `@Composable` functions are correct
3. **Navigation state**: Check `StateFlow` updates properly
4. **Memory leaks**: Ensure proper cleanup in `DisposableEffect`
5. **Thread safety**: Use `Mutex` or `atomic` for shared state

## PR Checklist

Before submitting a Pull Request:

- [ ] Branch is up to date with main
- [ ] All CI checks would pass
- [ ] PR description clearly explains changes
- [ ] Related issues are linked
- [ ] Breaking changes documented
- [ ] Migration guide provided if needed
