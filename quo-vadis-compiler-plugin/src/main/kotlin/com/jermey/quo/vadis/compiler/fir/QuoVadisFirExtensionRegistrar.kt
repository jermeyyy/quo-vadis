package com.jermey.quo.vadis.compiler.fir

import com.jermey.quo.vadis.compiler.fir.checkers.QuoVadisAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class QuoVadisFirExtensionRegistrar(private val modulePrefix: String) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> QuoVadisDeclarationGenerationExtension(session, modulePrefix) }
        +{ session: FirSession -> QuoVadisAdditionalCheckersExtension(session) }
    }
}
