# MIG-004: Process/Wizard Flow Example

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-004 |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | MIG-001 |
| **Output** | `docs/migration-examples/04-process-flow.md` |

## Objective

Demonstrate migration of multi-step process/wizard flows including sequential navigation, conditional branching, and flow completion with stack clearing.

## Patterns Demonstrated

| Pattern | Old API | New API |
|---------|---------|---------|
| Sequential steps | `navigate(NextStep)` | `navigate(NextStep)` |
| Conditional branch | `navigate(StepA)` or `navigate(StepB)` | Same |
| Complete & clear | `navigateAndClearTo(dest, route, inclusive)` | `navigator.navigateAndClear(dest)` |
| Pop to step | `popTo(Step, inclusive)` | `navigator.popTo(Step)` |
| Exit flow | `navigateBack()` multiple times | `navigator.exitFlow()` |

## Example Content Structure

### 1. Before (Old API)

```kotlin
// === Onboarding Flow Destinations ===
@Graph("onboarding", startDestination = "welcome")
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

// === Flow Navigation Logic ===
@Content(OnboardingDestination.Welcome::class)
@Composable
fun WelcomeContent(navigator: Navigator) {
    Column {
        Text("Welcome to MyApp!")
        Button(onClick = { 
            navigator.navigate(
                OnboardingDestination.UserTypeSelection,
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Get Started")
        }
    }
}

@Content(OnboardingDestination.UserTypeSelection::class)
@Composable
fun UserTypeContent(navigator: Navigator) {
    Column {
        Text("What type of user are you?")
        
        // Conditional branching
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

@Content(OnboardingDestination.Complete::class)
@Composable
fun CompleteContent(navigator: Navigator) {
    Column {
        Text("Setup Complete!")
        
        Button(onClick = {
            // Clear entire onboarding stack and go to main app
            navigator.navigateAndClearTo(
                destination = AppDestination.Home,
                upToRoute = "onboarding",  // String route
                inclusive = true
            )
        }) {
            Text("Enter App")
        }
    }
}

// === Cancel Flow ===
@Content(OnboardingDestination.PersonalInfo::class)
@Composable
fun PersonalInfoContent(navigator: Navigator) {
    Column {
        Text("Personal Information")
        
        Button(onClick = {
            navigator.navigate(OnboardingDestination.Preferences)
        }) {
            Text("Continue")
        }
        
        // Go back to user type selection
        TextButton(onClick = {
            navigator.popTo(OnboardingDestination.UserTypeSelection, inclusive = false)
        }) {
            Text("Change User Type")
        }
        
        // Cancel entire flow
        TextButton(onClick = {
            navigator.navigateBack()  // Or pop entire stack
        }) {
            Text("Cancel")
        }
    }
}
```

### 2. After (New API)

```kotlin
// === Onboarding Flow Destinations ===
@Stack(name = "onboarding", startDestination = "Welcome")
sealed class OnboardingDestination : Destination {
    
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

// === Screen Navigation Logic ===
@Screen(OnboardingDestination.Welcome::class)
@Composable
fun WelcomeScreen(navigator: Navigator) {
    Column {
        Text("Welcome to MyApp!")
        Button(onClick = { 
            // Transition configured in AnimationRegistry
            navigator.navigate(OnboardingDestination.UserTypeSelection)
        }) {
            Text("Get Started")
        }
    }
}

@Screen(OnboardingDestination.UserTypeSelection::class)
@Composable
fun UserTypeScreen(navigator: Navigator) {
    Column {
        Text("What type of user are you?")
        
        // Conditional branching - same pattern!
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

@Screen(OnboardingDestination.Complete::class)
@Composable
fun CompleteScreen(navigator: Navigator) {
    Column {
        Text("Setup Complete!")
        
        Button(onClick = {
            // Clear and navigate - uses destination class, not string route
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

// === Flow Control ===
@Screen(OnboardingDestination.PersonalInfo::class)
@Composable
fun PersonalInfoScreen(navigator: Navigator) {
    Column {
        Text("Personal Information")
        
        Button(onClick = {
            navigator.navigate(OnboardingDestination.Preferences)
        }) {
            Text("Continue")
        }
        
        // Type-safe popTo
        TextButton(onClick = {
            navigator.popTo(OnboardingDestination.UserTypeSelection)
        }) {
            Text("Change User Type")
        }
        
        // Exit entire flow
        TextButton(onClick = {
            navigator.exitFlow(OnboardingDestination::class)
        }) {
            Text("Cancel")
        }
    }
}
```

