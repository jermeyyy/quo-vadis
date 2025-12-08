`n
# KSP-006: Validation and Error Reporting

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-006 |
| **Task Name** | Validation and Error Reporting |
| **Phase** | Phase 4: KSP Processor Rewrite |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | KSP-001 (Annotation Extractors) |
| **Blocked By** | KSP-001 |
| **Blocks** | None |

---

## Overview

This task implements the **validation layer** of the KSP processor—a comprehensive validation system that checks annotation usage correctness and reports clear, actionable error messages. This is critical for developer experience, catching configuration errors at compile time rather than runtime.

### Purpose

The Validation and Error Reporting system:

1. **Validates** all annotation constraints and relationships
2. **Reports** clear, actionable error messages with source locations
3. **Prevents** invalid navigation configurations from compiling
4. **Warns** about potential issues that may cause runtime problems

### Rationale

Compile-time validation provides:

1. **Early error detection**: Catch misconfigurations before runtime crashes
2. **Clear diagnostics**: Error messages point to exact source locations
3. **Fast feedback**: Developers see issues immediately in their IDE
4. **Documentation**: Error messages explain what's wrong and how to fix it

---

## Validation Categories

### 1. Structural Validations

| Validation | Severity | Description |
|------------|----------|-------------|
| Orphan Destination | Error | `@Destination` not inside `@Stack`, `@Tab`, or `@Pane` |
| Missing Container | Error | Sealed class with `@Destination` children but no container annotation |
| Invalid Start Destination | Error | `@Stack(startDestination)` references non-existent destination |
| Invalid Initial Tab | Error | `@Tab(initialTab)` references non-existent tab |
| Empty Container | Error | `@Stack`, `@Tab`, or `@Pane` with no destinations |

### 2. Route Validations

| Validation | Severity | Description |
|------------|----------|-------------|
| Route Parameter Mismatch | Error | Route param `{name}` has no matching constructor parameter |
| Missing Route Parameter | Warning | Constructor param not in route (may be intentional) |
| Duplicate Routes | Error | Same route pattern on multiple destinations |
| Invalid Route Syntax | Error | Malformed route pattern |

### 3. Reference Validations

| Validation | Severity | Description |
|------------|----------|-------------|
| Invalid Root Graph | Error | `@TabItem(rootGraph)` references invalid class |
| Missing Screen Binding | Warning | Destination has no `@Screen` function |
| Duplicate Screen Binding | Error | Multiple `@Screen` for same destination |
| Invalid Destination Reference | Error | `@Screen(destination)` references non-destination class |

### 4. Type Validations

| Validation | Severity | Description |
|------------|----------|-------------|
| Non-Sealed Container | Error | `@Stack`/`@Tab`/`@Pane` on non-sealed class |
| Non-Data Destination | Error | `@Destination` on class that's not data object/class |
| Invalid Pane Role | Error | `@PaneItem` with invalid role enum value |

---

## Implementation

### ValidationEngine

```kotlin
package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.jermey.quo.vadis.ksp.models.*

/**
 * Validates annotation usage and reports errors/warnings.
 *
 * This engine performs comprehensive validation of all navigation
 * annotations and their relationships, providing clear error messages
 * with source locations.
 */
