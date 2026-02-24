package com.jermey.quo.vadis.core.navigation.node

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Type-safe wrapper for navigation node keys.
 *
 * Prevents accidental mixing of node keys with other string values
 * (routes, scope keys, wrapper keys) while having zero runtime overhead
 * thanks to Kotlin's inline value class.
 *
 * @property value The underlying string key
 */
@JvmInline
@Serializable
value class NodeKey(val value: String) {
    override fun toString(): String = value
}
