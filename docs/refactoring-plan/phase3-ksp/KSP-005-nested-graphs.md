# Task KSP-005: Support Nested Graph Definitions

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | KSP-005 |
| **Name** | Nested Graph Detection and Generation |
| **Phase** | 3 - KSP Processor Updates |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | KSP-004 (NavNode Builder Generator) |

## Overview

Enable the KSP processor to detect and process nested `@Graph` annotations within parent graphs, generating proper parent-child NavNode relationships. This is essential for complex navigation structures where one graph contains another (e.g., a main graph containing a tabbed graph).

## Current Behavior

Currently, graphs are processed independently without considering nesting relationships. Each `@Graph` generates its own isolated builder function.

## Target Behavior

Support hierarchical graph definitions:

```kotlin
@Graph(name = "app", type = GraphType.STACK)
sealed class AppGraph {
    @Route object Home : AppGraph()
    
    @Graph(name = "tabs", type = GraphType.TAB)  // Nested graph
    sealed class TabsGraph : AppGraph() {
        @Route object Feed : TabsGraph()
        @Route object Profile : TabsGraph()
    }
    
    @Route object Settings : AppGraph()
}
```

## Implementation

### Step 1: Detect Nested Graphs

Update `GraphProcessor.kt` to identify nested graph relationships:

```kotlin
class GraphProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    
    private val graphRelationships = mutableMapOf<String, GraphRelationship>()
    
    data class GraphRelationship(
        val graphInfo: GraphInfo,
        val parentGraphName: String?,
        val childGraphNames: List<String>
    )
    
    override fun process(resolver: Resolver): List<KSAnnotatedSymbol> {
        val graphs = resolver.getSymbolsWithAnnotation(Graph::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
        
        // First pass: collect all graphs
        val graphInfoMap = graphs.associate { 
            it.qualifiedName?.asString() to extractGraphInfo(it) 
        }
        
        // Second pass: determine relationships
        graphs.forEach { graphClass ->
            val parentClass = graphClass.superTypes
                .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
                .find { it.annotations.any { ann -> ann.shortName.asString() == "Graph" } }
            
            graphRelationships[graphClass.qualifiedName!!.asString()] = GraphRelationship(
                graphInfo = graphInfoMap[graphClass.qualifiedName!!.asString()]!!,
                parentGraphName = parentClass?.qualifiedName?.asString(),
                childGraphNames = findDirectChildren(graphClass, graphInfoMap.keys)
            )
        }
        
        // Generate in topological order
        val sortedGraphs = topologicalSort(graphRelationships)
        sortedGraphs.forEach { generateNavNode(it) }
        
        return emptyList()
    }
}
```

### Step 2: Topological Sort for Generation Order

Ensure child graphs are generated before parents that reference them:

```kotlin
private fun topologicalSort(
    relationships: Map<String, GraphRelationship>
): List<GraphRelationship> {
    val result = mutableListOf<GraphRelationship>()
    val visited = mutableSetOf<String>()
    val visiting = mutableSetOf<String>()
    
    fun visit(name: String) {
        if (name in visited) return
        if (name in visiting) {
            throw IllegalStateException("Circular graph reference detected: $name")
        }
        
        visiting.add(name)
        relationships[name]?.childGraphNames?.forEach { visit(it) }
        visiting.remove(name)
        visited.add(name)
        relationships[name]?.let { result.add(it) }
    }
    
    relationships.keys.forEach { visit(it) }
    return result
}
```

### Step 3: Generate Parent-Child Linkages

Update `NavNodeGenerator` to include nested graphs:

