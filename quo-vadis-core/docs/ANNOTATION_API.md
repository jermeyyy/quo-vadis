# Annotation-based API Guide

## Overview

The **Annotation-based API** is the recommended approach for defining navigation in Quo Vadis. It uses Kotlin Symbol Processing (KSP) to generate boilerplate code at compile-time, dramatically reducing the amount of manual code you need to write while maintaining full type-safety.

### What is the Annotation-based API?

Instead of manually registering routes and building navigation graphs with DSL functions, you simply annotate your destination classes and Composable functions. KSP then generates all the necessary wiring code automatically.

### Benefits Over Manual DSL

- **Less Boilerplate**: Eliminate 70-80% of manual navigation setup code
- **Auto-wiring**: Composables are automatically connected to destinations
- **Compile-time Safety**: Catch errors at compile-time, not runtime
- **Route Registration**: Routes are automatically registered on initialization
- **Type-safe Arguments**: Automatic serialization/deserialization of destination data
- **Maintainability**: Changes to destinations don't require updating graph builders
- **Discoverability**: IDE can help you find all destinations and their content functions

### When to Use Annotations vs Manual DSL

**Use Annotation-based API when:**
- Starting a new project or feature module
- You have many destinations with straightforward navigation
- You want minimal boilerplate and maximum maintainability
- Your team prefers declarative over imperative code

**Use Manual DSL when:**
- You need highly dynamic navigation that changes at runtime
- You're integrating with existing code that uses manual DSL
- You need fine-grained control over graph construction
- You're building a library that shouldn't depend on KSP

**Note:** You can mix both approaches in the same project. The annotation-based API generates standard DSL code, so they work together seamlessly.

---

## Setup

### 1. Add Dependencies

Add the Quo Vadis annotations and KSP plugin to your shared module's `build.gradle.kts`:

```kotlin
plugins {
    // ... other plugins
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core navigation library
                implementation("io.github.jermeyyy:quo-vadis-core:0.1.0")
                
                // Annotation definitions
                implementation("io.github.jermeyyy:quo-vadis-annotations:0.1.0")
                
                // Required for type-safe arguments
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
    }
}

dependencies {
    // KSP processor (applied to common source set)
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.1.0")
}
```

### 2. Apply KSP Plugin

Ensure the KSP plugin is in your version catalog (`gradle/libs.versions.toml`):

```toml
[versions]
ksp = "2.2.20-1.0.29"
quoVadis = "0.1.0"

[libraries]
quo-vadis-core = { module = "io.github.jermeyyy:quo-vadis-core", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "io.github.jermeyyy:quo-vadis-annotations", version.ref = "quoVadis" }
quo-vadis-ksp = { module = "io.github.jermeyyy:quo-vadis-ksp", version.ref = "quoVadis" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 3. Enable kotlinx.serialization

Add the serialization plugin for type-safe arguments:

```kotlin
plugins {
    kotlin("plugin.serialization") version "2.2.20"
    alias(libs.plugins.ksp)
}
```

### 4. Module Structure Requirements

Your navigation code should follow this structure:

```
src/commonMain/kotlin/
├── destinations/
│   └── Destinations.kt          # Annotated destination definitions
├── content/
│   └── ContentDefinitions.kt    # @Content Composable functions
└── graphs/
    └── NavigationGraphs.kt      # Include generated graphs
```

**Generated code will appear in:**
```
build/generated/ksp/metadata/commonMain/kotlin/
└── [your.package.name]/
    └── destinations/
        ├── MainDestinationRouteInitializer.kt
        ├── MainDestinationRegistry.kt
        └── MainDestinationGraphBuilderKt.kt
```

---

## Core Annotations

### @Graph

Marks a sealed class as a navigation graph. The sealed class must extend `Destination`.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String)
```

**Parameters:**
- `name`: Unique identifier for this navigation graph (used in generated function names)

**Example:**

```kotlin
@Graph("main")
sealed class MainDestination : Destination {
    // Destination objects/classes go here
}
```

