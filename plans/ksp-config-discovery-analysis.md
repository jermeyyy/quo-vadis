# KSP NavigationConfig Discovery — Cross-Platform Analysis

## Problem Statement

The KSP backend generates `{Prefix}__AggregatedConfig` with an `init {}` block that registers into `NavigationConfigRegistry`. However, Kotlin objects are **lazily initialized** — the init block never executes unless something explicitly references the object. The compiler plugin avoids this entirely by rewriting `navigationConfig<T>()` call sites at IR level to directly return the generated object. The KSP backend has no such rewrite capability.

**Core question:** How can we reliably trigger registration of the KSP-generated aggregated config at runtime across all KMP targets?

## Current Architecture

```
quo-vadis-core (commonMain):
  └── navigationConfig<T>() → NavigationConfigRegistry.get(T::class)
  └── NavigationConfigRegistry.register(rootClass, config)

KSP generates (in composeApp/build/generated/ksp/metadata/commonMain/):
  └── ComposeApp__AggregatedConfig object {
        init { NavigationConfigRegistry.register(AppNavigation::class, this) }
      }

Compiler plugin (IR phase):
  └── Rewrites navigationConfig<AppNavigation>() → IrGetObject(ComposeApp__AggregatedConfig)
  └── No registry needed — direct object reference
```

The `init {}` approach works if and only if something causes `ComposeApp__AggregatedConfig` to be class-loaded / initialized before `navigationConfig<T>()` is called.

## Options Analysis

### Option A: `expect/actual` class loader in quo-vadis-core

**Concept:** Add `expect fun discoverGeneratedConfig(rootClass: KClass<*>): NavigationConfig?` in commonMain, with actual implementations per platform that load the class by predictable name.

**Platform implementations:**
- **JVM/Android:** `Class.forName("com.jermey.quo.vadis.generated.${prefix}__AggregatedConfig").kotlin.objectInstance`
- **JS:** Not possible — JS has no reflection-based class loading. `js("require()")` doesn't work for KMP generated code.
- **WasmJS:** No class loading mechanism at all.
- **Native/iOS:** No `Class.forName` equivalent. `NSClassFromString` only works for Objective-C classes.

**Verdict: ❌ REJECTED** — JS, WasmJS, and Native cannot load classes by name. Only viable for JVM subset.

---

### Option B: KSP generates a resource file (META-INF/services pattern)

**Concept:** KSP writes a resource file containing the mapping `rootClass → configClass`, and `navigationConfig<T>()` reads it at runtime.

**Platform reality:**
- **JVM/Android:** Resource loading works (`ClassLoader.getResourceAsStream`).
- **JS:** No classpath resource loading.
- **WasmJS:** No file/resource access.
- **Native/iOS:** No classpath concept. Could bundle as a file but no standard mechanism.

**Verdict: ❌ REJECTED** — Same platform gap as Option A. Resources are JVM-only in KMP.

---

### Option C: KSP generates init in a place that IS eagerly loaded

**Concept:** Instead of relying on the `init {}` of a standalone object, embed the registration call somewhere that is guaranteed to execute.

**Sub-options:**
  - **C1:** Generate `init {}` inside the root class itself (e.g., `AppNavigation.init { ... }`) — Not possible via KSP; the root class is user-written code.
  - **C2:** Generate a file-level `val` instead of an object `init {}` — File-level `val` is still lazily loaded in most backends (not executed without reference).
  - **C3:** Generate `@EagerInitialization` on a top-level property — Only available on Kotlin/Native. No equivalent for JS/Wasm/JVM.

**Verdict: ❌ REJECTED** — No cross-platform mechanism for eager file-level evaluation.

---

### Option D: KSP generates an extension function on the root class

**Concept:** Instead of using the registry, generate a `fun AppNavigation.Companion.config(): NavigationConfig` or change `navigationConfig<T>()` to look up a well-known companion function.

**Issues:**
- Requires the root class to have a `companion object` — imposes a constraint on user code.
- `inline reified` cannot call a companion method at compile time without knowing the class. At runtime, `T::class` is erased after inlining; we'd need to go through reflection to find the companion method, which loops back to the JVM-only limitation.
- Could use `expect/actual`, but then the companion lookup problem differs per platform.

