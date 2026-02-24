package com.jermey.quo.vadis.core.navigation.node

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for navigation scope keys.
 *
 * Prevents accidental mixing of scope keys with other string values
 * (node keys, routes, wrapper keys) while having zero runtime overhead
 * thanks to Kotlin's inline value class.
 *
 * Scope keys identify navigation scopes used by containers (TabNode,
 * PaneNode, StackNode) to determine whether destinations belong to
 * their scope or should navigate outside.
 *
 * @property value The underlying string key
 */
@JvmInline
@Serializable
value class ScopeKey(val value: String) {
    override fun toString(): String = value
}