**What Gets Generated:**
- `buildMainDestinationGraph()` - Graph builder function
- `MainDestinationRouteInitializer` - Route registration object

---

### @Route

Specifies the route path for a destination. Applied to destination objects or classes.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)
```

**Parameters:**
- `path`: The route path string (e.g., "main/home", "details/{id}")

**Example:**

```kotlin
@Graph("main")
sealed class MainDestination : Destination {
    @Route("main/home")
    data object Home : MainDestination()
    
    @Route("main/settings")
    data object Settings : MainDestination()
}
```

**What Gets Generated:**
```kotlin
// Route registration in init block
object MainDestinationRouteInitializer {
    init {
        RouteRegistry.register(MainDestination.Home::class, "main/home")
        RouteRegistry.register(MainDestination.Settings::class, "main/settings")
    }
}
```

---

### @Argument

Specifies that a destination has typed, serializable arguments. Used with `TypedDestination`.

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<*>)
```

**Parameters:**
- `dataClass`: The `KClass` of the serializable data type (must be annotated with `@Serializable`)

**Example:**

```kotlin
@Serializable
data class DetailData(val itemId: String, val mode: String = "view")

@Graph("feature")
sealed class FeatureDestination : Destination {
    @Route("feature/detail")
    @Argument(DetailData::class)
    data class Detail(val itemId: String, val mode: String = "view") 
        : FeatureDestination(), TypedDestination<DetailData> {
        override val data = DetailData(itemId, mode)
    }
}
```

**What Gets Generated:**
```kotlin
// Typed destination extension in graph builder
fun NavigationGraphBuilder.typedDestinationDetail(
    destination: KClass<FeatureDestination.Detail>,
    transition: NavigationTransition? = null,
    content: @Composable (DetailData, Navigator) -> Unit
) {
    typedDestination(
        destination = destination,
        dataClass = DetailData::class,
        transition = transition,
        content = content
    )
}
```

**Requirements:**
- Destination must implement `TypedDestination<T>`
- Data class must be annotated with `@Serializable`
- Override the `data` property with your data instance

---

### @Content

Marks a Composable function as the content renderer for a specific destination. KSP automatically wires this function into the navigation graph.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Content(val destination: KClass<*>)
```

**Parameters:**
- `destination`: The KClass of the destination this Composable renders

**Function Signatures:**

For **simple destinations** (no typed arguments):
```kotlin
@Content(MainDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    // Your UI
}
```

For **typed destinations** (with `@Argument`):
```kotlin
@Content(FeatureDestination.Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
    // Your UI using 'data'
}
```

**What Gets Generated:**

```kotlin
fun buildMainDestinationGraph() = navigationGraph("main") {
    startDestination(MainDestination.Home)
    
    // Simple destination
    destination(MainDestination.Home) { _, navigator ->
        HomeContent(navigator)
    }
    
    // Typed destination
    typedDestinationDetail(
        destination = FeatureDestination.Detail::class,
        transition = null
    ) { data, navigator ->
        DetailContent(data, navigator)
    }
}
```

---

## Code Generation

### What Gets Generated

For each `@Graph` annotated sealed class, KSP generates three components:

#### 1. Route Initializer

Automatically registers all routes when the class is loaded:

```kotlin
object MainDestinationRouteInitializer {
    init {
        RouteRegistry.register(MainDestination.Home::class, "main/home")
        RouteRegistry.register(MainDestination.Profile::class, "main/profile")
        // ... for each @Route destination
    }
}
```

**Triggered by:** First reference to any destination in the sealed class

#### 2. Graph Builder Function

Creates the complete navigation graph with all content wired:

```kotlin
fun buildMainDestinationGraph(): NavigationGraph {
    return navigationGraph("main") {
        startDestination(MainDestination.Home)
        
        destination(MainDestination.Home) { _, navigator ->
            HomeContent(navigator)
        }
        
        destination(MainDestination.Profile) { _, navigator ->
            ProfileContent(navigator)
        }
        // ... for each destination with @Content
    }
}
```

**Usage:** Include in your root graph

#### 3. Typed Destination Extensions (for @Argument destinations)

Helper functions for type-safe destination registration:

```kotlin
fun NavigationGraphBuilder.typedDestinationDetail(
    destination: KClass<FeatureDestination.Detail>,
    transition: NavigationTransition? = null,
    content: @Composable (DetailData, Navigator) -> Unit
) {
    typedDestination(
        destination = destination,
        dataClass = DetailData::class,
        transition = transition,
        content = content
    )
}
```

**Usage:** Called automatically by generated graph builder

### Generated File Locations

Generated files are organized by package and graph name:

```
build/generated/ksp/metadata/commonMain/kotlin/
└── com/jermey/yourapp/destinations/
    ├── MainDestinationRouteInitializer.kt
    ├── MainDestinationRegistry.kt           # Contains buildMainDestinationGraph()
    └── FeatureDestinationRegistry.kt        # If you have multiple graphs
