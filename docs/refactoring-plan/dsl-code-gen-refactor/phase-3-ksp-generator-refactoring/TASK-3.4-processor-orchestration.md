# Task 3.4: Update Processor Orchestration

> **Task Status**: ✅ Completed  
> **Completed**: December 16, 2024  
> **Estimated Effort**: 2-3 days  
> **Dependencies**: Task 3.1, Task 3.2, Task 3.3  
> **Blocks**: Phase 4, Phase 5

---

## Objective

Update `QuoVadisSymbolProcessor` to orchestrate single-file generation using the new `NavigationConfigGenerator`, while maintaining support for incremental processing and improving error messages.

**Key Outcomes**:
1. Single unified generation pipeline for `GeneratedNavigationConfig.kt`
2. Proper dependency ordering for extraction and generation
3. Maintained incremental processing support
4. Enhanced error messages with actionable guidance
5. KSP options for generation mode control

---

## Current Processor Structure

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`

**Current Flow**:
```
process() 
├── processNavNodeBuilders()
│   ├── collectAllDestinations()
│   ├── extractStackInfo() / extractTabInfo() / extractPaneInfo()
│   ├── generateStackBuilders() / generateTabBuilders() / generatePaneBuilders()
│   └── generate*Registry() for each registry type
├── processDeepLinkHandler()
└── generateNavigatorExtensions()
```

**Current Generators Used**:
- `navNodeBuilderGenerator`
- `screenRegistryGenerator`
- `wrapperRegistryGenerator`
- `transitionRegistryGenerator`
- `scopeRegistryGenerator`
- `containerRegistryGenerator`
- `deepLinkHandlerGenerator`
- `navigatorExtGenerator`

---

## Target Processor Structure

**New Flow**:
```
process()
├── Phase 1: Collection
│   ├── collectAllDestinations()
│   ├── collectScreenInfo()
│   ├── collectContainerInfo() (tabs, stacks, panes)
│   ├── collectTransitionInfo()
│   └── collectWrapperInfo()
│
├── Phase 2: Validation
│   ├── validateDestinations()
│   ├── validateContainerReferences()
│   └── validateScopeConsistency()
│
├── Phase 3: Generation
│   ├── generateNavigationConfig() [NEW: main unified generation]
│   ├── generateLegacyRegistries() [OPTIONAL: deprecated mode]
│   ├── generateDeepLinkHandler()
│   └── generateNavigatorExtensions()
│
└── Phase 4: Finalization
    └── writeGeneratedFiles()
```

---

## File Specification

### Updated QuoVadisSymbolProcessor.kt

```kotlin
package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.jermey.quo.vadis.ksp.extractors.*
import com.jermey.quo.vadis.ksp.generators.base.*
import com.jermey.quo.vadis.ksp.generators.dsl.*
import com.jermey.quo.vadis.ksp.generators.legacy.*
import com.jermey.quo.vadis.ksp.models.*
import com.jermey.quo.vadis.ksp.validation.*

/**
 * Main KSP Symbol Processor for Quo Vadis navigation code generation.
 * 
 * This processor analyzes navigation-related annotations and generates:
 * 1. `GeneratedNavigationConfig` - Unified DSL-based configuration (primary)
 * 2. Legacy registry objects (deprecated, for backward compatibility)
 * 3. Navigator extension functions
 * 4. Deep link handler
 * 
 * ## Generation Modes
 * 
 * Controlled via KSP options:
 * - `quoVadis.mode=dsl` - Generate only DSL config (default after migration)
 * - `quoVadis.mode=legacy` - Generate only legacy registries
 * - `quoVadis.mode=both` - Generate both (default during transition)
 * 
 * ## Processing Pipeline
 * 
 * 1. **Collection**: Extract all annotated symbols
 * 2. **Validation**: Verify correctness and consistency
 * 3. **Generation**: Produce output files
 * 4. **Finalization**: Write files and report results
 */
class QuoVadisSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    
    // =========================================================================
    // Configuration
    // =========================================================================
    
    private val generationMode: GenerationMode = parseGenerationMode()
    private val targetPackage: String = options["quoVadis.package"] 
        ?: "com.example.navigation.generated"
    private val strictValidation: Boolean = options["quoVadis.strictValidation"]
        ?.toBoolean() ?: true
    
    // =========================================================================
    // Extractors
    // =========================================================================
    
    private val destinationExtractor = DestinationExtractor(logger)
    private val screenExtractor = ScreenExtractor(logger)
    private val stackExtractor = StackExtractor(logger)
    private val tabExtractor = TabExtractor(logger)
    private val paneExtractor = PaneExtractor(logger)
    private val transitionExtractor = TransitionExtractor(logger)
    private val wrapperExtractor = WrapperExtractor(logger)
    
    // =========================================================================
    // Generators
    // =========================================================================
    
    // New DSL generator (primary)
    private val navigationConfigGenerator = NavigationConfigGenerator(
        codeGenerator = codeGenerator,
        logger = logger,
        packageName = targetPackage
    )
    
    // Legacy generators (for backward compatibility)
    private val legacyGenerators = LegacyGenerators(
        codeGenerator = codeGenerator,
        logger = logger,
        packageName = targetPackage
    )
    
    // Standalone generators (always generated)
    private val deepLinkHandlerGenerator = DeepLinkHandlerGenerator(codeGenerator, logger)
    private val navigatorExtGenerator = NavigatorExtGenerator(codeGenerator, logger)
    
    // =========================================================================
    // Validation
    // =========================================================================
    
    private val validationEngine = ValidationEngine(logger, strictValidation)
    
    // =========================================================================
    // State
    // =========================================================================
    
    private var hasProcessed = false
    
    // Collected data
    private val collectedDestinations = mutableListOf<DestinationInfo>()
    private val collectedScreens = mutableListOf<ScreenInfo>()
    private val collectedStacks = mutableListOf<StackInfo>()
    private val collectedTabs = mutableListOf<TabInfo>()
    private val collectedPanes = mutableListOf<PaneInfo>()
    private val collectedTransitions = mutableListOf<TransitionInfo>()
    private val collectedWrappers = mutableListOf<WrapperInfo>()
    
    // Originating files for incremental processing
    private val originatingFiles = mutableSetOf<KSFile>()
    
    // =========================================================================
    // Main Processing Entry Point
    // =========================================================================
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasProcessed) return emptyList()
        
        logger.info("QuoVadis: Starting symbol processing (mode: $generationMode)")
        
        try {
            // Phase 1: Collection
            val deferredSymbols = collectAllSymbols(resolver)
            if (deferredSymbols.isNotEmpty()) {
                logger.info("QuoVadis: Deferring ${deferredSymbols.size} symbols for next round")
                return deferredSymbols
            }
            
            // Phase 2: Validation
            val validationResult = validateCollectedData()
            if (!validationResult.isValid) {
                validationResult.errors.forEach { error ->
                    logger.error("QuoVadis: $error")
                }
                if (strictValidation) {
                    return emptyList() // Abort generation on validation errors
                }
            }
            
            // Phase 3: Generation
            generateOutput()
            
            hasProcessed = true
            logger.info("QuoVadis: Processing complete")
            
        } catch (e: Exception) {
            logger.error("QuoVadis: Processing failed: ${e.message}")
            logger.exception(e)
        }
        
        return emptyList()
    }
    
    // =========================================================================
    // Phase 1: Collection
    // =========================================================================
    
    private fun collectAllSymbols(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        // Collect destinations
        deferred += collectDestinations(resolver)
        
        // Collect screens
        deferred += collectScreens(resolver)
        
        // Collect containers (depends on destinations)
        deferred += collectStacks(resolver)
        deferred += collectTabs(resolver)
        deferred += collectPanes(resolver)
        
        // Collect transitions
        deferred += collectTransitions(resolver)
        
        // Collect wrappers
        deferred += collectWrappers(resolver)
        
        return deferred
    }
    
    private fun collectDestinations(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.DESTINATION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                val result = destinationExtractor.extract(symbol)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedDestinations.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectScreens(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.SCREEN_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { symbol ->
                val result = screenExtractor.extract(symbol)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedScreens.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectStacks(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.STACK_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                val result = stackExtractor.extract(symbol, collectedDestinations)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedStacks.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectTabs(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.TABS_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                val result = tabExtractor.extract(symbol, collectedDestinations)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedTabs.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectPanes(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.PANE_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                val result = paneExtractor.extract(symbol, collectedDestinations)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedPanes.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectTransitions(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.TRANSITION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { symbol ->
                val result = transitionExtractor.extract(symbol)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedTransitions.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    private fun collectWrappers(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        
        // Tabs containers
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.TABS_CONTAINER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { symbol ->
                val result = wrapperExtractor.extractTabsContainer(symbol)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedWrappers.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        // Pane containers
        resolver.getSymbolsWithAnnotation(QuoVadisClassNames.PANE_CONTAINER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { symbol ->
                val result = wrapperExtractor.extractPaneContainer(symbol)
                when (result) {
                    is ExtractionResult.Success -> {
                        collectedWrappers.add(result.data)
                        symbol.containingFile?.let { originatingFiles.add(it) }
                    }
                    is ExtractionResult.Deferred -> deferred.add(symbol)
                    is ExtractionResult.Error -> logger.error(result.message, symbol)
                }
            }
        
        return deferred
    }
    
    // =========================================================================
    // Phase 2: Validation
    // =========================================================================
    
    private fun validateCollectedData(): ValidationResult {
        return validationEngine.validate(
            ValidationInput(
                destinations = collectedDestinations,
                screens = collectedScreens,
                stacks = collectedStacks,
                tabs = collectedTabs,
                panes = collectedPanes,
                transitions = collectedTransitions,
                wrappers = collectedWrappers
            )
        )
    }
    
    // =========================================================================
    // Phase 3: Generation
    // =========================================================================
    
    private fun generateOutput() {
        val originatingFilesList = originatingFiles.toList()
        
        when (generationMode) {
            GenerationMode.DSL -> {
                generateDslConfig(originatingFilesList)
            }
            GenerationMode.LEGACY -> {
                generateLegacyRegistries(originatingFilesList)
            }
            GenerationMode.BOTH -> {
                generateDslConfig(originatingFilesList)
                generateLegacyRegistries(originatingFilesList, withDeprecations = true)
            }
        }
        
        // Always generate these
        generateDeepLinkHandler(originatingFilesList)
        generateNavigatorExtensions(originatingFilesList)
    }
    
    /**
     * Generates the unified DSL-based NavigationConfig.
     */
    private fun generateDslConfig(originatingFiles: List<KSFile>) {
        if (collectedScreens.isEmpty() && collectedTabs.isEmpty() && 
            collectedStacks.isEmpty() && collectedPanes.isEmpty()) {
            logger.warn("QuoVadis: No navigation elements found, skipping DSL config generation")
            return
        }
        
        logger.info("QuoVadis: Generating DSL NavigationConfig...")
        
        val navigationData = NavigationConfigGenerator.NavigationData(
            screens = collectedScreens,
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            transitions = collectedTransitions,
            wrappers = collectedWrappers,
            destinations = collectedDestinations
        )
        
        navigationConfigGenerator.generate(navigationData, originatingFiles)
    }
    
    /**
     * Generates legacy registry objects (deprecated).
     */
    private fun generateLegacyRegistries(
        originatingFiles: List<KSFile>,
        withDeprecations: Boolean = false
    ) {
        logger.info("QuoVadis: Generating legacy registries (deprecated=$withDeprecations)...")
        
        legacyGenerators.generate(
            screens = collectedScreens,
            stacks = collectedStacks,
            tabs = collectedTabs,
            panes = collectedPanes,
            transitions = collectedTransitions,
            wrappers = collectedWrappers,
            originatingFiles = originatingFiles,
            addDeprecations = withDeprecations
        )
    }
    
    private fun generateDeepLinkHandler(originatingFiles: List<KSFile>) {
        val destinationsWithRoutes = collectedDestinations.filter { it.route != null }
        if (destinationsWithRoutes.isEmpty()) {
            logger.info("QuoVadis: No deep link routes found, skipping handler generation")
            return
        }
        
        logger.info("QuoVadis: Generating DeepLinkHandler...")
        deepLinkHandlerGenerator.generate(destinationsWithRoutes, originatingFiles)
    }
    
    private fun generateNavigatorExtensions(originatingFiles: List<KSFile>) {
        if (collectedDestinations.isEmpty()) {
            return
        }
        
        logger.info("QuoVadis: Generating Navigator extensions...")
        navigatorExtGenerator.generate(collectedDestinations, originatingFiles)
    }
    
    // =========================================================================
    // Configuration Parsing
    // =========================================================================
    
    private fun parseGenerationMode(): GenerationMode {
        return when (options["quoVadis.mode"]?.lowercase()) {
            "dsl" -> GenerationMode.DSL
            "legacy" -> GenerationMode.LEGACY
            "both" -> GenerationMode.BOTH
            else -> GenerationMode.BOTH // Default during transition period
        }
    }
    
    enum class GenerationMode {
        DSL,    // Generate only GeneratedNavigationConfig
        LEGACY, // Generate only legacy registries
        BOTH    // Generate both (default)
    }
}

// =========================================================================
// Provider
// =========================================================================

class QuoVadisSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return QuoVadisSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            options = environment.options
        )
    }
}

// =========================================================================
// Supporting Types
// =========================================================================

/**
 * Result of symbol extraction.
 */
sealed class ExtractionResult<T> {
    data class Success<T>(val data: T) : ExtractionResult<T>()
    data class Deferred<T>(val symbol: KSAnnotated) : ExtractionResult<T>()
    data class Error<T>(val message: String) : ExtractionResult<T>()
}

/**
 * Input for validation.
 */
data class ValidationInput(
    val destinations: List<DestinationInfo>,
    val screens: List<ScreenInfo>,
    val stacks: List<StackInfo>,
    val tabs: List<TabInfo>,
    val panes: List<PaneInfo>,
    val transitions: List<TransitionInfo>,
    val wrappers: List<WrapperInfo>
)

/**
 * Result of validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
```

---

## Supporting Classes

### LegacyGenerators.kt

```kotlin
package com.jermey.quo.vadis.ksp.generators.legacy

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.jermey.quo.vadis.ksp.models.*

/**
 * Orchestrates legacy generator execution for backward compatibility.
 */
class LegacyGenerators(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    packageName: String
) {
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)
    private val containerRegistryGenerator = ContainerRegistryGenerator(codeGenerator, logger)
    private val scopeRegistryGenerator = ScopeRegistryGenerator(codeGenerator, logger)
    private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)
    private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)
    
    fun generate(
        screens: List<ScreenInfo>,
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        transitions: List<TransitionInfo>,
        wrappers: List<WrapperInfo>,
        originatingFiles: List<KSFile>,
        addDeprecations: Boolean
    ) {
        // Generate screen registry
        if (screens.isNotEmpty()) {
            screenRegistryGenerator.generate(screens, originatingFiles, addDeprecations)
        }
        
        // Generate container registry
        if (tabs.isNotEmpty() || stacks.isNotEmpty() || panes.isNotEmpty()) {
            containerRegistryGenerator.generate(tabs, stacks, panes, originatingFiles, addDeprecations)
        }
        
        // Generate scope registry
        val scopeData = collectScopeData(tabs, stacks, panes)
        if (scopeData.isNotEmpty()) {
            scopeRegistryGenerator.generate(scopeData, originatingFiles, addDeprecations)
        }
        
        // Generate transition registry
        if (transitions.isNotEmpty()) {
            transitionRegistryGenerator.generate(transitions, originatingFiles, addDeprecations)
        }
        
        // Generate wrapper registry
        if (wrappers.isNotEmpty()) {
            wrapperRegistryGenerator.generate(wrappers, originatingFiles, addDeprecations)
        }
        
        // Generate NavNode builders (always deprecated when in BOTH mode)
        tabs.forEach { navNodeBuilderGenerator.generateTabBuilder(it, originatingFiles, addDeprecations) }
        stacks.forEach { navNodeBuilderGenerator.generateStackBuilder(it, originatingFiles, addDeprecations) }
        panes.forEach { navNodeBuilderGenerator.generatePaneBuilder(it, originatingFiles, addDeprecations) }
    }
    
    private fun collectScopeData(
        tabs: List<TabInfo>,
        stacks: List<StackInfo>,
        panes: List<PaneInfo>
    ): Map<String, Set<KSClassDeclaration>> {
        // ... scope collection logic
    }
}
```

### Enhanced ValidationEngine.kt

```kotlin
package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.ValidationInput
import com.jermey.quo.vadis.ksp.ValidationResult

/**
 * Validates collected navigation data before generation.
 */
class ValidationEngine(
    private val logger: KSPLogger,
    private val strictMode: Boolean
) {
    
    fun validate(input: ValidationInput): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate screen-destination mapping
        validateScreenDestinations(input, errors, warnings)
        
        // Validate container references
        validateContainerReferences(input, errors, warnings)
        
        // Validate scope consistency
        validateScopeConsistency(input, errors, warnings)
        
        // Validate wrapper references
        validateWrapperReferences(input, errors, warnings)
        
        // Check for duplicate registrations
        validateDuplicates(input, errors, warnings)
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun validateScreenDestinations(
        input: ValidationInput,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Check that all destination classes have corresponding screens
        val screenDestinations = input.screens.map { it.destination.qualifiedName?.asString() }.toSet()
        
        input.destinations
            .filter { !it.isContainer }
            .forEach { dest ->
                val destName = dest.declaration.qualifiedName?.asString()
                if (destName != null && destName !in screenDestinations) {
                    errors.add(
                        "Destination '$destName' has no @Screen. " +
                        "Add a @Screen function or remove the @Destination annotation."
                    )
                }
            }
    }
    
    private fun validateContainerReferences(
        input: ValidationInput,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Validate tab item destinations exist
        input.tabs.forEach { tab ->
            tab.items.forEach { item ->
                val itemDest = item.destination.qualifiedName?.asString()
                val exists = input.destinations.any { 
                    it.declaration.qualifiedName?.asString() == itemDest 
                }
                if (!exists) {
                    errors.add(
                        "Tab item '${item.destination.simpleName.asString()}' in " +
                        "'${tab.containerClass.simpleName.asString()}' references " +
                        "unknown destination. Add @Destination annotation."
                    )
                }
            }
        }
        
        // Similar validation for stacks and panes...
    }
    
    private fun validateScopeConsistency(
        input: ValidationInput,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Check for scope key conflicts
        val scopeKeys = mutableMapOf<String, MutableList<String>>()
        
        input.tabs.forEach { tab ->
            val key = tab.scopeKey ?: tab.containerClass.simpleName.asString()
            scopeKeys.getOrPut(key) { mutableListOf() }
                .add("tabs:${tab.containerClass.simpleName.asString()}")
        }
        
        input.stacks.forEach { stack ->
            val key = stack.scopeKey ?: stack.containerClass.simpleName.asString()
            scopeKeys.getOrPut(key) { mutableListOf() }
                .add("stack:${stack.containerClass.simpleName.asString()}")
        }
        
        // Warn about potential scope key collisions
        scopeKeys.filter { it.value.size > 1 }.forEach { (key, sources) ->
            warnings.add(
                "Scope key '$key' is used by multiple containers: ${sources.joinToString()}. " +
                "Consider using explicit scopeKey parameter to avoid conflicts."
            )
        }
    }
    
    private fun validateWrapperReferences(
        input: ValidationInput,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val tabsContainerKeys = input.wrappers
            .filter { it.type == WrapperInfo.WrapperType.TAB }
            .map { it.key }
            .toSet()
        
        val paneContainerKeys = input.wrappers
            .filter { it.type == WrapperInfo.WrapperType.PANE }
            .map { it.key }
            .toSet()
        
        // Check tab wrapperKey references
        input.tabs.filter { it.wrapperKey != null }.forEach { tab ->
            if (tab.wrapperKey !in tabsContainerKeys) {
                errors.add(
                    "Tab container '${tab.containerClass.simpleName.asString()}' " +
                    "references unknown wrapper '${tab.wrapperKey}'. " +
                    "Add @TabsContainer function with key='${tab.wrapperKey}'."
                )
            }
        }
        
        // Check pane wrapperKey references
        input.panes.filter { it.wrapperKey != null }.forEach { pane ->
            if (pane.wrapperKey !in paneContainerKeys) {
                errors.add(
                    "Pane container '${pane.containerClass.simpleName.asString()}' " +
                    "references unknown wrapper '${pane.wrapperKey}'. " +
                    "Add @PaneContainer function with key='${pane.wrapperKey}'."
                )
            }
        }
    }
    
    private fun validateDuplicates(
        input: ValidationInput,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Check for duplicate screen registrations
        val screenDests = mutableSetOf<String>()
        input.screens.forEach { screen ->
            val destName = screen.destination.qualifiedName?.asString() ?: return@forEach
            if (destName in screenDests) {
                errors.add("Duplicate @Screen registration for '$destName'")
            }
            screenDests.add(destName)
        }
        
        // Check for duplicate transition registrations
        val transitionDests = mutableSetOf<String>()
        input.transitions.forEach { transition ->
            val destName = transition.destination.qualifiedName?.asString() ?: return@forEach
            if (destName in transitionDests) {
                warnings.add("Duplicate @Transition for '$destName'. Last one wins.")
            }
            transitionDests.add(destName)
        }
    }
}
```

---

## KSP Options

| Option | Values | Default | Description |
|--------|--------|---------|-------------|
| `quoVadis.mode` | `dsl`, `legacy`, `both` | `both` | Controls which generation mode is used |
| `quoVadis.package` | Package name | `com.example.navigation.generated` | Target package for generated code |
| `quoVadis.strictValidation` | `true`, `false` | `true` | Whether validation errors abort generation |

**Usage in build.gradle.kts**:

```kotlin
ksp {
    arg("quoVadis.mode", "both")
    arg("quoVadis.package", "com.myapp.navigation.generated")
    arg("quoVadis.strictValidation", "true")
}
```

---

## Error Message Improvements

### Before (Current)

```
error: QuoVadis: Failed to process symbol
```

### After (Improved)

```
error: QuoVadis: Destination 'com.example.HomeDestination' has no @Screen.
       Add a @Screen function or remove the @Destination annotation.
       
       Example:
       @Screen
       @Composable
       fun HomeScreen(navigator: Navigator) { ... }
```

### Error Categories

1. **Missing Annotations**
   - Clear message about what's missing
   - Example of how to fix

2. **Invalid References**
   - What reference is invalid
   - Where it's referenced from
   - Suggestions for valid alternatives

3. **Configuration Conflicts**
   - What's conflicting
   - Which configurations are involved
   - How to resolve

4. **Type Mismatches**
   - Expected vs actual type
   - Location of the mismatch
   - Correction guidance

---

## Incremental Processing

### Dependency Tracking

```kotlin
// Properly track file dependencies for incremental builds
fileSpec.writeTo(
    codeGenerator = codeGenerator,
    aggregating = true,  // Important: marks as aggregating
    originatingKSFiles = originatingFiles
)
```

### What Triggers Regeneration

| Change | Regenerates |
|--------|-------------|
| @Destination class modified | All outputs |
| @Screen function modified | Screen registry sections |
| @Tabs container modified | Container and scope sections |
| New destination added | All outputs |
| Destination removed | All outputs |

### Optimization: Deferred Symbols

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    // Return symbols that couldn't be fully resolved yet
    // KSP will call process() again with these symbols
    val deferred = collectAllSymbols(resolver)
    if (deferred.isNotEmpty()) {
        return deferred // Will be reprocessed
    }
    // ... continue with generation
}
```

---

## Dependencies

### This Task Depends On

| Dependency | Description | Status |
|------------|-------------|--------|
| Task 3.1 | Base classes | Required |
| Task 3.2 | NavigationConfigGenerator | Required |
| Task 3.3 | Refactored legacy generators | Required |

### What This Task Blocks

| Task/Phase | Dependency Type |
|------------|-----------------|
| Phase 4 (Migration) | Processor must work correctly |
| Phase 5 (Documentation) | Documents new KSP options |

---

## Acceptance Criteria Checklist

### Processing Pipeline
- [ ] `process()` method correctly orchestrates collection, validation, generation
- [ ] Deferred symbols are returned and reprocessed
- [ ] Generation mode option works (DSL, LEGACY, BOTH)
- [ ] Package name option works
- [ ] Strict validation option works

### Collection Phase
- [ ] All annotated symbols are collected
- [ ] Originating files are tracked for incremental builds
- [ ] Extraction errors are logged with context

### Validation Phase
- [ ] Screen-destination mapping validated
- [ ] Container references validated
- [ ] Scope consistency checked
- [ ] Wrapper references validated
- [ ] Duplicate registrations detected
- [ ] Errors block generation in strict mode
- [ ] Warnings are logged but don't block

### Generation Phase
- [ ] DSL mode generates only `GeneratedNavigationConfig.kt`
- [ ] Legacy mode generates only old registry files
- [ ] Both mode generates all files with deprecations
- [ ] Deep link handler generated when routes exist
- [ ] Navigator extensions generated

### Error Messages
- [ ] All error messages include context
- [ ] All error messages include fix suggestions
- [ ] Error messages reference specific files/lines when possible

### Incremental Processing
- [ ] Changes to source files trigger correct regeneration
- [ ] Unchanged files don't cause unnecessary regeneration
- [ ] Aggregating mode is used correctly

### Testing
- [ ] Unit tests for validation logic
- [ ] Integration tests for full processing pipeline
- [ ] Test all KSP options
- [ ] Test incremental processing scenarios

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| Refactor main process() flow | 0.5 days |
| Implement collection phase | 0.5 days |
| Implement/enhance validation | 0.5 days |
| Implement generation orchestration | 0.5 days |
| KSP options parsing | 0.25 days |
| Error message improvements | 0.25 days |
| LegacyGenerators wrapper | 0.25 days |
| Testing & debugging | 0.5 days |
| **Total** | **2-3 days** |

---

## Related Files

- [Phase 3 Summary](./SUMMARY.md)
- [Task 3.1 - Generator Base Classes](./TASK-3.1-generator-base-classes.md)
- [Task 3.2 - NavigationConfigGenerator](./TASK-3.2-navigation-config-generator.md)
- [Task 3.3 - Refactor Existing Generators](./TASK-3.3-refactor-existing-generators.md)

### Current Processor File
- [QuoVadisSymbolProcessor.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt)

### Validation File
- [ValidationEngine.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt)
