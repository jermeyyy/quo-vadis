package com.jermey.navplayground

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.BaseModuleNavigation
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import com.jermey.quo.vadis.core.navigation.core.navigationGraph
import com.jermey.quo.vadis.core.navigation.core.typedDestination
import com.jermey.quo.vadis.core.navigation.mvi.NavigationIntent
import com.jermey.quo.vadis.core.navigation.mvi.NavigationViewModel
import kotlinx.serialization.Serializable

/**
 * Example of using the navigation library with MVI architecture.
 */

// Define feature-specific destinations
sealed class FeatureDestination : Destination {
    data object ProductList : FeatureDestination() {
        override val route = "products"
    }

    data class ProductDetail(val productId: String) : FeatureDestination(),
        TypedDestination<ProductDetail.ProductDetailData> {
        companion object {
            const val ROUTE = "product_detail"
        }

        override val route = ROUTE
        override val data = ProductDetailData(productId)

        /**
         * Serializable data for ProductDetail destination.
         */
        @Serializable
        data class ProductDetailData(val productId: String)
    }

    data class CartDetail(val itemCount: Int) : FeatureDestination(),
        TypedDestination<CartDetail.CartData> {
        companion object {
            const val ROUTE = "cart_detail"
        }

        override val route = ROUTE
        override val data = CartData(itemCount)

        /**
         * Serializable data for Cart destination.
         */
        @Serializable
        data class CartData(val itemCount: Int = 0)
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
                        destination = FeatureDestination.CartDetail(itemCount = 1),
                        transition = NavigationTransitions.SlideVertical
                    )
                )
            }

            is ProductIntent.ViewCart -> {
                handleNavigationIntent(
                    NavigationIntent.Navigate(
                        destination = FeatureDestination.CartDetail(itemCount = 0),
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

            typedDestination(FeatureDestination.ProductDetail.ROUTE) { data: FeatureDestination.ProductDetail, navigator ->
                ProductDetailScreen(data.productId, ProductViewModel(navigator))
            }

            typedDestination(FeatureDestination.CartDetail.ROUTE) { data: FeatureDestination.CartDetail, navigator ->
                CartScreen(data.itemCount, ProductViewModel(navigator))
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

