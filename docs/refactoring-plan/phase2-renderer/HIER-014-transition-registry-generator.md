````markdown
# HIER-014: Transition Registry Generator

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-014 |
| **Task Name** | Generate TransitionRegistry Implementation |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-007 (TransitionRegistry interface), HIER-011 (@Transition annotation) |
| **Blocked By** | HIER-007, HIER-011 |
| **Blocks** | HIER-015 |

---

## Overview

This task creates the `TransitionRegistryGenerator` that produces the `GeneratedTransitionRegistry` implementation from `@Transition` annotations. The generated registry maps destination classes to their navigation transition configurations.

### Context

The transition registry enables per-destination animation customization:
- Developers annotate destinations with `@Transition(type = TransitionType.FADE)`
- KSP generates a registry mapping destination classes to `NavTransition` instances
- `AnimationCoordinator` queries the registry at runtime to resolve transitions
- Unannotated destinations use the coordinator's default transition

---

## File Locations

### Generator
```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/TransitionRegistryGenerator.kt
```

### Extractor (New)
```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TransitionExtractor.kt
```

### Model
```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TransitionInfo.kt
```

---

## Implementation

### TransitionInfo Model

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

/**
 * Extracted metadata from @Transition annotations.
 *
 * @property annotatedElement The KSP element annotated with @Transition (class or function)
 * @property destinationClass The destination class this transition applies to
 * @property destinationClassName ClassName for code generation
 * @property transitionType The TransitionType enum value
 * @property customTransitionClass Optional custom NavTransitionProvider class for CUSTOM type
 * @property customTransitionClassName Optional ClassName for custom provider
 */
data class TransitionInfo(
    val annotatedElement: KSAnnotated,
    val destinationClass: KSClassDeclaration,
    val destinationClassName: ClassName,
    val transitionType: TransitionType,
    val customTransitionClass: KSClassDeclaration?,
    val customTransitionClassName: ClassName?
)

/**
 * Mirror of the annotation TransitionType enum for KSP processing.
 */
