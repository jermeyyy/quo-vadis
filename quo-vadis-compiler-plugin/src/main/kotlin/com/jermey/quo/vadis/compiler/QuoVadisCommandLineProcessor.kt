package com.jermey.quo.vadis.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class QuoVadisCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val PLUGIN_ID = "com.jermey.quo-vadis"

        val OPTION_MODULE_PREFIX = CliOption(
            optionName = "modulePrefix",
            valueDescription = "String",
            description = "Prefix for generated navigation config class names",
            required = false
        )
    }

    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(OPTION_MODULE_PREFIX)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OPTION_MODULE_PREFIX -> configuration.put(QuoVadisConfigurationKeys.MODULE_PREFIX, value)
        }
    }
}