```

### Naming Conventions

- **Route Initializer**: `{GraphClassName}RouteInitializer`
- **Registry File**: `{GraphClassName}Registry.kt`
- **Graph Builder**: `build{GraphClassName}Graph()`
- **Typed Extensions**: `typedDestination{DestinationName}(...)`

---

## Complete Examples

### Example 1: Simple Destinations (No Arguments)

**Define destinations:**

```kotlin
package com.example.app.destinations

import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Route
import com.jermey.quo.vadis.core.navigation.core.Destination

@Graph("main")
sealed class MainDestination : Destination {
    @Route("main/home")
    data object Home : MainDestination()
    
    @Route("main/profile")
    data object Profile : MainDestination()
    
    @Route("main/settings")
    data object Settings : MainDestination()
}
```

**Define content:**

```kotlin
package com.example.app.content

import androidx.compose.runtime.Composable
import com.example.app.destinations.MainDestination
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.Navigator

@Content(MainDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(
        onNavigateToProfile = { 
            navigator.navigate(MainDestination.Profile) 
        }
    )
}

@Content(MainDestination.Profile::class)
@Composable
fun ProfileContent(navigator: Navigator) {
    ProfileScreen(
        onNavigateToSettings = { 
            navigator.navigate(MainDestination.Settings) 
        }
    )
}

@Content(MainDestination.Settings::class)
@Composable
fun SettingsContent(navigator: Navigator) {
    SettingsScreen(
        onBack = { navigator.navigateBack() }
    )
}
```

**Use the generated graph:**

```kotlin
package com.example.app.graphs

import com.example.app.destinations.buildMainDestinationGraph
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

fun appRootGraph() = navigationGraph("app_root") {
    startDestination(MainDestination.Home)
    include(buildMainDestinationGraph())
}
```

**Setup in your app:**

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(appRootGraph())
        navigator.setStartDestination(MainDestination.Home)
    }
    
    GraphNavHost(
        graph = appRootGraph(),
        navigator = navigator
    )
}
```

---

### Example 2: Typed Destinations with Arguments

**Define destinations with typed data:**

```kotlin
package com.example.shop.destinations

import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

// Data classes must be @Serializable
@Serializable
data class ProductData(
    val productId: String,
    val category: String
)

@Serializable
data class CheckoutData(
    val items: List<String>,
    val totalPrice: Double
)

@Graph("shop")
sealed class ShopDestination : Destination {
    @Route("shop/catalog")
    data object Catalog : ShopDestination()
    
    @Route("shop/product")
    @Argument(ProductData::class)
    data class Product(
        val productId: String,
        val category: String
    ) : ShopDestination(), TypedDestination<ProductData> {
        override val data = ProductData(productId, category)
    }
    
    @Route("shop/checkout")
    @Argument(CheckoutData::class)
    data class Checkout(
        val items: List<String>,
        val totalPrice: Double
    ) : ShopDestination(), TypedDestination<CheckoutData> {
        override val data = CheckoutData(items, totalPrice)
    }
}
```

