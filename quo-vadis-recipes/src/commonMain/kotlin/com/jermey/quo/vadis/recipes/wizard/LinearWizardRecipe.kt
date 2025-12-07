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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// ============================================================
// MIG-004a: LINEAR WIZARD FLOW RECIPE
// ============================================================

/**
 * # Linear Wizard Flow Recipe
 *
 * Demonstrates sequential multi-step navigation for onboarding, setup wizards,
 * and any linear progression where users move through steps in order.
 *
 * ## What This Recipe Shows
 *
 * 1. **Sequential Navigation** - Step-by-step with `navigate(NextStep)`
 * 2. **Back Navigation** - Return to previous step with `navigateBack()`
 * 3. **Flow Completion** - Clear the wizard stack with `navigateAndClearTo()`
 * 4. **Progress Tracking** - Simple step counter in UI
 *
 * ## Flow Pattern
 *
 * ```
 * Welcome → UserType → PersonalInfo → Preferences → Complete
 *                                                      │
 *                                                      ▼
 *                                              (Clear & Exit)
 * ```
 *
 * ## Key Navigation Patterns
 *
 * | Action | Method | Purpose |
 * |--------|--------|---------|
 * | Next step | `navigate(NextDest)` | Push next wizard step |
 * | Previous step | `navigateBack()` | Pop current step |
 * | Complete flow | `navigateAndClearTo(HomeDest)` | Exit wizard, go to app |
 * | Cancel flow | `navigateBack()` repeated or `navigateAndClearAll()` | Abandon wizard |
 *
 * @see OnboardingDestination for destination definitions
 */

// ============================================================
// DESTINATION DEFINITION
// ============================================================

/**
 * Onboarding wizard destinations.
 *
 * A linear flow with 5 sequential steps.
 */
@Stack(name = "onboarding", startDestination = "Welcome")
sealed class OnboardingDestination : DestinationInterface {

    /** Step 1: Welcome screen - entry point */
    @Destination(route = "onboarding/welcome")
    data object Welcome : OnboardingDestination()

    /** Step 2: User type selection */
    @Destination(route = "onboarding/user_type")
    data object UserType : OnboardingDestination()

    /** Step 3: Personal information collection */
    @Destination(route = "onboarding/personal_info")
    data object PersonalInfo : OnboardingDestination()

    /** Step 4: Preferences configuration */
    @Destination(route = "onboarding/preferences")
    data object Preferences : OnboardingDestination()

    /** Step 5: Completion screen - exit point */
    @Destination(route = "onboarding/complete")
    data object Complete : OnboardingDestination()
}

/**
 * Main app destination (after onboarding).
 */
@Stack(name = "app", startDestination = "Home")
sealed class AppDestination : DestinationInterface {

    @Destination(route = "app/home")
    data object Home : AppDestination()
}

// ============================================================
// SCREENS
// ============================================================

/**
 * Step 1: Welcome screen.
 *
 * Pattern: Start of wizard - only forward navigation available.
 */
@Screen(OnboardingDestination.Welcome::class)
@Composable
fun WelcomeScreen(navigator: Navigator) {
    WizardStepLayout(
        step = 1,
        totalSteps = 5,
        title = "Welcome",
        subtitle = "Let's get you set up in a few steps.",
        onBack = null, // No back on first step
        onNext = { navigator.navigate(OnboardingDestination.UserType) }
    )
}

/**
 * Step 2: User type selection.
 *
 * Pattern: Mid-wizard step - both back and forward available.
 */
@Screen(OnboardingDestination.UserType::class)
@Composable
fun UserTypeScreen(navigator: Navigator) {
    WizardStepLayout(
        step = 2,
        totalSteps = 5,
        title = "User Type",
        subtitle = "What type of user are you?",
        onBack = { navigator.navigateBack() },
        onNext = { navigator.navigate(OnboardingDestination.PersonalInfo) }
    )
}

/**
 * Step 3: Personal information.
 *
 * Pattern: Mid-wizard step with data collection.
 */
@Screen(OnboardingDestination.PersonalInfo::class)
@Composable
fun PersonalInfoScreen(navigator: Navigator) {
    WizardStepLayout(
        step = 3,
        totalSteps = 5,
        title = "Personal Info",
        subtitle = "Enter your details.",
        onBack = { navigator.navigateBack() },
        onNext = { navigator.navigate(OnboardingDestination.Preferences) }
    )
}

/**
 * Step 4: Preferences.
 *
 * Pattern: Second-to-last step.
 */
@Screen(OnboardingDestination.Preferences::class)
@Composable
fun PreferencesScreen(navigator: Navigator) {
    WizardStepLayout(
        step = 4,
        totalSteps = 5,
        title = "Preferences",
        subtitle = "Configure your experience.",
        onBack = { navigator.navigateBack() },
        onNext = { navigator.navigate(OnboardingDestination.Complete) }
    )
}

/**
 * Step 5: Completion screen.
 *
 * Pattern: Flow completion - uses `navigateAndClearTo()` to exit wizard.
 *
 * ## Clearing the Wizard Stack
 *
 * When the wizard completes, we want to:
 * 1. Navigate to the main app
 * 2. Clear the entire onboarding stack
 * 3. Prevent user from navigating back into onboarding
 *
 * ```kotlin
 * // This clears the stack and navigates to Home
 * navigator.navigateAndClearTo(AppDestination.Home)
 * ```
 *
 * Note: `navigateAndClearTo` with clearRoute is deprecated.
 * Use `navigateAndClearAll()` or type-safe alternatives when available.
 */
@Screen(OnboardingDestination.Complete::class)
@Composable
fun CompleteScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Step 5 of 5",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "You're All Set!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        // PATTERN: Clear wizard stack and navigate to app
        // This prevents user from going back into onboarding
        Button(
            onClick = {
                @Suppress("DEPRECATION")
                navigator.navigateAndClearTo(AppDestination.Home)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter App")
        }
    }
}

// ============================================================
// SHARED UI COMPONENTS
// ============================================================

/**
 * Common layout for wizard steps.
 *
 * Provides consistent structure with:
 * - Progress indicator
 * - Title and subtitle
 * - Back/Next navigation buttons
 */
@Composable
private fun WizardStepLayout(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Step $step of $totalSteps",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { step.toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (step == totalSteps - 1) "Complete" else "Next")
            }
        }
    }
}

// ============================================================
// APP ENTRY POINT
// ============================================================

/**
 * Entry point for the Linear Wizard recipe.
 *
 * ## Production Setup
 *
 * ```kotlin
 * @Composable
 * fun OnboardingApp() {
 *     val navTree = remember { buildOnboardingNavNode() }  // KSP-generated
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
fun LinearWizardApp() {
    Text("LinearWizardApp - See KDoc for production implementation")
}
