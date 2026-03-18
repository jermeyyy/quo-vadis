package com.jermey.quo.vadis.compiler

import com.google.auto.service.AutoService
import com.jermey.quo.vadis.compiler.fir.QuoVadisFirExtensionRegistrar
import com.jermey.quo.vadis.compiler.ir.QuoVadisIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class QuoVadisCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    override val pluginId: String = QuoVadisCommandLineProcessor.PLUGIN_ID

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modulePrefix = configuration.get(QuoVadisConfigurationKeys.MODULE_PREFIX) ?: return

        FirExtensionRegistrarAdapter.registerExtension(QuoVadisFirExtensionRegistrar(modulePrefix))

        IrGenerationExtension.registerExtension(QuoVadisIrGenerationExtension(configuration, modulePrefix))
    }
}