**Define content with typed parameters:**

```kotlin
package com.example.shop.content

import androidx.compose.runtime.Composable
import com.example.shop.destinations.*
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.Navigator

@Content(ShopDestination.Catalog::class)
@Composable
fun CatalogContent(navigator: Navigator) {
    CatalogScreen(
        onProductClick = { productId, category ->
            navigator.navigate(
                ShopDestination.Product(productId, category)
            )
        }
    )
}

// Note: Typed destinations receive data as first parameter
@Content(ShopDestination.Product::class)
@Composable
fun ProductContent(data: ProductData, navigator: Navigator) {
    ProductScreen(
        productId = data.productId,
        category = data.category,
        onAddToCart = { /* ... */ },
        onBack = { navigator.navigateBack() }
    )
}

@Content(ShopDestination.Checkout::class)
@Composable
fun CheckoutContent(data: CheckoutData, navigator: Navigator) {
    CheckoutScreen(
        items = data.items,
        totalPrice = data.totalPrice,
        onConfirm = { /* ... */ },
        onBack = { navigator.navigateBack() }
    )
}
```

---

### Example 3: Complex Graph with Multiple Destination Types

This example shows a realistic feature module with various destination patterns:

```kotlin
package com.example.tasks.destinations

import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

// Serializable data classes
@Serializable
data class TaskDetailData(
    val taskId: String,
    val editMode: Boolean = false
)

@Serializable
data class TaskFilterData(
    val category: String?,
    val priority: String?,
    val status: String?
)

@Graph("tasks")
sealed class TasksDestination : Destination {
    // Simple destinations
    @Route("tasks/list")
    data object TaskList : TasksDestination()
    
    @Route("tasks/create")
    data object CreateTask : TasksDestination()
    
    @Route("tasks/settings")
    data object TaskSettings : TasksDestination()
    
    // Typed destination with arguments
    @Route("tasks/detail")
    @Argument(TaskDetailData::class)
    data class TaskDetail(
        val taskId: String,
        val editMode: Boolean = false
    ) : TasksDestination(), TypedDestination<TaskDetailData> {
        override val data = TaskDetailData(taskId, editMode)
    }
    
    // Typed destination with optional arguments
    @Route("tasks/filter")
    @Argument(TaskFilterData::class)
    data class FilterTasks(
        val category: String? = null,
        val priority: String? = null,
        val status: String? = null
    ) : TasksDestination(), TypedDestination<TaskFilterData> {
        override val data = TaskFilterData(category, priority, status)
    }
}
```

**Content definitions:**

```kotlin
package com.example.tasks.content

import androidx.compose.runtime.Composable
import com.example.tasks.destinations.*
import com.example.tasks.ui.screens.*
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

@Content(TasksDestination.TaskList::class)
@Composable
fun TaskListContent(navigator: Navigator) {
    TaskListScreen(
        onTaskClick = { taskId ->
            navigator.navigate(
                TasksDestination.TaskDetail(taskId, editMode = false)
            )
        },
        onCreateTask = {
            navigator.navigate(
                TasksDestination.CreateTask,
                NavigationTransitions.SlideUp
            )
        },
        onFilter = {
            navigator.navigate(
                TasksDestination.FilterTasks(),
                NavigationTransitions.Fade
            )
        },
        onSettings = {
            navigator.navigate(TasksDestination.TaskSettings)
        }
    )
}

@Content(TasksDestination.TaskDetail::class)
@Composable
fun TaskDetailContent(data: TaskDetailData, navigator: Navigator) {
    TaskDetailScreen(
        taskId = data.taskId,
        isEditMode = data.editMode,
        onEdit = {
            navigator.navigateAndReplace(
                TasksDestination.TaskDetail(data.taskId, editMode = true)
            )
        },
        onBack = { navigator.navigateBack() }
    )
}

@Content(TasksDestination.CreateTask::class)
@Composable
fun CreateTaskContent(navigator: Navigator) {
    CreateTaskScreen(
        onTaskCreated = { taskId ->
            navigator.navigateAndClearTo(
                TasksDestination.TaskDetail(taskId),
                clearRoute = "tasks/create",
                inclusive = true
            )
        },
        onCancel = { navigator.navigateBack() }
    )
}

@Content(TasksDestination.FilterTasks::class)
@Composable
fun FilterTasksContent(data: TaskFilterData, navigator: Navigator) {
    FilterScreen(
        initialCategory = data.category,
        initialPriority = data.priority,
        initialStatus = data.status,
        onApply = { category, priority, status ->
            // Navigate back with filter results
            navigator.navigateBack()
        },
        onClear = {
            navigator.navigateBack()
        }
    )
}

@Content(TasksDestination.TaskSettings::class)
@Composable
fun TaskSettingsContent(navigator: Navigator) {
    TaskSettingsScreen(
        onBack = { navigator.navigateBack() }
    )
}
```

