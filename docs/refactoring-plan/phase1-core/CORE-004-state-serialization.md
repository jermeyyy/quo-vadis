# CORE-004: Implement NavNode Serialization

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-004 |
| **Task Name** | Implement NavNode Serialization |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | CORE-001 |
| **Blocked By** | CORE-001 |
| **Blocks** | - |

---

## Overview

This task implements comprehensive serialization support for the `NavNode` hierarchy, enabling:

- **Process death survival** on Android (SavedStateHandle integration)
- **State restoration** across app restarts
- **State transfer** between platforms (e.g., debug tools)
- **Testing** with serializable state snapshots

The implementation leverages `kotlinx.serialization` with polymorphic support to handle the sealed class hierarchy.

---

## File Locations

| File | Action |
|------|--------|
| `quo-vadis-core/.../core/NavNode.kt` | Modify (add serialization annotations) |
| `quo-vadis-core/.../serialization/NavNodeSerializer.kt` | Create |
| `quo-vadis-core/.../serialization/StateRestoration.kt` | Create |
| `quo-vadis-core/src/androidMain/.../AndroidStateRestoration.kt` | Create |

---

## Implementation

### Phase 1: Core Serialization Setup

#### NavNode Serialization Annotations (already in CORE-001)

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base sealed interface with serialization support.
 * 
 * The @SerialName annotations ensure stable serialization across versions.
 */
@Serializable
sealed interface NavNode {
    val key: String
    val parentKey: String?
}

@Serializable
@SerialName("screen")
data class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: Destination
) : NavNode

@Serializable
@SerialName("stack")
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList()
) : NavNode {
    val activeChild: NavNode? get() = children.lastOrNull()
    val canGoBack: Boolean get() = children.size > 1
    val isEmpty: Boolean get() = children.isEmpty()
    val size: Int get() = children.size
}

@Serializable
@SerialName("tab")
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0
) : NavNode {
    init {
        require(stacks.isNotEmpty()) { "TabNode must have at least one stack" }
        require(activeStackIndex in stacks.indices) { 
            "activeStackIndex ($activeStackIndex) out of bounds" 
        }
    }
    val activeStack: StackNode get() = stacks[activeStackIndex]
    val tabCount: Int get() = stacks.size
}

@Serializable
@SerialName("pane")
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val panes: List<NavNode>,
    val activePaneIndex: Int = 0
) : NavNode {
    init {
        require(panes.isNotEmpty()) { "PaneNode must have at least one pane" }
        require(activePaneIndex in panes.indices) {
            "activePaneIndex ($activePaneIndex) out of bounds"
        }
    }
    val paneCount: Int get() = panes.size
    val activePane: NavNode get() = panes[activePaneIndex]
}
```

### Phase 2: NavNode Serializer Module

```kotlin
package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Serializers module for NavNode polymorphic serialization.
 * 
 * This module must be registered with any Json instance used to
 * serialize/deserialize NavNode trees.
 * 
 * ## Usage
 * 
 * ```kotlin
 * val json = Json {
 *     serializersModule = navNodeSerializersModule
 * }
 * 
 * val serialized = json.encodeToString(navNode)
 * val restored = json.decodeFromString<NavNode>(serialized)
 * ```
 */
val navNodeSerializersModule = SerializersModule {
    polymorphic(NavNode::class) {
        subclass(ScreenNode::class)
        subclass(StackNode::class)
        subclass(TabNode::class)
        subclass(PaneNode::class)
    }
    
    // Also register Destination hierarchy if it's polymorphic
    polymorphic(Destination::class) {
        // Subclasses should be registered by individual graphs/modules
        // using destinationSerializersModule.extend()
    }
}

/**
 * Pre-configured Json instance for NavNode serialization.
 * 
 * Features:
 * - Polymorphic support for NavNode hierarchy
 * - Class discriminator for type identification
 * - Lenient parsing for forward compatibility
 * - Pretty printing disabled for efficiency
 */
