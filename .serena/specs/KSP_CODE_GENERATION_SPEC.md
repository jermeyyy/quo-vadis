# KSP Code Generation Implementation Specification

## Project Context

**Quo Vadis Navigation Library** - Kotlin Multiplatform navigation library with type-safe navigation.

**Goal:** Implement KSP-based code generation to automatically generate navigation boilerplate from annotations, simplifying the DSL and reducing manual configuration.

---

## High-Level Overview

### Components to Build

1. **`quo-vadis-annotations`** - Multiplatform annotation definitions
2. **`quo-vadis-ksp`** - KSP processor (JVM-only)
3. **Build Configuration** - Gradle setup for all modules
4. **Generated Code** - Extensions and helpers for annotated destinations

### Target API (from NewDestinations.kt)

```kotlin
@Graph("master_detail")
sealed class MasterDetailDestination : Destination {
    @Route("master_detail/list")
    object List : MasterDetailDestination()

    @Route("master_detail/detail")
    @Argument(DetailData::class)
    data class Detail(val data: DetailData) : MasterDetailDestination()
}

// Usage (desired simplified API)
fun masterDetailGraph() = navigationGraph(MasterDetailDestination::class) {
    startDestination(MasterDetailDestination.List)
    
    destinationWithScopes(MasterDetailDestination.List) { _, nav, s, a ->
        MasterListScreen(...)
    }
    
    typedDestination(MasterDetailDestination.Detail::class) { data, nav ->
        DetailScreen(itemId = data.itemId, ...)
    }
}
```

---

## Phase 1: Setup Modules and Dependencies

### 1.1 Update `settings.gradle.kts`

**Location:** `/Users/jermey/Projects/NavPlayground/settings.gradle.kts`

**Action:** Add new modules to project includes

**Changes:**
```kotlin
include(":composeApp")
include(":quo-vadis-core")
include(":quo-vadis-annotations")  // ADD
include(":quo-vadis-ksp")         // ADD
```

**Rationale:** Register new modules with Gradle build system.

---

### 1.2 Update Version Catalog

**Location:** `/Users/jermey/Projects/NavPlayground/gradle/libs.versions.toml`

**Action:** Add KSP dependency

**Changes:**
```toml
[versions]
# ... existing versions ...
ksp = "2.2.20-1.0.30"  # KSP version matching Kotlin 2.2.20

[libraries]
# ... existing libraries ...

[plugins]
# ... existing plugins ...
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Rationale:** KSP version should match Kotlin version. Format: `{kotlin-version}-{ksp-api-version}`. For Kotlin 2.2.20, use KSP 2.2.20-1.0.30.

---

### 1.3 Create `quo-vadis-annotations/build.gradle.kts`

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-annotations/build.gradle.kts`

**Content:**
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    // All platforms that use the library
    jvm()
    
    androidTarget()
    
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    
    js(IR) {
        browser()
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // No dependencies - pure annotation module
        }
    }
}
```

**Rationale:** 
- Multiplatform module for annotations
- All target platforms need annotations at compile time
- No dependencies required for annotation definitions

---

### 1.4 Create `quo-vadis-ksp/build.gradle.kts`

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-ksp/build.gradle.kts`

**Content:**
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":quo-vadis-annotations"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.2.20-1.0.30")
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("com.squareup:kotlinpoet-ksp:1.18.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
```

**Rationale:**
- JVM-only module (KSP processors run on host JVM)
- KotlinPoet for code generation (type-safe, idiomatic)
- kotlinpoet-ksp for KSP-specific helpers

---

### 1.5 Update `quo-vadis-core/build.gradle.kts`

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-core/build.gradle.kts`

**Action:** Add optional dependency on annotations

**Changes:**
```kotlin
sourceSets {
    commonMain.dependencies {
        // ... existing dependencies ...
        
        // Optional: annotations for users who want to use KSP
        api(project(":quo-vadis-annotations"))
    }
}
```

**Rationale:** Make annotations available transitively to library users without forcing KSP usage.

---

### 1.6 Update `composeApp/build.gradle.kts`

**Location:** `/Users/jermey/Projects/NavPlayground/composeApp/build.gradle.kts`

**Action:** Apply KSP plugin and add dependencies

**Changes:**
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)  // ADD
}

