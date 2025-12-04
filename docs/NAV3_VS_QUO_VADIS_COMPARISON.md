# Navigation 3 vs Quo Vadis: Comprehensive Comparison

> **Generated:** November 30, 2025  
> **Purpose:** Detailed comparison of Jetpack Navigation 3 (Nav3) and Quo Vadis navigation libraries

---

## Executive Summary

| Aspect | Navigation 3 (Nav3) | Quo Vadis |
|--------|---------------------|-----------|
| **Developer** | Google (Jetpack) | Community/Custom |
| **Release Status** | Stable 1.0.0 (Nov 2025) | Production Ready |
| **Primary Target** | Jetpack Compose (Android first) | Kotlin/Compose Multiplatform |
| **Dependencies** | Part of AndroidX ecosystem | Zero external navigation deps |
| **Type Safety** | kotlinx.serialization | kotlinx.serialization + KSP |
| **Code Generation** | None | KSP-based |

---

## 1. Overview

### Navigation 3 (Nav3)

**Navigation 3** is Google's new navigation library specifically designed for Jetpack Compose. Released as stable in November 2025, it represents a fundamental redesign of Android navigation with a Compose-first approach.

**Key Philosophy:**
- Developer controls state; library displays it
- Declarative, state-driven navigation
- Clean separation between state management and UI rendering
- Built for modern Compose patterns

### Quo Vadis

**Quo Vadis** (Latin: "Where are you going?") is a comprehensive, type-safe navigation library for **Kotlin Multiplatform** and **Compose Multiplatform**. It provides a modern, annotation-driven approach with zero external navigation library dependencies.

**Key Philosophy:**
- True multiplatform from the ground up
- Annotation-driven code generation for reduced boilerplate
- Direct backstack control for maximum flexibility
- First-class MVI architecture support

---

## 2. Architecture Comparison

### Nav3 Architecture

```
Developer-Owned State (SnapshotStateList<NavKey>)
              ↓
       NavDisplay (Library UI)
              ↓
    SceneStrategy (Layout Logic)
              ↓
       Scene Rendering
              ↓
    NavEntryDecorators (Lifecycle, SavedState)
```

**Core Concepts:**
- **NavKey** - Serializable type representing a destination
- **NavBackStack** - Developer-managed `SnapshotStateList<T>` where T is NavKey
- **NavDisplay** - Composable that renders content based on backstack
- **entryProvider** - DSL for mapping NavKeys to Composable content
- **SceneStrategy** - Determines how entries are displayed (single pane, dialog, etc.)
- **NavEntryDecorator** - Adds capabilities like lifecycle, saved state

### Quo Vadis Architecture

```
Application Layer (Screens, ViewModels, Feature Modules)
              ↓
Code Generation Layer (KSP - @Graph, @Route, @Argument, @Content)
              ↓
Integration Layer (MVI Support, DI Integration, Testing)
              ↓
Compose Layer (NavHost, GraphNavHost, Animations/Transitions)
              ↓
Core Layer (Navigator, BackStack, Destination, NavigationGraph, DeepLink)
```

**Core Concepts:**
- **Destination** - Interface representing navigation targets
- **Navigator** - Central controller for all navigation operations
- **BackStack** - StateFlow-based reactive stack with direct manipulation
- **NavigationGraph** - Modular graph with DSL builder
- **TypedDestination<T>** - Type-safe destinations with serializable data

---

## 3. Feature-by-Feature Comparison

### 3.1 Type Safety

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| Route Definition | Serializable objects/data classes | Sealed classes + annotations |
| Compile-time Safety | ✅ Full | ✅ Full |
| Arguments | Via serializable properties | `@Argument` + `TypedDestination<T>` |
| Route Resolution | Direct type matching | RouteRegistry (KSP generated) |

**Nav3 Example:**
```kotlin
@Serializable
object Home : NavKey

@Serializable
data class Profile(val id: String) : NavKey

val backStack = rememberNavBackStack(Home)
backStack.add(Profile("123"))
```

**Quo Vadis Example:**
```kotlin
@Graph("app")
sealed class AppDestination : Destination {
    @Route("app/home")
    data object Home : AppDestination()
    
    @Route("app/profile")
    @Argument(ProfileData::class)
    data class Profile(val id: String) : AppDestination(), 
        TypedDestination<ProfileData>
}
```

