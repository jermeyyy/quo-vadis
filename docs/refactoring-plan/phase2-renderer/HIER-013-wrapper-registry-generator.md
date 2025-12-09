````markdown
# HIER-013: Wrapper Registry Generator

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-013 |
| **Task Name** | Generate WrapperRegistry Implementation |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Medium-Large |
| **Estimated Time** | 3-4 days |
| **Dependencies** | HIER-002 (WrapperRegistry interface), HIER-012 (WrapperExtractor) |
| **Blocked By** | HIER-002, HIER-012 |
| **Blocks** | HIER-015 |

---

## Overview

This task creates the `WrapperRegistryGenerator` that produces the `GeneratedWrapperRegistry` implementation from extracted `WrapperInfo` models. The generated registry dispatches to appropriate wrapper functions based on navigation node keys.

### Context

The wrapper registry is the runtime bridge between the hierarchical rendering engine and user-defined wrapper composables. It enables:
- Type-safe wrapper resolution at runtime
- Compile-time validation of wrapper bindings
- Default fallback for containers without custom wrappers
- Clean separation of wrapper definition from rendering logic

---

## File Location

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/WrapperRegistryGenerator.kt
```

---

## Implementation

### WrapperRegistryGenerator

```kotlin
package com.jermey.quo.vadis.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.QuoVadisClassNames
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.jermey.quo.vadis.ksp.models.WrapperType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT

/**
 * Generates the WrapperRegistry implementation from extracted wrapper info.
 *
 * Produces a `GeneratedWrapperRegistry` object that:
 * - Implements the `WrapperRegistry` interface
 * - Dispatches `TabWrapper()` calls to @TabWrapper functions based on node key
 * - Dispatches `PaneWrapper()` calls to @PaneWrapper functions based on node key
 * - Provides default fallback wrappers for unbound containers
 *
 * ## Generated Code Structure
 *
 * ```kotlin
 * object GeneratedWrapperRegistry : WrapperRegistry {
 *
 *     @Composable
 *     override fun TabWrapper(
 *         scope: TabWrapperScope,
 *         tabNodeKey: String,
 *         content: @Composable () -> Unit
 *     ) {
 *         when {
 *             tabNodeKey.contains("mainTabs") -> MainTabsWrapper(scope, content)
 *             else -> content()  // Default wrapper
 *         }
 *     }
 *
 *     @Composable
 *     override fun PaneWrapper(
 *         scope: PaneWrapperScope,
 *         paneNodeKey: String,
 *         content: @Composable PaneContentScope.() -> Unit
 *     ) {
 *         when {
 *             paneNodeKey.contains("catalog") -> CatalogPaneWrapper(scope, content)
 *             else -> DefaultPaneWrapper(scope, content)
 *         }
 *     }
 * }
 * ```
 *
 * @property codeGenerator KSP code generator for file output
 * @property logger KSP logger for info/warning/error output
 */
class WrapperRegistryGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    companion object {
        private const val GENERATED_CLASS_NAME = "GeneratedWrapperRegistry"
        private const val GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"
        
