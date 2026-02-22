package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * Runtime lifecycle state is managed by [LifecycleDelegate] and regenerated on restoration.
 *
 * @property key Unique identifier for this screen instance
 * @property parentKey Key of the containing StackNode or PaneNode
 * @property destination The destination data (route, arguments, transitions)
 */
@Serializable
@SerialName("screen")
data class ScreenNode(
    override val key: NodeKey,
    override val parentKey: NodeKey?,
    val destination: NavDestination
) : NavNode, LifecycleAwareNode by LifecycleDelegate()
