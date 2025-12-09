````markdown
# HIER-012: KSP Wrapper Processor

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-012 |
| **Task Name** | Create WrapperProcessor for Processing @TabWrapper and @PaneWrapper |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-009 (@TabWrapper), HIER-010 (@PaneWrapper) |
| **Blocked By** | HIER-009, HIER-010 |
| **Blocks** | HIER-013 |

---

## Overview

This task creates the `WrapperProcessor` component for the KSP processor. It extracts `@TabWrapper` and `@PaneWrapper` annotated functions, validates their signatures, and produces `WrapperInfo` models for code generation.

### Context

The wrapper processor follows the established extractor pattern in the Quo Vadis KSP architecture:
- Extractors parse annotations into strongly-typed models
- Generators consume models to produce code
- This separation enables testing and reusability

### Responsibilities

1. Find all `@TabWrapper` and `@PaneWrapper` annotated functions
2. Validate function signatures match expected patterns
3. Validate target classes have appropriate annotations (`@Tab` or `@Pane`)
4. Extract wrapper metadata into `WrapperInfo` models
5. Report clear errors for invalid usage

---

## File Locations

### Processor
```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/WrapperExtractor.kt
```

### Model
```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/WrapperInfo.kt
```

---

## Implementation

### WrapperInfo Model

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName

/**
 * Type of wrapper (tab or pane).
 */
enum class WrapperType {
    TAB,
    PANE
}

/**
 * Extracted metadata from @TabWrapper or @PaneWrapper annotations.
 *
 * This model contains all information needed by [WrapperRegistryGenerator]
 * to generate the wrapper dispatch code.
 *
 * @property wrapperType Whether this is a tab or pane wrapper
 * @property functionDeclaration The KSP function declaration for this wrapper
 * @property functionName Simple function name (e.g., "MainTabsWrapper")
 * @property packageName Package containing this function
 * @property qualifiedFunctionName Fully qualified function name for imports
 * @property targetClass The @Tab or @Pane class this wrapper is for
 * @property targetClassName ClassName for the target class (for code generation)
 * @property targetClassSimpleName Simple name of target class (e.g., "MainTabs")
 * @property targetNodeKey The key pattern used in NavNode for this target
 */
