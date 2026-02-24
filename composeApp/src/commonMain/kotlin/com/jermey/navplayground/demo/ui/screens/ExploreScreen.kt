package com.jermey.navplayground.demo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.components.NavigationBottomSheetContent
import com.jermey.navplayground.demo.ui.components.explore.AnimatedSearchBar
import com.jermey.navplayground.demo.ui.components.explore.CategoryChipsRow
import com.jermey.navplayground.demo.ui.components.explore.ExploreEmptyState
import com.jermey.navplayground.demo.ui.components.explore.ExploreGrid
import com.jermey.navplayground.demo.ui.components.explore.FilterSheet
import com.jermey.navplayground.demo.ui.components.explore.QuickPreviewSheet
import com.jermey.navplayground.demo.ui.components.explore.StaggerAnimationState
import com.jermey.navplayground.demo.ui.components.explore.rememberStaggerAnimationState
import com.jermey.navplayground.demo.ui.components.glassmorphism.GlassBottomSheet
import com.jermey.navplayground.demo.ui.screens.explore.ExploreAction
import com.jermey.navplayground.demo.ui.screens.explore.ExploreContainer
import com.jermey.navplayground.demo.ui.screens.explore.ExploreIntent
import com.jermey.navplayground.demo.ui.screens.explore.ExploreState
import com.jermey.navplayground.demo.ui.screens.explore.FilterChangeType
import com.jermey.navplayground.demo.ui.screens.explore.rememberExploreFilterState
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.flowmvi.rememberContainer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Explore Screen - Shows a grid of explore items with search, filtering, and preview capabilities.
 *
 * Features:
 * - MVI architecture via [ExploreContainer]
 * - Animated search bar with glassmorphism
 * - Category chip filtering
 * - Sort and rating filter sheet
 * - Quick preview on long press
 * - Navigation to detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MainTabs.ExploreTab.Feed::class)
@Composable
fun ExploreScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier,
    store: Store<ExploreState, ExploreIntent, ExploreAction> = rememberContainer(qualifier<ExploreContainer>())
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Subscribe to state and handle actions
    val state by store.subscribe { action ->
        scope.launch {
            when (action) {
                is ExploreAction.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = action.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is ExploreAction.NavigateToDetail -> {
                    navigator.navigate(MainTabs.ExploreTab.Detail(action.itemId))
                }

                is ExploreAction.ScrollToTop -> {
                    // Grid scroll handled internally
                }
            }
        }
    }

    // Navigation bottom sheet
    var showNavSheet by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            when {
                // Loading → Content: fade out loading + fade in content with slide up
                initialState is ExploreState.Loading && targetState is ExploreState.Content -> {
                    (fadeIn(animationSpec = tween(300)) + slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 20 }
                    )).togetherWith(
                        fadeOut(animationSpec = tween(200))
                    )
                }
                // Content → Error: crossfade
                initialState is ExploreState.Content && targetState is ExploreState.Error -> {
                    fadeIn(animationSpec = tween(300)).togetherWith(
                        fadeOut(animationSpec = tween(300))
                    )
                }
                // Error → Content (retry): fade + slide up
                initialState is ExploreState.Error && targetState is ExploreState.Content -> {
                    (fadeIn(animationSpec = tween(300)) + slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 20 }
                    )).togetherWith(
                        fadeOut(animationSpec = tween(200))
                    )
                }
                // Default: crossfade
                else -> {
                    fadeIn(animationSpec = tween(300)).togetherWith(
                        fadeOut(animationSpec = tween(300))
                    )
                }
            }.using(SizeTransform(clip = false))
        },
        contentKey = { it::class },
        label = "exploreStateTransition"
    ) { currentState ->
        when (currentState) {
            is ExploreState.Loading -> {
                ExploreLoadingState(modifier = modifier)
            }
            is ExploreState.Error -> {
                ExploreErrorState(
                    message = currentState.message,
                    onRetry = { store.intent(ExploreIntent.LoadItems) },
                    modifier = modifier
                )
            }
            is ExploreState.Content -> {
                ExploreContent(
                    state = currentState,
                    store = store,
                    navigator = navigator,
                    snackbarHostState = snackbarHostState,
                    showNavSheet = showNavSheet,
                    onShowNavSheet = { showNavSheet = it },
                    modifier = modifier
                )
            }
        }
    }
}

