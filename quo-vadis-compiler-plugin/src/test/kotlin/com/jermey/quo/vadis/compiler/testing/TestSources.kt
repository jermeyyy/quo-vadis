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

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem(parent = MainTabs::class, ordinal = 0)
        @Stack(name = "homeTab", startDestination = HomeTab.Feed::class)
        sealed class HomeTab : NavDestination {

            @Destination(route = "home/feed")
            data object Feed : HomeTab()

            @Destination(route = "home/article/{articleId}")
            data class Article(@Argument val articleId: String) : HomeTab()
        }

        @TabItem(parent = MainTabs::class, ordinal = 1)
        @Stack(name = "exploreTab", startDestination = ExploreTab.Discover::class)
        sealed class ExploreTab : NavDestination {

            @Destination(route = "explore/discover")
            data object Discover : ExploreTab()
        }

        @Tabs(
            name = "mainTabs",
        )
        object MainTabs
        """.trimIndent()
    )

    /**
     * Mixed tabs container with one flat destination tab and one stack-backed tab.
     */
    val mixedTabsWithStackBackedItem: SourceFile = SourceFile.kotlin(
        "MixedTabsDestination.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem(parent = MixedTabs::class, ordinal = 0)
        @Destination(route = "overview")
        data object OverviewTab : NavDestination

        @TabItem(parent = MixedTabs::class, ordinal = 1)
        @Stack(name = "settingsTab", startDestination = SettingsTab.Root::class)
        sealed class SettingsTab : NavDestination {

            @Destination(route = "settings/root")
            data object Root : SettingsTab()

            @Destination(route = "settings/detail/{section}")
            data class Detail(@Argument val section: String) : SettingsTab()
        }

        @Tabs(
            name = "mixedTabs",
        )
        object MixedTabs

        @TabsContainer(MixedTabs::class)
        @Composable
        fun MixedTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
        ) {
            content()
        }
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

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.*
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        // -- Tabs --

        @TabItem(parent = AppTabs::class, ordinal = 0)
        @Stack(name = "homeStack", startDestination = HomeDestinations.Feed::class)
        sealed class HomeDestinations : NavDestination {

            @Destination(route = "home/feed")
            data object Feed : HomeDestinations()

            @Transition(type = TransitionType.SlideHorizontal)
            @Destination(route = "home/article/{articleId}")
            data class Article(@Argument val articleId: String) : HomeDestinations()
        }

        @TabItem(parent = AppTabs::class, ordinal = 1)
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
        )
        object AppTabs

        @TabsContainer(AppTabs::class)
        @Composable
        fun AppTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
        ) {
            content()
        }

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

        @Tabs(name = "emptyTabs")
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

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem(parent = ScopedTabs::class, ordinal = 0)
        @Stack(name = "profileTab", startDestination = ProfileTab.Overview::class)
        sealed class ProfileTab : NavDestination {

            @Destination(route = "profile/overview")
            data object Overview : ProfileTab()

            @Destination(route = "profile/edit/{field}")
            data class Edit(@Argument val field: String) : ProfileTab()
        }

        @TabItem(parent = ScopedTabs::class, ordinal = 1)
        @Stack(name = "notificationsTab", startDestination = NotificationsTab.List::class)
        sealed class NotificationsTab : NavDestination {

            @Destination(route = "notifications/list")
            data object List : NotificationsTab()

            @Destination(route = "notifications/detail/{notifId}")
            data class Detail(@Argument val notifId: String) : NotificationsTab()
        }

        @Tabs(
            name = "scopedTabs",
        )
        object ScopedTabs

        @TabsContainer(ScopedTabs::class)
        @Composable
        fun ScopedTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
        ) {
            content()
        }
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

    // region 18b. Multi-Module Screen Sources

    val crossModuleFeatureOneSources: Array<SourceFile> = arrayOf(
        SourceFile.kotlin(
            "FeatureOneDestination.kt",
            """
            package test.feature1

            import androidx.compose.runtime.Composable
            import com.jermey.quo.vadis.annotations.Destination
            import com.jermey.quo.vadis.annotations.Screen
            import com.jermey.quo.vadis.annotations.Stack
            import com.jermey.quo.vadis.core.navigation.destination.NavDestination

            @Stack(name = "featureOne", startDestination = FeatureOneDestination.Home::class)
            sealed class FeatureOneDestination : NavDestination {

                @Destination(route = "feature1/home")
                data object Home : FeatureOneDestination()
            }

            @Screen(FeatureOneDestination.Home::class)
            @Composable
            fun FeatureOneHomeScreen() {
            }
            """.trimIndent()
        ),
    )

    val crossModuleFeatureTwoSources: Array<SourceFile> = arrayOf(
        SourceFile.kotlin(
            "FeatureTwoDestination.kt",
            """
            package test.feature2

            import androidx.compose.runtime.Composable
            import com.jermey.quo.vadis.annotations.Destination
            import com.jermey.quo.vadis.annotations.Screen
            import com.jermey.quo.vadis.annotations.Stack
            import com.jermey.quo.vadis.core.navigation.destination.NavDestination

            @Stack(name = "featureTwo", startDestination = FeatureTwoDestination.Login::class)
            sealed class FeatureTwoDestination : NavDestination {

                @Destination(route = "feature2/login")
                data object Login : FeatureTwoDestination()
            }

            @Screen(FeatureTwoDestination.Login::class)
            @Composable
            fun FeatureTwoLoginScreen() {
            }
            """.trimIndent()
        ),
    )

    val crossModuleAggregationAppSources: Array<SourceFile> = arrayOf(
        SourceFile.kotlin(
            "AppRoot.kt",
            """
            package test.app

            import com.jermey.quo.vadis.annotations.NavigationRoot

            @NavigationRoot
            object AppRoot
            """.trimIndent()
        ),
        SourceFile.kotlin(
            "ConfigResolver.kt",
            """
            package test.app

            import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
            import com.jermey.quo.vadis.core.navigation.config.navigationConfig

            fun resolveConfig(): NavigationConfig = navigationConfig<AppRoot>()
            """.trimIndent()
        ),
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

    // region 19b. Single-Module Config Resolution

    /**
     * A single-module source that calls navigationConfig<T>() to verify that
     * the compiler plugin rewrites the call-site even when no aggregated
     * config is present (single-module fallback).
     */
    val singleModuleConfigResolution: Array<SourceFile> = arrayOf(
        SourceFile.kotlin(
            "AppRoot.kt",
            """
            package test.app

            import com.jermey.quo.vadis.annotations.NavigationRoot

            @NavigationRoot
            object AppRoot
            """.trimIndent()
        ),
        SourceFile.kotlin(
            "SingleModuleDest.kt",
            """
            package test.app

            import com.jermey.quo.vadis.annotations.Destination
            import com.jermey.quo.vadis.annotations.Stack
            import com.jermey.quo.vadis.core.navigation.destination.NavDestination

            @Stack(name = "single", startDestination = SingleDest.Home::class)
            sealed class SingleDest : NavDestination {

                @Destination(route = "single/home")
                data object Home : SingleDest()
            }
            """.trimIndent()
        ),
        SourceFile.kotlin(
            "ConfigCaller.kt",
            """
            package test.app

            import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
            import com.jermey.quo.vadis.core.navigation.config.navigationConfig

            fun resolveConfig(): NavigationConfig = navigationConfig<AppRoot>()
            """.trimIndent()
        ),
    )

    // endregion

    // region 19c. Pane Back Behavior Variants

    /**
     * Three pane sealed classes with different PaneBackBehavior values
     * to verify ordinal mapping in the generated config.
     */
    val paneBackBehaviorVariants: SourceFile = SourceFile.kotlin(
        "PaneBackBehaviorVariants.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneBackBehavior
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "defaultPane")
        sealed class DefaultPane : NavDestination {
            @PaneItem(role = PaneRole.PRIMARY)
            data object List : DefaultPane()
            @PaneItem(role = PaneRole.SECONDARY)
            data object Detail : DefaultPane()
        }

        @Pane(name = "popLatestPane", backBehavior = PaneBackBehavior.PopLatest)
        sealed class PopLatestPane : NavDestination {
            @PaneItem(role = PaneRole.PRIMARY)
            data object List : PopLatestPane()
            @PaneItem(role = PaneRole.SECONDARY)
            data object Detail : PopLatestPane()
        }

        @Pane(name = "contentChangePane", backBehavior = PaneBackBehavior.PopUntilContentChange)
        sealed class ContentChangePane : NavDestination {
            @PaneItem(role = PaneRole.PRIMARY)
            data object List : ContentChangePane()
            @PaneItem(role = PaneRole.SECONDARY)
            data object Detail : ContentChangePane()
        }
        """.trimIndent()
    )

    // endregion

    // region 19d. Multiple Pane Scopes

    /**
     * Two pane sealed classes with different scopes and overlapping destination types
     * to verify PaneRoleRegistry scope filtering.
     */
    val multiplePaneScopes: SourceFile = SourceFile.kotlin(
        "MultiplePaneScopes.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "catalogPane")
        sealed class CatalogPane2 : NavDestination {
            @PaneItem(role = PaneRole.PRIMARY)
            data object Products : CatalogPane2()
            @PaneItem(role = PaneRole.SECONDARY)
            data object ProductInfo : CatalogPane2()
        }

        @Pane(name = "ordersPane")
        sealed class OrdersPane : NavDestination {
            @PaneItem(role = PaneRole.PRIMARY)
            data object OrderList : OrdersPane()
            @PaneItem(role = PaneRole.SECONDARY)
            data object OrderDetail : OrdersPane()
        }
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

    val standaloneDeepLinkDestinations: SourceFile = SourceFile.kotlin(
        "StandaloneDeepLinkDest.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Tabs(
            name = "standaloneTabs",
        )
        sealed class StandaloneTabs : NavDestination {

            companion object : NavDestination

            @Destination(route = "standalone/landing")
            data object Landing : StandaloneTabs()

            @Destination(route = "standalone/profile/{userId}")
            data class Profile(@Argument val userId: String) : StandaloneTabs()
        }

        @TabItem(parent = StandaloneTabs::class, ordinal = 0)
        @Stack(name = "standaloneHome", startDestination = StandaloneHomeTab.Home::class)
        sealed class StandaloneHomeTab : StandaloneTabs() {

            @Destination(route = "standalone/home")
            data object Home : StandaloneHomeTab()
        }

        @TabsContainer(StandaloneTabs::class)
        @Composable
        fun StandaloneTabsContainer(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
        ) {
            content()
        }
        """.trimIndent()
    )

    val queryBackedDeepLinkDestinations: SourceFile = SourceFile.kotlin(
        "QueryBackedDeepLinkDest.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Stack(name = "queryBacked", startDestination = QueryBackedDestination.Home::class)
        sealed class QueryBackedDestination : NavDestination {

            @Destination(route = "query/home")
            data object Home : QueryBackedDestination()

            @Destination(route = "query/detail/{id}?id={id}&ref={ref}")
            data class Detail(
                @Argument val id: String,
                @Argument val ref: String,
            ) : QueryBackedDestination()
        }
        """.trimIndent()
    )

    // endregion

    // region Tabs with @TabsContainer wrapper

    /**
     * Tabs source with a @TabsContainer wrapper function, plus another source file
     * defining the wrapper composable. Used for wrapper registration and dispatch tests.
     */
    val tabsContainerWrapper: SourceFile = SourceFile.kotlin(
        "TabsContainerWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @TabsContainer(MainTabs::class)
        @Composable
        fun MainTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Tabs source mirroring the composeApp state-driven demo shape where the wrapper
     * is targeted at the tabs root companion and normalizes back to the tabs scope key.
     */
    val companionTargetedTabsContainerSource: SourceFile = SourceFile.kotlin(
        "CompanionTargetedTabsContainer.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Tabs(
            name = "stateDrivenDemo",
        )
        sealed class StateDrivenDemoDestination : NavDestination {

            companion object : NavDestination
        }

        @TabItem(parent = StateDrivenDemoDestination::class, ordinal = 0)
        @Stack(name = "stateDrivenStack", startDestination = StateDrivenDemoTab.Home::class)
        sealed class StateDrivenDemoTab : StateDrivenDemoDestination() {

            @Destination(route = "state-driven/home")
            data object Home : StateDrivenDemoTab()

            @Destination(route = "state-driven/profile/{userId}")
            data class Profile(@Argument val userId: String) : StateDrivenDemoTab()
        }
        """.trimIndent()
    )

    /**
     * Companion-targeted tabs wrapper matching the normalized wrapper key shape used by composeApp.
     */
    val companionTargetedTabsContainerWrapper: SourceFile = SourceFile.kotlin(
        "CompanionTargetedTabsContainerWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @TabsContainer(StateDrivenDemoDestination.Companion::class)
        @Composable
        fun StateDrivenDemoContainer(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Tabs wrapper with Compose-style synthetic parameters to exercise transformed-signature validation.
     */
    val composeSyntheticTabsContainerWrapper: SourceFile = SourceFile.kotlin(
        "ComposeSyntheticTabsContainerWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.Composer
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @TabsContainer(MainTabs::class)
        @Composable
        fun MainTabsComposeTransformedWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
            `${'$'}composer`: Composer?,
            `${'$'}changed`: Int,
            `${'$'}default`: Int,
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Pane source with a @PaneContainer wrapper function for dispatch coverage.
     */
    val paneContainerWrapper: SourceFile = SourceFile.kotlin(
        "PaneContainerWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.PaneContainer
        import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope

        @PaneContainer(CatalogPane::class)
        @Composable
        fun CatalogPaneWrapper(
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Pane source closer to the composeApp messages pane path, including companion target shape
     * and pane back behavior metadata.
     */
    val messagesPaneContainerSource: SourceFile = SourceFile.kotlin(
        "MessagesPaneDestination.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Argument
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneBackBehavior
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
        sealed class MessagesPane : NavDestination {

            companion object : NavDestination

            @PaneItem(role = PaneRole.PRIMARY)
            @Destination(route = "messages/conversations")
            data object ConversationList : MessagesPane()

            @PaneItem(role = PaneRole.SECONDARY)
            @Destination(route = "messages/conversation/{conversationId}")
            data class ConversationDetail(@Argument val conversationId: String) : MessagesPane()
        }
        """.trimIndent()
    )

    /**
     * Pane wrapper aligned with the messages pane demo target.
     */
    val messagesPaneContainerWrapper: SourceFile = SourceFile.kotlin(
        "MessagesPaneContainerWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.PaneContainer
        import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope

        @PaneContainer(MessagesPane::class)
        @Composable
        fun MessagesPaneContainer(
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * @TabsContainer wrapper pointing at a class without a matching @Tabs declaration.
     */
    val missingTabsContainerTarget: SourceFile = SourceFile.kotlin(
        "MissingTabsContainerTarget.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        class MissingTabsHost

        @TabsContainer(MissingTabsHost::class)
        @Composable
        fun MissingTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * @PaneContainer wrapper pointing at a class without a matching @Pane declaration.
     */
    val missingPaneContainerTarget: SourceFile = SourceFile.kotlin(
        "MissingPaneContainerTarget.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.PaneContainer
        import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope

        class MissingCatalogPane

        @PaneContainer(MissingCatalogPane::class)
        @Composable
        fun MissingPaneWrapper(
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * @TabsContainer wrapper missing @Composable.
     */
    val nonComposableTabsContainerWrapper: SourceFile = SourceFile.kotlin(
        "NonComposableTabsContainerWrapper.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @TabsContainer(MainTabs::class)
        fun InvalidTabsWrapper(
            scope: TabsContainerScope,
            content: @androidx.compose.runtime.Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * @TabsContainer wrapper with an extra required user-defined parameter.
     */
    val invalidTabsContainerRequiredUserParameterWrapper: SourceFile = SourceFile.kotlin(
        "InvalidTabsContainerRequiredUserParameterWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @TabsContainer(MainTabs::class)
        @Composable
        fun InvalidMainTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit,
            title: String,
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * @PaneContainer wrapper with an invalid scope parameter type.
     */
    val invalidPaneContainerScopeWrapper: SourceFile = SourceFile.kotlin(
        "InvalidPaneContainerScopeWrapper.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.PaneContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope

        @PaneContainer(CatalogPane::class)
        @Composable
        fun InvalidCatalogPaneWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Two @TabsContainer wrappers that normalize to the same wrapper key via Companion targeting.
     */
    val duplicateNormalizedTabsContainerWrappers: SourceFile = SourceFile.kotlin(
        "DuplicateNormalizedTabsContainerWrappers.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.annotations.TabsContainer
        import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @TabItem(parent = MainTabs::class, ordinal = 0)
        @Stack(name = "homeTab", startDestination = HomeTab.Feed::class)
        sealed class HomeTab : NavDestination {

            @Destination(route = "home/feed")
            data object Feed : HomeTab()
        }

        @Tabs(
            name = "mainTabs",
        )
        class MainTabs {
            companion object
        }

        @TabsContainer(MainTabs::class)
        @Composable
        fun MainTabsWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }

        @TabsContainer(MainTabs.Companion::class)
        @Composable
        fun MainTabsCompanionWrapper(
            scope: TabsContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    /**
     * Two @PaneContainer wrappers that normalize to the same wrapper key via Companion targeting.
     */
    val duplicateNormalizedPaneContainerWrappers: SourceFile = SourceFile.kotlin(
        "DuplicateNormalizedPaneContainerWrappers.kt",
        """
        package test

        import androidx.compose.runtime.Composable
        import com.jermey.quo.vadis.annotations.Pane
        import com.jermey.quo.vadis.annotations.PaneContainer
        import com.jermey.quo.vadis.annotations.PaneItem
        import com.jermey.quo.vadis.annotations.PaneRole
        import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Pane(name = "catalog")
        sealed class CatalogPane : NavDestination {

            @PaneItem(role = PaneRole.PRIMARY)
            data object ProductList : CatalogPane()

            @PaneItem(role = PaneRole.SECONDARY)
            data object ProductDetail : CatalogPane()

            companion object
        }

        @PaneContainer(CatalogPane::class)
        @Composable
        fun CatalogPaneWrapper(
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }

        @PaneContainer(CatalogPane.Companion::class)
        @Composable
        fun CatalogPaneCompanionWrapper(
            scope: PaneContainerScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
        """.trimIndent()
    )

    // endregion

    // region TabItem Diagnostic Error Cases

    /**
     * Two @TabItem subclasses with the same ordinal = 0 under the same @Tabs parent.
     * Should trigger TABITEM_DUPLICATE_ORDINAL.
     */
    val tabItemDuplicateOrdinal: SourceFile = SourceFile.kotlin(
        "TabItemDuplicateOrdinal.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Tabs(name = "dupOrdTabs")
        sealed class DupOrdTabs : NavDestination {
            companion object : NavDestination
        }

        @TabItem(parent = DupOrdTabs::class, ordinal = 0)
        @Stack(name = "dupOrdTab1", startDestination = DupOrdTab1.Screen1::class)
        sealed class DupOrdTab1 : DupOrdTabs() {
            @Destination(route = "dup-ord/screen1")
            data object Screen1 : DupOrdTab1()
        }

        @TabItem(parent = DupOrdTabs::class, ordinal = 0)
        @Stack(name = "dupOrdTab2", startDestination = DupOrdTab2.Screen2::class)
        sealed class DupOrdTab2 : DupOrdTabs() {
            @Destination(route = "dup-ord/screen2")
            data object Screen2 : DupOrdTab2()
        }
        """.trimIndent()
    )

    /**
     * @TabItem ordinals start at 1 instead of 0.
     * Should trigger TABITEM_MISSING_ORDINAL_ZERO.
     */
    val tabItemMissingOrdinalZero: SourceFile = SourceFile.kotlin(
        "TabItemMissingOrdinalZero.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Tabs(name = "noZeroTabs")
        sealed class NoZeroTabs : NavDestination {
            companion object : NavDestination
        }

        @TabItem(parent = NoZeroTabs::class, ordinal = 1)
        @Stack(name = "noZeroTab1", startDestination = NoZeroTab1.Screen1::class)
        sealed class NoZeroTab1 : NoZeroTabs() {
            @Destination(route = "no-zero/screen1")
            data object Screen1 : NoZeroTab1()
        }

        @TabItem(parent = NoZeroTabs::class, ordinal = 2)
        @Stack(name = "noZeroTab2", startDestination = NoZeroTab2.Screen2::class)
        sealed class NoZeroTab2 : NoZeroTabs() {
            @Destination(route = "no-zero/screen2")
            data object Screen2 : NoZeroTab2()
        }
        """.trimIndent()
    )

    /**
     * @TabItem ordinals 0, 1, 3 — missing ordinal 2.
     * Should trigger TABITEM_ORDINAL_GAP.
     */
    val tabItemOrdinalGap: SourceFile = SourceFile.kotlin(
        "TabItemOrdinalGap.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        @Tabs(name = "gapTabs")
        sealed class GapTabs : NavDestination {
            companion object : NavDestination
        }

        @TabItem(parent = GapTabs::class, ordinal = 0)
        @Stack(name = "gapTab0", startDestination = GapTab0.Screen0::class)
        sealed class GapTab0 : GapTabs() {
            @Destination(route = "gap/screen0")
            data object Screen0 : GapTab0()
        }

        @TabItem(parent = GapTabs::class, ordinal = 1)
        @Stack(name = "gapTab1", startDestination = GapTab1.Screen1::class)
        sealed class GapTab1 : GapTabs() {
            @Destination(route = "gap/screen1")
            data object Screen1 : GapTab1()
        }

        @TabItem(parent = GapTabs::class, ordinal = 3)
        @Stack(name = "gapTab3", startDestination = GapTab3.Screen3::class)
        sealed class GapTab3 : GapTabs() {
            @Destination(route = "gap/screen3")
            data object Screen3 : GapTab3()
        }
        """.trimIndent()
    )

    /**
     * @TabItem parent references a class not annotated with @Tabs.
     * Should trigger TABITEM_INVALID_PARENT.
     */
    val tabItemInvalidParent: SourceFile = SourceFile.kotlin(
        "TabItemInvalidParent.kt",
        """
        package test

        import com.jermey.quo.vadis.annotations.Destination
        import com.jermey.quo.vadis.annotations.Stack
        import com.jermey.quo.vadis.annotations.TabItem
        import com.jermey.quo.vadis.annotations.Tabs
        import com.jermey.quo.vadis.core.navigation.destination.NavDestination

        class NotATabs

        @Tabs(name = "parentRefTabs")
        sealed class ParentRefTabs : NavDestination {
            companion object : NavDestination
        }

        @TabItem(parent = NotATabs::class, ordinal = 0)
        @Stack(name = "invalidParentTab", startDestination = InvalidParentTab.Screen1::class)
        sealed class InvalidParentTab : ParentRefTabs() {
            @Destination(route = "invalid-parent/screen1")
            data object Screen1 : InvalidParentTab()
        }
        """.trimIndent()
    )

    // endregion
}
