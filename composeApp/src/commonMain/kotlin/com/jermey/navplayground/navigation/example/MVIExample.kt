package com.jermey.navplayground.navigation.example

import androidx.compose.runtime.Composable
import com.jermey.navplayground.navigation.core.*
import com.jermey.navplayground.navigation.mvi.*

/**
 * Example of using the navigation library with MVI architecture.
 */

// Define feature-specific destinations
sealed class FeatureDestination : Destination {
    data object ProductList : FeatureDestination() {
        override val route = "products"
    }

    data class ProductDetail(val productId: String) : FeatureDestination() {
        override val route = "product_detail"
        override val arguments = mapOf("productId" to productId)
    }

    data class Cart(val items: Int = 0) : FeatureDestination() {
        override val route = "cart"
        override val arguments = mapOf("itemCount" to items)
    }
}

// Define MVI state for the feature
data class ProductState(
    val products: List<String> = emptyList(),
    val selectedProduct: String? = null,
    val cartItemCount: Int = 0
)

// Define MVI intents
sealed interface ProductIntent {
    data class SelectProduct(val productId: String) : ProductIntent
    data object AddToCart : ProductIntent
    data object ViewCart : ProductIntent
    data object NavigateBack : ProductIntent
}

// ViewModel using NavigationViewModel base class
class ProductViewModel(
    navigator: Navigator
) : NavigationViewModel(navigator) {

    fun handleIntent(intent: ProductIntent) {
        when (intent) {
            is ProductIntent.SelectProduct -> {
                handleNavigationIntent(
                    NavigationIntent.Navigate(
                        destination = FeatureDestination.ProductDetail(intent.productId),
                        transition = NavigationTransitions.SlideHorizontal
                    )
                )
            }

            is ProductIntent.AddToCart -> {
                // Business logic here
                // Then navigate
                handleNavigationIntent(
                    NavigationIntent.Navigate(
                        destination = FeatureDestination.Cart(items = 1),
                        transition = NavigationTransitions.SlideVertical
                    )
                )
            }

            is ProductIntent.ViewCart -> {
                handleNavigationIntent(
                    NavigationIntent.Navigate(
                        destination = FeatureDestination.Cart(),
                        transition = NavigationTransitions.Fade
                    )
                )
            }

            is ProductIntent.NavigateBack -> {
                handleNavigationIntent(NavigationIntent.NavigateBack)
            }
        }
    }
}

/**
 * Example of a modular feature with its own navigation graph.
 */
class ProductFeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph(): NavigationGraph {
        return navigationGraph("products_feature") {
            startDestination(FeatureDestination.ProductList)

            destination(FeatureDestination.ProductList) { _, navigator ->
                ProductListScreen(ProductViewModel(navigator))
            }

            destination(SimpleDestination("product_detail")) { dest, navigator ->
                val productId = dest.arguments["productId"] as? String ?: ""
                ProductDetailScreen(productId, ProductViewModel(navigator))
            }

            destination(SimpleDestination("cart")) { dest, navigator ->
                val itemCount = dest.arguments["itemCount"] as? Int ?: 0
                CartScreen(itemCount, ProductViewModel(navigator))
            }
        }
    }

    override fun entryPoints(): List<Destination> {
        return listOf(FeatureDestination.ProductList)
    }
}

@Composable
fun ProductListScreen(viewModel: ProductViewModel) {
    // Screen implementation
}

@Composable
fun ProductDetailScreen(productId: String, viewModel: ProductViewModel) {
    // Screen implementation
}

@Composable
fun CartScreen(itemCount: Int, viewModel: ProductViewModel) {
    // Screen implementation
}

