package com.jermey.quo.vadis.compiler.ir.generators.dsl

import com.jermey.quo.vadis.compiler.common.TransitionMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

/**
 * Generates transition registration DSL calls within the `navigationConfig { ... }` lambda.
 *
 * Transitions associate destination classes with specific [NavTransition] implementations,
 * controlling the animation used when navigating to/from that destination.
 */
class TransitionDslIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val transitions: List<TransitionMetadata>,
) {
    /**
     * Appends transition registration IR calls to [builderLambdaBody].
     *
     * For each transition in [transitions]:
     *   1. Resolves the [TransitionMetadata.type] to a [NavTransition] instance
     *      (e.g., `NavTransition.SlideHorizontal`, `NavTransition.Fade`, or a custom class)
     *   2. Creates an `IrCall` to register the transition:
     *      `transition<DestinationClass>(navTransition)`
     */
    fun generate(builderLambdaBody: IrBlockBody) {
        // TODO: Generate transition registration DSL calls from metadata
    }
}