class ValidationEngine(
    private val logger: KSPLogger
) {
    
    private var hasErrors = false
    
    /**
     * Validate all extracted metadata and report issues.
     *
     * @return true if validation passed (no errors), false otherwise
     */
    fun validate(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        screens: List<ScreenInfo>,
        allDestinations: List<DestinationInfo>,
        resolver: Resolver
    ): Boolean {
        hasErrors = false
        
        // Structural validations
        validateOrphanDestinations(allDestinations, stacks, tabs, panes)
        validateContainerStartDestinations(stacks)
        validateTabInitialTabs(tabs)
        validateEmptyContainers(stacks, tabs, panes)
        
        // Route validations
        validateRouteParameters(allDestinations)
        validateDuplicateRoutes(allDestinations)
        
        // Reference validations
        validateRootGraphReferences(tabs, panes, resolver)
        validateScreenBindings(screens, allDestinations)
        
        // Type validations
        validateContainerTypes(stacks, tabs, panes)
        validateDestinationTypes(allDestinations)
        
        return !hasErrors
    }
    
    // =========================================================================
    // Structural Validations
    // =========================================================================
    
    private fun validateOrphanDestinations(
        destinations: List<DestinationInfo>,
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        val containedDestinations = mutableSetOf<String>()
        
        stacks.forEach { stack ->
            stack.destinations.forEach { containedDestinations.add(it.qualifiedName) }
        }
        tabs.forEach { tab ->
            tab.tabs.forEach { containedDestinations.add(it.destination.qualifiedName) }
        }
        panes.forEach { pane ->
            pane.panes.forEach { containedDestinations.add(it.destination.qualifiedName) }
        }
        
        destinations.forEach { destination ->
            if (destination.qualifiedName !in containedDestinations) {
                reportError(
                    destination.classDeclaration,
                    "@Destination on \"${destination.className}\" - Must be inside a sealed class annotated with @Stack, @Tab, or @Pane"
                )
            }
        }
    }
    
    private fun validateContainerStartDestinations(stacks: List<StackInfo>) {
        stacks.forEach { stack ->
            if (stack.resolvedStartDestination == null) {
                reportError(
                    stack.classDeclaration,
                    "@Stack(startDestination = \"${stack.startDestination}\") - No destination named \"${stack.startDestination}\" found in ${stack.className}. " +
                    "Available destinations: ${stack.destinations.map { it.className }}"
                )
            }
        }
    }
    
    private fun validateTabInitialTabs(tabs: List<TabInfo>) {
        tabs.forEach { tab ->
            val initialTabExists = tab.tabs.any { it.destination.className == tab.initialTab }
            if (!initialTabExists) {
                reportError(
                    tab.classDeclaration,
                    "@Tab(initialTab = \"${tab.initialTab}\") - No tab named \"${tab.initialTab}\" found in ${tab.className}. " +
                    "Available tabs: ${tab.tabs.map { it.destination.className }}"
                )
            }
        }
    }
    
    private fun validateEmptyContainers(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        stacks.filter { it.destinations.isEmpty() }.forEach { stack ->
            reportError(
                stack.classDeclaration,
                "@Stack on \"${stack.className}\" - Stack must contain at least one @Destination"
            )
        }
        
        tabs.filter { it.tabs.isEmpty() }.forEach { tab ->
            reportError(
                tab.classDeclaration,
                "@Tab on \"${tab.className}\" - Tab container must contain at least one @TabItem"
            )
        }
        
        panes.filter { it.panes.isEmpty() }.forEach { pane ->
            reportError(
                pane.classDeclaration,
                "@Pane on \"${pane.className}\" - Pane container must contain at least one @PaneItem"
            )
        }
    }
    
    // =========================================================================
    // Route Validations
    // =========================================================================
    
    private fun validateRouteParameters(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            if (destination.route != null) {
                val constructorParamNames = destination.constructorParams.map { it.name }.toSet()
                
                // Check route params have matching constructor params
                destination.routeParams.forEach { routeParam ->
                    if (routeParam !in constructorParamNames) {
                        reportError(
                            destination.classDeclaration,
                            "@Destination(route = \"${destination.route}\") on ${destination.className} - " +
                            "Route param \"$routeParam\" has no matching constructor parameter. " +
                            "Available params: $constructorParamNames"
                        )
                    }
                }
                
                // Warn about constructor params not in route (only for data classes)
                if (destination.isDataClass) {
                    val routeParamSet = destination.routeParams.toSet()
                    constructorParamNames.filter { it !in routeParamSet }.forEach { missingParam ->
                        reportWarning(
                            destination.classDeclaration,
                            "Constructor param \"$missingParam\" in ${destination.className} is not in route pattern. " +
                            "This param won't be available via deep linking."
                        )
                    }
                }
            }
        }
    }
    
    private fun validateDuplicateRoutes(destinations: List<DestinationInfo>) {
        val routeToDestinations = destinations
            .filter { it.route != null }
            .groupBy { it.route }
        
        routeToDestinations.filter { it.value.size > 1 }.forEach { (route, dests) ->
            val destNames = dests.map { it.qualifiedName }
            dests.forEach { dest ->
                reportError(
                    dest.classDeclaration,
                    "Duplicate route \"$route\" found on: ${destNames.joinToString(", ")}. " +
                    "Each destination must have a unique route pattern."
                )
            }
        }
    }
    
    // =========================================================================
    // Reference Validations
    // =========================================================================
    
    private fun validateRootGraphReferences(
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        resolver: Resolver
    ) {
        tabs.forEach { tab ->
            tab.tabs.forEach { tabItem ->
                validateRootGraphClass(tabItem.rootGraphClass, tabItem.destination.classDeclaration)
            }
        }
        
        panes.forEach { pane ->
            pane.panes.forEach { paneItem ->
                validateRootGraphClass(paneItem.rootGraphClass, paneItem.destination.classDeclaration)
            }
        }
    }
    
    private fun validateRootGraphClass(
        rootGraphClass: KSClassDeclaration,
        usageSite: KSClassDeclaration
    ) {
        // Check if rootGraph has @Stack annotation
        val hasStackAnnotation = rootGraphClass.annotations.any { 
            it.shortName.asString() == "Stack" 
        }
        
        if (!hasStackAnnotation) {
            reportError(
                usageSite,
                "rootGraph = ${rootGraphClass.simpleName.asString()}::class - " +
                "Referenced class must be annotated with @Stack"
            )
        }
    }
    
    private fun validateScreenBindings(
        screens: List<ScreenInfo>,
        destinations: List<DestinationInfo>
    ) {
        val destinationQualifiedNames = destinations.map { it.qualifiedName }.toSet()
        val screenDestinations = mutableMapOf<String, MutableList<ScreenInfo>>()
        
        // Check each @Screen references a valid destination
        screens.forEach { screen ->
            val destName = screen.destinationClass.qualifiedName?.asString() ?: return@forEach
            
            if (destName !in destinationQualifiedNames) {
                reportError(
                    screen.functionDeclaration,
                    "@Screen(${screen.destinationClass.simpleName.asString()}::class) - " +
                    "Referenced class is not annotated with @Destination"
                )
            }
            
            screenDestinations.getOrPut(destName) { mutableListOf() }.add(screen)
        }
        
        // Check for duplicate screen bindings
        screenDestinations.filter { it.value.size > 1 }.forEach { (destName, screenList) ->
            val screenNames = screenList.map { it.functionName }
            screenList.forEach { screen ->
                reportError(
                    screen.functionDeclaration,
                    "Multiple @Screen bindings for ${screen.destinationClass.simpleName.asString()}: " +
                    "${screenNames.joinToString(", ")}. Each destination can only have one screen."
                )
            }
        }
        
        // Warn about destinations without screens
        val boundDestinations = screenDestinations.keys
        destinations.forEach { destination ->
            if (destination.qualifiedName !in boundDestinations) {
                reportWarning(
                    destination.classDeclaration,
                    "No @Screen found for ${destination.className} - " +
                    "This destination will have no content. Add a @Screen function to render this destination."
                )
            }
        }
    }
    
    // =========================================================================
    // Type Validations
    // =========================================================================
    
    private fun validateContainerTypes(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        stacks.forEach { stack ->
            if (!stack.classDeclaration.modifiers.any { it.name == "SEALED" }) {
                reportError(
                    stack.classDeclaration,
                    "@Stack on \"${stack.className}\" - Must be applied to a sealed class"
                )
            }
        }
        
        tabs.forEach { tab ->
            if (!tab.classDeclaration.modifiers.any { it.name == "SEALED" }) {
                reportError(
                    tab.classDeclaration,
                    "@Tab on \"${tab.className}\" - Must be applied to a sealed class"
                )
            }
        }
        
        panes.forEach { pane ->
            if (!pane.classDeclaration.modifiers.any { it.name == "SEALED" }) {
                reportError(
                    pane.classDeclaration,
                    "@Pane on \"${pane.className}\" - Must be applied to a sealed class"
                )
            }
        }
    }
    
    private fun validateDestinationTypes(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            if (!destination.isDataObject && !destination.isDataClass) {
                reportError(
                    destination.classDeclaration,
                    "@Destination on \"${destination.className}\" - " +
                    "Must be applied to a data object or data class"
                )
            }
        }
    }
    
    // =========================================================================
    // Error Reporting
    // =========================================================================
    
    private fun reportError(node: KSClassDeclaration, message: String) {
        hasErrors = true
        logger.error("error: $message", node)
    }
    
    private fun reportError(node: KSFunctionDeclaration, message: String) {
        hasErrors = true
        logger.error("error: $message", node)
    }
    
    private fun reportWarning(node: KSClassDeclaration, message: String) {
        logger.warn("warning: $message", node)
    }
}
```

---

## Error Message Examples

### Structural Errors

```
error: @Destination on "SomeScreen" - Must be inside a sealed class annotated with @Stack, @Tab, or @Pane
       --> com/example/SomeScreen.kt:15

