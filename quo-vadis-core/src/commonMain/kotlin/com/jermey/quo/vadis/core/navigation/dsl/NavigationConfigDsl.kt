package com.jermey.quo.vadis.core.navigation.dsl

/**
 * DSL marker annotation for navigation configuration builders.
 *
 * This annotation prevents implicit access to outer builder scopes when using
 * nested DSL constructs, ensuring type-safe and predictable builder behavior.
 *
 * ## Purpose
 *
 * Without this marker, nested lambdas could accidentally access methods from
 * outer scopes, leading to confusing bugs. For example:
 *
 * ```kotlin
 * navigationConfig {
 *     tabs<MyTabs> {
 *         // Without @DslMarker, this could accidentally call the outer screen() method
 *         tab(HomeTab) {
 *             screen<SomeScreen>() // This should only be available in StackBuilder
 *         }
 *     }
 * }
 * ```
 *
 * With `@NavigationConfigDsl`, attempting to call `screen()` from within a
 * `tab {}` block (when not in a `StackBuilder` context) produces a compile error.
 *
 * ## Usage
 *
 * Applied automatically to all DSL builder classes in the navigation configuration:
 * - [NavigationConfigBuilder]
 * - [StackBuilder]
 * - [TabsBuilder]
 * - [PanesBuilder]
 * - [PaneContentBuilder]
 *
 * @see NavigationConfigBuilder
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NavigationConfigDsl
