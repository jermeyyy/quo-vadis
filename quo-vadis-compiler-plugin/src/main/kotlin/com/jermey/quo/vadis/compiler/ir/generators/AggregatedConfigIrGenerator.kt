@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.ir.MultiModuleDiscovery
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/**
 * Generates IR bodies for the `{Prefix}__AggregatedConfig` object class.
 *
 * The aggregated config composes all discovered `@GeneratedConfig`-annotated
 * implementations from the classpath and current module using the `plus` operator,
 * then delegates every [NavigationConfig] member to the composite.
 */
class AggregatedConfigIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val discovery: MultiModuleDiscovery,
) {
    fun generate(irClass: IrClass) {
        val configSymbols = discovery.discoverGeneratedConfigs()
        val delegateField = createDelegateField(irClass, configSymbols)

        for (declaration in irClass.declarations.toList()) {
            when (declaration) {
                is IrProperty -> generatePropertyBody(declaration, delegateField)
                is IrSimpleFunction -> generateFunctionBody(declaration, delegateField)
                else -> { /* constructors, fields — left as-is */ }
            }
        }
    }

    // region Delegate field (NavigationConfig)

    private fun createDelegateField(irClass: IrClass, configSymbols: List<IrClassSymbol>): IrField {
        val configType = symbolResolver.navigationConfigClass.defaultType

        val field = pluginContext.irFactory.createField(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("_delegate"),
            visibility = DescriptorVisibilities.PRIVATE,
            symbol = IrFieldSymbolImpl(),
            type = configType,
            isFinal = true,
            isExternal = false,
            isStatic = false,
        )
        field.parent = irClass
        irClass.declarations.add(field)

        field.initializer = pluginContext.irFactory.createExpressionBody(
            buildDirectInitializer(field, configSymbols),
        )

        return field
    }

    private fun buildDirectInitializer(
        field: IrField,
        configSymbols: List<IrClassSymbol>,
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, field.symbol)

        return if (configSymbols.isEmpty()) {
            val emptyConfig = symbolResolver.resolveClass(
                "com.jermey.quo.vadis.core.navigation.internal.config",
                "EmptyNavigationConfig",
            )
            builder.irGetObject(emptyConfig)
        } else {
            val plusFun = symbolResolver.navigationConfigClass.owner.declarations
                .filterIsInstance<IrSimpleFunction>()
                .firstOrNull { it.name.asString() == "plus" }
                ?: error("Expected function 'plus' in NavigationConfig. Ensure quo-vadis-core version is compatible with compiler plugin.")

            var acc: IrExpression = builder.irGetObject(
                configSymbols.firstOrNull()
                    ?: error("Expected at least one NavigationConfig symbol for aggregation."),
            )
            for (i in 1 until configSymbols.size) {
                acc = builder.irCall(plusFun).apply {
                    arguments[plusFun.parameters[0]] = acc
                    arguments[plusFun.parameters[1]] = builder.irGetObject(configSymbols[i])
                }
            }
            acc
        }
    }

    // endregion

    // region Property body generation

    private fun generatePropertyBody(property: IrProperty, delegateField: IrField) {
        when (property.name.asString()) {
            "screenRegistry", "scopeRegistry", "transitionRegistry",
            "containerRegistry", "deepLinkRegistry", "paneRoleRegistry",
            -> generateDelegatedProperty(property, property.name.asString(), delegateField)
            "roots" -> generateRootsProperty(property)
        }
    }

    /**
     * Generates the `roots` property body returning an empty set.
     * `roots` is not part of the [NavigationConfig] interface — it's a utility property
     * that each per-module config computes from its own metadata. The aggregated config
     * doesn't need roots since it delegates all real work to the composite.
     */
    private fun generateRootsProperty(property: IrProperty) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val kClassOutNavDest = symbolResolver.kClassClass.createType(
            hasQuestionMark = false,
            arguments = listOf(
                makeTypeProjection(symbolResolver.navDestinationClass.defaultType, Variance.OUT_VARIANCE),
            ),
        )
        val emptySetOf = symbolResolver.setOfFunctions.firstOrNull {
            it.owner.parameters.isEmpty()
        } ?: error("Expected 'setOf()' function with no parameters in kotlin.collections. Ensure quo-vadis-core version is compatible with compiler plugin.")
        getter.body = builder.irBlockBody {
            +irReturn(
                irCall(emptySetOf, emptySetOf.owner.returnType, listOf(kClassOutNavDest)),
            )
        }
    }

    private fun generateDelegatedProperty(
        property: IrProperty,
        propertyName: String,
        delegateField: IrField,
    ) {
        val getter = property.getter ?: return
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)

        val navConfigProp = symbolResolver.navigationConfigClass.owner.declarations
            .filterIsInstance<IrProperty>()
            .firstOrNull { it.name.asString() == propertyName }
            ?: error("Expected property '$propertyName' in NavigationConfig. Ensure quo-vadis-core version is compatible with compiler plugin.")
        val navConfigGetter = navConfigProp.getter ?: return

        getter.body = builder.irBlockBody {
            val fieldGet = IrGetFieldImpl(
                startOffset, endOffset,
                delegateField.symbol,
                delegateField.type,
                irGet(getter.parameters[0]),
            )
            +irReturn(
                irCall(navConfigGetter).apply {
                    arguments[navConfigGetter.parameters[0]] = fieldGet
                },
            )
        }
    }

    // endregion

    // region Function body generation

    private fun generateFunctionBody(function: IrSimpleFunction, delegateField: IrField) {
        when (function.name.asString()) {
            "buildNavNode" -> generateBuildNavNodeBody(function, delegateField)
            "plus" -> generatePlusBody(function)
        }
    }

    private fun generateBuildNavNodeBody(function: IrSimpleFunction, delegateField: IrField) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)

        val navConfigBuildNavNode = symbolResolver.navigationConfigClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "buildNavNode" }
            ?: error("Expected function 'buildNavNode' in NavigationConfig. Ensure quo-vadis-core version is compatible with compiler plugin.")

        function.body = builder.irBlockBody {
            val fieldGet = IrGetFieldImpl(
                startOffset, endOffset,
                delegateField.symbol,
                delegateField.type,
                irGet(function.parameters[0]),
            )
            val call = irCall(navConfigBuildNavNode)
            call.arguments[navConfigBuildNavNode.parameters[0]] = fieldGet
            call.arguments[navConfigBuildNavNode.parameters[1]] = irGet(function.parameters[1])
            call.arguments[navConfigBuildNavNode.parameters[2]] = irGet(function.parameters[2])
            call.arguments[navConfigBuildNavNode.parameters[3]] = irGet(function.parameters[3])
            +irReturn(call)
        }
    }

    private fun generatePlusBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        val compositeClass = symbolResolver.compositeNavigationConfigClass
        val constructorDecl = compositeClass.owner.declarations
            .filterIsInstance<IrConstructor>()
            .firstOrNull() ?: return
        function.body = builder.irBlockBody {
            val constructorCall = irCallConstructor(constructorDecl.symbol, emptyList())
            constructorCall.arguments[constructorDecl.parameters[0]] = irGet(function.parameters[0])
            constructorCall.arguments[constructorDecl.parameters[1]] = irGet(function.parameters.last())
            +irReturn(constructorCall)
        }
    }

    // endregion
}