enum class TransitionType {
    DEFAULT,
    SLIDE_HORIZONTAL,
    SLIDE_VERTICAL,
    FADE,
    NONE,
    SCALE,
    CUSTOM
}
```

### TransitionExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.TransitionType
import com.squareup.kotlinpoet.ClassName

/**
 * Extracts @Transition annotations into TransitionInfo models.
 *
 * Handles transitions defined on:
 * - @Destination-annotated classes
 * - @Screen-annotated functions
 *
 * @property logger KSP logger for error/warning output
 */
class TransitionExtractor(
    private val logger: KSPLogger
) {

    companion object {
        private const val TRANSITION_ANNOTATION = "com.jermey.quo.vadis.annotations.Transition"
        private const val SCREEN_ANNOTATION = "com.jermey.quo.vadis.annotations.Screen"
        private const val DESTINATION_ANNOTATION = "com.jermey.quo.vadis.annotations.Destination"
        private const val NAV_TRANSITION_PROVIDER = "com.jermey.quo.vadis.core.navigation.compose.animation.NavTransitionProvider"
    }

    /**
     * Extract all @Transition annotations from the resolver.
     *
     * Processes both class-level (on @Destination) and function-level (on @Screen) annotations.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of TransitionInfo for all valid @Transition annotations
     */
    fun extractAll(resolver: Resolver): List<TransitionInfo> {
        val transitions = mutableListOf<TransitionInfo>()

        // Process @Transition on classes (@Destination classes)
        resolver.getSymbolsWithAnnotation(TRANSITION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                extractFromClass(classDeclaration)?.let { transitions.add(it) }
            }

        // Process @Transition on functions (@Screen functions)
        resolver.getSymbolsWithAnnotation(TRANSITION_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { functionDeclaration ->
                extractFromFunction(functionDeclaration)?.let { transitions.add(it) }
            }

        return transitions
    }

    /**
     * Extract TransitionInfo from a @Transition-annotated class.
     */
    private fun extractFromClass(classDeclaration: KSClassDeclaration): TransitionInfo? {
        val annotation = findTransitionAnnotation(classDeclaration) ?: return null

        // Class itself is the destination
        if (!hasAnnotation(classDeclaration, DESTINATION_ANNOTATION)) {
            logger.warn(
                "@Transition on class should also have @Destination annotation",
                classDeclaration
            )
        }

        val transitionType = extractTransitionType(annotation)
        val customTransitionInfo = extractCustomTransition(annotation, classDeclaration, transitionType)

        return TransitionInfo(
            annotatedElement = classDeclaration,
            destinationClass = classDeclaration,
            destinationClassName = ClassName(
                classDeclaration.packageName.asString(),
                classDeclaration.simpleName.asString()
            ),
            transitionType = transitionType,
            customTransitionClass = customTransitionInfo?.first,
            customTransitionClassName = customTransitionInfo?.second
        )
    }

    /**
     * Extract TransitionInfo from a @Transition-annotated function (@Screen).
     */
    private fun extractFromFunction(functionDeclaration: KSFunctionDeclaration): TransitionInfo? {
        val transitionAnnotation = findTransitionAnnotation(functionDeclaration) ?: return null

        // Get destination class from @Screen annotation
        val screenAnnotation = functionDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SCREEN_ANNOTATION
        }

        if (screenAnnotation == null) {
            logger.warn(
                "@Transition on function should also have @Screen annotation",
                functionDeclaration
            )
            return null
        }

        val destinationType = screenAnnotation.arguments.firstOrNull()?.value as? KSType
        val destinationClass = destinationType?.declaration as? KSClassDeclaration
        if (destinationClass == null) {
            logger.error(
                "Could not resolve destination class from @Screen annotation",
                functionDeclaration
            )
            return null
        }

        val transitionType = extractTransitionType(transitionAnnotation)
        val customTransitionInfo = extractCustomTransition(transitionAnnotation, functionDeclaration, transitionType)

        return TransitionInfo(
            annotatedElement = functionDeclaration,
            destinationClass = destinationClass,
            destinationClassName = ClassName(
                destinationClass.packageName.asString(),
                destinationClass.simpleName.asString()
            ),
            transitionType = transitionType,
            customTransitionClass = customTransitionInfo?.first,
            customTransitionClassName = customTransitionInfo?.second
        )
    }

    /**
     * Find @Transition annotation on an element.
     */
    private fun findTransitionAnnotation(element: KSAnnotated) = element.annotations.find {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == TRANSITION_ANNOTATION
    }

    /**
     * Extract TransitionType from annotation.
     */
    private fun extractTransitionType(annotation: com.google.devtools.ksp.symbol.KSAnnotation): TransitionType {
        val typeValue = annotation.arguments.find { it.name?.asString() == "type" }?.value
        val typeString = typeValue?.toString() ?: "DEFAULT"
        
        // KSP returns enum as reference, extract the simple name
        val enumName = typeString.substringAfterLast(".")
        
        return try {
            TransitionType.valueOf(enumName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unknown TransitionType: $enumName, using DEFAULT")
            TransitionType.DEFAULT
        }
    }

    /**
     * Extract custom transition class if type is CUSTOM.
     */
    private fun extractCustomTransition(
        annotation: com.google.devtools.ksp.symbol.KSAnnotation,
        element: KSAnnotated,
        transitionType: TransitionType
    ): Pair<KSClassDeclaration, ClassName>? {
        if (transitionType != TransitionType.CUSTOM) return null

        val customType = annotation.arguments.find { 
            it.name?.asString() == "customTransition" 
        }?.value as? KSType

        val customClass = customType?.declaration as? KSClassDeclaration
        if (customClass == null || customClass.qualifiedName?.asString() == "kotlin.Unit") {
            logger.error(
                "@Transition with type=CUSTOM requires customTransition parameter",
                element
            )
            return null
        }

        // Validate custom class implements NavTransitionProvider
        val implementsProvider = customClass.superTypes.any {
            it.resolve().declaration.qualifiedName?.asString() == NAV_TRANSITION_PROVIDER
        }

        if (!implementsProvider) {
            logger.error(
                "customTransition '${customClass.simpleName.asString()}' must implement NavTransitionProvider",
                element
            )
            return null
        }

        return Pair(
            customClass,
            ClassName(customClass.packageName.asString(), customClass.simpleName.asString())
        )
    }

    /**
     * Check if an element has a specific annotation.
     */
    private fun hasAnnotation(element: KSAnnotated, annotationQualifiedName: String): Boolean {
        return element.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationQualifiedName
        }
    }
}
```

