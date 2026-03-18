package com.jermey.quo.vadis.compiler.fir.checkers

import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.classArgument
import com.jermey.quo.vadis.compiler.fir.AnnotationExtractor.intArgument
import com.jermey.quo.vadis.compiler.fir.QuoVadisPredicates
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
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
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Validates @TabItem ordinal and parent constraints within @Tabs containers.
 *
 * When a @Tabs-annotated sealed class is encountered, collects all sealed subclasses
 * with @TabItem and validates:
 * - At least one ordinal = 0 exists (TABITEM_MISSING_ORDINAL_ZERO)
 * - No duplicate ordinals (TABITEM_DUPLICATE_ORDINAL)
 * - Ordinals are contiguous 0..N-1 (TABITEM_ORDINAL_GAP)
 * - @TabItem parent references a @Tabs-annotated class (TABITEM_INVALID_PARENT)
 */
object TabItemChecker : FirDeclarationChecker<FirClass>(MppCheckerKind.Common) {

    private val TABS_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.TABS_FQN)
    private val TAB_ITEM_CLASS_ID = ClassId.topLevel(QuoVadisPredicates.TAB_ITEM_FQN)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration !is FirRegularClass) return

        val tabsAnnotation = declaration.annotations.firstOrNull {
            it.toAnnotationClassId(context.session) == TABS_CLASS_ID
        } ?: return

        // Only sealed @Tabs classes can be validated via inheritor scanning.
        // Non-sealed @Tabs (e.g. annotation classes used as cross-module parents)
        // are validated at the KSP/IR level instead.
        if (declaration.classKind != ClassKind.CLASS ||
            declaration.status.modality != Modality.SEALED
        ) {
            return
        }

        val sealedInheritors = declaration.getSealedClassInheritors(context.session)
        val tabItems = mutableListOf<TabItemInfo>()

        for (inheritorClassId in sealedInheritors) {
            val inheritorSymbol = context.session
                .getRegularClassSymbolByClassId(inheritorClassId) ?: continue

            val tabItemAnnotation = inheritorSymbol.resolvedAnnotationsWithClassIds.firstOrNull {
                it.toAnnotationClassId(context.session) == TAB_ITEM_CLASS_ID
            } ?: continue

            val ordinal = tabItemAnnotation.intArgument("ordinal") ?: 0
            val parentClassId = tabItemAnnotation.classArgument("parent")

            tabItems.add(
                TabItemInfo(
                    ordinal = ordinal,
                    parentClassId = parentClassId,
                    symbol = inheritorSymbol,
                ),
            )
        }

        if (tabItems.isEmpty()) return

        validateParentReferences(tabItems)
        validateOrdinals(tabItems, tabsAnnotation)
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validateParentReferences(tabItems: List<TabItemInfo>) {
        for (item in tabItems) {
            val parentClassId = item.parentClassId ?: continue
            val parentSymbol = context.session
                .getRegularClassSymbolByClassId(parentClassId) ?: continue

            val hasTabsAnnotation = parentSymbol.fir.annotations.any {
                it.toAnnotationClassId(context.session) == TABS_CLASS_ID
            }
            if (!hasTabsAnnotation) {
                val tabItemAnnotation = item.symbol.resolvedAnnotationsWithClassIds.firstOrNull {
                    it.toAnnotationClassId(context.session) == TAB_ITEM_CLASS_ID
                }
                reporter.reportOn(
                    tabItemAnnotation?.source ?: item.symbol.source,
                    QuoVadisDiagnostics.TABITEM_INVALID_PARENT,
                    context,
                )
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun validateOrdinals(
        tabItems: List<TabItemInfo>,
        tabsAnnotation: org.jetbrains.kotlin.fir.expressions.FirAnnotation,
    ) {
        val ordinals = tabItems.map { it.ordinal }

        // Check: must have ordinal = 0
        if (0 !in ordinals) {
            reporter.reportOn(
                tabsAnnotation.source,
                QuoVadisDiagnostics.TABITEM_MISSING_ORDINAL_ZERO,
                context,
            )
        }

        // Check: no duplicate ordinals
        val ordinalGroups = tabItems.groupBy { it.ordinal }
        for ((ordinal, items) in ordinalGroups) {
            if (items.size > 1) {
                for (item in items) {
                    val tabItemAnnotation = item.symbol.resolvedAnnotationsWithClassIds.firstOrNull {
                        it.toAnnotationClassId(context.session) == TAB_ITEM_CLASS_ID
                    }
                    reporter.reportOn(
                        tabItemAnnotation?.source ?: item.symbol.source,
                        QuoVadisDiagnostics.TABITEM_DUPLICATE_ORDINAL,
                        ordinal.toString(),
                        context,
                    )
                }
            }
        }

        // Check: ordinals are contiguous 0..N-1
        val maxOrdinal = ordinals.max()
        val expected = (0..maxOrdinal).toSet()
        val actual = ordinals.toSet()
        val missing = expected - actual
        if (missing.isNotEmpty()) {
            reporter.reportOn(
                tabsAnnotation.source,
                QuoVadisDiagnostics.TABITEM_ORDINAL_GAP,
                missing.sorted().joinToString(", "),
                context,
            )
        }
    }

    private data class TabItemInfo(
        val ordinal: Int,
        val parentClassId: ClassId?,
        val symbol: FirRegularClassSymbol,
    )
}
