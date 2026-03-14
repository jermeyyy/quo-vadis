package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates that at most one class is annotated with `@NavigationRoot` per module.
 *
 * When multiple `@NavigationRoot` classes are found, reports
 * [QuoVadisDiagnostics.MULTIPLE_NAVIGATION_ROOTS] on each occurrence.
 */
object NavigationRootUniquenessChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val NAVIGATION_ROOT_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.NAVIGATION_ROOT_FQN)

    val NAVIGATION_ROOT_LOOKUP = LookupPredicate.create {
        annotated(QuoVadisPredicates.NAVIGATION_ROOT_FQN)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        // Only trigger on @NavigationRoot-annotated classes
        val rootAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == NAVIGATION_ROOT_CLASS_ID
        } ?: return

        // Query all @NavigationRoot symbols in the module via the predicate-based provider
        val allRoots = context.session.predicateBasedProvider
            .getSymbolsByPredicate(NAVIGATION_ROOT_LOOKUP)

        if (allRoots.size > 1) {
            reporter.reportOn(
                rootAnnotation.source,
                QuoVadisDiagnostics.MULTIPLE_NAVIGATION_ROOTS,
                context,
            )
        }
    }
}
