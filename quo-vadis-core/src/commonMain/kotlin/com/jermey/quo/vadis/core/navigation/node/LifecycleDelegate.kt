package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode

/**
 * Default implementation of [LifecycleAwareNode] lifecycle management.
 *
 * This delegate handles all lifecycle state transitions and callback management,
 * eliminating duplicated lifecycle code across [ScreenNode], [TabNode], and [PaneNode].
 *
 * ## Usage
 *
 * Use Kotlin delegation to add lifecycle support to a node:
 * ```kotlin
 * class MyNode : NavNode, LifecycleAwareNode by LifecycleDelegate()
 * ```
 *
 * ## Serialization
 *
 * All fields in this delegate are transient (runtime-only state).
 * The delegate is recreated fresh after deserialization.
 */
class LifecycleDelegate : LifecycleAwareNode {

    override var isAttachedToNavigator: Boolean = false
        private set

    override var isDisplayed: Boolean = false
        private set

    override var composeSavedState: Map<String, List<Any?>>? = null

    private val onDestroyCallbacks = mutableListOf<() -> Unit>()

    override fun attachToNavigator() {
        isAttachedToNavigator = true
    }

    override fun attachToUI() {
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

    private fun close() {
        onDestroyCallbacks.forEach { it.invoke() }
        onDestroyCallbacks.clear()
    }
}
