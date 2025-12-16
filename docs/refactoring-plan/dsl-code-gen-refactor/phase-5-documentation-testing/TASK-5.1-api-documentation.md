```markdown
# Task 5.1: API Documentation

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 2-3 days  
> **Dependencies**: Phase 1-4 complete  
> **Blocks**: Release

---

## Objective

Create comprehensive documentation for all new DSL-based navigation APIs. This includes KDoc comments in source code, updates to the documentation website, and updated code samples throughout the project.

**Target Outcome**:
- All new public APIs have complete KDoc documentation
- Website reflects new recommended patterns
- Code samples demonstrate one-liner, standard, and advanced usage

---

## Files to Create/Modify

### KDoc Documentation (Source Code)

| File | Location | Documentation Scope |
|------|----------|---------------------|
| `NavigationConfig.kt` | `quo-vadis-core/.../navigation/` | Interface, properties, methods |
| `NavigationConfigBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Builder class, DSL functions |
| `StackBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Stack DSL |
| `TabsBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Tabs DSL |
| `PanesBuilder.kt` | `quo-vadis-core/.../navigation/dsl/` | Panes DSL |
| `QuoVadisComposables.kt` | `quo-vadis-core/.../compose/` | Convenience composables |

### Website Documentation

| File | Location | Content |
|------|----------|---------|
| DSL Overview | `docs/site/src/` | Introduction to DSL approach |
| Quick Start | `docs/site/src/` | One-liner setup guide |
| Configuration Guide | `docs/site/src/` | Full DSL reference |
| Migration Guide | `docs/site/src/` | Link to MIGRATION_DSL.md |
| API Reference | `docs/site/src/` | Generated from KDoc |

### Code Samples

| Location | Updates Required |
|----------|------------------|
| `README.md` | Add new usage patterns |
| `composeApp/` | Demo app showcases new APIs |
| `docs/site/` | Website code examples |

---

## Documentation Requirements

### KDoc Standards

All new public APIs must include:

```kotlin
/**
 * Brief one-line description.
 * 
 * Detailed description explaining the purpose, behavior, and usage context.
 * 
 * ## Usage
 * 
 * ```kotlin
 * // Code example showing typical usage
 * ```
 * 
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 * @see RelatedClass
 * @since 1.x.0
 */
```

### Website Documentation Structure

```
docs/site/src/
├── getting-started/
│   ├── quick-start.md          # One-liner setup
│   └── installation.md         # Dependency setup
├── guides/
│   ├── dsl-configuration.md    # Full DSL reference
│   ├── multi-module.md         # Composition patterns
│   └── migration.md            # Migration from old APIs
├── api/
│   ├── navigation-config.md    # NavigationConfig reference
│   ├── dsl-builders.md         # Builder reference
│   └── composables.md          # Convenience functions
└── examples/
    ├── one-liner.md            # Simple setup
    ├── standard.md             # Standard pattern
    └── advanced.md             # Full control pattern
```

---

## Code Samples to Include

### One-Liner Pattern

```kotlin
@Composable
fun App() {
    QuoVadisNavigation(MainTabs::class)
}
```

### Standard Pattern

```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig
    )
}
```

### Advanced Pattern

```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        deepLinkHandler = CustomDeepLinkHandler
    )
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig + FeatureModuleConfig,
        enablePredictiveBack = true,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
        windowSizeClass = currentWindowSizeClass()
    )
}
```

### Multi-Module Composition

```kotlin
// Feature module
object FeatureAConfig : NavigationConfig by navigationConfig {
    screen<FeatureADestination> { ... }
    stack<FeatureAContainer> { ... }
}

// App module
val combinedConfig = GeneratedNavigationConfig + 
                     FeatureAConfig + 
                     FeatureBConfig
```

---

## Acceptance Criteria Checklist

### KDoc Documentation
- [ ] `NavigationConfig` interface fully documented
- [ ] All DSL builder classes documented
- [ ] All public functions have KDoc
- [ ] All parameters described
- [ ] Code examples in relevant KDoc
- [ ] `@see` links to related APIs
- [ ] `@since` tags for new APIs

### Website Documentation
- [ ] Quick start guide updated
- [ ] DSL configuration guide complete
- [ ] Multi-module guide added
- [ ] Migration guide linked
- [ ] API reference generated/updated
- [ ] All code samples tested and working

### Code Samples
- [ ] README.md shows new patterns
- [ ] Demo app uses new APIs
- [ ] Website examples compile
- [ ] Before/after comparison documented

### Quality
- [ ] Documentation reviewed for accuracy
- [ ] No broken links
- [ ] Consistent terminology
- [ ] Screenshots/diagrams where helpful

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| KDoc for core interfaces | 0.5 days |
| KDoc for DSL builders | 0.5 days |
| Website quick start | 0.25 days |
| Website configuration guide | 0.5 days |
| Website multi-module guide | 0.25 days |
| Code samples & README | 0.5 days |
| Review & polish | 0.5 days |
| **Total** | **2-3 days** |

---

## Implementation Notes

### KDoc Generation

Consider using Dokka for API reference generation:

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml {
    outputDirectory.set(file("docs/api"))
}
```

### Documentation Site

The documentation site in `docs/site/` uses:
- Vite for build tooling
- TypeScript configuration
- Static HTML generation

Update relevant files to reflect new navigation patterns.

### Cross-References

Ensure documentation links:
- Old API docs → Migration guide
- Migration guide → New API docs
- Quick start → Detailed configuration guide

---

## Related Files

- [Phase 5 Summary](./SUMMARY.md)
- [Task 5.2 - Comprehensive Testing](./TASK-5.2-comprehensive-testing.md)
- [Migration Guide](../../docs/MIGRATION_DSL.md) (Task 4.3)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)

```
