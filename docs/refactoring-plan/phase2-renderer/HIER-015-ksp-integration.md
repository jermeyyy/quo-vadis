````markdown
# HIER-015: KSP Integration

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-015 |
| **Task Name** | Integrate Wrapper and Transition Processors into Main KSP Pipeline |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Small |
| **Estimated Time** | 1 day |
| **Dependencies** | HIER-012 (WrapperExtractor), HIER-013 (WrapperRegistryGenerator), HIER-014 (TransitionRegistryGenerator) |
| **Blocked By** | HIER-012, HIER-013, HIER-014 |
| **Blocks** | Phase 3 (Renderer Implementation) |

---

## Overview

This task integrates the new wrapper and transition processors into the main `QuoVadisSymbolProcessor`. It ensures proper ordering of extraction and generation, handles dependencies between processors, and maintains backward compatibility with existing code generation.

### Context

The Quo Vadis KSP processor already generates:
- `NavNode` builders for `@Stack`, `@Tab`, `@Pane` annotations
- `ScreenRegistry` for `@Screen` functions
- `DeepLinkHandler` for route matching
- `NavigatorExtensions` for type-safe navigation

This task adds:
- `WrapperRegistry` for `@TabWrapper` and `@PaneWrapper` functions
- `TransitionRegistry` for `@Transition` annotations

---

