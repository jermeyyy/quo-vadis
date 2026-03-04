package com.jermey.quo.vadis.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.name.FqName

object QuoVadisPredicates {
    private const val ANNOTATIONS_PACKAGE = "com.jermey.quo.vadis.annotations"

    val STACK_FQN = FqName("$ANNOTATIONS_PACKAGE.Stack")
    val DESTINATION_FQN = FqName("$ANNOTATIONS_PACKAGE.Destination")
    val SCREEN_FQN = FqName("$ANNOTATIONS_PACKAGE.Screen")
    val TABS_FQN = FqName("$ANNOTATIONS_PACKAGE.Tabs")
    val TAB_ITEM_FQN = FqName("$ANNOTATIONS_PACKAGE.TabItem")
    val PANE_FQN = FqName("$ANNOTATIONS_PACKAGE.Pane")
    val PANE_ITEM_FQN = FqName("$ANNOTATIONS_PACKAGE.PaneItem")
    val TABS_CONTAINER_FQN = FqName("$ANNOTATIONS_PACKAGE.TabsContainer")
    val PANE_CONTAINER_FQN = FqName("$ANNOTATIONS_PACKAGE.PaneContainer")
    val TRANSITION_FQN = FqName("$ANNOTATIONS_PACKAGE.Transition")
    val ARGUMENT_FQN = FqName("$ANNOTATIONS_PACKAGE.Argument")
    val NAVIGATION_ROOT_FQN = FqName("$ANNOTATIONS_PACKAGE.NavigationRoot")

    val HAS_STACK = DeclarationPredicate.create { annotated(STACK_FQN) }
    val HAS_DESTINATION = DeclarationPredicate.create { annotated(DESTINATION_FQN) }
    val HAS_SCREEN = DeclarationPredicate.create { annotated(SCREEN_FQN) }
    val HAS_TABS = DeclarationPredicate.create { annotated(TABS_FQN) }
    val HAS_TAB_ITEM = DeclarationPredicate.create { annotated(TAB_ITEM_FQN) }
    val HAS_PANE = DeclarationPredicate.create { annotated(PANE_FQN) }
    val HAS_PANE_ITEM = DeclarationPredicate.create { annotated(PANE_ITEM_FQN) }
    val HAS_TABS_CONTAINER = DeclarationPredicate.create { annotated(TABS_CONTAINER_FQN) }
    val HAS_PANE_CONTAINER = DeclarationPredicate.create { annotated(PANE_CONTAINER_FQN) }
    val HAS_TRANSITION = DeclarationPredicate.create { annotated(TRANSITION_FQN) }
    val HAS_ARGUMENT = DeclarationPredicate.create { annotated(ARGUMENT_FQN) }
    val HAS_NAVIGATION_ROOT = DeclarationPredicate.create { annotated(NAVIGATION_ROOT_FQN) }

    val ALL_PREDICATES = listOf(
        HAS_STACK, HAS_DESTINATION, HAS_SCREEN,
        HAS_TABS, HAS_TAB_ITEM,
        HAS_PANE, HAS_PANE_ITEM,
        HAS_TABS_CONTAINER, HAS_PANE_CONTAINER,
        HAS_TRANSITION, HAS_ARGUMENT,
        HAS_NAVIGATION_ROOT
    )
}
