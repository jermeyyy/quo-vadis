package com.jermey.quo.vadis.core.navigation.node

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// =============================================================================
// Type-Checking Extension Functions with Kotlin Contracts
// =============================================================================

/**
 * Returns `true` if this node is a [ScreenNode].
 *
 * After this check returns `true`, the compiler smart-casts this node to [ScreenNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.isScreen(): Boolean {
    contract { returns(true) implies (this@isScreen is ScreenNode) }
    return this is ScreenNode
}

/**
 * Returns `true` if this node is a [StackNode].
 *
 * After this check returns `true`, the compiler smart-casts this node to [StackNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.isStack(): Boolean {
    contract { returns(true) implies (this@isStack is StackNode) }
    return this is StackNode
}

/**
 * Returns `true` if this node is a [TabNode].
 *
 * After this check returns `true`, the compiler smart-casts this node to [TabNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.isTab(): Boolean {
    contract { returns(true) implies (this@isTab is TabNode) }
    return this is TabNode
}

/**
 * Returns `true` if this node is a [PaneNode].
 *
 * After this check returns `true`, the compiler smart-casts this node to [PaneNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.isPane(): Boolean {
    contract { returns(true) implies (this@isPane is PaneNode) }
    return this is PaneNode
}

// =============================================================================
// Type-Requiring Extension Functions with Kotlin Contracts
// =============================================================================

/**
 * Casts this node to [ScreenNode] or throws [IllegalStateException].
 *
 * After this call returns, the compiler smart-casts this node to [ScreenNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.requireScreen(): ScreenNode {
    contract { returns() implies (this@requireScreen is ScreenNode) }
    return this as? ScreenNode
        ?: error("Expected ScreenNode but was ${this::class.simpleName} (key=$key)")
}

/**
 * Casts this node to [StackNode] or throws [IllegalStateException].
 *
 * After this call returns, the compiler smart-casts this node to [StackNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.requireStack(): StackNode {
    contract { returns() implies (this@requireStack is StackNode) }
    return this as? StackNode
        ?: error("Expected StackNode but was ${this::class.simpleName} (key=$key)")
}

/**
 * Casts this node to [TabNode] or throws [IllegalStateException].
 *
 * After this call returns, the compiler smart-casts this node to [TabNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.requireTab(): TabNode {
    contract { returns() implies (this@requireTab is TabNode) }
    return this as? TabNode
        ?: error("Expected TabNode but was ${this::class.simpleName} (key=$key)")
}

/**
 * Casts this node to [PaneNode] or throws [IllegalStateException].
 *
 * After this call returns, the compiler smart-casts this node to [PaneNode].
 */
@OptIn(ExperimentalContracts::class)
fun NavNode.requirePane(): PaneNode {
    contract { returns() implies (this@requirePane is PaneNode) }
    return this as? PaneNode
        ?: error("Expected PaneNode but was ${this::class.simpleName} (key=$key)")
}
