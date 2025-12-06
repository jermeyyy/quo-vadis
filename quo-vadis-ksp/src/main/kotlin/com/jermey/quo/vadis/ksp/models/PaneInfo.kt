package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Pane role in adaptive layouts (mirrors annotation enum).
 */
enum class PaneRole {
    PRIMARY,
    SECONDARY,
    EXTRA
}

/**
 * Adaptation strategy when screen space is limited (mirrors annotation enum).
 */
enum class AdaptStrategy {
    HIDE,
    COLLAPSE,
    OVERLAY,
    REFLOW
}

/**
 * Back navigation behavior for pane containers (mirrors annotation enum).
 */
enum class PaneBackBehavior {
    PopUntilScaffoldValueChange,
    PopUntilContentChange,
    PopLatest
}

/**
 * Extracted metadata from a @Pane annotation.
 *
 * @property classDeclaration The KSP class declaration for this pane container
 * @property name Pane container identifier from annotation
 * @property className Simple class name
 * @property packageName Package containing this class
 * @property backBehavior Back navigation behavior for panes
 * @property panes List of all @PaneItem subclasses within this sealed class
 */
data class PaneInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val backBehavior: PaneBackBehavior,
    val panes: List<PaneItemInfo>
)

/**
 * Extracted metadata from a @PaneItem annotation.
 *
 * @property destination The destination info for this pane
 * @property role The pane role (PRIMARY, SECONDARY, EXTRA)
 * @property adaptStrategy Strategy when pane space is limited (HIDE, COLLAPSE, OVERLAY, REFLOW)
 * @property rootGraphClass Class declaration for the root graph of this pane
 */
data class PaneItemInfo(
    val destination: DestinationInfo,
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy,
    val rootGraphClass: KSClassDeclaration
)
