package com.jermey.quo.vadis.compiler.ir.generators.dsl

import com.jermey.quo.vadis.compiler.common.TabsMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

/**
 * Generates `tabs<T>(name, initialTab) { tab<StackClass>() ... }` DSL calls
 * and appends them to the given builder lambda body.
 */
class TabsDslIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val tabs: List<TabsMetadata>,
) {
    /**
     * Appends tabs DSL IR calls to [builderLambdaBody].
     *
     * For each tabs definition in [tabs]:
     *   1. Creates an `IrCall` to `NavigationConfigBuilder.tabs<TabsClass>()`
     *   2. Sets the name string argument from [TabsMetadata.name]
     *   3. Sets the initialTab `KClass` reference from [TabsMetadata.initialTab] (if present)
     *   4. Creates a builder lambda for `TabsBuilder`
     *   5. Registers each tab item within the lambda via `tab<StackClass>()`
     */
    fun generate(builderLambdaBody: IrBlockBody) {
        // TODO: Generate tabs DSL calls from metadata
    }
}
