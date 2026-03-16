@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.common.TabItemType
import com.jermey.quo.vadis.compiler.common.TransitionMetadata
import com.jermey.quo.vadis.compiler.common.TransitionType
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
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
import org.jetbrains.kotlin.ir.declarations.IrProperty
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
        val listOfVararg = symbolResolver.listOfFunctions.firstOrNull {
            it.owner.parameters.size == 1 && it.owner.parameters[0].varargElementType != null
        } ?: error("Expected 'listOf(vararg)' function in kotlin.collections. Ensure quo-vadis-core version is compatible with compiler plugin.")

        return builder.irBlock(resultType = configType) {
            // val configBuilder = NavigationConfigBuilder()
            val builderVar = irTemporary(
                irCallConstructor(symbolResolver.navigationConfigBuilderConstructor.symbol, emptyList()),
                nameHint = "configBuilder",
            )

            // Register stacks
            for (stack in metadata.stacks) {
                val destRefs = stack.destinations.map { classReference(it.classId) }
                val destList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(kClassOutNavDest)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, destRefs)
                }
                val navDestType = symbolResolver.navDestinationClass.defaultType
                val nullableNavDestType = navDestType.makeNullable()
                val startDestinationSymbol = symbolResolver.resolveClass(stack.startDestination)
                val startDestinationInstance = if (startDestinationSymbol.owner.kind == ClassKind.OBJECT) {
                    irGetObject(startDestinationSymbol)
                } else {
                    irNull(nullableNavDestType)
                }
                val registerFun = symbolResolver.registerStackContainerFun
                +irCall(registerFun).apply {
                    arguments[registerFun.parameters[0]] = irGet(builderVar)
                    arguments[registerFun.parameters[1]] = classReference(stack.sealedClassId)
                    arguments[registerFun.parameters[2]] = irString(stack.name)
                    arguments[registerFun.parameters[3]] = classReference(stack.startDestination)
                    arguments[registerFun.parameters[4]] = startDestinationInstance
                    arguments[registerFun.parameters[5]] = destList
                }
            }

            // Register tabs after stacks so nested stack scopes remain the primary getScopeKey mapping.
            // Skip @Tabs with zero items — cross-module tabs are handled by the consuming module.
            for (tab in metadata.tabs) {
                if (tab.items.isEmpty()) continue
                val containerClassRef = classReference(tab.classId)
                val tabClassRefs = tab.items.map { classReference(it.classId) }
                val tabClassesList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(kClassOutNavDest)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, tabClassRefs)
                }
                val navDestType = symbolResolver.navDestinationClass.defaultType
                val nullableNavDestType = navDestType.makeNullable()
                val tabInstanceValues = tab.items.map { item ->
                    if (item.type == TabItemType.STACK || item.type == TabItemType.TABS) {
                        irNull(nullableNavDestType)
                    } else {
                        val classSymbol = symbolResolver.resolveClass(item.classId)
                        irGetObject(classSymbol)
                    }
                }
                val tabInstancesList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(nullableNavDestType)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(nullableNavDestType, tabInstanceValues)
                }
                val boolValues = tab.items.map { irBoolean(it.type == TabItemType.STACK || it.type == TabItemType.TABS) }
                val boolType = pluginContext.irBuiltIns.booleanType
                val tabIsContainerRefList = irCall(listOfVararg, listOfVararg.owner.returnType, listOf(boolType)).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(boolType, boolValues)
                }
                // Initial tab is always index 0 (ordinal=0 is sorted first)
                val initialTabIndex = 0

                val registerFun = symbolResolver.registerTabsContainerFun
                +irCall(registerFun).apply {
                    arguments[registerFun.parameters[0]] = irGet(builderVar)
                    arguments[registerFun.parameters[1]] = containerClassRef
                    arguments[registerFun.parameters[2]] = irString(tab.name)
                    arguments[registerFun.parameters[3]] = tabClassesList
                    arguments[registerFun.parameters[4]] = tabInstancesList
                    arguments[registerFun.parameters[5]] = tabIsContainerRefList
                    arguments[registerFun.parameters[6]] = irInt(initialTabIndex)
                }

                if (tab.allDestinationClassIds.isEmpty()) {
                    continue
                }

                val allDestRefs = tab.allDestinationClassIds.map { classReference(it) }
                val allDestList = irCall(
                    listOfVararg,
                    listOfVararg.owner.returnType,
                    listOf(kClassOutNavDest),
                ).also {
                    it.arguments[listOfVararg.owner.parameters[0]] = irVararg(kClassOutNavDest, allDestRefs)
                }

                val registerScopeFun = symbolResolver.registerScopeFun
                +irCall(registerScopeFun).apply {
                    arguments[registerScopeFun.parameters[0]] = irGet(builderVar)
                    arguments[registerScopeFun.parameters[1]] = irString(tab.name)
                    arguments[registerScopeFun.parameters[2]] = allDestList
                }
            }

            // Register panes
            for (pane in metadata.panes) {
                val containerClassRef = classReference(pane.classId)
                val paneClassRefs = pane.items.map { classReference(it.classId) }
                val paneClassesList = irCall(
                    listOfVararg,
                    listOfVararg.owner.returnType,
                    listOf(kClassOutNavDest),
                ).also {
                    it.arguments[listOfVararg.owner.parameters[0]] =
                        irVararg(kClassOutNavDest, paneClassRefs)
                }
                // Build pane instances list (object items get instances, others get null)
                val navDestType = symbolResolver.navDestinationClass.defaultType
                val nullableNavDestType = navDestType.makeNullable()
                val paneInstanceValues = pane.items.map { item ->
                    val classSymbol = symbolResolver.resolveClass(item.classId)
                    if (classSymbol.owner.kind == ClassKind.OBJECT) {
                        irGetObject(classSymbol)
                    } else {
                        irNull(nullableNavDestType)
                    }
                }
                val paneInstancesList = irCall(
                    listOfVararg,
                    listOfVararg.owner.returnType,
                    listOf(nullableNavDestType),
                ).also {
                    it.arguments[listOfVararg.owner.parameters[0]] =
                        irVararg(nullableNavDestType, paneInstanceValues)
                }
                // Build roles list (PaneRole ordinals)
                val intType = pluginContext.irBuiltIns.intType
                val roleValues = pane.items.map { irInt(it.role.ordinal) }
                val rolesList = irCall(
                    listOfVararg,
                    listOfVararg.owner.returnType,
                    listOf(intType),
                ).also {
                    it.arguments[listOfVararg.owner.parameters[0]] =
                        irVararg(intType, roleValues)
                }

                val registerFun = symbolResolver.registerPaneContainerFun
                +irCall(registerFun).apply {
                    arguments[registerFun.parameters[0]] = irGet(builderVar)
                    arguments[registerFun.parameters[1]] = containerClassRef
                    arguments[registerFun.parameters[2]] = irString(pane.name)
                    arguments[registerFun.parameters[3]] = paneClassesList
                    arguments[registerFun.parameters[4]] = paneInstancesList
                    arguments[registerFun.parameters[5]] = rolesList
                    arguments[registerFun.parameters[6]] = irInt(pane.backBehavior.ordinal)
                }

                // Register scope for ALL sealed subclass destinations (not just @PaneItem roots)
                if (pane.allDestinationClassIds.isNotEmpty()) {
                    val allDestRefs = pane.allDestinationClassIds.map { classReference(it) }
                    val allDestList = irCall(
                        listOfVararg,
                        listOfVararg.owner.returnType,
                        listOf(kClassOutNavDest),
                    ).also {
                        it.arguments[listOfVararg.owner.parameters[0]] =
                            irVararg(kClassOutNavDest, allDestRefs)
                    }
                    val registerScopeFun = symbolResolver.registerScopeFun
                    +irCall(registerScopeFun).apply {
                        arguments[registerScopeFun.parameters[0]] = irGet(builderVar)
                        arguments[registerScopeFun.parameters[1]] = irString(pane.name)
                        arguments[registerScopeFun.parameters[2]] = allDestList
                    }
                }
            }

            // Register transitions
            for (transition in metadata.transitions) {
                val transitionExpr = resolveNavTransitionExpression(transition)
                    ?: continue
                val destClassRef = classReference(transition.destinationClassId)
                val registerFun = symbolResolver.registerTransitionFun
                +irCall(registerFun).apply {
                    arguments[registerFun.parameters[0]] = irGet(builderVar)
                    arguments[registerFun.parameters[1]] = destClassRef
                    arguments[registerFun.parameters[2]] = transitionExpr
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

    /**
     * Resolves a [TransitionMetadata] to an IR expression referencing the
     * corresponding [NavTransition] companion property (e.g. `NavTransition.SlideHorizontal`).
     *
     * For [TransitionType.CUSTOM], attempts to resolve the custom class as an object instance.
     *
     * @return An [IrExpression] producing a [NavTransition], or `null` if resolution fails.
     */
    private fun IrBuilderWithScope.resolveNavTransitionExpression(
        transition: TransitionMetadata,
    ): IrExpression? {
        if (transition.type == TransitionType.CUSTOM) {
            val customClassId = transition.customClass ?: return null
            val customSymbol = symbolResolver.resolveClass(customClassId)
            if (customSymbol.owner.kind != ClassKind.OBJECT) return null
            return irGetObject(customSymbol)
        }

        val propertyName = when (transition.type) {
            TransitionType.SLIDE_HORIZONTAL -> "SlideHorizontal"
            TransitionType.SLIDE_VERTICAL -> "SlideVertical"
            TransitionType.FADE -> "Fade"
            TransitionType.NONE -> "None"
            TransitionType.CUSTOM -> return null
        }

        val companion = symbolResolver.navTransitionClass.owner.declarations
            .filterIsInstance<IrClass>()
            .firstOrNull { it.isCompanion } ?: return null

        val prop = companion.declarations
            .filterIsInstance<IrProperty>()
            .firstOrNull { it.name.asString() == propertyName } ?: return null

        val getter = prop.getter ?: return null

        return irCall(getter).apply {
            dispatchReceiver = irGetObject(companion.symbol)
        }
    }
}
