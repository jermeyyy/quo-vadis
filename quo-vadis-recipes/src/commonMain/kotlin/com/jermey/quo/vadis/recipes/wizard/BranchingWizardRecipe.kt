@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// ============================================================
// MIG-004b: BRANCHING WIZARD FLOW RECIPE
// ============================================================

/**
 * # Branching Wizard Flow Recipe
 *
 * Demonstrates conditional multi-step navigation for checkout processes,
 * dynamic forms, and flows where the path depends on user choices or state.
 *
 * ## What This Recipe Shows
 *
 * 1. **Conditional Branching** - Different paths based on state
 * 2. **Route Template Parameters** - Confirmation screen with `{orderId}`
 * 3. **Flow Completion** - Clear checkout stack with `navigateAndClearTo()`
 * 4. **Cancel Flow** - Exit checkout at any point
 *
 * ## Flow Pattern (Branching)
 *
 * ```
 * Cart → Shipping ─┬─→ DeliveryOptions ─┐
 *                  │   (physical items) │
 *                  │                    ▼
 *                  └─→ Payment ────────→ Review → Confirmation(orderId)
 *                      (digital only)              │
 *                                                  ▼
 *                                          (Clear & Exit)
 * ```
 *
 * ## Key Navigation Patterns
 *
 * | Action | Method | Purpose |
 * |--------|--------|---------|
 * | Conditional next | `if (cond) navigate(A) else navigate(B)` | Branch in flow |
 * | With parameter | `navigate(Confirmation(orderId))` | Pass data to destination |
 * | Complete flow | `navigateAndClearTo(HomeDest)` | Exit checkout, go home |
 * | Cancel flow | `navigateAndClearAll(CartDest)` | Abandon checkout |
 *
 * @see CheckoutDestination for destination definitions
 */

// ============================================================
// DESTINATION DEFINITION
// ============================================================

/**
 * Checkout wizard destinations with conditional branching.
 *
 * The flow branches at Shipping based on cart contents:
 * - Has physical products → DeliveryOptions → Payment
 * - Digital only → Payment (skips DeliveryOptions)
 */
@Stack(name = "checkout", startDestination = "Cart")
sealed class CheckoutDestination : DestinationInterface {

    /** Cart review - entry point */
    @Destination(route = "checkout/cart")
    data object Cart : CheckoutDestination()

    /** Shipping address - branches after this step */
    @Destination(route = "checkout/shipping")
    data object Shipping : CheckoutDestination()

    /** Delivery options - CONDITIONAL, only for physical products */
    @Destination(route = "checkout/delivery")
    data object DeliveryOptions : CheckoutDestination()

    /** Payment method selection */
    @Destination(route = "checkout/payment")
    data object Payment : CheckoutDestination()

    /** Order review before confirmation */
    @Destination(route = "checkout/review")
    data object Review : CheckoutDestination()

    /**
     * Order confirmation with order ID parameter.
     *
     * Route template: `checkout/confirmation/{orderId}`
     * Deep link: `myapp://checkout/confirmation/ORD-12345`
     *
     * @Argument annotation marks navigation arguments explicitly for KSP processing.
     */
    @Destination(route = "checkout/confirmation/{orderId}")
    data class Confirmation(
        @Argument val orderId: String
    ) : CheckoutDestination()
}

// ============================================================
// SIMULATED STATE (for conditional branching demo)
// ============================================================

/**
 * Simulated cart state for demonstrating conditional branching.
 *
 * In production, this would come from a ViewModel or repository.
 */
object CartState {
    /** Whether cart contains physical products (requires delivery options) */
    var hasPhysicalProducts: Boolean = true
}

/**
 * Generates a simple order ID for demo purposes.
 */
private fun generateOrderId(): String = "ORD-${(100000..999999).random()}"

// ============================================================
// SCREENS
// ============================================================

/**
 * Cart review screen - entry point for checkout.
 */