**Include in root graph:**

```kotlin
package com.example.app.graphs

import com.example.app.destinations.MainDestination
import com.example.app.destinations.buildMainDestinationGraph
import com.example.tasks.destinations.buildTasksDestinationGraph
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

fun appRootGraph() = navigationGraph("app_root") {
    startDestination(MainDestination.Home)
    
    // Include all generated graphs
    include(buildMainDestinationGraph())
    include(buildTasksDestinationGraph())
    // Add more as needed
}
```

---

### Example 4: Integration with Existing Manual DSL

You can mix annotation-based and manual approaches in the same project:

```kotlin
package com.example.app.graphs

import com.example.app.destinations.*
import com.jermey.quo.vadis.core.navigation.core.navigationGraph
import com.jermey.quo.vadis.core.navigation.core.SimpleDestination

fun hybridGraph() = navigationGraph("hybrid") {
    startDestination(MainDestination.Home)
    
    // Include annotation-generated graph
    include(buildMainDestinationGraph())
    
    // Add manual destinations
    destination(SimpleDestination("legacy/screen")) { dest, navigator ->
        LegacyScreen(navigator = navigator)
    }
    
    // Add dynamic destinations
    destination(SimpleDestination("dynamic/{id}")) { dest, navigator ->
        val id = dest.arguments["id"] as String
        DynamicScreen(id = id, navigator = navigator)
    }
    
    // Add nested graph manually
    nestedGraph("manual_nested") {
        startDestination(SimpleDestination("nested/start"))
        
        destination(SimpleDestination("nested/start")) { _, nav ->
            NestedStartScreen(navigator = nav)
        }
    }
}
```

---

## Advanced Topics

### Dynamic Routes

For routes that are truly dynamic (not known at compile-time), you can register them manually in an `init` block:

```kotlin
@Graph("tabs")
sealed class TabsDestination : Destination {
    @Route("tabs/main")
    data object Main : TabsDestination()
    
    // Dynamic route - register at instance creation
    data class Tab(val tabId: String) : TabsDestination() {
        init {
            RouteRegistry.register(this::class, "tabs/tab_$tabId")
        }
        override val data = tabId
    }
}
```

**Note:** Dynamic destinations won't have generated `@Content` wiring. You'll need to register them manually or handle them programmatically.

---

### Mixing Annotations with Manual Graph Building

If you need fine-grained control, you can define destinations with annotations but build the graph manually:

```kotlin
// Destinations.kt
@Graph("custom")
sealed class CustomDestination : Destination {
    @Route("custom/start")
    data object Start : CustomDestination()
    
    @Route("custom/end")
    data object End : CustomDestination()
}

// Manual graph building
fun customGraph() = navigationGraph("custom") {
    startDestination(CustomDestination.Start)
    
    // Manually register destinations with custom logic
    destination(CustomDestination.Start) { _, navigator ->
        if (someCondition) {
            StartScreenVariantA(navigator)
        } else {
            StartScreenVariantB(navigator)
        }
    }
    
    destination(CustomDestination.End) { _, navigator ->
        EndScreen(navigator)
    }
}
```

