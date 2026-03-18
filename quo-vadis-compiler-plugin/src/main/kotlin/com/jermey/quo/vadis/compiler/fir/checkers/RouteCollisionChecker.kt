package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.stringArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Checks for duplicate routes within a @Stack sealed hierarchy.
 * When a @Stack class is encountered, iterates its sealed subclasses and
 * their @Destination routes to detect collisions.
 */
object RouteCollisionChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val STACK_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.STACK_FQN)
    private val DESTINATION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.DESTINATION_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        // Only check @Stack-annotated sealed classes
        declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == STACK_CLASS_ID
        } ?: return

        // Skip non-sealed classes — StructuralChecker already reports STACK_NOT_SEALED
        if (declaration.status.modality != Modality.SEALED) return

        // Get sealed subclasses (which should have @Destination)
        val sealedInheritors = declaration.getSealedClassInheritors(context.session)

        // Collect route -> list of inheritor symbols
        val routeMap = mutableMapOf<String, MutableList<FirRegularClassSymbol>>()

        for (inheritorClassId in sealedInheritors) {
            val inheritorSymbol = context.session
                .getRegularClassSymbolByClassId(inheritorClassId) ?: continue

            val destAnnotation = inheritorSymbol.resolvedAnnotationsWithClassIds.firstOrNull {
                it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
            } ?: continue

            val route = destAnnotation.stringArgument("route")
            if (route.isNullOrEmpty()) continue

            routeMap.getOrPut(route) { mutableListOf() }.add(inheritorSymbol)
        }

        // Report duplicates
        for ((route, symbols) in routeMap) {
            if (symbols.size > 1) {
                for (symbol in symbols) {
                    val destAnnotation = symbol.resolvedAnnotationsWithClassIds.firstOrNull {
                        it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
                    }
                    reporter.reportOn(
                        destAnnotation?.source ?: symbol.source,
                        QuoVadisDiagnostics.DUPLICATE_ROUTE,
                        route,
                        context,
                    )
                }
            }
        }
    }
}
