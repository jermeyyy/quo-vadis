package com.jermey.quo.vadis.flowmvi.savedstate

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation using NSUserDefaults.
 * 
 * Provides persistent storage that survives:
 * - App background/foreground
 * - App restarts
 * - Device reboots
 * 
 * Uses the standard NSUserDefaults suite.
 */
class IosSavedStateManager(
    private val suiteName: String? = null
) : SavedStateManager {
    
    private val userDefaults: NSUserDefaults = if (suiteName != null) {
        NSUserDefaults(suiteName = suiteName)
    } else {
        NSUserDefaults.standardUserDefaults
    }
    
    override suspend fun save(key: String, value: String): Boolean {
        return try {
            userDefaults.setObject(value, forKey = key)
            userDefaults.synchronize()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun restore(key: String): String? {
        return try {
            userDefaults.stringForKey(key)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun delete(key: String): Boolean {
        return try {
            userDefaults.removeObjectForKey(key)
            userDefaults.synchronize()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearAll(): Boolean {
        return try {
            // Clear all keys in this suite
            userDefaults.dictionaryRepresentation().keys.forEach { key ->
                userDefaults.removeObjectForKey(key as String)
            }
            userDefaults.synchronize()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun exists(key: String): Boolean {
        return userDefaults.objectForKey(key) != null
    }
}

/**
 * iOS platform-specific factory.
 */
actual fun createPlatformSavedStateManager(): SavedStateManager {
    return IosSavedStateManager()
}

/**
 * Factory function to create iOS state manager with custom suite.
 * 
 * Usage:
 * ```kotlin
 * val stateManager = createIosSavedStateManager(suiteName = "com.myapp.navigation")
 * ```
 */
fun createIosSavedStateManager(suiteName: String? = null): SavedStateManager {
    return IosSavedStateManager(suiteName)
}
