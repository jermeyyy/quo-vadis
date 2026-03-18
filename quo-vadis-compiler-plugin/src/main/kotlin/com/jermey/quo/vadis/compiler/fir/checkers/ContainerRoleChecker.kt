package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.enumArgument
import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.stringArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates @Pane containers have valid @PaneItem role configurations.
 * Reports MISSING_PRIMARY_PANE when no PRIMARY role is found,
 * and DUPLICATE_PANE_ROLE when the same role appears multiple times.
 */
object ContainerRoleChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val PANE_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.PANE_FQN)
    private val PANE_ITEM_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.PANE_ITEM_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        val paneAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == PANE_CLASS_ID
        } ?: return

        val paneName = paneAnnotation.stringArgument("name") ?: "unknown"

        // Get sealed subclasses with @PaneItem
        val sealedInheritors = declaration.getSealedClassInheritors(context.session)
        val roleList = mutableListOf<Pair<String, FirRegularClassSymbol>>()

        for (inheritorClassId in sealedInheritors) {
            val inheritorSymbol = context.session
                .getRegularClassSymbolByClassId(inheritorClassId) ?: continue

            val paneItemAnnotation = inheritorSymbol.resolvedAnnotationsWithClassIds.firstOrNull {
                it.toAnnotationClassId(context.session) == PANE_ITEM_CLASS_ID
            } ?: continue

            val role = paneItemAnnotation.enumArgument("role") ?: "PRIMARY"
            roleList.add(role to inheritorSymbol)
        }

        // Check: must have exactly one PRIMARY
        val primaryItems = roleList.filter { it.first == "PRIMARY" }
        if (primaryItems.isEmpty()) {
            reporter.reportOn(
                paneAnnotation.source,
                QuoVadisDiagnostics.MISSING_PRIMARY_PANE,
                paneName,
                context,
            )
        }

        // Check: no duplicate roles
        val roleGroups = roleList.groupBy { it.first }
        for ((role, items) in roleGroups) {
            if (items.size > 1) {
                reporter.reportOn(
                    paneAnnotation.source,
                    QuoVadisDiagnostics.DUPLICATE_PANE_ROLE,
                    paneName,
                    role,
                    context,
                )
            }
        }
    }
}
