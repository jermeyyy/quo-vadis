# KSP-003: Update GraphInfoExtractor for GraphType

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-003 |
| **Task Name** | Update GraphInfoExtractor for GraphType |
| **Phase** | Phase 3: KSP Processor Updates |
| **Complexity** | Low-Medium |
| **Estimated Time** | 1-2 days |
| **Dependencies** | KSP-001 |
| **Blocked By** | KSP-001 |
| **Blocks** | KSP-004 |

---

## Overview

This task updates the `GraphInfoExtractor` to parse the new `GraphType` parameter from `@Graph` annotations and adds a `graphType` field to the `GraphInfo` data class. This extracted information is essential for the `NavNodeGenerator` (KSP-004) to generate the correct `NavNode` container type.

### Data Flow

```
@Graph("main", type = GraphType.TAB)  →  GraphInfoExtractor  →  GraphInfo(graphType = TAB)
                                                                      ↓
                                                              NavNodeGenerator
                                                                      ↓
                                                              TabNode structure
```

---

## File Locations

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/GraphInfo.kt
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/GraphInfoExtractor.kt
```

---

## Implementation

### Updated GraphInfo Data Class

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jermey.quo.vadis.annotations.GraphType

/**
 * Extracted information about a navigation graph.
 *
 * Contains all metadata needed for code generation, including:
 * - Class references for the graph and its destinations
 * - Graph configuration (name, type, start destination)
 * - Destination details (routes, arguments)
 *
 * @property graphClass KSP class declaration for the sealed graph class
 * @property graphName Unique identifier from @Graph annotation
 * @property packageName Package containing the graph class
 * @property className Simple name of the graph class
 * @property destinations List of extracted destination information
 * @property startDestinationName Optional explicit start destination name
 * @property graphType The structural type of the graph (STACK, TAB, PANE)
 */
data class GraphInfo(
    val graphClass: KSClassDeclaration,
    val graphName: String,
    val packageName: String,
    val className: String,
    val destinations: List<DestinationInfo>,
    val startDestinationName: String? = null,
    val graphType: GraphType = GraphType.STACK
) {
    /**
     * Resolved start destination.
     *
     * Returns the explicitly specified start destination, or the first destination
     * if none was specified.
     */
    val resolvedStartDestination: DestinationInfo?
        get() = startDestinationName?.let { name ->
            destinations.find { it.name == name }
        } ?: destinations.firstOrNull()

    /**
     * Whether this graph should generate a StackNode structure.
     */
    val isStackGraph: Boolean
        get() = graphType == GraphType.STACK

    /**
     * Whether this graph should generate a TabNode structure.
     */
    val isTabGraph: Boolean
        get() = graphType == GraphType.TAB

    /**
     * Whether this graph should generate a PaneNode structure.
     */
    val isPaneGraph: Boolean
        get() = graphType == GraphType.PANE
}

/**
 * Information about a single destination in a graph.
 *
 * @property destinationClass KSP class declaration for the destination
 * @property name Simple name of the destination class
 * @property route Route path from @Route annotation
 * @property isObject Whether the destination is a data object
 * @property isDataClass Whether the destination is a data class
 * @property argumentType Qualified name of the argument data class, if any
 */
data class DestinationInfo(
    val destinationClass: KSClassDeclaration,
    val name: String,
    val route: String,
    val isObject: Boolean,
    val isDataClass: Boolean,
    val argumentType: String?
) {
    /**
     * Whether this destination has typed arguments.
     */
    val hasArguments: Boolean
        get() = argumentType != null

    /**
     * The short name of the argument type (without package).
     */
    val argumentTypeName: String?
        get() = argumentType?.substringAfterLast('.')
}
```

### Updated GraphInfoExtractor

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.annotations.GraphType

/**
 * Extracts navigation graph information from annotated classes.
 *
 * Parses @Graph annotations and their nested @Route/@Argument annotations
 * to build a complete [GraphInfo] model for code generation.
 *
 * ## Extraction Process
 *
 * 1. Validate the annotated class is a sealed class
 * 2. Extract @Graph parameters (name, startDestination, type)
 * 3. Iterate sealed subclasses to extract destination info
 * 4. Build and return [GraphInfo] with all extracted data
 */
object GraphInfoExtractor {

