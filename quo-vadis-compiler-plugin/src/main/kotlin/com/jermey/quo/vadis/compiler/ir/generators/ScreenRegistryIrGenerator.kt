@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.common.ScreenMetadata
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
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name

/**
 * Generates the `screenRegistry` property body for the synthesized NavigationConfig class.
 *
 * When no screens are registered, returns `ScreenRegistry.Empty`.
 * When screens exist, generates an anonymous `object : ScreenRegistry` with `when`-based
 * dispatch that maps destination types to their corresponding @Screen composable functions.
 */
class ScreenRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val screens: List<ScreenMetadata>,
    private val moduleFragment: IrModuleFragment? = null,
) {

    /**
     * Finds a function by package and name in the module IR tree.
     * This returns the actual (post-Compose-transform) function, unlike
     * [SymbolResolver.resolveFunctions] which may return a stale pre-transform stub.
     */
    private fun findModuleFunction(packageFqn: String, name: String): IrSimpleFunction? {
        if (moduleFragment == null) return null
        for (file in moduleFragment.files) {
            if (file.packageFqName.asString() != packageFqn) continue
            for (decl in file.declarations) {
                if (decl is IrSimpleFunction && decl.name.asString() == name) {
                    return decl
                }
            }
        }
        return null
    }
    /**
     * Generates the screenRegistry property body.
     * If no screens are registered, returns ScreenRegistry.Empty.
     * Otherwise, generates an anonymous object implementing ScreenRegistry with when-based dispatch.
     */
    fun generatePropertyBody(irClass: IrClass, property: IrProperty) {
        val getter = property.getter ?: return

        if (screens.isEmpty()) {
            generateEmptyScreenRegistry(getter)
            return
        }

        generateScreenRegistryObject(getter)
    }

    /**
     * Generates bodies for a FIR-generated `ScreenRegistryImpl` class.
     * Iterates over the existing stub functions and fills in their bodies.
     */
    fun generateClassBodies(irClass: IrClass) {
        for (function in irClass.functions) {
            when (function.name.asString()) {
                "hasContent" -> {
                    if (function.body == null) generateHasContentBody(function)
                }
                "Content" -> {
                    if (function.body == null) {
                        generateContentBody(function)
                        // Change origin to DEFINED so the Compose compiler processes this function's body
                        function.origin = IrDeclarationOrigin.DEFINED
                    }
                }
            }
        }
        // Also change the class origin so Compose compiler doesn't skip it
        irClass.origin = IrDeclarationOrigin.DEFINED
    }

    private fun generateHasContentBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        // parameters: [0] = dispatch receiver, [1] = destination
        val destParam = function.parameters[1]

        function.body = builder.irBlockBody {
            if (screens.isEmpty()) {
                +irReturn(irFalse())
                return@irBlockBody
            }

            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.booleanType,
                IrStatementOrigin.WHEN,
            )

            for (screen in screens) {
                val destType = symbolResolver.resolveClass(screen.destinationClassId).defaultType
                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.booleanType,
                        IrTypeOperator.INSTANCEOF,
                        destType,
                        irGet(destParam),
                    ),
                    result = irTrue(),
                )
            }

            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irFalse(),
            )

            +irReturn(whenExpr)
        }
    }

    private fun generateContentBody(function: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, function.symbol)
        // parameters: [0] = dispatch receiver, [1] = destination, [2] = sharedTransitionScope, [3] = animatedVisibilityScope
        val destParam = function.parameters[1]
        val stsParam = function.parameters[2]
        val avsParam = function.parameters[3]

        function.body = builder.irBlockBody {
            if (screens.isEmpty()) {
                return@irBlockBody
            }

            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                IrStatementOrigin.WHEN,
            )

            for (screen in screens) {
                val destClassSymbol = symbolResolver.resolveClass(screen.destinationClassId)
                val destType = destClassSymbol.defaultType

                // Prefer the module IR function (post-Compose transform) over the
                // symbol-resolved stub which may be stale (pre-Compose transform).
                val screenFn = findModuleFunction(
                    screen.functionFqn.parent().asString(),
                    screen.functionFqn.shortName().asString(),
                ) ?: run {
                    val symbols = symbolResolver.resolveFunctions(
                        screen.functionFqn.parent().asString(),
                        screen.functionFqn.shortName().asString(),
                    )
                    symbols.firstOrNull()?.owner
                } ?: continue

                val screenCall = irCall(screenFn.symbol).apply {
                    // Track which original (non-Compose) params use their defaults
                    var defaultMask = 0
                    var originalParamIndex = 0

                    for (param in screenFn.parameters) {
                        if (param.kind == IrParameterKind.DispatchReceiver || param.kind == IrParameterKind.ExtensionReceiver) continue
                        val paramName = param.name.asString()

                        // Skip Compose-injected params for index tracking
                        if (paramName.startsWith("\$")) {
                            when (paramName) {
                                "\$composer" -> {
                                    val contentComposer = function.parameters.firstOrNull {
                                        it.name.asString() == "\$composer"
                                    }
                                    if (contentComposer != null) {
                                        arguments[param] = irGet(contentComposer)
                                    }
                                }
                                "\$changed" -> {
                                    arguments[param] = irInt(0)
                                }
                                "\$default" -> {
                                    // Will be set after the loop with computed mask
                                }
                            }
                            continue
                        }

                        // Regular parameter — try to provide explicitly
                        var argProvided = false
                        when (paramName) {
                            "destination" -> if (screen.hasDestinationParam) {
                                arguments[param] = IrTypeOperatorCallImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    destType,
                                    IrTypeOperator.IMPLICIT_CAST,
                                    destType,
                                    irGet(destParam),
                                )
                                argProvided = true
                            }
                            "sharedTransitionScope" -> if (screen.hasSharedTransitionScope) {
                                arguments[param] = irGet(stsParam)
                                argProvided = true
                            }
                            "animatedVisibilityScope" -> if (screen.hasAnimatedVisibilityScope) {
                                arguments[param] = irGet(avsParam)
                                argProvided = true
                            }
                            else -> {
                                param.defaultValue?.let { defaultBody ->
                                    arguments[param] = defaultBody.expression.deepCopyWithSymbols()
                                    argProvided = true
                                }
                            }
                        }

                        if (!argProvided) {
                            // Mark this param to use its default via the $default bitmask.
                            // Pass null as a placeholder — the function body will replace it.
                            defaultMask = defaultMask or (1 shl originalParamIndex)
                            arguments[param] = IrConstImpl.constNull(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                pluginContext.irBuiltIns.nothingNType,
                            )
                        }
                        originalParamIndex++
                    }

                    // Set $default with computed bitmask
                    screenFn.parameters
                        .firstOrNull { it.name.asString() == "\$default" }
                        ?.let { dp -> arguments[dp] = irInt(defaultMask) }
                }

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.booleanType,
                        IrTypeOperator.INSTANCEOF,
                        destType,
                        irGet(destParam),
                    ),
                    result = screenCall,
                )
            }

            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irGetObject(pluginContext.irBuiltIns.unitClass),
            )

            +whenExpr
        }
    }

    /**
     * Generates an anonymous `object : ScreenRegistry` with:
     * - `Content()`: @Composable override with when-based dispatch calling @Screen functions
     * - `hasContent()`: override returning true for registered destination types
     */
    private fun generateScreenRegistryObject(getter: IrSimpleFunction) {
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val screenRegistryType = symbolResolver.screenRegistryClass.defaultType

        // Create anonymous class implementing ScreenRegistry
        val anonymousClass = pluginContext.irFactory.buildClass {
            name = Name.special("<no name provided>")
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.LOCAL
        }
        anonymousClass.parent = getter
        anonymousClass.superTypes = listOf(screenRegistryType)

        // Construct type manually to avoid circular dependency (defaultType needs thisReceiver)
        val anonymousClassType = IrSimpleTypeImpl(
            classifier = anonymousClass.symbol,
            nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments = emptyList(),
            annotations = emptyList(),
        )

        // Add this receiver
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

        // Add hasContent() and Content() overrides
        addHasContentOverride(anonymousClass)
        addContentOverride(anonymousClass)

        // Return: object : ScreenRegistry { ... }
        getter.body = builder.irBlockBody {
            +irReturn(
                irBlock(resultType = screenRegistryType, origin = IrStatementOrigin.OBJECT_LITERAL) {
                    +anonymousClass
                    +irCallConstructor(constructor.symbol, emptyList())
                },
            )
        }
    }

    /**
     * Adds `hasContent()` override that returns true for all registered destination types.
     *
     * Generated IR equivalent:
     * ```
     * override fun hasContent(destination: NavDestination): Boolean {
     *     return when (destination) {
     *         is DestA, is DestB -> true
     *         else -> false
     *     }
     * }
     * ```
     */
    private fun addHasContentOverride(anonymousClass: IrClass) {
        val originalFun = symbolResolver.screenRegistryClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == "hasContent" }

        val overrideFun = anonymousClass.addFunction {
            name = Name.identifier("hasContent")
            returnType = pluginContext.irBuiltIns.booleanType
            modality = Modality.OPEN
            visibility = DescriptorVisibilities.PUBLIC
        }
        overrideFun.overriddenSymbols = listOf(originalFun.symbol)

        // Dispatch receiver (this) - must be added first
        overrideFun.addValueParameter {
            name = Name.special("<this>")
            type = anonymousClass.thisReceiver!!.type
            origin = IrDeclarationOrigin.INSTANCE_RECEIVER
            kind = IrParameterKind.DispatchReceiver
        }

        // Value parameter: destination: NavDestination
        val destParam = overrideFun.addValueParameter(
            "destination",
            symbolResolver.navDestinationClass.defaultType,
        )

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.booleanType,
                IrStatementOrigin.WHEN,
            )

            // One branch per registered screen: is DestType -> true
            for (screen in screens) {
                val destType = symbolResolver.resolveClass(screen.destinationClassId).defaultType
                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.booleanType,
                        IrTypeOperator.INSTANCEOF,
                        destType,
                        irGet(destParam),
                    ),
                    result = irTrue(),
                )
            }

            // else -> false
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irFalse(),
            )

            +irReturn(whenExpr)
        }
    }

    /**
     * Adds `Content()` @Composable override with when-based dispatch to @Screen functions.
     *
     * Generated IR equivalent:
     * ```
     * @Composable
     * override fun Content(
     *     destination: NavDestination,
     *     sharedTransitionScope: SharedTransitionScope?,
     *     animatedVisibilityScope: AnimatedVisibilityScope?
     * ) {
     *     when (destination) {
     *         is DestA -> ScreenA()
     *         is DestB -> ScreenB(destination = destination)
     *         else -> {} // no-op
     *     }
     * }
     * ```
     */
    private fun addContentOverride(anonymousClass: IrClass) {
        val originalFun = symbolResolver.screenRegistryClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == "Content" }

        val overrideFun = anonymousClass.addFunction {
            name = Name.identifier("Content")
            returnType = pluginContext.irBuiltIns.unitType
            modality = Modality.OPEN
            visibility = DescriptorVisibilities.PUBLIC
        }
        overrideFun.overriddenSymbols = listOf(originalFun.symbol)

        // Add @Composable annotation
        val composableClass = symbolResolver.composableAnnotation
        if (composableClass != null) {
            val composableConstructor = composableClass.owner.declarations
                .filterIsInstance<IrConstructor>()
                .first()
            val annotationBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
            overrideFun.annotations = overrideFun.annotations +
                annotationBuilder.irCallConstructor(composableConstructor.symbol, emptyList())
        }

        // Dispatch receiver (this) - must be added first
        overrideFun.addValueParameter {
            name = Name.special("<this>")
            type = anonymousClass.thisReceiver!!.type
            origin = IrDeclarationOrigin.INSTANCE_RECEIVER
            kind = IrParameterKind.DispatchReceiver
        }

        // Value parameters matching original interface method
        // parameters[0] = dispatch receiver, [1] = destination, [2] = sharedTransitionScope, [3] = animatedVisibilityScope
        val destParam = overrideFun.addValueParameter(
            "destination",
            symbolResolver.navDestinationClass.defaultType,
        )
        val stsParam = overrideFun.addValueParameter(
            "sharedTransitionScope",
            originalFun.parameters[2].type,
        )
        val avsParam = overrideFun.addValueParameter(
            "animatedVisibilityScope",
            originalFun.parameters[3].type,
        )

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                IrStatementOrigin.WHEN,
            )

            for (screen in screens) {
                val destClassSymbol = symbolResolver.resolveClass(screen.destinationClassId)
                val destType = destClassSymbol.defaultType

                // Resolve the @Screen composable function
                val screenFnSymbols = symbolResolver.resolveFunctions(
                    screen.functionFqn.parent().asString(),
                    screen.functionFqn.shortName().asString(),
                )
                val screenFnSymbol = screenFnSymbols.firstOrNull() ?: continue
                val screenFn = screenFnSymbol.owner

                // Build function call with parameters based on ScreenMetadata flags
                val screenCall = irCall(screenFnSymbol).apply {
                    for (param in screenFn.parameters) {
                        when (param.name.asString()) {
                            "destination" -> if (screen.hasDestinationParam) {
                                // Smart cast: destination as SpecificDestType
                                arguments[param] = IrTypeOperatorCallImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    destType,
                                    IrTypeOperator.IMPLICIT_CAST,
                                    destType,
                                    irGet(destParam),
                                )
                            }
                            "sharedTransitionScope" -> if (screen.hasSharedTransitionScope) {
                                arguments[param] = irGet(stsParam)
                            }
                            "animatedVisibilityScope" -> if (screen.hasAnimatedVisibilityScope) {
                                arguments[param] = irGet(avsParam)
                            }
                        }
                    }
                }

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = IrTypeOperatorCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.booleanType,
                        IrTypeOperator.INSTANCEOF,
                        destType,
                        irGet(destParam),
                    ),
                    result = screenCall,
                )
            }

            // else -> Unit (no-op for unregistered destinations)
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = irGetObject(pluginContext.irBuiltIns.unitClass),
            )

            +whenExpr
        }
    }

    private fun generateEmptyScreenRegistry(getter: IrSimpleFunction) {
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
                error(
                    "ScreenRegistry interface must have a companion object with an 'Empty' property. " +
                        "This is a bug in the quo-vadis-core library.",
                )
            }
        }
    }
}