**Verdict: ❌ REJECTED** — Requires runtime reflection to discover companion methods, which isn't cross-platform.

---

### Option E: Change the API — require explicit reference

**Concept:** Don't try to auto-discover. Require users to explicitly reference the generated config:

```kotlin
// Option E1: Direct object reference
val config = ComposeApp__AggregatedConfig  // forces init

// Option E2: DSL function with explicit import
val config = navigationConfig<AppNavigation>()  // works only with compiler plugin

// Option E3: KSP generates a top-level function instead
val config = composeAppNavigationConfig()  // generated function
```

**Analysis:**
- **E1/E3** work perfectly on all platforms because there's a direct code reference.
- Breaks the `navigationConfig<T>()` API parity goal between backends.
- However, the **existing interchangeability plan** already documents that the compiler plugin rewrites `navigationConfig<T>()` calls — under KSP, the consumer would need a different invocation pattern.

**Verdict: ⚠️ PARTIAL** — Works perfectly but breaks API parity. Could be an acceptable fallback.

---

### Option F: Inline the reference into `navigationConfig<T>()` itself

**Concept:** Make `navigationConfig<T>()` force-reference the aggregated config object before looking it up in the registry.

**The key insight:** `navigationConfig<T>()` is `inline reified`. The KSP can generate a **same-package top-level function** that the `navigationConfig<T>()` inlined body can discover. But wait — `navigationConfig<T>()` is in `quo-vadis-core` which is compiled before the consumer module. It cannot reference KSP-generated code.

**Alternative F2 — KSP generates a wrapper function:**

KSP generates:
```kotlin
// In com.jermey.quo.vadis.generated
fun navigationConfigForAppNavigation(): NavigationConfig {
    return ComposeApp__AggregatedConfig
}
```

Consumer code:
```kotlin
val config = navigationConfigForAppNavigation()
```

This is essentially Option E with a nicer name.

**Verdict: ⚠️ Same as E** — Avoids the discovery problem by avoiding discovery entirely.

---

### ⭐ Option G (RECOMMENDED): Trigger init from `navigationConfig<T>()` using `@GeneratedConfig` annotation scanning at first access

**THIS IS THE WRONG FRAMING.** Let me reconsider the entire problem.

---

## Reframing the Problem

The real insight is that **the KSP-generated code runs in the consumer module** (e.g., `composeApp`). Any code in the consumer module CAN reference the generated objects, because they're in the same compilation unit's generated sources.

