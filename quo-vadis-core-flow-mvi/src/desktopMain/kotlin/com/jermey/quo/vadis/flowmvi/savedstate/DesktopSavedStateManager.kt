package com.jermey.quo.vadis.flowmvi.savedstate

import java.io.File
import java.util.prefs.Preferences

/**
 * Desktop (JVM) implementation using Java Preferences API.
 * 
 * Preferences are stored in platform-specific locations:
 * - macOS: ~/Library/Preferences
 * - Windows: Registry
 * - Linux: ~/.java/.userPrefs
 * 
 * Alternative: File-based storage in user's app data directory.
 */
class DesktopSavedStateManager(
    private val nodeName: String = "com.jermey.quo.vadis.navigation",
    private val useFileStorage: Boolean = false,
    private val storageDir: File? = null
) : SavedStateManager {
    
    private val prefs: Preferences? = if (!useFileStorage) {
        Preferences.userRoot().node(nodeName)
    } else {
        null
    }
    
    private val fileStorageDir: File? = if (useFileStorage) {
        storageDir ?: File(System.getProperty("user.home"), ".quo-vadis/state")
            .also { it.mkdirs() }
    } else {
        null
    }
    
    override suspend fun save(key: String, value: String): Boolean {
        return try {
            if (useFileStorage) {
                val file = File(fileStorageDir, "$key.json")
                file.writeText(value)
            } else {
                prefs?.put(key, value)
                prefs?.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun restore(key: String): String? {
        return try {
            if (useFileStorage) {
                val file = File(fileStorageDir, "$key.json")
                if (file.exists()) file.readText() else null
            } else {
                prefs?.get(key, null)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun delete(key: String): Boolean {
        return try {
            if (useFileStorage) {
                val file = File(fileStorageDir, "$key.json")
                file.delete()
            } else {
                prefs?.remove(key)
                prefs?.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearAll(): Boolean {
        return try {
            if (useFileStorage) {
                fileStorageDir?.listFiles()?.forEach { it.delete() }
            } else {
                prefs?.clear()
                prefs?.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun exists(key: String): Boolean {
        return try {
            if (useFileStorage) {
                File(fileStorageDir, "$key.json").exists()
            } else {
                prefs?.get(key, null) != null
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Desktop platform-specific factory.
 */
actual fun createPlatformSavedStateManager(): SavedStateManager {
    return DesktopSavedStateManager()
}

/**
 * Factory to create desktop state manager with custom settings.
 * 
 * Usage:
 * ```kotlin
 * // Using Java Preferences (default)
 * val prefsManager = createDesktopSavedStateManager()
 * 
 * // Using file-based storage
 * val fileManager = createDesktopSavedStateManager(
 *     useFileStorage = true,
 *     storageDir = File(System.getProperty("user.home"), ".myapp")
 * )
 * ```
 */
fun createDesktopSavedStateManager(
    nodeName: String = "com.jermey.quo.vadis.navigation",
    useFileStorage: Boolean = false,
    storageDir: File? = null
): SavedStateManager {
    return DesktopSavedStateManager(nodeName, useFileStorage, storageDir)
}
