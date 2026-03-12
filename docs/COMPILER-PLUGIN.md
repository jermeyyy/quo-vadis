# Compiler Plugin Guide (Experimental)

This guide explains how to evaluate Quo Vadis's experimental compiler plugin as an alternative to KSP for annotation-based navigation.

> [!WARNING]
> The compiler plugin is experimental.
> Prefer KSP for the stable/default setup today.
> Do not enable KSP and the compiler plugin in the same module.
> If evaluation goes poorly, you can switch the module back to KSP without changing your navigation annotations.

## Overview

Quo Vadis supports two ways to generate annotation-based navigation infrastructure:

- **KSP** is the stable and recommended backend for production use.
- **Compiler plugin** is an experimental backend for early adopters who want to validate the K2-based path.

The annotations themselves are the frontend API. You still write `@Stack`, `@Destination`, `@Screen`, `@Tabs`, and `@Pane` the same way regardless of backend.

If you do not want a generated annotation pipeline at all, [DSL-Based Configuration](DSL-CONFIG.md) remains the manual, backend-independent alternative.

## When To Use It

Evaluate the compiler plugin if you want to:

- trial the experimental backend on the `compiler-plugin` branch,
- validate multi-module aggregation with `@NavigationRoot`, or
- compare K2 compiler-plugin behavior against your current KSP setup.

Stay on KSP if you want:

- the stable/default Quo Vadis setup,
- the lowest backend risk while Kotlin compiler APIs are still moving, or
- a predictable rollback target while testing the compiler plugin.

## Backend Model

Use this mental model across the docs:

- **Annotations** are the declarative frontend API.
- **KSP** and the **compiler plugin** are alternative backends for those annotations.
- **DSL configuration** is the manual, backend-independent option.

Backend rules:

- Choose exactly one Quo Vadis backend at a time.
- Do not wire Quo Vadis KSP processing and compiler-backend processing for the same module at the same time.
- Mixed-backend applications may compile in some cases, but they are not the recommended evaluation path right now. Prefer one backend across the app while testing.

## Installation

### Compiler Plugin Setup

Set the root backend property to compiler mode:

```properties
# gradle.properties
quoVadis.backend=compiler
```

Then apply the Quo Vadis Gradle plugin in your module:

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp) // optional during the experiment if you want a property-only switch
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
}
```

Requirements:

- Kotlin 2.1.0 or later.
- K2-enabled toolchain support in your IDE and CI.

### KSP Fallback Setup

If you want the stable/default backend instead, set the root backend property back to KSP:

```properties
# gradle.properties
quoVadis.backend=ksp
```

Then keep the normal KSP-capable module setup:

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
}
```

The deprecated `useCompilerPlugin` alias may still exist in older scripts, but `quoVadis.backend` is the primary rollout API now.

Use KSP as the baseline when adopting Quo Vadis for the first time unless you are explicitly evaluating the experimental backend.

## Switching Backends

To move a module from KSP to the compiler plugin:

1. Set `quoVadis.backend=compiler` at the root build level.
2. Remove Quo Vadis-specific `ksp(...)` dependencies.
3. Remove KSP-generated source directory wiring.
4. Remove KSP-only local development flags such as `useLocalKsp`.
5. Clean and rebuild the project.

To switch back to KSP:

1. Set `quoVadis.backend=ksp` at the root build level.
2. Restore Quo Vadis KSP dependencies if you removed them entirely.
3. Clean and rebuild.

If you are migrating an existing KSP project, use [MIGRATION.md](MIGRATION.md) for the detailed cutover checklist.

## Multi-Module Behavior

The main behavioral difference is how module-level configs are assembled.

With KSP, you typically combine generated configs manually:

```kotlin
val appConfig = Feature1NavigationConfig + Feature2NavigationConfig + AppNavigationConfig
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
```

With the compiler plugin, you can use `@NavigationRoot` to aggregate module configs:

```kotlin
@NavigationRoot
object MyApp

val navigator = rememberQuoVadisNavigator(MainTabs::class, MyAppNavigationConfig)
```

Manual config composition is still a valid fallback even when evaluating the compiler plugin.

## Generated Output Model

The generated API surface is intended to stay familiar, but the output model changes:

- KSP emits generated `.kt` source files.
- The compiler plugin provides synthetic declarations and IR transformations instead of checked-in or build-generated Kotlin source files.

Practical implications:

- IDE completion depends on K2/compiler-plugin support.
- You should not expect `build/generated/ksp/...` output when using the compiler plugin.
- Debugging differs from KSP because there are no generated source files to inspect directly.

## Compatibility And Limitations

- The compiler plugin uses Kotlin's `ExperimentalCompilerApi`.
- Kotlin and toolchain upgrades may affect compiler-plugin behavior sooner than the KSP path.
- The compiler plugin is documented for evaluation, not yet the stable recommendation.
- Do not enable both backends in the same module.

Local development note:

- If you change the compiler plugin implementation in this repository, downstream consumers such as `composeApp` may still resolve the published Maven artifact. Local plugin changes can require publishing the compiler plugin to `mavenLocal` before verifying app behavior.

## Verification Checklist

After enabling the compiler plugin, verify the change with a small parity pass:

1. Run a clean build.
2. Run the same smoke-test flow you use for the KSP-backed app.
3. Confirm navigation config, screen binding, and deep link behavior still match expectations.
4. If you rely on multi-module aggregation, verify the expected `@NavigationRoot` config is available.

Useful commands:

```bash
./gradlew clean :composeApp:desktopJar
./gradlew :feature1:allMetadataJar :feature2:allMetadataJar -PquoVadis.backend=compiler
./scripts/e2e-compiler-plugin.sh all
```

If you are flipping a previously built checkout from KSP to compiler mode or back again, keep the clean step. It prevents stale KSP outputs from producing duplicate or conflicting declarations.

## Rollback

Rollback is intentionally simple:

1. Set `quoVadis.backend=ksp`.
2. Restore Quo Vadis KSP dependencies if you removed them entirely.
3. Restore any required generated-source wiring for your KSP setup.
4. Run a clean build.

The same annotation model works on both backends, so rollback should be mostly a build-configuration change.

## Related Docs

- [Migration Companion](MIGRATION.md)
- [Annotation-Based Navigation Configuration](ANNOTATIONS.md)
- [DSL-Based Configuration](DSL-CONFIG.md)