// ... existing configuration ...

kotlin {
    // ... existing target configuration ...
    
    sourceSets {
        commonMain.dependencies {
            // ... existing dependencies ...
            implementation(project(":quo-vadis-annotations"))
        }
    }
}

// KSP configuration for each target
dependencies {
    add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
    add("kspAndroid", project(":quo-vadis-ksp"))
    add("kspIosArm64", project(":quo-vadis-ksp"))
    add("kspIosSimulatorArm64", project(":quo-vadis-ksp"))
    add("kspJs", project(":quo-vadis-ksp"))
    add("kspWasmJs", project(":quo-vadis-ksp"))
    add("kspJvm", project(":quo-vadis-ksp"))  // For Desktop target
}

// Generated code is automatically registered since KSP 1.8.0+
// No manual srcDir configuration needed
```

**Rationale:**
- Apply KSP plugin to enable processing
- Register processor for all platforms
- Link generated sources to commonMain

---

## Phase 2: Define Annotations

### 2.1 Create Annotation Definitions

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/`

#### File: `Annotations.kt`

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class as a navigation graph.
 * 
 * The sealed class should extend Destination and contain destination objects/classes
 * representing the screens in this graph.
 * 
 * @param name The unique identifier for this navigation graph
 * 
 * @sample
 * ```
 * @Graph("master_detail")
 * sealed class MasterDetailDestination : Destination
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String)

/**
 * Specifies the route path for a destination.
 * 
 * The route is used for navigation and deep linking.
 * 
 * @param path The route path (e.g., "master_detail/list")
 * 
 * @sample
 * ```
 * @Route("master_detail/list")
 * object List : MasterDetailDestination()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)

/**
 * Specifies the data class type for typed destinations.
 * 
 * The data class should be serializable (using kotlinx.serialization).
 * 
 * @param dataClass The KClass of the serializable data type
 * 
 * @sample
 * ```
 * @Route("master_detail/detail")
 * @Argument(DetailData::class)
 * data class Detail(val data: DetailData) : MasterDetailDestination()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<*>)
```

**Rationale:**
- SOURCE retention: annotations processed at compile time only
- Clear documentation with examples
- Covers essential navigation metadata: graph identity, routes, and typed arguments
- Simple and focused API

---

## Phase 3: Implement KSP Processor

### 3.1 Create Processor Entry Point

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`

#### File: `QuoVadisSymbolProcessor.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.annotations.Graph

/**
 * KSP processor for Quo Vadis navigation annotations.
 * 
 * Processes @Graph, @Route, @Argument, @StartDestination, @DefaultTransition
 * and generates type-safe navigation helpers.
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all classes annotated with @Graph
        val graphClasses = resolver
            .getSymbolsWithAnnotation(Graph::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
        
        if (!graphClasses.iterator().hasNext()) {
            return emptyList()
        }
        
        graphClasses.forEach { graphClass ->
            try {
                logger.info("Processing graph: ${graphClass.simpleName.asString()}")
                processGraphClass(graphClass, codeGenerator, logger)
            } catch (e: Exception) {
                logger.error("Error processing ${graphClass.simpleName.asString()}: ${e.message}", graphClass)
            }
        }
        
        return emptyList()
    }
    
    private fun processGraphClass(
        graphClass: KSClassDeclaration,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        val graphInfo = GraphInfoExtractor.extract(graphClass, logger)
        
        // Generate graph builder extension
        GraphBuilderGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate destination extensions
        DestinationExtensionsGenerator.generate(graphInfo, codeGenerator, logger)
        
        // Generate route constants
        RouteConstantsGenerator.generate(graphInfo, codeGenerator, logger)
    }
}

/**
 * Provider for the KSP processor.
 */
class QuoVadisSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return QuoVadisSymbolProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
    }
}
```

**Rationale:**
- Entry point for KSP processing
- Finds all @Graph annotated classes
- Delegates to specialized generators
- Proper error handling and logging

---

### 3.2 Create Data Models

#### File: `GraphInfo.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted information about a navigation graph.
 */
