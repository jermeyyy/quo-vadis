@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.companionObject

/**
 * Generates the `paneRoleRegistry` property body for the synthesized NavigationConfig class.
 *
 * Currently returns `PaneRoleRegistry.Empty` as a placeholder.
 * A future iteration will generate an anonymous `object : PaneRoleRegistry` with
 * pane role dispatch based on metadata from `@Pane`/`@PaneItem` annotations.
 */
class PaneRoleRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {
    /**
     * Generates the paneRoleRegistry property body.
     * Currently returns PaneRoleRegistry.Empty as placeholder.
     */
    fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)

        val companion = symbolResolver.paneRoleRegistryClass.owner.companionObject()
        getter.body = builder.irBlockBody {
            if (companion != null) {
                val emptyProp = companion.declarations
                    .filterIsInstance<IrProperty>()
                    .firstOrNull { it.name.asString() == "Empty" }
                val emptyGetter = emptyProp?.getter
                if (emptyGetter != null) {
                    +irReturn(
                        irCall(emptyGetter).apply {
                            dispatchReceiver = irGetObject(companion.symbol)
                        },
                    )
                } else {
                    // Fallback: return companion object itself
                    +irReturn(irGetObject(companion.symbol))
                }
            } else {
                // Fallback: return PaneRoleRegistry object (should not happen for a proper PaneRoleRegistry)
                +irReturn(irGetObject(symbolResolver.paneRoleRegistryClass))
            }
        }
    }
}