data class WrapperInfo(
    val wrapperType: WrapperType,
    val functionDeclaration: KSFunctionDeclaration,
    val functionName: String,
    val packageName: String,
    val qualifiedFunctionName: String,
    val targetClass: KSClassDeclaration,
    val targetClassName: ClassName,
    val targetClassSimpleName: String,
    val targetNodeKey: String
)
```

### WrapperExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.jermey.quo.vadis.ksp.models.WrapperType
import com.squareup.kotlinpoet.ClassName

/**
 * Extracts @TabWrapper and @PaneWrapper annotations into WrapperInfo models.
 *
 * This extractor handles:
 * - Finding annotated wrapper functions
 * - Validating function signatures
 * - Validating target class annotations
 * - Extracting wrapper metadata
 *
 * @property logger KSP logger for error/warning output
 */
class WrapperExtractor(
    private val logger: KSPLogger
) {

    companion object {
        private const val TAB_WRAPPER_ANNOTATION = "com.jermey.quo.vadis.annotations.TabWrapper"
        private const val PANE_WRAPPER_ANNOTATION = "com.jermey.quo.vadis.annotations.PaneWrapper"
        private const val TAB_ANNOTATION = "com.jermey.quo.vadis.annotations.Tab"
        private const val PANE_ANNOTATION = "com.jermey.quo.vadis.annotations.Pane"
        
        private const val TAB_WRAPPER_SCOPE = "TabWrapperScope"
        private const val PANE_WRAPPER_SCOPE = "PaneWrapperScope"
        private const val PANE_CONTENT_SCOPE = "PaneContentScope"
    }

    /**
     * Extract all @TabWrapper annotated functions from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of WrapperInfo for all valid @TabWrapper functions
     */
    fun extractTabWrappers(resolver: Resolver): List<WrapperInfo> {
        return resolver.getSymbolsWithAnnotation(TAB_WRAPPER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extractTabWrapper(it) }
            .toList()
    }

    /**
     * Extract all @PaneWrapper annotated functions from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of WrapperInfo for all valid @PaneWrapper functions
     */
    fun extractPaneWrappers(resolver: Resolver): List<WrapperInfo> {
        return resolver.getSymbolsWithAnnotation(PANE_WRAPPER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extractPaneWrapper(it) }
            .toList()
    }

    /**
     * Extract all wrapper functions (both tab and pane) from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of all WrapperInfo
     */
    fun extractAll(resolver: Resolver): List<WrapperInfo> {
        return extractTabWrappers(resolver) + extractPaneWrappers(resolver)
    }

    /**
     * Extract WrapperInfo from a @TabWrapper annotated function.
     */
    private fun extractTabWrapper(functionDeclaration: KSFunctionDeclaration): WrapperInfo? {
        val annotation = functionDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == TAB_WRAPPER_ANNOTATION
        } ?: return null

        // Validate function signature
        if (!validateTabWrapperSignature(functionDeclaration)) {
            return null
        }

        // Get target class from annotation
        val tabClassType = annotation.arguments.find {
            it.name?.asString() == "tabClass"
        }?.value as? KSType

        val targetClass = tabClassType?.declaration as? KSClassDeclaration
        if (targetClass == null) {
            logger.error(
                "@TabWrapper: Could not resolve tabClass",
                functionDeclaration
            )
            return null
        }

        // Validate target class has @Tab annotation
        if (!hasAnnotation(targetClass, TAB_ANNOTATION)) {
            logger.error(
                "@TabWrapper: tabClass '${targetClass.simpleName.asString()}' must be annotated with @Tab",
                functionDeclaration
            )
            return null
        }

        // Extract tab name for node key
        val tabName = extractTabName(targetClass)

        return WrapperInfo(
            wrapperType = WrapperType.TAB,
            functionDeclaration = functionDeclaration,
            functionName = functionDeclaration.simpleName.asString(),
            packageName = functionDeclaration.packageName.asString(),
            qualifiedFunctionName = "${functionDeclaration.packageName.asString()}.${functionDeclaration.simpleName.asString()}",
            targetClass = targetClass,
            targetClassName = ClassName(
                targetClass.packageName.asString(),
                targetClass.simpleName.asString()
            ),
            targetClassSimpleName = targetClass.simpleName.asString(),
            targetNodeKey = tabName ?: targetClass.simpleName.asString().lowercase()
        )
    }

    /**
     * Extract WrapperInfo from a @PaneWrapper annotated function.
     */
    private fun extractPaneWrapper(functionDeclaration: KSFunctionDeclaration): WrapperInfo? {
        val annotation = functionDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == PANE_WRAPPER_ANNOTATION
        } ?: return null

        // Validate function signature
        if (!validatePaneWrapperSignature(functionDeclaration)) {
            return null
        }

        // Get target class from annotation
        val paneClassType = annotation.arguments.find {
            it.name?.asString() == "paneClass"
        }?.value as? KSType

        val targetClass = paneClassType?.declaration as? KSClassDeclaration
        if (targetClass == null) {
            logger.error(
                "@PaneWrapper: Could not resolve paneClass",
                functionDeclaration
            )
            return null
        }

        // Validate target class has @Pane annotation
        if (!hasAnnotation(targetClass, PANE_ANNOTATION)) {
            logger.error(
                "@PaneWrapper: paneClass '${targetClass.simpleName.asString()}' must be annotated with @Pane",
                functionDeclaration
            )
            return null
        }

        // Extract pane name for node key
        val paneName = extractPaneName(targetClass)

        return WrapperInfo(
            wrapperType = WrapperType.PANE,
            functionDeclaration = functionDeclaration,
            functionName = functionDeclaration.simpleName.asString(),
            packageName = functionDeclaration.packageName.asString(),
            qualifiedFunctionName = "${functionDeclaration.packageName.asString()}.${functionDeclaration.simpleName.asString()}",
            targetClass = targetClass,
            targetClassName = ClassName(
                targetClass.packageName.asString(),
                targetClass.simpleName.asString()
            ),
            targetClassSimpleName = targetClass.simpleName.asString(),
            targetNodeKey = paneName ?: targetClass.simpleName.asString().lowercase()
        )
    }

    /**
     * Validate that the function has correct signature for @TabWrapper.
     *
     * Expected signature:
     * ```
     * @Composable
     * fun WrapperName(
     *     scope: TabWrapperScope,
     *     content: @Composable () -> Unit
     * )
     * ```
     */
    private fun validateTabWrapperSignature(function: KSFunctionDeclaration): Boolean {
        val parameters = function.parameters

        // Must have exactly 2 parameters
        if (parameters.size != 2) {
            logger.error(
                "@TabWrapper function must have exactly 2 parameters: TabWrapperScope and @Composable () -> Unit content",
                function
            )
            return false
        }

        // First parameter must be TabWrapperScope
        val firstParam = parameters[0]
        val firstParamType = firstParam.type.resolve()
        val firstParamTypeName = firstParamType.declaration.simpleName.asString()
        
        if (firstParamTypeName != TAB_WRAPPER_SCOPE) {
            logger.error(
                "@TabWrapper: First parameter must be TabWrapperScope, found: $firstParamTypeName",
                function
            )
            return false
        }

        // Second parameter must be a function type (content lambda)
        val secondParam = parameters[1]
        val secondParamType = secondParam.type.resolve()
        
        if (!isFunctionType(secondParamType)) {
            logger.error(
                "@TabWrapper: Second parameter must be a function type (@Composable () -> Unit)",
                function
            )
            return false
        }

        // Function should be @Composable (we can check for Composable annotation)
        val hasComposable = function.annotations.any {
            it.shortName.asString() == "Composable"
        }
        
        if (!hasComposable) {
            logger.warn(
                "@TabWrapper function should be annotated with @Composable",
                function
            )
        }

        return true
    }

    /**
     * Validate that the function has correct signature for @PaneWrapper.
     *
     * Expected signature:
     * ```
     * @Composable
     * fun WrapperName(
     *     scope: PaneWrapperScope,
     *     content: @Composable PaneContentScope.() -> Unit
     * )
     * ```
     */
    private fun validatePaneWrapperSignature(function: KSFunctionDeclaration): Boolean {
        val parameters = function.parameters

        // Must have exactly 2 parameters
        if (parameters.size != 2) {
            logger.error(
                "@PaneWrapper function must have exactly 2 parameters: PaneWrapperScope and @Composable PaneContentScope.() -> Unit content",
                function
            )
            return false
        }

        // First parameter must be PaneWrapperScope
        val firstParam = parameters[0]
        val firstParamType = firstParam.type.resolve()
        val firstParamTypeName = firstParamType.declaration.simpleName.asString()
        
        if (firstParamTypeName != PANE_WRAPPER_SCOPE) {
            logger.error(
                "@PaneWrapper: First parameter must be PaneWrapperScope, found: $firstParamTypeName",
                function
            )
            return false
        }

        // Second parameter must be a function type with PaneContentScope receiver
        val secondParam = parameters[1]
        val secondParamType = secondParam.type.resolve()
        
        if (!isFunctionType(secondParamType)) {
            logger.error(
                "@PaneWrapper: Second parameter must be a function type",
                function
            )
            return false
        }

        // Check for PaneContentScope receiver (extension function type)
        val hasCorrectReceiver = validatePaneContentScopeReceiver(secondParamType)
        if (!hasCorrectReceiver) {
            logger.error(
                "@PaneWrapper: Content parameter must have PaneContentScope receiver type",
                function
            )
            return false
        }

        // Function should be @Composable
        val hasComposable = function.annotations.any {
            it.shortName.asString() == "Composable"
        }
        
        if (!hasComposable) {
            logger.warn(
                "@PaneWrapper function should be annotated with @Composable",
                function
            )
        }

        return true
    }

    /**
     * Check if a type is a function type (lambda).
     */
    private fun isFunctionType(type: KSType): Boolean {
        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString() ?: ""
        return qualifiedName.startsWith("kotlin.Function") ||
               qualifiedName.startsWith("kotlin.coroutines.") ||
               qualifiedName.contains("Function")
    }

    /**
     * Validate that a function type has PaneContentScope as receiver.
     *
     * For extension function types like `PaneContentScope.() -> Unit`,
     * the receiver type appears as the first argument of the Function type.
     */
    private fun validatePaneContentScopeReceiver(functionType: KSType): Boolean {
        val arguments = functionType.arguments
        if (arguments.isEmpty()) return false
        
        // Extension functions have the receiver as first type argument
        val firstArg = arguments.firstOrNull()?.type?.resolve() ?: return false
        val receiverTypeName = firstArg.declaration.simpleName.asString()
        
        return receiverTypeName == PANE_CONTENT_SCOPE
    }

    /**
     * Check if a class has a specific annotation.
     */
    private fun hasAnnotation(classDeclaration: KSClassDeclaration, annotationQualifiedName: String): Boolean {
        return classDeclaration.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationQualifiedName
        }
    }

    /**
     * Extract the 'name' parameter from @Tab annotation.
     */
    private fun extractTabName(classDeclaration: KSClassDeclaration): String? {
        val tabAnnotation = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == TAB_ANNOTATION
        } ?: return null

        return tabAnnotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String
    }

    /**
     * Extract the 'name' parameter from @Pane annotation.
     */
    private fun extractPaneName(classDeclaration: KSClassDeclaration): String? {
        val paneAnnotation = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == PANE_ANNOTATION
        } ?: return null

        return paneAnnotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String
    }
}
```