### 3.2 Back Stack Management

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| Stack Type | SnapshotStateList (mutable) | StateFlow (reactive) |
| Direct Access | ✅ Full control | ✅ Full control |
| Push Operation | `backStack.add(key)` | `navigator.navigate()` / `backStack.push()` |
| Pop Operation | `backStack.removeLastOrNull()` | `navigator.navigateBack()` / `backStack.pop()` |
| Pop Until | `backStack.removeAll { ... }` | `backStack.popUntil { ... }` |
| Replace | Manual manipulation | `navigateAndReplace()` / `backStack.replace()` |
| Clear All | `backStack.clear()` | `navigateAndClearAll()` / `backStack.clear()` |
| Pop to Route | Manual | `backStack.popTo(route)` |
| Pop to Root | Manual | `backStack.popToRoot()` |

**Key Difference:** Nav3 gives raw list control; Quo Vadis wraps operations in semantic methods.

### 3.3 State Management

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| Core State Type | SnapshotStateList | StateFlow |
| Current Destination | Via backStack.lastOrNull() | `navigator.currentDestination: StateFlow` |
| Previous Destination | Via backStack indexing | `navigator.previousDestination: StateFlow` |
| Can Go Back | `backStack.size > 1` | `backStack.canGoBack: StateFlow<Boolean>` |
| State Persistence | `rememberNavBackStack()` + serialization | `KotlinxNavigationStateSerializer` |
| Lifecycle Integration | `SavedStateNavEntryDecorator` | Manual / Compose integration |

### 3.4 Navigation Host

**Nav3:**
```kotlin
val backStack = rememberNavBackStack(Home)

NavDisplay(backStack, entryProvider = entryProvider {
    entry<Home> { HomeScreen(onNavigate = { backStack.add(Profile("1")) }) }
    entry<Profile> { key -> ProfileScreen(key.id) }
})
```

**Quo Vadis:**
```kotlin
val navigator = rememberNavigator()
val appGraph = navigationGraph("app") {
    startDestination(Home)
    destination(Home) { _, nav -> HomeScreen(nav) }
    destination(Profile) { dest, nav -> ProfileScreen(dest.id, nav) }
}

GraphNavHost(navigator = navigator, graph = appGraph)
```

### 3.5 Transitions & Animations

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| Built-in Transitions | ✅ Default specs provided | ✅ Fade, Slide, Scale, Custom |
| Custom Transitions | Via `transitionSpec` parameter | Via `customTransition {}` DSL |
| Predictive Back | ✅ Via `predictivePopTransitionSpec` | ✅ First-class support (Android & iOS) |
| Shared Elements | Via Compose APIs | ✅ Built-in with `destinationWithScopes()` |
| Transition per Route | ✅ Per-scene metadata | ✅ Per-navigation call |

**Nav3 Transitions:**
```kotlin
NavDisplay(
    backStack = backStack,
    transitionSpec = { ... },
    predictivePopTransitionSpec = { ... },
    entryProvider = ...
)
```

**Quo Vadis Transitions:**
```kotlin
navigator.navigate(Profile("123"), NavigationTransitions.SlideHorizontal)

// Or custom
navigator.navigate(dest, customTransition {
    enter = slideInHorizontally() + fadeIn()
    exit = slideOutHorizontally() + fadeOut()
})
```

### 3.6 Deep Linking

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| URI Parsing | Via NavUri class | Via `DeepLink.parse()` |
| Pattern Matching | Via serialization | Manual pattern registration |
| Query Parameters | ✅ Supported | ✅ Via `deepLink.parameters` |
| Automatic Registration | ❌ Manual | ❌ Manual (`handler.register()`) |

### 3.7 Nested/Tabbed Navigation

| Feature | Nav3 | Quo Vadis |
|---------|------|-----------|
| Nested Graphs | Via scene strategies | `navigationGraph { include() }` |
| Tabbed Navigation | Custom scene strategy | First-class `@TabGraph`, `@Tab` |
| Independent Backstacks | Custom implementation | ✅ Built-in per tab |
| Tab State Preservation | Custom implementation | ✅ Automatic |

**Quo Vadis Tabbed Navigation (Unique Feature):**
```kotlin
@TabGraph(name = "main_tabs", initialTab = "Home", primaryTab = "Home")
sealed class MainTabs : TabDefinition {
    @Tab(route = "tab_home", label = "Home", icon = "home")
    data object Home : MainTabs()
    
    @Tab(route = "tab_profile", label = "Profile", icon = "person")
    data object Profile : MainTabs()
}
```

### 3.8 Scene Strategies (Nav3 Unique)

Nav3 introduces **SceneStrategy** for layout logic:

| Strategy | Purpose |
|----------|---------|
| `SinglePaneSceneStrategy` | Traditional single-screen navigation |
| `DialogSceneStrategy` | Overlays dialog on current content |
| Custom strategies | Multi-pane, adaptive layouts |

```kotlin
val customStrategy = rememberMySceneStrategy()

NavDisplay(
    backStack = backStack,
    sceneStrategy = customStrategy then SinglePaneSceneStrategy,
    entryProvider = ...
)
```

