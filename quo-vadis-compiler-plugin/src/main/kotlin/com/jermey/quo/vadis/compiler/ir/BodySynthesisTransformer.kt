package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.QuoVadisGeneratedKey
import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.generators.DeepLinkHandlerIrGenerator
import com.jermey.quo.vadis.compiler.ir.generators.NavigationConfigIrGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class BodySynthesisTransformer(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
    private val declarations: SynthesizedDeclarations,
) : IrElementTransformerVoid() {

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.origin is IrDeclarationOrigin.GeneratedByPlugin &&
            (declaration.origin as IrDeclarationOrigin.GeneratedByPlugin).pluginKey == QuoVadisGeneratedKey
        ) {
            when (declaration) {
                declarations.navigationConfigClass -> synthesizeNavigationConfigBody(declaration)
                declarations.deepLinkHandlerClass -> synthesizeDeepLinkHandlerBody(declaration)
            }
        }
        return super.visitClass(declaration)
    }

    private fun synthesizeNavigationConfigBody(irClass: IrClass) {
        NavigationConfigIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            metadata = metadata,
            declarations = declarations,
        ).generate(irClass)
    }

    private fun synthesizeDeepLinkHandlerBody(irClass: IrClass) {
        DeepLinkHandlerIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            metadata = metadata,
        ).generate(irClass)
    }
}
