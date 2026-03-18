package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.classArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates @Screen functions reference valid @Destination classes
 * and have expected parameter types.
 * Reports ORPHAN_SCREEN when the destination lacks @Destination annotation,
 * and SCREEN_INVALID_PARAMS when parameters don't match the expected signature.
 */
object ScreenValidationChecker : FirDeclarationChecker<FirNamedFunction>(MppCheckerKind.Common) {

    private val SCREEN_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.SCREEN_FQN)
    private val DESTINATION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.DESTINATION_FQN)

    private val VALID_PARAM_SHORT_NAMES = setOf(
        "Navigator",
        "SharedTransitionScope",
        "AnimatedVisibilityScope",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val screenAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == SCREEN_CLASS_ID
        } ?: return

        // Get the destination class reference
        val destinationClassId = screenAnnotation.classArgument("destination") ?: return

        // Check if destination exists and has @Destination
        val destSymbol = context.session
            .getRegularClassSymbolByClassId(destinationClassId)

        if (destSymbol == null) {
            reporter.reportOn(
                screenAnnotation.source,
                QuoVadisDiagnostics.ORPHAN_SCREEN,
                destinationClassId.shortClassName.asString(),
                context,
            )
            return
        }

        val hasDestination = destSymbol.resolvedAnnotationsWithClassIds.any {
            it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
        }

        if (!hasDestination) {
            reporter.reportOn(
                screenAnnotation.source,
                QuoVadisDiagnostics.ORPHAN_SCREEN,
                destinationClassId.shortClassName.asString(),
                context,
            )
        }

        // Validate parameter types
        val destTypeName = destinationClassId.shortClassName.asString()

        for (param in declaration.valueParameters) {
            val typeName = param.returnTypeRef.coneType.classId?.shortClassName?.asString() ?: continue
            if (typeName != destTypeName && typeName !in VALID_PARAM_SHORT_NAMES) {
                reporter.reportOn(
                    screenAnnotation.source,
                    QuoVadisDiagnostics.SCREEN_INVALID_PARAMS,
                    context,
                )
                break
            }
        }
    }
}