**Quo Vadis:** Uses a simpler model without scene strategies; dialogs and layouts handled differently.

---

## 4. Platform Support

| Platform | Nav3 | Quo Vadis |
|----------|------|-----------|
| Android | ✅ Primary target | ✅ Full support |
| iOS | ✅ (KMP runtime) | ✅ Full support |
| Desktop (JVM) | ✅ (KMP runtime) | ✅ Full support |
| JavaScript | ✅ (KMP runtime) | ✅ Full support |
| WebAssembly | ✅ (KMP runtime) | ✅ Full support |
| macOS Native | Via KMP | ✅ Full support |
| Linux Native | Via KMP | ✅ Full support |
| Windows Native | Via KMP | Via JVM Desktop |

**Note:** Nav3's KMP support was added across alpha releases. Quo Vadis was designed multiplatform from inception.

---

## 5. Testing Support

### Nav3 Testing

No dedicated testing artifact mentioned; relies on:
- Direct backstack manipulation for state verification
- Compose testing rules for UI verification

### Quo Vadis Testing

Dedicated `FakeNavigator` class:

```kotlin
@Test
fun `navigate to details screen`() {
    val navigator = FakeNavigator()
    val viewModel = MyViewModel(navigator)
    
    viewModel.onItemClicked("123")
    
    assertTrue(navigator.verifyNavigateTo("details"))
    assertEquals(1, navigator.getNavigateCallCount("details"))
    assertNotNull(navigator.navigationCalls.last().arguments["id"])
}
```

**Verification Methods:**
- `verifyNavigateTo(route): Boolean`
- `verifyNavigateBack(): Boolean`
- `getNavigateCallCount(route): Int`
- `navigationCalls: List<NavigationCall>` (full audit trail)

---

## 6. MVI/Architecture Integration

### Nav3

- Designed to work with any state management solution
- Developer owns the backstack state
- Can integrate with ViewModel, MVI, MVVM, etc.
- No opinionated architecture pattern

### Quo Vadis

Dedicated `quo-vadis-core-flow-mvi` module for FlowMVI integration:

```kotlin
class ProfileContainer(private val navigator: Navigator) : Container<ProfileState, ProfileIntent> {
    override val store: Store<ProfileState, ProfileIntent> = store(initial = ProfileState.Loading) {
        reduce { intent ->
            when (intent) {
                ProfileIntent.NavigateToSettings -> navigator.navigate(Settings)
                ProfileIntent.NavigateBack -> navigator.navigateBack()
            }
        }
    }
}
```

---

## 7. Code Generation Comparison

| Aspect | Nav3 | Quo Vadis |
|--------|------|-----------|
| Code Generation | ❌ None | ✅ KSP-based |
| Boilerplate Reduction | N/A | ~50-70% less code |
| Generated Artifacts | N/A | Route initializers, Graph builders, Navigation extensions |
| Build Configuration | Simple | Requires KSP plugin setup |

**Quo Vadis Generates:**
- `{Graph}RouteInitializer` - Auto-registers routes
- `build{Graph}Graph()` - Graph construction function
- `navigateTo{Destination}()` - Type-safe navigation extensions

---

## 8. API Surface Comparison

### Minimal Nav3 Usage

```kotlin
@Serializable object Home : NavKey
@Serializable data class Chat(val id: String) : NavKey

val backStack = rememberNavBackStack(Home)

NavDisplay(backStack, entryProvider = entryProvider {
    entry<Home> {
        Column {
            Text("Home")
            Button(onClick = { backStack.add(Chat("123")) }) {
                Text("Go to Chat")
            }
        }
    }
    entry<Chat> { key -> Text("Chat: ${key.id}") }
})
```

### Minimal Quo Vadis Usage

```kotlin
sealed class AppDest : Destination {
    object Home : AppDest() { override val route = "home" }
    data class Chat(val id: String) : AppDest() { override val route = "chat" }
}

val navigator = rememberNavigator()
val graph = navigationGraph("app") {
    startDestination(AppDest.Home)
    destination(AppDest.Home) { _, nav ->
        Column {
            Text("Home")
            Button(onClick = { nav.navigate(AppDest.Chat("123")) }) {
                Text("Go to Chat")
            }
        }
    }
    destination(AppDest.Chat::class) { dest, _ -> Text("Chat: ${dest.id}") }
}

GraphNavHost(navigator, graph)
```

---

## 9. Unique Features Summary

### Nav3 Exclusive Features

1. **SceneStrategy System** - Flexible layout strategies for adaptive UI
2. **Scene-level Lifecycle** - Lifecycle scoped to scenes, not just entries
3. **NavEntryDecorator Pattern** - Clean separation of cross-cutting concerns
4. **Simpler Mental Model** - Just a list and a display function
5. **Official Google Support** - Part of AndroidX with long-term commitment

