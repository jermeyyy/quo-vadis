@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir.validation

import com.jermey.quo.vadis.compiler.common.NormalizedPaneContainerBinding
import com.jermey.quo.vadis.compiler.common.NormalizedTabsContainerBinding
import com.jermey.quo.vadis.compiler.ir.SymbolResolver
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal data class ValidatedTabsContainerBinding(
    val tabClassId: ClassId,
    val scopeKey: String,
    val wrapperKey: String,
    val wrapperFunctionFqn: FqName,
    val wrapperFunction: IrSimpleFunction,
)

internal data class ValidatedPaneContainerBinding(
    val paneClassId: ClassId,
    val scopeKey: String,
    val wrapperKey: String,
    val wrapperFunctionFqn: FqName,
    val wrapperFunction: IrSimpleFunction,
)

internal data class ValidatedContainerBindings(
    val tabsBindings: List<ValidatedTabsContainerBinding>,
    val paneBindings: List<ValidatedPaneContainerBinding>,
)

internal class ContainerWrapperValidator(
    private val symbolResolver: SymbolResolver,
    private val moduleFragment: IrModuleFragment? = null,
) {
    fun validate(
        tabsBindings: List<NormalizedTabsContainerBinding>,
        paneBindings: List<NormalizedPaneContainerBinding>,
    ): ValidatedContainerBindings {
        return ValidatedContainerBindings(
            tabsBindings = tabsBindings.map { binding ->
                val wrapperFunction = validateWrapperBinding(
                    annotationName = "@TabsContainer",
                    containerClassId = binding.tabClassId,
                    wrapperKey = binding.wrapperKey,
                    wrapperFunctionFqn = binding.wrapperFunctionFqn,
                    expectedScopeClass = symbolResolver.tabsContainerScopeClass,
                )
                ValidatedTabsContainerBinding(
                    tabClassId = binding.tabClassId,
                    scopeKey = binding.scopeKey,
                    wrapperKey = binding.wrapperKey,
                    wrapperFunctionFqn = binding.wrapperFunctionFqn,
                    wrapperFunction = wrapperFunction,
                )
            },
            paneBindings = paneBindings.map { binding ->
                val wrapperFunction = validateWrapperBinding(
                    annotationName = "@PaneContainer",
                    containerClassId = binding.paneClassId,
                    wrapperKey = binding.wrapperKey,
                    wrapperFunctionFqn = binding.wrapperFunctionFqn,
                    expectedScopeClass = symbolResolver.paneContainerScopeClass,
                )
                ValidatedPaneContainerBinding(
                    paneClassId = binding.paneClassId,
                    scopeKey = binding.scopeKey,
                    wrapperKey = binding.wrapperKey,
                    wrapperFunctionFqn = binding.wrapperFunctionFqn,
                    wrapperFunction = wrapperFunction,
                )
            },
        )
    }

    private fun validateWrapperBinding(
        annotationName: String,
        containerClassId: ClassId,
        wrapperKey: String,
        wrapperFunctionFqn: FqName,
        expectedScopeClass: IrClassSymbol,
    ): IrSimpleFunction {
        val wrapperFunction = resolveWrapperFunction(wrapperFunctionFqn)
            ?: error(
                "Quo Vadis compiler plugin: declared $annotationName wrapper ${wrapperFunctionFqn.asString()} " +
                    "for ${containerClassId.asSingleFqName().asString()} could not be resolved during validation.",
            )

        val wrapperDescription = "$annotationName wrapper ${wrapperFunctionFqn.asString()}"
        val containerDescription = containerClassId.asSingleFqName().asString()

        if (!wrapperFunction.isComposable(symbolResolver)) {
            error(
                "Quo Vadis compiler plugin: $wrapperDescription for $containerDescription must be annotated with @Composable.",
            )
        }

        val regularParameters = wrapperFunction.parameters.filter { it.kind == IrParameterKind.Regular }
        val userVisibleRegularParameters = regularParameters.filterNot { it.isComposeSyntheticParameter() }

        val scopeParameter = userVisibleRegularParameters.firstOrNull { it.name.asString() == "scope" }
            ?: error(
                "Quo Vadis compiler plugin: $wrapperDescription for $containerDescription must declare " +
                    "parameter 'scope: ${expectedScopeClass.owner.name.asString()}'.",
            )
        if (scopeParameter.type.classOrNull != expectedScopeClass) {
            error(
                "Quo Vadis compiler plugin: $wrapperDescription for $containerDescription must declare " +
                    "parameter 'scope: ${expectedScopeClass.owner.name.asString()}'.",
            )
        }

        userVisibleRegularParameters.firstOrNull { it.name.asString() == "content" }
            ?: error(
                "Quo Vadis compiler plugin: $wrapperDescription for $containerDescription must declare " +
                    "parameter 'content'.",
            )

        val unsupportedParameters = userVisibleRegularParameters.filter { parameter ->
            val parameterName = parameter.name.asString()
            parameterName !in setOf("scope", "content") &&
                parameter.defaultValue == null
        }
        if (unsupportedParameters.isNotEmpty()) {
            val parameterNames = unsupportedParameters.joinToString { it.name.asString() }
            error(
                "Quo Vadis compiler plugin: $wrapperDescription for $containerDescription has unsupported required " +
                    "parameter(s): $parameterNames. Only 'scope', 'content', and parameters with defaults are supported.",
            )
        }

        return wrapperFunction
    }

    private fun resolveWrapperFunction(functionFqn: FqName): IrSimpleFunction? {
        val packageFqn = functionFqn.parent().asString()
        val functionName = functionFqn.shortName().asString()

        if (moduleFragment != null) {
            for (file in moduleFragment.files) {
                if (file.packageFqName.asString() != packageFqn) continue
                for (declaration in file.declarations) {
                    if (declaration is IrSimpleFunction && declaration.name.asString() == functionName) {
                        return declaration
                    }
                }
            }
        }

        return symbolResolver.resolveFunctions(packageFqn, functionName).firstOrNull()?.owner
    }
}

private fun IrSimpleFunction.isComposable(symbolResolver: SymbolResolver): Boolean {
    val composableAnnotation = symbolResolver.composableAnnotation ?: return false
    return annotations.any { it.type.classOrNull == composableAnnotation } ||
        parameters.any { it.name.asString() == "\$composer" }
}

private fun org.jetbrains.kotlin.ir.declarations.IrValueParameter.isComposeSyntheticParameter(): Boolean {
    val parameterName = name.asString()
    if (parameterName.startsWith("$")) {
        return true
    }

    return parameterName.contains("composer", ignoreCase = true) ||
        parameterName.startsWith("changed", ignoreCase = true) ||
        parameterName.startsWith("default", ignoreCase = true)
}