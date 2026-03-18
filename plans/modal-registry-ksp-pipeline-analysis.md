# KSP Code Generation Pipeline Analysis for ModalRegistry

## Executive Summary

This document maps the complete `@Transition` → `TransitionRegistry` pipeline from annotation to runtime, establishing the exact patterns to follow for implementing `@Modal` → `ModalRegistry`.

---

## 1. Annotation Definition

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Transition.kt`

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transition(
    val type: TransitionType = TransitionType.SlideHorizontal,
    val customTransition: KClass<*> = Unit::class
)
```

**Pattern:** Simple annotation with enum-typed properties + optional KClass reference for custom behavior. Applied to `@Destination`-annotated classes.

---

## 2. Info Model (Data Transfer)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TransitionInfo.kt`

```kotlin
data class TransitionInfo(
    val destinationClass: KSClassDeclaration,
    val destinationQualifiedName: String,
    val transitionType: String,
    val customTransitionClass: String?,
    val containingFile: KSFile
)
```

**Pattern:** Plain `data class` capturing:
- The annotated class declaration (for KotlinPoet `toClassName()`)
- Qualified name (for logging/error messages)
- Annotation parameter values as strings
- `containingFile` for KSP incremental compilation tracking

---

## 3. Extractor (Annotation → Info Model)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TransitionExtractor.kt`

```kotlin
class TransitionExtractor(private val logger: KSPLogger) {

    private companion object {
        private const val TRANSITION_ANNOTATION = "com.jermey.quo.vadis.annotations.Transition"
        // ...
    }

    fun extract(classDeclaration: KSClassDeclaration): TransitionInfo? {
        // 1. Find the @Transition annotation on the class
        // 2. Get containingFile (for incremental compilation)
        // 3. Get qualifiedName
        // 4. Extract annotation arguments by name ("type", "customTransition")
        // 5. Build and return TransitionInfo
    }

    fun extractAll(resolver: Resolver): List<TransitionInfo> {
        return resolver.getSymbolsWithAnnotation(TRANSITION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
```

**Pattern:**
- Constructor takes only `KSPLogger`
- `extract(classDeclaration)` → single item extraction, returns `null` on failure
- `extractAll(resolver)` → batch extraction using `resolver.getSymbolsWithAnnotation()`
- Annotation FQN is a `private const val`
- Validation happens inline (e.g., Custom type requires non-Unit class)

---

## 4. Block Generator (Info Model → KotlinPoet CodeBlock)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/TransitionBlockGenerator.kt`

```kotlin
class TransitionBlockGenerator(private val logger: KSPLogger) {

    fun generate(transitions: List<TransitionInfo>): CodeBlock {
        // Returns empty CodeBlock if list is empty
        // Otherwise builds: transition<DestClass>(NavTransition.SlideHorizontal)
    }

    private fun generateTransitionBlock(transition: TransitionInfo): CodeBlock {
        val destClass = transition.destinationClass.toClassName()
        val transitionExpr = buildTransitionExpression(transition)
        return CodeBlock.of("transition<%T>(%L)\n", destClass, transitionExpr)
    }
}
```

**Output format (inside `navigationConfig { }` DSL):**
```kotlin
transition<HomeDestination.Detail>(NavTransition.SlideHorizontal)
transition<ProfileDestination>(NavTransition.Fade)
transition<CustomScreen>(CustomTransitionProvider().transition)
```

**Pattern:**
- Constructor takes `KSPLogger`
- `generate(items: List<Info>): CodeBlock` — main entry point
- Uses `%T` for type references (KotlinPoet handles imports)
- Uses `%L` for literal expressions

---

## 5. NavigationConfigGenerator (Orchestrator)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`

### 5a. NavigationData Aggregates All Info

```kotlin
data class NavigationData(
    val screens: List<ScreenInfo>,
    val stacks: List<StackInfo>,
    val tabs: List<TabInfo>,
    val panes: List<PaneInfo>,
    val transitions: List<TransitionInfo>,  // ← transitions here
    val wrappers: List<ContainerInfoModel>,
    val destinations: List<DestinationInfo>
)
```

