# MIG-007C: Master-Detail Screens Migration

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007C |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 hours |
| **Dependencies** | MIG-007A (Foundation Destinations) |
| **Output** | Migrated master-detail screens using `@Screen` bindings |

## Objective

Migrate master-detail screens from callback-based patterns to the new `@Screen` annotation bindings with destination parameter injection. This removes the need for separate data classes and `TypedDestination<T>` usage.

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/
├── MasterListScreen.kt    # Add @Screen(MasterDetailDestination.List::class)
├── DetailScreen.kt        # Add @Screen(MasterDetailDestination.Detail::class) with destination param
└── Item.kt                # Keep as-is (UI model, not navigation data)
```

### Current Implementation Analysis

| File | Current Pattern | New Pattern |
|------|-----------------|-------------|
| `MasterListScreen.kt` | Callback-based: `onItemClick: (String) -> Unit` | `@Screen` + `Navigator` |
| `DetailScreen.kt` | Callback-based: `itemId: String, onBack: (), onNavigateToRelated: (String)` | `@Screen` + destination param + `Navigator` |
| `Item.kt` | UI data model | No change (not navigation-related) |

### Reference Recipes

| Recipe | Pattern |
|--------|---------|
| [MIG-002](./MIG-002-master-detail-example.md) | Master-Detail pattern with route templates |
| [ListDetailRecipe](../../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/masterdetail/ListDetailRecipe.kt) | Reference implementation |

---

## Migration Steps

### Step 1: Update MasterListScreen.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/MasterListScreen.kt`

#### 1.1 Add Required Imports

```kotlin
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
```

#### 1.2 Transform Screen Signature

```kotlin
// OLD: Callback-based
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MasterListScreen(
    onItemClick: (String) -> Unit,
    onBack: () -> Unit
) {
    // ...
}

// NEW: @Screen with Navigator
@Screen(MasterDetailDestination.List::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MasterListScreen(navigator: Navigator) {
    // ...
}
```

#### 1.3 Update Navigation Calls

```kotlin
// OLD: Callback invocation
onClick = { onItemClick(item.id) }

// NEW: Direct navigation
onClick = { navigator.navigate(MasterDetailDestination.Detail(itemId = item.id)) }
```

```kotlin
// OLD: Callback for back
navigationIcon = {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
    }
}

// NEW: Navigator back
navigationIcon = {
    IconButton(onClick = { navigator.navigateBack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
    }
}
```

#### 1.4 Complete Transformed MasterListScreen

```kotlin
package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.ui.components.ItemCard
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

private const val MASTER_LIST_ITEMS_COUNT = 50

/**
 * Master List Screen - Shows list of items (Master view)
 */
@Screen(MasterDetailDestination.List::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MasterListScreen(navigator: Navigator) {
    val items = remember {
        (1..MASTER_LIST_ITEMS_COUNT).map {
            Item(
                id = "item_$it",
                title = "Item $it",
                subtitle = "Description for item $it",
                category = listOf("Electronics", "Books", "Clothing", "Food")[it % 4]
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master-Detail Pattern") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Select an item to view details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(items) { item ->
                ItemCard(
                    item = item,
                    onClick = { navigator.navigate(MasterDetailDestination.Detail(itemId = item.id)) }
                )
            }
        }
    }
}
```

---

### Step 2: Update DetailScreen.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/DetailScreen.kt`

#### 2.1 Add Required Imports

```kotlin
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
```

#### 2.2 Transform Screen Signature

```kotlin
// OLD: Callback-based with itemId parameter
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onNavigateToRelated: (String) -> Unit
) {
    // ...
}

// NEW: @Screen with destination parameter
@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator
) {
    val itemId = destination.itemId
    // ...
}
```

#### 2.3 Update Navigation Calls

```kotlin
// OLD: Callback for back
IconButton(onClick = onBack) { ... }

// NEW: Navigator back
IconButton(onClick = { navigator.navigateBack() }) { ... }
```

```kotlin
// OLD: Callback for related item navigation
onNavigateToRelated = { onNavigateToRelated(relatedId) }

// NEW: Direct navigation
onNavigateToRelated = { navigator.navigate(MasterDetailDestination.Detail(itemId = relatedId)) }
```

```kotlin
// OLD: Back button callback
OutlinedButton(onClick = onBack, ...) { Text("Back to List") }

// NEW: Navigator back
OutlinedButton(onClick = { navigator.navigateBack() }, ...) { Text("Back to List") }
```

#### 2.4 Update RelatedItemCard Component

```kotlin
// OLD: Callback parameter
@Composable
private fun RelatedItemCard(
    relatedItemName: String,
    onNavigateToRelated: () -> Unit
) { ... }

// No change needed - component remains composable and reusable
// The onClick is passed from parent
```

#### 2.5 Complete Transformed DetailScreen