```kotlin
fun generateNavNodeBuilder(relationship: GraphRelationship): FileSpec {
    val graphInfo = relationship.graphInfo
    
    return FileSpec.builder(graphInfo.packageName, "${graphInfo.name}NavNodeBuilder")
        .addFunction(
            FunSpec.builder("build${graphInfo.name}NavNode")
                .addParameter("parentKey", String::class.asTypeName().copy(nullable = true))
                .returns(NAV_NODE)
                .apply {
                    when (graphInfo.graphType) {
                        GraphType.STACK -> generateStackWithNestedGraphs(relationship)
                        GraphType.TAB -> generateTabWithNestedGraphs(relationship)
                        GraphType.PANE -> generatePaneWithNestedGraphs(relationship)
                    }
                }
                .build()
        )
        .build()
}

private fun FunSpec.Builder.generateStackWithNestedGraphs(
    relationship: GraphRelationship
) {
    val graphInfo = relationship.graphInfo
    addStatement("val stackKey = %S", "stack_${graphInfo.name}")
    
    beginControlFlow("val children = listOfNotNull(")
    
    graphInfo.routes.forEach { route ->
        if (route.isNestedGraph) {
            // Reference the nested graph's builder
            addStatement(
                "build%LNavNode(parentKey = stackKey),",
                route.nestedGraphName
            )
        } else {
            addStatement(
                "%T(key = %S, parentKey = stackKey, destination = %T),",
                SCREEN_NODE, "screen_${route.name}", route.destinationClass
            )
        }
    }
    
    endControlFlow()
    addStatement(")")
    
    addStatement(
        "return %T(key = stackKey, parentKey = parentKey, children = children)",
        STACK_NODE
    )
}
```

### Step 4: Circular Reference Detection

Provide clear error messages for circular references:

```kotlin
private fun detectCircularReferences(
    relationships: Map<String, GraphRelationship>
): List<CircularReferenceError> {
    val errors = mutableListOf<CircularReferenceError>()
    val visited = mutableSetOf<String>()
    val path = mutableListOf<String>()
    
    fun dfs(name: String) {
        if (name in path) {
            val cycle = path.dropWhile { it != name } + name
            errors.add(CircularReferenceError(cycle))
            return
        }
        if (name in visited) return
        
        path.add(name)
        relationships[name]?.childGraphNames?.forEach { dfs(it) }
        path.removeLast()
        visited.add(name)
    }
    
    relationships.keys.forEach { dfs(it) }
    return errors
}

data class CircularReferenceError(val cycle: List<String>) {
    override fun toString() = "Circular graph reference: ${cycle.joinToString(" -> ")}"
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../GraphProcessor.kt` | Modify |
| `quo-vadis-ksp/src/main/kotlin/.../NavNodeGenerator.kt` | Modify |
| `quo-vadis-ksp/src/main/kotlin/.../GraphRelationship.kt` | New |

## Edge Cases

1. **Multiple Levels of Nesting**: A -> B -> C graph hierarchy
2. **Self-Referencing Graphs**: Detected and reported as error
3. **Diamond Dependencies**: A contains B and C, both contain D
4. **Empty Nested Graphs**: Graphs with no routes

## Acceptance Criteria

- [ ] Nested `@Graph` annotations are detected
- [ ] Parent-child relationships are tracked correctly
- [ ] Graphs are generated in topological order
- [ ] Circular references produce clear error messages
- [ ] Generated code includes correct parentKey linkages
- [ ] Multiple nesting levels are supported
- [ ] Unit tests cover all edge cases

## Testing Notes

```kotlin
@Test
fun `nested graph generates correct parent-child linkage`() {
    val compilation = compile(
        """
        @Graph(name = "parent", type = GraphType.STACK)
        sealed class ParentGraph {
            @Route object Home : ParentGraph()
            
            @Graph(name = "child", type = GraphType.TAB)
            sealed class ChildGraph : ParentGraph() {
                @Route object Tab1 : ChildGraph()
            }
        }
        """
    )
    
    val parentBuilder = compilation.generatedFile("ParentGraphNavNodeBuilder.kt")
    assertThat(parentBuilder).contains("buildChildNavNode(parentKey = stackKey)")
}

@Test
fun `circular reference produces error`() {
    val compilation = compile(
        """
        @Graph sealed class A { @Graph sealed class B : A() { /* references A */ } }
        """
    )
    
    assertThat(compilation.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
    assertThat(compilation.messages).contains("Circular graph reference")
}
```

## References

- [KSP-004: NavNode Builder Generator](./KSP-004-navnode-generator.md)
- [CORE-001: NavNode Hierarchy](../phase1-core/CORE-001-navnode-hierarchy.md)
- [Original Refactoring Plan - Section 3.3](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
