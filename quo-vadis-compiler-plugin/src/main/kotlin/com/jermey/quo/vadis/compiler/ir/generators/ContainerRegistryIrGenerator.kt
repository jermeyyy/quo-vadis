@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.generators

import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import com.jermey.quo.vadis.compiler.ir.validation.ValidatedPaneContainerBinding
import com.jermey.quo.vadis.compiler.ir.validation.ValidatedTabsContainerBinding
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
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
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.name.Name

/**
 * Generates the `containerRegistry` property body for the synthesized NavigationConfig class.
 *
 * When no tabs/pane containers are registered, returns `ContainerRegistry.Empty`.
 * When containers exist, generates an anonymous `object : ContainerRegistry` with:
 * - `getContainerInfo()` delegated to `_baseConfig.containerRegistry`
 * - `hasTabsContainer()`/`hasPaneContainer()` checking against known key sets
 * - `TabsContainer()`/`PaneContainer()` with when-based dispatch to annotated composables
 */
internal class ContainerRegistryIrGenerator(
    private val pluginContext: IrPluginContext,
    private val symbolResolver: SymbolResolver,
    private val tabsBindings: List<ValidatedTabsContainerBinding>,
    private val paneBindings: List<ValidatedPaneContainerBinding>,
) {
    /**
     * Generates the containerRegistry property body.
     * If no containers are registered, returns ContainerRegistry.Empty.
     * Otherwise, generates an anonymous object implementing ContainerRegistry with when-based dispatch.
     */
    fun generatePropertyBody(
        irClass: IrClass,
        property: IrProperty,
        baseConfigField: IrField? = null,
    ) {
        val getter = property.getter ?: return

        if (tabsBindings.isEmpty() && paneBindings.isEmpty()) {
            generateEmptyContainerRegistry(getter)
            return
        }

        generateContainerRegistryObject(getter, baseConfigField)
    }

    // region Anonymous Object Generation

    /**
     * Generates an anonymous `object : ContainerRegistry` with:
     * - `getContainerInfo()`: delegates to _baseConfig.containerRegistry via an explicit field
     * - `hasTabsContainer()`/`hasPaneContainer()`: key-set membership checks
     * - `TabsContainer()`/`PaneContainer()`: when-based dispatch to @TabsContainer/@PaneContainer composables
     *
     * The anonymous class stores the base ContainerRegistry in an explicit constructor
     * parameter + field rather than capturing a local variable. This avoids the
     * `LocalDeclarationsLowering` crash ("No dispatch receiver parameter for FUN
     * name:getContainerInfo") that occurred when the anonymous class captured a local
     * variable from the enclosing getter.
     */
    private fun generateContainerRegistryObject(getter: IrSimpleFunction, baseConfigField: IrField?) {
        val builder = DeclarationIrBuilder(pluginContext, getter.symbol)
        val containerRegistryType = symbolResolver.containerRegistryClass.defaultType

        // Create anonymous class implementing ContainerRegistry
        val anonymousClass = pluginContext.irFactory.buildClass {
            name = Name.special("<no name provided>")
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.LOCAL
        }
        anonymousClass.parent = getter
        anonymousClass.superTypes = listOf(containerRegistryType)

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

        // Add a private field to hold the base ContainerRegistry (avoids local variable capture)
        val baseRegistryField: IrField? = if (baseConfigField != null) {
            anonymousClass.addField {
                name = Name.identifier("baseContainerRegistry")
                type = containerRegistryType
                visibility = DescriptorVisibilities.PRIVATE
            }
        } else {
            null
        }

        // Add primary constructor with optional ContainerRegistry parameter, delegating to Any()
        val constructor = anonymousClass.addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }
        val constructorParam = if (baseRegistryField != null) {
            constructor.addValueParameter(
                "baseContainerRegistry",
                containerRegistryType,
            )
        } else {
            null
        }
        val anyConstructor = pluginContext.irBuiltIns.anyClass.owner.declarations
            .filterIsInstance<IrConstructor>()
            .firstOrNull()
            ?: error("Expected constructor in Any class. Ensure Kotlin stdlib is available.")
        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(anyConstructor)
            if (baseRegistryField != null && constructorParam != null) {
                // Store constructor parameter → field: this.baseContainerRegistry = param
                +IrSetFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    baseRegistryField.symbol,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        anonymousClassType,
                        anonymousClass.thisReceiver!!.symbol,
                    ),
                    irGet(constructorParam),
                    pluginContext.irBuiltIns.unitType,
                )
            }
            +IrInstanceInitializerCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                anonymousClass.symbol,
                pluginContext.irBuiltIns.unitType,
            )
        }

        // Add all ContainerRegistry overrides
        addGetContainerInfoOverride(anonymousClass, baseRegistryField)
        addHasTabsContainerOverride(anonymousClass)
        addHasPaneContainerOverride(anonymousClass)
        addTabsContainerOverride(anonymousClass)
        addPaneContainerOverride(anonymousClass)

        // Build the getter body: construct the anonymous object, passing the base
        // ContainerRegistry as a constructor argument (no local variable capture).
        getter.body = builder.irBlockBody {
            val constructorCall = irCallConstructor(constructor.symbol, emptyList())
            if (baseConfigField != null && constructorParam != null) {
                constructorCall.arguments[constructorParam] =
                    buildBaseContainerRegistryGet(getter, baseConfigField)
            }
            +irReturn(
                irBlock(resultType = containerRegistryType, origin = IrStatementOrigin.OBJECT_LITERAL) {
                    +anonymousClass
                    +constructorCall
                },
            )
        }
    }

    /**
     * Builds the IR expression for `_baseConfig.containerRegistry`.
     */
    private fun buildBaseContainerRegistryGet(
        getter: IrSimpleFunction,
        baseConfigField: IrField,
    ): org.jetbrains.kotlin.ir.expressions.IrExpression {
        val navConfigContainerRegistryProp = symbolResolver.navigationConfigClass.owner.declarations
            .filterIsInstance<IrProperty>()
            .firstOrNull { it.name.asString() == "containerRegistry" }
            ?: error("Expected property 'containerRegistry' in NavigationConfig.")
        val navConfigContainerRegistryGetter = navConfigContainerRegistryProp.getter!!

        val outerThisParam = getter.parameters.firstOrNull {
            it.kind == IrParameterKind.DispatchReceiver
        }
        val baseConfigGet = if (outerThisParam != null) {
            IrGetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                baseConfigField.symbol,
                baseConfigField.type,
                IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    outerThisParam.type,
                    outerThisParam.symbol,
                ),
            )
        } else {
            IrGetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                baseConfigField.symbol,
                baseConfigField.type,
            )
        }

        return IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            navConfigContainerRegistryGetter.returnType,
            navConfigContainerRegistryGetter.symbol,
            typeArgumentsCount = 0,
        ).apply {
            arguments[navConfigContainerRegistryGetter.parameters[0]] = baseConfigGet
        }
    }

    // endregion

    // region getContainerInfo — delegates to captured _baseConfig.containerRegistry

    /**
     * Generates `getContainerInfo(destination)` override using [deepCopyWithSymbols].
     * When [baseRegistryField] is non-null, the body delegates to the field:
     * `this.baseContainerRegistry.getContainerInfo(destination)`.
     * Otherwise, returns null.
     */
    private fun addGetContainerInfoOverride(
        anonymousClass: IrClass,
        baseRegistryField: IrField?,
    ) {
        val containerRegistryOwner = symbolResolver.containerRegistryClass.owner
        val originalFun = containerRegistryOwner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "getContainerInfo" }
            ?: error("Expected function 'getContainerInfo' in ContainerRegistry. Ensure quo-vadis-core version is compatible with compiler plugin.")

        val overrideFun = originalFun.deepCopyWithSymbols(anonymousClass).apply {
            isFakeOverride = false
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.DEFINED
            overriddenSymbols = listOf(originalFun.symbol)
            parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }?.let {
                it.type = anonymousClass.thisReceiver!!.type
            }
        }
        anonymousClass.declarations += overrideFun

        val thisParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.DispatchReceiver
        }
        val destParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.Regular && it.name.asString() == "destination"
        }

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        if (baseRegistryField != null) {
            // Delegate to the explicit field: this.baseContainerRegistry.getContainerInfo(destination)
            overrideFun.body = bodyBuilder.irBlockBody {
                val fieldGet = IrGetFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    baseRegistryField.symbol,
                    baseRegistryField.type,
                    irGet(thisParam),
                )
                +irReturn(
                    irCall(originalFun).apply {
                        arguments[originalFun.parameters[0]] = fieldGet
                        arguments[originalFun.parameters[1]] = irGet(destParam)
                    },
                )
            }
        } else {
            // No base config — return null
            overrideFun.body = bodyBuilder.irBlockBody {
                +irReturn(
                    IrConstImpl.constNull(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        pluginContext.irBuiltIns.nothingNType,
                    ),
                )
            }
        }
    }

    // endregion

    // region hasTabsContainer / hasPaneContainer — key-set checks

    private fun addHasTabsContainerOverride(anonymousClass: IrClass) {
        val tabKeys = tabsBindings.map { it.wrapperKey }.toSet()
        addHasContainerOverride(anonymousClass, "hasTabsContainer", "tabNodeKey", tabKeys)
    }

    private fun addHasPaneContainerOverride(anonymousClass: IrClass) {
        val paneKeys = paneBindings.map { it.wrapperKey }.toSet()
        addHasContainerOverride(anonymousClass, "hasPaneContainer", "paneNodeKey", paneKeys)
    }

    /**
     * Generates a has*Container override that returns true if the key matches one of the known keys.
     *
     * Uses [deepCopyWithSymbols] from the original interface method to produce an override
     * with identical IR structure (parameter types, mangling metadata, etc.). This ensures
     * the JVM backend treats it the same as a hand-written override, avoiding the missing
     * method bug that occurs when using [addFunction] for local anonymous classes.
     *
     * Generated IR equivalent:
     * ```
     * override fun hasTabsContainer(tabNodeKey: String): Boolean {
     *     return when (tabNodeKey) {
     *         "key1", "key2" -> true
     *         else -> false
     *     }
     * }
     * ```
     */
    private fun addHasContainerOverride(
        anonymousClass: IrClass,
        functionName: String,
        paramName: String,
        keys: Set<String>,
    ) {
        val containerRegistryOwner = symbolResolver.containerRegistryClass.owner
        val originalFun = containerRegistryOwner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == functionName }
            ?: error("Expected function '$functionName' in ContainerRegistry. Ensure quo-vadis-core version is compatible with compiler plugin.")

        val overrideFun = originalFun.deepCopyWithSymbols(anonymousClass).apply {
            isFakeOverride = false
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.DEFINED
            overriddenSymbols = listOf(originalFun.symbol)
            parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }?.let {
                it.type = anonymousClass.thisReceiver!!.type
            }
        }
        anonymousClass.declarations += overrideFun

        val keyParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.Regular && it.name.asString() == paramName
        }

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            if (keys.isEmpty()) {
                +irReturn(irFalse())
                return@irBlockBody
            }

            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.booleanType,
                IrStatementOrigin.WHEN,
            )

            for (key in keys) {
                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = irStringEquals(irGet(keyParam), irString(key)),
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

    // endregion

    // region TabsContainer / PaneContainer — composable when-dispatch

    private fun addTabsContainerOverride(anonymousClass: IrClass) {
        addComposableContainerOverride(
            anonymousClass = anonymousClass,
            functionName = "TabsContainer",
            keyParamName = "tabNodeKey",
            scopeParamName = "scope",
            fallbackToContent = false,
            containers = tabsBindings.map {
                ContainerDispatchEntry(
                    key = it.wrapperKey,
                    wrapperFunction = it.wrapperFunction,
                )
            },
        )
    }

    private fun addPaneContainerOverride(anonymousClass: IrClass) {
        addComposableContainerOverride(
            anonymousClass = anonymousClass,
            functionName = "PaneContainer",
            keyParamName = "paneNodeKey",
            scopeParamName = "scope",
            fallbackToContent = true,
            containers = paneBindings.map {
                ContainerDispatchEntry(
                    key = it.wrapperKey,
                    wrapperFunction = it.wrapperFunction,
                )
            },
        )
    }

    /**
        * Generates a composable container override with when-based dispatch.
     *
     * Generated IR equivalent:
     * ```
     * @Composable
     * override fun TabsContainer(tabNodeKey: String, scope: TabsContainerScope, content: @Composable () -> Unit) {
     *     when (tabNodeKey) {
     *         "MainTabs" -> MainTabsWrapper(scope = scope, content = content)
        *         else -> error(...)
     *     }
     * }
        *
        * PaneContainer generation uses the same helper but keeps the existing `else -> content()` fallback.
     * ```
     */
    @Suppress("LongMethod")
    private fun addComposableContainerOverride(
        anonymousClass: IrClass,
        functionName: String,
        keyParamName: String,
        scopeParamName: String,
        fallbackToContent: Boolean,
        containers: List<ContainerDispatchEntry>,
    ) {
        val containerRegistryOwner = symbolResolver.containerRegistryClass.owner
        val originalFun = containerRegistryOwner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == functionName }
            ?: error("Expected function '$functionName' in ContainerRegistry. Ensure quo-vadis-core version is compatible with compiler plugin.")

        // deepCopyWithSymbols from original interface method (consistent with non-composable
        // overrides like addHasContainerOverride) to preserve IR structure for JVM backend.
        val overrideFun = originalFun.deepCopyWithSymbols(anonymousClass).apply {
            isFakeOverride = false
            modality = Modality.OPEN
            origin = IrDeclarationOrigin.DEFINED
            // DO NOT link overriddenSymbols to the pre-Compose interface method.
            // The deserialized stub has pre-Compose types (content: @Composable () -> Unit = Function0),
            // but the compiled JVM interface has post-Compose types (content: Function2<Composer, Int, Unit>).
            // Linking via overriddenSymbols causes the JVM backend to compute the wrong JVM descriptor.
            overriddenSymbols = emptyList()
            parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver }?.let {
                it.type = anonymousClass.thisReceiver!!.type
            }
        }
        anonymousClass.declarations += overrideFun

        // Add @Composable annotation
        val composableClass = symbolResolver.composableAnnotation
        if (composableClass != null) {
            val composableConstructor = composableClass.owner.declarations
                .filterIsInstance<IrConstructor>()
                .firstOrNull()
                ?: error("Expected constructor in @Composable annotation class. Ensure Compose runtime is available.")
            val annotationBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
            overrideFun.annotations = overrideFun.annotations +
                annotationBuilder.irCallConstructor(composableConstructor.symbol, emptyList())
        }

        // The deep copy has pre-Compose parameter structure from the deserialized interface:
        //   [dispatch, keyParam: String, scope: *Scope, content: @Composable () -> Unit]
        // Transform to match post-Compose JVM ABI:
        //   [dispatch, keyParam: String, scope: *Scope, content: Function2<Composer?,Int,Unit>, $composer: Composer?, $changed: Int]

        // Build the Compose-transformed content type: Function2<Composer?, Int, Unit>
        val composerClassSymbol = symbolResolver.resolveClass("androidx.compose.runtime", "Composer")
        val composerNullableType = IrSimpleTypeImpl(
            classifier = composerClassSymbol,
            nullability = SimpleTypeNullability.MARKED_NULLABLE,
            arguments = emptyList(),
            annotations = emptyList(),
        )
        val function2ContentType = pluginContext.irBuiltIns.functionN(2).typeWith(
            composerNullableType,
            pluginContext.irBuiltIns.intType,
            pluginContext.irBuiltIns.unitType,
        )

        // Replace content param type from @Composable () -> Unit to Function2
        overrideFun.parameters
            .firstOrNull { it.kind == IrParameterKind.Regular && it.name.asString() == "content" }
            ?.let { it.type = function2ContentType }

        // Add Compose-injected params ($composer, $changed)
        overrideFun.addValueParameter("\$composer", composerNullableType)
        overrideFun.addValueParameter("\$changed", pluginContext.irBuiltIns.intType)

        // Extract param references for use in the body
        val keyParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.Regular && it.name.asString() == keyParamName
        }
        val scopeParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.Regular && it.name.asString() == scopeParamName
        }
        val contentParam = overrideFun.parameters.first {
            it.kind == IrParameterKind.Regular && it.name.asString() == "content"
        }

        val bodyBuilder = DeclarationIrBuilder(pluginContext, overrideFun.symbol)
        overrideFun.body = bodyBuilder.irBlockBody {
            val whenExpr = IrWhenImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                IrStatementOrigin.WHEN,
            )

            for (entry in containers) {
                val wrapperCall = irCall(entry.wrapperFunction.symbol).apply {
                    var defaultMask = 0
                    var originalParamIndex = 0

                    for (param in entry.wrapperFunction.parameters) {
                        if (param.kind == IrParameterKind.DispatchReceiver ||
                            param.kind == IrParameterKind.ExtensionReceiver
                        ) continue
                        val paramName = param.name.asString()

                        // Handle Compose-injected params
                        if (paramName.startsWith("\$")) {
                            when (paramName) {
                                "\$composer" -> {
                                    val myComposer = overrideFun.parameters.firstOrNull {
                                        it.name.asString() == "\$composer"
                                    }
                                    if (myComposer != null) {
                                        arguments[param] = irGet(myComposer)
                                    }
                                }
                                "\$changed" -> {
                                    arguments[param] = irInt(0)
                                }
                                "\$default" -> { /* Set after the loop */ }
                            }
                            continue
                        }

                        // Regular parameters
                        var argProvided = false
                        when (paramName) {
                            "scope" -> {
                                arguments[param] = irGet(scopeParam)
                                argProvided = true
                            }
                            "content" -> {
                                arguments[param] = irGet(contentParam)
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
                            defaultMask = defaultMask or (1 shl originalParamIndex)
                            arguments[param] = IrConstImpl.constNull(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                pluginContext.irBuiltIns.nothingNType,
                            )
                        }
                        originalParamIndex++
                    }

                    // Set $default bitmask
                    entry.wrapperFunction.parameters
                        .firstOrNull { it.name.asString() == "\$default" }
                        ?.let { dp -> arguments[dp] = irInt(defaultMask) }
                }

                whenExpr.branches += IrBranchImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    condition = irStringEquals(irGet(keyParam), irString(entry.key)),
                    result = wrapperCall,
                )
            }

            val elseResult = if (fallbackToContent) {
                generateContentInvoke(contentParam, overrideFun)
            } else {
                val errorFunction = symbolResolver.resolveFunctions("kotlin", "error")
                    .singleOrNull()
                    ?: error("Expected kotlin.error(String) to be available for tabs container dispatch")
                IrCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.nothingType,
                    errorFunction,
                    typeArgumentsCount = 0,
                    origin = null,
                ).apply {
                    arguments[0] = irString(
                        "Quo Vadis compiler plugin: no generated @TabsContainer wrapper dispatch " +
                            "matched the requested tab key. Ensure every @Tabs container has a matching wrapper.",
                    )
                }
            }
            whenExpr.branches += IrElseBranchImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                condition = irTrue(),
                result = elseResult,
            )

            +whenExpr
        }
    }

    // endregion

    // region Helpers

    /**
     * Generates `content()` invocation.
     * After Compose transformation, content becomes Function2<Composer, Int, Unit>,
     * so we must forward $composer and $changed.
     */
    private fun generateContentInvoke(
        contentParam: org.jetbrains.kotlin.ir.declarations.IrValueParameter,
        overrideFun: IrSimpleFunction,
    ): org.jetbrains.kotlin.ir.expressions.IrExpression {
        // After Compose transformation, content's type is Function2<Composer, Int, Unit>
        val composerParam = overrideFun.parameters.firstOrNull { it.name.asString() == "\$composer" }

        if (composerParam != null) {
            // Compose has transformed - content is Function2<Composer, Int, Unit>
            val function2Class = pluginContext.irBuiltIns.functionN(2)
            val invokeFun = function2Class.declarations
                .filterIsInstance<IrSimpleFunction>()
                .firstOrNull { it.name.asString() == "invoke" }
                ?: error("Expected function 'invoke' in Function2.")

            return IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                invokeFun.symbol,
                typeArgumentsCount = 3,
                origin = IrStatementOrigin.INVOKE,
            ).apply {
                // dispatch receiver = content
                arguments[invokeFun.parameters[0]] = IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    contentParam.type,
                    contentParam.symbol,
                )
                // arg1 = $composer
                arguments[invokeFun.parameters[1]] = IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    composerParam.type,
                    composerParam.symbol,
                )
                // arg2 = $changed
                val changedParam = overrideFun.parameters.firstOrNull { it.name.asString() == "\$changed" }
                arguments[invokeFun.parameters[2]] = if (changedParam != null) {
                    IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, changedParam.type, changedParam.symbol)
                } else {
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.intType, 0)
                }
            }
        } else {
            // No Compose transformation - use original Function0 path
            val function0Class = pluginContext.irBuiltIns.functionN(0)
            val invokeFun = function0Class.declarations
                .filterIsInstance<IrSimpleFunction>()
                .firstOrNull { it.name.asString() == "invoke" }
                ?: error("Expected function 'invoke' in Function0.")

            return IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.unitType,
                invokeFun.symbol,
                typeArgumentsCount = 1,
                origin = IrStatementOrigin.INVOKE,
            ).apply {
                arguments[invokeFun.parameters[0]] = IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    contentParam.type,
                    contentParam.symbol,
                )
            }
        }
    }

    /**
     * Generates an IR string equality comparison: `left == right`.
     */
    private fun irStringEquals(
        left: org.jetbrains.kotlin.ir.expressions.IrExpression,
        right: org.jetbrains.kotlin.ir.expressions.IrExpression,
    ): org.jetbrains.kotlin.ir.expressions.IrExpression {
        return IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            pluginContext.irBuiltIns.booleanType,
            pluginContext.irBuiltIns.eqeqSymbol,
            typeArgumentsCount = 0,
            origin = IrStatementOrigin.EQEQ,
        ).apply {
            arguments[0] = left
            arguments[1] = right
        }
    }

    // endregion

    // region Empty fallback

    private fun generateEmptyContainerRegistry(getter: IrSimpleFunction) {
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

    // endregion
}

/**
 * Holds the dispatch info for a single when-branch in TabsContainer/PaneContainer.
 */
private data class ContainerDispatchEntry(
    val key: String,
    val wrapperFunction: IrSimpleFunction,
)