error: @Stack(startDestination = "Unknown") - No destination named "Unknown" found in HomeDestination. Available destinations: [Feed, Detail, Search]
       --> com/example/HomeDestination.kt:8

error: @Tab(initialTab = "Settings") - No tab named "Settings" found in MainTabs. Available tabs: [Home, Profile]
       --> com/example/MainTabs.kt:5

error: @Stack on "EmptyStack" - Stack must contain at least one @Destination
       --> com/example/EmptyStack.kt:3
```

### Route Errors

```
error: @Destination(route = "detail/{id}/{name}") on Detail - Route param "name" has no matching constructor parameter. Available params: [id]
       --> com/example/HomeDestination.kt:18

error: Duplicate route "home/detail" found on: com.example.HomeDestination.Detail, com.example.ProfileDestination.Detail. Each destination must have a unique route pattern.
       --> com/example/HomeDestination.kt:18
       --> com/example/ProfileDestination.kt:12
```

### Reference Errors

```
error: rootGraph = SomeClass::class - Referenced class must be annotated with @Stack
       --> com/example/MainTabs.kt:12

error: @Screen(NonDestination::class) - Referenced class is not annotated with @Destination
       --> com/example/screens/SomeScreen.kt:25

error: Multiple @Screen bindings for HomeDestination.Detail: DetailScreen, DetailScreenV2. Each destination can only have one screen.
       --> com/example/screens/DetailScreen.kt:10
       --> com/example/screens/DetailScreenV2.kt:8
