@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.NavigationMetadata
import com.jermey.quo.vadis.compiler.common.PaneRole as MetadataPaneRole
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/**
 * Generates the `paneRoleRegistry` property body for the synthesized NavigationConfig class.
 *
 * When no pane items are registered, returns `PaneRoleRegistry.Empty`.
 * When pane items exist, generates an anonymous `object : PaneRoleRegistry` with
 * `when`-based dispatch that maps destination types to their corresponding [PaneRole] values
 * based on metadata from `@Pane`/`@PaneItem` annotations.
 */
class PaneRoleRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val metadata: NavigationMetadata,
) {

    /** Core `PaneRole` enum class for IR references. */
    private val paneRoleEnumClass by lazy {
        symbolResolver.resolveClass("com.jermey.quo.vadis.core.navigation.pane", "PaneRole")
    }

    /** Cached enum entries from the core `PaneRole` enum. */
    private val paneRoleEntries by lazy {
        paneRoleEnumClass.owner.declarations.filterIsInstance<IrEnumEntry>()
    }

    /** Flattened list of all pane item entries across all panes in metadata. */
    private data class PaneItemEntry(
        val destinationClassId: ClassId,
        val role: MetadataPaneRole,
    )

    private val allPaneItems: List<PaneItemEntry> by lazy {
        metadata.panes.flatMap { pane ->
            pane.items.map { item ->
                PaneItemEntry(item.classId, item.role)
            }
        }
    }

    /**
     * Generates the paneRoleRegistry property body.
     * If no pane items are registered, returns PaneRoleRegistry.Empty.
     * Otherwise, generates an anonymous object implementing PaneRoleRegistry with when-based dispatch.
     */
    fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        val getter = property.getter ?: return

        if (allPaneItems.isEmpty()) {
            generateEmptyPaneRoleRegistry(getter)
            return
        }

        generatePaneRoleRegistryObject(getter)
    }

    private fun generateEmptyPaneRoleRegistry(getter: IrSimpleFunction) {
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
                    +irReturn(irGetObject(companion.symbol))
                }
            } else {
                +irReturn(irGetObject(symbolResolver.paneRoleRegistryClass))
            }
        }
    }

    /**
     * Generates an anonymous `object : PaneRoleRegistry` with:
     * - `getPaneRole(scopeKey, destination)`: override with `is` type-check dispatch
     * - `getPaneRole(scopeKey, destinationClass)`: override with `KClass` equality dispatch
     */
    private fun generatePaneRoleRegistryObject(getter: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val paneRoleRegistryType = symbolResolver.paneRoleRegistryClass.defaultType

        // Create anonymous class implementing PaneRoleRegistry
        val anonymousClass = pluginContext.irFactory.buildClass {
            name = Name.special("<no name provided>")
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.LOCAL
        }
        anonymousClass.parent = getter
        anonymousClass.superTypes = listOf(paneRoleRegistryType)

        val anonymousClassType = IrSimpleTypeImpl(
            classifier = anonymousClass.symbol,
            nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments = emptyList(),
            annotations = emptyList(),
        )

        anonymousClass.thisReceiver = buildValueParameter(anonymousClass) {
            name = Name.special("<this>")
            type = anonymousClassType
            origin = IrDeclarationOrigin.INSTANCE_RECEIVER
            kind = IrParameterKind.DispatchReceiver
        }

        // Add primary constructor delegating to Any()
        val constructor = anonymousClass.addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }
        val anyConstructor = pluginContext.irBuiltIns.anyClass.owner.declarations
            .filterIsInstance<IrConstructor>()
            .first()
        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(anyConstructor)
            +IrInstanceInitializerCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                anonymousClass.symbol,
                pluginContext.irBuiltIns.unitType,
            )
        }

        // Add both getPaneRole overrides
        addGetPaneRoleByDestination(anonymousClass)
        addGetPaneRoleByKClass(anonymousClass)

        // Return: object : PaneRoleRegistry { ... }
        getter.body = builder.irBlockBody {
            +irReturn(
                irBlock(resultType = paneRoleRegistryType, origin = IrStatementOrigin.OBJECT_LITERAL) {
                    +anonymousClass
                    +irCallConstructor(constructor.symbol, emptyList())
                },
            )
        }
    }

    /**
     * Adds the `getPaneRole(scopeKey, destination: NavDestination): PaneRole?` override
     * using `is` type checks in a when expression.
     *
     * Generated IR equivalent:
     * ```
     * override fun getPaneRole(scopeKey: ScopeKey, destination: NavDestination): PaneRole? {
     *     return when {
     *         destination is ListDetailPanes.ListScreen -> PaneRole.Primary
     *         destination is ListDetailPanes.DetailScreen -> PaneRole.Supporting
     *         else -> null
     *     }
     * }
     * ```
     */
    private fun addGetPaneRoleByDestination(anonymousClass: IrClass) {
        val originalFun = findGetPaneRoleOverload(byDestination = true)

        val paneRoleType = paneRoleEnumClass.defaultType
        val paneRoleNullableType = paneRoleType.makeNullable()

        val overrideFun = anonymousClass.addFunction {
            name = Name.identifier("getPaneRole")
            returnType = paneRoleNullableType
            modality = Modality.OPEN
            visibility = DescriptorVisibilities.PUBLIC
        }
        overrideFun.overriddenSymbols = listOf(originalFun.symbol)

        // Dispatch receiver
        overrideFun.addValueParameter {
            name = Name.special("<this>")
            type = anonymousClass.thisReceiver!!.type
            origin = IrDeclarationOrigin.INSTANCE_RECEIVER
            kind = IrParameterKind.DispatchReceiver
        }

        // Value parameters matching interface: scopeKey, destination
        val originalRegularParams = originalFun.parameters.filter { it.kind == IrParameterKind.Regular }
        overrideFun.addValueParameter("scopeKey", originalRegularParams[0].type)
        val destParam = overrideFun.addValueParameter(
            "destination",
            symbolResolver.navDestinationClass.defaultType,
        )

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                paneRoleNullableType,
                IrStatementOrigin.WHEN,
            )

            for (item in allPaneItems) {
                val destType = symbolResolver.resolveClass(item.destinationClassId).defaultType
                val enumEntry = resolveCorePaneRoleEntry(item.role)

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.booleanType,
                        IrTypeOperator.INSTANCEOF,
                        destType,
                        irGet(destParam),
                    ),
                    result = IrGetEnumValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        paneRoleType,
                        enumEntry.symbol,
                    ),
                )
            }

            // else -> null
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = IrConstImpl.constNull(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.nothingNType,
                ),
            )

            +irReturn(whenExpr)
        }
    }

    /**
     * Adds the `getPaneRole(scopeKey, destinationClass: KClass<out NavDestination>): PaneRole?` override
     * using class reference equality checks in a when expression.
     *
     * Generated IR equivalent:
     * ```
     * override fun getPaneRole(scopeKey: ScopeKey, destinationClass: KClass<out NavDestination>): PaneRole? {
     *     return when {
     *         destinationClass == ListDetailPanes.ListScreen::class -> PaneRole.Primary
     *         destinationClass == ListDetailPanes.DetailScreen::class -> PaneRole.Supporting
     *         else -> null
     *     }
     * }
     * ```
     */
    private fun addGetPaneRoleByKClass(anonymousClass: IrClass) {
        val originalFun = findGetPaneRoleOverload(byDestination = false)

        val paneRoleType = paneRoleEnumClass.defaultType
        val paneRoleNullableType = paneRoleType.makeNullable()

        val overrideFun = anonymousClass.addFunction {
            name = Name.identifier("getPaneRole")
            returnType = paneRoleNullableType
            modality = Modality.OPEN
            visibility = DescriptorVisibilities.PUBLIC
        }
        overrideFun.overriddenSymbols = listOf(originalFun.symbol)

        // Dispatch receiver
        overrideFun.addValueParameter {
            name = Name.special("<this>")
            type = anonymousClass.thisReceiver!!.type
            origin = IrDeclarationOrigin.INSTANCE_RECEIVER
            kind = IrParameterKind.DispatchReceiver
        }

        // Value parameters matching interface: scopeKey, destinationClass
        val originalRegularParams = originalFun.parameters.filter { it.kind == IrParameterKind.Regular }
        overrideFun.addValueParameter("scopeKey", originalRegularParams[0].type)
        val destClassParam = overrideFun.addValueParameter(
            "destinationClass",
            originalRegularParams[1].type,
        )

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                paneRoleNullableType,
                IrStatementOrigin.WHEN,
            )

            for (item in allPaneItems) {
                val destClassSymbol = symbolResolver.resolveClass(item.destinationClassId)
                val destType = destClassSymbol.defaultType
                val enumEntry = resolveCorePaneRoleEntry(item.role)

                // Build KClass reference: SomeDestination::class
                val kClassType = IrSimpleTypeImpl(
                    classifier = symbolResolver.kClassClass,
                    nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
                    arguments = listOf(makeTypeProjection(destType, Variance.INVARIANT)),
                    annotations = emptyList(),
                )
                val classRef = IrClassReferenceImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    kClassType,
                    destClassSymbol,
                    destType,
                )

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = irEquals(irGet(destClassParam), classRef),
                    result = IrGetEnumValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        paneRoleType,
                        enumEntry.symbol,
                    ),
                )
            }

            // else -> null
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = IrConstImpl.constNull(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.nothingNType,
                ),
            )

            +irReturn(whenExpr)
        }
    }

    /**
     * Finds the correct `getPaneRole` overload from the PaneRoleRegistry interface.
     *
     * @param byDestination true for the `NavDestination` overload, false for the `KClass` overload
     */
    private fun findGetPaneRoleOverload(byDestination: Boolean): IrSimpleFunction {
        val functions = symbolResolver.paneRoleRegistryClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "getPaneRole" }

        return if (byDestination) {
            functions.first { fn ->
                fn.parameters.any { it.name.asString() == "destination" }
            }
        } else {
            functions.first { fn ->
                fn.parameters.any { it.name.asString() == "destinationClass" }
            }
        }
    }

    /**
     * Resolves a metadata [PaneRole] to the corresponding core `PaneRole` enum entry.
     */
    private fun resolveCorePaneRoleEntry(role: MetadataPaneRole): IrEnumEntry {
        val entryName = when (role) {
            MetadataPaneRole.PRIMARY -> "Primary"
            MetadataPaneRole.SECONDARY -> "Supporting"
            MetadataPaneRole.EXTRA -> "Extra"
        }
        return paneRoleEntries.first { it.name.asString() == entryName }
    }
}
