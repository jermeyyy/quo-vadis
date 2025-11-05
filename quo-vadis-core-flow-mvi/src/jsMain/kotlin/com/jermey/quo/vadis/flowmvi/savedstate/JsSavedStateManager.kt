package com.jermey.quo.vadis.flowmvi.savedstate

import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import org.w3c.dom.Storage

/**
 * JavaScript/Web implementation using localStorage or sessionStorage.
 * 
 * - localStorage: Persists across browser sessions
 * - sessionStorage: Persists only for the current tab/session
 * 
 * Note: Storage is limited to ~5-10MB per origin.
 */
class JsSavedStateManager(
    private val storage: Storage = localStorage,
    private val prefix: String = "navigation_state_"
) : SavedStateManager {
    
    private fun prefixedKey(key: String) = "$prefix$key"
    
    override suspend fun save(key: String, value: String): Boolean {
        return try {
            storage.setItem(prefixedKey(key), value)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun restore(key: String): String? {
        return try {
            storage.getItem(prefixedKey(key))
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun delete(key: String): Boolean {
        return try {
            storage.removeItem(prefixedKey(key))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearAll(): Boolean {
        return try {
            // Only clear keys with our prefix
            val keysToRemove = mutableListOf<String>()
            for (i in 0 until storage.length) {
                storage.key(i)?.let { key ->
                    if (key.startsWith(prefix)) {
                        keysToRemove.add(key)
                    }
                }
            }
            keysToRemove.forEach { storage.removeItem(it) }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun exists(key: String): Boolean {
        return storage.getItem(prefixedKey(key)) != null
    }
}

/**
 * JavaScript platform-specific factory.
 */
actual fun createPlatformSavedStateManager(): SavedStateManager {
    return JsSavedStateManager(storage = localStorage)
}

/**
 * Factory to create state manager with custom storage.
 * 
 * Usage:
 * ```kotlin
 * // For session-only persistence
 * val sessionManager = createJsSavedStateManager(sessionStorage)
 * 
 * // For cross-session persistence (default)
 * val persistentManager = createJsSavedStateManager(localStorage)
 * ```
 */
fun createJsSavedStateManager(
    storage: Storage = localStorage,
    prefix: String = "navigation_state_"
): SavedStateManager {
    return JsSavedStateManager(storage, prefix)
}