---

## Integration Points

### 1. Main Symbol Processor

Update `QuoVadisSymbolProcessor` to include wrapper extraction:

```kotlin
// In QuoVadisSymbolProcessor.kt

private val wrapperExtractor = WrapperExtractor(logger)

private fun processWrappers(resolver: Resolver) {
    val wrappers = wrapperExtractor.extractAll(resolver)
    
    if (wrappers.isNotEmpty()) {
        wrapperRegistryGenerator.generate(wrappers)
    }
}
```

### 2. Registry Generation (HIER-013)

The `WrapperRegistryGenerator` consumes `WrapperInfo` models to generate the registry code.

### 3. Validation Engine (Optional Enhancement)

Can be integrated with existing `ValidationEngine` to cross-validate:
- Each `@Tab` class has at most one `@TabWrapper`
- Each `@Pane` class has at most one `@PaneWrapper`

---

## Testing Requirements

### Unit Tests

```kotlin
class WrapperExtractorTest {

    private lateinit var extractor: WrapperExtractor
    private lateinit var mockLogger: KSPLogger

    @Before
    fun setup() {
        mockLogger = mock()
        extractor = WrapperExtractor(mockLogger)
    }

    @Test
    fun `extracts valid tab wrapper`() {
        // Given a compilation with valid @TabWrapper
        val compilation = compile("""
            @Tab(name = "main", items = [])
            object MainTabs
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun MainTabsWrapper(
                scope: TabWrapperScope,
                content: @Composable () -> Unit
            ) { content() }
        """)
        
        // When
        val wrappers = extractor.extractTabWrappers(compilation.resolver)
        
        // Then
        assertEquals(1, wrappers.size)
        val wrapper = wrappers.first()
        assertEquals(WrapperType.TAB, wrapper.wrapperType)
        assertEquals("MainTabsWrapper", wrapper.functionName)
        assertEquals("MainTabs", wrapper.targetClassSimpleName)
        assertEquals("main", wrapper.targetNodeKey)
    }

    @Test
    fun `extracts valid pane wrapper`() {
        // Given a compilation with valid @PaneWrapper
        val compilation = compile("""
            @Pane(name = "catalog")
            sealed class CatalogPane : Destination
            
            @PaneWrapper(CatalogPane::class)
            @Composable
            fun CatalogPaneWrapper(
                scope: PaneWrapperScope,
                content: @Composable PaneContentScope.() -> Unit
            ) { }
        """)
        
        // When
        val wrappers = extractor.extractPaneWrappers(compilation.resolver)
        
        // Then
        assertEquals(1, wrappers.size)
        val wrapper = wrappers.first()
        assertEquals(WrapperType.PANE, wrapper.wrapperType)
        assertEquals("CatalogPaneWrapper", wrapper.functionName)
        assertEquals("CatalogPane", wrapper.targetClassSimpleName)
        assertEquals("catalog", wrapper.targetNodeKey)
    }

    @Test
    fun `rejects tab wrapper with wrong first parameter`() {
        // Given
        val compilation = compile("""
            @Tab(name = "main", items = [])
            object MainTabs
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun BadWrapper(
                scope: String,  // Wrong type!
                content: @Composable () -> Unit
            ) { }
        """)
        
        // When
        val wrappers = extractor.extractTabWrappers(compilation.resolver)
        
        // Then
        assertTrue(wrappers.isEmpty())
        verify(mockLogger).error(contains("First parameter must be TabWrapperScope"), any())
    }

    @Test
    fun `rejects tab wrapper with too many parameters`() {
        // Given
        val compilation = compile("""
            @TabWrapper(MainTabs::class)
            @Composable
            fun BadWrapper(
                scope: TabWrapperScope,
                content: @Composable () -> Unit,
                extra: Int  // Extra parameter!
            ) { }
        """)
        
        // When
        val wrappers = extractor.extractTabWrappers(compilation.resolver)
        
        // Then
        assertTrue(wrappers.isEmpty())
        verify(mockLogger).error(contains("must have exactly 2 parameters"), any())
    }

    @Test
    fun `rejects wrapper for non-Tab class`() {
        // Given
        val compilation = compile("""
            @Stack(name = "home", startDestination = "Feed")
            sealed class HomeDestination : Destination
            
            @TabWrapper(HomeDestination::class)  // Not a @Tab!
            @Composable
            fun BadWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { }
        """)
        
        // When
        val wrappers = extractor.extractTabWrappers(compilation.resolver)
        
        // Then
        assertTrue(wrappers.isEmpty())
        verify(mockLogger).error(contains("must be annotated with @Tab"), any())
    }

    @Test
    fun `rejects pane wrapper without PaneContentScope receiver`() {
        // Given
        val compilation = compile("""
            @Pane(name = "catalog")
            sealed class CatalogPane : Destination
            
            @PaneWrapper(CatalogPane::class)
            @Composable
            fun BadWrapper(
                scope: PaneWrapperScope,
                content: @Composable () -> Unit  // Missing PaneContentScope receiver!
            ) { }
        """)
        
        // When
        val wrappers = extractor.extractPaneWrappers(compilation.resolver)
        
        // Then
        assertTrue(wrappers.isEmpty())
        verify(mockLogger).error(contains("PaneContentScope receiver"), any())
    }

    @Test
    fun `extractAll returns both tab and pane wrappers`() {
        // Given
        val compilation = compile("""
            @Tab(name = "main", items = [])
            object MainTabs
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { }
            
            @Pane(name = "catalog")
            sealed class CatalogPane : Destination
            
            @PaneWrapper(CatalogPane::class)
            @Composable
            fun CatalogPaneWrapper(
                scope: PaneWrapperScope,
                content: @Composable PaneContentScope.() -> Unit
            ) { }
        """)
        
        // When
        val wrappers = extractor.extractAll(compilation.resolver)
        
        // Then
        assertEquals(2, wrappers.size)
        assertTrue(wrappers.any { it.wrapperType == WrapperType.TAB })
        assertTrue(wrappers.any { it.wrapperType == WrapperType.PANE })
    }
}
```

