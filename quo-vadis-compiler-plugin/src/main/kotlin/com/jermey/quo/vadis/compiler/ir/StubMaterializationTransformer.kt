package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.QuoVadisGeneratedKey
import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

class StubMaterializationTransformer(
    private val pluginContext: IrPluginContext,
    private val modulePrefix: String,
    private val metadata: NavigationMetadata,
) : IrElementTransformerVoid() {

    private var configClass: IrClass? = null
    private var deepLinkHandlerClass: IrClass? = null

    private val expectedConfigName = Name.identifier("${modulePrefix}NavigationConfig")
    private val expectedDeepLinkName = Name.identifier("${modulePrefix}DeepLinkHandler")

    val synthesizedDeclarations: SynthesizedDeclarations?
        get() {
            val config = configClass ?: return null
            val deepLink = deepLinkHandlerClass ?: return null
            return SynthesizedDeclarations(config, deepLink)
        }

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.origin is IrDeclarationOrigin.GeneratedByPlugin &&
            (declaration.origin as IrDeclarationOrigin.GeneratedByPlugin).pluginKey == QuoVadisGeneratedKey
        ) {
            when (declaration.name) {
                expectedConfigName -> configClass = declaration
                expectedDeepLinkName -> deepLinkHandlerClass = declaration
            }
        }
        return super.visitClass(declaration)
    }
}