private const val SKELETON_CARD_COUNT = 6
private const val SKELETON_GRID_COLUMNS = 2
private const val SKELETON_CARD_HEIGHT = 200
private const val SKELETON_GRID_SPACING = 12
private const val SKELETON_CORNER_RADIUS = 16
private const val SHIMMER_DURATION_MS = 1200
private const val SHIMMER_TRANSLATE_TARGET = 1000f
private const val SHIMMER_TRANSLATE_OFFSET = 500f

/**
 * Skeleton loading state with shimmer cards matching the actual grid layout.
 *
 * Displays a 2-column grid of shimmer placeholder cards that mirror the real
 * content structure, providing a polished loading experience.
 */
@Composable
private fun ExploreLoadingState(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest
    )

    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_TRANSLATE_TARGET,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonShimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - SHIMMER_TRANSLATE_OFFSET, translateAnim - SHIMMER_TRANSLATE_OFFSET),
        end = Offset(translateAnim, translateAnim)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(SKELETON_GRID_COLUMNS),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(SKELETON_GRID_SPACING.dp),
        verticalArrangement = Arrangement.spacedBy(SKELETON_GRID_SPACING.dp),
        userScrollEnabled = false
    ) {
        items(SKELETON_CARD_COUNT) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SKELETON_CARD_HEIGHT.dp),
                shape = RoundedCornerShape(SKELETON_CORNER_RADIUS.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                )
            }
        }
    }
}

/**
 * Error state with retry button.
 */
@Composable
private fun ExploreErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Main content state with all explore features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreContent(
    state: ExploreState.Content,
    store: Store<ExploreState, ExploreIntent, ExploreAction>,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    showNavSheet: Boolean,
    onShowNavSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Separate haze state for top bar blur (grid content as source)
    val topBarHazeState = remember { HazeState() }

    ExploreScaffold(
        state = state,
        store = store,
        navigator = navigator,
        snackbarHostState = snackbarHostState,
        topBarHazeState = topBarHazeState,
        onShowNavSheet = onShowNavSheet,
        modifier = modifier
    )

    ExploreBottomSheets(
        state = state,
        store = store,
        navigator = navigator,
        hazeState = topBarHazeState,
        showNavSheet = showNavSheet,
        onShowNavSheet = onShowNavSheet
    )
}

/**
 * Main scaffold with collapsing top bar using proper scroll behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreScaffold(
    state: ExploreState.Content,
    store: Store<ExploreState, ExploreIntent, ExploreAction>,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    topBarHazeState: HazeState,
    onShowNavSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    // Create the TopAppBarScrollBehavior for enter-always collapse/expand
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ExploreTopBar(
                state = state,
                store = store,
                hazeState = topBarHazeState,
                scrollBehavior = scrollBehavior,
                onShowNavSheet = onShowNavSheet
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        // Calculate bottom padding: navigation bar height + navbar insets + extra spacing
        val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
        val bottomPadding = 80.dp + navBarInsets.calculateBottomPadding() + 16.dp
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = topBarHazeState)
        ) {
            ExploreGridContent(
                state = state,
                store = store,
                navigator = navigator,
                gridState = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding(),
                    bottom = bottomPadding
                )
            )
        }
    }
}

/**
 * Collapsible top bar with search and category chips.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
private fun ExploreTopBar(
    state: ExploreState.Content,
    store: Store<ExploreState, ExploreIntent, ExploreAction>,
    hazeState: HazeState,
    scrollBehavior: TopAppBarScrollBehavior,
    onShowNavSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeStyle = HazeMaterials.ultraThin()

    Column(
        modifier = modifier
            .hazeEffect(state = hazeState) {
                style = hazeStyle
            }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
    ) {
        TopAppBar(
            title = { Text("Explore") },
            navigationIcon = {
                IconButton(onClick = { onShowNavSheet(true) }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = { store.intent(ExploreIntent.ToggleFilterSheet) }) {
                    BadgedBox(
                        badge = {
                            if (state.isFiltered) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior
        )
        AnimatedSearchBar(
            query = state.searchQuery,
            onQueryChange = { store.intent(ExploreIntent.Search(it)) },
            onFocusChange = { store.intent(ExploreIntent.SetSearchFocus(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        CategoryChipsRow(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { store.intent(ExploreIntent.SelectCategory(it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Grid or empty state content.
 */
