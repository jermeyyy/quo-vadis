package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates structural constraints:
 * - @Stack must be on a sealed class or sealed interface
 * - @Destination must be a direct subclass of a @Stack-annotated sealed class
 */
object StructuralChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val STACK_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.STACK_FQN)
    private val DESTINATION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.DESTINATION_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        checkStackSealed(declaration)
        checkDestinationInStack(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStackSealed(declaration: FirRegularClass) {
        val stackAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == STACK_CLASS_ID
        } ?: return

        if (declaration.status.modality != Modality.SEALED) {
            reporter.reportOn(
                stackAnnotation.source,
                QuoVadisDiagnostics.STACK_NOT_SEALED,
                context,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDestinationInStack(declaration: FirRegularClass) {
        val destAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
        } ?: return

        // Check that at least one supertype has @Stack
        val isInStack = declaration.superTypeRefs.any { superTypeRef ->
            val superClassId = superTypeRef.coneType.classId ?: return@any false
            val superSymbol = context.session
                .getRegularClassSymbolByClassId(superClassId) ?: return@any false
            superSymbol.resolvedAnnotationsWithClassIds.any {
                it.toAnnotationClassId(context.session) == STACK_CLASS_ID
            }
        }

        if (!isInStack) {
            reporter.reportOn(
                destAnnotation.source,
                QuoVadisDiagnostics.DESTINATION_NOT_IN_STACK,
                context,
            )
        }
    }
}
