package com.jermey.quo.vadis.core.navigation

/**
 * Navigation Library for Kotlin Multiplatform & Compose Multiplatform
 *
 * Main package providing comprehensive navigation capabilities:
 *
 * ## Core Components (navigation.core)
 * - Destination: Navigation targets
 * - Navigator: Central navigation controller
 * - NavNode: Tree-based navigation state (StackNode, TabNode, PaneNode, ScreenNode)
 * - NavigationTransition: Animation support
 * - DeepLink: Deep link handling
 *
 * ## Compose Integration (navigation.compose)
 * - NavigationHost: Main navigation host composable
 * - rememberTreeNavigator: Navigator factory
 *
 * ## DI Integration (navigation.integration)
 * - NavigationFactory: Factory for DI containers
 * - Koin integration helpers
 *
 * ## Utilities (navigation.utils)
 * - Extension functions for Navigator and NavNode
 * - Navigation scopes and builders
 *
 * ## Testing (navigation.testing)
 * - FakeNavigator: Test double for navigation
 * - NavigationCall: Track navigation actions
 * - Test builders and DSL
 *
 * ## State Persistence (navigation.serialization)
 * - NavigationStateSerializer: Save/restore state
 * - Process death handling
 *
 * ## MVI Support (FlowMVI Module)
 * For MVI architecture integration, use the separate `quo-vadis-core-flow-mvi` module
 * which provides integration with the FlowMVI library.
 *
 * @see com.jermey.navplayground.demo for usage examples
 */