```

### Warnings

```
warning: Constructor param "filter" in Search is not in route pattern. This param won't be available via deep linking.
         --> com/example/HomeDestination.kt:25

warning: No @Screen found for HomeDestination.Search - This destination will have no content. Add a @Screen function to render this destination.
         --> com/example/HomeDestination.kt:22
```

---

## Integration with Processor

### QuoVadisSymbolProcessor

```kotlin
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    
    private val validationEngine = ValidationEngine(logger)
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Extract all annotation metadata
        val stacks = stackExtractor.extractAll(resolver)
        val tabs = tabExtractor.extractAll(resolver)
        val panes = paneExtractor.extractAll(resolver)
        val screens = screenExtractor.extractAll(resolver)
        val allDestinations = collectAllDestinations(stacks, tabs, panes)
        
        // Validate before generation
        val isValid = validationEngine.validate(
            stacks = stacks,
            tabs = tabs,
            panes = panes,
            screens = screens,
            allDestinations = allDestinations,
            resolver = resolver
        )
        
        if (!isValid) {
            // Stop processing - errors have been reported
            return emptyList()
        }
        
        // Proceed with code generation
        generateCode(stacks, tabs, panes, screens)
        
        return emptyList()
    }
}
```

---

## Implementation Steps

### Step 1: Create ValidationEngine Class (Day 1 - 4 hours)

Create `ValidationEngine.kt` with:
- Constructor accepting `KSPLogger`
- Main `validate()` function signature
- Error/warning reporting helpers

### Step 2: Implement Structural Validations (Day 1 - 4 hours)

Implement validations:
- Orphan destination detection
- Start destination resolution
- Initial tab verification
- Empty container detection

### Step 3: Implement Route Validations (Day 2 - 4 hours)

Implement validations:
- Route parameter matching
- Constructor parameter coverage
- Duplicate route detection

### Step 4: Implement Reference Validations (Day 2 - 4 hours)

Implement validations:
- Root graph class verification
- Screen binding validation
- Duplicate screen detection
- Missing screen warnings

### Step 5: Implement Type Validations (Day 3 - 2 hours)

Implement validations:
- Sealed class requirement
- Data object/class requirement

### Step 6: Integration and Testing (Day 3 - 4 hours)

- Wire ValidationEngine into processor
- Test all error scenarios
- Verify error message clarity
- Test warning suppression (optional)

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../validation/ValidationEngine.kt` | Create | Main validation engine |
| `quo-vadis-ksp/src/main/kotlin/.../QuoVadisSymbolProcessor.kt` | Modify | Wire validation into processing |