### 5b. Sub-generator is instantiated in constructor

```kotlin
private val transitionBlockGenerator = TransitionBlockGenerator(logger)
```

### 5c. Transitions are added to DSL content in `buildDslContent()`

```kotlin
private fun buildDslContent(data: NavigationData): CodeBlock {
    val builder = CodeBlock.builder()
    // ... CONTAINERS section ...
    // ... SCOPES section ...

    // TRANSITIONS section (no composable lambdas)
    if (data.transitions.isNotEmpty()) {
        builder.add("\n")
        builder.add(StringTemplates.TRANSITIONS_SECTION)
        builder.add("\n\n")
        builder.add(transitionBlockGenerator.generate(data.transitions))
    }
    // ... DEEP LINKS section ...
    return builder.build()
}
```

### 5d. TransitionRegistry is delegated to baseConfig

```kotlin
private fun buildDelegationProperties(): List<PropertySpec> {
    return listOf(
        buildDelegationProperty("scopeRegistry", QuoVadisClassNames.SCOPE_REGISTRY),
        buildDelegationProperty("transitionRegistry", QuoVadisClassNames.TRANSITION_REGISTRY)
    )
}
```

This generates:
```kotlin
override val transitionRegistry = baseConfig.transitionRegistry
```

**Key insight:** `transitionRegistry` is one of only TWO registries that are delegated directly to `baseConfig` (the other is `scopeRegistry`). Screen registry and container registry use custom anonymous object implementations because they need composable lambdas.

---

## 6. QuoVadisSymbolProcessor (Main Processor)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`

### 6a. Extractor instantiation

```kotlin
private val transitionExtractor = TransitionExtractor(logger)
```

### 6b. Collection state

```kotlin
private val collectedTransitions = mutableListOf<TransitionInfo>()
```

### 6c. Collection method (Step 7 in `collectAllSymbols`)

```kotlin
private fun collectTransitions(resolver: Resolver) {
    val transitions = transitionExtractor.extractAll(resolver)
    collectedTransitions.addAll(transitions)
    transitions.forEach { transition ->
        originatingFiles.add(transition.containingFile)
    }
}
```

### 6d. Wiring into NavigationData for generation

```kotlin
val navigationData = NavigationConfigGenerator.NavigationData(
    screens = collectedScreens,
    stacks = collectedStacks,
    tabs = collectedTabs,
    panes = collectedPanes,
    transitions = collectedTransitions,  // ← passed here
    wrappers = collectedContainers,
    destinations = collectedDestinations
)
navigationConfigGenerator.generate(navigationData, originatingFilesList)
```

---

## 7. Core Registry Interface

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/TransitionRegistry.kt`

```kotlin
@Stable
interface TransitionRegistry {
    fun getTransition(destinationClass: KClass<*>): NavTransition?

    companion object {
        val Empty: TransitionRegistry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<*>): NavTransition? = null
        }
    }
}
```

**Pattern:**
- `@Stable` annotation for Compose optimization
- Single lookup method returning nullable result
- `companion object` with `Empty` singleton (returns `null` for all lookups)

---

## 8. DSL Implementation of Registry

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslTransitionRegistry.kt`

```kotlin
@InternalQuoVadisApi
@Stable
internal class DslTransitionRegistry(
    private val transitions: Map<KClass<out NavDestination>, NavTransition>
) : TransitionRegistry {
    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return transitions[destinationClass]
    }
}
```

**Note:** There's also a **public** version at `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/DslTransitionRegistry.kt` — appears to be a duplicate. The `internal` version in `dsl/internal/` is the one actually used.

---

## 9. Composite Registry (Multi-module merging)

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/internal/CompositeTransitionRegistry.kt`

```kotlin
@InternalQuoVadisApi
internal class CompositeTransitionRegistry(
    private val primary: TransitionRegistry,
    private val secondary: TransitionRegistry
) : TransitionRegistry {
    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return secondary.getTransition(destinationClass) ?: primary.getTransition(destinationClass)
    }
}
```

**Pattern:** Secondary (right-hand `+` operand) takes priority, falls back to primary.

---

## 10. NavigationConfig Interface (Registry Holder)

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/NavigationConfig.kt`

