# Phase 5: Testing, Migration & Deprecation

**Status**: Complete (5A–5H all implemented)  
**Created**: 2 March 2026  
**Updated**: 3 March 2026  
**Dependencies**: Phase 3 (partial — test infrastructure can start once IR backend exists), Phase 4 (multi-module tests)  
**Estimated Sub-phases**: 5A–5H

---

## Overview

Phase 5 ensures the compiler plugin is production-ready, provides a smooth migration path for existing KSP users, and establishes the deprecation timeline for the legacy KSP module.

Compiler plugins operate at a lower abstraction level than KSP — they manipulate the compiler's internal representation directly. Bugs in FIR extensions produce cryptic IDE errors; bugs in IR lowering produce runtime crashes or silent data corruption. There is no "generated source" to inspect visually. This makes automated testing **essential**, not optional.

The testing strategy is multi-layered:
- **FIR diagnostic tests** — verify that annotation misuse produces the correct errors/warnings
- **IR codegen "box tests"** — compile annotated source, execute generated code, verify behavior  
- **IR dump regression tests** — detect unintended changes in generated IR structure
- **E2E verification** — `composeApp` and feature modules build and run on all platforms  

The migration strategy uses parallel support:
- KSP and compiler plugin coexist behind a Gradle flag
- KSP is deprecated with clear timeline
- Migration guide documents all required changes

---

## Testing Strategy

### Layer 1: FIR Diagnostic Tests (Sub-phase 5B)

Compile annotated Kotlin source using `kotlin-compile-testing` with `QuoVadisCompilerPluginRegistrar`. Assert that specific `DiagnosticFactory` messages are reported (error, warning, or none).

**Why**: Ensures real-time IDE feedback is correct — bad annotations produce actionable errors, valid annotations produce no noise.

### Layer 2: IR Codegen Box Tests (Sub-phase 5C)

Compile annotated source → instantiate generated objects → call methods → assert return values. Uses `kotlin-compile-testing`'s classloader to load compiled classes and invoke them reflectively.

**Why**: Proves the IR lowering phase produces functionally correct bytecode. This is the most critical layer — it validates that `NavigationConfig` and `DeepLinkHandler` behave identically to KSP-generated equivalents.

### Layer 3: IR Structural Verification (Sub-phase 5D)

Enable `-Xverify-ir` in all test compilations. Additionally, dump textual IR and compare against golden files for regression detection.

**Why**: `-Xverify-ir` catches structural IR violations (malformed trees, type mismatches) that may not surface as runtime errors but indicate corruption. Golden file comparison catches unintended behavioral drift across Kotlin version upgrades.

### Layer 4: E2E Platform Verification (Sub-phase 5E)

Build the existing `composeApp` demo application using the compiler plugin instead of KSP. Verify it compiles and runs on all target platforms (Desktop, Android, iOS, JS, WasmJS).

**Why**: The demo app exercises all navigation patterns (stacks, tabs, panes, deep links, transitions, multi-module). If it works, the compiler plugin is functionally complete.

---

## Sub-phase 5A: Test Infrastructure

**Goal**: Set up the test harness so all subsequent test sub-phases can write tests immediately.

### Task 5A.1: Add `kotlin-compile-testing` Dependency

**Module**: `quo-vadis-compiler-plugin`

Add to `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("dev.zacsweers.kctfork:core:<version>")
    testImplementation("dev.zacsweers.kctfork:ksp:<version>")  // optional: for KSP comparison tests
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.junit)
}
```

> **Note**: Use the `kctfork` maintained fork of `kotlin-compile-testing`, which tracks K2 compiler API changes.

**Files**:
- `quo-vadis-compiler-plugin/build.gradle.kts` — add test dependencies

**Acceptance Criteria**:
- [ ] `./gradlew :quo-vadis-compiler-plugin:test` runs (even with no tests yet)
- [ ] `kotlin-compile-testing` resolves from Maven Central

---

### Task 5A.2: Create Test Helper — Embedded Compiler Configuration

Create a test utility that configures the Kotlin compiler with `QuoVadisCompilerPluginRegistrar` and the `quo-vadis-core` + `quo-vadis-annotations` runtime on the classpath.

```kotlin
// quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/CompilerTestHelper.kt

object CompilerTestHelper {

    fun compile(
        vararg sourceFiles: SourceFile,
        modulePrefix: String = "Test",
        expectSuccess: Boolean = true,
    ): CompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            compilerPluginRegistrars = listOf(QuoVadisCompilerPluginRegistrar())
            pluginOptions = listOf(
                PluginOption("com.jermey.quo-vadis", "modulePrefix", modulePrefix)
            )
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile().also { result ->
            if (expectSuccess) {
                assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
            }
        }
    }
}
```

**Files**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/CompilerTestHelper.kt`

**Acceptance Criteria**:
- [ ] A trivial test using `CompilerTestHelper.compile(SourceFile.kotlin("Empty.kt", "class Empty"))` passes
- [ ] Plugin registrar is correctly loaded and invoked

---

### Task 5A.3: Create Test Source Code Templates

Create reusable Kotlin source snippets representing common annotation patterns for test input.

```kotlin
// quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/TestSources.kt

object TestSources {

    val basicStack = SourceFile.kotlin("BasicDestination.kt", """
        import com.jermey.quo.vadis.annotations.*
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "basic", startDestination = BasicDestination.Home::class)
        sealed class BasicDestination : NavDestination {
            @Destination(route = "basic/home")
            data object Home : BasicDestination()

            @Destination(route = "basic/detail/{id}")
            data class Detail(@Argument val id: String) : BasicDestination()
        }
    """.trimIndent())

