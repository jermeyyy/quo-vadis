package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.BooksDetail
import com.jermey.navplayground.demo.destinations.DemoTabs
import com.jermey.navplayground.demo.destinations.MoviesDetail
import com.jermey.navplayground.demo.destinations.MusicDetail
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Data class representing an item in the tab lists.
 */
private data class TabItem(
    val id: String,
    val title: String,
    val subtitle: String
)

// =============================================================================
// Music Tab List Screen
// =============================================================================

/**
 * Music Tab List Screen - Shows a list of music items.
 *
 * Clicking an item navigates to the detail screen within the music tab stack.
 */
@Screen(DemoTabs.MusicTab.List::class)
@Composable
fun MusicTabListScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val items = remember {
        (1..20).map {
            TabItem(
                id = "music_$it",
                title = getMusicTitle(it),
                subtitle = getMusicArtist(it)
            )
        }
    }

    TabListContent(
        modifier = modifier,
        items = items,
        icon = Icons.Default.MusicNote,
        headerText = "Music Library",
        headerDescription = "Tap a song to view details",
        onItemClick = { item ->
            navigator.navigate(MusicDetail(itemId = item.id))
        }
    )
}

private fun getMusicTitle(index: Int): String {
    val titles = listOf(
        "Bohemian Rhapsody",
        "Hotel California",
        "Stairway to Heaven",
        "Imagine",
        "Sweet Child O' Mine",
        "Smells Like Teen Spirit",
        "Billie Jean",
        "Like a Rolling Stone",
        "Hey Jude",
        "Purple Rain"
    )
    return titles[(index - 1) % titles.size]
}

private fun getMusicArtist(index: Int): String {
    val artists = listOf(
        "Queen",
        "Eagles",
        "Led Zeppelin",
        "John Lennon",
        "Guns N' Roses",
        "Nirvana",
        "Michael Jackson",
        "Bob Dylan",
        "The Beatles",
        "Prince"
    )
    return artists[(index - 1) % artists.size]
}

// =============================================================================
// Movies Tab List Screen
// =============================================================================

/**
 * Movies Tab List Screen - Shows a list of movie items.
 *
 * Clicking an item navigates to the detail screen within the movies tab stack.
 */
@Screen(DemoTabs.MoviesTab.List::class)
@Composable
fun MoviesTabListScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val items = remember {
        (1..20).map {
            TabItem(
                id = "movie_$it",
                title = getMovieTitle(it),
                subtitle = getMovieYear(it)
            )
        }
    }

    TabListContent(
        modifier = modifier,
        items = items,
        icon = Icons.Default.Movie,
        headerText = "Movie Collection",
        headerDescription = "Tap a movie to view details",
        onItemClick = { item ->
            navigator.navigate(MoviesDetail(itemId = item.id))
        }
    )
}

private fun getMovieTitle(index: Int): String {
    val titles = listOf(
        "The Shawshank Redemption",
        "The Godfather",
        "The Dark Knight",
        "Pulp Fiction",
        "Forrest Gump",
        "Inception",
        "The Matrix",
        "Goodfellas",
        "Fight Club",
        "Interstellar"
    )
    return titles[(index - 1) % titles.size]
}

private fun getMovieYear(index: Int): String {
    val years = listOf(
        "1994",
        "1972",
        "2008",
        "1994",
        "1994",
        "2010",
        "1999",
        "1990",
        "1999",
        "2014"
    )
    return years[(index - 1) % years.size]
}

// =============================================================================
// Books Tab List Screen
// =============================================================================

/**
 * Books Tab List Screen - Shows a list of book items.
 *
 * Clicking an item navigates to the detail screen within the books tab stack.
 */
@Screen(DemoTabs.BooksTab.List::class)
@Composable
fun BooksTabListScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val items = remember {
        (1..20).map {
            TabItem(
                id = "book_$it",
                title = getBookTitle(it),
                subtitle = getBookAuthor(it)
            )
        }
    }

    TabListContent(
        modifier = modifier,
        items = items,
        icon = Icons.AutoMirrored.Default.MenuBook,
        headerText = "Book Library",
        headerDescription = "Tap a book to view details",
        onItemClick = { item ->
            navigator.navigate(BooksDetail(itemId = item.id))
        }
    )
}

private fun getBookTitle(index: Int): String {
    val titles = listOf(
        "1984",
        "To Kill a Mockingbird",
        "The Great Gatsby",
        "Pride and Prejudice",
        "The Catcher in the Rye",
        "Lord of the Flies",
        "Animal Farm",
        "Brave New World",
        "The Hobbit",
        "Fahrenheit 451"
    )
    return titles[(index - 1) % titles.size]
}

private fun getBookAuthor(index: Int): String {
    val authors = listOf(
        "George Orwell",
        "Harper Lee",
        "F. Scott Fitzgerald",
        "Jane Austen",
        "J.D. Salinger",
        "William Golding",
        "George Orwell",
        "Aldous Huxley",
        "J.R.R. Tolkien",
        "Ray Bradbury"
    )
    return authors[(index - 1) % authors.size]
}

// =============================================================================
// Shared Tab List Content
// =============================================================================

/**
 * Shared composable for rendering tab list content.
 *
 * @param modifier Modifier for the root container
 * @param items List of items to display
 * @param icon Icon to show for each item
 * @param headerText Title text at the top of the list
 * @param headerDescription Description text below the title
 * @param onItemClick Callback when an item is clicked
 */
@Composable
private fun TabListContent(
    modifier: Modifier = Modifier,
    items: List<TabItem>,
    icon: ImageVector,
    headerText: String,
    headerDescription: String,
    onItemClick: (TabItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = headerDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(items, key = { it.id }) { item ->
            TabItemCard(
                item = item,
                icon = icon,
                onClick = { onItemClick(item) }
            )
        }
    }
}

/**
 * Card component for displaying a single tab item.
 *
 * @param item The item data to display
 * @param icon Icon to show for this item
 * @param onClick Callback when the card is clicked
 */
@Composable
private fun TabItemCard(
    item: TabItem,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
