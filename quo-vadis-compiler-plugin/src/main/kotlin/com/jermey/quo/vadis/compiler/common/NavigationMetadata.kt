package com.jermey.quo.vadis.compiler.common

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

data class NavigationMetadata(
    val modulePrefix: String,
    val stacks: List<StackMetadata> = emptyList(),
    val tabs: List<TabsMetadata> = emptyList(),
    val panes: List<PaneMetadata> = emptyList(),
    val routableDestinations: List<DestinationMetadata> = emptyList(),
    val screens: List<ScreenMetadata> = emptyList(),
    val tabsContainers: List<TabsContainerMetadata> = emptyList(),
    val paneContainers: List<PaneContainerMetadata> = emptyList(),
    val transitions: List<TransitionMetadata> = emptyList(),
) {
    val isEmpty: Boolean get() = stacks.isEmpty() && tabs.isEmpty() && panes.isEmpty() &&
        routableDestinations.isEmpty() && screens.isEmpty() && tabsContainers.isEmpty() && paneContainers.isEmpty() &&
        transitions.isEmpty()
}

data class StackMetadata(
    val name: String,
    val startDestination: ClassId,
    val sealedClassId: ClassId,
    val destinations: List<DestinationMetadata> = emptyList(),
)

data class DestinationMetadata(
    val classId: ClassId,
    val route: String? = null,
    val arguments: List<ArgumentMetadata> = emptyList(),
    val constructorParameters: List<ConstructorParameterMetadata> = emptyList(),
    val transitionType: TransitionType? = null,
    val isSealedClass: Boolean = false,
)

data class ArgumentMetadata(
    val name: String,
    val key: String,
    val type: ArgumentType,
    val optional: Boolean = false,
)

data class ConstructorParameterMetadata(
    val name: String,
    val key: String,
    val type: ArgumentType,
    val hasDefault: Boolean = false,
    val isArgument: Boolean = false,
    val optional: Boolean = false,
    val nullable: Boolean = false,
)

enum class ArgumentType {
    STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN, ENUM,
}

enum class TransitionType {
    SLIDE_HORIZONTAL, SLIDE_VERTICAL, FADE, NONE, CUSTOM,
}

data class TabsMetadata(
    val name: String,
    val classId: ClassId,
    val initialTab: ClassId? = null,
    val items: List<TabItemMetadata> = emptyList(),
    val allDestinationClassIds: List<ClassId> = emptyList(),
)

data class TabItemMetadata(
    val classId: ClassId,
    val type: TabItemType,
)

enum class TabItemType {
    NESTED_STACK, FLAT_SCREEN,
}

data class PaneMetadata(
    val name: String,
    val classId: ClassId,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.POP_UNTIL_SCAFFOLD_VALUE_CHANGE,
    val items: List<PaneItemMetadata> = emptyList(),
    val allDestinationClassIds: List<ClassId> = emptyList(),
)

data class PaneItemMetadata(
    val classId: ClassId,
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy = AdaptStrategy.HIDE,
)

enum class PaneBackBehavior {
    POP_UNTIL_SCAFFOLD_VALUE_CHANGE, POP_UNTIL_CONTENT_CHANGE, POP_LATEST,
}

enum class PaneRole {
    PRIMARY, SECONDARY, EXTRA,
}

enum class AdaptStrategy {
    HIDE, SHOW_AS_DIALOG, OVERLAY,
}

data class ScreenMetadata(
    val functionFqn: FqName,
    val destinationClassId: ClassId,
    val hasDestinationParam: Boolean = false,
    val hasSharedTransitionScope: Boolean = false,
    val hasAnimatedVisibilityScope: Boolean = false,
)

data class TabsContainerMetadata(
    val functionFqn: FqName,
    val tabClassId: ClassId,
)

data class PaneContainerMetadata(
    val functionFqn: FqName,
    val paneClassId: ClassId,
)

data class TransitionMetadata(
    val destinationClassId: ClassId,
    val type: TransitionType,
    val customClass: ClassId? = null,
)
