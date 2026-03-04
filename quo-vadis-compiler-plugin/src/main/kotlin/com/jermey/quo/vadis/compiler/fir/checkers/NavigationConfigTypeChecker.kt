package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Validates that `navigationConfig<T>()` is called with a type argument `T`
 * that is annotated with `@NavigationRoot`.
 *
 * Reports [QuoVadisDiagnostics.NAVIGATION_ROOT_REQUIRED] when the type argument
 * lacks the `@NavigationRoot` annotation.
 */
object NavigationConfigTypeChecker : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {

    private val NAVIGATION_CONFIG_CALLABLE_ID = CallableId(
        FqName("com.jermey.quo.vadis.core.navigation.config"),
        Name.identifier("navigationConfig"),
    )

    private val NAVIGATION_ROOT_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.NAVIGATION_ROOT_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        // Match only calls to navigationConfig<T>()
        val callee = expression.calleeReference as? FirResolvedNamedReference ?: return
        val symbol = callee.resolvedSymbol as? FirNamedFunctionSymbol ?: return
        if (symbol.callableId != NAVIGATION_CONFIG_CALLABLE_ID) return

        // Extract the type argument
        val typeProjection = expression.typeArguments.firstOrNull()
            as? FirTypeProjectionWithVariance ?: return
        val typeClassId = typeProjection.typeRef.coneType.classId ?: return

        // Resolve the type argument class and check for @NavigationRoot
        val classSymbol = context.session
            .getRegularClassSymbolByClassId(typeClassId) ?: return

        val hasNavigationRoot = classSymbol.resolvedAnnotationsWithClassIds.any {
            it.toAnnotationClassId(context.session) == NAVIGATION_ROOT_CLASS_ID
        }

        if (!hasNavigationRoot) {
            reporter.reportOn(
                expression.source,
                QuoVadisDiagnostics.NAVIGATION_ROOT_REQUIRED,
                context,
            )
        }
    }
}