    /**
     * Extract graph information from an @Graph annotated class.
     *
     * @param graphClass The KSP class declaration to extract from
     * @param logger KSP logger for warnings and errors
     * @return Extracted [GraphInfo] containing all graph metadata
     * @throws IllegalStateException if the class is not a valid graph definition
     */
    fun extract(graphClass: KSClassDeclaration, logger: KSPLogger): GraphInfo {
        val graphAnnotation = graphClass.annotations
            .first { it.shortName.asString() == "Graph" }

        // Extract annotation parameters
        val graphName = graphAnnotation.arguments
            .first { it.name?.asString() == "name" }
            .value as String

        val startDestinationName = graphAnnotation.arguments
            .firstOrNull { it.name?.asString() == "startDestination" }
            ?.value as? String

        // Extract GraphType parameter (NEW)
        val graphType = extractGraphType(graphAnnotation, logger)

        val packageName = graphClass.packageName.asString()
        val className = graphClass.simpleName.asString()

        // Validate sealed class
        if (!graphClass.modifiers.contains(Modifier.SEALED)) {
            logger.error("@Graph can only be applied to sealed classes", graphClass)
            error("Graph class must be sealed")
        }

        // Extract destinations from sealed subclasses
        val destinations = extractDestinations(graphClass, logger)

        // Validate destinations for specific graph types
        validateGraphTypeRequirements(graphType, destinations, graphClass, logger)

        return GraphInfo(
            graphClass = graphClass,
            graphName = graphName,
            packageName = packageName,
            className = className,
            destinations = destinations,
            startDestinationName = startDestinationName?.takeIf { it.isNotEmpty() },
            graphType = graphType
        )
    }

    /**
     * Extract GraphType from @Graph annotation.
     *
     * Falls back to GraphType.STACK if not specified (backward compatibility).
     */
    private fun extractGraphType(
        graphAnnotation: com.google.devtools.ksp.symbol.KSAnnotation,
        logger: KSPLogger
    ): GraphType {
        val typeArg = graphAnnotation.arguments
            .firstOrNull { it.name?.asString() == "type" }

        if (typeArg == null) {
            // Default to STACK for backward compatibility
            return GraphType.STACK
        }

        // The value is a KSType representing the enum entry
        val typeValue = typeArg.value

        return when {
            typeValue is KSType -> {
                // Extract enum entry name from the type
                val enumName = typeValue.declaration.simpleName.asString()
                try {
                    GraphType.valueOf(enumName)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unknown GraphType: $enumName, defaulting to STACK")
                    GraphType.STACK
                }
            }
            typeValue is String -> {
                // Fallback for string representation
                try {
                    GraphType.valueOf(typeValue)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unknown GraphType: $typeValue, defaulting to STACK")
                    GraphType.STACK
                }
            }
            else -> {
                logger.warn("Could not parse GraphType, defaulting to STACK")
                GraphType.STACK
            }
        }
    }

    /**
     * Validate that the graph meets requirements for its declared type.
     */
    private fun validateGraphTypeRequirements(
        graphType: GraphType,
        destinations: List<DestinationInfo>,
        graphClass: KSClassDeclaration,
        logger: KSPLogger
    ) {
        when (graphType) {
            GraphType.STACK -> {
                // Stack graphs should have at least one destination
                if (destinations.isEmpty()) {
                    logger.warn(
                        "Stack graph has no destinations, generated code may be incomplete",
                        graphClass
                    )
                }
            }

            GraphType.TAB -> {
                // Tab graphs typically need multiple destinations (one per tab)
                if (destinations.size < 2) {
                    logger.warn(
                        "Tab graph has fewer than 2 destinations. " +
                            "Consider using STACK for single-destination graphs.",
                        graphClass
                    )
                }
            }

            GraphType.PANE -> {
                // Pane graphs should have 2+ panes for meaningful split view
                if (destinations.size < 2) {
                    logger.warn(
                        "Pane graph has fewer than 2 destinations. " +
                            "Consider defining at least 2 panes for adaptive layouts.",
                        graphClass
                    )
                }
            }
        }
    }

    private fun extractDestinations(
        graphClass: KSClassDeclaration,
        logger: KSPLogger
    ): List<DestinationInfo> {
        return graphClass.getSealedSubclasses().mapNotNull { destinationClass ->
            extractDestinationInfo(destinationClass, logger)
        }.toList()
    }

