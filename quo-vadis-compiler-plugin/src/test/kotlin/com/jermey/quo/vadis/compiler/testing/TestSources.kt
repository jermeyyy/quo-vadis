package com.jermey.quo.vadis.compiler.testing

import com.tschuchort.compiletesting.SourceFile

/**
 * Reusable test source snippets covering all annotation patterns
 * for the Quo Vadis compiler plugin test suite.
 */
object TestSources {

    // region 1. Basic Stack

    /**
     * A sealed class with @Stack containing a data object Home and data class Detail with @Argument.
     */
    val basicStack: SourceFile = SourceFile.kotlin(
        "BasicDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "basic", startDestination = BasicDestination.Home::class)
        sealed class BasicDestination : NavDestination {

            @Destination(route = "basic/home")
            data object Home : BasicDestination()

            @Destination(route = "basic/detail/{id}")
            data class Detail(@Argument val id: String) : BasicDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 2. Stack with Multiple Arguments

    /**
     * A stack destination with multiple @Argument params of different types: String, Int, Boolean.
     */
    val stackWithMultipleArgs: SourceFile = SourceFile.kotlin(
        "MultiArgDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "multiArg", startDestination = MultiArgDestination.Search::class)
        sealed class MultiArgDestination : NavDestination {

            @Destination(route = "multi/search")
            data object Search : MultiArgDestination()

            @Destination(route = "multi/result/{query}/{page}/{includeArchived}")
            data class Result(
                @Argument val query: String,
                @Argument val page: Int,
                @Argument val includeArchived: Boolean,
            ) : MultiArgDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 3. Tabs with Items

    /**
     * Two @TabItem @Stack sealed classes (HomeTab, ExploreTab) and a @Tabs object (MainTabs).
     */
    val tabsWithItems: SourceFile = SourceFile.kotlin(
        "TabsDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem
        @Stack(name = "homeTab", startDestination = HomeTab.Feed::class)
        sealed class HomeTab : NavDestination {

            @Destination(route = "home/feed")
            data object Feed : HomeTab()

            @Destination(route = "home/article/{articleId}")
            data class Article(@Argument val articleId: String) : HomeTab()
        }

        @TabItem
        @Stack(name = "exploreTab", startDestination = ExploreTab.Discover::class)
        sealed class ExploreTab : NavDestination {

            @Destination(route = "explore/discover")
            data object Discover : ExploreTab()
        }

        @Tabs(
            name = "mainTabs",
            initialTab = HomeTab::class,
            items = [HomeTab::class, ExploreTab::class]
        )
        object MainTabs
        """.trimIndent()
    )

    // endregion

    // region 4. Pane with Roles

    /**
     * A @Pane sealed class with @PaneItem(role = PRIMARY) and @PaneItem(role = SECONDARY) subclasses.
     */
    val paneWithRoles: SourceFile = SourceFile.kotlin(
        "PaneDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "catalog")
        sealed class CatalogPane : NavDestination {

            @PaneItem(role = PaneRole.PRIMARY)
            data object ProductList : CatalogPane()

            @PaneItem(role = PaneRole.SECONDARY)
            data object ProductDetail : CatalogPane()
        }
        """.trimIndent()
    )

    // endregion

    // region 5. Destination with Transition

    /**
     * A stack with a destination annotated with @Transition(type = TransitionType.Fade).
     */
    val destinationWithTransition: SourceFile = SourceFile.kotlin(
        "TransitionDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.Transition
        import com.jermey.quo.vadis.annotations.TransitionType
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "animated", startDestination = AnimatedDestination.List::class)
        sealed class AnimatedDestination : NavDestination {

            @Destination(route = "animated/list")
            data object List : AnimatedDestination()

            @Transition(type = TransitionType.Fade)
            @Destination(route = "animated/overlay")
            data object Overlay : AnimatedDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 6. Full Navigation Graph

    /**
     * Comprehensive source combining stack, tabs, pane, transitions, and arguments.
     */
    val fullNavigationGraph: SourceFile = SourceFile.kotlin(
        "FullNavigationGraph.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.*
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        // -- Tabs --

        @TabItem
        @Stack(name = "homeStack", startDestination = HomeDestinations.Feed::class)
        sealed class HomeDestinations : NavDestination {

            @Destination(route = "home/feed")
            data object Feed : HomeDestinations()

            @Transition(type = TransitionType.SlideHorizontal)
            @Destination(route = "home/article/{articleId}")
            data class Article(@Argument val articleId: String) : HomeDestinations()
        }

        @TabItem
        @Stack(name = "settingsStack", startDestination = SettingsDestinations.Root::class)
        sealed class SettingsDestinations : NavDestination {

            @Destination(route = "settings/root")
            data object Root : SettingsDestinations()

            @Transition(type = TransitionType.Fade)
            @Destination(route = "settings/account/{userId}/{section}")
            data class Account(
                @Argument val userId: String,
                @Argument val section: Int,
            ) : SettingsDestinations()
        }

        @Tabs(
            name = "appTabs",
            initialTab = HomeDestinations::class,
            items = [HomeDestinations::class, SettingsDestinations::class]
        )
        object AppTabs

        // -- Pane --

        @Pane(name = "messages", backBehavior = PaneBackBehavior.PopUntilContentChange)
        sealed class MessagesPane : NavDestination {

            @PaneItem(role = PaneRole.PRIMARY)
            data object Inbox : MessagesPane()

            @PaneItem(role = PaneRole.SECONDARY, adaptStrategy = AdaptStrategy.OVERLAY)
            data object Thread : MessagesPane()
        }
        """.trimIndent()
    )

    // endregion

    // region 7. Duplicate Route Stack (error case)

    /**
     * Two destinations in the same @Stack with the same route string.
     */
    val duplicateRouteStack: SourceFile = SourceFile.kotlin(
        "DuplicateRoutes.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "dupRoute", startDestination = DuplicateRouteDestination.ScreenA::class)
        sealed class DuplicateRouteDestination : NavDestination {

            @Destination(route = "dup/same")
            data object ScreenA : DuplicateRouteDestination()

            @Destination(route = "dup/same")
            data object ScreenB : DuplicateRouteDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 8. Missing Route Argument (error case)

    /**
     * Route has {id} placeholder but destination has no @Argument val id.
     */
    val missingRouteArgument: SourceFile = SourceFile.kotlin(
        "MissingRouteArgument.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "missingArg", startDestination = MissingArgDestination.Home::class)
        sealed class MissingArgDestination : NavDestination {

            @Destination(route = "missing/home")
            data object Home : MissingArgDestination()

            @Destination(route = "missing/detail/{id}")
            data object Detail : MissingArgDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 9. Extra Argument (error case)

    /**
     * Destination has @Argument val name but route has no {name} placeholder.
     */
    val extraArgument: SourceFile = SourceFile.kotlin(
        "ExtraArgument.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "extraArg", startDestination = ExtraArgDestination.Home::class)
        sealed class ExtraArgDestination : NavDestination {

            @Destination(route = "extra/home")
            data object Home : ExtraArgDestination()

            @Destination(route = "extra/profile")
            data class Profile(@Argument val name: String) : ExtraArgDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 10. Missing Primary Pane (error case)

    /**
     * @Pane with only SECONDARY PaneItem, no PRIMARY.
     */
    val missingPrimaryPane: SourceFile = SourceFile.kotlin(
        "MissingPrimaryPane.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "brokenPane")
        sealed class BrokenPane : NavDestination {

            @PaneItem(role = PaneRole.SECONDARY)
            data object Detail : BrokenPane()
        }
        """.trimIndent()
    )

    // endregion

    // region 11. Orphan Screen (error case)

    /**
     * @Screen referencing a class that has no @Destination annotation.
     */
    val orphanScreen: SourceFile = SourceFile.kotlin(
        "OrphanScreen.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Screen
        import com.jermey.quo.vadis.core.navigation.navigator.Navigator

        class NotADestination

        @Screen(NotADestination::class)
        fun OrphanScreenFun(navigator: Navigator) {
            // This screen references a class without @Destination
        }
        """.trimIndent()
    )

    // endregion

    // region 12. Stack Not Sealed (error case)

    /**
     * @Stack applied to a regular (non-sealed) class.
     */
    val stackNotSealed: SourceFile = SourceFile.kotlin(
        "StackNotSealed.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "notSealed", startDestination = NotSealedDestination.Home::class)
        open class NotSealedDestination : NavDestination {

            @Destination(route = "notsealed/home")
            data object Home : NotSealedDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 13. Destination Not In Stack (error case)

    /**
     * @Destination on a class inside a sealed class that lacks a container annotation.
     */
    val destinationNotInStack: SourceFile = SourceFile.kotlin(
        "DestinationNotInStack.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        sealed class NotAContainer : NavDestination {
            @Destination(route = "orphan/route")
            data object OrphanDestination : NotAContainer()
        }
        """.trimIndent()
    )

    // endregion

    // region 14. Empty Tabs (edge case)

    /**
     * @Tabs with empty items array — structurally valid annotation but produces
     * a tab container with no tabs.
     */
    val emptyTabs: SourceFile = SourceFile.kotlin(
        "EmptyTabs.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Tabs

        @Tabs(name = "emptyTabs", items = [])
        object EmptyTabs
        """.trimIndent()
    )

    // endregion

    // region 15. Valid Screen Binding

    /**
     * A @Screen function binding to a valid @Destination, with proper Navigator parameter.
     */
    val validScreenBinding: SourceFile = SourceFile.kotlin(
        "ValidScreenBinding.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Screen
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination
        import com.jermey.quo.vadis.core.navigation.navigator.Navigator

        @Stack(name = "screen", startDestination = ScreenDestination.Home::class)
        sealed class ScreenDestination : NavDestination {

            @Destination(route = "screen/home")
            data object Home : ScreenDestination()

            @Destination(route = "screen/detail/{itemId}")
            data class Detail(@Argument val itemId: String) : ScreenDestination()
        }

        @Screen(ScreenDestination.Home::class)
        fun HomeScreen(navigator: Navigator) {
            // Simple screen with only Navigator
        }

        @Screen(ScreenDestination.Detail::class)
        fun DetailScreen(destination: ScreenDestination.Detail, navigator: Navigator) {
            // Screen receiving the destination instance and Navigator
        }
        """.trimIndent()
    )

    // endregion

    // region 16. Deep Link Argument Types

    val deepLinkArgumentTypes: SourceFile = SourceFile.kotlin(
        "DeepLinkArgTypes.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        enum class Status { ACTIVE, INACTIVE, PENDING }

        @Stack(name = "argTypes", startDestination = ArgTypeDestination.Home::class)
        sealed class ArgTypeDestination : NavDestination {

            @Destination(route = "args/home")
            data object Home : ArgTypeDestination()

            @Destination(route = "args/string/{name}")
            data class StringArg(@Argument val name: String) : ArgTypeDestination()

            @Destination(route = "args/int/{count}")
            data class IntArg(@Argument val count: Int) : ArgTypeDestination()

            @Destination(route = "args/long/{id}")
            data class LongArg(@Argument val id: Long) : ArgTypeDestination()

            @Destination(route = "args/float/{score}")
            data class FloatArg(@Argument val score: Float) : ArgTypeDestination()

            @Destination(route = "args/double/{precise}")
            data class DoubleArg(@Argument val precise: Double) : ArgTypeDestination()

            @Destination(route = "args/boolean/{flag}")
            data class BooleanArg(@Argument val flag: Boolean) : ArgTypeDestination()

            @Destination(route = "args/enum/{status}")
            data class EnumArg(@Argument val status: Status) : ArgTypeDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 17. Scoped Destinations via Tabs

    val scopedDestinations: SourceFile = SourceFile.kotlin(
        "ScopedDestinations.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem
        @Stack(name = "profileTab", startDestination = ProfileTab.Overview::class)
        sealed class ProfileTab : NavDestination {

            @Destination(route = "profile/overview")
            data object Overview : ProfileTab()

            @Destination(route = "profile/edit/{field}")
            data class Edit(@Argument val field: String) : ProfileTab()
        }

        @TabItem
        @Stack(name = "notificationsTab", startDestination = NotificationsTab.List::class)
        sealed class NotificationsTab : NavDestination {

            @Destination(route = "notifications/list")
            data object List : NotificationsTab()

            @Destination(route = "notifications/detail/{notifId}")
            data class Detail(@Argument val notifId: String) : NotificationsTab()
        }

        @Tabs(
            name = "scopedTabs",
            initialTab = ProfileTab::class,
            items = [ProfileTab::class, NotificationsTab::class]
        )
        object ScopedTabs
        """.trimIndent()
    )

    // endregion

    // region 18. Multi-Module Feature Source

    val featureModuleSource: SourceFile = SourceFile.kotlin(
        "FeatureDestination.kt",
        """
        package test.feature

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "feature", startDestination = FeatureDestination.Main::class)
        sealed class FeatureDestination : NavDestination {

            @Destination(route = "feature/main")
            data object Main : FeatureDestination()

            @Destination(route = "feature/detail/{itemId}")
            data class Detail(@Argument val itemId: String) : FeatureDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region 19. Navigation Root

    val navigationRootSource: SourceFile = SourceFile.kotlin(
        "AppRoot.kt",
        """
        package test.app

        import com.jermey.quo.vadis.annotations.NavigationRoot

        @NavigationRoot
        object AppRoot
        """.trimIndent()
    )

    // endregion

    // region 20. Deep Link Destinations

    val deepLinkDestinations: SourceFile = SourceFile.kotlin(
        "DeepLinkDest.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "deepLink", startDestination = DeepLinkDestination.Home::class)
        sealed class DeepLinkDestination : NavDestination {

            @Destination(route = "deep/home")
            data object Home : DeepLinkDestination()

            @Destination(route = "deep/detail/{id}")
            data class Detail(@Argument val id: String) : DeepLinkDestination()

            @Destination(route = "deep/user/{userId}/post/{postId}")
            data class UserPost(
                @Argument val userId: String,
                @Argument val postId: String,
            ) : DeepLinkDestination()
        }
        """.trimIndent()
    )

    // endregion
}