Routes are still auto-registered, but you control graph construction.

---

### Testing Strategies

**Unit Testing Destinations:**

```kotlin
@Test
fun testDestinationCreation() {
    val dest = ShopDestination.Product(
        productId = "123",
        category = "electronics"
    )
    
    assertEquals("123", dest.data.productId)
    assertEquals("electronics", dest.data.category)
}
```

**Testing Navigation with FakeNavigator:**

```kotlin
@Test
fun testNavigationFlow() {
    val fakeNavigator = FakeNavigator()
    val viewModel = ShopViewModel(fakeNavigator)
    
    viewModel.onProductClick("123", "electronics")
    
    val lastDestination = fakeNavigator.lastDestination as ShopDestination.Product
    assertEquals("123", lastDestination.productId)
    assertEquals("electronics", lastDestination.category)
}
```

**Testing Generated Graph:**

```kotlin
@Test
fun testGeneratedGraphStructure() {
    val graph = buildShopDestinationGraph()
    
    assertEquals("shop", graph.graphRoute)
    assertEquals(ShopDestination.Catalog, graph.startDestination)
    assertTrue(graph.destinations.size >= 3)
}
```

---

### Troubleshooting KSP Generation

#### Problem: Generated code not appearing

**Solution 1:** Clean and rebuild
```bash
./gradlew clean build
```

**Solution 2:** Check KSP is applied correctly
```kotlin
// In build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.1.0")
}
```

**Solution 3:** Verify annotation retention
Annotations must have `SOURCE` retention for KSP:
```kotlin
@Retention(AnnotationRetention.SOURCE)
```

#### Problem: Compilation errors in generated code

**Solution:** Check your destination/content signatures match:

**Simple destination:**
```kotlin
@Content(MyDestination.Screen::class)
@Composable
fun ScreenContent(navigator: Navigator) { /* ... */ }
```

**Typed destination:**
```kotlin
@Content(MyDestination.DetailScreen::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) { /* ... */ }
```

#### Problem: Routes not registered

**Solution:** Ensure you reference at least one destination from the sealed class before navigating. This triggers the `RouteInitializer` init block:

```kotlin
// Somewhere in your code, reference a destination
LaunchedEffect(Unit) {
    navigator.registerGraph(buildMainDestinationGraph())
    navigator.setStartDestination(MainDestination.Home) // This triggers init
}
```

#### Problem: IDE doesn't recognize generated code

**Solution 1:** Mark generated sources directory:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }
}
```

**Solution 2:** Invalidate caches and restart IDE

**Solution 3:** Run Gradle sync

---

## Comparison: Annotation-based vs Manual DSL

### Before (Manual DSL): ~150 lines

```kotlin
// Destinations
sealed class ShopDestination : Destination {
    object Catalog : ShopDestination() {
        override val route = "shop/catalog"
    }
    
    data class Product(val productId: String, val category: String) : ShopDestination() {
        override val route = "shop/product"
        override val arguments = mapOf(
            "productId" to productId,
            "category" to category
        )
    }
}

// Manual route registration
object ShopRoutes {
    init {
        RouteRegistry.register(ShopDestination.Catalog::class, "shop/catalog")
        RouteRegistry.register(ShopDestination.Product::class, "shop/product")
    }
}

// Manual graph building
fun shopGraph() = navigationGraph("shop") {
    startDestination(ShopDestination.Catalog)
    
    destination(ShopDestination.Catalog) { _, navigator ->
        CatalogScreen(
            onProductClick = { id, cat ->
                navigator.navigate(ShopDestination.Product(id, cat))
            }
        )
    }
    
    destination(SimpleDestination("shop/product")) { dest, navigator ->
        val productId = dest.arguments["productId"] as String
        val category = dest.arguments["category"] as String
        
        ProductScreen(
            productId = productId,
            category = category,
            onBack = { navigator.navigateBack() }
        )
    }
}
```

### After (Annotation-based): ~50 lines

```kotlin
// Destinations
@Serializable
data class ProductData(val productId: String, val category: String)

