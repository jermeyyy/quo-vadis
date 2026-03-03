@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.common.TabItemType
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/**
 * Generates the `_baseConfig` backing field that builds a [NavigationConfig] via
 * [NavigationConfigBuilder] using the non-reified helper methods.
 *
 * The field is added to the generated NavigationConfig object class and initialized
 * eagerly during object construction. Properties like `scopeRegistry`, `containerRegistry`,
 * `transitionRegistry`, and `buildNavNode` delegate to this field.
 */
class BaseConfigIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {
    /**
     * Creates the `_baseConfig` field on [irClass] and populates its initializer
     * with builder calls derived from [metadata].
     *
     * @return The created [IrField] so callers can reference it for property delegation.
     */
    fun generate(irClass: IrClass): IrField {
        val configType = symbolResolver.navigationConfigClass.defaultType

        val field = pluginContext.irFactory.createField(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("_baseConfig"),
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
            buildInitializerExpression(field),
        )

        return field
    }

    private fun buildInitializerExpression(field: IrField): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, field.symbol)
        val configType = symbolResolver.navigationConfigClass.defaultType
        val kClassOutNavDest = kClassOutNavDestinationType()

        // Find the vararg overload of listOf
        val listOfVararg = symbolResolver.listOfFunctions.first {
            it.owner.parameters.size == 1 && it.owner.parameters[0].varargElementType != null
        }

        return builder.irBlock(resultType = configType) {
            // val configBuilder = NavigationConfigBuilder()
            val builderVar = irTemporary(
                irCallConstructor(symbolResolver.navigationConfigBuilderConstructor.symbol, emptyList()),
                nameHint = "configBuilder",
            )

            // Register tabs
            for (tab in metadata.tabs) {
                val containerClassRef = classReference(tab.classId)
                val tabClassRefs = tab.items.map { classReference(it.classId) }
                val tabClassesList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(kClassOutNavDest)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, tabClassRefs)
                }
                // Build tabInstances list: irGetObject for flat tabs, irNull for container refs
                val navDestType = symbolResolver.navDestinationClass.defaultType
                val nullableNavDestType = navDestType.makeNullable()
                val tabInstanceValues = tab.items.map { item ->
                    if (item.type == TabItemType.NESTED_STACK) {
                        irNull(nullableNavDestType)
                    } else {
                        val classSymbol = symbolResolver.resolveClass(item.classId)
                        irGetObject(classSymbol)
                    }
                }
                val tabInstancesList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(nullableNavDestType)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(nullableNavDestType, tabInstanceValues)
                }
                val boolValues = tab.items.map { irBoolean(it.type == TabItemType.NESTED_STACK) }
                val boolType = pluginContext.irBuiltIns.booleanType
                val tabIsContainerRefList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(boolType)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(boolType, boolValues)
                }
                val initialTabIndex = tab.initialTab?.let { initialTabClassId ->
                    tab.items.indexOfFirst { it.classId == initialTabClassId }.takeIf { it >= 0 } ?: 0
                } ?: 0

                val registerFun = symbolResolver.registerTabsContainerFun
                +irCall(registerFun).apply {
                    arguments[registerFun.parameters[0]] = irGet(builderVar) // dispatch receiver
                    arguments[registerFun.parameters[1]] = containerClassRef
                    arguments[registerFun.parameters[2]] = irString(tab.name)
                    arguments[registerFun.parameters[3]] = tabClassesList
                    arguments[registerFun.parameters[4]] = tabInstancesList
                    arguments[registerFun.parameters[5]] = tabIsContainerRefList
                    arguments[registerFun.parameters[6]] = irInt(initialTabIndex)
                }
            }

            // Determine which stacks are tab items
            val tabItemClassIds = metadata.tabs.flatMap { it.items }.map { it.classId }.toSet()

            // Register stacks
            for (stack in metadata.stacks) {
                if (stack.sealedClassId !in tabItemClassIds) {
                    // Non-tab stack: register as full container
                    val destRefs = stack.destinations.map { classReference(it.classId) }
                    val destList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(kClassOutNavDest)).also {
                        it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, destRefs)
                    }
                    val registerFun = symbolResolver.registerStackContainerFun
                    +irCall(registerFun).apply {
                        arguments[registerFun.parameters[0]] = irGet(builderVar)
                        arguments[registerFun.parameters[1]] = classReference(stack.sealedClassId)
                        arguments[registerFun.parameters[2]] = irString(stack.name)
                        arguments[registerFun.parameters[3]] = classReference(stack.startDestination)
                        arguments[registerFun.parameters[4]] = destList
                    }
                } else if (stack.destinations.isNotEmpty()) {
                    // Tab-item stack: register scope for its destinations
                    val memberRefs = stack.destinations.map { classReference(it.classId) }
                    val membersList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(kClassOutNavDest)).also {
                        it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, memberRefs)
                    }
                    val registerFun = symbolResolver.registerScopeFun
                    +irCall(registerFun).apply {
                        arguments[registerFun.parameters[0]] = irGet(builderVar)
                        arguments[registerFun.parameters[1]] = irString(stack.name)
                        arguments[registerFun.parameters[2]] = membersList
                    }
                }
            }

            // Build the config (last expression = block result)
            val buildFun = symbolResolver.configBuilderBuildFun
            +irCall(buildFun).apply {
                arguments[buildFun.parameters[0]] = irGet(builderVar)
            }
        }
    }

    // ---- Helpers ----

    private fun classReference(classId: ClassId): IrExpression {
        val classSymbol = symbolResolver.resolveClass(classId)
        return IrClassReferenceImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = kClassOutType(classSymbol.defaultType),
            symbol = classSymbol,
            classType = classSymbol.defaultType,
        )
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
