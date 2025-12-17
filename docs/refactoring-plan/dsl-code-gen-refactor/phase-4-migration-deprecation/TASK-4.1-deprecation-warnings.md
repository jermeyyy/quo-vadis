# Task 4.1: Add Deprecation Warnings to Old APIs

> **Task Status**: ✅ Completed  
> **Completed**: December 2024  
> **Estimated Effort**: 1-2 days  
> **Dependencies**: Phase 1, Phase 2, Phase 3 complete  
> **Blocks**: None (parallel with 4.2, 4.3)

---

## Objective

Add `@Deprecated` annotations to all old APIs that are superseded by the new DSL-based architecture. This provides IDE warnings and clear migration paths to guide users toward the new APIs without breaking existing code.

**Key Principle**: All old code must continue to compile and run. Deprecation is informational, not enforced.

---

## Items to Deprecate

### 1. Old `build*NavNode()` Functions

These generated functions are replaced by `NavigationConfig.buildNavNode()`.

| Old API | Location | Replacement |
|---------|----------|-------------|
| `buildMainTabsNavNode()` | Generated | `config.buildNavNode(MainTabs::class)` |
| `buildProfileStackNavNode()` | Generated | `config.buildNavNode(ProfileStack::class)` |
| `build*NavNode()` (all variants) | Generated | `config.buildNavNode(ContainerClass::class)` |

**Deprecation Template**:
```kotlin
@Deprecated(
    message = "Use NavigationConfig.buildNavNode() instead for unified configuration. " +
              "See migration guide at docs/MIGRATION_DSL.md",
    replaceWith = ReplaceWith(
        expression = "GeneratedNavigationConfig.buildNavNode(MainTabs::class, key, parentKey)",
        imports = ["com.example.app.navigation.generated.GeneratedNavigationConfig"]
    ),
    level = DeprecationLevel.WARNING
)
fun buildMainTabsNavNode(
    key: String = "MainTabs",
    parentKey: String? = null,
    initialTabIndex: Int = 0
): TabNode {
    // ... existing implementation preserved
}
```

### 2. Individual Registry Objects

These generated singleton objects are replaced by properties on `GeneratedNavigationConfig`.

| Old API | Location | Replacement |
|---------|----------|-------------|
| `GeneratedScreenRegistry` | Generated | `GeneratedNavigationConfig.screenRegistry` |
| `GeneratedScopeRegistry` | Generated | `GeneratedNavigationConfig.scopeRegistry` |
| `GeneratedContainerRegistry` | Generated | `GeneratedNavigationConfig.containerRegistry` |
| `GeneratedTransitionRegistry` | Generated | `GeneratedNavigationConfig.transitionRegistry` |
| `GeneratedWrapperRegistry` | Generated | `GeneratedNavigationConfig.wrapperRegistry` |
| `GeneratedDeepLinkHandler` | Generated | `GeneratedNavigationConfig.deepLinkHandler` |

**Deprecation Template**:
```kotlin
@Deprecated(
    message = "Use GeneratedNavigationConfig.screenRegistry instead. " +
              "The unified NavigationConfig provides all registries in one place. " +
              "See migration guide at docs/MIGRATION_DSL.md",
    replaceWith = ReplaceWith(
        expression = "GeneratedNavigationConfig.screenRegistry",
        imports = ["com.example.app.navigation.generated.GeneratedNavigationConfig"]
    ),
    level = DeprecationLevel.WARNING
)
object GeneratedScreenRegistry : ScreenRegistry {
    // ... existing implementation preserved
}
```

### 3. Direct Registry Parameters in NavigationHost

The `NavigationHost` overload with individual registry parameters should guide users to the config-based overload.

| Old Pattern | New Pattern |
|-------------|-------------|
| Multiple registry params | Single `config` param |

**Note**: This deprecation is handled via documentation rather than `@Deprecated` annotation since we're adding a new overload, not deprecating the function itself. The KDoc should indicate the preferred approach.

**Documentation Update in NavigationHost**:
```kotlin
/**
 * ...existing docs...
 * 
 * @deprecated Prefer using the [NavigationHost] overload that accepts [NavigationConfig]
 * for simpler integration:
 * ```
 * NavigationHost(
 *     navigator = navigator,
 *     config = GeneratedNavigationConfig
 * )
 * ```
 */
@Composable
fun NavigationHost(
    navigator: Navigator,
    screenRegistry: ScreenRegistry,
    wrapperRegistry: WrapperRegistry,
    scopeRegistry: ScopeRegistry,
    transitionRegistry: TransitionRegistry,
    // ... other params
) {
    // ... implementation
}
```

---

## Deprecation Message Format

### Standard Message Template

```kotlin
@Deprecated(
    message = "[Brief description of what's deprecated]. " +
              "[What to use instead]. " +
              "See migration guide at docs/MIGRATION_DSL.md",
    replaceWith = ReplaceWith(
        expression = "[replacement expression]",
        imports = ["[required imports]"]
    ),
    level = DeprecationLevel.WARNING
)
```

### Message Guidelines

1. **Be specific**: Explain what's deprecated and why
2. **Provide alternative**: Always mention the replacement
3. **Link to docs**: Reference the migration guide
4. **Keep it concise**: Avoid overly long messages

### Examples

**Good**:
```kotlin
message = "Use GeneratedNavigationConfig.buildNavNode() instead. " +
          "See docs/MIGRATION_DSL.md for migration guide."
```

**Bad**:
```kotlin
message = "Deprecated"  // Too vague, no guidance
```

---

## DeprecationLevel Strategy

### Level Definitions

