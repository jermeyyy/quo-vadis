package com.jermey.quo.vadis.compiler.ir.generators.dsl

import com.jermey.quo.vadis.compiler.common.StackMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

/**
 * Generates `stack<D>(scopeKey, startDestination) { destination<SubClass>() ... }` DSL calls
 * and appends them to the given builder lambda body.
 */
class StackDslIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val stacks: List<StackMetadata>,
) {
    /**
     * Appends stack DSL IR calls to [builderLambdaBody].
     *
     * For each stack in [stacks]:
     *   1. Creates an `IrCall` to `NavigationConfigBuilder.stack<SealedClass>()`
     *   2. Sets the scopeKey string argument from [StackMetadata.name]
     *   3. Sets the startDestination `KClass` reference from [StackMetadata.startDestination]
     *   4. Creates a builder lambda for `StackBuilder`
     *   5. Registers each destination within the lambda via `destination<SubClass>()`
     */
    fun generate(builderLambdaBody: IrBlockBody) {
        // TODO: Generate stack DSL calls from metadata
    }
}
