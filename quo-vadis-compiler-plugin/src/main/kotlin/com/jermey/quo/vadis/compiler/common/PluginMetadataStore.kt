package com.jermey.quo.vadis.compiler.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PluginMetadataStore {
    private val METADATA_KEY = CompilerConfigurationKey<NavigationMetadata>("quo-vadis-navigation-metadata")

    fun store(configuration: CompilerConfiguration, metadata: NavigationMetadata) {
        configuration.put(METADATA_KEY, metadata)
    }

    fun retrieve(configuration: CompilerConfiguration): NavigationMetadata? {
        return configuration.get(METADATA_KEY)
    }
}
