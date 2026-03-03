package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext

/**
 * Generates the `baseConfig` lazy property that calls `navigationConfig { ... }` DSL.
 *
 * The baseConfig is a private backing field added to the NavigationConfig IR class. Properties
 * like `scopeRegistry` and `transitionRegistry` delegate to it. The DSL sub-generators
 * ([StackDslIrGenerator], [TabsDslIrGenerator], [PanesDslIrGenerator], [ScopeDslIrGenerator],
 * [TransitionDslIrGenerator]) fill the lambda body.
 *
 * This skeleton is a compile-safe placeholder. Full implementation will be added when
 * the DSL generators produce real IR calls.
 */
class BaseConfigIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {
    // TODO: Generate the `private val baseConfig: NavigationConfig by lazy { navigationConfig { ... } }` property.
    //
    // Steps to implement in a follow-up task:
    //   1. Create an IrField for the lazy delegate backing field
    //   2. Build an irCall to `kotlin.lazy {}` with a lambda body
    //   3. Inside the lambda, call `navigationConfig { ... }` DSL function
    //   4. Populate the DSL lambda body using DSL sub-generators:
    //      - StackDslIrGenerator  (stack declarations)
    //      - TabsDslIrGenerator   (tabs declarations)
    //      - PanesDslIrGenerator  (pane declarations)
    //      - ScopeDslIrGenerator  (scope registrations)
    //      - TransitionDslIrGenerator (transition registrations)
    //   5. Wire the delegated properties (scopeRegistry, transitionRegistry, buildNavNode)
    //      to read from baseConfig
}
