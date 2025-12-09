# HIER-003: TransitionRegistry Interface

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-003 |
| **Task Name** | Define TransitionRegistry Interface |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Small |
| **Estimated Time** | 0.5 day |
| **Dependencies** | HIER-004 (NavTransition) |
| **Blocked By** | - |
| **Blocks** | HIER-005, HIER-014 |

---

## Overview

The `TransitionRegistry` interface defines how destination-specific transitions are resolved at runtime. KSP generates an implementation that maps `@Transition`-annotated destinations to their corresponding `NavTransition` configurations.

### Purpose

- Define the contract for transition lookup
- Enable per-destination animation customization
- Provide null-returning default for destinations without annotations
- Support testing with fake registries

### Design Decisions

1. **KClass-based lookup**: Uses destination class as key for type safety
2. **Nullable return**: Returns null for unregistered destinations (fallback to default)
3. **Single method**: Simple interface focused on one responsibility

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/TransitionRegistry.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.registry

import com.jermey.quo.vadis.core.navigation.Destination
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import kotlin.reflect.KClass

/**
 * Registry for resolving destination-specific transitions at runtime.
 *
 * This interface is implemented by KSP-generated code that maps
 * `@Transition` annotated destinations to their [NavTransition] configurations.
 *
 * ## How It Works
 *
 * 1. User annotates destinations with `@Transition(TransitionType.Fade)`
 * 2. KSP generates `GeneratedTransitionRegistry` implementing this interface
 * 3. [AnimationCoordinator] queries this registry when resolving transitions
 * 4. If no annotation exists, null is returned and default transition is used
 *
 * ## Example Generated Code
 *
 * ```kotlin
 * object GeneratedTransitionRegistry : TransitionRegistry {
 *     override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
 *         return when (destinationClass) {
 *             DetailScreen::class -> NavTransition.SlideHorizontal
 *             SettingsScreen::class -> NavTransition.Fade
 *             ModalDialog::class -> NavTransition.SlideVertical
 *             else -> null
 *         }
 *     }
 * }
 * ```
 *
 * @see NavTransition
 * @see AnimationCoordinator
 * @see DefaultTransitionRegistry
 */
interface TransitionRegistry {
    
    /**
     * Gets the transition configuration for a destination class.
     *
     * @param destinationClass The [KClass] of the destination to look up
     * @return The [NavTransition] if annotated, null otherwise
     */
    fun getTransition(destinationClass: KClass<out Destination>): NavTransition?
}

/**
 * Extension function for type-safe transition lookup.
 *
 * ## Example
 *
 * ```kotlin
 * val transition = registry.getTransition<DetailScreen>()
 * ```
 */
inline fun <reified T : Destination> TransitionRegistry.getTransition(): NavTransition? {
    return getTransition(T::class)
}

/**
 * Default transition registry that returns null for all lookups.
 *
 * Used when:
 * - No transitions are annotated in the project
 * - Testing without KSP-generated code
 * - Fallback behavior desired
 *
 * @see TransitionRegistry
 */
object DefaultTransitionRegistry : TransitionRegistry {
    
    /**
     * Always returns null, indicating no specific transition is configured.
     *
     * The [AnimationCoordinator] will use its default transition when
     * this returns null.
     */
    override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? = null
}

/**
 * Composite registry that checks multiple registries in order.
 *
 * Useful for combining generated registries with programmatic overrides.
 *
 * ## Example
 *
 * ```kotlin
 * val registry = CompositeTransitionRegistry(
 *     MyCustomRegistry,           // Check first
 *     GeneratedTransitionRegistry // Fallback
 * )
 * ```
 */
class CompositeTransitionRegistry(
    private vararg val registries: TransitionRegistry
) : TransitionRegistry {
    
    override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
        for (registry in registries) {
            registry.getTransition(destinationClass)?.let { return it }
        }
        return null
    }
}
```

---

## Integration Points

### Providers

- **KSP Generator** (HIER-014): Generates `GeneratedTransitionRegistry`
- **HierarchicalQuoVadisHost** (HIER-024): Provides registry to AnimationCoordinator

### Consumers

- **AnimationCoordinator** (HIER-005): Queries for destination transitions

### Related Components

| Component | Relationship |
|-----------|--------------|
| `NavTransition` | Return type (HIER-004) |
| `@Transition` annotation | Marks destinations (HIER-011) |
| `AnimationCoordinator` | Primary consumer (HIER-005) |

---

## Testing Requirements

### Unit Tests

```kotlin
class TransitionRegistryTest {
    
    @Test
    fun `DefaultTransitionRegistry returns null for any class`() {
        assertNull(DefaultTransitionRegistry.getTransition(TestDestination::class))
        assertNull(DefaultTransitionRegistry.getTransition(AnotherDestination::class))
    }
    
    @Test
    fun `inline getTransition extension works`() {
        val registry = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
                return if (destinationClass == TestDestination::class) {
                    NavTransition.Fade
                } else null
            }
        }
        
        assertEquals(NavTransition.Fade, registry.getTransition<TestDestination>())
        assertNull(registry.getTransition<AnotherDestination>())
    }
    
    @Test
    fun `CompositeTransitionRegistry checks registries in order`() {
        val first = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
                return if (destinationClass == TestDestination::class) NavTransition.Fade else null
            }
        }
        
        val second = object : TransitionRegistry {
            override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
                return NavTransition.SlideHorizontal // Default for all
            }
        }
        
        val composite = CompositeTransitionRegistry(first, second)
        
        // First registry wins for TestDestination
        assertEquals(NavTransition.Fade, composite.getTransition<TestDestination>())
        // Falls through to second for others
        assertEquals(NavTransition.SlideHorizontal, composite.getTransition<AnotherDestination>())
    }
    
    @Test
    fun `CompositeTransitionRegistry returns null when all return null`() {
        val composite = CompositeTransitionRegistry(
            DefaultTransitionRegistry,
            DefaultTransitionRegistry
        )
        
        assertNull(composite.getTransition<TestDestination>())
    }
}

// Test fixtures
private data object TestDestination : Destination {
    override val route = "test"
}

private data object AnotherDestination : Destination {
    override val route = "another"
}
```

---

## Acceptance Criteria

- [ ] `TransitionRegistry` interface with `getTransition(KClass)` method
- [ ] Inline reified extension function `getTransition<T>()`
- [ ] `DefaultTransitionRegistry` returning null
- [ ] `CompositeTransitionRegistry` for combining multiple registries
- [ ] Full KDoc documentation with examples
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should we support transition inheritance (base class transitions apply to subclasses)?
2. Should CompositeTransitionRegistry be part of the public API?

### Design Rationale

- **Nullable return**: Allows AnimationCoordinator to apply defaults without special "None" value
- **KClass-based**: Type-safe, works with sealed class destinations
- **Composite registry**: Enables programmatic overrides without modifying generated code
