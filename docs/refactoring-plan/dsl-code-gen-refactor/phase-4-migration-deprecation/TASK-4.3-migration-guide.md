# Task 4.3: Create Migration Guide Documentation

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 2 days  
> **Dependencies**: Phase 1, Phase 2, Phase 3 complete  
> **Blocks**: None (parallel with 4.1, 4.2)

---

## Objective

Create comprehensive migration documentation that guides users from the old imperative API pattern to the new DSL-based architecture. The guide should be **practical**, **step-by-step**, and cover all common migration scenarios.

**Output File**: `docs/MIGRATION_DSL.md`

---

## Target Content Structure

```
docs/MIGRATION_DSL.md
├── Overview & Benefits
├── Quick Migration (TL;DR)
├── Step-by-Step Migration
│   ├── Step 1: Update Navigator Creation
│   ├── Step 2: Replace Registry Parameters
│   ├── Step 3: Remove Deprecated Imports
│   └── Step 4: Verify and Test
├── Migration Patterns
│   ├── Simple App Migration
│   ├── Multi-Module Migration
│   └── Custom Configuration Migration
├── API Mapping Reference
├── Common Scenarios
├── Troubleshooting
├── FAQ
└── Getting Help
```

---

## Document Template

Below is the complete template for `docs/MIGRATION_DSL.md`:

```markdown
# Migrating to DSL-Based Navigation

> **Version**: Quo Vadis X.X.0+  
> **Migration Complexity**: Low to Medium  
> **Breaking Changes**: None (fully backward compatible)

This guide walks you through migrating from the old imperative navigation setup to the new DSL-based architecture. The new approach reduces boilerplate, improves readability, and enables powerful multi-module composition.

---

## Why Migrate?

### Before (Old Pattern)
```kotlin
// 20+ lines of setup code
val navigator = TreeNavigator(
    initialState = buildMainTabsNavNode(),
    scopeRegistry = GeneratedScopeRegistry,
    containerRegistry = GeneratedContainerRegistry,
    deepLinkHandler = GeneratedDeepLinkHandlerImpl,
    coroutineScope = coroutineScope
)

NavigationHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    wrapperRegistry = GeneratedWrapperRegistry,
    scopeRegistry = GeneratedScopeRegistry,
    transitionRegistry = GeneratedTransitionRegistry,
    // ... more parameters
)
```

### After (New Pattern)
```kotlin
// 1-3 lines of setup code
QuoVadisNavigation(MainTabs::class)

// Or for more control:
val navigator = rememberQuoVadisNavigator(MainTabs::class)
NavigationHost(navigator = navigator, config = GeneratedNavigationConfig)
```

### Benefits

| Benefit | Description |
|---------|-------------|
| **Less Boilerplate** | 20+ lines → 1-3 lines |
| **Single Config** | 6 registries → 1 unified config |
| **Type Safety** | Class reference instead of builder function |
| **Composability** | Easy multi-module integration via `+` |
| **Future Proof** | New features added to config automatically |

---

## Quick Migration (TL;DR)

For a simple app, migration is three changes:

1. **Replace navigator creation**:
   ```kotlin
   // Old
   val navigator = TreeNavigator(initialState = buildMainTabsNavNode(), ...)
   
   // New
   val navigator = rememberQuoVadisNavigator(MainTabs::class)
   ```

2. **Replace NavigationHost parameters**:
   ```kotlin
   // Old
   NavigationHost(
       navigator = navigator,
       screenRegistry = GeneratedScreenRegistry,
       wrapperRegistry = GeneratedWrapperRegistry,
       scopeRegistry = GeneratedScopeRegistry,
       transitionRegistry = GeneratedTransitionRegistry
   )
   
   // New
   NavigationHost(
       navigator = navigator,
       config = GeneratedNavigationConfig
   )
   ```

3. **Or use the one-liner**:
   ```kotlin
   // Replaces both navigator creation and NavigationHost
   QuoVadisNavigation(MainTabs::class)
   ```

That's it! Your app should work exactly as before.

---

## Step-by-Step Migration

### Step 1: Update Navigator Creation

**Find code like this**:
```kotlin
val coroutineScope = rememberCoroutineScope()