    val duplicateRouteStack = SourceFile.kotlin("DuplicateRoute.kt", """
        // ... source with two @Destination having the same route
    """.trimIndent())

    // Additional templates: tabs, panes, deep links, transitions, screens, etc.
}
```

**Files**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/TestSources.kt`

**Acceptance Criteria**:
- [ ] Templates cover: basic stack, stack with arguments, tabs, panes, deep links, transitions, screens, containers
- [ ] Templates for error cases: duplicate routes, argument mismatches, missing roles, orphan screens

---

### Task 5A.4: Create Assertion Utilities for FIR Diagnostics

Helper functions to assert expected diagnostics from compilation results.

```kotlin
// quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/DiagnosticAssertions.kt

fun CompilationResult.assertHasError(messageSubstring: String) { /* ... */ }
fun CompilationResult.assertHasWarning(messageSubstring: String) { /* ... */ }
fun CompilationResult.assertNoDiagnostics() { /* ... */ }
fun CompilationResult.assertErrorCount(count: Int) { /* ... */ }
```

**Files**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/DiagnosticAssertions.kt`

**Acceptance Criteria**:
- [ ] `assertHasError` correctly finds error messages by substring
- [ ] `assertNoDiagnostics` passes for clean compilations
- [ ] `assertHasWarning` distinguishes warnings from errors

---

## Sub-phase 5B: FIR Diagnostic Tests

**Goal**: Verify all annotation validation diagnostics fire correctly.  
**Depends on**: Phase 2 (FIR frontend) + Task 5A.1–5A.4

### Task 5B.1: Route Collision Tests

Two `@Destination` annotations with identical `route` values must produce a compiler error.

```kotlin
@Test
fun `duplicate routes produce error`() {
    val result = CompilerTestHelper.compile(
        TestSources.duplicateRouteStack,
        expectSuccess = false,
    )
    result.assertHasError("Duplicate route")
}
```

**Test cases**:
- Same route in same `@Stack` → error
- Same route across different `@Stack`s → error
- Different routes → no error

**Acceptance Criteria**:
- [ ] Duplicate route produces error with route value in message
- [ ] Non-duplicate routes compile cleanly

---

### Task 5B.2: Argument Parity Tests

Route pattern `{param}` must match `@Argument`-annotated properties in the destination class.

**Test cases**:
- Route has `{id}` but destination has no `@Argument val id` → error
- Destination has `@Argument val name` but route has no `{name}` → warning
- Route `{id}` matches `@Argument val id: String` → no diagnostic
- Multiple arguments, one missing → error with specific parameter name

**Acceptance Criteria**:
- [ ] Missing route parameter → error identifying the parameter
- [ ] Extra `@Argument` not in route → warning
- [ ] Matching parameters → no diagnostic

---

### Task 5B.3: Container Role Validation

`@Pane` must include at least a `PRIMARY` role among its `@PaneItem` children.

**Test cases**:
- `@Pane` with `@PaneItem(role = PaneRole.PRIMARY)` + `@PaneItem(role = PaneRole.DETAIL)` → no error
- `@Pane` with only `@PaneItem(role = PaneRole.DETAIL)` → error (missing PRIMARY)
- `@Tabs` with no `@TabItem` → error
- `@Tabs` with `@TabItem` children → no error

**Acceptance Criteria**:
- [ ] Missing PRIMARY role in pane → error
- [ ] Empty tab container → error
- [ ] Valid containers → no diagnostic

---

### Task 5B.4: Transition Compatibility Tests

`@Transition` annotation must reference a valid `NavTransition` instance.

**Test cases**:
- `@Transition` with valid `NavTransition` object → no error
- `@Transition` on a non-destination class → error
- `@Transition` referencing non-existent transition → error

**Acceptance Criteria**:
- [ ] Valid transition references compile cleanly
- [ ] Invalid references produce error with class name

---

### Task 5B.5: Orphan `@Screen` Tests

`@Screen` referencing a destination class that has no `@Destination` annotation should produce a warning.

**Test cases**:
- `@Screen(SomeClass::class)` where `SomeClass` has no `@Destination` → warning
- `@Screen(ValidDestination::class)` → no warning
- `@Screen` function with wrong parameter signature → error

**Acceptance Criteria**:
- [ ] Orphan screen → warning with destination class name
- [ ] Valid screen binding → no diagnostic

---

### Task 5B.6: Positive Diagnostic Tests (No-Error Baseline)

Compile a comprehensive set of correctly-annotated sources and assert zero diagnostics.

**Test cases**:
- Full stack with destinations, screens, arguments, deep links
- Tabs with tab items and tab container
- Panes with pane items (PRIMARY + DETAIL) and pane container
- Transitions on destinations
- Scope annotations

**Acceptance Criteria**:
- [ ] All positive test sources compile with zero errors and zero warnings
- [ ] Covers every annotation type at least once

---

## Sub-phase 5C: IR Codegen Tests (Box Tests)

**Goal**: Verify generated `NavigationConfig` and `DeepLinkHandler` are functionally correct.  
**Depends on**: Phase 3 (IR backend) + Task 5A.1–5A.3

> **Pattern**: Compile source → load compiled classes via `result.classLoader` → instantiate generated object → call methods → assert results.

### Task 5C.1: Basic `@Stack` + `@Destination` → `NavigationConfig` Exists

```kotlin
@Test
fun `NavigationConfig is generated and callable`() {
    val result = CompilerTestHelper.compile(TestSources.basicStack)
    val configClass = result.classLoader.loadClass("TestNavigationConfig")
    val instance = configClass.kotlin.objectInstance
    assertNotNull(instance)
    assertTrue(instance is NavigationConfig)
}
```

**Acceptance Criteria**:
- [ ] `TestNavigationConfig` object exists in compiled output
- [ ] Implements `NavigationConfig` interface
- [ ] `buildNavNode()` returns non-null `StackNode`
- [ ] `roots` property contains the stack

---

### Task 5C.2: Screen Registry Dispatch

Verify `@Screen` functions are wired to the correct destinations in the screen registry.

**Test cases**:
- `getScreen(HomeDestination.Feed::class)` returns non-null composable lambda
- `getScreen(HomeDestination.Article::class)` returns non-null composable lambda
- `getScreen(UnknownDestination::class)` returns null or throws

**Acceptance Criteria**:
- [ ] Each destination maps to exactly one screen composable
- [ ] Unmapped destinations return null

---

### Task 5C.3: Deep Link Handling

Compile destinations with routes containing parameters. Verify `DeepLinkHandler.resolve(uri)` returns the correct destination instance with extracted arguments.

**Test cases**:
- `resolve("basic/home")` → `BasicDestination.Home`
- `resolve("basic/detail/123")` → `BasicDestination.Detail(id = "123")`
- `resolve("nonexistent/path")` → null
- `createUri(BasicDestination.Detail("456"))` → `"basic/detail/456"`

**Acceptance Criteria**:
- [ ] Route patterns match correctly
- [ ] Arguments are extracted into destination properties
- [ ] URI creation round-trips correctly
- [ ] Unknown URIs return null

---

### Task 5C.4: Tabs Container Building

Compile `@Tabs` + `@TabItem` annotations and verify the generated `NavigationConfig` builds a correct `TabNode` structure.

**Test cases**:
- `buildNavNode()` returns tree containing `TabNode`
- `TabNode` children match `@TabItem` declarations in order
- Each tab child is a `StackNode` with the correct start destination

**Acceptance Criteria**:
- [ ] `TabNode` present in nav tree
- [ ] Correct number of tab children
- [ ] Tab ordering matches annotation order
- [ ] Start destinations are correct

---

### Task 5C.5: Pane Container Building

Compile `@Pane` + `@PaneItem` with `PaneRole.PRIMARY` and `PaneRole.DETAIL`. Verify `PaneNode` structure.

**Test cases**:
- `PaneNode` is present in nav tree
- Primary and detail stacks identified correctly
- `PaneRoleRegistry` returns correct role for each destination

**Acceptance Criteria**:
- [ ] `PaneNode` present in tree
- [ ] PRIMARY and DETAIL stacks correctly assigned
- [ ] `PaneRoleRegistry.getRole()` returns expected roles

---

### Task 5C.6: Transition Registry

Compile destinations with `@Transition` and verify `TransitionRegistry.getTransition()` returns the correct `NavTransition`.

**Test cases**:
- Destination with `@Transition` → returns specified `NavTransition`
- Destination without `@Transition` → returns null (uses default)

**Acceptance Criteria**:
- [ ] Annotated destinations return correct transition
- [ ] Non-annotated destinations return null

---

### Task 5C.7: Scope Registry

Compile destinations with scope annotations and verify `ScopeRegistry.isInScope()` and `ScopeRegistry.getScopeKey()`.

**Test cases**:
- Destination with explicit scope → `isInScope()` returns true for that scope
- Destination with different scope → `isInScope()` returns false
- `getScopeKey()` returns correct key string

**Acceptance Criteria**:
- [ ] Scope membership correctly resolved
- [ ] Scope keys match expected values

---

### Task 5C.8: Deep Link Argument Type Tests

Verify all supported argument types are extracted correctly from route patterns.

| Type | Route | URI | Expected Value |
|------|-------|-----|----------------|
| `String` | `route/{name}` | `route/hello` | `"hello"` |
| `Int` | `route/{count}` | `route/42` | `42` |
| `Long` | `route/{id}` | `route/9999999999` | `9999999999L` |
| `Float` | `route/{score}` | `route/3.14` | `3.14f` |
| `Double` | `route/{precise}` | `route/3.14159` | `3.14159` |
| `Boolean` | `route/{flag}` | `route/true` | `true` |
| `Enum` | `route/{status}` | `route/ACTIVE` | `Status.ACTIVE` |

**Acceptance Criteria**:
- [ ] All 7 argument types extract correctly
- [ ] Type conversion errors produce meaningful exceptions
- [ ] Enum values are case-sensitive

---

### Task 5C.9: Complex Graph — Full Navigation Graph

Compile a source set that combines stacks, tabs, panes, deep links, transitions, and scopes into a single navigation graph. Verify the entire `NavigationConfig` is coherent.

**Test cases**:
- `buildNavNode()` returns a well-formed tree
- All registries (screen, scope, transition, container, deep link, pane role) populated
- `plus()` operator works to merge two configs
- Navigation through the tree follows expected paths

**Acceptance Criteria**:
- [ ] Complex graph compiles without errors
- [ ] All 6 registries return expected data
- [ ] Tree structure matches annotation declarations
- [ ] `plus()` operator merges without conflicts

---

### Task 5C.10: Multi-Module Aggregation Tests

**Depends on**: Phase 4 (multi-module auto-discovery)

Simulate multi-module setup by compiling "feature" source sets separately with different `modulePrefix` values, then compiling a "root" module with `@NavigationRoot` that depends on both.

**Test cases**:
- Root module discovers `Feature1NavigationConfig` and `Feature2NavigationConfig`
- Aggregated config contains destinations from all modules
- Screen registry dispatches correctly across modules
- Deep links resolve across module boundaries

**Acceptance Criteria**:
- [ ] Auto-discovery finds all module configs
- [ ] Merged config has no missing registrations
- [ ] Cross-module navigation works correctly

---

## Sub-phase 5D: IR Verification & Regression

**Goal**: Catch structural IR issues and detect unintended output drift.  
**Depends on**: Phase 3 + Task 5A.1–5A.2

### Task 5D.1: Enable `-Xverify-ir` in All Test Compilations

Add the `-Xverify-ir` flag to `CompilerTestHelper` so every box test also validates IR structural integrity.

```kotlin
// In CompilerTestHelper.compile()
kotlincArguments = listOf("-Xverify-ir")
```

**Acceptance Criteria**:
- [ ] All existing box tests pass with `-Xverify-ir` enabled
- [ ] Any IR structural violation fails the test immediately

---

### Task 5D.2: Set Up Textual IR Dump Generation

Configure test compilations to dump IR output before and after the Quo-Vadis IR lowering phase using `-Xphases-to-dump-before` and `-Xphases-to-dump-after`.

```kotlin
kotlincArguments = listOf(
    "-Xverify-ir",
    "-Xphases-to-dump-after=QuoVadisIrGeneration",
)
```

**Files**:
- `CompilerTestHelper.kt` — add IR dump configuration
- Dump output directory: `quo-vadis-compiler-plugin/src/test/resources/ir-dumps/`

**Acceptance Criteria**:
- [ ] IR dumps are generated as text files
- [ ] Dump output is deterministic for the same input

---

### Task 5D.3: Create Golden File Comparison for IR Dumps

Implement a test that compares IR dump output against checked-in golden files. Any difference fails the test, requiring explicit golden file update.

**Flow**:
1. Test compiles source with IR dump enabled
2. Reads expected output from `src/test/resources/ir-golden/{testName}.txt`
3. Compares actual vs expected
4. On mismatch: fails with diff
5. Update mode: set system property `-DupdateGoldenFiles=true` to overwrite

**Files**:
- `quo-vadis-compiler-plugin/src/test/kotlin/com/jermey/quo/vadis/compiler/testing/GoldenFileComparator.kt`
- `quo-vadis-compiler-plugin/src/test/resources/ir-golden/` — golden files directory

**Acceptance Criteria**:
- [ ] Golden file comparison works for basic stack IR output
- [ ] Mismatch produces readable diff in test failure message
- [ ] Update mode overwrites golden files when flag is set
- [ ] Golden files checked into version control

---

### Task 5D.4: Set Up CI Job for IR Regression Detection

Add CI configuration (GitHub Actions) that:
1. Runs all compiler plugin tests with `-Xverify-ir`
2. Fails if any golden file diff detected
3. Runs on every PR against `main`

**Files**:
- `.github/workflows/compiler-plugin-tests.yml` (or update existing CI config)

**Acceptance Criteria**:
- [ ] CI runs compiler plugin tests on every PR
- [ ] IR golden file changes require explicit update commit
- [ ] `-Xverify-ir` failures block merge

---

## Sub-phase 5E: E2E Demo App Verification

**Goal**: Prove the compiler plugin works end-to-end on all target platforms.  
**Depends on**: Phase 3 (single-module), Phase 4 (multi-module)

### Task 5E.1: Create Gradle Build Variant for Compiler Plugin

Add a Gradle property or build flag to switch `composeApp`, `feature1`, and `feature2` from KSP to compiler plugin.

**Approach**:

```kotlin
// composeApp/build.gradle.kts
val useCompilerPlugin: Boolean = project.findProperty("quoVadis.useCompilerPlugin")?.toString()?.toBoolean() ?: false

