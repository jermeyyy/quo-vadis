# Implementation Plan: Multi-Module Definition Resolution Testing

## Overview

Restructure the demo app so destinations and composable screens are defined in **separate modules**, following the `-api` module pattern to expose public APIs and avoid cyclic dependencies. This validates that the Quo Vadis KSP processor correctly resolves `@Screen`, `@TabItem`, `@TabsContainer`, and cross-module destination references when annotations span multiple Gradle modules.

Additionally, one feature (ResultDemo in feature1) uses the **FeatureEntry pattern** — a `FeatureEntry` interface in `navigation-api` that exposes a `suspend fun start(): R?` function, with the implementation in feature1 internally using `navigator.navigateForResult(ResultDemoDestination.ItemPicker)` and returning the `SelectedItem` result to the caller. This hides concrete destination types from consuming modules and tests suspendable navigation-for-result across module boundaries.

## Requirements

- Destinations (`@Stack`, `@Destination`, `@TabItem`) live in `*-api` modules
- Screen composables (`@Screen`) and containers (`@TabsContainer`, `@PaneContainer`) live in implementation modules
- `-api` modules expose only destination types and `NavDestination` implementations
- No cyclic dependencies between feature modules
- Cross-module navigation via `-api` module imports
- `composeApp` is the only module that depends on all feature + feature-api modules and merges `NavigationConfig`s
- All existing demo functionality continues to work
- Feature1's ResultDemo entry point is hidden behind a `FeatureEntry` interface in `navigation-api`
- `FeatureEntry` implementation is provided via Koin DI — consumers inject the interface, never reference `ResultDemoDestination` directly

## Module Dependency Graph

```
navigation-api  (MainTabs @Tabs definition + FeatureEntry interface + ResultDemoFeatureEntry interface)
     ↑
     ├── feature1-api  (HomeTab, ExploreTab destinations + @TabItem)
     ├── feature2-api  (SettingsTab, AuthFlow destinations + @TabItem)
     ├── feature3-api  (ShowcaseTab destination + @TabItem)
     │
     ├── feature1  (depends on: feature1-api, feature2-api, navigation-api)
     │   ├── @Screen bindings for HomeTab, ExploreTab, ResultDemo
     │   └── ResultDemoFeatureEntryImpl (Koin-bound, injects Navigator, suspend start() returns SelectedItem?)
     │
     ├── feature2  (depends on: feature2-api, navigation-api)
     │   └── @Screen bindings for SettingsTab, AuthFlow
     │
     ├── feature3  (depends on: feature3-api, feature1-api, navigation-api)
     │   ├── @Screen bindings for ShowcaseTab
     │   └── Uses ResultDemoFeatureEntry via Koin (no dependency on feature1!)
     │
     └── composeApp  (depends on: ALL modules above)
         └── @TabsContainer(MainTabs), @PaneContainer, NavigationConfig merging, DI, DemoApp
```

**FeatureEntry dependency flow:**
```
navigation-api         defines: FeatureEntry, ResultDemoFeatureEntry
     ↑                          (interfaces only, no destination types)
     │
     ├── feature1        implements: ResultDemoFeatureEntryImpl
     │                   (injects Navigator, calls navigate(ResultDemoDestination.Demo))
     │                   (Koin: binds ResultDemoFeatureEntry → ResultDemoFeatureEntryImpl)
     │
     └── feature3        consumes: scope.launch { val result = koinInject<ResultDemoFeatureEntry>().start() }
                         (no dependency on feature1 — only navigation-api)
                         (suspend call returns SelectedItem? from ResultDemo's ItemPicker)
```

## KSP Resolution Scenarios Tested

| Scenario | Where Tested |
|----------|-------------|
| `@Screen` in module A binds to `@Destination` in module B's `-api` | feature1 → feature1-api |
| `@TabItem` in `-api` module pointing to `@Tabs` in `navigation-api` | feature1-api, feature2-api, feature3-api → navigation-api |
| `@TabsContainer` in `composeApp` wrapping tabs from 4+ modules | composeApp MainTabsContainer |
| Cross-module destination navigation (feature1 screen navigating to feature2-api dest) | feature1 HomeScreen → AuthFlowDestination |
| `NavigationConfig` merging across 6+ modules | composeApp DI.kt |
| `@Stack` with `startDestination` referencing inner sealed class dest | feature1-api ExploreTab |
| Standalone `@Destination` (no parent stack) across modules | feature3-api |
| FeatureEntry interface pattern — suspendable navigate-for-result without knowing destination type | feature3 → navigation-api → feature1 (via Koin DI) |

---

## Tasks

### Phase 1: Create `-api` Modules

#### Task 1.1: Create `feature1-api` module

**Description:** New module exposing HomeTab, ExploreTab, and ProfileTab destination definitions.

**Files to create:**
- `feature1-api/build.gradle.kts`
- `feature1-api/src/commonMain/kotlin/com/jermey/feature1/api/HomeTabDestination.kt`
- `feature1-api/src/commonMain/kotlin/com/jermey/feature1/api/ExploreTabDestination.kt`
- `feature1-api/src/commonMain/kotlin/com/jermey/feature1/api/ProfileTabDestination.kt`

