# Task Completion Checklist

When completing a task in the NavPlayground project, follow this checklist to ensure quality and consistency.

## 1. Code Quality

### Before Committing
- [ ] Code follows Kotlin official style guide
- [ ] All public APIs have KDoc documentation
- [ ] Complex logic has explanatory comments
- [ ] No unnecessary imports
- [ ] No unused variables or functions
- [ ] Proper null safety handling

### Naming & Structure
- [ ] Classes/interfaces use PascalCase
- [ ] Functions/properties use camelCase
- [ ] Destinations use `Destination` suffix
- [ ] Test implementations use `Fake` prefix
- [ ] Default implementations use `Default` prefix
- [ ] Files are named after their primary class

## 2. Build & Compilation

### Verify Build
- [ ] Run: `./gradlew clean build`
- [ ] Check for compilation errors
- [ ] Check for warnings (address if relevant)
- [ ] Verify both modules build: `composeApp` and `quo-vadis-core`

### Platform-Specific
- [ ] Android builds successfully: `./gradlew :composeApp:assembleDebug`
- [ ] iOS framework builds: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- [ ] If applicable, test on both platforms

## 3. Testing

### Unit Tests
- [ ] Write/update unit tests for new functionality
- [ ] Run all tests: `./gradlew test`
- [ ] All tests pass
- [ ] Test coverage for critical paths
- [ ] Use `FakeNavigator` for navigation testing

### Manual Testing (if UI changes)
- [ ] Test on Android emulator/device
- [ ] Test on iOS simulator/device (if applicable)
- [ ] Test navigation flows
- [ ] Test back button behavior
- [ ] Test edge cases (empty states, errors, etc.)

## 4. Architecture Compliance

### Navigation Library (`quo-vadis-core`)
- [ ] No dependencies on external navigation libraries
- [ ] Maintain multiplatform compatibility
- [ ] Public APIs are type-safe
- [ ] State is reactive (using StateFlow/SharedFlow)
- [ ] Thread-safe implementations

### Demo Application (`composeApp`)
- [ ] Demonstrates features correctly
- [ ] Follows navigation patterns from docs
- [ ] Uses library APIs properly
- [ ] Clear separation of concerns

## 5. Documentation

### Code Documentation
- [ ] Update KDoc for modified public APIs
- [ ] Add/update inline comments for complex logic
- [ ] Update README if feature-relevant

### Architecture Documentation
- [ ] Update `ARCHITECTURE.md` if architecture changed
- [ ] Update `API_REFERENCE.md` if public API changed
- [ ] Update demo README if new pattern added

## 6. Multiplatform Considerations

### Source Sets
- [ ] Common code in `commonMain`
- [ ] Android-specific code in `androidMain`
- [ ] iOS-specific code in `iosMain`
- [ ] No platform-specific code in common source sets

### Compatibility
- [ ] Verify no Android-only APIs in common code
- [ ] Verify no iOS-only APIs in common code
- [ ] Use expect/actual for platform differences

## 7. Performance

### Optimization
- [ ] No unnecessary object allocations in hot paths
- [ ] Proper use of `remember` in Composables
- [ ] StateFlow used appropriately (not creating duplicates)
- [ ] LaunchedEffect dependencies are correct
- [ ] No memory leaks (especially in navigation state)

## 8. Code Review Readiness

### Before Pushing
- [ ] Remove debug prints/logs
- [ ] Remove commented-out code
- [ ] Remove TODO comments or create issues for them
- [ ] Commit messages are descriptive
- [ ] Changes are logically grouped in commits

### Git
- [ ] Branch is up to date with main
- [ ] No merge conflicts
- [ ] `.gitignore` properly configured
- [ ] No build artifacts committed

## 9. Linting (if configured)

### Android Lint
- [ ] Run: `./gradlew lint`
- [ ] Review lint report
- [ ] Address critical issues
- [ ] Suppress only when justified (with comment)

## 10. Dependency Management

### If Dependencies Changed
- [ ] Version catalog updated (`gradle/libs.versions.toml`)
- [ ] All modules use catalog versions
- [ ] No version conflicts
- [ ] Dependencies are multiplatform-compatible

## 11. Gradle Configuration

### Build Files
- [ ] No hardcoded versions (use version catalog)
- [ ] Proper dependency scopes (`implementation`, `api`, etc.)
- [ ] Configuration cache compatible
- [ ] Build cache friendly

## 12. Final Verification

### Smoke Test
- [ ] Clean build: `./gradlew clean build`
- [ ] Run app on Android
- [ ] Basic navigation works
- [ ] No crashes on startup
- [ ] No obvious visual bugs

### Pre-Push Checklist
- [ ] All automated tests pass
- [ ] Build is successful
- [ ] Code is documented
- [ ] Commits are clean
- [ ] Ready for review

## Quick Commands for Verification

```bash
# Clean and build everything
./gradlew clean build

# Run all tests
./gradlew test

# Build Android debug
./gradlew :composeApp:assembleDebug

# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Check for lint issues (if configured)
./gradlew lint

# Install and run on Android device
./gradlew :composeApp:installDebug
```

## Notes

- Not all items apply to every task
- Use judgment on which items are relevant
- When in doubt, ask for clarification
- Document any deviations from these guidelines
