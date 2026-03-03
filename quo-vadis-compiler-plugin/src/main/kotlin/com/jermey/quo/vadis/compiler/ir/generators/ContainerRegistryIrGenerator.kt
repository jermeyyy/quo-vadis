@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.PaneContainerMetadata
import com.jermey.quo.vadis.compiler.common.TabsContainerMetadata
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
 * Generates the `containerRegistry` property body for the synthesized NavigationConfig class.
 *
 * When no tabs/pane containers are registered, returns `ContainerRegistry.Empty`.
 * When containers exist, a full anonymous `object : ContainerRegistry` with when-based dispatch
 * will be generated in a follow-up iteration.
 * For now, both paths return `ContainerRegistry.Empty` as a compile-safe placeholder.
 */
class ContainerRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val tabsContainers: List<TabsContainerMetadata>,
    private val paneContainers: List<PaneContainerMetadata>,
) {
    /**
     * Generates the containerRegistry property body.
     * Currently returns ContainerRegistry.Empty as a compile-safe placeholder.
     * Full anonymous object implementation with when-based dispatch will be done in a subsequent iteration.
     */
    fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        val getter = property.getter ?: return

        if (tabsContainers.isEmpty() && paneContainers.isEmpty()) {
            generateEmptyContainerRegistry(getter)
            return
        }

        // TODO: Generate anonymous object : ContainerRegistry with when-based dispatch.
        //  TabContainer/PaneContainer branches match destination types and call the
        //  corresponding @TabsContainer/@PaneContainer composable functions.
        //  tabBuilder/paneBuilder branches construct Tab/Pane instances from metadata.
        //  Using ContainerRegistry.Empty as placeholder until that is implemented.
        generateEmptyContainerRegistry(getter)
    }

    private fun generateEmptyContainerRegistry(getter: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val containerRegistryOwner = symbolResolver.containerRegistryClass.owner
        val companion = containerRegistryOwner.companionObject()

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
                // Fallback: return ContainerRegistry object (should not happen for a proper ContainerRegistry)
                +irReturn(irGetObject(symbolResolver.containerRegistryClass))
            }
        }
    }
}