val navigator = remember {
    TreeNavigator(
        initialState = buildMainTabsNavNode(),
        scopeRegistry = GeneratedScopeRegistry,
        containerRegistry = GeneratedContainerRegistry,
        deepLinkHandler = GeneratedDeepLinkHandlerImpl,
        coroutineScope = coroutineScope
    )
}
```

**Replace with**:
```kotlin
val navigator = rememberQuoVadisNavigator(MainTabs::class)
```

**Notes**:
- `rememberQuoVadisNavigator` handles `remember`, coroutine scope, and all registries
- Pass your root destination class (e.g., `MainTabs::class`)
- Default config is `GeneratedNavigationConfig`

**With custom config**:
```kotlin
val navigator = rememberQuoVadisNavigator(
    rootDestination = MainTabs::class,
    config = GeneratedNavigationConfig  // or custom config
)
```

### Step 2: Replace Registry Parameters

**Find NavigationHost calls like this**:
```kotlin
NavigationHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    wrapperRegistry = GeneratedWrapperRegistry,
    scopeRegistry = GeneratedScopeRegistry,
    transitionRegistry = GeneratedTransitionRegistry,
    enablePredictiveBack = true,
    modifier = Modifier.fillMaxSize()
)
```

**Replace with**:
```kotlin
NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig,
    enablePredictiveBack = true,
    modifier = Modifier.fillMaxSize()
)
```

**Notes**:
- All 5+ registry parameters become 1 `config` parameter
- Other parameters (modifier, predictiveBack, etc.) remain unchanged

### Step 3: Remove Deprecated Imports

**Remove these imports** (if present):
```kotlin
import com.example.navigation.generated.GeneratedScreenRegistry
import com.example.navigation.generated.GeneratedScopeRegistry
import com.example.navigation.generated.GeneratedContainerRegistry
import com.example.navigation.generated.GeneratedTransitionRegistry
import com.example.navigation.generated.GeneratedWrapperRegistry
import com.example.navigation.generated.GeneratedDeepLinkHandlerImpl
import com.example.navigation.generated.buildMainTabsNavNode
```

**Keep/Add this import**:
```kotlin
import com.example.navigation.generated.GeneratedNavigationConfig
```

### Step 4: Verify and Test

1. **Build the project**: Ensure no compilation errors
2. **Run the app**: Verify all navigation works
3. **Test navigation flows**:
   - Tab switching
   - Stack navigation (push/pop)
   - Back button behavior
   - Deep links (if used)
4. **Check for deprecation warnings**: Old APIs should show warnings

---

## Migration Patterns

### Pattern A: Simple App Migration

**Scenario**: Single-module app with basic navigation

**Before**:
```kotlin
@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    
    val navigator = remember {
        TreeNavigator(
            initialState = buildMainTabsNavNode(),
            scopeRegistry = GeneratedScopeRegistry,
            containerRegistry = GeneratedContainerRegistry,
            deepLinkHandler = GeneratedDeepLinkHandlerImpl,
            coroutineScope = coroutineScope
        )
    }
    
    MaterialTheme {
        NavigationHost(
            navigator = navigator,
            screenRegistry = GeneratedScreenRegistry,
            wrapperRegistry = GeneratedWrapperRegistry,
            scopeRegistry = GeneratedScopeRegistry,
            transitionRegistry = GeneratedTransitionRegistry
        )
    }
}
```

**After (Option 1 - One-liner)**:
```kotlin
@Composable
fun App() {
    MaterialTheme {
        QuoVadisNavigation(MainTabs::class)
    }
}
```

**After (Option 2 - With navigator access)**:
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    MaterialTheme {
        NavigationHost(
            navigator = navigator,
            config = GeneratedNavigationConfig
        )
    }
}
```

### Pattern B: Multi-Module Migration

**Scenario**: App with feature modules that have their own navigation configs

**Before**:
```kotlin
@Composable
fun App() {
    // Complex manual registry combination
    val combinedScreenRegistry = CompositeScreenRegistry(
        AppScreenRegistry,
        FeatureAScreenRegistry,
        FeatureBScreenRegistry
    )
    val combinedScopeRegistry = CompositeScopeRegistry(
        AppScopeRegistry,
        FeatureAScopeRegistry,
        FeatureBScopeRegistry
    )
    // ... similar for other registries
    
    val navigator = TreeNavigator(
        initialState = buildMainTabsNavNode(),
        scopeRegistry = combinedScopeRegistry,
        // ...
    )
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = combinedScreenRegistry,
        // ...
    )
}
```

**After**:
```kotlin
@Composable
fun App() {
    // Simple composition with + operator
    val combinedConfig = GeneratedNavigationConfig +
                         FeatureANavigationConfig +
                         FeatureBNavigationConfig
    
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = combinedConfig
    )
    
    NavigationHost(
        navigator = navigator,
        config = combinedConfig
    )
}
```

### Pattern C: Custom Configuration Migration

**Scenario**: App with custom deep link handler or other customizations

