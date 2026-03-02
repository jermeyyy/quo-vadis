package com.jermey.quo.vadis.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class QuoVadisCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    override val pluginId: String = QuoVadisCommandLineProcessor.PLUGIN_ID

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modulePrefix = configuration.get(QuoVadisConfigurationKeys.MODULE_PREFIX) ?: return

        // Phase 2: FIR extensions will be registered here
        // Phase 3: IR extensions will be registered here
    }
}