**`feature1-api/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    useLocalKsp = true
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.feature1.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(compose.runtime)
                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
                implementation(projects.navigationApi)
            }
        }
    }
}
```

**`HomeTabDestination.kt`** — Move `HomeTab` from `composeApp/.../destinations/MainTabs.kt`:
```kotlin
package com.jermey.feature1.api

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class, ordinal = 0)
@Destination(route = "main/home")
@Transition(type = TransitionType.Fade)
data object HomeTab : NavDestination
```

**`ExploreTabDestination.kt`** — Move `ExploreTab` sealed class from `composeApp/.../destinations/MainTabs.kt`:
```kotlin
package com.jermey.feature1.api

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class, ordinal = 1)
@Stack(name = "exploreTabStack", startDestination = ExploreTab.Feed::class)
@Transition(type = TransitionType.Fade)
sealed class ExploreTab : NavDestination {
    @Destination(route = "explore/feed")
    @Transition(type = TransitionType.Fade)
    data object Feed : ExploreTab()

    @Destination(route = "explore/detail/{itemId}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class Detail(@Argument val itemId: String) : ExploreTab()

    @Destination(route = "explore/category/{category}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class CategoryView(@Argument val category: String) : ExploreTab()
}
```

**`ProfileTabDestination.kt`** — Move `ProfileTab` from `composeApp/.../destinations/MainTabs.kt`:
```kotlin
package com.jermey.feature1.api

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class, ordinal = 2)
@Destination(route = "main/profile")
@Transition(type = TransitionType.Fade)
data object ProfileTab : NavDestination
```

**Dependencies:** None  
**Acceptance Criteria:**
- [ ] Module compiles independently
- [ ] KSP generates `Feature1ApiNavigationConfig` with HomeTab, ExploreTab, ProfileTab registrations
- [ ] @TabItem references to MainTabs resolve correctly

---

#### Task 1.2: Create `feature2-api` module

**Description:** New module exposing SettingsTab and AuthFlow destination definitions.

**Files to create:**
- `feature2-api/build.gradle.kts`
- `feature2-api/src/commonMain/kotlin/com/jermey/feature2/api/SettingsTabDestination.kt`
- `feature2-api/src/commonMain/kotlin/com/jermey/feature2/api/AuthFlowDestination.kt`

**`feature2-api/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    useLocalKsp = true
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.feature2.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(compose.runtime)
                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
                implementation(projects.navigationApi)
            }
        }
    }
}
```

**`SettingsTabDestination.kt`** — Move `SettingsTab` from `composeApp/.../destinations/MainTabs.kt`:
```kotlin
package com.jermey.feature2.api

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class, ordinal = 3)
@Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
@Transition(type = TransitionType.Fade)
sealed class SettingsTab : NavDestination {
    @Destination(route = "settings/main")
    @Transition(type = TransitionType.Fade)
    data object Main : SettingsTab()

    @Destination(route = "settings/profile")
    @Transition(type = TransitionType.SlideHorizontal)
    data object Profile : SettingsTab()

    @Destination(route = "settings/notifications")
    @Transition(type = TransitionType.SlideHorizontal)
    data object Notifications : SettingsTab()

    @Destination(route = "settings/about")
    @Transition(type = TransitionType.SlideHorizontal)
    data object About : SettingsTab()
}
```

**`AuthFlowDestination.kt`** — Move from `feature2/src/.../AuthFlowDestination.kt`:
```kotlin
package com.jermey.feature2.api

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@Stack(name = "auth", startDestination = AuthFlowDestination.Login::class)
sealed class AuthFlowDestination : NavDestination {
    @Destination(route = "auth/login")
    data object Login : AuthFlowDestination()

    @Destination(route = "auth/register")
    data object Register : AuthFlowDestination()

    @Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlowDestination()
}
```

**Dependencies:** None  
**Acceptance Criteria:**
- [ ] Module compiles independently
- [ ] KSP generates `Feature2ApiNavigationConfig` with SettingsTab and AuthFlow registrations
- [ ] @TabItem for SettingsTab references MainTabs correctly

---

#### Task 1.3: Create `feature3-api` module

**Description:** New feature module API with a "Showcase" tab that demonstrates navigation patterns, providing a fresh cross-module tab test case.

**Files to create:**
- `feature3-api/build.gradle.kts`
- `feature3-api/src/commonMain/kotlin/com/jermey/feature3/api/ShowcaseTabDestination.kt`

**`feature3-api/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    useLocalKsp = true
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.feature3.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(compose.runtime)
                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
                implementation(projects.navigationApi)
            }
        }
    }
}
```

**`ShowcaseTabDestination.kt`:**
```kotlin
package com.jermey.feature3.api

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Showcase tab — demonstrates cross-module navigation patterns.
 *
 * Defined in feature3-api, screens implemented in feature3.
 * Ordinal = 5 (after ResultDemo's ordinal = 4 in feature1).
 */
@TabItem(parent = MainTabs::class, ordinal = 5)
@Stack(name = "showcaseTabStack", startDestination = ShowcaseTab.Overview::class)
@Transition(type = TransitionType.Fade)
sealed class ShowcaseTab : NavDestination {

    @Destination(route = "showcase/overview")
    @Transition(type = TransitionType.Fade)
    data object Overview : ShowcaseTab()

    @Destination(route = "showcase/detail/{itemId}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class Detail(@Argument val itemId: String) : ShowcaseTab()
}
```