data class GraphInfo(
    val graphClass: KSClassDeclaration,
    val graphName: String,
    val packageName: String,
    val className: String,
    val destinations: List<DestinationInfo>
)

/**
 * Information about a single destination in a graph.
 */
data class DestinationInfo(
    val destinationClass: KSClassDeclaration,
    val name: String,
    val route: String,
    val isObject: Boolean,
    val isDataClass: Boolean,
    val argumentType: String?
)
```

**Rationale:**
- Clean separation of data extraction and code generation
- Immutable data classes for thread safety
- Complete metadata capture

---

### 3.3 Create Graph Info Extractor

#### File: `GraphInfoExtractor.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.annotations.*

/**
 * Extracts navigation graph information from annotated classes.
 */
object GraphInfoExtractor {
    
    fun extract(graphClass: KSClassDeclaration, logger: KSPLogger): GraphInfo {
        val graphAnnotation = graphClass.annotations
            .first { it.shortName.asString() == "Graph" }
        
        val graphName = graphAnnotation.arguments
            .first { it.name?.asString() == "name" }
            .value as String
        
        val packageName = graphClass.packageName.asString()
        val className = graphClass.simpleName.asString()
        
        // Validate sealed class
        if (graphClass.modifiers.contains(Modifier.SEALED).not()) {
            logger.error("@Graph can only be applied to sealed classes", graphClass)
            throw IllegalStateException("Graph class must be sealed")
        }
        
        // Extract destinations from sealed subclasses
        val destinations = extractDestinations(graphClass, logger)
        
        return GraphInfo(
            graphClass = graphClass,
            graphName = graphName,
            packageName = packageName,
            className = className,
            destinations = destinations
        )
    }
    
    private fun extractDestinations(
        graphClass: KSClassDeclaration,
        logger: KSPLogger
    ): List<DestinationInfo> {
        return graphClass.getSealedSubclasses().map { destinationClass ->
            extractDestinationInfo(destinationClass, logger)
        }.toList()
    }
    
    private fun extractDestinationInfo(
        destinationClass: KSClassDeclaration,
        logger: KSPLogger
    ): DestinationInfo {
        // Extract @Route
        val routeAnnotation = destinationClass.annotations
            .firstOrNull { it.shortName.asString() == "Route" }
        
        if (routeAnnotation == null) {
            logger.error("Destination must have @Route annotation", destinationClass)
            throw IllegalStateException("Missing @Route")
        }
        
        val route = routeAnnotation.arguments
            .first { it.name?.asString() == "path" }
            .value as String
        
        // Extract @Argument if present
        val argumentAnnotation = destinationClass.annotations
            .firstOrNull { it.shortName.asString() == "Argument" }
        
        val argumentType = argumentAnnotation?.let {
            val dataClassArg = it.arguments.first { arg -> 
                arg.name?.asString() == "dataClass" 
            }
            val typeRef = dataClassArg.value as? KSType
            typeRef?.declaration?.qualifiedName?.asString()
        }
        
        return DestinationInfo(
            destinationClass = destinationClass,
            name = destinationClass.simpleName.asString(),
            route = route,
            isObject = destinationClass.classKind == ClassKind.OBJECT,
            isDataClass = destinationClass.modifiers.contains(Modifier.DATA),
            argumentType = argumentType
        )
    }
}
```

**Rationale:**
- Validates all annotation requirements
- Extracts complete metadata
- Clear error messages for common mistakes
- Handles optional annotations gracefully

---

### 3.4 Create Route Constants Generator

#### File: `RouteConstantsGenerator.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates route constants for compile-time safety.
 */
object RouteConstantsGenerator {
    