**Before**:
```kotlin
@Composable
fun App() {
    val customDeepLinkHandler = remember { CustomDeepLinkHandler() }
    
    val navigator = TreeNavigator(
        initialState = buildMainTabsNavNode(),
        deepLinkHandler = customDeepLinkHandler,  // Custom
        scopeRegistry = GeneratedScopeRegistry,
        containerRegistry = GeneratedContainerRegistry,
        coroutineScope = coroutineScope
    )
    // ...
}
```

**After**:
```kotlin
@Composable
fun App() {
    val customDeepLinkHandler = remember { CustomDeepLinkHandler() }
    
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        deepLinkHandler = customDeepLinkHandler  // Still supported
    )
    // ...
}
```

---

## API Mapping Reference

### Navigator Creation

| Old API | New API |
|---------|---------|
| `TreeNavigator(initialState = buildXNavNode(), ...)` | `rememberQuoVadisNavigator(X::class)` |
| `buildMainTabsNavNode()` | `config.buildNavNode(MainTabs::class)` |
| `buildProfileStackNavNode()` | `config.buildNavNode(ProfileStack::class)` |

### Registry Access

| Old API | New API |
|---------|---------|
| `GeneratedScreenRegistry` | `GeneratedNavigationConfig.screenRegistry` |
| `GeneratedScopeRegistry` | `GeneratedNavigationConfig.scopeRegistry` |
| `GeneratedContainerRegistry` | `GeneratedNavigationConfig.containerRegistry` |
| `GeneratedTransitionRegistry` | `GeneratedNavigationConfig.transitionRegistry` |
| `GeneratedWrapperRegistry` | `GeneratedNavigationConfig.wrapperRegistry` |
| `GeneratedDeepLinkHandlerImpl` | `GeneratedNavigationConfig.deepLinkHandler` |

### NavigationHost Parameters

| Old Parameters | New Parameter |
|----------------|---------------|
| `screenRegistry = ...` | `config = GeneratedNavigationConfig` |
| `wrapperRegistry = ...` | (included in config) |
| `scopeRegistry = ...` | (included in config) |
| `transitionRegistry = ...` | (included in config) |

### Convenience Functions

| Use Case | New API |
|----------|---------|
| One-liner setup | `QuoVadisNavigation(RootDestination::class)` |
| Standard setup | `rememberQuoVadisNavigator()` + `NavigationHost(config = ...)` |
| Multi-module | `ConfigA + ConfigB + ConfigC` |

---

## Common Scenarios

### Scenario 1: I need navigator access outside NavigationHost

**Solution**: Use `rememberQuoVadisNavigator` instead of one-liner

```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    // Use navigator anywhere
    LaunchedEffect(someEvent) {
        navigator.navigate(SomeDestination)
    }
    
    Column {
        TopBar(onMenuClick = { navigator.navigate(MenuDestination) })
        NavigationHost(navigator = navigator, config = GeneratedNavigationConfig)
    }
}
```

### Scenario 2: I have custom screen wrappers

**Solution**: Wrappers are included in the config - no changes needed

```kotlin
// Wrappers defined via @TabWrapper/@PaneWrapper annotations
// are automatically included in GeneratedNavigationConfig.wrapperRegistry
NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig  // wrappers included
)
```

### Scenario 3: I'm using predictive back gesture

**Solution**: The parameter remains on NavigationHost

```kotlin
NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig,
    enablePredictiveBack = true,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)
```

### Scenario 4: I have a different initial tab

**Solution**: Pass initial state via buildNavNode or navigator

```kotlin
// Option 1: Via config.buildNavNode
val initialState = GeneratedNavigationConfig.buildNavNode(
    destinationClass = MainTabs::class,
    key = "custom-key"
)

// Option 2: Navigator will handle based on destination class
val navigator = rememberQuoVadisNavigator(
    rootDestination = MainTabs::class,
    config = GeneratedNavigationConfig
)
// Then switch tab programmatically if needed
```

### Scenario 5: I need to handle deep links specially

**Solution**: Pass custom handler to rememberQuoVadisNavigator

```kotlin
val navigator = rememberQuoVadisNavigator(
    rootDestination = MainTabs::class,
    config = GeneratedNavigationConfig,
    deepLinkHandler = MyCustomDeepLinkHandler()
)
```

---

## Troubleshooting

### Problem: "Unresolved reference: rememberQuoVadisNavigator"

**Cause**: Missing import or dependency

**Solution**:
1. Add import: `import com.jermey.quo.vadis.core.compose.rememberQuoVadisNavigator`
2. Ensure quo-vadis-core dependency is up to date

