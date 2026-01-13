package com.jermey.navplayground.demo.ui.screens.profile

import kotlinx.coroutines.delay
import org.koin.core.annotation.Factory


/**
 * Simple profile repository for demo purposes.
 * 
 * In a real app, this would:
 * - Call network APIs
 * - Cache data locally
 * - Handle authentication
 * - Provide reactive data streams
 */
@Factory
class ProfileRepository {

    companion object {
        private const val loginDelay = 1000L
        private const val updateUserDelay = 1500L
        private const val logoutDelay = 500L
        private const val nameMaxLenght = 50

        private const val bioMaxLength = 500
    }
    
    // Mock user data
    private var currentUser = UserData(
        id = "user_123",
        name = "John Doe",
        email = "john.doe@example.com",
        bio = "Software engineer passionate about Kotlin Multiplatform and Compose.",
        avatarUrl = null,
        joinedDate = "2024-01-15"
    )
    
    /**
     * Fetch user profile.
     * Simulates network delay.
     */
    suspend fun getUser(): UserData {
        delay(loginDelay) // Simulate network delay
        return currentUser
    }
    
    /**
     * Update user profile.
     * Validates and saves data.
     */
    suspend fun updateUser(
        name: String,
        email: String,
        bio: String
    ): Result<UserData> {
        delay(updateUserDelay) // Simulate network delay
        
        // Validate
        val errors = validateUserData(name, email, bio)
        if (errors.isNotEmpty()) {
            return Result.failure(ValidationException(errors))
        }
        
        // Save
        currentUser = currentUser.copy(
            name = name,
            email = email,
            bio = bio
        )
        
        return Result.success(currentUser)
    }
    
    /**
     * Validate user data.
     */
    private fun validateUserData(
        name: String,
        email: String,
        bio: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (name.isBlank()) {
            errors["name"] = "Name cannot be empty"
        } else if (name.length < 2) {
            errors["name"] = "Name must be at least 2 characters"
        } else if (name.length > nameMaxLenght) {
            errors["name"] = "Name must be less than 50 characters"
        }
        
        if (email.isBlank()) {
            errors["email"] = "Email cannot be empty"
        } else if (!email.contains("@")) {
            errors["email"] = "Invalid email format"
        }
        
        if (bio.length > bioMaxLength) {
            errors["bio"] = "Bio must be less than 500 characters"
        }
        
        return errors
    }
    
    /**
     * Logout user (clear session).
     */
    suspend fun logout() {
        delay(logoutDelay)
        // In real app: clear tokens, cache, etc.
    }
}

/**
 * Validation exception with field errors.
 */
class ValidationException(
    val errors: Map<String, String>
) : Exception("Validation failed: ${errors.size} error(s)")