        // Class names for imports
        private val WRAPPER_REGISTRY = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.registry",
            "WrapperRegistry"
        )
        private val TAB_WRAPPER_SCOPE = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.scope",
            "TabWrapperScope"
        )
        private val PANE_WRAPPER_SCOPE = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.scope",
            "PaneWrapperScope"
        )
        private val PANE_CONTENT_SCOPE = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.scope",
            "PaneContentScope"
        )
        private val COMPOSABLE = ClassName("androidx.compose.runtime", "Composable")
        private val PANE_ROLE = ClassName(
            "com.jermey.quo.vadis.core.navigation.core",
            "PaneRole"
        )
    }

    /**
     * Generate the WrapperRegistry implementation.
     *
     * @param wrappers List of extracted WrapperInfo from WrapperExtractor
     */
    fun generate(wrappers: List<WrapperInfo>) {
        if (wrappers.isEmpty()) {
            logger.info("No wrappers found, generating empty WrapperRegistry")
        }

        val tabWrappers = wrappers.filter { it.wrapperType == WrapperType.TAB }
        val paneWrappers = wrappers.filter { it.wrapperType == WrapperType.PANE }

        logger.info("Generating WrapperRegistry with ${tabWrappers.size} tab wrappers and ${paneWrappers.size} pane wrappers")

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, GENERATED_CLASS_NAME)
            .addType(buildRegistryObject(tabWrappers, paneWrappers))
            .addImport("androidx.compose.runtime", "Composable")
            .apply {
                // Import all wrapper functions
                wrappers.forEach { wrapper ->
                    addImport(wrapper.packageName, wrapper.functionName)
                }
            }
            .build()

        // Collect source files for incremental processing
        val dependencies = wrappers.mapNotNull { 
            it.functionDeclaration.containingFile 
        }.toTypedArray()

        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *dependencies),
            packageName = GENERATED_PACKAGE,
            fileName = GENERATED_CLASS_NAME
        ).bufferedWriter().use { writer ->
            fileSpec.writeTo(writer)
        }

        logger.info("Generated $GENERATED_PACKAGE.$GENERATED_CLASS_NAME")
    }

    /**
     * Build the GeneratedWrapperRegistry object.
     */
    private fun buildRegistryObject(
        tabWrappers: List<WrapperInfo>,
        paneWrappers: List<WrapperInfo>
    ): TypeSpec {
        return TypeSpec.objectBuilder(GENERATED_CLASS_NAME)
            .addSuperinterface(WRAPPER_REGISTRY)
            .addKdoc(buildClassKDoc(tabWrappers, paneWrappers))
            .addFunction(buildTabWrapperFunction(tabWrappers))
            .addFunction(buildPaneWrapperFunction(paneWrappers))
            .addFunction(buildHasTabWrapperFunction(tabWrappers))
            .addFunction(buildHasPaneWrapperFunction(paneWrappers))
            .build()
    }

    /**
     * Build KDoc for the generated class.
     */
    private fun buildClassKDoc(
        tabWrappers: List<WrapperInfo>,
        paneWrappers: List<WrapperInfo>
    ): CodeBlock {
        return CodeBlock.builder()
            .addStatement("Generated WrapperRegistry implementation.")
            .addStatement("")
            .addStatement("Tab Wrappers: ${tabWrappers.size}")
            .apply {
                tabWrappers.forEach { wrapper ->
                    addStatement("  - ${wrapper.targetClassSimpleName} -> ${wrapper.functionName}()")
                }
            }
            .addStatement("")
            .addStatement("Pane Wrappers: ${paneWrappers.size}")
            .apply {
                paneWrappers.forEach { wrapper ->
                    addStatement("  - ${wrapper.targetClassSimpleName} -> ${wrapper.functionName}()")
                }
            }
            .addStatement("")
            .addStatement("@generated by Quo Vadis KSP Processor")
            .build()
    }

    /**
     * Build the TabWrapper function.
     *
     * ```kotlin
     * @Composable
     * override fun TabWrapper(
     *     scope: TabWrapperScope,
     *     tabNodeKey: String,
     *     content: @Composable () -> Unit
     * ) {
     *     when {
     *         tabNodeKey.contains("mainTabs") -> MainTabsWrapper(scope, content)
     *         else -> content()
     *     }
     * }
     * ```
     */
    private fun buildTabWrapperFunction(tabWrappers: List<WrapperInfo>): FunSpec {
        val contentLambdaType = LambdaTypeName.get(returnType = UNIT)
            .copy(annotations = listOf(
                com.squareup.kotlinpoet.AnnotationSpec.builder(COMPOSABLE).build()
            ))

        return FunSpec.builder("TabWrapper")
            .addAnnotation(COMPOSABLE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("scope", TAB_WRAPPER_SCOPE)
            .addParameter("tabNodeKey", String::class)
            .addParameter("content", contentLambdaType)
            .addCode(buildTabWrapperDispatch(tabWrappers))
            .build()
    }

    /**
     * Build the when expression for tab wrapper dispatch.
     */
    private fun buildTabWrapperDispatch(tabWrappers: List<WrapperInfo>): CodeBlock {
        if (tabWrappers.isEmpty()) {
            return CodeBlock.builder()
                .addStatement("// No tab wrappers registered, render content directly")
                .addStatement("content()")
                .build()
        }

        return CodeBlock.builder()
            .beginControlFlow("when")
            .apply {
                tabWrappers.forEach { wrapper ->
                    addStatement(
                        "tabNodeKey.contains(%S) -> %N(scope, content)",
                        wrapper.targetNodeKey,
                        wrapper.functionName
                    )
                }
            }
            .addStatement("else -> content()  // Default: no wrapper")
            .endControlFlow()
            .build()
    }

    /**
     * Build the PaneWrapper function.
     *
     * ```kotlin
     * @Composable
     * override fun PaneWrapper(
     *     scope: PaneWrapperScope,
     *     paneNodeKey: String,
     *     content: @Composable PaneContentScope.() -> Unit
     * ) {
     *     when {
     *         paneNodeKey.contains("catalog") -> CatalogPaneWrapper(scope, content)
     *         else -> DefaultPaneWrapper(scope, content)
     *     }
     * }
     * ```
     */
    private fun buildPaneWrapperFunction(paneWrappers: List<WrapperInfo>): FunSpec {
        val contentLambdaType = LambdaTypeName.get(
            receiver = PANE_CONTENT_SCOPE,
            returnType = UNIT
        ).copy(annotations = listOf(
            com.squareup.kotlinpoet.AnnotationSpec.builder(COMPOSABLE).build()
        ))

        return FunSpec.builder("PaneWrapper")
            .addAnnotation(COMPOSABLE)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("scope", PANE_WRAPPER_SCOPE)
            .addParameter("paneNodeKey", String::class)
            .addParameter("content", contentLambdaType)
            .addCode(buildPaneWrapperDispatch(paneWrappers))
            .build()
    }

    /**
     * Build the when expression for pane wrapper dispatch.
     */
    private fun buildPaneWrapperDispatch(paneWrappers: List<WrapperInfo>): CodeBlock {
        if (paneWrappers.isEmpty()) {
            return CodeBlock.builder()
                .addStatement("// No pane wrappers registered, use default")
                .addStatement("DefaultPaneWrapper(scope, content)")
                .build()
        }

        return CodeBlock.builder()
            .beginControlFlow("when")
            .apply {
                paneWrappers.forEach { wrapper ->
                    addStatement(
                        "paneNodeKey.contains(%S) -> %N(scope, content)",
                        wrapper.targetNodeKey,
                        wrapper.functionName
                    )
                }
            }
            .addStatement("else -> DefaultPaneWrapper(scope, content)")
            .endControlFlow()
            .build()
    }

    /**
     * Build the hasTabWrapper function.
     *
     * ```kotlin
     * override fun hasTabWrapper(tabNodeKey: String): Boolean {
     *     return when {
     *         tabNodeKey.contains("mainTabs") -> true
     *         else -> false
     *     }
     * }
     * ```
     */
    private fun buildHasTabWrapperFunction(tabWrappers: List<WrapperInfo>): FunSpec {
        return FunSpec.builder("hasTabWrapper")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("tabNodeKey", String::class)
            .returns(Boolean::class)
            .addCode(buildHasWrapperCheck(tabWrappers))
            .build()
    }

    /**
     * Build the hasPaneWrapper function.
     *
     * ```kotlin
     * override fun hasPaneWrapper(paneNodeKey: String): Boolean {
     *     return when {
     *         paneNodeKey.contains("catalog") -> true
     *         else -> false
     *     }
     * }
     * ```
     */
    private fun buildHasPaneWrapperFunction(paneWrappers: List<WrapperInfo>): FunSpec {
        return FunSpec.builder("hasPaneWrapper")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("paneNodeKey", String::class)
            .returns(Boolean::class)
            .addCode(buildHasWrapperCheck(paneWrappers))
            .build()
    }

    /**
     * Build the when expression for hasWrapper check.
     */
    private fun buildHasWrapperCheck(wrappers: List<WrapperInfo>): CodeBlock {
        if (wrappers.isEmpty()) {
            return CodeBlock.of("return false\n")
        }

        return CodeBlock.builder()
            .beginControlFlow("return when")
            .apply {
                wrappers.forEach { wrapper ->
                    addStatement(
                        "%L.contains(%S) -> true",
                        if (wrappers.first().wrapperType == WrapperType.TAB) "tabNodeKey" else "paneNodeKey",
                        wrapper.targetNodeKey
                    )
                }
            }
            .addStatement("else -> false")
            .endControlFlow()
            .build()
    }
}
```

### Default Pane Wrapper Helper

The generator references a `DefaultPaneWrapper` function. This should be provided in the core module:

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/DefaultPaneWrapper.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneContentScope
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Default pane wrapper used when no custom @PaneWrapper is defined.
 *
 * Provides a simple side-by-side layout on expanded screens and
 * single-pane layout on compact screens.
 *
 * @param scope The pane wrapper scope with pane state
 * @param content Content accessor for rendering individual panes
 */
@Composable
fun DefaultPaneWrapper(
    scope: PaneWrapperScope,
    content: @Composable PaneContentScope.() -> Unit
) {
    if (scope.isExpanded) {
        // Multi-pane layout for expanded screens
        Row(modifier = Modifier.fillMaxSize()) {
            // Primary pane
            if (scope.hasPane(PaneRole.PRIMARY)) {
                Box(modifier = Modifier.weight(1f)) {
                    content.invoke(scope.paneContent(PaneRole.PRIMARY))
                }
            }
            
            // Secondary pane
            if (scope.hasPane(PaneRole.SECONDARY) && scope.isPaneVisible(PaneRole.SECONDARY)) {
                Box(modifier = Modifier.weight(1f)) {
                    content.invoke(scope.paneContent(PaneRole.SECONDARY))
                }
            }
            
            // Extra pane (if configured and visible)
            if (scope.hasPane(PaneRole.EXTRA) && scope.isPaneVisible(PaneRole.EXTRA)) {
                Box(modifier = Modifier.weight(1f)) {
                    content.invoke(scope.paneContent(PaneRole.EXTRA))
                }
            }
        }
    } else {
        // Single-pane layout for compact screens
        Box(modifier = Modifier.fillMaxSize()) {
            content.invoke(scope.paneContent(scope.activePaneRole))
        }
    }
}
```

