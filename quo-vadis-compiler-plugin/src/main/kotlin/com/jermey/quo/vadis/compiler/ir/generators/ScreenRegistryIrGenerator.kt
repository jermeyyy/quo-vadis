@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.ScreenMetadata
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
 * Generates the `screenRegistry` property body for the synthesized NavigationConfig class.
 *
 * When no screens are registered, returns `ScreenRegistry.Empty`.
 * When screens exist, a full anonymous `object : ScreenRegistry` with `when`-based dispatch
 * will be generated in a follow-up iteration (requires Compose compiler IR interaction).
 * For now, both paths return `ScreenRegistry.Empty` as a compile-safe placeholder.
 */
class ScreenRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val screens: List<ScreenMetadata>,
) {
    /**
     * Generates the screenRegistry property body.
     * If no screens are registered, returns ScreenRegistry.Empty.
     * Otherwise, returns ScreenRegistry.Empty as a placeholder (TODO: full anonymous object).
     */
    fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        val getter = property.getter ?: return

        if (screens.isEmpty()) {
            generateEmptyScreenRegistry(getter)
            return
        }

        // TODO: Generate anonymous object : ScreenRegistry with when-based dispatch.
        //  Each branch matches a destination type and calls the corresponding @Screen function:
        //    is FeedDestination.Home -> HomeScreen(navigator)
        //    is FeedDestination.Detail -> DetailScreen(destination, navigator)
        //  Parameters are injected based on ScreenMetadata flags:
        //    hasDestinationParam, hasSharedTransitionScope, hasAnimatedVisibilityScope.
        //  This requires creating an anonymous IrClass implementing ScreenRegistry with a
        //  @Composable Screen() override, which needs Compose compiler IR cooperation.
        //  Using ScreenRegistry.Empty as placeholder until that is implemented.
        generateEmptyScreenRegistry(getter)
    }

    private fun generateEmptyScreenRegistry(getter: org.jetbrains.kotlin.ir.declarations.IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val screenRegistryOwner = symbolResolver.screenRegistryClass.owner
        val companion = screenRegistryOwner.companionObject()

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
                // Fallback: return ScreenRegistry object (should not happen for a proper ScreenRegistry)
                +irReturn(irGetObject(symbolResolver.screenRegistryClass))
            }
        }
    }
}