```kotlin
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry     // ← here
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    // ...
}
```

---

## 11. DSL Builder Function

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/NavigationConfigBuilder.kt`

```kotlin
// The DSL function:
inline fun <reified D : NavDestination> transition(transition: NavTransition) {
    transitions[D::class] = transition
}

// Storage:
internal val transitions: MutableMap<KClass<out NavDestination>, NavTransition> = mutableMapOf()

// Passed to DslNavigationConfig in build():
fun build(): NavigationConfig {
    return DslNavigationConfig(
        screens = screens.toMap(),
        containers = containers.toMap(),
        scopes = scopes.mapValues { it.value.toSet() },
        transitions = transitions.toMap(),   // ← here
        tabsContainers = tabsContainers.toMap(),
        paneContainers = paneContainers.toMap()
    )
}
```

---

## 12. DslNavigationConfig Wiring

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslNavigationConfig.kt`

```kotlin
internal class DslNavigationConfig(
    // ...
    private val transitions: Map<KClass<out NavDestination>, NavTransition>,
    // ...
) : NavigationConfig {
    override val transitionRegistry: TransitionRegistry by lazy {
        DslTransitionRegistry(transitions)
    }
}
```

---

## 13. CompositeNavigationConfig Merging

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/CompositeNavigationConfig.kt`

```kotlin
override val transitionRegistry: TransitionRegistry = CompositeTransitionRegistry(
    primary = primary.transitionRegistry,
    secondary = secondary.transitionRegistry
)
```

---

## 14. Runtime Consumption

### NavigationHost extracts the registry from config:

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavigationHost.kt`

```kotlin
// In the config-based overload:
transitionRegistry = config.transitionRegistry

// In the parametric overload:
val animationCoordinator = remember(transitionRegistry) {
    AnimationCoordinator(transitionRegistry)
}
```

### AnimationCoordinator looks up transitions:

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/AnimationCoordinator.kt`

```kotlin
class AnimationCoordinator(
    private val transitionRegistry: TransitionRegistry = TransitionRegistry.Empty
) {
    fun getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition {
        val lookupNode = if (isBack) from else to
        val screenNode = lookupNode as? ScreenNode
        screenNode?.destination?.let { dest ->
            transitionRegistry.getTransition(dest::class)?.let { return it }
        }
        return defaultTransition
    }
}
```

---

## 15. DestinationExtractor

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/DestinationExtractor.kt`

The `DestinationExtractor` is the **foundational extractor** — it extracts metadata from individual `@Destination`-annotated classes. Unlike `TransitionExtractor` which is standalone, `DestinationExtractor` is **composed into** other extractors:

```kotlin
// In QuoVadisSymbolProcessor:
private val destinationExtractor = DestinationExtractor(logger)
private val stackExtractor = StackExtractor(destinationExtractor, logger)
private val tabExtractor = TabExtractor(destinationExtractor, logger, stackExtractor)
private val paneExtractor = PaneExtractor(destinationExtractor, logger)
```

It extracts: route, routeParams, isObject, isDataObject, isDataClass, constructorParams, parentSealedClass, paneRole.

**Key methods:**
- `extract(classDeclaration)` → single `DestinationInfo?`
- `extractFromContainer(containerClass)` → extracts from all sealed subclasses

---