if (useCompilerPlugin) {
    // Apply compiler plugin (no KSP)
    // Gradle plugin handles registration automatically
} else {
    // Existing KSP setup
}
```

**Usage**: `./gradlew :composeApp:build -PquoVadis.useCompilerPlugin=true`

**Files**:
- `composeApp/build.gradle.kts` — conditional plugin application
- `feature1/build.gradle.kts` — same
- `feature2/build.gradle.kts` — same

**Acceptance Criteria**:
- [ ] Default build still uses KSP (no regression)
- [ ] `-PquoVadis.useCompilerPlugin=true` switches to compiler plugin
- [ ] Both modes produce a compilable project

---

### Task 5E.2: Verify `composeApp` Builds and Runs on Desktop

```bash
./gradlew :composeApp:run -PquoVadis.useCompilerPlugin=true
```

**Verification**:
- App launches without crash
- Navigation between screens works
- Tabs and panes render correctly
- Deep links resolve
- Back navigation works

**Acceptance Criteria**:
- [ ] Desktop app launches successfully
- [ ] All demo navigation patterns work
- [ ] No runtime exceptions in console

---

### Task 5E.3: Verify `composeApp` Builds on Android

```bash
./gradlew :composeApp:assembleDebug -PquoVadis.useCompilerPlugin=true
```

**Acceptance Criteria**:
- [ ] Android APK/AAB builds without errors
- [ ] App installs and launches on emulator/device

---

### Task 5E.4: Verify `composeApp` Builds on iOS

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -PquoVadis.useCompilerPlugin=true
```

