# KSP-001: Create Annotation Extractors

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | KSP-001 |
| **Task Name** | Create Annotation Extractors |
| **Phase** | Phase 4: KSP Processor Rewrite |
| **Complexity** | Medium-High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | ANN-001, ANN-002, ANN-003, ANN-004, ANN-005, **ANN-006** (Phase 4 Annotations) |
| **Blocked By** | Phase 3 Annotations |
| **Blocks** | KSP-002, KSP-003, KSP-004, KSP-005 |

---

## Overview

This task creates the **extraction layer** of the KSP processor—a set of extractor classes that parse each annotation type into strongly-typed intermediate models. These models serve as the foundation for all code generation in subsequent KSP tasks.

### Rationale

The extraction phase separates annotation parsing from code generation, providing:

1. **Clean separation of concerns**: Parsing logic isolated from generation logic
2. **Testability**: Extractors can be unit tested independently
3. **Reusability**: Multiple generators consume the same extracted models
4. **Type safety**: Strongly-typed models prevent runtime errors during generation

---

## Module Structure

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/
├── extractors/
│   ├── DestinationExtractor.kt     # @Destination extraction
│   ├── StackExtractor.kt           # @Stack extraction
│   ├── TabExtractor.kt             # @Tab/@TabItem extraction
│   ├── PaneExtractor.kt            # @Pane/@PaneItem extraction
│   └── ScreenExtractor.kt          # @Screen extraction
│
└── models/
    ├── DestinationInfo.kt          # Destination metadata
    ├── ParamInfo.kt                # Constructor parameter metadata
    ├── StackInfo.kt                # Stack container metadata
    ├── TabInfo.kt                  # Tab container metadata
    ├── TabItemInfo.kt              # Tab item metadata
    ├── PaneInfo.kt                 # Pane container metadata
    ├── PaneItemInfo.kt             # Pane item metadata
    └── ScreenInfo.kt               # Screen binding metadata
```

---

## Output Models

### DestinationInfo

Represents metadata extracted from `@Destination` annotations.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Extracted metadata from a @Destination annotation.
 *
 * @property classDeclaration The KSP class declaration for this destination
 * @property className Simple class name (e.g., "Detail")
 * @property qualifiedName Fully qualified name (e.g., "com.example.HomeDestination.Detail")
 * @property route Deep link route pattern (e.g., "home/detail/{id}"), null if not specified
 * @property routeParams List of route parameter names extracted from the route pattern
 * @property isDataObject True if this is a `data object`
 * @property isDataClass True if this is a `data class`
 * @property constructorParams List of constructor parameters (for data classes)
 * @property parentSealedClass Simple name of the parent sealed class, if any
 */
data class DestinationInfo(
    val classDeclaration: KSClassDeclaration,
    val className: String,
    val qualifiedName: String,
    val route: String?,
    val routeParams: List<String>,
    val isDataObject: Boolean,
    val isDataClass: Boolean,
    val constructorParams: List<ParamInfo>,
    val parentSealedClass: String?
)
```

### ParamInfo

Represents constructor parameter metadata, including `@Argument` annotation details.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSType

/**
 * Metadata for a constructor parameter, including @Argument annotation details.
 *
 * @property name Parameter name in Kotlin code
 * @property type KSP type of the parameter
 * @property hasDefault True if the parameter has a default value
 * @property isArgument True if parameter has @Argument annotation
 * @property argumentKey Key for URL serialization (from @Argument(key=...) or param name)
 * @property isOptionalArgument True if @Argument(optional = true)
 * @property serializerType How to serialize this argument for deep links
 */
data class ParamInfo(
    val name: String,
    val type: KSType,
    val hasDefault: Boolean,
    // @Argument-specific fields
    val isArgument: Boolean = false,
    val argumentKey: String = "",
    val isOptionalArgument: Boolean = false,
    val serializerType: SerializerType = SerializerType.STRING
)