## 16. QuoVadisClassNames Registry

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisClassNames.kt`

Houses KotlinPoet `ClassName` constants for all core types:

```kotlin
val TRANSITION_REGISTRY: ClassName = TransitionRegistry::class.toClassName()
val SCREEN_REGISTRY: ClassName = ScreenRegistry::class.toClassName()
val SCOPE_REGISTRY: ClassName = ScopeRegistry::class.toClassName()
// etc.
```

---

## File Inventory for ModalRegistry Implementation

### NEW FILES to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `quo-vadis-annotations/.../Modal.kt` | `@Modal` annotation (+ `ModalType` enum if needed) |
| 2 | `quo-vadis-ksp/.../models/ModalInfo.kt` | Data class for extracted modal metadata |
| 3 | `quo-vadis-ksp/.../extractors/ModalExtractor.kt` | Extracts `@Modal` annotations → `ModalInfo` |
| 4 | `quo-vadis-ksp/.../generators/dsl/ModalBlockGenerator.kt` | Generates `modal<T>(...)` DSL calls |
| 5 | `quo-vadis-core/.../registry/ModalRegistry.kt` | `interface ModalRegistry` with `Empty` companion |
| 6 | `quo-vadis-core/.../registry/internal/CompositeModalRegistry.kt` | Multi-module composite registry |
| 7 | `quo-vadis-core/.../dsl/internal/DslModalRegistry.kt` | DSL-based implementation |

### EXISTING FILES to Modify

| # | File | Change |
|---|------|--------|
| 8 | `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | Add `ModalExtractor`, `collectedModals`, `collectModals()`, wire into `NavigationData` |
| 9 | `quo-vadis-ksp/.../QuoVadisClassNames.kt` | Add `MODAL_REGISTRY` ClassName constant |
| 10 | `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt` | Add `ModalBlockGenerator` sub-generator, add `modals` to `NavigationData`, generate MODALS section in DSL, add `modalRegistry` delegation property |
| 11 | `quo-vadis-ksp/.../generators/base/StringTemplates.kt` | Add `MODALS_SECTION` header constant |
| 12 | `quo-vadis-core/.../navigation/config/NavigationConfig.kt` | Add `val modalRegistry: ModalRegistry` property (with default `ModalRegistry.Empty`) |
| 13 | `quo-vadis-core/.../dsl/NavigationConfigBuilder.kt` | Add `modals` map, `modal<D>()` DSL function, pass to `DslNavigationConfig` |
| 14 | `quo-vadis-core/.../dsl/internal/DslNavigationConfig.kt` | Accept `modals` parameter, create `DslModalRegistry` lazily |
| 15 | `quo-vadis-core/.../navigation/internal/config/CompositeNavigationConfig.kt` | Add `modalRegistry` composite wiring |
| 16 | `quo-vadis-core/.../navigation/internal/config/EmptyNavigationConfig.kt` | Add `modalRegistry = ModalRegistry.Empty` |
| 17 | `quo-vadis-core/.../compose/NavigationHost.kt` | Extract `modalRegistry` from config, pass to renderer/coordinator |
| 18 | Renderer files (StackRenderer or a new ModalRenderer) | Consume `modalRegistry` at render time |

### TOTAL: 7 new files + 11 modified files (minimum)

---

## Pipeline Summary Diagram

```
@Modal annotation (quo-vadis-annotations)
        │
        ▼
ModalExtractor.extractAll(resolver)  (quo-vadis-ksp/extractors)
        │
        ▼
List<ModalInfo>  (quo-vadis-ksp/models)
        │
        ├──► Collected in QuoVadisSymbolProcessor.collectedModals
        │
        ▼
NavigationData(modals = collectedModals)
        │
        ▼
ModalBlockGenerator.generate(modals)  (quo-vadis-ksp/generators/dsl)
        │
        ▼
CodeBlock: modal<Dest>(ModalConfig.BottomSheet)  (inside navigationConfig { })
        │
        ▼
Generated: GeneratedNavigationConfig object
        │   override val modalRegistry = baseConfig.modalRegistry
        │
        ▼
At runtime, DSL evaluates:
        NavigationConfigBuilder.modal<D>(config)
        → DslNavigationConfig(modals = ...)
        → DslModalRegistry(modals)
        → implements ModalRegistry interface
        │
        ▼
NavigationHost reads config.modalRegistry
        → passes to renderer/coordinator
        → Renderer checks isModal(destination) before rendering
```