**Acceptance Criteria**:
- [ ] iOS framework compiles successfully
- [ ] App launches in iOS Simulator

---

### Task 5E.5: Verify `composeApp` Builds on JS/WasmJS

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun -PquoVadis.useCompilerPlugin=true
./gradlew :composeApp:wasmJsBrowserDevelopmentRun -PquoVadis.useCompilerPlugin=true
```

**Acceptance Criteria**:
- [ ] JS build compiles and runs in browser
- [ ] WasmJS build compiles and runs in browser

---

### Task 5E.6: Verify Multi-Module with Compiler Plugin

```bash
./gradlew build -PquoVadis.useCompilerPlugin=true
```

Verify `feature1` and `feature2` module configs are discovered and merged by `composeApp`.

**Acceptance Criteria**:
- [ ] `feature1` and `feature2` compile with compiler plugin
- [ ] `composeApp` discovers and merges feature configs
- [ ] Navigation to feature module destinations works

---

## Sub-phase 5F: KSP Deprecation

**Goal**: Formally deprecate the KSP module while maintaining parallel support.  
**Depends on**: Sub-phase 5E complete (compiler plugin verified working)

### Task 5F.1: Add `@Deprecated` Annotations to KSP Module

Add `@Deprecated` to key KSP public APIs with `ReplaceWith` hints.

```kotlin
@Deprecated(
    message = "KSP processor is deprecated. Migrate to the Quo-Vadis compiler plugin. " +
              "See https://github.com/jermeyyy/quo-vadis/blob/main/docs/MIGRATION.md",
    level = DeprecationLevel.WARNING,
)
class QuoVadisSymbolProcessor : SymbolProcessor { /* ... */ }
```

**Files**:
- `quo-vadis-ksp/src/main/kotlin/.../QuoVadisSymbolProcessor.kt`
- `quo-vadis-ksp/src/main/kotlin/.../QuoVadisSymbolProcessorProvider.kt`

**Acceptance Criteria**:
- [ ] KSP processor classes annotated with `@Deprecated`
- [ ] Deprecation level is `WARNING` (not `ERROR` — preserve functionality)
- [ ] Message includes migration guide URL

---

### Task 5F.2: Gradle Plugin Configuration Flag

Add `useCompilerPlugin` and `useKsp` flags to `QuoVadisExtension`.

```kotlin
abstract class QuoVadisExtension {
    abstract val modulePrefix: Property<String>
    abstract val useLocalKsp: Property<Boolean>