/**
 * Serialization strategy for navigation arguments.
 *
 * Determines how argument values are converted to/from URL strings
 * for deep linking support.
 */
enum class SerializerType {
    /** Direct string - no conversion needed */
    STRING,
    
    /** Integer - uses toInt()/toString() */
    INT,
    
    /** Long - uses toLong()/toString() */
    LONG,
    
    /** Float - uses toFloat()/toString() */
    FLOAT,
    
    /** Double - uses toDouble()/toString() */
    DOUBLE,
    
    /** Boolean - "true"/"false" (case-insensitive parsing) */
    BOOLEAN,
    
    /** Enum - uses enumValueOf<T>()/name */
    ENUM,
    
    /** Complex type - requires kotlinx.serialization @Serializable */
    JSON
}
```

### StackInfo

Represents metadata extracted from `@Stack` annotations.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Stack annotation.
 *
 * @property classDeclaration The KSP class declaration for this stack
 * @property name Stack identifier from annotation (e.g., "home")
 * @property className Simple class name (e.g., "HomeDestination")
 * @property packageName Package containing this class
 * @property startDestination Simple name of the start destination (e.g., "Feed")
 * @property destinations List of all @Destination subclasses within this sealed class
 * @property resolvedStartDestination The DestinationInfo matching startDestination, if found
 */
data class StackInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val startDestination: String,
    val destinations: List<DestinationInfo>,
    val resolvedStartDestination: DestinationInfo?
)
```

### TabInfo & TabItemInfo

Represents metadata extracted from `@Tab` and `@TabItem` annotations.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Tab annotation.
 *
 * @property classDeclaration The KSP class declaration for this tab container
 * @property name Tab container identifier from annotation (e.g., "main")
 * @property className Simple class name (e.g., "MainTabs")
 * @property packageName Package containing this class
 * @property initialTab Simple name of the initial tab (e.g., "Home")
 * @property tabs List of all @TabItem subclasses within this sealed class
 */
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTab: String,
    val tabs: List<TabItemInfo>
)

/**
 * Extracted metadata from a @TabItem annotation.
 *
 * @property destination The destination info for this tab
 * @property label Display label for the tab (e.g., "Home")
 * @property icon Icon identifier for the tab (e.g., "home")
 * @property rootGraphClass Class declaration for the root graph of this tab
 */
data class TabItemInfo(
    val destination: DestinationInfo,
    val label: String,
    val icon: String,
    val rootGraphClass: KSClassDeclaration
)
```

### PaneInfo & PaneItemInfo

Represents metadata extracted from `@Pane` and `@PaneItem` annotations.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Pane annotation.
 *
 * @property classDeclaration The KSP class declaration for this pane container
 * @property name Pane container identifier from annotation
 * @property className Simple class name
 * @property packageName Package containing this class
 * @property backBehavior Back navigation behavior for panes
 * @property panes List of all @PaneItem subclasses within this sealed class
 */
data class PaneInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val backBehavior: PaneBackBehavior,
    val panes: List<PaneItemInfo>
)

/**
 * Extracted metadata from a @PaneItem annotation.
 *
 * @property destination The destination info for this pane
 * @property role The pane role (PRIMARY, SUPPORTING, EXTRA)
 * @property adaptStrategy Strategy when pane space is limited (HIDE, LEVITATE, REFLOW)
 * @property rootGraphClass Class declaration for the root graph of this pane
 */
data class PaneItemInfo(
    val destination: DestinationInfo,
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy,
    val rootGraphClass: KSClassDeclaration
)

/**
 * Pane role in adaptive layouts.
 */
enum class PaneRole {
    PRIMARY,
    SUPPORTING,
    EXTRA
}

/**
 * Adaptation strategy when screen space is limited.
 */
enum class AdaptStrategy {
    HIDE,
    LEVITATE,
    REFLOW
}

/**
 * Back navigation behavior for pane containers.
 */
enum class PaneBackBehavior {
    POP_UNTIL_CONTENT_CHANGE,
    POP_LATEST
}
```