---

## Acceptance Criteria

- [ ] `ValidationEngine` class implemented with comprehensive validations
- [ ] Orphan `@Destination` classes detected and reported
- [ ] Invalid `startDestination`/`initialTab` references reported with suggestions
- [ ] Route parameter mismatches detected with available params listed
- [ ] Duplicate routes detected across all containers
- [ ] Invalid `rootGraph` references detected
- [ ] Missing `@Screen` bindings reported as warnings
- [ ] Duplicate `@Screen` bindings reported as errors
- [ ] Non-sealed containers reported as errors
- [ ] Non-data destinations reported as errors
- [ ] Error messages include source location
- [ ] Error messages include actionable suggestions
- [ ] Processor stops generation on errors
- [ ] Unit tests for each validation scenario
- [ ] Integration test with invalid annotation configurations

---

## Configuration Options

```kotlin
// In build.gradle.kts
ksp {
    arg("quoVadis.strictMode", "true")           // Treat warnings as errors
    arg("quoVadis.suppressWarnings", "false")    // Suppress all warnings
}
```

---

## Testing Notes

### Unit Test Examples

```kotlin
class ValidationEngineTest {
    
    @Test
    fun `reports error for orphan destination`() {
        val source = """
            @Destination(route = "orphan")
            data object OrphanDest : Destination
        """.trimIndent()
        
        val result = compile(source)
        
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Must be inside a sealed class annotated with @Stack")
    }
    
    @Test
    fun `reports error for invalid start destination`() {
        val source = """
            @Stack(name = "test", startDestination = "NonExistent")
            sealed class TestDestination : Destination {
                @Destination(route = "home")
                data object Home : TestDestination()
            }
        """.trimIndent()
        
        val result = compile(source)
        
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("No destination named \"NonExistent\"")
        assertThat(result.messages).contains("Available destinations: [Home]")
    }
    
    @Test
    fun `reports error for route param mismatch`() {
        val source = """
            @Stack(name = "test", startDestination = "Detail")
            sealed class TestDestination : Destination {
                @Destination(route = "detail/{id}/{name}")
                data class Detail(val id: String) : TestDestination()
            }
        """.trimIndent()
        
        val result = compile(source)
        
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Route param \"name\" has no matching constructor parameter")
    }
    
    @Test
    fun `reports warning for missing screen binding`() {
        val source = """
            @Stack(name = "test", startDestination = "Home")
            sealed class TestDestination : Destination {
                @Destination(route = "home")
                data object Home : TestDestination()
            }
            // No @Screen for Home
        """.trimIndent()
        
        val result = compile(source)
        
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.messages).contains("warning: No @Screen found for Home")
    }
    
    @Test
    fun `reports error for duplicate routes`() {
        val source = """
            @Stack(name = "test", startDestination = "First")
            sealed class TestDestination : Destination {
                @Destination(route = "same/route")
                data object First : TestDestination()
                
                @Destination(route = "same/route")
                data object Second : TestDestination()
            }
        """.trimIndent()
        
        val result = compile(source)
        
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Duplicate route \"same/route\"")
    }
}
```

---

## References

- [INDEX.md](../INDEX.md) - Phase 4 KSP Overview
- [KSP-001](./KSP-001-graph-type-enum.md) - Annotation Extractors (prerequisite)
- [KSP Error Reporting](https://kotlinlang.org/docs/ksp-why-ksp.html#comparison-to-kapt) - KSP Documentation

````