    /** Use the native K2 compiler plugin (default: true). */
    abstract val useCompilerPlugin: Property<Boolean>

    /**
     * Use the legacy KSP processor instead of the compiler plugin.
     * @deprecated Migrate to the compiler plugin. KSP support will be removed in a future version.
     */
    @Deprecated("Use useCompilerPlugin = true instead. KSP will be removed in a future version.")
    abstract val useKsp: Property<Boolean>
}
```

**Gradle plugin logic**:
- `useCompilerPlugin = true` (default) → registers `KotlinCompilerPluginSupportPlugin`, skips KSP
- `useKsp = true` → applies KSP as before, emits Gradle build warning
- Both true → error
- Both false → error

**Files**:
- `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisExtension.kt`
- `quo-vadis-gradle-plugin/src/main/kotlin/.../QuoVadisPlugin.kt`

**Acceptance Criteria**:
- [ ] Default behavior uses compiler plugin (no user configuration needed)
- [ ] `useKsp = true` still works with deprecation warning in Gradle output
- [ ] Conflicting flags produce clear error message
- [ ] `useLocalKsp` only applies when `useKsp = true`

---

### Task 5F.3: Runtime Deprecation Warning

When the runtime detects a KSP-generated `NavigationConfig` (identifiable by a marker annotation or specific class pattern), log a deprecation warning.

```kotlin
// In NavigationHost or Navigator initialization
if (config::class.hasAnnotation<KspGenerated>()) {
    logger.warn(
        "Quo-Vadis: This NavigationConfig was generated by KSP. " +
        "Migrate to the compiler plugin for better IDE support and faster builds. " +
        "See: docs/MIGRATION.md"
    )
}
```

**Alternative approach**: KSP generator adds a `@KspGenerated` marker annotation to generated classes. Runtime checks for this marker.

**Acceptance Criteria**:
- [ ] KSP-generated config triggers one-time warning at startup
- [ ] Compiler-plugin-generated config produces no warning
- [ ] Warning includes migration guide reference

---

### Task 5F.4: Set KSP Removal Timeline

Document the deprecation schedule:

| Version | KSP Status |
|---------|------------|
| Current + 1 minor | Deprecated with `WARNING` — KSP and compiler plugin both supported |
| Current + 2 minor | Deprecated with `WARNING` — migration guide prominently featured in release notes |
| Current + 3 minor | `DeprecationLevel.ERROR` — KSP fails to compile, must migrate |
| Current + 4 minor | KSP module removed from repository |

**Files**:
- `CHANGELOG.md` — add deprecation notice
- `README.md` — update to show compiler plugin as primary approach

**Acceptance Criteria**:
- [ ] Timeline documented in CHANGELOG
- [ ] README reflects compiler plugin as default
- [ ] KSP module README states deprecation status

---

## Sub-phase 5G: Migration Guide

**Goal**: Enable existing users to migrate from KSP to compiler plugin with zero ambiguity.  
**Depends on**: Sub-phases 5E + 5F (functionality verified, deprecation in place)

### Task 5G.1: Write Migration Guide — Build Configuration Changes

Document exact `build.gradle.kts` changes:

**Content**:
1. Remove `alias(libs.plugins.ksp)` from `plugins {}` block
2. Remove `quoVadis { useLocalKsp = true }` if present
3. Ensure `alias(libs.plugins.quoVadis)` is applied (now auto-configures compiler plugin)
4. Remove `ksp(...)` dependency declarations if manually added
5. Update `libs.versions.toml` — bump quo-vadis version to compiler plugin release

**Files**:
- `docs/MIGRATION.md`

**Acceptance Criteria**:
- [ ] Step-by-step instructions with before/after code blocks
- [ ] Covers single-module and multi-module setups
- [ ] Includes version requirements (minimum Kotlin version)

---

### Task 5G.2: Document Annotation Retention Changes

Explain the `SOURCE` → `BINARY` retention change and its implications.

**Content**:
- Why retention changed (cross-module visibility)
- Impact: annotations now visible in compiled `.klib`/`.jar` metadata
- No action required from users — annotations are the same, only retention differs
- If users inspect metadata (rare), they will now see Quo-Vadis annotations

**Files**:
- `docs/MIGRATION.md` — section within migration guide

**Acceptance Criteria**:
- [ ] Retention change explained clearly
- [ ] Users understand no code changes needed for this specifically

---

### Task 5G.3: Document `@NavigationRoot` Setup for Multi-Module

Explain how multi-module aggregation changes from manual `+` operator to automatic discovery.

**Content**:
- Before: `val config = Feature1NavigationConfig + Feature2NavigationConfig + AppNavigationConfig`
- After: Add `@NavigationRoot` to app module's sealed destination, all feature configs discovered automatically
- Manual `+` still works as fallback
- `@NavigationRoot` annotation usage: placement, constraints

**Files**:
- `docs/MIGRATION.md` — section within migration guide

**Acceptance Criteria**:
- [ ] Before/after comparison for multi-module setup
- [ ] `@NavigationRoot` usage documented with example
- [ ] Fallback approach documented

---

### Task 5G.4: Document Behavioral Differences

Document any differences between KSP-generated and compiler-plugin-generated output.

**Content**:
- Expected: behavior is identical
- Generated class names remain the same (`{Prefix}NavigationConfig`, `{Prefix}DeepLinkHandler`)
- No physical generated source files (can't inspect — use IR dump mode for debugging)
- IDE autocomplete should work without `build` step (FIR synthetic declarations)
- Known differences (if any discovered during Phase 5 testing)

**Files**:
- `docs/MIGRATION.md` — section within migration guide

**Acceptance Criteria**:
- [ ] All known differences documented
- [ ] Debugging approach documented (no generated source to read)
- [ ] IDE behavior differences noted

---

### Task 5G.5: FAQ Section

**Content**:

| Question | Answer |
|----------|--------|
| Do I need to change my annotations? | No — same annotations, same properties |
| Will my deep links still work? | Yes — same route patterns, same argument extraction |
| What Kotlin version is required? | ≥ 2.x.x (K2 compiler required) |
| Can I use KSP and compiler plugin in the same module? | No — pick one per module |
| Can different modules use different backends? | Technically yes, but not recommended. All modules should migrate together |
| My IDE doesn't show generated classes — how do I fix it? | Ensure K2 mode is enabled in IDE settings. Invalidate caches if needed |
| How do I debug what the compiler plugin generates? | Use `-PquoVadis.dumpIr=true` gradle property to dump IR output |
| I'm getting a "duplicate class" error — what happened? | You likely have both KSP and compiler plugin active. Remove KSP plugin |

**Files**:
- `docs/MIGRATION.md` — FAQ section

**Acceptance Criteria**:
- [ ] At least 8 FAQ entries covering common migration scenarios
- [ ] Answers are actionable, not vague

---

### Task 5G.6: Publish Migration Guide in `docs/` Directory

Ensure migration guide is linked from README, CHANGELOG, and documentation site.

**Files**:
- `docs/MIGRATION.md` — primary document
- `README.md` — add link to migration guide
- `CHANGELOG.md` — reference in deprecation notice
- `docs/site/` — add migration page to documentation site (if applicable)

**Acceptance Criteria**:
- [ ] `docs/MIGRATION.md` exists and is complete
- [ ] README links to migration guide
- [ ] CHANGELOG references migration guide
- [ ] Documentation site updated (if applicable)

---

## Sub-phase 5H: CI/CD & Publishing

**Goal**: Ensure compiler plugin artifacts are published and CI validates everything.  
**Depends on**: Phase 1 (module infrastructure)

### Task 5H.1: Configure Publishing for `quo-vadis-compiler-plugin`

Set up Maven Central publishing using the existing `vanniktech-maven-publish` plugin (already used by other modules).

```kotlin
// quo-vadis-compiler-plugin/build.gradle.kts
plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Quo Vadis Compiler Plugin")
        description.set("K2 compiler plugin for Quo Vadis navigation library")
        // ... standard POM metadata
    }
}
```

**Artifact coordinates**: `com.jermey.quo-vadis:quo-vadis-compiler-plugin:<version>`

**Acceptance Criteria**:
- [ ] `./gradlew :quo-vadis-compiler-plugin:publishToMavenLocal` succeeds
- [ ] Published POM has correct metadata
- [ ] Artifact is a JAR containing compiler plugin registrar + SPI manifest

---

### Task 5H.2: Configure Publishing for `quo-vadis-compiler-plugin-native`

Same as 5H.1 but for the native-specific plugin wrapper.

**Artifact coordinates**: `com.jermey.quo-vadis:quo-vadis-compiler-plugin-native:<version>`

**Acceptance Criteria**:
- [ ] `./gradlew :quo-vadis-compiler-plugin-native:publishToMavenLocal` succeeds
- [ ] Artifact compatible with Kotlin/Native compiler

---

### Task 5H.3: Automated Source Sync (Compiler Plugin → Native)

The `compiler-plugin-native` module wraps the same FIR/IR logic but targets the native compiler host. Set up a Gradle task or symbolic approach to keep them in sync.

**Options**:
1. **Source symlinks** — `compiler-plugin-native/src` → `compiler-plugin/src` (platform-specific entry point only differs)
2. **Gradle `sourceSets` sharing** — `compiler-plugin-native` includes `compiler-plugin`'s source directories
3. **Copy task** — Gradle task copies source on build

**Recommended**: Option 2 (Gradle source set sharing) — least fragile, works on all OS.

```kotlin
// quo-vadis-compiler-plugin-native/build.gradle.kts
sourceSets {
    main {
        kotlin.srcDir(project(":quo-vadis-compiler-plugin").file("src/main/kotlin"))
    }
}
```

**Acceptance Criteria**:
- [ ] Both modules compile from same source
- [ ] Changes to compiler-plugin source are reflected in native module without manual steps
- [ ] Both modules pass their respective tests

---

### Task 5H.4: CI Pipeline

Configure GitHub Actions (or existing CI) with:

```yaml
# .github/workflows/compiler-plugin.yml
jobs:
  test:
    steps:
      - name: Compiler Plugin Unit Tests
        run: ./gradlew :quo-vadis-compiler-plugin:test

      - name: IR Verification
        run: ./gradlew :quo-vadis-compiler-plugin:test -Dverify.ir=true

      - name: Golden File Check
        run: ./gradlew :quo-vadis-compiler-plugin:test -DcheckGoldenFiles=true

      - name: E2E Desktop Build (Compiler Plugin)
        run: ./gradlew :composeApp:jvmJar -PquoVadis.useCompilerPlugin=true

      - name: E2E Android Build (Compiler Plugin)
        run: ./gradlew :composeApp:assembleDebug -PquoVadis.useCompilerPlugin=true

      - name: Publish Snapshot
        if: github.ref == 'refs/heads/main'
        run: ./gradlew publish -PSNAPSHOT=true