The problem is specifically:
1. `navigationConfig<T>()` lives in `quo-vadis-core` (compiled earlier, can't reference generated code).
2. The generated `__AggregatedConfig` has an `init {}` that would register, but nobody triggers it.
3. We need a bridge between the consumer's call to `navigationConfig<T>()` and the generated object.

### ⭐⭐ RECOMMENDED: Option H — KSP generates explicit registration at the call site level

**Concept:** Don't rely on lazy init. Instead, KSP generates a **top-level initializer function** that all generated configs expose, and the `navigationConfig<T>()` function is redesigned to accept an explicit trigger.

But more practically, the simplest and most robust pattern is:

## Final Recommendation: **Combine E + current init approach with a forced-reference guard**

### Approach: KSP generates a `provideNavigationConfig()` function

The KSP backend generates the aggregated config **and** a well-known top-level function:

```kotlin
// KSP-generated: ComposeApp__AggregatedConfig.kt
package com.jermey.quo.vadis.generated

@GeneratedConfig
object ComposeApp__AggregatedConfig : NavigationConfig by (
    ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig
) {
    init {
        NavigationConfigRegistry.register(AppNavigation::class, this)
    }
}
```

No change there. The init block stays as documentation / belt-and-suspenders.

**The actual fix is in how the consumer uses it.** Instead of relying on auto-discovery, change the `navigationConfig<T>()` function signature and the KSP-generated output as follows:

### Design: Two-phase approach

**Phase 1: Make `navigationConfig<T>()` work by forcing object access inline**

```kotlin
// quo-vadis-core, commonMain
@OptIn(InternalQuoVadisApi::class)
inline fun <reified T> navigationConfig(): NavigationConfig {
    // First try the registry (populated by init blocks or explicit registration)
    NavigationConfigRegistry.get(T::class)?.let { return it }

    // If not found, try to force-init the generated config via well-known name resolution
    NavigationConfigRegistry.triggerDiscovery(T::class)

    return NavigationConfigRegistry.get(T::class)
        ?: error("navigationConfig<${T::class.simpleName}>() could not resolve...")
}
```

But `triggerDiscovery` can't load classes cross-platform... **so we're back to square one**.

---

## Actual Final Recommendation

After exhaustive analysis, there are only **two viable cross-platform patterns**:

### Pattern 1 (RECOMMENDED): Explicit reference — the consumer forces object initialization

The KSP-generated `__AggregatedConfig` already registers itself in its `init {}`. The consumer just needs to **reference the object once** before calling `navigationConfig<T>()`.

**KSP generates a helper initialization function:**

```kotlin
// KSP-generated: ComposeApp__Init.kt
package com.jermey.quo.vadis.generated

/**
 * Forces initialization of the aggregated config.
 * Call this once at app startup before using navigationConfig<AppNavigation>().
 *
 * When using the compiler plugin backend, this call is not needed (and is a no-op).
 */
fun initQuoVadisNavigation() {
    // Force object initialization, which triggers the init {} block
    ComposeApp__AggregatedConfig
}
```

**Consumer code (DI.kt):**

```kotlin
@NavigationRoot
object AppNavigation

object DI {
    init {
        // Required for KSP backend; compiler plugin rewrites navigationConfig<T>() directly
        initQuoVadisNavigation()
    }

    fun navigationConfig(): NavigationConfig {
        return navigationConfig<AppNavigation>()
    }
}
```

**When the compiler plugin is active**, `NavigationConfigCallTransformer` rewrites `navigationConfig<AppNavigation>()` → `ComposeApp__AggregatedConfig`, so the `initQuoVadisNavigation()` call is harmless (an extra object reference) but not needed. The compiler plugin could optionally also remove the `initQuoVadisNavigation()` call as dead code, or just leave it.

**Platform compatibility:** ✅ All platforms. Direct object reference works everywhere.

**Trade-off:** Consumer must call `initQuoVadisNavigation()` when using KSP. This is documented and generated by KSP.

---

### Pattern 2 (ALTERNATIVE): Replace registry lookup with direct return

Eliminate the dynamic registry entirely for KSP. Instead, KSP generates a `navigationConfig<T>()` **override function** (or a differently-named function) that directly returns the config:

```kotlin
// KSP-generated replacement wrapper
package com.jermey.quo.vadis.generated

inline fun <reified T> resolvedNavigationConfig(): NavigationConfig {
    return ComposeApp__AggregatedConfig
}
```

The consumer code calls the generated function directly:

```kotlin
val config = resolvedNavigationConfig<AppNavigation>()
// or simply:
val config = ComposeApp__AggregatedConfig
```

**Trade-off:** Different API from compiler plugin path. Violates the interchangeability goal.

---

## Detailed Recommendation: Pattern 1

### Why Pattern 1 wins

| Criterion | Pattern 1 (init + helper) | Pattern 2 (direct return) |
|-----------|--------------------------|--------------------------|
| Works on all KMP targets | ✅ | ✅ |
| Preserves `navigationConfig<T>()` API | ✅ | ❌ |
| Zero code changes when switching backends | ⚠️ One init call | ❌ Different function |
| Compiler plugin compatibility | ✅ Harmless extra reference | N/A |
| Minimal changes to quo-vadis-core | ✅ None | ❌ New function |
| KSP generation changes | Small (one extra file) | Large (different approach) |
| Consumer understanding | Obvious — "init before use" | Confusing — "which function?" |

### Exact Code Changes

#### 1. KSP: `AggregatedConfigGenerator` — generate init helper

Add generation of a top-level function `initQuoVadisNavigation()` in the same file as the aggregated config:

```kotlin
// In AggregatedConfigGenerator.kt — add to buildAggregatedObject or as a new file

// Generated output becomes:
// ComposeApp__AggregatedConfig.kt
@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.generated

@GeneratedConfig
object ComposeApp__AggregatedConfig : NavigationConfig by (
    ComposeAppNavigationConfig + Feature1NavigationConfig
) {
    init {
        NavigationConfigRegistry.register(AppNavigation::class, this)
    }
}

/**
 * Initializes Quo Vadis navigation configuration.
 * Must be called once before [navigationConfig]`<AppNavigation>()` when using the KSP backend.
 * When using the compiler plugin backend, this is a harmless no-op.
 */
fun initQuoVadisNavigation() {
    ComposeApp__AggregatedConfig
}
```

Changes in `AggregatedConfigGenerator.kt`:

```kotlin
private fun buildFileSpec(...): FileSpec {
    return FileSpec.builder(packageName, aggregatedObjectName)
        .addFileComment("Auto-generated by Quo Vadis KSP processor.\nDo not modify manually.")
        .addAnnotation(optInAnnotation)
        .addType(buildAggregatedObject(rootClass, allConfigNames))
        .addFunction(buildInitFunction())  // ← ADD THIS
        .build()
}

private fun buildInitFunction(): FunSpec {
    return FunSpec.builder("initQuoVadisNavigation")
        .addKdoc(
            "Initializes the Quo Vadis navigation configuration.\n" +
            "Must be called once before navigationConfig<T>() when using the KSP backend.\n" +
            "Safe to call multiple times; idempotent via object initialization semantics."
        )
        .addStatement("%L", aggregatedObjectName)
        .build()
}
```

#### 2. quo-vadis-core: No changes needed

`NavigationConfigRegistry` and `navigationConfig<T>()` stay as-is. The registry lookup works once the init has been triggered.

#### 3. Consumer code change (one-time, template-provided)

```kotlin
import com.jermey.quo.vadis.generated.initQuoVadisNavigation

// At app startup, before any navigation:
initQuoVadisNavigation()
```

### Platform-Specific Gotchas

| Platform | Lazy Object Init | `init {}` Trigger | Notes |
|----------|-----------------|-------------------|-------|
| **JVM** | Yes (class loading) | Works when object is referenced | Standard JVM class initialization semantics |
| **Android** | Same as JVM | Works when object is referenced | No special handling needed |
| **iOS/Native** | Yes (Kotlin/Native uses lazy singletons) | Works when object is referenced | No tree shaking issues — the `initQuoVadisNavigation()` call is a direct reference |
| **JS** | Module-level code runs during module loading | Works — but JS bundlers (webpack) may tree-shake unreferenced objects | The `initQuoVadisNavigation()` function body references the object, preventing tree-shaking |
| **WasmJS** | Similar to JS | Same as JS | The function call creates a reference chain that prevents DCE |

**Critical for JS/WasmJS:** The init function's body `ComposeApp__AggregatedConfig` is a direct reference that prevents dead-code elimination. If it were only referenced via `init {}`, the bundler might eliminate the entire object. By having `initQuoVadisNavigation()` — which the consumer calls from their app code — we create an unbreakable reference chain:

```
App code → initQuoVadisNavigation() → ComposeApp__AggregatedConfig → init {} → NavigationConfigRegistry.register()
```

### Summary of Changes

| File | Change |
|------|--------|
| `quo-vadis-ksp/.../AggregatedConfigGenerator.kt` | Add `initQuoVadisNavigation()` function generation |
| Consumer `DI.kt` / startup code | Add `initQuoVadisNavigation()` call |
| Documentation | Document KSP init requirement |
| `quo-vadis-core` | **No changes** |

### Future Enhancement

If the `quo-vadis-gradle-plugin` gains an Application Plugin integration or Android Application hooks, the `initQuoVadisNavigation()` call could be auto-injected via a Gradle task or compiler plugin pass. But for now, a single function call is minimal, obvious, and works on all platforms.