@Composable
private fun ExploreGridContent(
    state: ExploreState.Content,
    store: Store<ExploreState, ExploreIntent, ExploreAction>,
    navigator: Navigator,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val filterState = rememberExploreFilterState()
    val staggerState = rememberStaggerAnimationState()

    LaunchedEffect(state.filteredItems.map { it.id }.toSet()) {
        val changeType = filterState.update(state.filteredItems.map { it.id }.toSet())
        if (changeType != FilterChangeType.NONE) {
            staggerState.triggerAnimation()
        }
    }

    val isEmpty = state.filteredItems.isEmpty()

    AnimatedContent(
        targetState = isEmpty,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)).togetherWith(
                fadeOut(animationSpec = tween(300))
            ).using(SizeTransform(clip = false))
        },
        label = "gridEmptyTransition"
    ) { isEmptyState ->
        if (isEmptyState) {
            ExploreEmptyState(
                searchQuery = state.searchQuery,
                selectedCategory = state.selectedCategory,
                onClearFilters = { store.intent(ExploreIntent.ClearFilters) },
                modifier = Modifier.fillMaxSize().padding(contentPadding)
            )
        } else {
            ExploreGrid(
                items = state.filteredItems,
                onItemClick = { item -> navigator.navigate(MainTabs.ExploreTab.Detail(item.id)) },
                onItemLongClick = { item -> store.intent(ExploreIntent.ShowPreview(item)) },
                contentPadding = contentPadding,
                gridState = gridState,
                staggerState = staggerState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Bottom sheets for preview, filter, and navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreBottomSheets(
    state: ExploreState.Content,
    store: Store<ExploreState, ExploreIntent, ExploreAction>,
    navigator: Navigator,
    hazeState: HazeState,
    showNavSheet: Boolean,
    onShowNavSheet: (Boolean) -> Unit
) {
    QuickPreviewSheet(
        item = state.previewItem,
        onDismiss = { store.intent(ExploreIntent.DismissPreview) },
        onViewDetails = { item ->
            store.intent(ExploreIntent.DismissPreview)
            navigator.navigate(MainTabs.ExploreTab.Detail(item.id))
        },
        hazeState = hazeState
    )

    if (state.isFilterSheetVisible) {
        FilterSheet(
            currentSortOrder = state.sortOrder,
            currentRatingFilter = state.ratingFilter,
            selectedCategory = state.selectedCategory,
            categories = state.categories,
            resultCount = state.filteredItems.size,
            onSortOrderChange = { store.intent(ExploreIntent.SetSortOrder(it)) },
            onRatingFilterChange = { store.intent(ExploreIntent.SetRatingFilter(it)) },
            onCategoryChange = { store.intent(ExploreIntent.SelectCategory(it)) },
            onApply = { store.intent(ExploreIntent.ToggleFilterSheet) },
            onReset = { store.intent(ExploreIntent.ClearFilters) },
            onDismiss = { store.intent(ExploreIntent.ToggleFilterSheet) },
            hazeState = hazeState
        )
    }

    if (showNavSheet) {
        GlassBottomSheet(
            onDismissRequest = { onShowNavSheet(false) },
            hazeState = hazeState
        ) {
            NavigationBottomSheetContent(
                currentRoute = "explore",
                onNavigate = { destination ->
                    navigator.navigate(destination)
                    onShowNavSheet(false)
                }
            )
        }
    }
}
