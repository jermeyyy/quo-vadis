package com.jermey.quo.vadis.compiler.ir.generators.dsl

import com.jermey.quo.vadis.compiler.common.StackMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

/**
 * Generates scope registration DSL calls within the `navigationConfig { ... }` lambda.
 *
 * Scopes associate destination classes with their parent stack's scope key, enabling
 * scope-aware navigation (e.g., `navigator.navigate(destination)` automatically finds the
 * correct parent stack).
 */
class ScopeDslIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val stacks: List<StackMetadata>,
) {
    /**
     * Appends scope registration IR calls to [builderLambdaBody].
     *
     * For each stack in [stacks], registers a scope mapping from each
     * destination class to its parent stack's scope key:
     *   `scope<DestinationClass>(scopeKey)`
     */
    fun generate(builderLambdaBody: IrBlockBody) {
        // TODO: Generate scope registration DSL calls from metadata
    }
}
