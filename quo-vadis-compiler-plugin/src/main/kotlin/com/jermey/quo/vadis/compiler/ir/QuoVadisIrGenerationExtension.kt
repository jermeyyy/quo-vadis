package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class QuoVadisIrGenerationExtension(
    private val configuration: CompilerConfiguration,
    private val modulePrefix: String,
) : IrGenerationExtension {

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val collector = IrMetadataCollector(modulePrefix, pluginContext)
        val metadata = collector.collect(moduleFragment)

        val symbolResolver = SymbolResolver(pluginContext)

        // Pass 1: Locate and validate FIR-generated synthetic declarations
        val synthesizedDeclarations = StubMaterializationTransformer(
            modulePrefix = modulePrefix,
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
            configuration = configuration,
            moduleFragment = moduleFragment,
        ).also { moduleFragment.transform(it, null) }

        // Pass 3: Replace navigationConfig<T>() call-sites with config references.
        // Prefer the aggregated config (multi-module) but fall back to the
        // single-module navigationConfigClass so that single-module projects
        // also get call-site rewriting.
        val configForCallSites = synthesizedDeclarations.aggregatedConfigClass
            ?: synthesizedDeclarations.navigationConfigClass
        NavigationConfigCallTransformer(
            aggregatedConfigClass = configForCallSites,
        ).also { moduleFragment.transform(it, null) }
    }
}
