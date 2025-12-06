/**
 * # Stack Navigation Recipes
 *
 * Linear navigation patterns with push/pop semantics.
 *
 * ## When to Use
 *
 * - Settings screens with hierarchical navigation
 * - Single-path flows (onboarding, tutorials)
 * - Any navigation that follows a strict back-stack model
 *
 * ## Pattern Overview
 *
 * Stack navigation uses a simple push/pop model where each screen is placed
 * on top of a stack. The back action pops the top screen, returning to the previous one.
 *
 * ## Available Recipes
 *
 * - [SettingsStackRecipe] - A typical settings hierarchy with nested options
 *
 * @see com.jermey.quo.vadis.annotations.Stack
 */
package com.jermey.quo.vadis.recipes.stack