---

## Generated Code Example

Given these wrapper definitions:

```kotlin
// Tab wrapper
@Tab(name = "mainTabs", items = [HomeTab::class, ProfileTab::class])
object MainTabs

@TabWrapper(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabWrapperScope,
    content: @Composable () -> Unit
) {
    Scaffold(bottomBar = { /* ... */ }) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}

// Pane wrapper
@Pane(name = "catalog")
sealed class CatalogPane : Destination

@PaneWrapper(CatalogPane::class)
@Composable
fun CatalogPaneWrapper(
    scope: PaneWrapperScope,
    content: @Composable PaneContentScope.() -> Unit
) {
    ListDetailPaneScaffold(
        listPane = { content.invoke(scope.paneContent(PaneRole.PRIMARY)) },
        detailPane = { content.invoke(scope.paneContent(PaneRole.SECONDARY)) }
    )
}
```

KSP generates:

```kotlin
package com.jermey.quo.vadis.generated

import androidx.compose.runtime.Composable
import com.example.app.MainTabsWrapper
import com.example.app.CatalogPaneWrapper
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.DefaultPaneWrapper
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneContentScope
import com.jermey.quo.vadis.core.navigation.compose.scope.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.scope.TabWrapperScope

/**
 * Generated WrapperRegistry implementation.
 *
 * Tab Wrappers: 1
 *   - MainTabs -> MainTabsWrapper()
 *
 * Pane Wrappers: 1
 *   - CatalogPane -> CatalogPaneWrapper()
 *
 * @generated by Quo Vadis KSP Processor
 */
object GeneratedWrapperRegistry : WrapperRegistry {

    @Composable
    override fun TabWrapper(
        scope: TabWrapperScope,
        tabNodeKey: String,
        content: @Composable () -> Unit
    ) {
        when {
            tabNodeKey.contains("mainTabs") -> MainTabsWrapper(scope, content)
            else -> content()  // Default: no wrapper
        }
    }

    @Composable
    override fun PaneWrapper(
        scope: PaneWrapperScope,
        paneNodeKey: String,
        content: @Composable PaneContentScope.() -> Unit
    ) {
        when {
            paneNodeKey.contains("catalog") -> CatalogPaneWrapper(scope, content)
            else -> DefaultPaneWrapper(scope, content)
        }
    }

    override fun hasTabWrapper(tabNodeKey: String): Boolean {
        return when {
            tabNodeKey.contains("mainTabs") -> true
            else -> false
        }
    }

    override fun hasPaneWrapper(paneNodeKey: String): Boolean {
        return when {
            paneNodeKey.contains("catalog") -> true
            else -> false
        }
    }
}
```