    fun generate(graphInfo: GraphInfo, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val fileName = "${graphInfo.className}Routes"
        
        val fileSpec = FileSpec.builder(graphInfo.packageName, fileName)
            .addComment("Generated by Quo Vadis KSP. Do not edit manually.")
            .apply {
                // Generate object with route constants
                val routesObject = TypeSpec.objectBuilder("${graphInfo.className}Routes")
                    .addKdoc(
                        """
                        Route constants for ${graphInfo.className}.
                        
                        Generated for compile-time safety and IDE autocomplete.
                        """.trimIndent()
                    )
                
                graphInfo.destinations.forEach { dest ->
                    routesObject.addProperty(
                        PropertySpec.builder(dest.name.uppercase(), String::class)
                            .addModifiers(KModifier.CONST)
                            .initializer("%S", dest.route)
                            .addKdoc("Route for ${dest.name}: `${dest.route}`")
                            .build()
                    )
                }
                
                addType(routesObject.build())
            }
            .build()
        
        fileSpec.writeTo(codeGenerator, Dependencies(false, graphInfo.graphClass.containingFile!!))
        logger.info("Generated route constants: $fileName")
    }
}
```

**Rationale:**
- Compile-time constant routes
- IDE autocomplete support
- Prevents typos in route strings
- Documentation for each route

---

### 3.5 Create Destination Extensions Generator

#### File: `DestinationExtensionsGenerator.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates extension functions for destinations.
 */
object DestinationExtensionsGenerator {
    
    fun generate(graphInfo: GraphInfo, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val fileName = "${graphInfo.className}Extensions"
        
        val fileSpec = FileSpec.builder(graphInfo.packageName, fileName)
            .addComment("Generated by Quo Vadis KSP. Do not edit manually.")
            .apply {
                graphInfo.destinations.forEach { dest ->
                    generateDestinationExtensions(dest, graphInfo)
                }
            }
            .build()
        
        fileSpec.writeTo(codeGenerator, Dependencies(false, graphInfo.graphClass.containingFile!!))
        logger.info("Generated destination extensions: $fileName")
    }
    
    private fun FileSpec.Builder.generateDestinationExtensions(
        dest: DestinationInfo,
        graphInfo: GraphInfo
    ) {
        val destinationType = ClassName(graphInfo.packageName, graphInfo.className, dest.name)
        
        // Generate route property override
        addProperty(
            PropertySpec.builder("route", String::class)
                .receiver(destinationType)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return %S", dest.route)
                        .build()
                )
                .addKdoc("Auto-generated route for ${dest.name}")
                .build()
        )
        
        // Generate data property for TypedDestination
        if (dest.argumentType != null) {
            val argType = ClassName.bestGuess(dest.argumentType)
            
            addProperty(
                PropertySpec.builder("data", argType.copy(nullable = true))
                    .receiver(destinationType)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return this.data as? %T", argType)
                            .build()
                    )
                    .addKdoc("Auto-generated typed data accessor")
                    .build()
            )
        }
    }
}
```

**Rationale:**
- Eliminates manual route property implementation
- Type-safe data accessors for arguments
- Clean extension-based API

---

### 3.6 Create Graph Builder Generator

#### File: `GraphBuilderGenerator.kt`

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates simplified graph builder functions.
 */
object GraphBuilderGenerator {
    
    fun generate(graphInfo: GraphInfo, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val fileName = "${graphInfo.className}GraphBuilder"
        
        val fileSpec = FileSpec.builder(graphInfo.packageName, fileName)
            .addComment("Generated by Quo Vadis KSP. Do not edit manually.")
            .addImport("com.jermey.quo.vadis.core.navigation.core", "navigationGraph")
            .addImport("com.jermey.quo.vadis.core.navigation.core", "NavigationGraph")
            .apply {
                generateGraphBuilderFunction(graphInfo)
            }
            .build()
        
        fileSpec.writeTo(codeGenerator, Dependencies(false, graphInfo.graphClass.containingFile!!))
        logger.info("Generated graph builder: $fileName")
    }
    
    private fun FileSpec.Builder.generateGraphBuilderFunction(graphInfo: GraphInfo) {
        val graphClassName = ClassName(graphInfo.packageName, graphInfo.className)
        val builderLambdaType = LambdaTypeName.get(
            receiver = ClassName("com.jermey.quo.vadis.core.navigation.core", "NavigationGraphBuilder"),
            returnType = UNIT
        )
        
        addFunction(
            FunSpec.builder("${graphInfo.className.replaceFirstChar { it.lowercase() }}Graph")
                .addParameter("builder", builderLambdaType)
                .returns(ClassName("com.jermey.quo.vadis.core.navigation.core", "NavigationGraph"))
                .addKdoc(
                    """
                    Creates a navigation graph for ${graphInfo.className}.
                    
                    Auto-generated from @Graph("${graphInfo.graphName}") annotation.
                    
                    @param builder DSL lambda for configuring destinations
                    @return Configured NavigationGraph
                    """.trimIndent()
                )
                .addStatement("return navigationGraph(%S, builder)", graphInfo.graphName)
                .build()
        )
    }
}
```

