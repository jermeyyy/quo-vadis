# Migration Guide: Process/Wizard Flow Navigation

> **Difficulty**: Easy | **Time Estimate**: 30-45 minutes | **Prerequisites**: [01-simple-stack.md](./01-simple-stack.md) complete

This guide demonstrates how to migrate multi-step process and wizard flows (e.g., onboarding, checkout) from the old Quo Vadis API to the new NavNode architecture, with focus on sequential navigation, conditional branching, flow completion with stack clearing, and centralized animation configuration.

---

## Table of Contents

1. [Overview](#overview)
2. [Before (Old API)](#before-old-api)
3. [After (New API)](#after-new-api)
4. [Key Migration Steps](#key-migration-steps)
5. [Flow Control Methods](#flow-control-methods)
6. [AnimationRegistry for Flows](#animationregistry-for-flows)
7. [Common Pitfalls](#common-pitfalls)
8. [Next Steps](#next-steps)
9. [Related Resources](#related-resources)

---

## Overview

Process and wizard flows are common patterns for onboarding, checkout, forms, and any multi-step user journey. The new NavNode architecture simplifies these patterns by:

- **Providing type-safe navigation** — Destinations are classes, not string routes
- **Type-safe stack clearing** — `navigateAndClear` uses `::class` reference instead of string routes
- **Type-safe popTo** — Navigate back to a specific step using destination class
- **New `exitFlow` method** — Convenience for canceling multi-step flows
- **Centralized animations** — `AnimationRegistry` configures flow transitions in one place

### Navigation Method Changes Summary

| Pattern | Old API | New API |
|---------|---------|---------|
| Sequential steps | `navigate(NextStep, transition)` | `navigate(NextStep)` |
| Conditional branch | `navigate(StepA)` or `navigate(StepB)` | Same — no change needed |
| Complete & clear | `navigateAndClearTo(dest, "route", inclusive)` | `navigateAndClear(dest, DestClass::class, inclusive)` |
| Pop to step | `popTo("route", inclusive)` | `popTo(DestInstance)` or `popTo(DestClass::class)` |
| Exit flow | Multiple `navigateBack()` calls | `exitFlow(FlowDestination::class)` |

### Annotation Changes Summary

| Old Annotation | New Annotation | Purpose |
|----------------|----------------|---------|
| `@Graph("name")` | `@Stack(name = "name", startDestination = "...")` | Define a navigation stack container |
| `@Route("path")` | `@Destination(route = "path")` | Mark a class as a navigation target |
| `@Content(Dest::class)` | `@Screen(Dest::class)` | Bind a Composable to render a destination |

---

## Before (Old API)

### Complete Onboarding Flow Example

```kotlin
package com.example.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.*

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Onboarding Graph
// ═══════════════════════════════════════════════════════════

/**
 * Onboarding flow with conditional branching (personal vs business).
 * 
 * OLD: Uses @Graph to define the container and @Route for each destination.
 */
@Graph("onboarding", startDestination = "onboarding/welcome")
sealed class OnboardingDestination : Destination {
    
    @Route("onboarding/welcome")
    data object Welcome : OnboardingDestination()
    
    @Route("onboarding/user_type")
    data object UserTypeSelection : OnboardingDestination()
    
    // Conditional branches
    @Route("onboarding/personal_info")
    data object PersonalInfo : OnboardingDestination()
    
    @Route("onboarding/business_info")
    data object BusinessInfo : OnboardingDestination()
    
    @Route("onboarding/preferences")
    data object Preferences : OnboardingDestination()
    
    @Route("onboarding/complete")
    data object Complete : OnboardingDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Flow Screen Content
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Sequential navigation with per-call transition specification.
 */
@Content(OnboardingDestination.Welcome::class)
@Composable
fun WelcomeContent(navigator: Navigator) {
    Column {
        Text("Welcome to MyApp!")
        Text("Let's set up your account")
        
        Button(onClick = { 
            // OLD: Transition specified per-call
            navigator.navigate(
                OnboardingDestination.UserTypeSelection,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Get Started")
        }
    }
}

/**
 * OLD: Conditional branching based on user choice.
 */
@Content(OnboardingDestination.UserTypeSelection::class)
@Composable
fun UserTypeContent(navigator: Navigator) {
    Column {
        Text("What type of user are you?")
        
        // Conditional branching - navigate to different paths
        Button(onClick = {
            navigator.navigate(
                OnboardingDestination.PersonalInfo,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Personal")
        }
        
        Button(onClick = {
            navigator.navigate(
                OnboardingDestination.BusinessInfo,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Business")
        }
    }
}

/**
 * OLD: Mid-flow screen with multiple navigation options.
 */
@Content(OnboardingDestination.PersonalInfo::class)
@Composable
fun PersonalInfoContent(navigator: Navigator) {
    Column {
        Text("Personal Information")
        // ... form fields ...
        
        // Continue to next step
        Button(onClick = {
            navigator.navigate(
                OnboardingDestination.Preferences,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Continue")
        }
        
        // OLD: Pop to specific step using string route
        TextButton(onClick = {
            navigator.popTo("onboarding/user_type", inclusive = false)
        }) {
            Text("Change User Type")
        }
        
        // OLD: Cancel entire flow - must pop multiple times or use route
        TextButton(onClick = {
            navigator.popTo("onboarding/welcome", inclusive = true)
            // Or navigate back to parent graph
        }) {
            Text("Cancel")
        }
    }
}

@Content(OnboardingDestination.BusinessInfo::class)
@Composable
fun BusinessInfoContent(navigator: Navigator) {
    Column {
        Text("Business Information")
        // ... business form fields ...
        
        Button(onClick = {
            navigator.navigate(
                OnboardingDestination.Preferences,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Continue")
        }
        
        TextButton(onClick = {
            navigator.popTo("onboarding/user_type", inclusive = false)
        }) {
            Text("Change User Type")
        }
    }
}

@Content(OnboardingDestination.Preferences::class)
@Composable
fun PreferencesContent(navigator: Navigator) {
    Column {
        Text("Set Your Preferences")
        // ... preference toggles ...
        
        Button(onClick = {
            navigator.navigate(
                OnboardingDestination.Complete,
                NavigationTransitions.FadeThrough
            )
        }) {
            Text("Finish Setup")
        }
    }
}

/**
 * OLD: Flow completion with string-based stack clearing.
 */
@Content(OnboardingDestination.Complete::class)
@Composable
fun CompleteContent(navigator: Navigator) {
    Column {
        Text("Setup Complete!")
        Text("You're all set to start using MyApp")
        
        Button(onClick = {
            // OLD: Clear stack using string route - not type-safe!
            navigator.navigateAndClearTo(
                destination = AppDestination.Home,
                upToRoute = "onboarding",  // String route - easy to typo!
                inclusive = true
            )
        }) {
            Text("Enter App")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Set Up Navigation Host
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Requires manual initialization.
 */
@Composable
fun OnboardingFlow() {
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val graph = remember { onboardingGraph() }
    
    LaunchedEffect(navigator, graph) {
        navigator.registerGraph(graph)
        navigator.setStartDestination(OnboardingDestination.Welcome)
    }
    
    GraphNavHost(
        graph = graph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### Old API Characteristics

1. **String routes for popTo** — `popTo("onboarding/user_type")` is error-prone
2. **String routes for navigateAndClearTo** — `upToRoute = "onboarding"` can have typos
3. **Per-call transitions** — Repeating `NavigationTransitions.SlideHorizontal` everywhere
4. **No `exitFlow` convenience** — Must manually manage back navigation
5. **Manual initialization** — `initializeQuoVadisRoutes()`, `registerGraph()`, etc.

---

## After (New API)

### Complete Migrated Onboarding Flow

```kotlin
package com.example.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
import com.example.onboarding.generated.GeneratedScreenRegistry
import com.example.onboarding.generated.buildOnboardingNavNode

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Onboarding Stack
// ═══════════════════════════════════════════════════════════

/**
 * Onboarding flow with conditional branching (personal vs business).
 * 
 * NEW: Uses @Stack with class-name-based startDestination.
 */
@Stack(name = "onboarding", startDestination = "Welcome")
sealed class OnboardingDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    
    @Destination(route = "onboarding/welcome")
    data object Welcome : OnboardingDestination()
    
    @Destination(route = "onboarding/user_type")
    data object UserTypeSelection : OnboardingDestination()
    
    // Conditional branches
    @Destination(route = "onboarding/personal_info")
    data object PersonalInfo : OnboardingDestination()
    
    @Destination(route = "onboarding/business_info")
    data object BusinessInfo : OnboardingDestination()
    
    @Destination(route = "onboarding/preferences")
    data object Preferences : OnboardingDestination()
    
    @Destination(route = "onboarding/complete")
    data object Complete : OnboardingDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Flow Screen Content
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Sequential navigation without per-call transitions.
 * Transitions configured via AnimationRegistry.
 */
@Screen(OnboardingDestination.Welcome::class)
@Composable
fun WelcomeScreen(navigator: Navigator) {
    Column {
        Text("Welcome to MyApp!")
        Text("Let's set up your account")
        
        Button(onClick = { 
            // NEW: Just navigate! Transition comes from AnimationRegistry
            navigator.navigate(OnboardingDestination.UserTypeSelection)
        }) {
            Text("Get Started")
        }
    }
}

/**
 * NEW: Conditional branching - same pattern, no change needed!
 */
@Screen(OnboardingDestination.UserTypeSelection::class)
@Composable
fun UserTypeScreen(navigator: Navigator) {
    Column {
        Text("What type of user are you?")
        
        // Conditional branching - navigate to different paths
        Button(onClick = {
            navigator.navigate(OnboardingDestination.PersonalInfo)
        }) {
            Text("Personal")
        }
        
        Button(onClick = {
            navigator.navigate(OnboardingDestination.BusinessInfo)
        }) {
            Text("Business")
        }
    }
}

/**
 * NEW: Mid-flow screen with type-safe navigation options.
 */
@Screen(OnboardingDestination.PersonalInfo::class)
@Composable
fun PersonalInfoScreen(navigator: Navigator) {
    Column {
        Text("Personal Information")
        // ... form fields ...
        
        // Continue to next step
        Button(onClick = {
            navigator.navigate(OnboardingDestination.Preferences)
        }) {
            Text("Continue")
        }
        
        // NEW: Type-safe popTo using destination instance
        TextButton(onClick = {
            navigator.popTo(OnboardingDestination.UserTypeSelection)
        }) {
            Text("Change User Type")
        }
        
        // NEW: Exit entire flow with one call!
        TextButton(onClick = {
            navigator.exitFlow(OnboardingDestination::class)
        }) {
            Text("Cancel")
        }
    }
}

@Screen(OnboardingDestination.BusinessInfo::class)
@Composable
fun BusinessInfoScreen(navigator: Navigator) {
    Column {
        Text("Business Information")
        // ... business form fields ...
        
        Button(onClick = {
            navigator.navigate(OnboardingDestination.Preferences)
        }) {
            Text("Continue")
        }
        
        // NEW: Type-safe popTo
        TextButton(onClick = {
            navigator.popTo(OnboardingDestination.UserTypeSelection)
        }) {
            Text("Change User Type")
        }
        
        // NEW: Exit flow
        TextButton(onClick = {
            navigator.exitFlow(OnboardingDestination::class)
        }) {
            Text("Cancel")
        }
    }
}

@Screen(OnboardingDestination.Preferences::class)
@Composable
fun PreferencesScreen(navigator: Navigator) {
    Column {
        Text("Set Your Preferences")
        // ... preference toggles ...
        
        Button(onClick = {
            navigator.navigate(OnboardingDestination.Complete)
        }) {
            Text("Finish Setup")
        }
    }
}

/**
 * NEW: Flow completion with type-safe stack clearing.
 */
@Screen(OnboardingDestination.Complete::class)
@Composable
fun CompleteScreen(navigator: Navigator) {
    Column {
        Text("Setup Complete!")
        Text("You're all set to start using MyApp")
        
        Button(onClick = {
            // NEW: Type-safe! Uses ::class reference instead of string
            navigator.navigateAndClear(
                destination = AppDestination.Home,
                clearUpTo = OnboardingDestination::class,  // Type-safe!
                inclusive = true
            )
        }) {
            Text("Enter App")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Animation Configuration
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Centralized animation configuration for the entire flow.
 * No more repeating transitions in every navigate() call!
 */
val onboardingAnimations = AnimationRegistry {
    // All forward navigation in onboarding uses slide
    withinGraph(OnboardingDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Completion screen uses fade for "entering the app" feeling
    from(OnboardingDestination.Complete::class)
        .to(AppDestination.Home::class)
        .uses(FadeThrough)
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Set Up Navigation Host
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Minimal setup with KSP-generated code.
 */
@Composable
fun OnboardingFlow() {
    // KSP generates buildOnboardingNavNode() from @Stack annotation
    val navTree = remember { buildOnboardingNavNode() }
    
    // Navigator initialized directly with NavNode tree
    val navigator = rememberNavigator(navTree)
    
    // NEW: QuoVadisHost with centralized animations
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        animationRegistry = onboardingAnimations
    )
}
```

### New API Characteristics

1. **Type-safe `popTo`** — `popTo(OnboardingDestination.UserTypeSelection)` catches typos at compile time
2. **Type-safe `navigateAndClear`** — `clearUpTo = OnboardingDestination::class` is checked by compiler
3. **New `exitFlow` method** — Single call to cancel and exit the entire flow
4. **`AnimationRegistry`** — Configure all transitions in one place
5. **No manual initialization** — KSP generates everything

---

## Key Migration Steps

Follow these steps to migrate your process/wizard flow:

### Step 1: Update Graph Annotation

```diff
- @Graph("onboarding", startDestination = "onboarding/welcome")
+ @Stack(name = "onboarding", startDestination = "Welcome")
  sealed class OnboardingDestination : Destination {
```

> ⚠️ **Important**: `startDestination` now uses the **class name** (`"Welcome"`) not the route (`"onboarding/welcome"`).

### Step 2: Update Route Annotations

```diff
-     @Route("onboarding/welcome")
+     @Destination(route = "onboarding/welcome")
      data object Welcome : OnboardingDestination()
```

### Step 3: Update Content Annotations

```diff
- @Content(OnboardingDestination.Welcome::class)
+ @Screen(OnboardingDestination.Welcome::class)
  @Composable
- fun WelcomeContent(navigator: Navigator) {
+ fun WelcomeScreen(navigator: Navigator) {
```

### Step 4: Remove Per-Call Transitions

```diff
  Button(onClick = { 
-     navigator.navigate(
-         OnboardingDestination.UserTypeSelection,
-         NavigationTransitions.SlideHorizontal
-     )
+     navigator.navigate(OnboardingDestination.UserTypeSelection)
  }) {
```

### Step 5: Update popTo to Use Type-Safe Destinations

```diff
  TextButton(onClick = {
-     navigator.popTo("onboarding/user_type", inclusive = false)
+     navigator.popTo(OnboardingDestination.UserTypeSelection)
  }) {
```

> 💡 **Tip**: You can also use `popTo(OnboardingDestination.UserTypeSelection, inclusive = true)` if needed.

### Step 6: Update navigateAndClearTo to navigateAndClear

```diff
  Button(onClick = {
-     navigator.navigateAndClearTo(
-         destination = AppDestination.Home,
-         upToRoute = "onboarding",
-         inclusive = true
-     )
+     navigator.navigateAndClear(
+         destination = AppDestination.Home,
+         clearUpTo = OnboardingDestination::class,
+         inclusive = true
+     )
  }) {
```

### Step 7: Replace Multiple navigateBack with exitFlow

```diff
  TextButton(onClick = {
-     // Pop back to welcome, then exit
-     navigator.popTo("onboarding/welcome", inclusive = true)
+     navigator.exitFlow(OnboardingDestination::class)
  }) {
      Text("Cancel")
  }
```

### Step 8: Create AnimationRegistry

```kotlin
// NEW: Centralized transition configuration
val onboardingAnimations = AnimationRegistry {
    withinGraph(OnboardingDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Special transition for completion
    from(OnboardingDestination.Complete::class)
        .to(AppDestination.Home::class)
        .uses(FadeThrough)
}
```

### Step 9: Update Host Setup

```diff
  @Composable
  fun OnboardingFlow() {
-     remember { initializeQuoVadisRoutes() }
-     
-     val navigator = rememberNavigator()
-     val graph = remember { onboardingGraph() }
-     
-     LaunchedEffect(navigator, graph) {
-         navigator.registerGraph(graph)
-         navigator.setStartDestination(OnboardingDestination.Welcome)
-     }
-     
-     GraphNavHost(
-         graph = graph,
-         navigator = navigator,
-         defaultTransition = NavigationTransitions.SlideHorizontal
-     )
+     val navTree = remember { buildOnboardingNavNode() }
+     val navigator = rememberNavigator(navTree)
+     
+     QuoVadisHost(
+         navigator = navigator,
+         screenRegistry = GeneratedScreenRegistry,
+         animationRegistry = onboardingAnimations
+     )
  }
```

### Step 10: Update Imports

```diff
- import com.jermey.quo.vadis.core.navigation.Graph
- import com.jermey.quo.vadis.core.navigation.Route
- import com.jermey.quo.vadis.core.navigation.Content
- import com.jermey.quo.vadis.core.navigation.GraphNavHost
- import com.jermey.quo.vadis.core.navigation.NavigationTransitions
+ import com.jermey.quo.vadis.annotations.Destination
+ import com.jermey.quo.vadis.annotations.Screen
+ import com.jermey.quo.vadis.annotations.Stack
+ import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
+ import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
+ import com.example.onboarding.generated.GeneratedScreenRegistry
+ import com.example.onboarding.generated.buildOnboardingNavNode
```

### Step 11: Build and Verify

```bash
./gradlew :app:assembleDebug
```

---

## Flow Control Methods

The Navigator provides several methods specifically useful for process/wizard flows:

### Sequential Navigation

```kotlin
// Push to next step in the flow
navigator.navigate(OnboardingDestination.PersonalInfo)

// With conditional logic
val nextStep = if (isBusinessUser) {
    OnboardingDestination.BusinessInfo
} else {
    OnboardingDestination.PersonalInfo
}
navigator.navigate(nextStep)
```

### Pop to Specific Step

```kotlin
// Pop back to a specific step (keep that step)
navigator.popTo(OnboardingDestination.UserTypeSelection)

// Pop back to a specific step (remove that step too)
navigator.popTo(OnboardingDestination.UserTypeSelection, inclusive = true)

// Pop using class reference (finds first matching instance in stack)
navigator.popTo(OnboardingDestination.UserTypeSelection::class)
```

### Clear Stack and Navigate

```kotlin
// Complete flow and navigate to new destination
navigator.navigateAndClear(
    destination = AppDestination.Home,
    clearUpTo = OnboardingDestination::class,  // Type-safe!
    inclusive = true  // Remove the flow root too
)

// Clear only part of the stack
navigator.navigateAndClear(
    destination = OnboardingDestination.Complete,
    clearUpTo = OnboardingDestination.PersonalInfo::class,
    inclusive = false  // Keep PersonalInfo in stack
)
```

### Exit Flow

```kotlin
// Exit the entire flow (pops all destinations in the flow)
navigator.exitFlow(OnboardingDestination::class)

// Equivalent to: popTo(flowRoot, inclusive = true) + notify parent
```

### Flow State Queries

```kotlin
// Check current position
val currentDestination = navigator.currentDestination

// Check if user can go back
val canGoBack = navigator.canNavigateBack

// Get stack depth (useful for progress indicators)
val stackDepth = navigator.currentStackDepth

// Check if at flow root
val isAtRoot = stackDepth == 1
```

---

## AnimationRegistry for Flows

Process flows benefit from consistent animations that reinforce the "moving forward/backward through steps" mental model.

### Basic Flow Configuration

```kotlin
val checkoutAnimations = AnimationRegistry {
    // All navigation within checkout uses slide
    withinGraph(CheckoutDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
}
```

### Step-Specific Transitions

```kotlin
val onboardingAnimations = AnimationRegistry {
    // Default for flow
    withinGraph(OnboardingDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Special: completion uses fade (feels like "arriving")
    from(OnboardingDestination.Preferences::class)
        .to(OnboardingDestination.Complete::class)
        .uses(FadeThrough)
    
    // Special: exiting flow uses fade
    from(OnboardingDestination.Complete::class)
        .to(AppDestination.Home::class)
        .uses(FadeThrough)
}
```

### Conditional Branch Animations

```kotlin
val wizardAnimations = AnimationRegistry {
    // Different animations for different branches
    from(UserTypeSelection::class)
        .to(PersonalInfo::class)
        .uses(SlideFromRight)
    
    from(UserTypeSelection::class)
        .to(BusinessInfo::class)
        .uses(SlideFromRight)
    
    // Both branches converge at Preferences - same animation
    from(PersonalInfo::class)
        .to(Preferences::class)
        .uses(SlideFromRight)
    
    from(BusinessInfo::class)
        .to(Preferences::class)
        .uses(SlideFromRight)
}
```

### Animation Precedence

```kotlin
// More specific rules take precedence over general rules
val animations = AnimationRegistry {
    // General: all forward navigation
    default(FadeThrough)
    
    // More specific: within a graph
    withinGraph(OnboardingDestination::class) {
        forward uses SlideHorizontal
    }
    
    // Most specific: exact pair
    from(OnboardingDestination.Complete::class)
        .to(AppDestination.Home::class)
        .uses(FadeThrough)  // Takes precedence for this exact transition
}
```

---

## Checkout Flow Example

A more complex example demonstrating conditional steps based on cart contents:

```kotlin
// ═══════════════════════════════════════════════════════════
// Checkout Flow with Conditional Steps
// ═══════════════════════════════════════════════════════════

@Stack(name = "checkout", startDestination = "Cart")
sealed class CheckoutDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    
    @Destination(route = "checkout/cart")
    data object Cart : CheckoutDestination()
    
    @Destination(route = "checkout/shipping")
    data object Shipping : CheckoutDestination()
    
    // Conditional: Only for physical products
    @Destination(route = "checkout/delivery")
    data object DeliveryOptions : CheckoutDestination()
    
    @Destination(route = "checkout/payment")
    data object Payment : CheckoutDestination()
    
    @Destination(route = "checkout/review")
    data object Review : CheckoutDestination()
    
    // Parameterized destination for confirmation
    @Destination(route = "checkout/confirmation/{orderId}")
    data class Confirmation(val orderId: String) : CheckoutDestination()
}

// ═══════════════════════════════════════════════════════════
// Conditional Navigation Example
// ═══════════════════════════════════════════════════════════

@Screen(CheckoutDestination.Shipping::class)
@Composable
fun ShippingScreen(navigator: Navigator, cartState: CartState) {
    Column {
        Text("Shipping Address")
        // ... shipping form ...
        
        Button(onClick = {
            // Conditional navigation based on cart contents
            val nextStep = if (cartState.hasPhysicalProducts) {
                CheckoutDestination.DeliveryOptions
            } else {
                CheckoutDestination.Payment  // Skip delivery for digital-only
            }
            navigator.navigate(nextStep)
        }) {
            Text("Continue")
        }
        
        // Return to cart
        TextButton(onClick = {
            navigator.popTo(CheckoutDestination.Cart)
        }) {
            Text("Edit Cart")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Flow Completion with Dynamic Destination
// ═══════════════════════════════════════════════════════════

@Screen(CheckoutDestination.Review::class)
@Composable
fun ReviewScreen(navigator: Navigator, viewModel: CheckoutViewModel) {
    Column {
        Text("Review Your Order")
        // ... order summary ...
        
        Button(onClick = {
            viewModel.placeOrder { orderId ->
                // Navigate to confirmation with order ID
                navigator.navigateAndClear(
                    destination = CheckoutDestination.Confirmation(orderId),
                    clearUpTo = CheckoutDestination.Cart::class,
                    inclusive = false  // Keep Cart for "Continue Shopping"
                )
            }
        }) {
            Text("Place Order")
        }
    }
}

@Screen(CheckoutDestination.Confirmation::class)
@Composable
fun ConfirmationScreen(
    destination: CheckoutDestination.Confirmation,  // Destination with orderId
    navigator: Navigator
) {
    Column {
        Text("Order Confirmed!")
        Text("Order #${destination.orderId}")
        
        Button(onClick = {
            // Clear checkout and go home
            navigator.navigateAndClear(
                destination = AppDestination.Home,
                clearUpTo = CheckoutDestination::class,
                inclusive = true
            )
        }) {
            Text("Continue Shopping")
        }
        
        // Or view order details
        Button(onClick = {
            navigator.navigateAndClear(
                destination = AppDestination.OrderDetails(destination.orderId),
                clearUpTo = CheckoutDestination::class,
                inclusive = true
            )
        }) {
            Text("View Order Details")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Checkout Animation Configuration
// ═══════════════════════════════════════════════════════════

val checkoutAnimations = AnimationRegistry {
    // Forward progress uses slide
    withinGraph(CheckoutDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Order placed - use celebratory fade
    from(CheckoutDestination.Review::class)
        .to(CheckoutDestination.Confirmation::class)
        .uses(FadeThrough)
    
    // Exit to home/order details uses fade
    from(CheckoutDestination.Confirmation::class)
        .toAny()
        .uses(FadeThrough)
}
```

---

## Common Pitfalls

| Pitfall | Symptom | Solution |
|---------|---------|----------|
| **Using string routes in `navigateAndClear`** | Runtime error or wrong stack cleared | Use `::class` reference: `clearUpTo = OnboardingDestination::class` |
| **Forgetting `inclusive` parameter** | Unexpected behavior (root kept or removed) | Be explicit: `inclusive = true` to remove the root |
| **Complex logic in navigation calls** | Hard to test, bugs in branching | Move logic to ViewModel, pass result to `navigate()` |
| **Deep back stack without exit option** | User trapped in flow | Always provide "Cancel" or "Exit" button using `exitFlow()` |
| **Wrong `startDestination` format** | `IllegalArgumentException` at startup | Use class name (`"Welcome"`) not route (`"onboarding/welcome"`) |
| **Missing `@Screen` for step** | `No screen registered for destination` | Every destination needs a `@Screen` binding |
| **Inconsistent transitions** | Jarring user experience | Use `AnimationRegistry.withinGraph()` for consistent flow animations |
| **Using `popTo` with non-existent destination** | Runtime crash or no-op | Ensure destination exists in current stack before calling |
| **Calling `exitFlow` from wrong context** | Exits wrong flow or crashes | Use correct flow class: `exitFlow(OnboardingDestination::class)` |

### Debugging Tips

1. **Check stack state**: Use `navigator.currentStackDepth` to verify navigation
2. **Log navigation events**: Add logging in screen composables to track flow
3. **Verify AnimationRegistry**: Ensure transitions are registered for all step pairs
4. **Test conditional branches**: Cover both branches in your flow tests
5. **Build clean**: `./gradlew clean build` if KSP seems out of sync

---

## Next Steps

After migrating process/wizard flows:

- **[05-nested-flows.md](./05-nested-flows.md)** — Migrate nested flows (flow within flow)
- **[06-adaptive-panes.md](./06-adaptive-panes.md)** — Migrate adaptive multi-pane layouts

---

## Related Resources

- [01-simple-stack.md](./01-simple-stack.md) — Prerequisite: Simple stack migration guide
- [02-master-detail.md](./02-master-detail.md) — Master-detail pattern with typed arguments
- [03-tabbed-navigation.md](./03-tabbed-navigation.md) — Tabbed navigation migration
- [Phase 1: NavNode Architecture](../refactoring-plan/phase1-core/CORE-001-navnode-hierarchy.md) — NavNode type definitions
- [Phase 1: TreeMutator](../refactoring-plan/phase1-core/CORE-002-tree-mutator.md) — Navigation operations
- [Phase 2: AnimationRegistry](../refactoring-plan/phase2-renderer/RENDER-006-animation-registry.md) — Animation configuration
- [MIG-004 Spec](../refactoring-plan/phase5-migration/MIG-004-process-flow-example.md) — Original task specification