### 3. Key Migration Steps

1. **Rename annotations** - Same as MIG-001
2. **Replace string routes in `navigateAndClearTo`** - Use destination class reference
3. **Update `popTo`** - Use destination instance or class, not string
4. **Remove per-call transitions** - Configure via `AnimationRegistry`
5. **Use `exitFlow()`** - New convenience method for canceling multi-step flows

### 4. Flow Navigation Methods

```kotlin
// === Navigator Flow Methods ===

// Push to next step
navigator.navigate(OnboardingDestination.PersonalInfo)

// Pop to specific step
navigator.popTo(OnboardingDestination.UserTypeSelection)
navigator.popTo(OnboardingDestination.UserTypeSelection, inclusive = true)

// Clear stack and navigate
navigator.navigateAndClear(
    destination = AppDestination.Home,
    clearUpTo = OnboardingDestination::class,
    inclusive = true  // Remove onboarding root too
)

// Exit current flow entirely
navigator.exitFlow(OnboardingDestination::class)

// Check current position in flow
val currentStep = navigator.currentDestination
val canGoBack = navigator.canNavigateBack
val stackDepth = navigator.currentStackDepth
```

### 5. Conditional Flow Example

```kotlin
// === Wizard with Conditional Steps ===
@Stack(name = "checkout", startDestination = "Cart")
sealed class CheckoutDestination : Destination {
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
    
    @Destination(route = "checkout/confirmation/{orderId}")
    data class Confirmation(val orderId: String) : CheckoutDestination()
}

@Screen(CheckoutDestination.Shipping::class)
@Composable
fun ShippingScreen(navigator: Navigator, cartState: CartState) {
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
}

@Screen(CheckoutDestination.Confirmation::class)
@Composable
fun ConfirmationScreen(
    destination: CheckoutDestination.Confirmation,
    navigator: Navigator
) {
    Column {
        Text("Order ${destination.orderId} confirmed!")
        
        Button(onClick = {
            // Clear checkout flow, return to home
            navigator.navigateAndClear(
                destination = AppDestination.Home,
                clearUpTo = CheckoutDestination::class,
                inclusive = true
            )
        }) {
            Text("Continue Shopping")
        }
    }
}
```

### 6. Animation Configuration for Flows

```kotlin
// Wizard flows often use consistent forward/back animations
val checkoutAnimations = AnimationRegistry {
    // All forward navigation in checkout uses slide
    withinGraph(CheckoutDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Completion uses fade
    from(CheckoutDestination.Review::class)
        .to(CheckoutDestination.Confirmation::class)
        .uses(FadeThrough)
    
    // Exit to home uses fade
    from(CheckoutDestination.Confirmation::class)
        .to(AppDestination.Home::class)
        .uses(FadeThrough)
}
```

## Acceptance Criteria

- [ ] Sequential navigation example complete
- [ ] Conditional branching demonstrated
- [ ] `navigateAndClear` with type-safe destination shown
- [ ] `popTo` with type-safe destination shown
- [ ] `exitFlow` convenience method documented
- [ ] Animation configuration for flows included

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Using string routes in `navigateAndClear` | Use `::class` reference for type safety |
| Forgetting `inclusive` parameter | Defaults vary - be explicit |
| Complex conditional logic in navigation | Keep navigation simple, put logic in ViewModel |
| Deep back stack without exit option | Always provide "Cancel" or "Exit" option |

## Related Tasks

- [MIG-001: Simple Stack Navigation](./MIG-001-simple-stack-example.md)
- [MIG-002: Master-Detail Pattern](./MIG-002-master-detail-example.md)
- [CORE-002: TreeMutator Operations](../phase1-core/CORE-002-tree-mutator.md)
