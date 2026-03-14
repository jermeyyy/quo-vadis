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
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Validates structural constraints:
 * - @Stack must be on a sealed class or sealed interface
 * - @Destination inside a sealed class must have a container annotation (@Stack, @Tabs, or @Pane)
 *   on the parent sealed class. Standalone destinations (extending NavDestination directly) are allowed.
 */
object StructuralChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val STACK_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.STACK_FQN)
    private val DESTINATION_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.DESTINATION_FQN)
    private val TABS_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.TABS_FQN)
    private val PANE_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.PANE_FQN)

    private val NAV_DESTINATION_CLASS_ID = ClassId.topLevel(
        FqName("com.jermey.quo.vadis.core.navigation.destination.NavDestination")
    )

    /** Annotations that mark a sealed class as a valid container for @Destination subclasses. */
    private val CONTAINER_ANNOTATIONS = setOf(STACK_CLASS_ID, TABS_CLASS_ID, PANE_CLASS_ID)

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

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDestinationInStack(declaration: FirRegularClass) {
        val destAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == DESTINATION_CLASS_ID
        } ?: return

        // Check each supertype: if it's a sealed class, it MUST have a container annotation.
        // If no supertype is a sealed class (standalone destination), skip the check.
        var hasSealedParent = false
        var isInContainer = false

        for (superTypeRef in declaration.superTypeRefs) {
            val superClassId = superTypeRef.coneType.classId ?: continue
            // Skip NavDestination itself — it's the base interface, not a container
            if (superClassId == NAV_DESTINATION_CLASS_ID) continue

            val superSymbol = context.session
                .getRegularClassSymbolByClassId(superClassId) ?: continue

            if (superSymbol.resolvedStatus.modality == Modality.SEALED) {
                hasSealedParent = true
                // Access annotations via the FIR element directly for reliable resolution
                val hasContainerAnnotation = superSymbol.fir.annotations.any {
                    it.toAnnotationClassId(context.session) in CONTAINER_ANNOTATIONS
                }
                if (hasContainerAnnotation) {
                    isInContainer = true
                    break
                }
            }
        }

        // Only report if the destination IS inside a sealed hierarchy but the parent lacks
        // a container annotation. Standalone destinations (no sealed parent) are allowed.
        if (hasSealedParent && !isInContainer) {
            reporter.reportOn(
                destAnnotation.source,
                QuoVadisDiagnostics.DESTINATION_NOT_IN_STACK,
                context,
            )
        }
    }
}