val navNodeJson = Json {
    serializersModule = navNodeSerializersModule
    classDiscriminator = "_type"
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Serializer utilities for NavNode trees.
 */
object NavNodeSerializer {
    
    /**
     * Serialize a NavNode tree to JSON string.
     * 
     * @param node The root node to serialize
     * @return JSON string representation
     */
    fun toJson(node: NavNode): String {
        return navNodeJson.encodeToString(NavNode.serializer(), node)
    }
    
    /**
     * Deserialize a NavNode tree from JSON string.
     * 
     * @param json The JSON string to deserialize
     * @return The deserialized NavNode tree
     * @throws kotlinx.serialization.SerializationException if parsing fails
     */
    fun fromJson(json: String): NavNode {
        return navNodeJson.decodeFromString(NavNode.serializer(), json)
    }
    
    /**
     * Safely deserialize a NavNode tree, returning null on failure.
     * 
     * Use this for state restoration where failures should be handled gracefully.
     * 
     * @param json The JSON string to deserialize
     * @return The deserialized NavNode tree, or null if parsing fails
     */
    fun fromJsonOrNull(json: String?): NavNode? {
        if (json.isNullOrBlank()) return null
        return try {
            fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a Json instance with custom Destination serializers.
     * 
     * Use this when your app has custom Destination subclasses that need
     * to be registered for serialization.
     * 
     * @param additionalModule Additional SerializersModule with Destination subclasses
     * @return Configured Json instance
     */
    fun createJson(additionalModule: SerializersModule): Json {
        return Json {
            serializersModule = navNodeSerializersModule + additionalModule
            classDiscriminator = "_type"
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
}
```

### Phase 3: State Restoration Interface

```kotlin
package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for platform-specific state restoration.
 * 
 * Implementations handle saving and restoring NavNode state
 * in a platform-appropriate manner (e.g., SavedStateHandle on Android,
 * UserDefaults on iOS, localStorage on Web).
 */
interface StateRestoration {
    
    /**
     * Save the navigation state.
     * 
     * @param state The NavNode tree to save
     */
    suspend fun saveState(state: NavNode)
    
    /**
     * Restore the navigation state.
     * 
     * @return The restored NavNode tree, or null if no saved state exists
     */
    suspend fun restoreState(): NavNode?
    
    /**
     * Clear any saved navigation state.
     */
    suspend fun clearState()
    
    /**
     * Whether auto-save is enabled.
     */
    val autoSaveEnabled: Boolean
    
    /**
     * Enable auto-save that persists state on every change.
     * 
     * @param stateFlow The StateFlow to observe for changes
     */
    fun enableAutoSave(stateFlow: StateFlow<NavNode>)
    
    /**
     * Disable auto-save.
     */
    fun disableAutoSave()
}

/**
 * In-memory implementation of StateRestoration for testing.
 */
class InMemoryStateRestoration : StateRestoration {
    private var savedState: NavNode? = null
    private var autoSaveJob: kotlinx.coroutines.Job? = null
    
    override val autoSaveEnabled: Boolean
        get() = autoSaveJob?.isActive == true
    
    override suspend fun saveState(state: NavNode) {
        savedState = state
    }
    
    override suspend fun restoreState(): NavNode? {
        return savedState
    }
    
    override suspend fun clearState() {
        savedState = null
    }
    
    override fun enableAutoSave(stateFlow: StateFlow<NavNode>) {
        // For in-memory, just store the latest value
        autoSaveJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            stateFlow.collect { state ->
                savedState = state
            }
        }
    }
    
    override fun disableAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
}
```

### Phase 4: Android SavedStateHandle Integration

```kotlin
// File: quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/serialization/AndroidStateRestoration.kt

package com.jermey.quo.vadis.core.navigation.serialization

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android implementation of StateRestoration using SavedStateHandle.
 * 
 * Integrates with ViewModel SavedStateHandle for automatic process death survival.
 * 
 * ## Usage with ViewModel
 * 
 * ```kotlin
 * class NavigationViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *     private val stateRestoration = AndroidStateRestoration(savedStateHandle)
 *     
 *     val navigator = TreeNavigator(
 *         initialState = runBlocking { stateRestoration.restoreState() }
 *     ).also { nav ->
 *         stateRestoration.enableAutoSave(nav.state)
 *     }
 * }
 * ```
 */
class AndroidStateRestoration(
    private val savedStateHandle: SavedStateHandle,
    private val key: String = NAV_STATE_KEY
) : StateRestoration {
    
    private var autoSaveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    override val autoSaveEnabled: Boolean
        get() = autoSaveJob?.isActive == true
    
    override suspend fun saveState(state: NavNode) {
        val json = NavNodeSerializer.toJson(state)
        savedStateHandle[key] = json
    }
    
    override suspend fun restoreState(): NavNode? {
        val json: String? = savedStateHandle[key]
        return NavNodeSerializer.fromJsonOrNull(json)
    }
    
    override suspend fun clearState() {
        savedStateHandle.remove<String>(key)
    }
    
    override fun enableAutoSave(stateFlow: StateFlow<NavNode>) {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            stateFlow.collectLatest { state ->
                saveState(state)
            }
        }
    }
    
    override fun disableAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
    
    companion object {
        const val NAV_STATE_KEY = "quo_vadis_nav_state"
    }
}

/**
 * Extension function to create AndroidStateRestoration from Bundle.
 * 
 * Use this for Activity-level state restoration.
 */
fun createStateRestorationFromBundle(
    bundle: Bundle?,
    key: String = AndroidStateRestoration.NAV_STATE_KEY
): NavNode? {
    val json = bundle?.getString(key) ?: return null
    return NavNodeSerializer.fromJsonOrNull(json)
}

/**
 * Extension function to save NavNode to Bundle.
 */
fun Bundle.saveNavState(
    state: NavNode,
    key: String = AndroidStateRestoration.NAV_STATE_KEY
) {
    putString(key, NavNodeSerializer.toJson(state))
}
```

### Phase 5: iOS State Restoration (Optional)

```kotlin
// File: quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/serialization/IosStateRestoration.kt

package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation using NSUserDefaults.
 */
class IosStateRestoration(
    private val key: String = "quo_vadis_nav_state"
) : StateRestoration {
    
    private var autoSaveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    override val autoSaveEnabled: Boolean
        get() = autoSaveJob?.isActive == true
    
    override suspend fun saveState(state: NavNode) {
        val json = NavNodeSerializer.toJson(state)
        userDefaults.setObject(json, key)
    }
    
    override suspend fun restoreState(): NavNode? {
        val json = userDefaults.stringForKey(key) ?: return null
        return NavNodeSerializer.fromJsonOrNull(json)
    }
    
    override suspend fun clearState() {
        userDefaults.removeObjectForKey(key)
    }
    
    override fun enableAutoSave(stateFlow: StateFlow<NavNode>) {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            stateFlow.collect { state ->
                saveState(state)
            }
        }
    }
    
    override fun disableAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
}
```

---

## Handling Custom Destinations

The `Destination` interface also needs serialization support. Since users define their own destinations, the library must provide a way to register custom serializers.

### Destination Serialization Registration

```kotlin
/**
 * Register custom Destination subclasses for serialization.
 * 
 * Call this during app initialization to ensure all destination types
 * can be serialized.
 * 
 * ## Example
 * 
 * ```kotlin
 * val customModule = SerializersModule {
 *     polymorphic(Destination::class) {
 *         subclass(HomeDestination::class)
 *         subclass(ProfileDestination::class)
 *         subclass(SettingsDestination::class)
 *     }
 * }
 * 
 * NavNodeSerializer.registerDestinationModule(customModule)
 * ```
 */
object DestinationSerializerRegistry {
    private var customModule: SerializersModule? = null
    
    fun registerModule(module: SerializersModule) {
        customModule = module
    }
    
    fun getJson(): Json {
        val custom = customModule
        return if (custom != null) {
            NavNodeSerializer.createJson(custom)
        } else {
            navNodeJson
        }
    }
}
```

---

## Serialization Format Example

```json
{
  "_type": "stack",
  "key": "root",
  "parentKey": null,
  "children": [
    {
      "_type": "screen",
      "key": "screen-1",
      "parentKey": "root",
      "destination": {
        "_type": "home",
        "route": "home"
      }
    },
    {
      "_type": "tab",
      "key": "tabs",
      "parentKey": "root",
      "stacks": [
        {
          "_type": "stack",
          "key": "feed-stack",
          "parentKey": "tabs",
          "children": [
            {
              "_type": "screen",
              "key": "screen-2",
              "parentKey": "feed-stack",
              "destination": {
                "_type": "feed",
                "route": "feed"
              }
            }
          ]
        },
        {
          "_type": "stack",
          "key": "profile-stack",
          "parentKey": "tabs",
          "children": [
            {
              "_type": "screen",
              "key": "screen-3",
              "parentKey": "profile-stack",
              "destination": {
                "_type": "profile",
                "route": "profile",
                "userId": "12345"
              }
            }
          ]
        }
      ],
      "activeStackIndex": 0
    }
  ]
}
```

---

## Platform-Specific Considerations

### Android

- **SavedStateHandle**: Primary mechanism for ViewModel state survival
- **Bundle size limit**: ~1MB for process death state
- **Compression**: Consider gzip for large state trees
- **Parcelize**: Alternative to JSON for efficiency (but less portable)

### iOS

- **NSUserDefaults**: Simple key-value storage
- **State Restoration API**: Integration with UIKit restoration
- **Scene-based restoration**: For multi-window apps

### Desktop/Web

- **LocalStorage**: Web persistence
- **FileSystem**: Desktop file-based storage
- **Session storage**: Temporary state during session

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/NavNode.kt` | Modify | Add @SerialName annotations |
| `quo-vadis-core/.../serialization/NavNodeSerializer.kt` | Create | Core serialization utilities |
| `quo-vadis-core/.../serialization/StateRestoration.kt` | Create | Platform abstraction |
| `quo-vadis-core/src/androidMain/.../AndroidStateRestoration.kt` | Create | Android implementation |
| `quo-vadis-core/src/iosMain/.../IosStateRestoration.kt` | Create | iOS implementation (optional) |

---

## Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| CORE-001 | Hard | NavNode hierarchy must be defined |
| kotlinx.serialization | Library | Serialization framework |

### Build Configuration

Ensure `build.gradle.kts` includes:

```kotlin
plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

---

## Acceptance Criteria

- [ ] All NavNode subclasses have `@Serializable` annotation
- [ ] All NavNode subclasses have stable `@SerialName` annotations
- [ ] `navNodeSerializersModule` correctly registers polymorphic types
- [ ] `navNodeJson` pre-configured with appropriate settings
- [ ] `NavNodeSerializer.toJson()` serializes complete trees
- [ ] `NavNodeSerializer.fromJson()` deserializes with type preservation
- [ ] `NavNodeSerializer.fromJsonOrNull()` handles errors gracefully
- [ ] `StateRestoration` interface defined
- [ ] `InMemoryStateRestoration` implementation for testing
- [ ] `AndroidStateRestoration` integrates with SavedStateHandle
- [ ] Destination polymorphic registration mechanism provided
- [ ] Round-trip serialization verified (serialize → deserialize → equal)
- [ ] Large state trees handled efficiently
- [ ] Backward compatibility strategy for schema changes
- [ ] Comprehensive KDoc documentation

---

## Testing Notes

See [CORE-006](./CORE-006-unit-tests.md) for comprehensive test requirements.

```kotlin
@Test
fun `NavNode serialization round-trip preserves structure`() {
    val original = StackNode(
        key = "root",
        parentKey = null,
        children = listOf(
            ScreenNode("s1", "root", BasicDestination("home")),
            TabNode(
                key = "tabs",
                parentKey = "root",
                stacks = listOf(
                    StackNode("t0", "tabs", listOf(
                        ScreenNode("s2", "t0", BasicDestination("feed"))
                    )),
                    StackNode("t1", "tabs", listOf(
                        ScreenNode("s3", "t1", BasicDestination("profile"))
                    ))
                ),
                activeStackIndex = 1
            )
        )
    )
    
    val json = NavNodeSerializer.toJson(original)
    val restored = NavNodeSerializer.fromJson(json)
    
    assertEquals(original, restored)
}

@Test
fun `AndroidStateRestoration survives process death simulation`() {
    val savedStateHandle = SavedStateHandle()
    val restoration = AndroidStateRestoration(savedStateHandle)
    
    runBlocking {
        restoration.saveState(testState)
    }
    
    // Simulate process death - create new restoration with same handle
    val newRestoration = AndroidStateRestoration(savedStateHandle)
    val restored = runBlocking { newRestoration.restoreState() }
    
    assertEquals(testState, restored)
}

@Test
fun `fromJsonOrNull returns null on invalid JSON`() {
    val result = NavNodeSerializer.fromJsonOrNull("invalid json {{{")
    assertNull(result)
}
```

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
- [INDEX](../INDEX.md) - Phase 1 Overview
- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode definitions
- [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization)
- [Android SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)