### TransitionRegistryGenerator

```kotlin
package com.jermey.quo.vadis.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.TransitionType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

/**
 * Generates the TransitionRegistry implementation from @Transition annotations.
 *
 * Produces a `GeneratedTransitionRegistry` object that:
 * - Implements the `TransitionRegistry` interface
 * - Maps destination classes to NavTransition instances
 * - Handles all TransitionType enum values
 * - Supports custom transition providers
 *
 * ## Generated Code Structure
 *
 * ```kotlin
 * object GeneratedTransitionRegistry : TransitionRegistry {
 *
 *     override fun getTransition(destinationClass: KClass<*>): NavTransition? {
 *         return when (destinationClass) {
 *             PhotoViewer::class -> NavTransition.Fade
 *             ModalScreen::class -> NavTransition.SlideVertical
 *             SpecialScreen::class -> MyCustomTransition.provide()
 *             else -> null
 *         }
 *     }
 * }
 * ```
 *
 * @property codeGenerator KSP code generator for file output
 * @property logger KSP logger for info/warning/error output
 */
class TransitionRegistryGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    companion object {
        private const val GENERATED_CLASS_NAME = "GeneratedTransitionRegistry"
        private const val GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"

        private val TRANSITION_REGISTRY = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.registry",
            "TransitionRegistry"
        )
        private val NAV_TRANSITION = ClassName(
            "com.jermey.quo.vadis.core.navigation.compose.animation",
            "NavTransition"
        )
        private val KCLASS = KClass::class.asClassName()
    }

    /**
     * Generate the TransitionRegistry implementation.
     *
     * @param transitions List of extracted TransitionInfo
     */
    fun generate(transitions: List<TransitionInfo>) {
        // Filter out DEFAULT type - those use coordinator's default
        val effectiveTransitions = transitions.filter { it.transitionType != TransitionType.DEFAULT }

        if (effectiveTransitions.isEmpty()) {
            logger.info("No @Transition annotations found (excluding DEFAULT), generating empty registry")
        } else {
            logger.info("Generating TransitionRegistry with ${effectiveTransitions.size} custom transitions")
        }

        val fileSpec = FileSpec.builder(GENERATED_PACKAGE, GENERATED_CLASS_NAME)
            .addType(buildRegistryObject(effectiveTransitions))
            .addImport("kotlin.reflect", "KClass")
            .apply {
                // Import destination classes
                effectiveTransitions.forEach { transition ->
                    addImport(
                        transition.destinationClassName.packageName,
                        transition.destinationClassName.simpleName
                    )
                }
                // Import custom transition providers
                effectiveTransitions
                    .filter { it.customTransitionClassName != null }
                    .forEach { transition ->
                        addImport(
                            transition.customTransitionClassName!!.packageName,
                            transition.customTransitionClassName.simpleName
                        )
                    }
            }
            .build()

        // Collect source files for incremental processing
        val dependencies = transitions.mapNotNull { info ->
            when (val element = info.annotatedElement) {
                is com.google.devtools.ksp.symbol.KSClassDeclaration -> element.containingFile
                is com.google.devtools.ksp.symbol.KSFunctionDeclaration -> element.containingFile
                else -> null
            }
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
     * Build the GeneratedTransitionRegistry object.
     */
    private fun buildRegistryObject(transitions: List<TransitionInfo>): TypeSpec {
        return TypeSpec.objectBuilder(GENERATED_CLASS_NAME)
            .addSuperinterface(TRANSITION_REGISTRY)
            .addKdoc(buildClassKDoc(transitions))
            .addFunction(buildGetTransitionFunction(transitions))
            .build()
    }

    /**
     * Build KDoc for the generated class.
     */
    private fun buildClassKDoc(transitions: List<TransitionInfo>): CodeBlock {
        return CodeBlock.builder()
            .addStatement("Generated TransitionRegistry implementation.")
            .addStatement("")
            .addStatement("Registered Transitions: ${transitions.size}")
            .apply {
                transitions.forEach { transition ->
                    val transitionDesc = when (transition.transitionType) {
                        TransitionType.CUSTOM -> "${transition.customTransitionClassName?.simpleName}.provide()"
                        else -> "NavTransition.${transition.transitionType.toNavTransitionName()}"
                    }
                    addStatement("  - ${transition.destinationClassName.simpleName} -> $transitionDesc")
                }
            }
            .addStatement("")
            .addStatement("@generated by Quo Vadis KSP Processor")
            .build()
    }

    /**
     * Build the getTransition function.
     *
     * ```kotlin
     * override fun getTransition(destinationClass: KClass<*>): NavTransition? {
     *     return when (destinationClass) {
     *         PhotoViewer::class -> NavTransition.Fade
     *         else -> null
     *     }
     * }
     * ```
     */
    private fun buildGetTransitionFunction(transitions: List<TransitionInfo>): FunSpec {
        val returnType = NAV_TRANSITION.copy(nullable = true)
        val paramType = KCLASS.parameterizedBy(STAR)

        return FunSpec.builder("getTransition")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("destinationClass", paramType)
            .returns(returnType)
            .addCode(buildGetTransitionBody(transitions))
            .build()
    }

    /**
     * Build the when expression for transition lookup.
     */
    private fun buildGetTransitionBody(transitions: List<TransitionInfo>): CodeBlock {
        if (transitions.isEmpty()) {
            return CodeBlock.of("return null\n")
        }

        return CodeBlock.builder()
            .beginControlFlow("return when (destinationClass)")
            .apply {
                transitions.forEach { transition ->
                    val transitionValue = buildTransitionValue(transition)
                    addStatement(
                        "%T::class -> %L",
                        transition.destinationClassName,
                        transitionValue
                    )
                }
            }
            .addStatement("else -> null")
            .endControlFlow()
            .build()
    }

    /**
     * Build the NavTransition value for a given transition type.
     */
    private fun buildTransitionValue(transition: TransitionInfo): CodeBlock {
        return when (transition.transitionType) {
            TransitionType.DEFAULT -> CodeBlock.of("null")
            TransitionType.SLIDE_HORIZONTAL -> CodeBlock.of("%T.SlideHorizontal", NAV_TRANSITION)
            TransitionType.SLIDE_VERTICAL -> CodeBlock.of("%T.SlideVertical", NAV_TRANSITION)
            TransitionType.FADE -> CodeBlock.of("%T.Fade", NAV_TRANSITION)
            TransitionType.NONE -> CodeBlock.of("%T.None", NAV_TRANSITION)
            TransitionType.SCALE -> CodeBlock.of("%T.Scale", NAV_TRANSITION)
            TransitionType.CUSTOM -> {
                if (transition.customTransitionClassName != null) {
                    CodeBlock.of("%T.provide()", transition.customTransitionClassName)
                } else {
                    logger.error("CUSTOM transition missing provider", transition.annotatedElement)
                    CodeBlock.of("null")
                }
            }
        }
    }

    /**
     * Convert TransitionType to NavTransition companion object name.
     */
    private fun TransitionType.toNavTransitionName(): String = when (this) {
        TransitionType.DEFAULT -> "Default"
        TransitionType.SLIDE_HORIZONTAL -> "SlideHorizontal"
        TransitionType.SLIDE_VERTICAL -> "SlideVertical"
        TransitionType.FADE -> "Fade"
        TransitionType.NONE -> "None"
        TransitionType.SCALE -> "Scale"
        TransitionType.CUSTOM -> "Custom"
    }
}
```