| Level | Behavior | When to Use |
|-------|----------|-------------|
| `WARNING` | IDE warning, compiles normally | **This phase** - initial deprecation |
| `ERROR` | Compilation error | Future major version - forcing migration |
| `HIDDEN` | Not visible in completions | Final stage before removal |

### Phased Approach

```
Phase 4 (Now)       → DeprecationLevel.WARNING
Next Minor Version  → DeprecationLevel.WARNING (continued)
Next Major Version  → DeprecationLevel.ERROR
Major + 1 Version   → DeprecationLevel.HIDDEN or removal
```

### Rationale

- **WARNING first**: Gives users time to migrate without breaking builds
- **ERROR later**: Forces migration before removing APIs
- **HIDDEN/removal**: Clean up codebase in major version

---

## Files to Modify

### KSP Generators (Output Templates)

| File | Changes |
|------|---------|
| `NavNodeBuilderGenerator.kt` | Add deprecation to generated `build*NavNode()` functions |
| `ScreenRegistryGenerator.kt` | Add deprecation to `GeneratedScreenRegistry` object |
| `ScopeRegistryGenerator.kt` | Add deprecation to `GeneratedScopeRegistry` object |
| `ContainerRegistryGenerator.kt` | Add deprecation to `GeneratedContainerRegistry` object |
| `TransitionRegistryGenerator.kt` | Add deprecation to `GeneratedTransitionRegistry` object |
| `WrapperRegistryGenerator.kt` | Add deprecation to `GeneratedWrapperRegistry` object |
| `DeepLinkHandlerGenerator.kt` | Add deprecation to `GeneratedDeepLinkHandler` object |

### Core Library (Documentation)

| File | Changes |
|------|---------|
| `NavigationHost.kt` | Update KDoc to recommend config-based overload |

---

## Implementation Details

### Modifying NavNodeBuilderGenerator

Location: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt`

Add deprecation annotation generation:

```kotlin
// In the function generation code, add:
private fun generateDeprecationAnnotation(
    containerClassName: String,
    packageName: String
): AnnotationSpec {
    return AnnotationSpec.builder(Deprecated::class)
        .addMember(
            "message = %S",
            "Use NavigationConfig.buildNavNode() instead. " +
            "See migration guide at docs/MIGRATION_DSL.md"
        )
        .addMember(
            "replaceWith = %T(%S, %S)",
            ReplaceWith::class,
            "GeneratedNavigationConfig.buildNavNode($containerClassName::class, key, parentKey)",
            "$packageName.generated.GeneratedNavigationConfig"
        )
        .addMember("level = %T.%L", DeprecationLevel::class, "WARNING")
        .build()
}
```

### Modifying Registry Generators

Similar pattern for each registry generator - add deprecation to the generated object:

```kotlin
// Example for ScreenRegistryGenerator
private fun generateDeprecationAnnotation(): AnnotationSpec {
    return AnnotationSpec.builder(Deprecated::class)
        .addMember(
            "message = %S",
            "Use GeneratedNavigationConfig.screenRegistry instead. " +
            "See migration guide at docs/MIGRATION_DSL.md"
        )
        .addMember(
            "replaceWith = %T(%S, %S)",
            ReplaceWith::class,
            "GeneratedNavigationConfig.screenRegistry",
            "$packageName.generated.GeneratedNavigationConfig"
        )
        .addMember("level = %T.%L", DeprecationLevel::class, "WARNING")
        .build()
}
```

---

## Testing Checklist

### Deprecation Visibility

- [ ] IDE shows warning icon on deprecated API usage
- [ ] Warning message appears in IDE tooltip/hover
- [ ] `ReplaceWith` suggestion appears in quick-fix menu
- [ ] Deprecation shows in generated KDoc

### Compilation Behavior

- [ ] Old code using deprecated APIs still compiles
- [ ] No compilation errors introduced
- [ ] Warnings appear in build output (not errors)
- [ ] CI/CD builds don't fail due to deprecations

### ReplaceWith Functionality

- [ ] IDE can auto-apply `ReplaceWith` transformation
- [ ] Correct imports added after replacement
- [ ] Replacement code compiles correctly
- [ ] Replacement preserves original behavior

---

## Acceptance Criteria

### Annotation Requirements

- [ ] All `build*NavNode()` functions have `@Deprecated` annotation
- [ ] All individual registry objects have `@Deprecated` annotation
- [ ] All deprecation messages include migration guide reference
- [ ] All applicable deprecations have `ReplaceWith` with correct expression
- [ ] All deprecations use `DeprecationLevel.WARNING`

### Code Quality

- [ ] Deprecation annotations use consistent message format
- [ ] Package names in `ReplaceWith` are correct
- [ ] No typos in deprecation messages
- [ ] Imports in `ReplaceWith` are minimal and correct

### Backward Compatibility

- [ ] All existing code continues to compile
- [ ] All existing tests continue to pass
- [ ] No runtime behavior changes
- [ ] Demo app compiles with warnings (not errors)

### Documentation

- [ ] `NavigationHost` KDoc updated to recommend config overload
- [ ] Deprecation messages reference `docs/MIGRATION_DSL.md`

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| Modify `NavNodeBuilderGenerator` | 0.25 days |
| Modify registry generators (6 files) | 0.5 days |
| Update `NavigationHost` KDoc | 0.25 days |
| Test deprecation behavior | 0.5 days |
| Verify ReplaceWith works | 0.25 days |
| Code review and fixes | 0.25 days |
| **Total** | **1-2 days** |

---

## Related Files

- [Phase 4 Summary](./SUMMARY.md)
- [Task 4.2 - Update Demo Application](./TASK-4.2-update-demo-app.md)
- [Task 4.3 - Migration Guide](./TASK-4.3-migration-guide.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
