/**
 * # Wizard/Process Flow Recipes
 *
 * Multi-step flows with linear progression and conditional branching.
 *
 * ## When to Use
 *
 * - Onboarding flows
 * - Checkout processes
 * - Multi-step forms
 * - Any guided process with sequential steps
 *
 * ## Pattern Overview
 *
 * Wizard flows guide users through a series of steps. They can be linear
 * (fixed sequence) or branching (steps depend on previous choices).
 * Data is typically accumulated across steps.
 *
 * ## Available Recipes
 *
 * - [LinearWizardRecipe] - Simple step-by-step flow with fixed sequence
 * - [BranchingWizardRecipe] - Flow with conditional paths based on user choices
 *
 * @see com.jermey.quo.vadis.annotations.Stack
 */
package com.jermey.quo.vadis.recipes.wizard