---

## Integration Points

### 1. Main Symbol Processor (HIER-015)

Update `QuoVadisSymbolProcessor` to invoke the generator:

```kotlin
private val wrapperExtractor = WrapperExtractor(logger)
private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)

private fun processWrappers(resolver: Resolver) {
    val wrappers = wrapperExtractor.extractAll(resolver)
    wrapperRegistryGenerator.generate(wrappers)
}
```

### 2. Runtime Usage (HierarchicalQuoVadisHost)

The generated registry is used at runtime:

```kotlin
@Composable
fun HierarchicalQuoVadisHost(
    navigator: Navigator,
    wrapperRegistry: WrapperRegistry = GeneratedWrapperRegistry,
    // ...
) {
    // TabRenderer and PaneRenderer use wrapperRegistry internally
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class WrapperRegistryGeneratorTest {

    private lateinit var generator: WrapperRegistryGenerator
    private lateinit var mockCodeGenerator: CodeGenerator
    private lateinit var mockLogger: KSPLogger

    @Before
    fun setup() {
        mockCodeGenerator = mock()
        mockLogger = mock()
        generator = WrapperRegistryGenerator(mockCodeGenerator, mockLogger)
    }

    @Test
    fun `generates empty registry when no wrappers`() {
        // Given
        val wrappers = emptyList<WrapperInfo>()
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "object GeneratedWrapperRegistry")
        assertContains(output, "content()") // Default behavior
        assertContains(output, "DefaultPaneWrapper(scope, content)")
    }

    @Test
    fun `generates tab wrapper dispatch`() {
        // Given
        val wrappers = listOf(
            createTabWrapperInfo(
                functionName = "MainTabsWrapper",
                targetNodeKey = "mainTabs"
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "tabNodeKey.contains(\"mainTabs\")")
        assertContains(output, "MainTabsWrapper(scope, content)")
    }

    @Test
    fun `generates pane wrapper dispatch`() {
        // Given
        val wrappers = listOf(
            createPaneWrapperInfo(
                functionName = "CatalogPaneWrapper",
                targetNodeKey = "catalog"
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "paneNodeKey.contains(\"catalog\")")
        assertContains(output, "CatalogPaneWrapper(scope, content)")
    }

    @Test
    fun `generates hasTabWrapper function`() {
        // Given
        val wrappers = listOf(
            createTabWrapperInfo(functionName = "Wrapper", targetNodeKey = "main")
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "fun hasTabWrapper(tabNodeKey: String): Boolean")
        assertContains(output, "tabNodeKey.contains(\"main\") -> true")
    }

    @Test
    fun `imports all wrapper functions`() {
        // Given
        val wrappers = listOf(
            createTabWrapperInfo(
                functionName = "MainTabsWrapper",
                packageName = "com.example.wrappers"
            ),
            createPaneWrapperInfo(
                functionName = "DetailPaneWrapper",
                packageName = "com.example.panes"
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "import com.example.wrappers.MainTabsWrapper")
        assertContains(output, "import com.example.panes.DetailPaneWrapper")
    }

    @Test
    fun `handles multiple wrappers of same type`() {
        // Given
        val wrappers = listOf(
            createTabWrapperInfo(functionName = "MainTabsWrapper", targetNodeKey = "main"),
            createTabWrapperInfo(functionName = "SettingsTabsWrapper", targetNodeKey = "settings")
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(wrappers) }
        
        // Then
        assertContains(output, "tabNodeKey.contains(\"main\")")
        assertContains(output, "tabNodeKey.contains(\"settings\")")
    }
}
```