### Quo Vadis Exclusive Features

1. **KSP Code Generation** - Annotation-driven boilerplate reduction
2. **First-class Tabbed Navigation** - `@TabGraph` and `@Tab` annotations
3. **FakeNavigator for Testing** - Dedicated testing utilities
4. **FlowMVI Integration** - First-class MVI architecture support
5. **Gray Box Pattern** - Feature modules expose entry points, hide internals
6. **Direct BackStack Semantic Methods** - `popUntil()`, `popTo()`, `popToRoot()`
7. **Independent from AndroidX** - No external navigation dependencies
8. **Predictive Back on iOS** - Same smooth animations as Android

---

## 10. Migration Considerations

### From Jetpack Navigation (Nav 2.x) to Nav3

- Completely new API; no migration path
- Remove NavController, NavGraph XML
- Convert routes to serializable types
- Replace NavHost with NavDisplay

### From Jetpack Navigation to Quo Vadis

- Replace NavController with Navigator
- Convert routes to sealed class Destinations
- Replace NavHost with GraphNavHost
- Consider adding KSP for annotation-based approach

### From Nav3 to Quo Vadis (or vice versa)

**Similar concepts:**
- Type-safe routes via kotlinx.serialization
- Direct backstack manipulation
- Compose-first approach

**Key differences to adapt:**
- Navigator vs raw backstack control
- SceneStrategy vs NavigationGraph
- entryProvider vs destination() DSL

---

## 11. Performance Considerations

| Aspect | Nav3 | Quo Vadis |
|--------|------|-----------|
| State Updates | SnapshotStateList (efficient Compose integration) | StateFlow (Kotlin coroutines) |
| Recomposition | Optimized via stable keys | Optimized via keyed content |
| Memory | Minimal (just a list) | Additional wrapper objects |
| Startup | No code generation | KSP build step required |
| Animation | SeekableTransitionState | Custom animation system |

---

## 12. When to Choose Which

### Choose Nav3 When:

- ✅ Building primarily for **Android** with some KMP
- ✅ Want **official Google support** and documentation
- ✅ Prefer **simplest possible API** (just a list!)
- ✅ Need **adaptive layouts** with SceneStrategy
- ✅ Already invested in **AndroidX ecosystem**
- ✅ Want **minimal learning curve** for Compose developers

### Choose Quo Vadis When:

- ✅ Building **true multiplatform** apps (Android, iOS, Desktop, Web)
- ✅ Want **annotation-driven code generation** for less boilerplate
- ✅ Need **first-class tabbed navigation** with independent backstacks
- ✅ Using **MVI architecture** (FlowMVI integration)
- ✅ Want **dedicated testing utilities** (FakeNavigator)
- ✅ Prefer **independence from AndroidX** navigation ecosystem
- ✅ Need **predictive back animations on iOS**
- ✅ Want **gray box modularization** for feature modules

---

## 13. Conclusion

Both Navigation 3 and Quo Vadis represent modern, type-safe approaches to Compose navigation with significant improvements over traditional approaches.

**Nav3** excels in simplicity and official support, giving developers raw control over a simple list while the library handles rendering. Its SceneStrategy system enables sophisticated adaptive layouts.

**Quo Vadis** excels in multiplatform support, developer experience through code generation, and comprehensive features like tabbed navigation and MVI integration. It provides more semantic navigation APIs and dedicated testing support.

The choice ultimately depends on:
1. **Platform targets** - True multiplatform favors Quo Vadis
2. **Architecture preferences** - MVI favors Quo Vadis; flexible patterns favor Nav3
3. **Team familiarity** - AndroidX ecosystem favors Nav3
4. **Feature requirements** - Tabbed navigation, testing utilities favor Quo Vadis
5. **Build complexity tolerance** - Simple build favors Nav3; KSP usage favors Quo Vadis

Both libraries can successfully power production navigation systems with type safety, modern APIs, and excellent developer experience.

---

## Appendix: Version Information

| Library | Version | Release Date | Status |
|---------|---------|--------------|--------|
| Navigation 3 | 1.0.0 | November 19, 2025 | Stable |
| Quo Vadis Core | See build.gradle.kts | October 28, 2025 | Production |
| Navigation (2.x) | 2.9.6 | - | Stable (predecessor) |

---

## References

- [Navigation 3 Release Notes](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Navigation 3 Blog Announcement](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html)
- [Nav3 Recipes Repository](https://github.com/android/nav3-recipes)
- [Quo Vadis Documentation](./quo-vadis-core/docs/)
- [Navigation Design Principles](https://developer.android.com/guide/navigation/design/principles)
