package com.jermey.quo.vadis.flowmvi.utils

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Helper function to get current timestamp.
 * Uses kotlinx-datetime for multiplatform support.
 */
@OptIn(ExperimentalTime::class)
private fun getCurrentTimestamp(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * State management utilities for navigation.
 * 
 * Provides extension functions and helpers for working with NavigationState,
 * including validation, transformation, and history tracking.
 */

/**
 * Validates if a navigation state is consistent.
 * 
 * Checks:
 * - If backStackSize > 0, currentDestination should not be null
 * - backStackSize should be non-negative
 * 
 * @return true if state is valid, false otherwise
 */
fun NavigationState.isValid(): Boolean {
    return when {
        backStackSize < 0 -> false
        backStackSize > 0 && currentDestination == null -> false
        else -> true
    }
}

/**
 * Returns a default/empty navigation state.
 * Useful for initialization or reset scenarios.
 */
fun emptyNavigationState(): NavigationState = object : NavigationState {
    override val currentDestination: Destination? = null
    override val backStackSize: Int = 0
    override val canGoBack: Boolean = false
    override fun equals(other: Any?): Boolean = other is NavigationState && 
        other.currentDestination == null &&
        other.backStackSize == 0 &&
        !other.canGoBack
    override fun hashCode(): Int = 0
}

/**
 * Creates a navigation state with a single destination.
 * 
 * @param destination The initial destination
 * @return NavigationState with single entry
 */
fun navigationStateOf(destination: Destination): NavigationState = object : NavigationState {
    override val currentDestination: Destination = destination
    override val backStackSize: Int = 1
    override val canGoBack: Boolean = false
    override fun equals(other: Any?): Boolean = other is NavigationState && 
        other.currentDestination == destination &&
        other.backStackSize == 1 &&
        !other.canGoBack
    override fun hashCode(): Int = destination.hashCode() * 31 + 1
}

/**
 * State history tracker for debugging.
 * Maintains a limited history of state changes for time-travel debugging.
 * 
 * @param maxSize Maximum number of states to track (default: 50)
 */
class NavigationStateHistory(private val maxSize: Int = 50) {
    private val history = mutableListOf<NavigationStateSnapshot>()
    
    /**
     * Records a state snapshot.
     * 
     * @param state The state to record
     * @param timestamp Optional timestamp (defaults to current time)
     */
    fun record(state: NavigationState, timestamp: Long = getCurrentTimestamp()) {
        if (history.size >= maxSize) {
            history.removeAt(0)
        }
        history.add(
            NavigationStateSnapshot(
                currentDestination = state.currentDestination,
                backStackSize = state.backStackSize,
                timestamp = timestamp
            )
        )
    }
    
    /**
     * Gets the entire history.
     */
    fun getHistory(): List<NavigationStateSnapshot> = history.toList()
    
    /**
     * Clears the history.
     */
    fun clear() {
        history.clear()
    }
    
    /**
     * Gets the most recent state, or null if empty.
     */
    fun getLatest(): NavigationStateSnapshot? = history.lastOrNull()
    
    /**
     * Gets a state at a specific index (0 = oldest, size-1 = newest).
     */
    fun getAtIndex(index: Int): NavigationStateSnapshot? {
        return history.getOrNull(index)
    }
}

/**
 * Immutable snapshot of navigation state for history tracking.
 * 
 * @property currentDestination The destination at snapshot time
 * @property backStackSize The back stack size at snapshot time
 * @property timestamp When the snapshot was taken
 */
data class NavigationStateSnapshot(
    val currentDestination: Destination?,
    val backStackSize: Int,
    val timestamp: Long
)

/**
 * Extension to check if two navigation states are equivalent.
 * 
 * @param other The state to compare with
 * @return true if states represent the same navigation position
 */
fun NavigationState.isEquivalentTo(other: NavigationState): Boolean {
    return currentDestination == other.currentDestination &&
           backStackSize == other.backStackSize
}

/**
 * Extension to create a copy of a navigation state with modifications.
 * 
 * @param currentDestination New current destination (null = keep existing)
 * @param backStackSize New back stack size (null = keep existing)
 * @return New NavigationState with modifications
 */
/**
 * Extension to copy and modify a NavigationState immutably.
 */
fun NavigationState.copy(
    currentDestination: Destination? = this.currentDestination,
    backStackSize: Int = this.backStackSize,
    canGoBack: Boolean = this.canGoBack
): NavigationState = object : NavigationState {
    override val currentDestination: Destination? = currentDestination
    override val backStackSize: Int = backStackSize
    override val canGoBack: Boolean = canGoBack
    override fun equals(other: Any?): Boolean = other is NavigationState && 
        this.currentDestination == other.currentDestination &&
        this.backStackSize == other.backStackSize &&
        this.canGoBack == other.canGoBack
    override fun hashCode(): Int {
        var result = currentDestination?.hashCode() ?: 0
        result = 31 * result + backStackSize
        result = 31 * result + canGoBack.hashCode()
        return result
    }
}

/**
 * Debug string representation of navigation state.
 */
fun NavigationState.toDebugString(): String {
    return buildString {
        append("NavigationState(")
        append("destination=${currentDestination?.let { it::class.simpleName }}, ")
        append("backStackSize=$backStackSize, ")
        append("canGoBack=$canGoBack")
        append(")")
    }
}