## File Location

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt
```

---

## Implementation

### Updated QuoVadisSymbolProcessor

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Pane
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.ksp.extractors.DestinationExtractor
import com.jermey.quo.vadis.ksp.extractors.PaneExtractor
import com.jermey.quo.vadis.ksp.extractors.ScreenExtractor
import com.jermey.quo.vadis.ksp.extractors.StackExtractor
import com.jermey.quo.vadis.ksp.extractors.TabExtractor
import com.jermey.quo.vadis.ksp.extractors.TransitionExtractor
import com.jermey.quo.vadis.ksp.extractors.WrapperExtractor
import com.jermey.quo.vadis.ksp.generators.DeepLinkHandlerGenerator
import com.jermey.quo.vadis.ksp.generators.NavNodeBuilderGenerator
import com.jermey.quo.vadis.ksp.generators.NavigatorExtGenerator
import com.jermey.quo.vadis.ksp.generators.ScreenRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.TransitionRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.WrapperRegistryGenerator
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.validation.ValidationEngine

/**
 * KSP processor for Quo Vadis navigation annotations.
 *
 * Processes the NavNode-based architecture annotations and generates:
 * - NavNode builders for @Stack, @Tab, @Pane containers
 * - ScreenRegistry for @Screen composable bindings
 * - WrapperRegistry for @TabWrapper and @PaneWrapper functions (NEW)
 * - TransitionRegistry for @Transition annotations (NEW)
 * - DeepLinkHandler for URI pattern matching
 * - NavigatorExtensions for type-safe navigation
 *
 * ## Processing Order
 *
 * 1. **Extraction Phase** (collect all annotation data)
 *    - Extract @Stack, @Tab, @Pane containers
 *    - Extract @Screen functions
 *    - Extract @TabWrapper, @PaneWrapper functions
 *    - Extract @Transition annotations
 *
 * 2. **Validation Phase** (cross-validate extracted data)
 *    - Validate container relationships
 *    - Validate screen bindings
 *    - Validate wrapper targets
 *    - Validate transition providers
 *
 * 3. **Generation Phase** (produce output files)
 *    - Generate NavNode builders
 *    - Generate ScreenRegistry
 *    - Generate WrapperRegistry
 *    - Generate TransitionRegistry
 *    - Generate DeepLinkHandler
 *    - Generate NavigatorExtensions
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    // =========================================================================
    // Extractors
    // =========================================================================

    // Existing extractors
    private val destinationExtractor = DestinationExtractor(logger)
    private val stackExtractor = StackExtractor(destinationExtractor, logger)
    private val tabExtractor = TabExtractor(destinationExtractor, logger)
    private val paneExtractor = PaneExtractor(destinationExtractor, logger)
    private val screenExtractor = ScreenExtractor(logger)

    // NEW: Hierarchical rendering extractors
    private val wrapperExtractor = WrapperExtractor(logger)
    private val transitionExtractor = TransitionExtractor(logger)

    // =========================================================================
    // Generators
    // =========================================================================

    // Existing generators
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)
    private val deepLinkHandlerGenerator = DeepLinkHandlerGenerator(codeGenerator, logger)
    private val navigatorExtGenerator = NavigatorExtGenerator(codeGenerator, logger)

    // NEW: Hierarchical rendering generators
    private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)
    private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)

    // Validation engine
    private val validationEngine = ValidationEngine(logger)

    // =========================================================================
    // State
    // =========================================================================

    private val stackInfoMap = mutableMapOf<String, StackInfo>()
    private val collectedStacks = mutableListOf<StackInfo>()
    private val collectedTabs = mutableListOf<TabInfo>()
    private val collectedPanes = mutableListOf<PaneInfo>()

    private var hasGenerated = false

    // =========================================================================
    // Processing
    // =========================================================================

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasGenerated) {
            return emptyList()
        }

        logger.info("QuoVadisSymbolProcessor: Starting processing")

        // =====================================================================
        // Phase 1: Extraction
        // =====================================================================

        // Extract containers (existing logic)
        extractContainers(resolver)

        // Extract screens
        val screens = screenExtractor.extractAll(resolver)
        logger.info("Extracted ${screens.size} @Screen functions")

        // NEW: Extract wrappers
        val wrappers = wrapperExtractor.extractAll(resolver)
        logger.info("Extracted ${wrappers.size} wrapper functions (${wrappers.count { it.wrapperType == com.jermey.quo.vadis.ksp.models.WrapperType.TAB }} tab, ${wrappers.count { it.wrapperType == com.jermey.quo.vadis.ksp.models.WrapperType.PANE }} pane)")

        // NEW: Extract transitions
        val transitions = transitionExtractor.extractAll(resolver)
        logger.info("Extracted ${transitions.size} @Transition annotations")

        // Collect all destinations for validation
        val allDestinations = collectAllDestinations()

        // =====================================================================
        // Phase 2: Validation
        // =====================================================================

        val isValid = validationEngine.validate(
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            screens = screens,
            allDestinations = allDestinations,
            resolver = resolver
        )

        if (!isValid) {
            logger.error("Validation failed - skipping code generation")
            return emptyList()
        }

        // =====================================================================
        // Phase 3: Generation
        // =====================================================================

        // Generate existing artifacts
        generateNavNodeBuilders()
        generateScreenRegistry(screens)
        generateDeepLinkHandler(resolver)
        generateNavigatorExtensions()

        // NEW: Generate wrapper registry
        generateWrapperRegistry(wrappers)

        // NEW: Generate transition registry
        generateTransitionRegistry(transitions)

        hasGenerated = true
        logger.info("QuoVadisSymbolProcessor: Processing complete")

        return emptyList()
    }

    // =========================================================================
    // Extraction Helpers (Existing - unchanged)
    // =========================================================================

    private fun extractContainers(resolver: Resolver) {
        // Stack extraction
        resolver.getSymbolsWithAnnotation(Stack::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { extractStackInfo(it) }

        // Tab extraction (with TabItem cache)
        tabExtractor.populateTabItemCache(resolver)
        resolver.getSymbolsWithAnnotation(Tab::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { extractTabInfo(it) }

        // Pane extraction
        resolver.getSymbolsWithAnnotation(Pane::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { extractPaneInfo(it) }

        logger.info("Extracted ${collectedStacks.size} stacks, ${collectedTabs.size} tabs, ${collectedPanes.size} panes")
    }

    private fun extractStackInfo(classDeclaration: KSClassDeclaration) {
        val stackInfo = stackExtractor.extract(classDeclaration) ?: return
        val qualifiedName = classDeclaration.qualifiedName?.asString() ?: stackInfo.className
        stackInfoMap[qualifiedName] = stackInfo
        collectedStacks.add(stackInfo)
    }

    private fun extractTabInfo(classDeclaration: KSClassDeclaration) {
        val tabInfo = tabExtractor.extract(classDeclaration) ?: return
        collectedTabs.add(tabInfo)
    }

    private fun extractPaneInfo(classDeclaration: KSClassDeclaration) {
        val paneInfo = paneExtractor.extract(classDeclaration) ?: return
        collectedPanes.add(paneInfo)
    }

    private fun collectAllDestinations(): List<DestinationInfo> {
        val destinations = mutableListOf<DestinationInfo>()
        collectedStacks.forEach { destinations.addAll(it.destinations) }
        collectedTabs.forEach { tab -> destinations.addAll(tab.tabs.mapNotNull { it.destination }) }
        collectedPanes.forEach { pane -> destinations.addAll(pane.panes.map { it.destination }) }
        return destinations
    }

    // =========================================================================
    // Generation Helpers (Existing - unchanged)
    // =========================================================================

    private fun generateNavNodeBuilders() {
        collectedStacks.forEach { navNodeBuilderGenerator.generateStackBuilder(it) }
        collectedTabs.forEach { navNodeBuilderGenerator.generateTabBuilder(it, stackInfoMap) }
        collectedPanes.forEach { navNodeBuilderGenerator.generatePaneBuilder(it) }
    }

    private fun generateScreenRegistry(screens: List<ScreenInfo>) {
        screenRegistryGenerator.generate(screens)
    }

    private fun generateDeepLinkHandler(resolver: Resolver) {
        val destinations = resolver.getSymbolsWithAnnotation(Destination::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { destinationExtractor.extract(it) }
            .toList()
        deepLinkHandlerGenerator.generate(destinations)
    }

    private fun generateNavigatorExtensions() {
        if (collectedStacks.isEmpty() && collectedTabs.isEmpty() && collectedPanes.isEmpty()) return

        val basePackage = collectedStacks.firstOrNull()?.packageName
            ?: collectedTabs.firstOrNull()?.packageName
            ?: collectedPanes.firstOrNull()?.packageName
            ?: return

        navigatorExtGenerator.generate(collectedStacks, collectedTabs, collectedPanes, basePackage)
    }

    // =========================================================================
    // NEW: Wrapper Registry Generation
    // =========================================================================

    /**
     * Generate the WrapperRegistry from extracted wrapper functions.
     *
     * The registry provides runtime dispatch for @TabWrapper and @PaneWrapper
     * functions based on navigation node keys.
     */
    private fun generateWrapperRegistry(wrappers: List<com.jermey.quo.vadis.ksp.models.WrapperInfo>) {
        logger.info("Generating WrapperRegistry...")
        wrapperRegistryGenerator.generate(wrappers)
    }

    // =========================================================================
    // NEW: Transition Registry Generation
    // =========================================================================

    /**
     * Generate the TransitionRegistry from @Transition annotations.
     *
     * The registry maps destination classes to NavTransition instances
     * for use by AnimationCoordinator.
     */
    private fun generateTransitionRegistry(transitions: List<com.jermey.quo.vadis.ksp.models.TransitionInfo>) {
        logger.info("Generating TransitionRegistry...")
        transitionRegistryGenerator.generate(transitions)
    }
}

/**
 * Provider for QuoVadisSymbolProcessor.
 */
class QuoVadisSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return QuoVadisSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
```