```kotlin
package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.ui.components.DetailRow
import com.jermey.navplayground.demo.ui.components.SpecificationRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.quoVadisSharedElement
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.sharedBounds
import com.jermey.quo.vadis.core.navigation.core.sharedElement

private const val RELATED_ITEMS_COUNT = 5

/**
 * Detail Screen - Shows details of selected item (Detail view)
 */
@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator
) {
    val itemId = destination.itemId
    
    val relatedItems = remember(itemId) {
        (1..RELATED_ITEMS_COUNT).map { "Related item $it" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Favorite, "Favorite")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DetailHeaderCard(itemId)
            }

            item {
                SpecificationsCard()
            }

            item {
                RelatedItemsHeader()
            }

            items(relatedItems.size) { index ->
                val relatedId = "related_${itemId}_$index"
                RelatedItemCard(
                    relatedItemName = relatedItems[index],
                    onNavigateToRelated = { navigator.navigate(MasterDetailDestination.Detail(itemId = relatedId)) }
                )
            }

            item {
                ActionButtons(onBack = { navigator.navigateBack() })
            }
        }
    }
}

// ... (private helper composables remain unchanged)
```

---

### Step 3: Verify Item.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/Item.kt`

**No changes required.** The `Item` data class is a UI model used for displaying list items, NOT a navigation argument class. It should remain as-is:

```kotlin
package com.jermey.navplayground.demo.ui.screens.masterdetail

data class Item(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String
)
```

**Important:** Do NOT confuse UI models (`Item`) with navigation destinations (`MasterDetailDestination.Detail`). The destination only carries the `itemId`, and the screen reconstructs or fetches the full `Item` data as needed.

---

### Step 4: Verify Destination Definition

Ensure `MasterDetailDestination` in `Destinations.kt` is updated per MIG-007A:

```kotlin
@Stack(name = "masterDetail", startDestination = "List")
sealed class MasterDetailDestination : DestinationInterface {
    @Destination(route = "masterDetail/list")
    data object List : MasterDetailDestination()
    
    @Destination(route = "masterDetail/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String
    ) : MasterDetailDestination()
}
```

---

## Key Transformation Summary

| Aspect | Before | After |
|--------|--------|-------|
| **List Screen Signature** | `fun MasterListScreen(onItemClick: (String), onBack: ())` | `@Screen fun MasterListScreen(navigator: Navigator)` |
| **Detail Screen Signature** | `fun DetailScreen(itemId: String, onBack: (), onNavigateToRelated: (String))` | `@Screen fun DetailScreen(destination: MasterDetailDestination.Detail, navigator: Navigator)` |
| **Item Click** | `onItemClick(item.id)` | `navigator.navigate(MasterDetailDestination.Detail(itemId = item.id))` |
| **Back Navigation** | `onBack()` | `navigator.navigateBack()` |
| **Related Item Navigation** | `onNavigateToRelated(relatedId)` | `navigator.navigate(MasterDetailDestination.Detail(itemId = relatedId))` |
| **Data Access** | Direct `itemId` parameter | `destination.itemId` |

---

## Checklist

- [ ] Add `@Screen(MasterDetailDestination.List::class)` to `MasterListScreen`
- [ ] Update `MasterListScreen` signature: remove callbacks, add `Navigator`
- [ ] Replace `onItemClick(id)` with `navigator.navigate(MasterDetailDestination.Detail(itemId = id))`
- [ ] Replace `onBack()` with `navigator.navigateBack()` in `MasterListScreen`
- [ ] Add `@Screen(MasterDetailDestination.Detail::class)` to `DetailScreen`
- [ ] Update `DetailScreen` signature: add `destination` param, add `Navigator`, remove callbacks
- [ ] Extract `itemId` from `destination.itemId`
- [ ] Replace all `onBack()` calls with `navigator.navigateBack()` in `DetailScreen`
- [ ] Replace `onNavigateToRelated(id)` with `navigator.navigate(MasterDetailDestination.Detail(itemId = id))`
- [ ] Verify `Item.kt` remains unchanged (UI model, not navigation data)
- [ ] Add required imports to both files
- [ ] Run compilation to verify changes

---

## Verification

```bash
# Verify compilation after screen changes
./gradlew :composeApp:compileKotlinMetadata

# Check for remaining callback patterns in masterdetail screens
grep -r "onItemClick\|onBack\|onNavigateToRelated" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/

# Check for @Screen annotations
grep -r "@Screen" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/

# Verify no TypedDestination usage
grep -r "TypedDestination" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/masterdetail/
```

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Prerequisite - defines `MasterDetailDestination`)
- [MIG-002: Master-Detail Recipe](./MIG-002-master-detail-example.md) (Pattern reference)
- [ListDetailRecipe.kt](../../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/masterdetail/ListDetailRecipe.kt) (New architecture example)