---

## Acceptance Criteria

- [ ] `WrapperInfo` data class created with all required properties
- [ ] `WrapperType` enum created with TAB and PANE values
- [ ] `WrapperExtractor` class implemented
- [ ] `extractTabWrappers(Resolver)` extracts all @TabWrapper functions
- [ ] `extractPaneWrappers(Resolver)` extracts all @PaneWrapper functions
- [ ] `extractAll(Resolver)` returns combined list
- [ ] Tab wrapper signature validation:
  - [ ] Exactly 2 parameters
  - [ ] First parameter is `TabWrapperScope`
  - [ ] Second parameter is function type
- [ ] Pane wrapper signature validation:
  - [ ] Exactly 2 parameters
  - [ ] First parameter is `PaneWrapperScope`
  - [ ] Second parameter is function type with `PaneContentScope` receiver
- [ ] Target class validation:
  - [ ] Tab wrapper target has `@Tab` annotation
  - [ ] Pane wrapper target has `@Pane` annotation
- [ ] Node key extraction from `@Tab`/`@Pane` name parameter
- [ ] Clear error messages for all validation failures
- [ ] Unit tests with >80% coverage
- [ ] Integration with `QuoVadisSymbolProcessor`

---

## Notes

### Error Message Guidelines

Error messages should:
1. State what was expected
2. State what was found
3. Reference the problematic symbol

Example:
```
@TabWrapper: First parameter must be TabWrapperScope, found: String
```

### KSP Quirks

1. **Function Type Detection**: KSP represents function types with qualified names like `kotlin.Function1`. Extension function receivers appear as the first type argument.

2. **Annotation Resolution**: Use `annotationType.resolve()` to get the actual type declaration for comparison.

3. **Composable Detection**: The `@Composable` annotation may not always be directly visible; it's applied via compiler plugin. We warn but don't error.

---

## References

- [KSP-001-graph-type-enum.md](../phase3-ksp/KSP-001-graph-type-enum.md) - Extractor pattern reference
- [QuoVadisSymbolProcessor.kt](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt) - Main processor
- [ScreenExtractor.kt](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/ScreenExtractor.kt) - Similar extractor pattern

````