# MIG-007A: Foundation - Core Destination Classes

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007A |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 hours |
| **Dependencies** | Phase 1-4 complete, MIG-001 through MIG-006 recipes |
| **Output** | Migrated destination files in `composeApp/` |

## Objective

Migrate all **foundation destination classes** from legacy annotations (`@Graph`, `@Route`, `TypedDestination<T>`) to the new NavNode architecture annotations (`@Stack`, `@Destination`, `@Argument`).

This is the **first subtask** and must be completed before all other MIG-007 subtasks, as all screen bindings and navigation setups depend on correctly defined destinations.

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── destinations/
│   ├── Destinations.kt          # Core app destinations (AppDestination, TabDestination)
│   ├── SettingsDestination.kt   # Settings sub-screens
│   └── StateDrivenDestinations.kt # State-driven demo (raw Destination)
```

### Reference Code (Main Branch)

| File | GitHub Permalink |
|------|------------------|
| Destinations.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/Destinations.kt |
| SettingsDestination.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/SettingsDestination.kt |

### Reference Recipes

| Recipe | Pattern |
|--------|---------|
| [MIG-001](./MIG-001-simple-stack-example.md) | `@Stack` + `@Destination` basic pattern |
| [MIG-002](./MIG-002-master-detail-example.md) | `@Argument` for parameterized destinations |

---

## Migration Steps

### Step 1: Update Destinations.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/Destinations.kt`

#### 1.1 AppDestination (Root App Container)

```kotlin
// OLD
@Graph(name = "app", startDestination = "app/main_tabs")
sealed class AppDestination : Destination {
    @Route("app/main_tabs")
    data object MainTabs : AppDestination()
}

// NEW
@Stack(name = "app", startDestination = "MainTabs")
sealed class AppDestination : DestinationInterface {
    @Destination(route = "app/main_tabs")
    data object MainTabs : AppDestination()
}
```

**Key Changes:**
- `@Graph` → `@Stack`
- `startDestination` uses class name `"MainTabs"`, NOT route `"app/main_tabs"`
- `@Route` → `@Destination`
- `Destination` → `DestinationInterface` (new base interface)

#### 1.2 TabDestination (Tab Root Destinations)

```kotlin
// OLD
sealed class TabDestination : Destination {
    @Route("tab/home")
    data object Home : TabDestination()
    
    @Route("tab/explore")
    data object Explore : TabDestination()
    
    @Route("tab/profile")
    data object Profile : TabDestination()
    
    @Route("tab/settings")
    data object Settings : TabDestination()
}

// NEW - Per-Tab Stacks
@Stack(name = "homeStack", startDestination = "Home")
sealed class HomeDestination : DestinationInterface {
    @Destination(route = "tab/home")
    data object Home : HomeDestination()
}

@Stack(name = "exploreStack", startDestination = "Explore")
sealed class ExploreDestination : DestinationInterface {
    @Destination(route = "tab/explore")
    data object Explore : ExploreDestination()
}

@Stack(name = "profileStack", startDestination = "Profile")
sealed class ProfileDestination : DestinationInterface {
    @Destination(route = "tab/profile")
    data object Profile : ProfileDestination()
}

@Stack(name = "settingsStack", startDestination = "Main")
sealed class SettingsStackDestination : DestinationInterface {
    @Destination(route = "tab/settings")
    data object Main : SettingsStackDestination()
    // Note: Settings sub-screens move to SettingsDestination.kt or here
}
```

**Key Changes:**
- Each tab root becomes its own `@Stack`
- `startDestination` uses class name
- Each tab can grow into a full navigation stack

#### 1.3 MasterDetailDestination

```kotlin
// OLD
@Graph(name = "masterDetail", startDestination = "masterDetail/list")
sealed class MasterDetailDestination : Destination {
    @Route("masterDetail/list")
    data object List : MasterDetailDestination()
    
    @Route("masterDetail/detail")
    @Argument(ItemDetail::class)
    data class Detail(val item: ItemDetail) : MasterDetailDestination(), TypedDestination<ItemDetail> {
        override val data: ItemDetail get() = item
    }
}

@Serializable
data class ItemDetail(val id: String, val title: String, val description: String)

// NEW
@Stack(name = "masterDetail", startDestination = "List")
sealed class MasterDetailDestination : DestinationInterface {
    @Destination(route = "masterDetail/list")
    data object List : MasterDetailDestination()
    
    @Destination(route = "masterDetail/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String,
        // Optional: Additional data passed directly
        val title: String = "",
        val description: String = ""
    ) : MasterDetailDestination()
}
```