---

## Generated Code Example

Given these transition definitions:

```kotlin
@Transition(type = TransitionType.FADE)
@Destination(route = "photo/{photoId}")
data class PhotoViewer(val photoId: String) : GalleryDestination()

@Transition(type = TransitionType.SLIDE_VERTICAL)
@Screen(SettingsModal::class)
@Composable
fun SettingsModalScreen(destination: SettingsModal) { }

@Transition(type = TransitionType.CUSTOM, customTransition = BounceTransition::class)
@Destination(route = "celebration")
data object CelebrationScreen : AppDestinations()

object BounceTransition : NavTransitionProvider {
    override fun provide(): NavTransition = NavTransition(
        enter = fadeIn() + scaleIn(),
        exit = fadeOut(),
        popEnter = fadeIn(),
        popExit = fadeOut() + scaleOut()
    )
}
```

KSP generates:

```kotlin
package com.jermey.quo.vadis.generated

import com.example.app.BounceTransition
import com.example.app.CelebrationScreen
import com.example.app.PhotoViewer
import com.example.app.SettingsModal
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import kotlin.reflect.KClass

/**
 * Generated TransitionRegistry implementation.
 *
 * Registered Transitions: 3
 *   - PhotoViewer -> NavTransition.Fade
 *   - SettingsModal -> NavTransition.SlideVertical
 *   - CelebrationScreen -> BounceTransition.provide()
 *
 * @generated by Quo Vadis KSP Processor
 */
object GeneratedTransitionRegistry : TransitionRegistry {

    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return when (destinationClass) {
            PhotoViewer::class -> NavTransition.Fade
            SettingsModal::class -> NavTransition.SlideVertical
            CelebrationScreen::class -> BounceTransition.provide()
            else -> null
        }
    }
}
```

