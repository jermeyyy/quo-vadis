package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.QuoVadisGeneratedKey
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name

class StubMaterializationTransformer(
    private val modulePrefix: String,
) : IrElementTransformerVoid() {

    private var configClass: IrClass? = null
    private var deepLinkHandlerClass: IrClass? = null
    private var screenRegistryClass: IrClass? = null
    private var aggregatedConfigClass: IrClass? = null

    private val expectedConfigName = Name.identifier("${modulePrefix}NavigationConfig")
    private val expectedDeepLinkName = Name.identifier("${modulePrefix}DeepLinkHandler")
    private val expectedScreenRegistryName = Name.identifier("${modulePrefix}ScreenRegistryImpl")

    val synthesizedDeclarations: SynthesizedDeclarations?
        get() {
            val config = configClass ?: return null
            val deepLink = deepLinkHandlerClass ?: return null
            return SynthesizedDeclarations(config, deepLink, screenRegistryClass, aggregatedConfigClass)
        }

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.origin is IrDeclarationOrigin.GeneratedByPlugin &&
            (declaration.origin as IrDeclarationOrigin.GeneratedByPlugin).pluginKey == QuoVadisGeneratedKey
        ) {
            when (declaration.name) {
                expectedConfigName -> configClass = declaration
                expectedDeepLinkName -> deepLinkHandlerClass = declaration
                expectedScreenRegistryName -> screenRegistryClass = declaration
                else -> {
                    if (declaration.name.asString().endsWith(AGGREGATED_CONFIG_SUFFIX)) {
                        aggregatedConfigClass = declaration
                    }
                }
            }
        }
        return super.visitClass(declaration)
    }

    private companion object {
        const val AGGREGATED_CONFIG_SUFFIX = "__AggregatedConfig"
    }
}
