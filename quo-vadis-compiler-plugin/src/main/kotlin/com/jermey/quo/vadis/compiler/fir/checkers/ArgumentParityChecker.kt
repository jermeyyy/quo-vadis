package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.stringArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates that @Argument properties match route placeholders and vice versa.
 * Reports ARGUMENT_ROUTE_MISMATCH when an @Argument property has no matching placeholder,
 * and MISSING_ROUTE_ARGUMENT when a route placeholder has no matching @Argument property.
 */
object ArgumentParityChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val DESTINATION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.DESTINATION_FQN)
    private val ARGUMENT_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.ARGUMENT_FQN)
    private val PLACEHOLDER_REGEX = Regex("\\{(\\w+)}")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        // Only check @Destination classes
        val destAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
        } ?: return

        val route = destAnnotation.stringArgument("route")
        if (route.isNullOrEmpty()) return

        // Extract placeholders from route
        val placeholders = PLACEHOLDER_REGEX.findAll(route).map { it.groupValues[1] }.toSet()

        // Get @Argument-annotated constructor parameters
        val argumentKeys = mutableSetOf<String>()
        val primaryCtor = declaration.primaryConstructorIfAny(context.session)

        primaryCtor?.valueParameterSymbols?.forEach { paramSymbol ->
            val argAnnotation = paramSymbol.resolvedAnnotationsWithClassIds.firstOrNull {
                it.toAnnotationClassId(context.session) == ARGUMENT_CLASS_ID
            } ?: return@forEach

            val key = argAnnotation.stringArgument("key")?.takeIf { it.isNotEmpty() }
                ?: paramSymbol.name.asString()
            argumentKeys.add(key)
        }

        // Check for @Argument properties not in route
        for (key in argumentKeys) {
            if (key !in placeholders) {
                reporter.reportOn(
                    destAnnotation.source,
                    QuoVadisDiagnostics.ARGUMENT_ROUTE_MISMATCH,
                    key,
                    route,
                    context,
                )
            }
        }

        // Check for route placeholders without @Argument or matching constructor param
        // (backward-compatible with KSP's name-based matching)
        val allConstructorParamNames = primaryCtor?.valueParameterSymbols
            ?.map { it.name.asString() }?.toSet() ?: emptySet()

        for (placeholder in placeholders) {
            if (placeholder !in argumentKeys && placeholder !in allConstructorParamNames) {
                reporter.reportOn(
                    destAnnotation.source,
                    QuoVadisDiagnostics.MISSING_ROUTE_ARGUMENT,
                    placeholder,
                    route,
                    context,
                )
            }
        }
    }
}
