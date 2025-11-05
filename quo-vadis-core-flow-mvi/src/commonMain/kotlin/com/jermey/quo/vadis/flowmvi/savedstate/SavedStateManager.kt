package com.jermey.quo.vadis.flowmvi.savedstate

/**
 * Platform-agnostic interface for saving and restoring navigation state.
 * 
 * Each platform provides its own implementation:
 * - Android: SavedStateHandle or Bundle
 * - iOS: UserDefaults or NSCoder
 * - Web: localStorage or sessionStorage
 * - Desktop: File-based storage
 * 
 * Implementations should handle:
 * - State persistence to platform-specific storage
 * - State restoration on app restart/process recreation
 * - Error handling for corrupted state
 * - Optional encryption for sensitive data
 */
interface SavedStateManager {
    /**
     * Save a serialized state to storage.
     * 
     * @param key Unique identifier for the state
     * @param value Serialized state (typically JSON string)
     * @return true if save was successful, false otherwise
     */
    suspend fun save(key: String, value: String): Boolean
    
    /**
     * Restore a serialized state from storage.
     * 
     * @param key Unique identifier for the state
     * @return Serialized state if found, null otherwise
     */
    suspend fun restore(key: String): String?
    
    /**
     * Delete a saved state.
     * 
     * @param key Unique identifier for the state
     * @return true if deletion was successful, false otherwise
     */
    suspend fun delete(key: String): Boolean
    
    /**
     * Clear all saved states (optional).
     * Use with caution - may clear other app data.
     */
    suspend fun clearAll(): Boolean
    
    /**
     * Check if a state exists in storage.
     * 
     * @param key Unique identifier for the state
     * @return true if state exists, false otherwise
     */
    suspend fun exists(key: String): Boolean
}

/**
 * In-memory implementation for testing or when persistence is not needed.
 */
class InMemorySavedStateManager : SavedStateManager {
    private val storage = mutableMapOf<String, String>()
    
    override suspend fun save(key: String, value: String): Boolean {
        storage[key] = value
        return true
    }
    
    override suspend fun restore(key: String): String? {
        return storage[key]
    }
    
    override suspend fun delete(key: String): Boolean {
        storage.remove(key)
        return true
    }
    
    override suspend fun clearAll(): Boolean {
        storage.clear()
        return true
    }
    
    override suspend fun exists(key: String): Boolean {
        return storage.containsKey(key)
    }
}

/**
 * Expected platform-specific SavedStateManager.
 * Each platform provides its own implementation in platform source sets.
 */
expect fun createPlatformSavedStateManager(): SavedStateManager