@Screen(CheckoutDestination.Cart::class)
@Composable
fun CartScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Cart",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Review your items before checkout.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Simulated cart info
        Text(
            text = if (CartState.hasPhysicalProducts) {
                "Contains physical items (delivery options will show)"
            } else {
                "Digital only (delivery options will be skipped)"
            },
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navigator.navigate(CheckoutDestination.Shipping) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Proceed to Checkout")
        }
    }
}

/**
 * Shipping screen with conditional branching.
 *
 * ## Branching Pattern
 *
 * After this step, the flow branches based on cart contents:
 *
 * ```kotlin
 * val nextStep = if (CartState.hasPhysicalProducts) {
 *     CheckoutDestination.DeliveryOptions  // Physical items need delivery
 * } else {
 *     CheckoutDestination.Payment  // Skip delivery for digital-only
 * }
 * navigator.navigate(nextStep)
 * ```
 */
@Screen(CheckoutDestination.Shipping::class)
@Composable
fun ShippingScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shipping Address",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Enter your shipping details.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Show what the next step will be
        Text(
            text = "Next: ${if (CartState.hasPhysicalProducts) "DeliveryOptions" else "Payment (skip delivery)"}",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    // CONDITIONAL BRANCHING: Different path based on cart contents
                    val nextStep = if (CartState.hasPhysicalProducts) {
                        CheckoutDestination.DeliveryOptions
                    } else {
                        CheckoutDestination.Payment  // Skip delivery for digital-only
                    }
                    navigator.navigate(nextStep)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Delivery options screen - CONDITIONAL step.
 *
 * Only shown when cart contains physical products.
 */
@Screen(CheckoutDestination.DeliveryOptions::class)
@Composable
fun DeliveryOptionsScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Delivery Options",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Choose your delivery method.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "(This step only appears for physical products)",
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = { navigator.navigate(CheckoutDestination.Payment) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Payment screen.
 */
@Screen(CheckoutDestination.Payment::class)
@Composable
fun PaymentScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Payment",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Select your payment method.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = { navigator.navigate(CheckoutDestination.Review) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
        }
    }
}

/**
 * Review screen - last step before confirmation.
 *
 * ## Parameterized Navigation
 *
 * When placing the order, we navigate to Confirmation with an orderId:
 *
 * ```kotlin
 * val orderId = generateOrderId()
 * navigator.navigate(CheckoutDestination.Confirmation(orderId))
 * ```
 */
@Screen(CheckoutDestination.Review::class)
@Composable
fun ReviewScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Review Order",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Review your order before placing it.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    // Navigate with parameter: generate order ID
                    val orderId = generateOrderId()
                    navigator.navigate(CheckoutDestination.Confirmation(orderId))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Place Order")
            }
        }
    }
}

/**
 * Confirmation screen with order ID parameter.
 *
 * ## Flow Completion
 *
 * When user is done, clear the checkout stack:
 *
 * ```kotlin
 * navigator.navigateAndClearTo(AppDestination.Home)
 * ```
 *
 * This prevents user from navigating back into completed checkout.
 */
@Screen(CheckoutDestination.Confirmation::class)
@Composable
fun ConfirmationScreen(
    destination: CheckoutDestination.Confirmation,
    navigator: Navigator
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "Order Confirmed!",
            style = MaterialTheme.typography.headlineMedium
        )

        // Display the order ID from destination parameter
        Text(
            text = "Order ID: ${destination.orderId}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        // PATTERN: Clear checkout stack and navigate home
        // This prevents user from going back into completed checkout
        Button(
            onClick = {
                @Suppress("DEPRECATION")
                navigator.navigateAndClearTo(AppDestination.Home)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue Shopping")
        }
    }
}

// ============================================================
// APP ENTRY POINT
// ============================================================

/**
 * Entry point for the Branching Wizard recipe.
 *
 * ## Production Setup
 *
 * ```kotlin
 * @Composable
 * fun CheckoutApp() {
 *     val navTree = remember { buildCheckoutNavNode() }  // KSP-generated
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry
 *     )
 * }
 * ```
 */
@Composable
fun BranchingWizardApp() {
    Text("BranchingWizardApp - See KDoc for production implementation")
}