**Key Changes:**
- No more `@Argument(Class::class)` annotation → use `@Argument` on properties
- No more `TypedDestination<T>` interface
- No more separate `@Serializable` data class
- Route template includes `{itemId}` for deep linking
- Non-route properties can still be passed (but won't be in deep links)

#### 1.4 ProcessDestination (Wizard Flow)

```kotlin
// OLD
@Graph(name = "process", startDestination = "process/start")
sealed class ProcessDestination : Destination {
    @Route("process/start")
    data object Start : ProcessDestination()
    
    @Route("process/step1")
    @Argument(ProcessStep1Data::class)
    data class Step1(val data: ProcessStep1Data) : ProcessDestination(), TypedDestination<ProcessStep1Data>
    
    // ... similar for Step2A, Step2B, Step3, Complete
}

// NEW
@Stack(name = "process", startDestination = "Start")
sealed class ProcessDestination : DestinationInterface {
    @Destination(route = "process/start")
    data object Start : ProcessDestination()
    
    @Destination(route = "process/step1/{accountType}")
    data class Step1(
        @Argument val accountType: String
    ) : ProcessDestination()
    
    @Destination(route = "process/step2a/{accountType}/{userName}")
    data class Step2A(
        @Argument val accountType: String,
        @Argument val userName: String
    ) : ProcessDestination()
    
    @Destination(route = "process/step2b/{accountType}/{companyName}")
    data class Step2B(
        @Argument val accountType: String,
        @Argument val companyName: String
    ) : ProcessDestination()
    
    @Destination(route = "process/step3/{accountType}")
    data class Step3(
        @Argument val accountType: String,
        val userName: String = "",
        val companyName: String = ""
    ) : ProcessDestination()
    
    @Destination(route = "process/complete")
    data object Complete : ProcessDestination()
}
```

#### 1.5 DeepLinkDestination

```kotlin
// OLD
sealed class DeepLinkDestination : Destination {
    @Route("deeplink/demo")
    data object Demo : DeepLinkDestination()
}

// NEW
@Stack(name = "deepLink", startDestination = "Demo")
sealed class DeepLinkDestination : DestinationInterface {
    @Destination(route = "deeplink/demo")
    data object Demo : DeepLinkDestination()
}
```

---

### Step 2: Update SettingsDestination.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/SettingsDestination.kt`

```kotlin
// OLD
@Graph(name = "settings", startDestination = "settings/main")
sealed class SettingsDestination : Destination {
    @Route("settings/main")
    data object Main : SettingsDestination()
    
    @Route("settings/profile")
    data object Profile : SettingsDestination()
    
    @Route("settings/notifications")
    data object Notifications : SettingsDestination()
    
    @Route("settings/about")
    data object About : SettingsDestination()
}

// NEW
@Stack(name = "settings", startDestination = "Main")
sealed class SettingsDestination : DestinationInterface {
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()
    
    @Destination(route = "settings/profile")
    data object Profile : SettingsDestination()
    
    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()
    
    @Destination(route = "settings/about")
    data object About : SettingsDestination()
}
```

---

### Step 3: Update StateDrivenDestinations.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/StateDrivenDestinations.kt`

This file may have raw `Destination` classes without annotations. Update to use new architecture:

```kotlin
// OLD (if using raw Destination)
sealed class StateDrivenDemoDestination : Destination {
    data object Demo : StateDrivenDemoDestination()
}

// NEW
@Stack(name = "stateDriven", startDestination = "Demo")
sealed class StateDrivenDemoDestination : DestinationInterface {
    @Destination(route = "statedriven/demo")
    data object Demo : StateDrivenDemoDestination()
    
    // Content screens can also be destinations
    @Destination(route = "statedriven/screen/{screenId}")
    data class Screen(
        @Argument val screenId: Int
    ) : StateDrivenDemoDestination()
}
```

---

### Step 4: Update TabsDestination (Nested Tabs Demo)

If there's a separate `TabsDestination` for the nested tabs demo:

```kotlin
// NEW
@Stack(name = "tabsDemo", startDestination = "Main")
sealed class TabsDestination : DestinationInterface {
    @Destination(route = "tabsdemo/main")
    data object Main : TabsDestination()
    
    @Destination(route = "tabsdemo/subitem/{itemId}")
    data class SubItem(
        @Argument val itemId: String
    ) : TabsDestination()
}
```

---

## Checklist

- [ ] Convert `AppDestination` from `@Graph` to `@Stack`
- [ ] Split `TabDestination` into per-tab stack classes (`HomeDestination`, `ExploreDestination`, etc.)
- [ ] Convert `MasterDetailDestination` with `@Argument` on properties
- [ ] Convert `ProcessDestination` with route templates
- [ ] Convert `SettingsDestination` from `@Graph` to `@Stack`
- [ ] Convert `DeepLinkDestination` to `@Stack`
- [ ] Convert `StateDrivenDemoDestination` to `@Stack`
- [ ] Convert `TabsDestination` if present
- [ ] Remove all `TypedDestination<T>` interfaces
- [ ] Remove all separate `@Serializable` data classes for arguments
- [ ] Ensure all `startDestination` use class names, not routes
- [ ] Verify all routes are unique across all stacks
- [ ] Run `./gradlew :composeApp:compileKotlinMetadata` to verify compilation

---

## Verification

```bash
# Verify compilation after destination changes
./gradlew :composeApp:compileKotlinMetadata

# Check for any remaining legacy annotations
grep -r "@Graph" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/
grep -r "@Route" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/
grep -r "TypedDestination" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/

# Should return empty results after migration
```

---

## Downstream Dependencies

After MIG-007A completion, the following subtasks can proceed:

| Subtask | Dependency |
|---------|------------|
| MIG-007B (Tab System) | Uses `HomeDestination`, `ExploreDestination`, etc. |
| MIG-007C (Master-Detail) | Uses `MasterDetailDestination` |
| MIG-007D (Process/Wizard) | Uses `ProcessDestination` |
| MIG-007E (Settings Stack) | Uses `SettingsDestination` |
| MIG-007F (Feature Screens) | Uses all destinations for `@Screen` bindings |

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-001: Simple Stack Recipe](./MIG-001-simple-stack-example.md) (`@Stack` + `@Destination` pattern)
- [MIG-002: Master-Detail Recipe](./MIG-002-master-detail-example.md) (`@Argument` pattern)
- [MIG-009: Type-Safe Arguments Recipe](./MIG-009-type-safe-arguments-recipe.md) (`@Argument` detailed patterns)
- [PREP-002: Deprecated Annotations](./PREP-002-deprecated-annotations.md) (Legacy API reference)