```

**Acceptance Criteria**:
- [ ] CI runs on every PR
- [ ] All test layers execute (unit, box, IR verification, golden files)
- [ ] E2E build validation included
- [ ] Snapshot publishing on main branch merge
- [ ] Failures block PR merge

---

### Task 5H.5: Release Checklist Document

Create a release checklist for compiler plugin versions.

**Content**:
1. All tests pass (unit, box, IR, E2E)
2. Golden files up-to-date
3. Version bumped in `gradle.properties` / `libs.versions.toml`
4. CHANGELOG updated
5. Migration guide updated (if behavioral changes)
6. Both `compiler-plugin` and `compiler-plugin-native` published
7. Gradle plugin updated to reference new artifact version
8. Demo app verified on all platforms
9. Tag release in Git
10. Publish to Maven Central (staging → release)

**Files**:
- `plans/compiler-plugin/RELEASE-CHECKLIST.md`

**Acceptance Criteria**:
- [ ] Checklist covers all publishing steps
- [ ] No manual step is ambiguous
- [ ] Includes rollback procedure

---

## Acceptance Criteria Summary

### Sub-phase 5A: Test Infrastructure
- [ ] `kotlin-compile-testing` dependency configured and resolving
- [ ] `CompilerTestHelper` compiles trivial source with plugin registrar
- [ ] Test source templates cover all annotation types
- [ ] Diagnostic assertion utilities work for errors, warnings, and clean compilations

### Sub-phase 5B: FIR Diagnostic Tests
- [ ] Route collision produces error
- [ ] Argument parity mismatch produces error/warning
- [ ] Missing PRIMARY pane role produces error
- [ ] Transition validation works
- [ ] Orphan screen produces warning
- [ ] All valid annotations compile with zero diagnostics

### Sub-phase 5C: IR Codegen Box Tests
- [ ] `NavigationConfig` object generated and implements interface
- [ ] Screen registry dispatches correctly
- [ ] Deep links resolve with type-safe arguments (all 7 types)
- [ ] Tabs build correct `TabNode` structure
- [ ] Panes build correct `PaneNode` structure with roles
- [ ] Transitions registered correctly
- [ ] Scopes registered correctly
- [ ] Complex full-graph test passes
- [ ] Multi-module aggregation works (Phase 4 dependency)

### Sub-phase 5D: IR Verification & Regression
- [ ] `-Xverify-ir` enabled in all tests, passes
- [ ] IR dump generation configured
- [ ] Golden file comparison functional
- [ ] CI job configured for regression detection

### Sub-phase 5E: E2E Demo App Verification
- [ ] Build variant flag works for KSP ↔ compiler plugin switching
- [ ] Desktop build and run succeeds
- [ ] Android build succeeds
- [ ] iOS build succeeds
- [ ] JS and WasmJS builds succeed
- [ ] Multi-module (feature1 + feature2) works with compiler plugin

### Sub-phase 5F: KSP Deprecation
- [ ] `@Deprecated` on KSP public APIs
- [ ] Gradle plugin flag defaults to compiler plugin
- [ ] Runtime deprecation warning for KSP-generated configs
- [ ] Removal timeline documented

### Sub-phase 5G: Migration Guide
- [ ] Build configuration changes documented step-by-step
- [ ] Annotation retention changes explained
- [ ] `@NavigationRoot` multi-module setup documented
- [ ] Behavioral differences listed
- [ ] FAQ with ≥8 entries
- [ ] Guide published and linked from README + CHANGELOG

### Sub-phase 5H: CI/CD & Publishing
- [ ] Both compiler plugin modules publishable to Maven Central
- [ ] Source sync between plugin and native module automated
- [ ] CI pipeline runs all test layers
- [ ] Release checklist document exists

---

## Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| `kotlin-compile-testing` fork lags behind Kotlin 2.3.x | High | Medium | Pin to known-working version; contribute upstream if needed; fallback to Gradle TestKit-based integration tests |
| IR dump format changes between Kotlin versions | Medium | High | Golden files tied to specific Kotlin version; CI matrix tests multiple versions; auto-update mode for golden files |
| E2E platform builds have non-plugin-related failures | Medium | Medium | Run E2E with KSP first as baseline; isolate plugin-specific failures from platform toolchain issues |
| KSP users resist migration | Low | Medium | Generous deprecation timeline (3 minor versions); clear migration guide; no behavioral changes |
| Compose compiler interaction differs between KSP and plugin output | High | Low | Box tests specifically test composable screen dispatch; E2E tests exercise actual Compose rendering |
| Native compiler plugin diverges from JVM/JS plugin | Medium | Medium | Automated source sync (Task 5H.3); run IR tests on native target in CI |
| Runtime deprecation warning false-positives | Low | Low | Use explicit marker annotation rather than heuristic detection |

---

## Sequencing Notes

### Can Start During Earlier Phases

| Task | Earliest Start | Reason |
|------|---------------|--------|
| 5A.1–5A.4 (test infra) | Phase 1 complete | Only needs plugin registrar to exist, not FIR/IR logic |
| 5B.* (FIR tests) | Phase 2 in progress | Write tests as diagnostics are implemented |
| 5C.* (box tests) | Phase 3 in progress | Write tests as IR lowering features land |
| 5D.1–5D.3 (IR verification) | Phase 3 in progress | Can validate IR output incrementally |
| 5F.2 (Gradle flag) | Phase 1 complete | Gradle plugin changes are independent of plugin logic |
| 5G.1–5G.2 (migration docs) | Phase 2 complete | Build config changes are known once infrastructure exists |
| 5H.1–5H.3 (publishing) | Phase 1 complete | Publishing config is independent of plugin logic |
| 5H.4 (CI) | 5A.1 complete | CI can run even a single test |

### Must Wait For

| Task | Blocker | Reason |
|------|---------|--------|
| 5C.10 (multi-module box tests) | Phase 4 complete | Requires `@NavigationRoot` and metadata aggregation |
| 5E.* (E2E verification) | Phase 3 + 4 complete | Full plugin functionality needed |
| 5F.1, 5F.3, 5F.4 (KSP deprecation) | 5E complete | Don't deprecate until replacement is verified |
| 5G.3–5G.6 (migration guide finalization) | 5E + 5F complete | Guide needs verified compiler plugin + deprecation in place |
| 5H.5 (release checklist) | 5H.1–5H.4 complete | Checklist covers full pipeline |

### Recommended Execution Order

```
Phase 1 complete
    ├── Start 5A.1–5A.4 (test infrastructure)
    ├── Start 5F.2 (Gradle plugin flag — stub)
    └── Start 5H.1–5H.3 (publishing config)
Phase 2 in progress
    ├── Start 5B.* (FIR diagnostic tests — evolve with Phase 2)
    └── Start 5G.1–5G.2 (draft migration docs for build changes)
Phase 3 in progress
    ├── Start 5C.1–5C.9 (box tests — evolve with Phase 3)
    ├── Start 5D.1–5D.3 (IR verification)
    └── Start 5H.4 (CI pipeline — run whatever tests exist)
Phase 4 complete
    ├── Complete 5C.10 (multi-module box tests)
    └── Start 5E.* (E2E verification)
5E complete
    ├── Complete 5F.* (KSP deprecation)
    ├── Complete 5G.* (migration guide)
    └── Complete 5D.4 (CI golden file enforcement)
5F + 5G complete
    └── Complete 5H.5 (release checklist)
```

---

## Related Documents

- [Phase 0: Overview](00-overview.md)
- [Phase 1: Infrastructure & Gradle Plugin](01-infrastructure.md)
- [Phase 2: FIR Frontend](02-fir-frontend.md)
- [Phase 3: IR Backend](03-ir-backend.md)
- [Phase 4: Multi-Module Auto-Discovery](04-multi-module.md)
- [KSP Analysis Report](../ksp-analysis-report.md)
