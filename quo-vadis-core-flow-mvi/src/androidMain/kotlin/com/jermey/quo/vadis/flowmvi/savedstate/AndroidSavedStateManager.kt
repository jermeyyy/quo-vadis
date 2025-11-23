package com.jermey.quo.vadis.flowmvi.savedstate

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android implementation using SharedPreferences.
 * 
 * Provides persistent storage that survives:
 * - Configuration changes
 * - Process death
 * - App restarts
 * 
 * Optional encryption using AndroidX Security library.
 */
class AndroidSavedStateManager(
    private val context: Context,
    private val prefsName: String = "navigation_state",
    private val useEncryption: Boolean = false
) : SavedStateManager {
    
    private val prefs: SharedPreferences by lazy {
        if (useEncryption) {
            createEncryptedPrefs()
        } else {
            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        }
    }
    
    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            prefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun save(key: String, value: String): Boolean {
        return try {
            prefs.edit().putString(key, value).commit()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun restore(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun delete(key: String): Boolean {
        return try {
            prefs.edit().remove(key).commit()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun clearAll(): Boolean {
        return try {
            prefs.edit().clear().commit()
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun exists(key: String): Boolean {
        return prefs.contains(key)
    }
}

/**
 * Android platform-specific factory.
 * Note: Requires Context which should be provided via Koin or similar.
 */
actual fun createPlatformSavedStateManager(): SavedStateManager {
    // Return in-memory by default (Context must be provided explicitly)
    return InMemorySavedStateManager()
}

/**
 * Factory function to create Android state manager with context.
 * 
 * Usage with Koin:
 * ```kotlin
 * single<SavedStateManager> {
 *     createAndroidSavedStateManager(
 *         context = androidContext(),
 *         useEncryption = false
 *     )
 * }
 * ```
 */
fun createAndroidSavedStateManager(
    context: Context,
    prefsName: String = "navigation_state",
    useEncryption: Boolean = false
): SavedStateManager {
    return AndroidSavedStateManager(context, prefsName, useEncryption)
}
