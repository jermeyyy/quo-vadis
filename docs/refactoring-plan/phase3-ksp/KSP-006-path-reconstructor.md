# Task KSP-006: Generate Path Reconstructors for Deep Linking

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | KSP-006 |
| **Name** | Deep Link Path Reconstructor Generation |
| **Phase** | 3 - KSP Processor Updates |
| **Complexity** | High |
| **Estimated Time** | 4-5 days |
| **Dependencies** | KSP-004, KSP-005 |

## Overview

Generate "Path Reconstructors" that can synthesize the full NavNode tree path when a deep link targets a specific destination. This ensures that when a user opens a deep link to a nested screen, the entire navigation hierarchy is properly constructed with a functional back stack.

## Problem Statement

In tree-based navigation, a deep link to `profile/settings` needs to:
1. Create the root StackNode
2. Navigate to the correct Tab (if in a TabNode)
3. Build the correct stack within that tab
4. Land on the target destination

Without proper path reconstruction, the user might land on the screen but with no back navigation available.

## Implementation

### Step 1: Create PathReconstructorGenerator

```kotlin
// quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/PathReconstructorGenerator.kt

class PathReconstructorGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    
    fun generatePathReconstructors(
        graphInfo: GraphInfo,
        graphRelationships: Map<String, GraphRelationship>
    ) {
        val fileSpec = FileSpec.builder(
            graphInfo.packageName,
            "${graphInfo.name}PathReconstructor"
        ).apply {
            addImport("com.jermey.quo.vadis.core.navigation.core", "NavNode")
            addImport("com.jermey.quo.vadis.core.navigation.core", "ScreenNode")
            addImport("com.jermey.quo.vadis.core.navigation.core", "StackNode")
            addImport("com.jermey.quo.vadis.core.navigation.core", "TabNode")
            
            graphInfo.routes.forEach { route ->
                addFunction(generateReconstructorForRoute(route, graphInfo, graphRelationships))
            }
        }.build()
        
        fileSpec.writeTo(codeGenerator, aggregating = false)
    }
    
    private fun generateReconstructorForRoute(
        route: RouteInfo,
        graphInfo: GraphInfo,
        relationships: Map<String, GraphRelationship>
    ): FunSpec {
        val path = findPathToDestination(route, graphInfo, relationships)
        
        return FunSpec.builder("reconstructPathTo${route.name}")
            .addKdoc("Reconstructs the full navigation path to ${route.name}.\n")
            .addKdoc("@param arguments Optional arguments for the destination\n")
            .addKdoc("@return NavNode tree with the destination as the active screen\n")
            .addParameter(
                ParameterSpec.builder(
                    "arguments",
                    Map::class.asClassName()
                        .parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true))
                )
                .defaultValue("emptyMap()")
                .build()
            )
            .returns(NAV_NODE)
            .apply {
                generatePathReconstruction(path, route)
            }
            .build()
    }
}
```

### Step 2: Path Finding Algorithm

```kotlin
data class PathSegment(
    val graphInfo: GraphInfo,
    val nodeType: NodeType,
    val tabIndex: Int? = null  // For TabNode, which tab to select
)

enum class NodeType { STACK, TAB, PANE, SCREEN }

private fun findPathToDestination(
    route: RouteInfo,
    targetGraph: GraphInfo,
    relationships: Map<String, GraphRelationship>
): List<PathSegment> {
    val path = mutableListOf<PathSegment>()
    
    // Build path from root to target
    var currentGraph: GraphInfo? = targetGraph
    while (currentGraph != null) {
        val relationship = relationships[currentGraph.qualifiedName]
        
        val nodeType = when (currentGraph.graphType) {
            GraphType.STACK -> NodeType.STACK
            GraphType.TAB -> NodeType.TAB
            GraphType.PANE -> NodeType.PANE
        }
        
        val tabIndex = if (nodeType == NodeType.TAB) {
            findTabIndexContainingRoute(currentGraph, route)
        } else null
        
        path.add(0, PathSegment(currentGraph, nodeType, tabIndex))
        
        currentGraph = relationship?.parentGraphName?.let { parentName ->
            relationships[parentName]?.graphInfo
        }
    }
    
    return path
}

private fun findTabIndexContainingRoute(
    tabGraph: GraphInfo,
    targetRoute: RouteInfo
): Int {
    return tabGraph.tabs.indexOfFirst { tab ->
        tab.routes.any { it.name == targetRoute.name } ||
        tab.nestedGraphs.any { containsRoute(it, targetRoute) }
    }
}
```