**Rationale:**
- Auto-generates graph function with correct name
- Simple wrapper around existing navigationGraph DSL
- Maintains full DSL flexibility
- Clear documentation

---

### 3.7 Register KSP Processor

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-ksp/src/main/resources/META-INF/services/`

#### File: `com.google.devtools.ksp.processing.SymbolProcessorProvider`

```
com.jermey.quo.vadis.ksp.QuoVadisSymbolProcessorProvider
```

**Rationale:** Required for KSP to discover the processor via Java ServiceLoader mechanism.

---

## Phase 4: Testing and Validation

### 4.1 Create Test Destinations

**Location:** `/Users/jermey/Projects/NavPlayground/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/TestAnnotatedDestinations.kt`

```kotlin
package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlinx.serialization.Serializable

@Graph("test_graph")
sealed class TestDestination : Destination {
    
    @Route("test/home")
    object Home : TestDestination()
    
    @Route("test/details")
    @Argument(TestDetailsData::class)
    data class Details(override val data: TestDetailsData) : TestDestination()
}

@Serializable
data class TestDetailsData(val id: String, val title: String)
```

**Rationale:** 
- Simple test case covering all features
- Validates processor with real usage
- Includes serializable data class

---

### 4.2 Validation Steps

1. **Build Project**
   ```bash
   ./gradlew clean build
   ```

2. **Verify Generated Files**
   ```bash
   ls composeApp/build/generated/ksp/metadata/commonMain/kotlin/
   # Expected files:
   # - TestDestinationRoutes.kt
   # - TestDestinationExtensions.kt
   # - TestDestinationGraphBuilder.kt
   ```

3. **Check Generated Content**
   - Routes object with constants
   - Extension properties (route, data, transition)
   - Graph builder function with auto-configured start

4. **Verify Compilation**
   ```bash
   ./gradlew :composeApp:assembleDebug
   ```

5. **Test API Usage**
   ```kotlin
   // Generated function should be available
   val graph = testDestinationGraph {
       startDestination(TestDestination.Home)  // Must specify start destination
       
       destination(TestDestination.Home) { _, nav ->
           HomeScreen(nav)
       }
       
       typedDestination(TestDestination.Details.ROUTE) { data, nav ->
           DetailsScreen(data.id, data.title, nav)
       }
   }
   ```

---

## Phase 5: Update Existing Code

### 5.1 Migrate NewDestinations.kt

**Action:** Replace placeholder code with actual implementation

**Changes:**
```kotlin
package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlinx.serialization.Serializable

@Graph("master_detail")
sealed class MasterDetailDestination : Destination {
    
    @Route("master_detail/list")
    object List : MasterDetailDestination()

    @Route("master_detail/detail")
    @Argument(DetailData::class)
    data class Detail(override val data: DetailData) : MasterDetailDestination()
}

@Serializable
data class DetailData(val itemId: String)

