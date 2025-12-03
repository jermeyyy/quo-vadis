# Task ANN-004: Create @SharedElement Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-004 |
| **Name** | Create @SharedElement Annotation |
| **Phase** | 4 - Annotations Enhancement |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |

## Overview

Create an annotation to mark shared element transition keys in destination definitions. This enables KSP to generate helper code for shared element matching between screens.

## Implementation

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/.../annotations/SharedElementAnnotations.kt

package com.jermey.quo.vadis.annotations

/**
 * Marks a property as a shared element key for transitions.
 * 
 * When navigating between destinations, elements with matching
 * keys will animate smoothly between their positions.
 * 
 * @param key The unique identifier for this shared element.
 *        Must match exactly on source and target destinations.
 * @param type The type of shared element animation
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class SharedElement(
    val key: String,
    val type: SharedElementType = SharedElementType.BOUNDS
)

/**
 * Types of shared element transitions.
 */
enum class SharedElementType {
    /**
     * Animates bounds (position and size).
     * Best for images, icons, and containers.
     */
    BOUNDS,
    
    /**
     * Animates bounds with content crossfade.
     * Best for text and content that changes.
     */
    BOUNDS_TRANSFORM,
    
    /**
     * Custom animation defined separately.
     */
    CUSTOM
}

/**
 * Marks a destination class as participating in shared element transitions.
 * 
 * This enables the generated code to provide SharedTransitionScope
 * to the destination's content composable.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SharedElementDestination
```

## Usage Examples

```kotlin
@Graph(name = "catalog")
sealed class CatalogGraph {
    
    @Route
    data class ProductList(
        @SharedElement(key = "product_image")
        val selectedImageKey: String? = null
    ) : CatalogGraph()
    
    @Route
    @SharedElementDestination
    data class ProductDetail(
        val productId: String,
        @SharedElement(key = "product_image")
        val imageKey: String,
        @SharedElement(key = "product_title", type = SharedElementType.BOUNDS_TRANSFORM)
        val titleKey: String
    ) : CatalogGraph()
}
```

## Generated Code

```kotlin
// Generated: CatalogGraphSharedElements.kt

/**
 * Shared element keys for CatalogGraph.
 */
object CatalogGraphSharedElements {
    const val PRODUCT_IMAGE = "product_image"
    const val PRODUCT_TITLE = "product_title"
    
    fun productImageKey(productId: String) = "product_image_$productId"
    fun productTitleKey(productId: String) = "product_title_$productId"
}

/**
 * Extension to check if destination participates in shared elements.
 */
val Destination.hasSharedElements: Boolean
    get() = this::class.annotations.any { it is SharedElementDestination }
```

## Integration with QuoVadisHost

The renderer uses this information to:
1. Detect which destinations have shared elements
2. Provide `SharedTransitionScope` to those destinations
3. Ensure both source and target are rendered during transition

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/SharedElementAnnotations.kt` | New |

## Acceptance Criteria

- [ ] `@SharedElement` annotation created
- [ ] `@SharedElementDestination` annotation created
- [ ] `SharedElementType` enum created
- [ ] Annotations target correct elements (property, parameter, class)
- [ ] KDoc documentation complete

## References

- [RENDER-004: QuoVadisHost](../phase2-renderer/RENDER-004-quovadis-host.md)
- [Current SharedElementModifiers.kt](../../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/SharedElementModifiers.kt)
