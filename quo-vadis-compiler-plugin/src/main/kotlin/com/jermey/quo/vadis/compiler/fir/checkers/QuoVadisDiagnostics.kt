package com.jermey.quo.vadis.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.diagnostics.warning2
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement

object QuoVadisDiagnostics : KtDiagnosticsContainer() {

    // Errors
    val DUPLICATE_ROUTE by error1<KtAnnotationEntry, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val ARGUMENT_ROUTE_MISMATCH by error2<KtAnnotationEntry, String, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val MISSING_ROUTE_ARGUMENT by error2<KtAnnotationEntry, String, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val MISSING_PRIMARY_PANE by error1<KtAnnotationEntry, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val DUPLICATE_PANE_ROLE by error2<KtAnnotationEntry, String, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val STACK_NOT_SEALED by error0<KtAnnotationEntry>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val DESTINATION_NOT_IN_STACK by error0<KtAnnotationEntry>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val MULTIPLE_NAVIGATION_ROOTS by error0<KtAnnotationEntry>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val NAVIGATION_ROOT_REQUIRED by error0<KtElement>(
        SourceElementPositioningStrategies.DEFAULT,
    )

    // Warnings
    val INCOMPATIBLE_TRANSITION by warning2<KtAnnotationEntry, String, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val ORPHAN_SCREEN by warning1<KtAnnotationEntry, String>(
        SourceElementPositioningStrategies.DEFAULT,
    )
    val SCREEN_INVALID_PARAMS by warning0<KtAnnotationEntry>(
        SourceElementPositioningStrategies.DEFAULT,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = QuoVadisDiagnosticRenderer
}

object QuoVadisDiagnosticRenderer : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("QuoVadis") {
        it.put(
            QuoVadisDiagnostics.DUPLICATE_ROUTE,
            "Duplicate route \"{0}\" detected. Routes must be unique within a module.",
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.ARGUMENT_ROUTE_MISMATCH,
            "@Argument property \"{0}\" does not match any placeholder in route \"{1}\".",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.MISSING_ROUTE_ARGUMENT,
            "Route placeholder \"{0}\" has no matching @Argument property in route \"{1}\".",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.MISSING_PRIMARY_PANE,
            "@Pane \"{0}\" must have exactly one @PaneItem with role = PaneRole.PRIMARY.",
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.DUPLICATE_PANE_ROLE,
            "@Pane \"{0}\" has multiple @PaneItem entries with role = {1}.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.STACK_NOT_SEALED,
            "@Stack must be applied to a sealed class or sealed interface.",
        )
        it.put(
            QuoVadisDiagnostics.DESTINATION_NOT_IN_STACK,
            "@Destination must be a direct subclass of a @Stack, @Tabs, or @Pane-annotated sealed class.",
        )
        it.put(
            QuoVadisDiagnostics.MULTIPLE_NAVIGATION_ROOTS,
            "Multiple @NavigationRoot annotations found in this module. " +
                "Only one @NavigationRoot is allowed per compilation unit.",
        )
        it.put(
            QuoVadisDiagnostics.NAVIGATION_ROOT_REQUIRED,
            "Type argument T of navigationConfig<T>() must be annotated with @NavigationRoot.",
        )
        it.put(
            QuoVadisDiagnostics.INCOMPATIBLE_TRANSITION,
            "Transition {0} may not work correctly within a {1} container.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.ORPHAN_SCREEN,
            "@Screen references destination {0} which has no @Destination annotation.",
            CommonRenderers.STRING,
        )
        it.put(
            QuoVadisDiagnostics.SCREEN_INVALID_PARAMS,
            "@Screen function has unexpected parameter types. " +
                "Expected: destination, Navigator, SharedTransitionScope?, AnimatedVisibilityScope?",
        )
    }
}