**Dependencies:** None  
**Acceptance Criteria:**
- [ ] Module compiles independently
- [ ] KSP generates `Feature3ApiNavigationConfig`
- [ ] @TabItem ordinal = 5, pointing to MainTabs in navigation-api

---

### Phase 2: Create `feature3` Implementation Module

#### Task 2.1: Create `feature3` module with @Screen bindings

**Description:** New feature module providing screen composables for feature3-api destinations.

**Files to create:**
- `feature3/build.gradle.kts`
- `feature3/src/commonMain/kotlin/com/jermey/feature3/ShowcaseScreens.kt`

**`feature3/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    useLocalKsp = true
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.feature3"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)

                implementation(projects.quoVadisAnnotations)
                implementation(projects.quoVadisCore)
                implementation(projects.navigationApi)
                implementation(projects.feature3Api)

                // Cross-module navigation: can navigate to feature1-api destinations
                implementation(projects.feature1Api)
            }
        }
    }
}
```

**`ShowcaseScreens.kt`:**
```kotlin
package com.jermey.feature3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.feature1.api.ExploreTab
import com.jermey.feature3.api.ShowcaseTab
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Screen(ShowcaseTab.Overview::class)
@Composable
fun ShowcaseOverviewScreen(navigator: Navigator = koinInject()) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Showcase") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cross-Module Navigation Demo", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This tab is defined in feature3-api, screens in feature3.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navigator.navigate(ShowcaseTab.Detail("showcase_1")) }) {
                Text("Navigate to Detail (same module)")
            }
            // Cross-module navigation: navigate to feature1-api destination
            Button(onClick = { navigator.navigate(ExploreTab.Feed) }) {
                Text("Navigate to Explore Feed (feature1-api)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Screen(ShowcaseTab.Detail::class)
@Composable
fun ShowcaseDetailScreen(
    destination: ShowcaseTab.Detail,
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Showcase Detail") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Item: ${destination.itemId}", style = MaterialTheme.typography.headlineMedium)
            Text("Screen defined in feature3, destination in feature3-api")
        }
    }
}
```

**Dependencies:** Task 1.1, Task 1.3  
**Acceptance Criteria:**
- [ ] Module compiles with dependencies on feature3-api and feature1-api
- [ ] KSP generates `Feature3NavigationConfig` with screen registrations for ShowcaseTab.Overview and ShowcaseTab.Detail
- [ ] Cross-module navigation reference to ExploreTab.Feed compiles

---

### Phase 2B: Add FeatureEntry Pattern

#### Task 2.2: Define `FeatureEntry` and `ResultDemoFeatureEntry` interfaces in `navigation-api`

**Description:** Add a generic `FeatureEntry` interface and a feature-specific `ResultDemoFeatureEntry` sub-interface to `navigation-api`. This allows any module to start the ResultDemo feature without depending on feature1 or knowing about `ResultDemoDestination`.

**Files to create:**
- `navigation-api/src/commonMain/kotlin/com/jermey/navplayground/navigation/FeatureEntry.kt`
- `navigation-api/src/commonMain/kotlin/com/jermey/navplayground/navigation/SelectedItemResult.kt`
- `navigation-api/src/commonMain/kotlin/com/jermey/navplayground/navigation/ResultDemoFeatureEntry.kt`

**`FeatureEntry.kt`:**
```kotlin
package com.jermey.navplayground.navigation

/**
 * Base interface for feature entry points that return a typed result.
 *
 * Feature modules expose their entry point as a [FeatureEntry] implementation,
 * hiding concrete destination types from consuming modules. Consumers inject
 * the interface via DI and call [start] to navigate into the feature and
 * await the result.
 *
 * The [start] function is suspendable to support `navigateForResult` — the
 * coroutine suspends until the feature's result-returning destination calls
 * `navigateBackWithResult` or the user presses back (returning null).
 *
 * This enables true module encapsulation — the consuming module needs no
 * dependency on the feature's internal destinations or screens.
 *
 * @param R The result type returned by this feature entry. Use [Unit] for
 *          fire-and-forget navigation (no result expected).
 */
interface FeatureEntry<R : Any> {
    /**
     * Start navigating into this feature and await the result.
     *
     * Implementation handles all internal navigation using
     * `navigator.navigateForResult(...)`. The Navigator is injected
     * into the implementation, not passed by the caller.
     *
     * @return The result from the feature, or null if the user navigated back
     *         without providing a result.
     */
    suspend fun start(): R?
}
```

**`SelectedItemResult.kt`** — Result type shared via `navigation-api` so consumers can use the result without depending on feature1:
```kotlin
package com.jermey.navplayground.navigation

/**
 * Result returned by the Result Demo feature's item picker.
 *
 * Defined in `navigation-api` so consuming modules can use the result type
 * without depending on feature1.
 *
 * @property id Unique identifier of the selected item
 * @property name Display name of the selected item
 */
data class SelectedItemResult(
    val id: String,
    val name: String
)
```