@Graph("shop")
sealed class ShopDestination : Destination {
    @Route("shop/catalog")
    data object Catalog : ShopDestination()
    
    @Route("shop/product")
    @Argument(ProductData::class)
    data class Product(val productId: String, val category: String) 
        : ShopDestination(), TypedDestination<ProductData> {
        override val data = ProductData(productId, category)
    }
}

// Content
@Content(ShopDestination.Catalog::class)
@Composable
fun CatalogContent(navigator: Navigator) {
    CatalogScreen(
        onProductClick = { id, cat ->
            navigator.navigate(ShopDestination.Product(id, cat))
        }
    )
}

@Content(ShopDestination.Product::class)
@Composable
fun ProductContent(data: ProductData, navigator: Navigator) {
    ProductScreen(
        productId = data.productId,
        category = data.category,
        onBack = { navigator.navigateBack() }
    )
}

// Usage
fun appGraph() = navigationGraph("app") {
    include(buildShopDestinationGraph()) // All wiring done automatically!
}
```

**Result:** 67% less boilerplate, 100% type-safety maintained, zero manual argument parsing.

---

## Best Practices

### 1. Organize by Feature

Keep destinations, content, and screens together:

```
features/
├── shop/
│   ├── destinations/
│   │   └── ShopDestinations.kt
│   ├── content/
│   │   └── ShopContent.kt
│   └── ui/
│       └── screens/
│           ├── CatalogScreen.kt
│           └── ProductScreen.kt
```

### 2. Use Meaningful Graph Names

Graph names appear in function names, so make them clear:

```kotlin
@Graph("user_profile")  // → buildUserProfileDestinationGraph()
@Graph("settings")      // → buildSettingsDestinationGraph()
@Graph("checkout")      // → buildCheckoutDestinationGraph()
```

### 3. Group Related Destinations

Use sealed classes to group related destinations:

```kotlin
@Graph("onboarding")
sealed class OnboardingDestination : Destination { /* ... */ }

@Graph("main_app")
sealed class MainDestination : Destination { /* ... */ }

@Graph("admin")
sealed class AdminDestination : Destination { /* ... */ }
```

### 4. Leverage Serialization

Make data classes rich with kotlinx.serialization features:

```kotlin
@Serializable
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("created_at")
    val createdAt: Long,
    val roles: List<String> = emptyList()
)
```

### 5. Document Your Destinations

Add KDoc to help team members:

```kotlin
/**
 * Main application destinations for authenticated users.
 * 
 * Includes home, profile, and settings screens.
 */
@Graph("main")
sealed class MainDestination : Destination {
    /**
     * Home screen - the default landing page after login.
     */
    @Route("main/home")
    data object Home : MainDestination()
}
```

### 6. Use Descriptive Route Paths

Make routes reflect the navigation hierarchy:

```kotlin
@Route("shop/catalog")           // Good
@Route("shop/catalog/category")  // Better
@Route("shop/product/detail")    // Better

@Route("screen1")                // Avoid
@Route("s1")                     // Avoid
```

---

## See Also

- [TYPED_DESTINATIONS.md](TYPED_DESTINATIONS.md) - Deep dive into type-safe arguments
- [MIGRATION.md](MIGRATION.md) - Migrating from manual DSL to annotations
- [API_REFERENCE.md](API_REFERENCE.md) - Complete API documentation
- [NAVIGATION_IMPLEMENTATION.md](NAVIGATION_IMPLEMENTATION.md) - Implementation details

---

## Summary

The annotation-based API is the recommended way to define navigation in Quo Vadis because it:

✅ Reduces boilerplate by 70-80%  
✅ Maintains full compile-time type-safety  
✅ Automatically wires destinations to content  
✅ Generates optimal navigation code  
✅ Works seamlessly with manual DSL  
✅ Scales well for large applications  

Start with annotations for new projects, and gradually migrate existing code as needed. The two approaches coexist perfectly, giving you the flexibility to use the right tool for each situation.