### ScreenInfo

Represents metadata extracted from `@Screen` annotations.

```kotlin
package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Extracted metadata from a @Screen annotation.
 *
 * @property functionDeclaration The KSP function declaration for this screen
 * @property functionName Simple function name (e.g., "DetailScreen")
 * @property destinationClass Class declaration for the destination this screen renders
 * @property hasDestinationParam True if the function accepts a destination parameter
 * @property hasSharedTransitionScope True if the function accepts SharedTransitionScope
 * @property hasAnimatedVisibilityScope True if the function accepts AnimatedVisibilityScope
 * @property packageName Package containing this function
 */
data class ScreenInfo(
    val functionDeclaration: KSFunctionDeclaration,
    val functionName: String,
    val destinationClass: KSClassDeclaration,
    val hasDestinationParam: Boolean,
    val hasSharedTransitionScope: Boolean,
    val hasAnimatedVisibilityScope: Boolean,
    val packageName: String
)
```

---

## Extractor Implementations

### 1. DestinationExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.ParamInfo
import com.jermey.quo.vadis.ksp.models.SerializerType

/**
 * Extracts @Destination annotations into DestinationInfo models.
 * 
 * Enhanced to extract @Argument annotations from constructor parameters,
 * enabling type-safe navigation arguments with proper serialization.
 */