**`ResultDemoFeatureEntry.kt`:**
```kotlin
package com.jermey.navplayground.navigation

/**
 * Entry point for the Result Demo feature (feature1).
 *
 * Consuming modules inject this interface via Koin and call [start]
 * to navigate into the Result Demo and await a [SelectedItemResult].
 * The consumer does not need to know about `ResultDemoDestination`
 * or depend on the feature1 module.
 *
 * ## Usage
 *
 * ```kotlin
 * // In any module that depends on navigation-api:
 * val resultDemoEntry: ResultDemoFeatureEntry = koinInject()
 * scope.launch {
 *     val result: SelectedItemResult? = resultDemoEntry.start()
 *     if (result != null) {
 *         // User picked an item
 *     }
 * }
 * ```
 */
interface ResultDemoFeatureEntry : FeatureEntry<SelectedItemResult>
```

**Dependencies:** None
**Acceptance Criteria:**
- [ ] `FeatureEntry<R>` interface with `suspend fun start(): R?` is in `navigation-api`
- [ ] `SelectedItemResult` data class is in `navigation-api`
- [ ] `ResultDemoFeatureEntry` extends `FeatureEntry<SelectedItemResult>`
- [ ] All are visible to modules that depend on `navigation-api`

---

#### Task 2.3: Implement `ResultDemoFeatureEntryImpl` in `feature1`

**Description:** Create the concrete implementation that injects `Navigator` via Koin and navigates to `ResultDemoDestination.Demo` on `start()`. Register the binding in feature1's Koin module.

**Files to create:**
- `feature1/src/commonMain/kotlin/com/jermey/feature1/ResultDemoFeatureEntryImpl.kt`

**`ResultDemoFeatureEntryImpl.kt`:**
```kotlin
package com.jermey.feature1

import com.jermey.feature1.resultdemo.ResultDemoDestination
import com.jermey.feature1.resultdemo.SelectedItem
import com.jermey.navplayground.navigation.ResultDemoFeatureEntry
import com.jermey.navplayground.navigation.SelectedItemResult
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.result.navigateForResult
import org.koin.core.annotation.Single

/**
 * Concrete implementation of [ResultDemoFeatureEntry].
 *
 * Hides [ResultDemoDestination] and [SelectedItem] from consuming modules.
 * Uses [navigateForResult] to navigate to the item picker and suspend
 * until the user selects an item or navigates back.
 *
 * Maps the internal [SelectedItem] result to the public [SelectedItemResult]
 * type defined in navigation-api.
 */
@Single(binds = [ResultDemoFeatureEntry::class])
class ResultDemoFeatureEntryImpl(
    private val navigator: Navigator
) : ResultDemoFeatureEntry {
    override suspend fun start(): SelectedItemResult? {
        val result: SelectedItem? = navigator.navigateForResult(
            ResultDemoDestination.ItemPicker
        )
        return result?.let { SelectedItemResult(id = it.id, name = it.name) }
    }
}
```

**Files to modify:**
- `feature1/src/commonMain/kotlin/com/jermey/feature1/resultdemo/Di.kt` — Ensure `@ComponentScan` covers the new class (it scans `com.jermey.feature1.resultdemo` currently; may need to widen to `com.jermey.feature1` or add a second scan)

**Option A:** Move `ResultDemoFeatureEntryImpl` into the `resultdemo` sub-package so existing `@ComponentScan` picks it up.

**Option B (recommended):** Update the `@ComponentScan` value in `Feature1Module` to scan the parent package:
```kotlin
@Module
@ComponentScan("com.jermey.feature1")
class Feature1Module
```

**Dependencies:** Task 2.2, Task 3.1
**Acceptance Criteria:**
- [ ] `ResultDemoFeatureEntryImpl` compiles in feature1
- [ ] Koin `@Single(binds = [ResultDemoFeatureEntry::class])` is picked up by component scan
- [ ] `suspend fun start()` calls `navigator.navigateForResult(ResultDemoDestination.ItemPicker)`
- [ ] Internal `SelectedItem` is mapped to public `SelectedItemResult`

---

#### Task 2.4: Use `ResultDemoFeatureEntry` in `feature3` (cross-module consumption)

**Description:** Update the ShowcaseOverviewScreen in feature3 to demonstrate navigating to the ResultDemo feature via `ResultDemoFeatureEntry` — without any dependency on feature1.

**File to modify:** `feature3/src/commonMain/kotlin/com/jermey/feature3/ShowcaseScreens.kt`

