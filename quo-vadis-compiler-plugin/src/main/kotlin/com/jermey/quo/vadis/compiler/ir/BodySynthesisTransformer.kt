package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.QuoVadisGeneratedKey
import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.generators.DeepLinkHandlerIrGenerator
import com.jermey.quo.vadis.compiler.ir.generators.NavigationConfigIrGenerator
import com.jermey.quo.vadis.compiler.ir.generators.ScreenRegistryIrGenerator
import com.jermey.quo.vadis.compiler.ir.generators.AggregatedConfigIrGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class BodySynthesisTransformer(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
    private val declarations: SynthesizedDeclarations,
    private val configuration: CompilerConfiguration,
    private val moduleFragment: IrModuleFragment,
) : IrElementTransformerVoid() {

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.origin is IrDeclarationOrigin.GeneratedByPlugin &&
            (declaration.origin as IrDeclarationOrigin.GeneratedByPlugin).pluginKey == QuoVadisGeneratedKey
        ) {
            when (declaration.name) {
                declarations.navigationConfigClass.name -> synthesizeNavigationConfigBody(declaration)
                declarations.deepLinkHandlerClass.name -> synthesizeDeepLinkHandlerBody(declaration)
                declarations.screenRegistryClass?.name -> synthesizeScreenRegistryBody(declaration)
                declarations.aggregatedConfigClass?.name -> synthesizeAggregatedConfigBody(declaration)
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
            moduleFragment = moduleFragment,
        ).generate(irClass)
    }

    private fun synthesizeDeepLinkHandlerBody(irClass: IrClass) {
        DeepLinkHandlerIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            metadata = metadata,
        ).generate(irClass)
    }

    private fun synthesizeScreenRegistryBody(irClass: IrClass) {
        ScreenRegistryIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            screens = metadata.screens,
            moduleFragment = moduleFragment,
        ).generateClassBodies(irClass)
    }

    private fun synthesizeAggregatedConfigBody(irClass: IrClass) {
        AggregatedConfigIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            discovery = MultiModuleDiscovery(pluginContext, moduleFragment, configuration),
        ).generate(irClass)
    }
}
