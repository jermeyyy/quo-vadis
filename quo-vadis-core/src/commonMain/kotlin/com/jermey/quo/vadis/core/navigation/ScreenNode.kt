package com.jermey.quo.vadis.core.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi

/**
 * Leaf node representing a single screen/destination.
 *
 * A ScreenNode is the terminal state in the navigation tree - it cannot
 * contain children. It holds a reference to the [NavDestination] that defines
 * the actual content to render.
 *
 * ## Lifecycle Management
 *
 * ScreenNode implements [LifecycleAwareNode] to provide proper lifecycle
 * state management. The node transitions through states:
 * - Created → Attached (added to navigation tree)
 * - Attached → Displayed (UI enters composition)
 * - Displayed → Attached (UI leaves composition but node remains in tree)
 * - Attached → Destroyed (removed from navigation tree)
 *
 * ## Serialization
 *
 * Only persistent navigation state ([key], [parentKey], [destination]) is serialized.
 * Runtime state ([isAttachedToNavigator], [isDisplayed], [composeSavedState])
 * is marked as `@Transient` and regenerated on restoration.
 *
 * @property key Unique identifier for this screen instance
 * @property parentKey Key of the containing StackNode or PaneNode
 * @property destination The destination data (route, arguments, transitions)
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("screen")
class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: NavDestination
) : NavNode, LifecycleAwareNode {

    // --- Lifecycle Infrastructure (Transient - not serialized) ---

    /**
     * Whether this node is attached to the navigator tree.
     */
    @Transient
    override var isAttachedToNavigator: Boolean = false
        private set

    /**
     * Whether this node is currently being displayed.
     */
    @Transient
    override var isDisplayed: Boolean = false
        private set

    /**
     * Saved state for Compose rememberSaveable.
     */
    @Transient
    override var composeSavedState: Map<String, List<Any?>>? = null

    /**
     * Callbacks to invoke when this node is destroyed.
     */
    @Transient
    private val onDestroyCallbacks = mutableListOf<() -> Unit>()

    // --- Lifecycle Transitions ---

    override fun attachToNavigator() {
        // Idempotent - safe to call multiple times
        isAttachedToNavigator = true
    }

    override fun attachToUI() {
        // Auto-attach to navigator if not already attached
        // This handles cases where nodes are created and immediately rendered
        if (!isAttachedToNavigator) {
            attachToNavigator()
        }
        isDisplayed = true
    }

    override fun detachFromUI() {
        isDisplayed = false
        if (!isAttachedToNavigator) {
            close()
        }
    }

    override fun detachFromNavigator() {
        isAttachedToNavigator = false
        if (!isDisplayed) {
            close()
        }
    }

    override fun addOnDestroyCallback(callback: () -> Unit) {
        onDestroyCallbacks.add(callback)
    }

    override fun removeOnDestroyCallback(callback: () -> Unit) {
        onDestroyCallbacks.remove(callback)
    }

    /**
     * Cleanup when node is fully detached (not displayed and not in tree).
     */
    private fun close() {
        // Invoke all destroy callbacks
        onDestroyCallbacks.forEach { it.invoke() }
        onDestroyCallbacks.clear()
    }

    // --- Copy function (replacing data class copy) ---

    /**
     * Creates a copy of this ScreenNode with optionally modified properties.
     * Only navigation state is copied; lifecycle state is reset.
     */
    fun copy(
        key: String = this.key,
        parentKey: String? = this.parentKey,
        destination: NavDestination = this.destination
    ): ScreenNode = ScreenNode(
        key = key,
        parentKey = parentKey,
        destination = destination
    )

    // --- Equality based on persistent properties ---

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenNode) return false
        return key == other.key &&
            parentKey == other.parentKey &&
            destination == other.destination
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (parentKey?.hashCode() ?: 0)
        result = 31 * result + destination.hashCode()
        return result
    }

    override fun toString(): String =
        "ScreenNode(key='$key', parentKey=$parentKey, destination=$destination)"
}