### Step 3: Generate Reconstruction Code

```kotlin
private fun FunSpec.Builder.generatePathReconstruction(
    path: List<PathSegment>,
    targetRoute: RouteInfo
) {
    // Generate from innermost (target) to outermost
    val reversedPath = path.reversed()
    
    // Start with the target screen
    addStatement(
        "val targetScreen = %T(",
        SCREEN_NODE
    )
    addStatement("    key = %S,", "screen_${targetRoute.name}")
    addStatement("    parentKey = %S,", "stack_${reversedPath.first().graphInfo.name}")
    addStatement("    destination = %T(", targetRoute.destinationClass)
    
    // Handle arguments
    if (targetRoute.arguments.isNotEmpty()) {
        targetRoute.arguments.forEach { arg ->
            addStatement(
                "        %L = arguments[%S] as? %T ?: %L,",
                arg.name, arg.name, arg.type, arg.defaultValue ?: "null"
            )
        }
    }
    addStatement("    )")
    addStatement(")")
    addStatement("")
    
    // Build parent nodes
    var childVarName = "targetScreen"
    reversedPath.forEachIndexed { index, segment ->
        val varName = if (index == reversedPath.lastIndex) "rootNode" else "node$index"
        
        when (segment.nodeType) {
            NodeType.STACK -> {
                addStatement(
                    "val %L = %T(",
                    varName, STACK_NODE
                )
                addStatement("    key = %S,", "stack_${segment.graphInfo.name}")
                addStatement("    parentKey = %L,", 
                    if (index == reversedPath.lastIndex) "null" else "\"stack_${reversedPath[index + 1].graphInfo.name}\""
                )
                addStatement("    children = listOf(%L)", childVarName)
                addStatement(")")
            }
            NodeType.TAB -> {
                addStatement(
                    "val %L = %T(",
                    varName, TAB_NODE
                )
                addStatement("    key = %S,", "tab_${segment.graphInfo.name}")
                addStatement("    parentKey = %L,",
                    if (index == reversedPath.lastIndex) "null" else "\"stack_${reversedPath[index + 1].graphInfo.name}\""
                )
                addStatement("    stacks = listOf(")
                segment.graphInfo.tabs.forEachIndexed { tabIdx, tab ->
                    if (tabIdx == segment.tabIndex) {
                        addStatement("        %T(key = %S, parentKey = %S, children = listOf(%L)),",
                            STACK_NODE, "stack_${tab.name}", "tab_${segment.graphInfo.name}", childVarName
                        )
                    } else {
                        // Empty stack for other tabs
                        addStatement("        %T(key = %S, parentKey = %S, children = emptyList()),",
                            STACK_NODE, "stack_${tab.name}", "tab_${segment.graphInfo.name}"
                        )
                    }
                }
                addStatement("    ),")
                addStatement("    activeStackIndex = %L", segment.tabIndex)
                addStatement(")")
            }
            NodeType.PANE -> {
                // Similar handling for PaneNode
            }
            NodeType.SCREEN -> { /* Already handled */ }
        }
        
        childVarName = varName
    }
    
    addStatement("return rootNode")
}
```

### Step 4: Deep Link URL Parsing Integration