// Usage with generated helper:
fun masterDetailGraph() = masterDetailDestinationGraph {
    startDestination(MasterDetailDestination.List)  // Must specify start destination
    
    destinationWithScopes(MasterDetailDestination.List) { _, navigator, shared, animated ->
        MasterListScreen(
            onItemClick = { itemId ->
                navigator.navigate(
                    MasterDetailDestination.Detail(DetailData(itemId))
                )
            },
            onBack = { navigator.navigateBack() }
        )
    }

    typedDestination(MasterDetailDestination.Detail::class) { data, navigator ->
        DetailScreen(
            itemId = data.itemId,
            onBack = { navigator.navigateBack() },
            onNavigateToRelated = { relatedId ->
                navigator.navigate(
                    MasterDetailDestination.Detail(DetailData(relatedId))
                )
            }
        )
    }
}
```

**Rationale:**
- Demonstrates real usage with annotations
- Shows generated helper function
- Maintains explicit control over start destination and transitions
- Removes placeholder comment

---

### 5.2 Optional: Migrate Other Destination Classes

**Action:** Gradually annotate existing destination hierarchies

**Files to Consider:**
- `Destinations.kt` - Main demo destinations
  - `MainDestination` → `@Graph("main")`
  - `TabsDestination` → `@Graph("tabs")`
  - `ProcessDestination` → `@Graph("process")`

**Strategy:**
- One graph at a time
- Keep old code until verified
- Use feature flags if needed
- Update corresponding graph builders

---

## Phase 6: Documentation and Polish

### 6.1 Create KSP Usage Guide

**Location:** `/Users/jermey/Projects/NavPlayground/quo-vadis-core/docs/KSP_CODE_GENERATION.md`

**Content Outline:**
```markdown
# KSP Code Generation Guide

## Overview
Quo Vadis supports automatic code generation via Kotlin Symbol Processing (KSP)...

## Setup
1. Add KSP plugin to your build.gradle.kts
2. Add dependencies...

## Annotations Reference
### @Graph
### @Route
### @Argument

## Generated Code
### Route Constants
### Extension Properties
### Graph Builder Functions

## Examples
### Simple Graph
### Typed Destinations
### Nested Graphs

## Migration Guide
### From Manual DSL to Annotations

## Troubleshooting
### Common Errors
### Build Issues
```

---

### 6.2 Update Main README

**Location:** `/Users/jermey/Projects/NavPlayground/README.md`

**Action:** Add KSP feature to feature list

**Changes:**
```markdown
## ✨ Key Features

- ✅ **Type-Safe Navigation** - Compile-time safety with no string-based routing
- ✅ **KSP Code Generation** - Automatic boilerplate generation from annotations  // NEW
- ✅ **Multiplatform** - Works on Android, iOS, Desktop, and Web
...
```

---

### 6.3 Add KDoc to Generated Code

**Already included in generators** - ensure all generated code has:
- File header: "Generated by Quo Vadis KSP. Do not edit manually."
- Class/function KDoc with usage examples
- Parameter documentation
- Return value documentation

---

## Phase 7: Build and Verification

### 7.1 Full Build Sequence

```bash
# 1. Clean everything
./gradlew clean
./gradlew --stop
rm -rf .gradle/configuration-cache

# 2. Build annotations module
./gradlew :quo-vadis-annotations:build

# 3. Build KSP processor
./gradlew :quo-vadis-ksp:build

# 4. Build core library (with annotations)
./gradlew :quo-vadis-core:build

# 5. Build demo app (triggers KSP generation)
./gradlew :composeApp:build

# 6. Verify Android build (fastest)
./gradlew :composeApp:assembleDebug

# 7. Run tests
./gradlew test
```

---

### 7.2 Validation Checklist

- [ ] All modules build successfully
- [ ] KSP generates files in `build/generated/ksp/`
- [ ] Generated code compiles without errors
- [ ] IDE recognizes generated code (restart may be needed)
- [ ] Existing navigation still works
- [ ] New annotated destinations work
- [ ] All platforms compile (Android, iOS, JS, Wasm, Desktop)
- [ ] Tests pass
- [ ] No detekt violations
- [ ] Documentation is complete

---

## Detailed Code Generation Examples

### Example: MasterDetailDestination Processing

**Input (annotated):**
```kotlin
@Graph("master_detail")
sealed class MasterDetailDestination : Destination {
    @Route("master_detail/list")
    object List : MasterDetailDestination()

    @Route("master_detail/detail")
    @Argument(DetailData::class)
    data class Detail(val data: DetailData) : MasterDetailDestination()
}
```

**Generated: MasterDetailDestinationRoutes.kt**
```kotlin
// Generated by Quo Vadis KSP. Do not edit manually.
package com.jermey.navplayground.demo.destinations

/**
 * Route constants for MasterDetailDestination.
 * 
 * Generated for compile-time safety and IDE autocomplete.
 */
object MasterDetailDestinationRoutes {
    /** Route for List: `master_detail/list` */
    const val LIST = "master_detail/list"
    