**Changes to `ShowcaseOverviewScreen`:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Screen(ShowcaseTab.Overview::class)
@Composable
fun ShowcaseOverviewScreen(navigator: Navigator = koinInject()) {
    // FeatureEntry — no dependency on feature1, only on navigation-api
    val resultDemoEntry: ResultDemoFeatureEntry = koinInject()
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<SelectedItemResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Showcase") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cross-Module Navigation Demo", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This tab is defined in feature3-api, screens in feature3.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navigator.navigate(ShowcaseTab.Detail("showcase_1")) }) {
                Text("Navigate to Detail (same module)")
            }
            // Cross-module navigation: navigate to feature1-api destination
            Button(onClick = { navigator.navigate(ExploreTab.Feed) }) {
                Text("Navigate to Explore Feed (feature1-api)")
            }
            // FeatureEntry pattern: suspend navigate-for-result, no feature1 dependency
            Button(onClick = {
                scope.launch {
                    val result = resultDemoEntry.start()
                    lastResult = result
                }
            }) {
                Text("Pick Item via Result Demo (FeatureEntry)")
            }
            // Show the result returned from the feature
            lastResult?.let { item ->
                Text(
                    "Last picked: ${item.name} (${item.id})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

**Note:** feature3's `build.gradle.kts` already depends on `navigation-api` (where `ResultDemoFeatureEntry` and `SelectedItemResult` live). No new dependency on feature1 is needed — that's the key benefit of the pattern.

**Dependencies:** Task 2.2, Task 2.1
**Acceptance Criteria:**
- [ ] feature3 compiles without any dependency on feature1
- [ ] `ResultDemoFeatureEntry` is injected via Koin at runtime
- [ ] Clicking "Pick Item via Result Demo" navigates to the ItemPicker screen
- [ ] After selecting an item, `SelectedItemResult` is returned and displayed in the Showcase screen
- [ ] Pressing back without picking returns null (no crash)

---

### Phase 3: Restructure Existing Feature Modules

#### Task 3.1: Update `feature1` to depend on `feature1-api`

**Description:** Update feature1's build.gradle.kts to depend on feature1-api instead of defining its own tab items. Add `feature2-api` dependency for cross-module navigation scenario.

**File:** `feature1/build.gradle.kts`

**Changes:**
- Add `implementation(projects.feature1Api)` to commonMain dependencies
- Add `implementation(projects.feature2Api)` to commonMain dependencies (for cross-module navigation from ResultDemo to AuthFlow)
- feature1 already has `@TabItem(parent = MainTabs::class, ordinal = 4)` on ResultDemoDestination — this stays as-is

**Acceptance Criteria:**
- [ ] feature1 compiles with new dependencies
- [ ] ResultDemoDestination still has ordinal=4 @TabItem

---

#### Task 3.2: Update `feature2` to depend on `feature2-api`

**Description:** Update feature2 to depend on feature2-api. Move `AuthFlowDestination` definition to feature2-api (it was defined in feature2's root package). Update screen imports.

**File:** `feature2/build.gradle.kts`

**Changes:**
- Add `implementation(projects.feature2Api)` to commonMain dependencies

**Files to modify:**
- `feature2/src/commonMain/kotlin/com/jermey/feature2/AuthFlowDestination.kt` — **DELETE** (moved to feature2-api)
- `feature2/src/commonMain/kotlin/com/jermey/feature2/auth/AuthLoginScreen.kt` — Update import from `com.jermey.feature2.AuthFlowDestination` → `com.jermey.feature2.api.AuthFlowDestination`
- `feature2/src/commonMain/kotlin/com/jermey/feature2/auth/AuthRegisterScreen.kt` — Same import update
- `feature2/src/commonMain/kotlin/com/jermey/feature2/auth/AuthForgotPasswordScreen.kt` — Same import update

**Acceptance Criteria:**
- [ ] feature2 compiles with feature2-api dependency
- [ ] All auth screens reference AuthFlowDestination from feature2-api
- [ ] Old AuthFlowDestination.kt in feature2 is deleted

---

#### Task 3.3: Move HomeTab, ExploreTab, ProfileTab screen bindings from `composeApp` to `feature1`

**Description:** Move @Screen composables for HomeTab, ExploreTab, and ProfileTab from composeApp to feature1. These screens currently bind to destinations that are moving to feature1-api.

**Files to move to `feature1`:**
- `composeApp/.../ui/screens/HomeScreen.kt` → `feature1/src/commonMain/kotlin/com/jermey/feature1/home/HomeScreen.kt`
- `composeApp/.../ui/screens/ExploreScreen.kt` → `feature1/src/commonMain/kotlin/com/jermey/feature1/explore/ExploreScreen.kt`
- `composeApp/.../ui/screens/explore/ExploreDetailScreen.kt` → `feature1/src/commonMain/kotlin/com/jermey/feature1/explore/ExploreDetailScreen.kt`
- `composeApp/.../ui/screens/explore/CategoryViewScreen.kt` → `feature1/src/commonMain/kotlin/com/jermey/feature1/explore/CategoryViewScreen.kt`
- `composeApp/.../ui/screens/profile/ProfileScreen.kt` → `feature1/src/commonMain/kotlin/com/jermey/feature1/profile/ProfileScreen.kt`

**Import updates required:**
- All moved screens: Update destination imports from `com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.HomeTab` → `com.jermey.feature1.api.HomeTab`
- Same for ExploreTab → `com.jermey.feature1.api.ExploreTab`
- Same for ProfileTab → `com.jermey.feature1.api.ProfileTab`

**feature1 dependencies to add:**
- Haze libraries (for HomeScreen glassmorphism): `implementation(libs.haze)`, `implementation(libs.haze.materials)`
- Coil (if ExploreScreen uses image loading): `implementation(libs.coil.compose)`
- Any MVI containers used by these screens need to move to feature1 too, or stay in composeApp if shared

**Note:** MVI containers (`ExploreContainer`, `ExploreDetailContainer`, `ProfileContainer`) that are currently in composeApp should also move to feature1 if they only serve feature1 screens. Evaluate each container's dependencies.

**Dependencies:** Task 1.1  
**Acceptance Criteria:**
- [ ] HomeScreen, ExploreScreen, ProfileScreen compile in feature1
- [ ] @Screen annotations reference feature1-api destination types
- [ ] No duplicate @Screen registrations across modules

---

#### Task 3.4: Move SettingsTab screen bindings from `composeApp` to `feature2`

**Description:** Move @Screen composables for SettingsTab from composeApp to feature2.

**Files to move to `feature2`:**
- `composeApp/.../ui/screens/SettingsScreen.kt` → `feature2/src/commonMain/kotlin/com/jermey/feature2/settings/SettingsScreen.kt`
- `composeApp/.../ui/screens/SettingsDetailScreens.kt` → `feature2/src/commonMain/kotlin/com/jermey/feature2/settings/SettingsDetailScreens.kt`

**Import updates required:**
- Update SettingsTab imports from `com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.SettingsTab` → `com.jermey.feature2.api.SettingsTab`
- Update NavigationMenuDestination import (SettingsScreen navigates to it — this destination stays in composeApp, so feature2 needs access to it via navigation-api OR composeApp keeps the @Screen for NavigationMenuDestination)

**Decision:** `NavigationMenuDestination` should remain in composeApp since it's a modal overlay specific to the demo app chrome. SettingsScreen's navigation to it can be done via Navigator without type-safe destination reference (use a callback / event-based pattern), or `NavigationMenuDestination` can be moved to `navigation-api` as a shared modal.

**Recommended approach:** Move `NavigationMenuDestination` to `navigation-api` since it's used from multiple screens.

**Dependencies:** Task 1.2  
**Acceptance Criteria:**
- [ ] SettingsScreen and SettingsDetailScreens compile in feature2
- [ ] @Screen annotations reference feature2-api SettingsTab types
- [ ] Settings → NavigationMenu navigation still works

---

### Phase 4: Update `composeApp` and Wiring

#### Task 4.1: Remove moved destinations from `composeApp`

**Description:** Delete destination definitions that have been moved to `-api` modules. The file `composeApp/.../destinations/MainTabs.kt` currently defines HomeTab, ExploreTab, ProfileTab, and SettingsTab — all moved to feature1-api and feature2-api.

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/app/sample/showcase/destinations/veeeeery/looong/packages/names/length/test/destinations/MainTabs.kt`

**Action:** Delete `HomeTab`, `ExploreTab`, `ProfileTab`, `SettingsTab` from this file. The file may be deleted entirely if no other destinations remain in it.

**Files to update imports in (remaining composeApp screens that reference these types):**
- `composeApp/.../tabs/MainTabsUI.kt` — Update imports to use `com.jermey.feature1.api.HomeTab`, `com.jermey.feature1.api.ExploreTab`, `com.jermey.feature1.api.ProfileTab`, `com.jermey.feature2.api.SettingsTab`
- Any remaining composeApp screens that reference these destination types

**Dependencies:** Tasks 1.1, 1.2, 3.3, 3.4  
**Acceptance Criteria:**
- [ ] No duplicate destination definitions across modules
- [ ] composeApp compiles without old destination types
- [ ] MainTabsContainer imports from feature1-api and feature2-api

---

#### Task 4.2: Update `composeApp` build.gradle.kts dependencies

**Description:** Add dependencies on all new `-api` and feature modules.

**File:** `composeApp/build.gradle.kts`

**Changes to commonMain dependencies:**
```kotlin
// Existing
implementation(projects.feature1)
implementation(projects.feature2)
implementation(projects.navigationApi)

// Add new
implementation(projects.feature1Api)
implementation(projects.feature2Api)
implementation(projects.feature3Api)
implementation(projects.feature3)
```

**Acceptance Criteria:**
- [ ] composeApp depends on all 7 navigation-relevant modules

---

#### Task 4.3: Update NavigationConfig merging in `composeApp` DI

**Description:** Update `DI.kt` to merge configs from all modules including the new `-api` and feature modules.

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DI.kt`

**Changes:**
```kotlin
import com.jermey.quo.vadis.generated.ComposeAppNavigationConfig
import com.jermey.quo.vadis.generated.Feature1NavigationConfig
import com.jermey.quo.vadis.generated.Feature1ApiNavigationConfig
import com.jermey.quo.vadis.generated.Feature2NavigationConfig
import com.jermey.quo.vadis.generated.Feature2ApiNavigationConfig
import com.jermey.quo.vadis.generated.Feature3NavigationConfig
import com.jermey.quo.vadis.generated.Feature3ApiNavigationConfig

@Single
fun navigationConfig(): NavigationConfig =
    ComposeAppNavigationConfig +
        Feature1ApiNavigationConfig +
        Feature1NavigationConfig +
        Feature2ApiNavigationConfig +
        Feature2NavigationConfig +
        Feature3ApiNavigationConfig +
        Feature3NavigationConfig
```

**Note:** Order matters — `-api` configs provide container/stack definitions, impl configs provide screen registrations. `-api` should come before impl in the chain.

**Dependencies:** Tasks 1.1–2.1, 4.2  
**Acceptance Criteria:**
- [ ] All 7 NavigationConfigs are merged
- [ ] Navigator initializes with full tab structure (6 tabs: Home, Explore, Profile, Settings, ResultDemo, Showcase)

---

#### Task 4.4: Update `MainTabsContainer` in composeApp for new tabs

**Description:** Update the MainTabsContainer (bottom navigation) to handle the new Showcase tab from feature3-api. Also update imports for moved destinations.

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabsUI.kt`

**Changes:**
- Update imports for HomeTab, ExploreTab, ProfileTab → feature1-api
- Update imports for SettingsTab → feature2-api
- Add import for ShowcaseTab → feature3-api
- Add case in `when (tab)` block for ShowcaseTab → "Showcase" with appropriate icon

**Dependencies:** Tasks 1.1, 1.2, 1.3  
**Acceptance Criteria:**
- [ ] Bottom navigation shows all 6 tabs
- [ ] Correct icons and labels for each tab
- [ ] Tab switching works across all tabs

---

#### Task 4.5: Register new modules in `settings.gradle.kts`

**Description:** Add new modules to the project settings.

**File:** `settings.gradle.kts`

**Changes — append:**
```kotlin
include(":feature1-api")
include(":feature2-api")
include(":feature3-api")
include(":feature3")
```

**Dependencies:** None (can be done first)  
**Acceptance Criteria:**
- [ ] Gradle sync succeeds with all new modules

---

### Phase 5: Cleanup and Validation

#### Task 5.1: Remove moved screen files from `composeApp`

**Description:** Delete screen files that have been moved to feature modules to avoid duplicate @Screen registrations.

**Files to delete from composeApp:**
- `HomeScreen.kt` (moved to feature1)
- `ExploreScreen.kt` (moved to feature1)
- `explore/ExploreDetailScreen.kt` (moved to feature1)
- `explore/CategoryViewScreen.kt` (moved to feature1)
- `profile/ProfileScreen.kt` (moved to feature1)
- `SettingsScreen.kt` (moved to feature2)
- `SettingsDetailScreens.kt` (moved to feature2)

**Also evaluate moving associated containers:**
- `explore/ExploreContainer.kt`, `explore/ExploreDetailContainer.kt` → feature1
- `profile/ProfileContainer.kt` → feature1

**Dependencies:** Tasks 3.3, 3.4  
**Acceptance Criteria:**
- [ ] No duplicate @Screen annotations for same destinations
- [ ] composeApp builds successfully with reduced screen set
- [ ] feature1 and feature2 build with their new screens

---

#### Task 5.2: Fix remaining import references across `composeApp`

**Description:** Search and fix all remaining imports in composeApp that reference the old long package path for moved destinations.

**Search pattern:** `com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.HomeTab`

**Replace with:** `com.jermey.feature1.api.HomeTab` (and similar for other moved types)

**Files likely affected:**
- Any remaining component in composeApp that references HomeTab, ExploreTab, ProfileTab, SettingsTab, or AuthFlowDestination
- `NavigationBottomSheetContent` or similar UI components

**Dependencies:** Tasks 4.1  
**Acceptance Criteria:**
- [ ] No compilation errors from stale imports
- [ ] Full grep for old package paths returns no results

---

#### Task 5.3: Verify full build and navigation functionality

**Description:** Build the entire project and verify all navigation scenarios work.

**Verification steps:**
1. `./gradlew build` — Full build succeeds
2. `./gradlew :feature1-api:kspCommonMainKotlinMetadata` — KSP generates Feature1ApiNavigationConfig
3. `./gradlew :feature2-api:kspCommonMainKotlinMetadata` — KSP generates Feature2ApiNavigationConfig
4. `./gradlew :feature3-api:kspCommonMainKotlinMetadata` — KSP generates Feature3ApiNavigationConfig
5. `./gradlew :feature3:kspCommonMainKotlinMetadata` — KSP generates Feature3NavigationConfig
6. `./gradlew :composeApp:kspCommonMainKotlinMetadata` — KSP generates ComposeAppNavigationConfig
7. Verify generated configs:
   - Feature1ApiNavigationConfig contains HomeTab, ExploreTab, ProfileTab container/stack registrations
   - Feature2ApiNavigationConfig contains SettingsTab, AuthFlow container/stack registrations
   - Feature3ApiNavigationConfig contains ShowcaseTab container/stack registrations
   - Feature1NavigationConfig contains screen registrations for HomeTab, ExploreTab, ResultDemo
   - Feature2NavigationConfig contains screen registrations for SettingsTab, AuthFlow
   - Feature3NavigationConfig contains screen registrations for ShowcaseTab
   - ComposeAppNavigationConfig contains @TabsContainer, @PaneContainer, remaining screen registrations
8. Run demo app — all 6 tabs visible, navigation within and across tabs works
9. Verify FeatureEntry pattern:
   - From Showcase tab, tap "Open Result Demo (FeatureEntry)"
   - Confirm ItemPicker screen appears (not ResultDemo.Demo — the entry navigates directly to the picker)
   - Select an item → confirm `SelectedItemResult` is returned and displayed in Showcase
   - Press back without selecting → confirm null is returned (no crash)
   - Confirm feature3 has NO dependency on feature1 in its `build.gradle.kts`
   - Confirm `ResultDemoFeatureEntry` binding resolves via Koin

**Dependencies:** All previous tasks  
**Acceptance Criteria:**
- [ ] Full build passes
- [ ] All KSP-generated configs contain expected registrations
- [ ] Demo app runs with all tabs and screens functional
- [ ] Cross-module navigation (e.g., Showcase → Explore Feed) works
- [ ] Back navigation works correctly across module boundaries
- [ ] FeatureEntry suspend pattern works: Showcase → ItemPicker → result returned to Showcase (no feature1 dependency in feature3)

---

## Sequencing

```
Phase 1 (parallel):
  Task 1.1 ─┐
  Task 1.2 ─┼── All -api modules (independent)
  Task 1.3 ─┘
  Task 4.5 ─── settings.gradle.kts (independent)

Phase 2 (depends on Phase 1):
  Task 2.1 ─── feature3 impl (depends on 1.1, 1.3)
  Task 2.2 ─── FeatureEntry interfaces in navigation-api (independent)
  Task 2.3 ─── ResultDemoFeatureEntryImpl in feature1 (depends on 2.2, 3.1)
  Task 2.4 ─── Use FeatureEntry in feature3 (depends on 2.2, 2.1)

Phase 3 (depends on Phase 1):
  Task 3.1 ─── feature1 restructure (depends on 1.1)
  Task 3.2 ─── feature2 restructure (depends on 1.2)
  Task 3.3 ─── Move screens to feature1 (depends on 1.1, 3.1)
  Task 3.4 ─── Move screens to feature2 (depends on 1.2, 3.2)

Phase 4 (depends on Phase 2, 3):
  Task 4.1 ─── Remove old destinations (depends on 3.3, 3.4)
  Task 4.2 ─── Update composeApp deps (depends on Phase 1, 2)
  Task 4.3 ─── Update config merging (depends on 4.2)
  Task 4.4 ─── Update MainTabsContainer (depends on 4.1)

Phase 5 (depends on Phase 4):
  Task 5.1 ─── Remove duplicate screens (depends on 3.3, 3.4)
  Task 5.2 ─── Fix imports (depends on 4.1)
  Task 5.3 ─── Full build verification (depends on all)
           ─── Includes FeatureEntry runtime verification
```

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| KSP ordinal validation fails for cross-module @TabItem | Build failure | Cross-module @Tabs skip ordinal continuity validation (existing behavior documented in copilot-instructions.md) |
| Duplicate @Screen registration for same destination across modules | Runtime crash | Task 5.1 explicitly removes moved screen files from composeApp |
| NavigationConfig merge order causes wrong screen/container resolution | Incorrect rendering | Secondary (right-hand) config takes priority; put -api before impl in merge chain |
| MVI containers have dependencies on composeApp-specific code | Compilation failure | Evaluate each container before moving; keep shared containers in composeApp if they reference app-specific types |
| iOS framework names conflict with new modules | iOS build failure | Ensure unique `baseName` for each iOS framework binary |
| Koin fails to resolve `ResultDemoFeatureEntry` at runtime | Crash on injection | Ensure feature1's Koin module is loaded in composeApp's `startKoin`; verify `@ComponentScan` covers `ResultDemoFeatureEntryImpl` |
| FeatureEntry `start()` called before Navigator is ready | Navigation ignored | Navigator is a Koin `@Single` — always available after DI initialization |

## Open Questions

1. **`NavigationMenuDestination` placement:** Should it move to `navigation-api` (recommended) or stay in composeApp? If it stays, screens in feature modules cannot navigate to it type-safely.
2. **MVI container placement:** Should MVI containers follow their screens to feature modules, or remain centralized? Containers with Koin DI wiring may need their `@Module`/`@ComponentScan` declarations updated.
3. **Shared UI components:** Components like `GlassBottomSheet`, `SettingItem`, theme utilities are in composeApp. If feature modules need them, consider extracting a `ui-common` module or keeping them in composeApp and passing them via composition locals.
4. **FeatureEntry parameterization:** Should `FeatureEntry<R>.start()` support typed input parameters? Current plan uses a no-arg `start()` — the feature-specific interface could add overloads like `suspend fun start(filter: String): R?` if needed. Alternatively, a second type parameter `FeatureEntry<I, R>` could be introduced.
5. **Additional FeatureEntry candidates:** Should other features (e.g., AuthFlow in feature2) also adopt the FeatureEntry pattern, or is one example sufficient for testing?
6. **SelectedItem vs SelectedItemResult:** The internal `SelectedItem` in feature1 is mapped to `SelectedItemResult` in navigation-api. Consider whether feature1's `SelectedItem` should be replaced entirely by `SelectedItemResult`, or keep both to demonstrate the mapping pattern.
