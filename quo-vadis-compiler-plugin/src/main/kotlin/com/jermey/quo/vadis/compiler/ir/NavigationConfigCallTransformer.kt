@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Replaces `navigationConfig<T>()` call-sites with direct references to
 * the generated `{Prefix}__AggregatedConfig` object.
 *
 * This runs after body synthesis so that the aggregated config object
 * already has its bodies populated.
 */
class NavigationConfigCallTransformer(
    @Suppress("unused") private val pluginContext: IrPluginContext,
    private val aggregatedConfigClass: IrClass,
) : IrElementTransformerVoid() {

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        val fqn = callee.fqNameWhenAvailable?.asString()
        if (fqn != NAVIGATION_CONFIG_FQN) {
            return super.visitCall(expression)
        }

        // Replace with IrGetObjectValue referencing the aggregated config
        return IrGetObjectValueImpl(
            startOffset = expression.startOffset,
            endOffset = expression.endOffset,
            type = aggregatedConfigClass.symbol.defaultType,
            symbol = aggregatedConfigClass.symbol,
        )
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    private companion object {
        const val NAVIGATION_CONFIG_FQN =
            "com.jermey.quo.vadis.core.navigation.config.navigationConfig"
    }
}