### Problem: "Unresolved reference: GeneratedNavigationConfig"

**Cause**: KSP hasn't regenerated code

**Solution**:
1. Clean and rebuild: `./gradlew clean build`
2. Check that KSP is configured correctly
3. Verify annotations are on your destinations

### Problem: "No container registered for class X"

**Cause**: The destination class passed to `rememberQuoVadisNavigator` isn't a container

**Solution**:
- Pass a container class (annotated with `@Tabs`, `@Stack`, or `@Pane`)
- Check spelling and imports of the destination class

### Problem: Navigation doesn't work after migration

**Cause**: Possible config mismatch or missing registrations

**Solution**:
1. Verify all destinations are annotated
2. Rebuild to regenerate code
3. Check that you're using `GeneratedNavigationConfig` (not a partial config)
4. Enable debug logging to trace navigation issues

### Problem: Deprecation warnings everywhere

**Cause**: Old APIs are now deprecated

**Solution**: This is expected! Follow this guide to migrate away from deprecated APIs.

---

## FAQ

### Q: Do I have to migrate immediately?

**A**: No. Old APIs are deprecated but still work. You can migrate at your own pace. However, we recommend migrating to benefit from the simpler API and future improvements.

### Q: Will my annotations change?

**A**: No. All annotations (`@Screen`, `@Tabs`, `@Stack`, `@Pane`, etc.) work exactly as before. Only the consumption of generated code changes.

### Q: Is there a performance difference?

**A**: No measurable difference. The new APIs are wrappers around the same underlying implementation.

### Q: Can I mix old and new patterns?

**A**: Technically yes, but not recommended. Pick one pattern and stick with it for consistency.

### Q: What if I'm using a custom registry implementation?

**A**: You can still access individual registries via `GeneratedNavigationConfig.screenRegistry`, etc. Or create a custom `NavigationConfig` implementation.

### Q: How do I migrate in a large codebase?

**A**: Migrate one module/feature at a time. The old and new APIs can coexist during the transition.

---

## Getting Help

- **Documentation**: [Quo Vadis Docs](https://quo-vadis.dev)
- **GitHub Issues**: [Report bugs or ask questions](https://github.com/example/quo-vadis/issues)
- **Discussions**: [Community discussions](https://github.com/example/quo-vadis/discussions)

---

## Changelog

| Version | Changes |
|---------|---------|
| X.X.0 | Initial DSL-based APIs introduced |
| X.X.0 | Old APIs deprecated with migration path |
```

---

## Implementation Steps

### Step 1: Create the File

Create `docs/MIGRATION_DSL.md` with the template above.

### Step 2: Customize for Project

1. Update version numbers
2. Adjust package names/imports to match project
3. Add project-specific examples if needed
4. Update links to actual documentation/GitHub

### Step 3: Review and Test Examples

1. Copy each code example
2. Verify it compiles
3. Fix any errors in the guide

### Step 4: Cross-Reference

1. Ensure deprecation messages in Task 4.1 point to this file
2. Link from demo app comments (Task 4.2)
3. Add to project documentation index

---

## Acceptance Criteria

### Content Requirements

- [ ] All sections from template included
- [ ] Quick migration (TL;DR) section present
- [ ] Step-by-step migration with clear instructions
- [ ] At least 3 migration patterns documented
- [ ] Complete API mapping reference table
- [ ] At least 5 common scenarios covered
- [ ] Troubleshooting section with common problems
- [ ] FAQ section with anticipated questions

### Code Example Requirements

- [ ] All code examples compile without errors
- [ ] Before/after comparisons are accurate
- [ ] Examples match actual API signatures
- [ ] Import statements are correct

### Quality Requirements

- [ ] Clear, concise writing
- [ ] Consistent formatting
- [ ] No typos or grammatical errors
- [ ] Proper Markdown rendering
- [ ] Appropriate use of code blocks, tables, headings

### Integration Requirements

- [ ] Referenced from deprecation warnings (Task 4.1)
- [ ] Linked from demo app documentation (Task 4.2)
- [ ] Added to docs index if applicable

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| Write initial content from template | 0.75 days |
| Customize for project specifics | 0.25 days |
| Test all code examples | 0.5 days |
| Review and polish writing | 0.25 days |
| Cross-reference with other tasks | 0.25 days |
| **Total** | **~2 days** |

---

## Related Files

- [Phase 4 Summary](./SUMMARY.md)
- [Task 4.1 - Deprecation Warnings](./TASK-4.1-deprecation-warnings.md)
- [Task 4.2 - Update Demo Application](./TASK-4.2-update-demo-app.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
