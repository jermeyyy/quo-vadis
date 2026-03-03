@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.ir.SynthesizedDeclarations
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

class NavigationConfigIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
    private val declarations: SynthesizedDeclarations,
) {
    fun generate(irClass: IrClass) {
        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrProperty -> generatePropertyBody(irClass, declaration)
                is IrSimpleFunction -> generateFunctionBody(irClass, declaration)
                else -> { /* constructors, etc - left as-is */ }
            }
        }
    }

    private fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        when (property.name.asString()) {
            "screenRegistry" -> generateScreenRegistryProperty(irClass, property)
            "scopeRegistry" -> generateDelegatedRegistryProperty(property, "scopeRegistry")
            "transitionRegistry" -> generateDelegatedRegistryProperty(property, "transitionRegistry")
            "containerRegistry" -> generateContainerRegistryProperty(irClass, property)
            "deepLinkRegistry" -> generateDeepLinkRegistryProperty(property)
            "paneRoleRegistry" -> generatePaneRoleRegistryProperty(irClass, property)
            "roots" -> generateRootsProperty(property)
        }
    }

    private fun generateFunctionBody(irClass: IrClass, function: IrSimpleFunction) {
        when (function.name.asString()) {
            "buildNavNode" -> generateBuildNavNodeBody(function)
            "plus" -> generatePlusBody(function)
        }
    }

    // ---- Property generators (stubs — full implementations in Tasks 3C/3D/3F) ----

    private fun generateScreenRegistryProperty(irClass: IrClass, property: IrProperty) {
        ScreenRegistryIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            screens = metadata.screens,
        ).generatePropertyBody(irClass, property)
    }

    private fun generateContainerRegistryProperty(irClass: IrClass, property: IrProperty) {
        ContainerRegistryIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            tabsContainers = metadata.tabsContainers,
            paneContainers = metadata.paneContainers,
        ).generatePropertyBody(irClass, property)
    }

    private fun generatePaneRoleRegistryProperty(irClass: IrClass, property: IrProperty) {
        PaneRoleRegistryIrGenerator(
            pluginContext = pluginContext,
            symbolResolver = symbolResolver,
            metadata = metadata,
        ).generatePropertyBody(irClass, property)
    }

    private fun generateDeepLinkRegistryProperty(property: IrProperty) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        getter.body = builder.irBlockBody {
            +irReturn(irGetObject(declarations.deepLinkHandlerClass.symbol))
        }
    }

    private fun generateDelegatedRegistryProperty(property: IrProperty, registryName: String) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val registryClassSymbol = when (registryName) {
            "scopeRegistry" -> symbolResolver.scopeRegistryClass
            "transitionRegistry" -> symbolResolver.transitionRegistryClass
            else -> return
        }
        val companion = registryClassSymbol.owner.companionObject()
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
                    +irReturn(irGetObject(companion.symbol))
                }
            } else {
                // Fallback: shouldn't happen in practice
                +irReturn(irGetObject(registryClassSymbol))
            }
        }
    }

    private fun generateRootsProperty(property: IrProperty) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val rootClassIds = collectRootClassIds()
        val kClassOutNavDest = kClassOutNavDestinationType()

        getter.body = builder.irBlockBody {
            if (rootClassIds.isEmpty()) {
                // emptySet<KClass<out NavDestination>>()
                val emptySetOf = symbolResolver.setOfFunctions.first {
                    it.owner.parameters.isEmpty()
                }
                +irReturn(
                    irCall(emptySetOf, emptySetOf.owner.returnType, listOf(kClassOutNavDest)),
                )
            } else {
                val kClassArgs = rootClassIds.map { classId ->
                    val classSymbol = symbolResolver.resolveClass(classId)
                    IrClassReferenceImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = kClassOutType(classSymbol.defaultType),
                        symbol = classSymbol,
                        classType = classSymbol.defaultType,
                    )
                }
                // setOf(vararg elements: T)
                val setOfVararg = symbolResolver.setOfFunctions.first {
                    it.owner.parameters.size == 1 &&
                        it.owner.parameters[0].varargElementType != null
                }
                val call = irCall(setOfVararg, setOfVararg.owner.returnType, listOf(kClassOutNavDest))
                call.arguments[setOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, kClassArgs)
                +irReturn(call)
            }
        }
    }

    // ---- Function generators ----

    private fun generateBuildNavNodeBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        function.body = builder.irBlockBody {
            +irReturn(irNull())
        }
    }

    private fun generatePlusBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        val compositeClass = symbolResolver.compositeNavigationConfigClass
        val constructorDecl = compositeClass.owner.declarations
            .filterIsInstance<IrConstructor>()
            .firstOrNull() ?: run {
            // Fallback: return the other config parameter
            function.body = builder.irBlockBody {
                +irReturn(irGet(function.parameters.last()))
            }
            return
        }
        function.body = builder.irBlockBody {
            val constructorCall = irCallConstructor(constructorDecl.symbol, emptyList())
            // parameters[0] = dispatch receiver (this), last = other: NavigationConfig
            constructorCall.arguments[constructorDecl.parameters[0]] = irGet(function.parameters[0])
            constructorCall.arguments[constructorDecl.parameters[1]] = irGet(function.parameters.last())
            +irReturn(constructorCall)
        }
    }

    // ---- Helpers ----

    private fun collectRootClassIds(): Set<ClassId> {
        val rootClassIds = mutableSetOf<ClassId>()
        for (tab in metadata.tabs) {
            rootClassIds.add(tab.classId)
        }
        for (pane in metadata.panes) {
            rootClassIds.add(pane.classId)
        }
        val tabItemClassIds = metadata.tabs.flatMap { it.items }.map { it.classId }.toSet()
        for (stack in metadata.stacks) {
            if (stack.sealedClassId !in tabItemClassIds) {
                rootClassIds.add(stack.sealedClassId)
            }
        }
        return rootClassIds
    }

    private fun kClassOutNavDestinationType(): IrType =
        symbolResolver.kClassClass.createType(
            hasQuestionMark = false,
            arguments = listOf(
                makeTypeProjection(symbolResolver.navDestinationClass.defaultType, Variance.OUT_VARIANCE),
            ),
        )

    private fun kClassOutType(classType: IrType): IrType =
        symbolResolver.kClassClass.createType(
            hasQuestionMark = false,
            arguments = listOf(
                makeTypeProjection(classType, Variance.OUT_VARIANCE),
            ),
        )
}