    /** Route for Detail: `master_detail/detail` */
    const val DETAIL = "master_detail/detail"
}
```

**Generated: MasterDetailDestinationExtensions.kt**
```kotlin
// Generated by Quo Vadis KSP. Do not edit manually.
package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/** Auto-generated route for List */
val MasterDetailDestination.List.route: String
    get() = "master_detail/list"

/** Auto-generated route for Detail */
val MasterDetailDestination.Detail.route: String
    get() = "master_detail/detail"

/** Auto-generated typed data accessor */
val MasterDetailDestination.Detail.data: DetailData?
    get() = this.data as? DetailData
```

**Generated: MasterDetailDestinationGraphBuilder.kt**
```kotlin
// Generated by Quo Vadis KSP. Do not edit manually.
package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationGraphBuilder
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

/**
 * Creates a navigation graph for MasterDetailDestination.
 * 
 * Auto-generated from @Graph("master_detail") annotation.
 * 
 * @param builder DSL lambda for configuring destinations
 * @return Configured NavigationGraph
 */
fun masterDetailDestinationGraph(
    builder: NavigationGraphBuilder.() -> Unit
): NavigationGraph = navigationGraph("master_detail", builder)
```

---

## Edge Cases and Considerations

### Multiple Graphs in Same File
- **Supported**: Each @Graph class processed independently
- **Generated files**: Separate files per graph

### Nested Sealed Classes
- **Supported**: KSP processes full sealed hierarchy
- **Example:**
  ```kotlin
  @Graph("nested")
  sealed class Parent : Destination {
      sealed class Child : Parent() {
          @Route("child/a")
          object A : Child()
      }
  }
  ```

### Missing Annotations
- **@Route missing**: Compile error with clear message
- **@Argument optional**: Destinations without arguments are supported

### Type Resolution
- **Data classes must be in scope**: Import statements preserved
- **Generic types**: Not supported in v1, error with message
- **Nullable types**: Supported via nullable data property

### Platform-Specific Code
- **Generated code is commonMain**: Works on all platforms
- **No platform-specific generation**: Future enhancement

### Incremental Compilation
- **KSP supports incremental builds**: Only changed files reprocessed
- **Cache invalidation**: Changing annotation triggers regeneration

---

## Future Enhancements (Out of Scope)

### Phase N+1: Advanced Features
1. **Deep Link Generation**
   - `@DeepLink("app://detail/{id}")`
   - Auto-generate deep link handlers

2. **Navigation Validation**
   - Compile-time graph connectivity checks
   - Detect unreachable destinations

3. **Multi-Module Graphs**
   - `@SubGraph` annotation
   - Cross-module navigation

4. **Transition DSL**
   - Custom transition builders
   - Per-edge transition configuration

5. **Analytics Integration**
   - Auto-generate screen view events
   - Navigation flow tracking

---

## Troubleshooting Guide

### KSP Not Running
**Symptoms:** No files in `build/generated/ksp/`

**Solutions:**
1. Verify KSP plugin applied: `alias(libs.plugins.ksp)`
2. Check processor dependency: `kspCommonMainMetadata(project(":quo-vadis-ksp"))`
3. Clean and rebuild: `./gradlew clean build`
4. Check logs: `./gradlew build --info`

---

### Generated Code Not Found
**Symptoms:** IDE shows red underlines, compile fails

**Solutions:**
1. Invalidate caches: File → Invalidate Caches / Restart
2. Verify source set configuration in build.gradle.kts
3. Check generated file location matches source set
4. Reimport Gradle project

---

### Annotation Not Recognized
**Symptoms:** `@Graph` annotation not found

**Solutions:**
1. Verify annotations module dependency
2. Check import: `import com.jermey.quo.vadis.annotations.*`
3. Rebuild annotations module first
4. Check multiplatform target alignment

---

### Type Resolution Errors
**Symptoms:** Generated code references unknown types

**Solutions:**
1. Ensure data classes are in same package or imported
2. Use fully qualified names in @Argument
3. Check for circular dependencies
4. Verify serialization plugin applied

---

## Performance Considerations

### Build Time Impact
- **First build**: +2-5 seconds (processor setup)
- **Incremental builds**: +0.5-1 second (only changed files)
- **Large projects (100+ destinations)**: +3-8 seconds

### Optimization Strategies
1. **Modularize graphs**: Separate feature modules
2. **Avoid deep nesting**: Keep sealed hierarchies shallow
3. **Use build cache**: Enable Gradle build cache
4. **Parallel builds**: Enable `org.gradle.parallel=true`

### Resource Usage
- **Memory**: ~100MB additional heap during processing
- **CPU**: Processor runs on all cores via Gradle parallelism
- **Disk**: Generated files ~5-10KB per graph

---

## Success Criteria

### Must Have (Phase 1-5)
- ✅ All annotations defined and documented
- ✅ KSP processor processes @Graph classes
- ✅ Route constants generated
- ✅ Extension properties generated
- ✅ Graph builder functions generated
- ✅ All platforms build successfully
- ✅ Generated code is idiomatic Kotlin
- ✅ Clear error messages for invalid usage

### Should Have (Phase 6-7)
- ✅ Comprehensive documentation
- ✅ Usage examples in demo app
- ✅ Migration guide from manual DSL
- ✅ Troubleshooting guide
- ✅ All tests pass

### Nice to Have (Future)
- ⬜ Deep link generation
- ⬜ Compile-time navigation validation
- ⬜ Multi-module graph support
- ⬜ Custom transition DSL
- ⬜ Analytics hooks

---

## Timeline Estimate

- **Phase 1** (Setup): 1-2 hours
- **Phase 2** (Annotations): 30 minutes
- **Phase 3** (Processor): 4-6 hours
- **Phase 4** (Testing): 1-2 hours
- **Phase 5** (Migration): 1-2 hours
- **Phase 6** (Documentation): 2-3 hours
- **Phase 7** (Verification): 1 hour

**Total: 10-16 hours** (1-2 days of focused development)

---

## Dependencies Summary

### New Dependencies
```kotlin
// Version catalog additions
ksp = "2.2.20-1.0.30"
kotlinpoet = "1.18.1"

