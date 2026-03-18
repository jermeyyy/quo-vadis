package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.ir.declarations.IrClass

data class SynthesizedDeclarations(
    val navigationConfigClass: IrClass,
    val deepLinkHandlerClass: IrClass,
    val screenRegistryClass: IrClass? = null,
    val aggregatedConfigClass: IrClass? = null,
)
