package com.jermey.navplayground.demo.destinations

import com.jermey.navplayground.demo.destinations.DemoTabs.BooksTab
import com.jermey.navplayground.demo.destinations.DemoTabs.MoviesTab
import com.jermey.navplayground.demo.destinations.DemoTabs.MusicTab
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * Tabs Demo navigation destinations.
 *
 * Demonstrates nested tab navigation with:
 * - 3 tabs (Music, Movies, Books)
 * - Each tab has its own navigation stack
 * - Clicking items navigates within the tab's stack
 *
 * Navigation to `DemoTabs.MusicTab.List` (or any tab's root) will show the
 * tabs demo with the DemoTabsWrapper providing the tab strip UI.
 */
@Tabs(
    name = "demoTabs",
    initialTab = MusicTab::class,
    items = [MusicTab::class, MoviesTab::class, BooksTab::class]
)
sealed class DemoTabs : NavDestination {

    /**
     * Companion object used as the wrapper key for @TabsContainer.
     * This allows the wrapper to be associated with the DemoTabs container.
     */
    companion object: NavDestination

    /**
     * Music tab - shows a list of music items
     */
    @TabItem(label = "Music", icon = "music_note")
    @Stack(name = "musicStack", startDestination = MusicTab.List::class)
    sealed class MusicTab : DemoTabs() {

        @Destination(route = "demo/tabs/music/list")
        data object List : MusicTab()
    }

    /**
     * Movies tab - shows a list of movie items
     */
    @TabItem(label = "Movies", icon = "movie")
    @Stack(name = "moviesStack", startDestination = MoviesTab.List::class)
    sealed class MoviesTab : DemoTabs() {

        @Destination(route = "demo/tabs/movies/list")
        data object List : MoviesTab()
    }

    /**
     * Books tab - shows a list of book items
     */
    @TabItem(label = "Books", icon = "book")
    @Stack(name = "booksStack", startDestination = BooksTab.List::class)
    sealed class BooksTab : DemoTabs() {

        @Destination(route = "demo/tabs/books/list")
        data object List : BooksTab()


    }
}

@Destination(route = "demo/tabs/music/detail/{itemId}")
data class MusicDetail(@Argument val itemId: String): NavDestination

@Destination(route = "demo/tabs/movies/detail/{itemId}")
data class MoviesDetail(@Argument val itemId: String) : NavDestination

@Destination(route = "demo/tabs/books/detail/{itemId}")
data class BooksDetail(@Argument val itemId: String) : NavDestination