// quo-vadis-annotations: NONE
// quo-vadis-ksp:
- com.google.devtools.ksp:symbol-processing-api:2.2.20-1.0.30
- com.squareup:kotlinpoet:1.18.1
- com.squareup:kotlinpoet-ksp:1.18.1

// composeApp:
- project(":quo-vadis-annotations")
- ksp plugin applied
```

### Version Compatibility
- KSP 2.2.20: Matches Kotlin 2.2.20
- KotlinPoet 1.18.1: Compatible with Kotlin 2.2.20
- All dependencies: JVM 11+

---

## Risk Mitigation

### Breaking Changes
- **Risk**: Generated API differs from manual DSL
- **Mitigation**: Keep manual DSL working, annotations are additive
- **Rollback**: Remove KSP plugin, delete generated code

### Build Complexity
- **Risk**: KSP adds complexity to build
- **Mitigation**: Clear documentation, troubleshooting guide
- **Fallback**: Annotations optional, manual DSL always works

### Generated Code Quality
- **Risk**: Generated code is unidiomatic or buggy
- **Mitigation**: KotlinPoet ensures idiomatic code, extensive testing
- **Validation**: Code review generated output

### Multiplatform Issues
- **Risk**: Generated code not platform-agnostic
- **Mitigation**: Target commonMain only, no platform-specific APIs
- **Testing**: Build all platforms in CI

---

## Conclusion

This specification provides a complete blueprint for implementing KSP-based code generation in the Quo Vadis navigation library. The implementation:

1. **Reduces Boilerplate**: Generates route constants and extension properties automatically
2. **Maintains Simplicity**: Only 3 core annotations (@Graph, @Route, @Argument)
3. **Preserves Control**: Developers explicitly configure start destinations and transitions
4. **Type-Safe**: Compile-time guarantees with KotlinPoet-generated code
5. **Well-Documented**: Clear guides and examples
6. **Production-Ready**: Error handling, validation, multiplatform support

The phased approach allows for incremental implementation and validation at each step. The simplified annotation set keeps the API easy to understand while still providing significant value through automated code generation.