### Integration Test

```kotlin
@Test
fun `generated registry compiles and functions correctly`() {
    // Compile test source with wrappers
    val compilation = compile("""
        @Tab(name = "test", items = [])
        object TestTabs
        
        @TabWrapper(TestTabs::class)
        @Composable
        fun TestTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
            content()
        }
    """)
    
    // Verify generation succeeded
    assertTrue(compilation.success)
    
    // Verify generated class exists
    val generatedClass = compilation.classLoader
        .loadClass("com.jermey.quo.vadis.generated.GeneratedWrapperRegistry")
    assertNotNull(generatedClass)
    
    // Verify hasTabWrapper returns correct values
    val instance = generatedClass.kotlin.objectInstance as WrapperRegistry
    assertTrue(instance.hasTabWrapper("test"))
    assertFalse(instance.hasTabWrapper("unknown"))
}
```

---

## Acceptance Criteria

- [ ] `WrapperRegistryGenerator` class implemented
- [ ] Generates `GeneratedWrapperRegistry` object
- [ ] Implements `WrapperRegistry` interface
- [ ] `TabWrapper()` function generated with when-expression dispatch
- [ ] `PaneWrapper()` function generated with when-expression dispatch
- [ ] `hasTabWrapper()` function generated
- [ ] `hasPaneWrapper()` function generated
- [ ] Default fallback for tabs: `content()` (render without wrapper)
- [ ] Default fallback for panes: `DefaultPaneWrapper(scope, content)`
- [ ] All wrapper functions imported correctly
- [ ] Proper `@Composable` annotations on generated functions
- [ ] KDoc generated with wrapper summary
- [ ] Incremental compilation support via Dependencies
- [ ] `DefaultPaneWrapper` helper created in core module
- [ ] Unit tests for generator logic
- [ ] Integration test verifying generated code compiles

---

## Notes

### Key Matching Strategy

The generated code uses `contains()` for key matching rather than exact equality:

```kotlin
tabNodeKey.contains("mainTabs")  // vs tabNodeKey == "mainTabs"
```

This allows flexibility in key formats (e.g., `"root/mainTabs"` still matches `"mainTabs"`).

### Incremental Processing

The generator uses `Dependencies(aggregating = true, ...)` because the output depends on all wrapper declarations. Any change to a wrapper function triggers regeneration.

### Future Enhancements

1. **Priority/Ordering**: If multiple wrappers could match, add priority annotation parameter
2. **Generic Wrappers**: Support wrappers that apply to all tabs/panes of certain types
3. **Conditional Wrappers**: Runtime conditions for wrapper selection

---

## References

- [HIER-002](HIER-002-wrapper-registry.md) - WrapperRegistry interface (Phase 1)
- [HIER-012](HIER-012-ksp-wrapper-processor.md) - WrapperExtractor
- [ScreenRegistryGenerator.kt](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScreenRegistryGenerator.kt) - Similar generator pattern

````