@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.functions

class DeepLinkHandlerIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {
    fun generate(irClass: IrClass) {
        for (function in irClass.functions) {
            generateFunctionBody(function)
        }
    }

    private fun generateFunctionBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        when (function.name.asString()) {
            "resolve" -> {
                function.body = builder.irBlockBody {
                    +irReturn(irNull())
                }
            }
            "register", "registerAction" -> {
                function.body = builder.irBlockBody { }
            }
            "handle" -> {
                function.body = builder.irBlockBody {
                    +irReturn(irBoolean(false))
                }
            }
            "createUri" -> {
                function.body = builder.irBlockBody {
                    +irReturn(irNull())
                }
            }
            "canHandle" -> {
                function.body = builder.irBlockBody {
                    +irReturn(irBoolean(false))
                }
            }
            "getRegisteredPatterns" -> {
                val emptyListFn = symbolResolver.resolveFunctions("kotlin.collections", "emptyList").first()
                function.body = builder.irBlockBody {
                    +irReturn(irCall(emptyListFn))
                }
            }
            "handleDeepLink" -> {
                function.body = builder.irBlockBody {
                    +irReturn(irNull())
                }
            }
        }
    }
}
