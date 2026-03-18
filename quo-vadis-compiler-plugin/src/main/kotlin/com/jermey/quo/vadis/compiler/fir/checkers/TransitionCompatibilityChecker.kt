package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.enumArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

/**
 * Warns about potentially incompatible transition types for Tab and Pane containers.
 * Reports INCOMPATIBLE_TRANSITION when a transition type is unusual for the parent container.
 */
object TransitionCompatibilityChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val TRANSITION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.TRANSITION_FQN)
    private val TABS_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.TABS_FQN)
    private val PANE_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.PANE_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        val transitionAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == TRANSITION_CLASS_ID
        } ?: return

        val transitionType = transitionAnnotation.enumArgument("type") ?: return

        // Check supertypes for @Tabs or @Pane
        for (superTypeRef in declaration.superTypeRefs) {
            val superClassId = superTypeRef.coneType.classId ?: continue
            val superSymbol = context.session
                .getRegularClassSymbolByClassId(superClassId) ?: continue

            val hasTabs = superSymbol.resolvedAnnotationsWithClassIds.any {
                it.toAnnotationClassId(context.session) == TABS_CLASS_ID
            }
            val hasPane = superSymbol.resolvedAnnotationsWithClassIds.any {
                it.toAnnotationClassId(context.session) == PANE_CLASS_ID
            }

            if (hasTabs && transitionType == "SlideVertical") {
                reporter.reportOn(
                    transitionAnnotation.source,
                    QuoVadisDiagnostics.INCOMPATIBLE_TRANSITION,
                    transitionType,
                    "Tabs",
                    context,
                )
            }

            if (hasPane && (transitionType == "SlideHorizontal" || transitionType == "SlideVertical")) {
                reporter.reportOn(
                    transitionAnnotation.source,
                    QuoVadisDiagnostics.INCOMPATIBLE_TRANSITION,
                    transitionType,
                    "Pane",
                    context,
                )
            }
        }
    }
}