class DestinationExtractor(
    private val logger: KSPLogger
) {
    
    companion object {
        private const val ARGUMENT_ANNOTATION = "Argument"
        private const val SERIALIZABLE_ANNOTATION = "Serializable"
        
        // Primitive type qualified names for serializer detection
        private val PRIMITIVE_TYPES = mapOf(
            "kotlin.String" to SerializerType.STRING,
            "kotlin.Int" to SerializerType.INT,
            "kotlin.Long" to SerializerType.LONG,
            "kotlin.Float" to SerializerType.FLOAT,
            "kotlin.Double" to SerializerType.DOUBLE,
            "kotlin.Boolean" to SerializerType.BOOLEAN
        )
    }
    
    /**
     * Extract DestinationInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @Destination
     * @return DestinationInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): DestinationInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Destination"
        } ?: return null
        
        val route = annotation.arguments.find { 
            it.name?.asString() == "route" 
        }?.value as? String
        
        val routeParams = route?.let { extractRouteParams(it) } ?: emptyList()
        
        val isDataObject = classDeclaration.modifiers.contains(Modifier.DATA) &&
                          classDeclaration.classKind.name == "OBJECT"
        
        val isDataClass = classDeclaration.modifiers.contains(Modifier.DATA) &&
                         classDeclaration.classKind.name == "CLASS"
        
        val constructorParams = if (isDataClass) {
            extractConstructorParams(classDeclaration, routeParams)
        } else {
            emptyList()
        }
        
        val parentSealedClass = classDeclaration.parentDeclaration?.let {
            (it as? KSClassDeclaration)?.simpleName?.asString()
        }
        
        return DestinationInfo(
            classDeclaration = classDeclaration,
            className = classDeclaration.simpleName.asString(),
            qualifiedName = classDeclaration.qualifiedName?.asString() ?: "",
            route = route,
            routeParams = routeParams,
            isDataObject = isDataObject,
            isDataClass = isDataClass,
            constructorParams = constructorParams,
            parentSealedClass = parentSealedClass
        )
    }
    
    /**
     * Extract all destinations from a container (sealed class).
     */
    fun extractFromContainer(containerClass: KSClassDeclaration): List<DestinationInfo> {
        return containerClass.getSealedSubclasses()
            .mapNotNull { extract(it) }
            .toList()
    }
    
    private fun extractRouteParams(route: String): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(route).map { it.groupValues[1] }.toList()
    }
    
    /**
     * Extract constructor parameters with @Argument annotation support.
     * 
     * For each parameter:
     * 1. Check for @Argument annotation
     * 2. Extract key (custom or parameter name)
     * 3. Extract optional flag
     * 4. Determine serializer type from parameter type
     */
    private fun extractConstructorParams(
        classDeclaration: KSClassDeclaration,
        routeParams: List<String>
    ): List<ParamInfo> {
        val primaryConstructor = classDeclaration.primaryConstructor ?: return emptyList()
        
        return primaryConstructor.parameters.map { param ->
            val argumentAnnotation = param.annotations.find {
                it.shortName.asString() == ARGUMENT_ANNOTATION
            }
            
            val isArgument = argumentAnnotation != null
            val paramName = param.name?.asString() ?: ""
            
            // Extract @Argument properties
            val argumentKey = if (isArgument) {
                val customKey = argumentAnnotation?.arguments?.find {
                    it.name?.asString() == "key"
                }?.value as? String
                customKey?.takeIf { it.isNotEmpty() } ?: paramName
            } else {
                paramName
            }
            
            val isOptionalArgument = argumentAnnotation?.arguments?.find {
                it.name?.asString() == "optional"
            }?.value as? Boolean ?: false
            
            val type = param.type.resolve()
            val serializerType = determineSerializerType(param, type)
            
            ParamInfo(
                name = paramName,
                type = type,
                hasDefault = param.hasDefault,
                isArgument = isArgument,
                argumentKey = argumentKey,
                isOptionalArgument = isOptionalArgument,
                serializerType = serializerType
            )
        }
    }
    
    /**
     * Determine the appropriate serializer type for a parameter.
     * 
     * Priority:
     * 1. Primitive types (String, Int, Long, etc.)
     * 2. Enum types
     * 3. @Serializable types (JSON)
     * 4. Default to STRING for unknown types
     */
    private fun determineSerializerType(
        param: KSValueParameter,
        type: com.google.devtools.ksp.symbol.KSType
    ): SerializerType {
        val qualifiedName = type.declaration.qualifiedName?.asString()
        
        // Check primitive types
        PRIMITIVE_TYPES[qualifiedName]?.let { return it }
        
        // Check if it's an enum
        val declaration = type.declaration
        if (declaration is KSClassDeclaration && 
            declaration.classKind.name == "ENUM_CLASS") {
            return SerializerType.ENUM
        }
        
        // Check for @Serializable annotation (kotlinx.serialization)
        if (declaration is KSClassDeclaration) {
            val hasSerializable = declaration.annotations.any {
                it.shortName.asString() == SERIALIZABLE_ANNOTATION
            }
            if (hasSerializable) {
                return SerializerType.JSON
            }
        }
        
        // Check if it's a collection type (List, Set, etc.)
        if (qualifiedName?.startsWith("kotlin.collections.") == true ||
            qualifiedName?.startsWith("java.util.") == true) {
            return SerializerType.JSON
        }
        
        // Default to STRING for simple types or report warning
        logger.warn("Unknown type for @Argument: $qualifiedName, defaulting to STRING serialization")
        return SerializerType.STRING
    }
}
```

### 2. StackExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jermey.quo.vadis.ksp.models.StackInfo

/**
 * Extracts @Stack annotations into StackInfo models.
 */
class StackExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {
    
    /**
     * Extract StackInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Stack
     * @return StackInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): StackInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Stack"
        } ?: return null
        
        val name = annotation.arguments.find { 
            it.name?.asString() == "name" 
        }?.value as? String ?: return null
        
        val startDestination = annotation.arguments.find { 
            it.name?.asString() == "startDestination" 
        }?.value as? String ?: ""
        
        val destinations = destinationExtractor.extractFromContainer(classDeclaration)
        
        val resolvedStart = destinations.find { 
            it.className == startDestination 
        } ?: destinations.firstOrNull()
        
        return StackInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            startDestination = startDestination,
            destinations = destinations,
            resolvedStartDestination = resolvedStart
        )
    }
    
    /**
     * Extract all @Stack-annotated classes from the resolver.
     */
    fun extractAll(resolver: Resolver): List<StackInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Stack")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
```

