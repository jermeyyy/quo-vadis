package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.common.PluginMetadataStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class QuoVadisIrGenerationExtension(
    private val configuration: CompilerConfiguration,
    private val modulePrefix: String,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val metadata = PluginMetadataStore.retrieve(configuration) ?: NavigationMetadata(modulePrefix)

        val symbolResolver = SymbolResolver(pluginContext)

        // Pass 1: Locate and validate FIR-generated synthetic declarations
        val synthesizedDeclarations = StubMaterializationTransformer(
            pluginContext = pluginContext,
            modulePrefix = modulePrefix,
            metadata = metadata,
        ).also { moduleFragment.transform(it, null) }
            .synthesizedDeclarations

        // If no synthetic declarations were found, nothing to do
        if (synthesizedDeclarations == null) return

        // Pass 2: Inject bodies into synthetic declarations
        BodySynthesisTransformer(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            metadata = metadata,
            declarations = synthesizedDeclarations,
        ).also { moduleFragment.transform(it, null) }
    }
}
