package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.BooksDetail
import com.jermey.navplayground.demo.destinations.MoviesDetail
import com.jermey.navplayground.demo.destinations.MusicDetail
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

// =============================================================================
// Music Detail Screen
// =============================================================================

/**
 * Music Detail Screen - Shows details for a selected music item.
 *
 * Demonstrates nested navigation within a tab - this screen is pushed
 * onto the Music tab's stack.
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MusicDetail::class)
@Composable
fun MusicDetailScreen(
    destination: MusicDetail,
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val itemIndex = destination.itemId.removePrefix("music_").toIntOrNull() ?: 1
    val title = getMusicTitleById(itemIndex)
    val artist = getMusicArtistById(itemIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        DetailContent(
            modifier = modifier.padding(padding),
            icon = Icons.Default.MusicNote,
            title = title,
            subtitle = artist,
            details = listOf(
                DetailRow(Icons.Default.Person, "Artist", artist),
                DetailRow(Icons.Default.Album, "Album", "Greatest Hits ${(itemIndex % 5) + 1}"),
                DetailRow(Icons.Default.CalendarMonth, "Year", "${1970 + (itemIndex * 3) % 50}"),
                DetailRow(Icons.Default.Star, "Rating", "${"★".repeat((itemIndex % 5) + 1)}${"☆".repeat(5 - (itemIndex % 5) - 1)}")
            ),
            description = "This is a classic song that has stood the test of time. " +
                "It showcases the unique musical style of $artist and continues to " +
                "inspire musicians and fans around the world."
        )
    }
}

private fun getMusicTitleById(index: Int): String {
    val titles = listOf(
        "Bohemian Rhapsody", "Hotel California", "Stairway to Heaven", "Imagine",
        "Sweet Child O' Mine", "Smells Like Teen Spirit", "Billie Jean",
        "Like a Rolling Stone", "Hey Jude", "Purple Rain"
    )
    return titles[(index - 1) % titles.size]
}

private fun getMusicArtistById(index: Int): String {
    val artists = listOf(
        "Queen", "Eagles", "Led Zeppelin", "John Lennon", "Guns N' Roses",
        "Nirvana", "Michael Jackson", "Bob Dylan", "The Beatles", "Prince"
    )
    return artists[(index - 1) % artists.size]
}

// =============================================================================
// Movies Detail Screen
// =============================================================================

/**
 * Movies Detail Screen - Shows details for a selected movie item.
 *
 * Demonstrates nested navigation within a tab - this screen is pushed
 * onto the Movies tab's stack.
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MoviesDetail::class)
@Composable
fun MoviesDetailScreen(
    destination: MoviesDetail,
    navigator: Navigator = koinInject()
) {
    val itemIndex = destination.itemId.removePrefix("movie_").toIntOrNull() ?: 1
    val title = getMovieTitleById(itemIndex)
    val year = getMovieYearById(itemIndex)
    val director = getMovieDirectorById(itemIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        DetailContent(
            modifier = Modifier.padding(padding),
            icon = Icons.Default.Movie,
            title = title,
            subtitle = "Directed by $director",
            details = listOf(
                DetailRow(Icons.Default.Person, "Director", director),
                DetailRow(Icons.Default.CalendarMonth, "Year", year),
                DetailRow(Icons.Default.Star, "Rating", "${"★".repeat((itemIndex % 5) + 1)}${"☆".repeat(5 - (itemIndex % 5) - 1)}"),
                DetailRow(Icons.Default.Movie, "Genre", getMovieGenreById(itemIndex))
            ),
            description = "A critically acclaimed film that has become a landmark in cinema. " +
                "This movie showcases exceptional storytelling and has left a lasting " +
                "impact on audiences worldwide."
        )
    }
}

private fun getMovieTitleById(index: Int): String {
    val titles = listOf(
        "The Shawshank Redemption", "The Godfather", "The Dark Knight", "Pulp Fiction",
        "Forrest Gump", "Inception", "The Matrix", "Goodfellas", "Fight Club", "Interstellar"
    )
    return titles[(index - 1) % titles.size]
}

private fun getMovieYearById(index: Int): String {
    val years = listOf("1994", "1972", "2008", "1994", "1994", "2010", "1999", "1990", "1999", "2014")
    return years[(index - 1) % years.size]
}

private fun getMovieDirectorById(index: Int): String {
    val directors = listOf(
        "Frank Darabont", "Francis Ford Coppola", "Christopher Nolan", "Quentin Tarantino",
        "Robert Zemeckis", "Christopher Nolan", "The Wachowskis", "Martin Scorsese",
        "David Fincher", "Christopher Nolan"
    )
    return directors[(index - 1) % directors.size]
}

private fun getMovieGenreById(index: Int): String {
    val genres = listOf(
        "Drama", "Crime", "Action", "Crime", "Drama", "Sci-Fi", "Sci-Fi", "Crime", "Drama", "Sci-Fi"
    )
    return genres[(index - 1) % genres.size]
}

// =============================================================================
// Books Detail Screen
// =============================================================================

/**
 * Books Detail Screen - Shows details for a selected book item.
 *
 * Demonstrates nested navigation within a tab - this screen is pushed
 * onto the Books tab's stack.
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
@Screen(BooksDetail::class)
@Composable
fun BooksDetailScreen(
    destination: BooksDetail,
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val itemIndex = destination.itemId.removePrefix("book_").toIntOrNull() ?: 1
    val title = getBookTitleById(itemIndex)
    val author = getBookAuthorById(itemIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        DetailContent(
            modifier = modifier.padding(padding),
            icon = Icons.AutoMirrored.Default.MenuBook,
            title = title,
            subtitle = "by $author",
            details = listOf(
                DetailRow(Icons.Default.Person, "Author", author),
                DetailRow(Icons.Default.CalendarMonth, "Published", getBookYearById(itemIndex)),
                DetailRow(Icons.Default.Star, "Rating", "${"★".repeat((itemIndex % 5) + 1)}${"☆".repeat(5 - (itemIndex % 5) - 1)}"),
                DetailRow(Icons.AutoMirrored.Default.MenuBook, "Genre", getBookGenreById(itemIndex))
            ),
            description = "A timeless literary masterpiece that continues to captivate readers. " +
                "This book explores profound themes and has influenced generations of writers " +
                "and thinkers."
        )
    }
}

private fun getBookTitleById(index: Int): String {
    val titles = listOf(
        "1984", "To Kill a Mockingbird", "The Great Gatsby", "Pride and Prejudice",
        "The Catcher in the Rye", "Lord of the Flies", "Animal Farm", "Brave New World",
        "The Hobbit", "Fahrenheit 451"
    )
    return titles[(index - 1) % titles.size]
}

private fun getBookAuthorById(index: Int): String {
    val authors = listOf(
        "George Orwell", "Harper Lee", "F. Scott Fitzgerald", "Jane Austen",
        "J.D. Salinger", "William Golding", "George Orwell", "Aldous Huxley",
        "J.R.R. Tolkien", "Ray Bradbury"
    )
    return authors[(index - 1) % authors.size]
}

private fun getBookYearById(index: Int): String {
    val years = listOf("1949", "1960", "1925", "1813", "1951", "1954", "1945", "1932", "1937", "1953")
    return years[(index - 1) % years.size]
}

private fun getBookGenreById(index: Int): String {
    val genres = listOf(
        "Dystopian", "Drama", "Tragedy", "Romance", "Fiction", "Allegory",
        "Political Satire", "Dystopian", "Fantasy", "Dystopian"
    )
    return genres[(index - 1) % genres.size]
}

// =============================================================================
// Shared Detail Content
// =============================================================================

/**
 * Data class for a detail row in the detail screen.
 */
private data class DetailRow(
    val icon: ImageVector,
    val label: String,
    val value: String
)

/**
 * Shared composable for rendering detail content.
 *
 * @param modifier Modifier for the root container
 * @param icon Main icon for the item
 * @param title Title of the item
 * @param subtitle Subtitle of the item
 * @param details List of detail rows to display
 * @param description Description text
 */
@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    details: List<DetailRow>,
    description: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Details Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium
                )
                details.forEach { detail ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = detail.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = detail.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = detail.value,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Description Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