### 3. TabExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo

/**
 * Extracts @Tab and @TabItem annotations into TabInfo models.
 */
class TabExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {
    
    /**
     * Extract TabInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Tab
     * @return TabInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): TabInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Tab"
        } ?: return null
        
        val name = annotation.arguments.find { 
            it.name?.asString() == "name" 
        }?.value as? String ?: return null
        
        val initialTab = annotation.arguments.find { 
            it.name?.asString() == "initialTab" 
        }?.value as? String ?: ""
        
        val tabs = classDeclaration.getSealedSubclasses()
            .mapNotNull { extractTabItem(it) }
            .toList()
        
        return TabInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            initialTab = initialTab,
            tabs = tabs
        )
    }
    
    private fun extractTabItem(classDeclaration: KSClassDeclaration): TabItemInfo? {
        val tabItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "TabItem"
        } ?: return null
        
        val destination = destinationExtractor.extract(classDeclaration) ?: return null
        
        val label = tabItemAnnotation.arguments.find { 
            it.name?.asString() == "label" 
        }?.value as? String ?: ""
        
        val icon = tabItemAnnotation.arguments.find { 
            it.name?.asString() == "icon" 
        }?.value as? String ?: ""
        
        val rootGraphType = tabItemAnnotation.arguments.find { 
            it.name?.asString() == "rootGraph" 
        }?.value as? KSType
        
        val rootGraphClass = rootGraphType?.declaration as? KSClassDeclaration ?: return null
        
        return TabItemInfo(
            destination = destination,
            label = label,
            icon = icon,
            rootGraphClass = rootGraphClass
        )
    }
    
    /**
     * Extract all @Tab-annotated classes from the resolver.
     */
    fun extractAll(resolver: Resolver): List<TabInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tab")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
```

### 4. PaneExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.*

/**
 * Extracts @Pane and @PaneItem annotations into PaneInfo models.
 */
class PaneExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {
    
    /**
     * Extract PaneInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Pane
     * @return PaneInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): PaneInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Pane"
        } ?: return null
        
        val name = annotation.arguments.find { 
            it.name?.asString() == "name" 
        }?.value as? String ?: return null
        
        val backBehaviorStr = annotation.arguments.find { 
            it.name?.asString() == "backBehavior" 
        }?.value?.toString() ?: "POP_UNTIL_CONTENT_CHANGE"
        
        val backBehavior = PaneBackBehavior.valueOf(backBehaviorStr)
        
        val panes = classDeclaration.getSealedSubclasses()
            .mapNotNull { extractPaneItem(it) }
            .toList()
        
        return PaneInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            backBehavior = backBehavior,
            panes = panes
        )
    }
    
    private fun extractPaneItem(classDeclaration: KSClassDeclaration): PaneItemInfo? {
        val paneItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "PaneItem"
        } ?: return null
        
        val destination = destinationExtractor.extract(classDeclaration) ?: return null
        
        val roleStr = paneItemAnnotation.arguments.find { 
            it.name?.asString() == "role" 
        }?.value?.toString() ?: "PRIMARY"
        
        val adaptStrategyStr = paneItemAnnotation.arguments.find { 
            it.name?.asString() == "adaptStrategy" 
        }?.value?.toString() ?: "HIDE"
        
        val rootGraphType = paneItemAnnotation.arguments.find { 
            it.name?.asString() == "rootGraph" 
        }?.value as? KSType
        
        val rootGraphClass = rootGraphType?.declaration as? KSClassDeclaration ?: return null
        
        return PaneItemInfo(
            destination = destination,
            role = PaneRole.valueOf(roleStr),
            adaptStrategy = AdaptStrategy.valueOf(adaptStrategyStr),
            rootGraphClass = rootGraphClass
        )
    }
    
    /**
     * Extract all @Pane-annotated classes from the resolver.
     */
    fun extractAll(resolver: Resolver): List<PaneInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Pane")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
```

### 5. ScreenExtractor

```kotlin
package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.ScreenInfo

/**
 * Extracts @Screen annotations into ScreenInfo models.
 */
class ScreenExtractor(
    private val logger: KSPLogger
) {
    
    /**
     * Extract ScreenInfo from a function declaration.
     *
     * @param functionDeclaration The @Composable function annotated with @Screen
     * @return ScreenInfo or null if extraction fails
     */
    fun extract(functionDeclaration: KSFunctionDeclaration): ScreenInfo? {
        val annotation = functionDeclaration.annotations.find {
            it.shortName.asString() == "Screen"
        } ?: return null
        
        val destinationType = annotation.arguments.firstOrNull()?.value as? KSType
        val destinationClass = destinationType?.declaration as? KSClassDeclaration ?: return null
        
        val parameters = functionDeclaration.parameters
        
        val hasDestinationParam = parameters.any { param ->
            val paramType = param.type.resolve()
            paramType.declaration.qualifiedName?.asString() == 
                destinationClass.qualifiedName?.asString()
        }
        
        val hasSharedTransitionScope = parameters.any { param ->
            param.type.resolve().declaration.simpleName.asString() == "SharedTransitionScope"
        }
        
        val hasAnimatedVisibilityScope = parameters.any { param ->
            param.type.resolve().declaration.simpleName.asString() == "AnimatedVisibilityScope"
        }
        
        return ScreenInfo(
            functionDeclaration = functionDeclaration,
            functionName = functionDeclaration.simpleName.asString(),
            destinationClass = destinationClass,
            hasDestinationParam = hasDestinationParam,
            hasSharedTransitionScope = hasSharedTransitionScope,
            hasAnimatedVisibilityScope = hasAnimatedVisibilityScope,
            packageName = functionDeclaration.packageName.asString()
        )
    }
    
    /**
     * Extract all @Screen-annotated functions from the resolver.
     */
    fun extractAll(resolver: Resolver): List<ScreenInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Screen")
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
```

---

## Implementation Steps

### Step 1: Create Model Classes (Day 1 - 4 hours)

Create all model data classes in `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/`:

1. `DestinationInfo.kt`
2. `ParamInfo.kt`
3. `StackInfo.kt`
4. `TabInfo.kt` and `TabItemInfo.kt`
5. `PaneInfo.kt` and `PaneItemInfo.kt`
6. `ScreenInfo.kt`

### Step 2: Implement DestinationExtractor (Day 1 - 3 hours)

Create `DestinationExtractor.kt` with:
- Route parameter extraction from pattern strings
- Constructor parameter extraction
- Data object vs data class detection
- Parent sealed class detection

### Step 3: Implement Container Extractors (Day 2 - 6 hours)

Create extractors in order:
1. `StackExtractor.kt` - delegates to DestinationExtractor
2. `TabExtractor.kt` - handles @TabItem sub-annotations
3. `PaneExtractor.kt` - handles @PaneItem sub-annotations

### Step 4: Implement ScreenExtractor (Day 2 - 2 hours)

Create `ScreenExtractor.kt` with:
- Function parameter analysis
- Scope parameter detection (SharedTransitionScope, AnimatedVisibilityScope)

### Step 5: Write Unit Tests (Day 3 - 4 hours)

Create comprehensive tests for each extractor:
- Happy path extraction
- Missing annotation handling
- Edge cases (empty routes, default values)

### Step 6: Integration Testing (Day 3 - 2 hours)

Test all extractors together in the processor pipeline.

---

## Code Examples

### Input Annotations

```kotlin
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/detail/{id}")
    data class Detail(
        @Argument val id: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()
}

@Tab(name = "main", initialTab = "Home")
sealed class MainTabs : Destination {
    
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tab/home")
    data object Home : MainTabs()
    
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tab/profile")
    data object Profile : MainTabs()
}

@Screen(HomeDestination.Detail::class)
@Composable
fun DetailScreen(destination: HomeDestination.Detail, navigator: Navigator) { }
```

### Extracted Output

```kotlin
// ParamInfo for Detail.id
ParamInfo(
    name = "id",
    type = String::class,
    hasDefault = false,
    isArgument = true,
    argumentKey = "id",
    isOptionalArgument = false,
    serializerType = SerializerType.STRING
)

// ParamInfo for Detail.showComments
ParamInfo(
    name = "showComments",
    type = Boolean::class,
    hasDefault = true,
    isArgument = true,
    argumentKey = "showComments",
    isOptionalArgument = true,
    serializerType = SerializerType.BOOLEAN
)

// DestinationInfo for Detail
DestinationInfo(
    className = "Detail",
    qualifiedName = "com.example.HomeDestination.Detail",
    route = "home/detail/{id}",
    routeParams = listOf("id"),
    isDataObject = false,
    isDataClass = true,
    constructorParams = listOf(idParamInfo, showCommentsParamInfo),
    parentSealedClass = "HomeDestination"
)

// StackInfo for HomeDestination
StackInfo(
    name = "home",
    className = "HomeDestination",
    packageName = "com.example",
    startDestination = "Feed",
    destinations = listOf(feedDestInfo, detailDestInfo),
    resolvedStartDestination = feedDestInfo
)

// TabInfo for MainTabs
TabInfo(
    name = "main",
    className = "MainTabs",
    packageName = "com.example",
    initialTab = "Home",
    tabs = listOf(
        TabItemInfo(destination = homeTabDest, label = "Home", icon = "home", ...),
        TabItemInfo(destination = profileTabDest, label = "Profile", icon = "person", ...)
    )
)

// ScreenInfo for DetailScreen
ScreenInfo(
    functionName = "DetailScreen",
    destinationClass = HomeDestination.Detail::class,
    hasDestinationParam = true,
    hasSharedTransitionScope = false,
    hasAnimatedVisibilityScope = false,
    packageName = "com.example"
)
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../models/DestinationInfo.kt` | Create | Destination metadata model |
| `quo-vadis-ksp/src/main/kotlin/.../models/ParamInfo.kt` | **Update** | Add @Argument fields (isArgument, argumentKey, etc.) |
| `quo-vadis-ksp/src/main/kotlin/.../models/SerializerType.kt` | **Create** | Enum for serialization strategies |
| `quo-vadis-ksp/src/main/kotlin/.../models/StackInfo.kt` | Create | Stack metadata model |
| `quo-vadis-ksp/src/main/kotlin/.../models/TabInfo.kt` | Create | Tab/TabItem metadata models |
| `quo-vadis-ksp/src/main/kotlin/.../models/PaneInfo.kt` | Create | Pane/PaneItem metadata models |
| `quo-vadis-ksp/src/main/kotlin/.../models/ScreenInfo.kt` | Create | Screen binding metadata model |
| `quo-vadis-ksp/src/main/kotlin/.../extractors/DestinationExtractor.kt` | **Update** | Extract @Argument from parameters |
| `quo-vadis-ksp/src/main/kotlin/.../extractors/StackExtractor.kt` | Create | @Stack extractor |
| `quo-vadis-ksp/src/main/kotlin/.../extractors/TabExtractor.kt` | Create | @Tab/@TabItem extractor |
| `quo-vadis-ksp/src/main/kotlin/.../extractors/PaneExtractor.kt` | Create | @Pane/@PaneItem extractor |
| `quo-vadis-ksp/src/main/kotlin/.../extractors/ScreenExtractor.kt` | Create | @Screen extractor |

---

## Acceptance Criteria

- [ ] All model data classes created with proper KDoc documentation
- [ ] `ParamInfo` includes @Argument fields (isArgument, argumentKey, isOptionalArgument, serializerType)
- [ ] `SerializerType` enum created with all supported types
- [ ] `DestinationExtractor` correctly extracts @Argument annotations from parameters
- [ ] `DestinationExtractor` correctly determines SerializerType from parameter types
- [ ] `DestinationExtractor` correctly extracts route params and constructor params
- [ ] `StackExtractor` resolves start destination from destinations list
- [ ] `TabExtractor` extracts nested @TabItem annotations with rootGraph references
- [ ] `PaneExtractor` extracts nested @PaneItem annotations with role and strategy
- [ ] `ScreenExtractor` detects optional scope parameters
- [ ] All extractors handle missing/malformed annotations gracefully
- [ ] Unit tests for each extractor with >80% coverage
- [ ] Unit tests for @Argument extraction (key, optional, serializerType)
- [ ] Integration test verifying all extractors work together
- [ ] KSP module compiles successfully

---

## Testing Notes

### Unit Test Examples

```kotlin
class DestinationExtractorTest {
    
    @Test
    fun `extracts route params from pattern`() {
        // Given a route pattern "home/detail/{id}/{section}"
        // When extracted
        // Then routeParams = ["id", "section"]
    }
    
    @Test
    fun `identifies data object correctly`() {
        // Given: data object Feed : HomeDestination()
        // When extracted
        // Then: isDataObject = true, isDataClass = false
    }
    
    @Test
    fun `extracts constructor params for data class`() {
        // Given: data class Detail(val id: String, val title: String = "")
        // When extracted
        // Then: constructorParams has 2 entries, second has hasDefault = true
    }
    
    @Test
    fun `extracts @Argument annotation with default key`() {
        // Given: data class Detail(@Argument val id: String)
        // When extracted
        // Then: param.isArgument = true, param.argumentKey = "id"
    }
    
    @Test
    fun `extracts @Argument annotation with custom key`() {
        // Given: data class Search(@Argument(key = "query") val searchTerm: String)
        // When extracted
        // Then: param.argumentKey = "query"
    }
    
    @Test
    fun `extracts @Argument optional flag`() {
        // Given: data class Detail(@Argument(optional = true) val ref: String? = null)
        // When extracted
        // Then: param.isOptionalArgument = true
    }
    
    @Test
    fun `determines SerializerType for primitives`() {
        // Given: @Argument val count: Int
        // When extracted
        // Then: param.serializerType = SerializerType.INT
    }
    
    @Test
    fun `determines SerializerType for enums`() {
        // Given: @Argument val sort: SortOption
        // When extracted
        // Then: param.serializerType = SerializerType.ENUM
    }
    
    @Test
    fun `determines SerializerType for @Serializable classes`() {
        // Given: @Argument val filters: FilterOptions (where FilterOptions is @Serializable)
        // When extracted
        // Then: param.serializerType = SerializerType.JSON
    }
}

class StackExtractorTest {
    
    @Test
    fun `resolves start destination by name`() {
        // Given: @Stack(name = "home", startDestination = "Detail")
        // When extracted
        // Then: resolvedStartDestination.className == "Detail"
    }
    
    @Test
    fun `falls back to first destination if start not found`() {
        // Given: @Stack(name = "home", startDestination = "Unknown")
        // When extracted
        // Then: resolvedStartDestination == destinations.first()
    }
}
```

---

## References

- [INDEX.md](../INDEX.md) - Phase 4 KSP Overview
- [ANN-001](../phase4-annotations/ANN-001-graph-type.md) - @Destination Annotation
- [ANN-002](../phase4-annotations/ANN-002-pane-graph.md) - @Stack Annotation
- [ANN-003](../phase4-annotations/ANN-003-route-transitions.md) - @Tab/@TabItem Annotations
- [ANN-004](../phase4-annotations/ANN-004-shared-element.md) - @Pane/@PaneItem Annotations
- [ANN-005](../phase4-annotations/ANN-005-screen.md) - @Screen Annotation
