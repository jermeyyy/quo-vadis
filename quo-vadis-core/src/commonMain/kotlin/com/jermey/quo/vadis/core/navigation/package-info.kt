package com.jermey.quo.vadis.core.navigation

/**
 * Navigation Library for Kotlin Multiplatform & Compose Multiplatform
 *
 * Main package providing comprehensive navigation capabilities:
 *
 * ## Core Components (navigation.core)
 * - Destination: Navigation targets
 * - Navigator: Central navigation controller
 * - BackStack: Direct backstack manipulation
 * - NavigationGraph: Modular navigation graphs
 * - NavigationTransition: Animation support
 * - DeepLink: Deep link handling
 *
 * ## Compose Integration (navigation.compose)
 * - NavHost: Main navigation host composable
 * - GraphNavHost: Graph-specific navigation host
 * - rememberNavigator: Navigator factory
 *
 * ## DI Integration (navigation.integration)
 * - NavigationFactory: Factory for DI containers
 * - Koin integration helpers
 *
 * ## Utilities (navigation.utils)
 * - Extension functions for Navigator and BackStack
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
 * @see com.jermey.navplayground.navigation.example for usage examples
 */