    private fun extractDestinationInfo(
        destinationClass: KSClassDeclaration,
        logger: KSPLogger
    ): DestinationInfo? {
        // Extract @Route
        val routeAnnotation = destinationClass.annotations
            .firstOrNull { it.shortName.asString() == "Route" }

        if (routeAnnotation == null) {
            val destName = destinationClass.simpleName.asString()
            logger.warn(
                "Destination $destName has no @Route annotation, skipping",
                destinationClass
            )
            return null
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

---

## Implementation Steps

### Step 1: Update GraphInfo Data Class (30 minutes)

Add the `graphType` field and helper properties:

```kotlin
data class GraphInfo(
    // ... existing fields ...
    val graphType: GraphType = GraphType.STACK
) {
    val isStackGraph: Boolean get() = graphType == GraphType.STACK
    val isTabGraph: Boolean get() = graphType == GraphType.TAB
    val isPaneGraph: Boolean get() = graphType == GraphType.PANE
}
```

### Step 2: Add Import for GraphType (5 minutes)

```kotlin
import com.jermey.quo.vadis.annotations.GraphType
```

### Step 3: Implement extractGraphType() (1 hour)

Create the private function to parse the enum from annotation:

```kotlin
private fun extractGraphType(
    graphAnnotation: KSAnnotation,
    logger: KSPLogger
): GraphType {
    // Implementation as shown above
}
```

### Step 4: Add Validation (30 minutes)

Implement `validateGraphTypeRequirements()` to warn about potentially misconfigured graphs.

### Step 5: Update extract() Function (30 minutes)

Call the new extraction and validation functions:

```kotlin
fun extract(graphClass: KSClassDeclaration, logger: KSPLogger): GraphInfo {
    // ... existing code ...
    val graphType = extractGraphType(graphAnnotation, logger)
    // ... existing code ...
    validateGraphTypeRequirements(graphType, destinations, graphClass, logger)
    
    return GraphInfo(
        // ... existing fields ...
        graphType = graphType
    )
}
```

### Step 6: Update Existing Generators (1-2 hours)

Ensure existing generators handle the new field gracefully:
- `GraphBuilderGenerator` - may need to check `graphType`
- `GraphGenerator` - should ignore `graphType` for now

### Step 7: Unit Tests (1 hour)

Add tests for the new extraction logic.

---

## Edge Cases

### Default Value Handling

```kotlin
// No type specified - should default to STACK
@Graph("main")
sealed class MainDest : Destination { /* ... */ }

// GraphInfoExtractor should return:
// GraphInfo(graphType = GraphType.STACK)
```

### Invalid Enum Value (Future-Proofing)

```kotlin
// If annotation has unknown enum value (shouldn't happen but handle gracefully)
// Log warning and default to STACK
```

### Empty Destinations

```kotlin
// Empty graph with TAB type
@Graph("tabs", type = GraphType.TAB)
sealed class EmptyTabs : Destination
// Should log warning about missing tabs
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/.../GraphInfo.kt` | Modify | Add `graphType` field and helper properties |
| `quo-vadis-ksp/.../GraphInfoExtractor.kt` | Modify | Add `extractGraphType()` and validation |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| KSP-001 (GraphType enum) | Hard | Must complete first |

---

## Acceptance Criteria

- [ ] `GraphInfo` has `graphType: GraphType` field
- [ ] `GraphInfo` has `isStackGraph`, `isTabGraph`, `isPaneGraph` helper properties
- [ ] `GraphInfoExtractor.extractGraphType()` correctly parses enum from annotation
- [ ] Default value is `GraphType.STACK` when type is not specified
- [ ] Validation warnings for potentially misconfigured graphs
- [ ] Existing generators continue to work without changes
- [ ] Unit tests for extraction logic
- [ ] KDoc documentation on new APIs

---

## Testing Notes

```kotlin
@Test
fun `extracts default GraphType as STACK`() {
    val source = """
        @Graph("test")
        sealed class TestGraph : Destination {
            @Route("home") data object Home : TestGraph()
        }
    """.trimIndent()

    val graphInfo = process(source)
    assertEquals(GraphType.STACK, graphInfo.graphType)
    assertTrue(graphInfo.isStackGraph)
}

@Test
fun `extracts explicit TAB GraphType`() {
    val source = """
        @Graph("test", type = GraphType.TAB)
        sealed class TestGraph : Destination {
            @Route("home") data object Home : TestGraph()
            @Route("profile") data object Profile : TestGraph()
        }
    """.trimIndent()

    val graphInfo = process(source)
    assertEquals(GraphType.TAB, graphInfo.graphType)
    assertTrue(graphInfo.isTabGraph)
}

@Test
fun `extracts explicit PANE GraphType`() {
    val source = """
        @Graph("test", type = GraphType.PANE)
        sealed class TestGraph : Destination {
            @Route("list") data object List : TestGraph()
            @Route("detail") data object Detail : TestGraph()
        }
    """.trimIndent()

    val graphInfo = process(source)
    assertEquals(GraphType.PANE, graphInfo.graphType)
    assertTrue(graphInfo.isPaneGraph)
}

@Test
fun `warns for TAB graph with single destination`() {
    val source = """
        @Graph("test", type = GraphType.TAB)
        sealed class TestGraph : Destination {
            @Route("only") data object Only : TestGraph()
        }
    """.trimIndent()

    process(source)
    // Verify warning was logged
    assertTrue(logger.warnings.any { "fewer than 2 destinations" in it })
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 3 Overview
- [KSP-001](./KSP-001-graph-type-enum.md) - GraphType enum definition
- [Current GraphInfoExtractor](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/GraphInfoExtractor.kt)
- [Current GraphInfo](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/GraphInfo.kt)
