package com.jermey.quo.vadis.compiler.ir.generators.dsl

import com.jermey.quo.vadis.compiler.common.PaneMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

/**
 * Generates `panes<P>(name, backBehavior) { pane<StackClass>(role, adaptStrategy) ... }` DSL calls
 * and appends them to the given builder lambda body.
 */
class PanesDslIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val panes: List<PaneMetadata>,
) {
    /**
     * Appends panes DSL IR calls to [builderLambdaBody].
     *
     * For each pane definition in [panes]:
     *   1. Creates an `IrCall` to `NavigationConfigBuilder.panes<PaneClass>()`
     *   2. Sets the name string argument from [PaneMetadata.name]
     *   3. Sets the backBehavior enum argument from [PaneMetadata.backBehavior]
     *   4. Creates a builder lambda for `PanesBuilder`
     *   5. Registers each pane item within the lambda via `pane<StackClass>(role, adaptStrategy)`
     */
    fun generate(builderLambdaBody: IrBlockBody) {
        // TODO: Generate panes DSL calls from metadata
    }
}