```kotlin
// Generate URL pattern matching
fun generateDeepLinkMatcher(graphInfo: GraphInfo): FunSpec {
    return FunSpec.builder("matchDeepLink")
        .addParameter("uri", Uri::class)
        .returns(NAV_NODE.copy(nullable = true))
        .apply {
            beginControlFlow("return when")
            
            graphInfo.routes
                .filter { it.deepLinkPattern != null }
                .forEach { route ->
                    addStatement(
                        "uri.path?.matches(Regex(%S)) == true -> {",
                        route.deepLinkPattern!!.toRegex()
                    )
                    addStatement("    val args = extractArguments(uri, %S)", route.deepLinkPattern)
                    addStatement("    reconstructPathTo%L(args)", route.name)
                    addStatement("}")
                }
            
            addStatement("else -> null")
            endControlFlow()
        }
        .build()
}

private fun generateExtractArguments(): FunSpec {
    return FunSpec.builder("extractArguments")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("uri", Uri::class)
        .addParameter("pattern", String::class)
        .returns(Map::class.asClassName().parameterizedBy(String::class.asClassName(), Any::class.asClassName().copy(nullable = true)))
        .addCode("""
            val regex = Regex(pattern)
            val match = regex.find(uri.path ?: "") ?: return emptyMap()
            return match.groupValues
                .drop(1)
                .mapIndexed { index, value -> 
                    regex.pattern.findParameterNames()[index] to value 
                }
                .toMap()
        """.trimIndent())
        .build()
}
```

## Generated Output Example

For a graph structure:
```kotlin
@Graph(name = "app", type = GraphType.STACK)
sealed class AppGraph {
    @Route object Home : AppGraph()
    
    @Graph(name = "tabs", type = GraphType.TAB)
    sealed class TabsGraph : AppGraph() {
        @Route object Feed : TabsGraph()
        @Route @DeepLink("/profile/{userId}") 
        data class Profile(val userId: String) : TabsGraph()
    }
}
```

Generated code:
```kotlin
// AppPathReconstructor.kt
fun reconstructPathToProfile(arguments: Map<String, Any?> = emptyMap()): NavNode {
    val targetScreen = ScreenNode(
        key = "screen_Profile",
        parentKey = "stack_tabs_1",
        destination = Profile(
            userId = arguments["userId"] as? String ?: ""
        )
    )
    
    val tabNode = TabNode(
        key = "tab_tabs",
        parentKey = "stack_app",
        stacks = listOf(
            StackNode(key = "stack_tabs_0", parentKey = "tab_tabs", children = emptyList()),
            StackNode(key = "stack_tabs_1", parentKey = "tab_tabs", children = listOf(targetScreen))
        ),
        activeStackIndex = 1
    )
    
    val rootNode = StackNode(
        key = "stack_app",
        parentKey = null,
        children = listOf(tabNode)
    )
    
    return rootNode
}

fun matchDeepLink(uri: Uri): NavNode? {
    return when {
        uri.path?.matches(Regex("/profile/(.+)")) == true -> {
            val args = extractArguments(uri, "/profile/(?<userId>.+)")
            reconstructPathToProfile(args)
        }
        else -> null
    }
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../PathReconstructorGenerator.kt` | New |
| `quo-vadis-ksp/src/main/kotlin/.../GraphProcessor.kt` | Modify |

## Acceptance Criteria

- [ ] Path reconstructors generated for each `@Route`
- [ ] TabNode scenarios select correct tab index
- [ ] PaneNode scenarios populate required slots
- [ ] Arguments extracted from deep link URLs
- [ ] Generated code compiles and runs correctly
- [ ] Back navigation works from reconstructed state
- [ ] Missing required arguments produce clear errors
- [ ] Unit tests cover various graph structures

## References

- [KSP-004: NavNode Builder Generator](./KSP-004-navnode-generator.md)
- [KSP-005: Nested Graph Support](./KSP-005-nested-graphs.md)
- [RISK-003: Deep Link Validator](../phase6-risks/RISK-003-deeplink-validator.md)