---

## Processing Order

The processor follows this order to ensure dependencies are met:

```
┌─────────────────────────────────────────────────────────────┐
│                    Phase 1: EXTRACTION                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   @Stack    │  │   @Screen   │  │ @TabWrapper │         │
│  │   @Tab      │  └─────────────┘  │ @PaneWrapper│         │
│  │   @Pane     │                   └─────────────┘         │
│  └─────────────┘                                           │
│        │                                                    │
│        ▼                                                    │
│  ┌─────────────────┐         ┌─────────────────────┐       │
│  │ Container Info  │         │   Wrapper Info      │       │
│  │ (Stack, Tab,    │         │   (Tab & Pane)      │       │
│  │  Pane)          │         └─────────────────────┘       │
│  └─────────────────┘                                       │
│                                                             │
│  ┌─────────────────┐         ┌─────────────────────┐       │
│  │  @Destination   │         │    @Transition      │       │
│  │  (for deep      │         │    (per-screen      │       │
│  │   links)        │         │     animations)     │       │
│  └─────────────────┘         └─────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Phase 2: VALIDATION                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  • Validate container relationships                         │
│  • Validate screen bindings                                 │
│  • Cross-reference wrapper targets with @Tab/@Pane          │
│  • Validate custom transition providers                     │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         If validation fails, STOP here              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Phase 3: GENERATION                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │  NavNode Builders   │    │   ScreenRegistry    │        │
│  │  (existing)         │    │   (existing)        │        │
│  └─────────────────────┘    └─────────────────────┘        │
│                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │  WrapperRegistry    │    │ TransitionRegistry  │        │
│  │  (NEW)              │    │ (NEW)               │        │
│  └─────────────────────┘    └─────────────────────┘        │
│                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │  DeepLinkHandler    │    │NavigatorExtensions  │        │
│  │  (existing)         │    │ (existing)          │        │
│  └─────────────────────┘    └─────────────────────┘        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Generated Files Summary

After processing, the following files are generated:

```
build/generated/ksp/*/kotlin/com/jermey/quo/vadis/generated/
├── NavNodeBuilders.kt           # @Stack/@Tab/@Pane builders
├── GeneratedScreenRegistry.kt    # @Screen -> Composable mapping
├── GeneratedWrapperRegistry.kt   # @TabWrapper/@PaneWrapper dispatch (NEW)
├── GeneratedTransitionRegistry.kt# @Transition -> NavTransition mapping (NEW)
├── GeneratedDeepLinkHandler.kt   # Route -> Destination parsing
└── NavigatorExtensions.kt        # Type-safe navigation functions
```

---

## Testing Requirements

### Integration Tests

```kotlin
class KspIntegrationTest {

    @Test
    fun `processor generates all registries`() {
        // Given complete source with all annotation types
        val compilation = compile("""
            // Stack
            @Stack(name = "home", startDestination = "Feed")
            sealed class HomeDestination : Destination {
                @Destination(route = "home/feed")
                data object Feed : HomeDestination()
            }
            
            // Tab with wrapper
            @Tab(name = "main", items = [HomeTab::class])
            object MainTabs
            
            @TabItem(label = "Home", icon = "home")
            @Stack(name = "homeTab", startDestination = "HomeFeed")
            sealed class HomeTab : Destination {
                @Destination(route = "tab/home")
                data object HomeFeed : HomeTab()
            }
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
                content()
            }
            
            // Screen with transition
            @Transition(type = TransitionType.FADE)
            @Screen(HomeDestination.Feed::class)
            @Composable
            fun FeedScreen(dest: HomeDestination.Feed) { }
        """)
        
        // When
        assertTrue(compilation.success)
        
        // Then - all registries generated
        val generatedFiles = compilation.generatedFiles.map { it.name }
        assertContains(generatedFiles, "GeneratedScreenRegistry.kt")
        assertContains(generatedFiles, "GeneratedWrapperRegistry.kt")
        assertContains(generatedFiles, "GeneratedTransitionRegistry.kt")
        assertContains(generatedFiles, "GeneratedDeepLinkHandler.kt")
        assertContains(generatedFiles, "NavigatorExtensions.kt")
    }

    @Test
    fun `processor handles empty project gracefully`() {
        // Given no annotations
        val compilation = compile("")
        
        // When
        assertTrue(compilation.success)
        
        // Then - empty registries generated (no crash)
        val wrapperRegistry = compilation.generatedFiles
            .find { it.name == "GeneratedWrapperRegistry.kt" }
        assertNotNull(wrapperRegistry)
    }

    @Test
    fun `processor stops on validation error`() {
        // Given invalid wrapper target
        val compilation = compile("""
            @Stack(name = "home", startDestination = "Feed")
            sealed class HomeDestination : Destination
            
            @TabWrapper(HomeDestination::class)  // Error: not a @Tab
            @Composable
            fun BadWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { }
        """)
        
        // When
        assertFalse(compilation.success)
        
        // Then - error message present
        assertTrue(compilation.messages.any { 
            it.contains("must be annotated with @Tab") 
        })
    }

    @Test
    fun `incremental processing regenerates on wrapper change`() {
        // First compilation
        val compilation1 = compile("""
            @Tab(name = "main", items = [])
            object MainTabs
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
                content()
            }
        """)
        assertTrue(compilation1.success)
        
        // Second compilation with modified wrapper
        val compilation2 = compile("""
            @Tab(name = "main", items = [])
            object MainTabs
            
            @TabWrapper(MainTabs::class)
            @Composable
            fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
                Scaffold { content() }  // Modified!
            }
        """)
        assertTrue(compilation2.success)
        
        // Registry should be regenerated
        // (Exact verification depends on test infrastructure)
    }
}
```

---

## Acceptance Criteria

- [ ] `QuoVadisSymbolProcessor` updated with wrapper and transition extractors
- [ ] `QuoVadisSymbolProcessor` updated with wrapper and transition generators
- [ ] Processing order: extract all → validate → generate all
- [ ] `WrapperExtractor.extractAll()` called during extraction phase
- [ ] `TransitionExtractor.extractAll()` called during extraction phase
- [ ] `WrapperRegistryGenerator.generate()` called during generation phase
- [ ] `TransitionRegistryGenerator.generate()` called during generation phase
- [ ] Empty registries generated when no annotations present (no errors)
- [ ] Validation errors stop generation (fail fast)
- [ ] Logging indicates progress at each phase
- [ ] Integration tests pass for complete annotation scenarios
- [ ] Backward compatibility with existing generated code

---

## Notes

### Logging

The processor logs at each phase for debugging:
```
QuoVadisSymbolProcessor: Starting processing
Extracted 3 stacks, 1 tabs, 0 panes
Extracted 5 @Screen functions
Extracted 2 wrapper functions (1 tab, 1 pane)
Extracted 3 @Transition annotations
Generating WrapperRegistry...
Generated com.jermey.quo.vadis.generated.GeneratedWrapperRegistry
Generating TransitionRegistry...
Generated com.jermey.quo.vadis.generated.GeneratedTransitionRegistry
QuoVadisSymbolProcessor: Processing complete
```

### Multi-Round Processing

The `hasGenerated` flag prevents duplicate generation in multi-round KSP processing. This is important because KSP may invoke `process()` multiple times.

---

## References

- [QuoVadisSymbolProcessor.kt](../../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt) - Current implementation
- [HIER-012](HIER-012-ksp-wrapper-processor.md) - WrapperExtractor
- [HIER-013](HIER-013-wrapper-registry-generator.md) - WrapperRegistryGenerator
- [HIER-014](HIER-014-transition-registry-generator.md) - TransitionRegistryGenerator

````