---

## Integration Points

### 1. Main Symbol Processor (HIER-015)

Update `QuoVadisSymbolProcessor`:

```kotlin
private val transitionExtractor = TransitionExtractor(logger)
private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)

private fun processTransitions(resolver: Resolver) {
    val transitions = transitionExtractor.extractAll(resolver)
    transitionRegistryGenerator.generate(transitions)
}
```

### 2. Runtime Usage (AnimationCoordinator)

The generated registry is used by `AnimationCoordinator`:

```kotlin
class AnimationCoordinator(
    private val transitionRegistry: TransitionRegistry = GeneratedTransitionRegistry,
    private val defaultTransition: NavTransition = NavTransition.SlideHorizontal
) {
    fun getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition {
        val toDestination = (to as? ScreenNode)?.destination
        val annotatedTransition = toDestination?.let { 
            transitionRegistry.getTransition(it::class)
        }
        return annotatedTransition ?: defaultTransition
    }
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class TransitionExtractorTest {

    @Test
    fun `extracts transition from destination class`() {
        // Given
        val compilation = compile("""
            @Transition(type = TransitionType.FADE)
            @Destination(route = "photo")
            data object PhotoScreen : AppDestinations()
        """)
        
        // When
        val transitions = extractor.extractAll(compilation.resolver)
        
        // Then
        assertEquals(1, transitions.size)
        assertEquals(TransitionType.FADE, transitions[0].transitionType)
        assertEquals("PhotoScreen", transitions[0].destinationClassName.simpleName)
    }

    @Test
    fun `extracts transition from screen function`() {
        // Given
        val compilation = compile("""
            @Destination(route = "settings")
            data object SettingsScreen : AppDestinations()
            
            @Transition(type = TransitionType.SLIDE_VERTICAL)
            @Screen(SettingsScreen::class)
            @Composable
            fun SettingsScreenComposable(destination: SettingsScreen) { }
        """)
        
        // When
        val transitions = extractor.extractAll(compilation.resolver)
        
        // Then
        assertEquals(1, transitions.size)
        assertEquals(TransitionType.SLIDE_VERTICAL, transitions[0].transitionType)
        assertEquals("SettingsScreen", transitions[0].destinationClassName.simpleName)
    }

    @Test
    fun `extracts custom transition provider`() {
        // Given
        val compilation = compile("""
            object CustomTransition : NavTransitionProvider {
                override fun provide() = NavTransition.Fade
            }
            
            @Transition(type = TransitionType.CUSTOM, customTransition = CustomTransition::class)
            @Destination(route = "special")
            data object SpecialScreen : AppDestinations()
        """)
        
        // When
        val transitions = extractor.extractAll(compilation.resolver)
        
        // Then
        assertEquals(1, transitions.size)
        assertEquals(TransitionType.CUSTOM, transitions[0].transitionType)
        assertEquals("CustomTransition", transitions[0].customTransitionClassName?.simpleName)
    }

    @Test
    fun `rejects CUSTOM without provider`() {
        // Given
        val compilation = compile("""
            @Transition(type = TransitionType.CUSTOM)  // Missing customTransition!
            @Destination(route = "special")
            data object SpecialScreen : AppDestinations()
        """)
        
        // When
        val transitions = extractor.extractAll(compilation.resolver)
        
        // Then
        assertTrue(transitions.isEmpty())
        verify(mockLogger).error(contains("requires customTransition"), any())
    }

    @Test
    fun `rejects custom class not implementing NavTransitionProvider`() {
        // Given
        val compilation = compile("""
            object NotAProvider  // Does not implement NavTransitionProvider
            
            @Transition(type = TransitionType.CUSTOM, customTransition = NotAProvider::class)
            @Destination(route = "special")
            data object SpecialScreen : AppDestinations()
        """)
        
        // When
        val transitions = extractor.extractAll(compilation.resolver)
        
        // Then
        assertTrue(transitions.isEmpty())
        verify(mockLogger).error(contains("must implement NavTransitionProvider"), any())
    }
}

class TransitionRegistryGeneratorTest {

    @Test
    fun `generates empty registry when no transitions`() {
        // Given
        val transitions = emptyList<TransitionInfo>()
        
        // When
        val output = captureGeneratedCode { generator.generate(transitions) }
        
        // Then
        assertContains(output, "object GeneratedTransitionRegistry")
        assertContains(output, "return null")
    }

    @Test
    fun `generates transition lookup for FADE`() {
        // Given
        val transitions = listOf(
            createTransitionInfo(
                destinationClassName = ClassName("com.example", "PhotoScreen"),
                transitionType = TransitionType.FADE
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(transitions) }
        
        // Then
        assertContains(output, "PhotoScreen::class -> NavTransition.Fade")
    }

    @Test
    fun `generates custom provider invocation`() {
        // Given
        val transitions = listOf(
            createTransitionInfo(
                destinationClassName = ClassName("com.example", "SpecialScreen"),
                transitionType = TransitionType.CUSTOM,
                customTransitionClassName = ClassName("com.example", "MyTransition")
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(transitions) }
        
        // Then
        assertContains(output, "SpecialScreen::class -> MyTransition.provide()")
    }

    @Test
    fun `skips DEFAULT transitions`() {
        // Given
        val transitions = listOf(
            createTransitionInfo(
                destinationClassName = ClassName("com.example", "DefaultScreen"),
                transitionType = TransitionType.DEFAULT
            )
        )
        
        // When
        val output = captureGeneratedCode { generator.generate(transitions) }
        
        // Then
        // DEFAULT is skipped, so only returns null
        assertContains(output, "return null")
        assertFalse(output.contains("DefaultScreen::class"))
    }
}
```

---

## Acceptance Criteria

- [ ] `TransitionInfo` model class created
- [ ] `TransitionType` enum (KSP model) created
- [ ] `TransitionExtractor` class implemented
- [ ] Extracts transitions from `@Destination` classes
- [ ] Extracts transitions from `@Screen` functions
- [ ] Validates CUSTOM type requires customTransition parameter
- [ ] Validates customTransition implements `NavTransitionProvider`
- [ ] `TransitionRegistryGenerator` class implemented
- [ ] Generates `GeneratedTransitionRegistry` object
- [ ] Implements `TransitionRegistry` interface
- [ ] `getTransition()` function generated with when-expression
- [ ] Maps to `NavTransition.{Type}` for built-in types
- [ ] Invokes `{Provider}.provide()` for custom types
- [ ] Returns `null` for unregistered destinations
- [ ] Skips `DEFAULT` type transitions in generation
- [ ] KDoc generated with transition summary
- [ ] Unit tests for extractor
- [ ] Unit tests for generator

---

## Notes

### Priority: Screen vs Class Annotation

When both a destination class and its screen function have `@Transition`:
1. The extractor collects both
2. Since they map to the same destination class, the last one processed wins
3. In practice, this means function-level annotations take precedence

Consider adding deduplication with explicit priority if this becomes an issue.

### TransitionType Enum Duplication

`TransitionType` exists in both:
- `quo-vadis-annotations` (annotation enum)
- `quo-vadis-ksp/models` (KSP processing model)

This is intentional to avoid KSP dependencies in the annotations module.

---

## References

- [HIER-007](HIER-007-transition-registry.md) - TransitionRegistry interface (Phase 1)
- [HIER-011](HIER-011-transition-annotation.md) - @Transition annotation
- [HIER-003](HIER-003-nav-transition.md) - NavTransition data class (Phase 1)

````