package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Auth flow destinations - Demonstrates scope-aware stack navigation.
 *
 * This stack has a scopeKey automatically generated from the sealed class name ("AuthFlowDestination").
 * When navigating from within this stack to a destination outside the scope (e.g., MainTabs),
 * the navigation will go to the parent stack instead of staying inside AuthFlow.
 *
 * This demonstrates Phase 4 of Stack Scope Navigation:
 * - In-scope navigation: Login → Register stays within AuthFlow stack
 * - Out-of-scope navigation: AuthFlow → MainTabs navigates above the AuthFlow stack
 */
@Stack(name = "auth", startDestination = "Login")
sealed class AuthFlowDestination : Destination {
    @com.jermey.quo.vadis.annotations.Destination(route = "auth/login")
    data object Login : AuthFlowDestination()

    @com.jermey.quo.vadis.annotations.Destination(route = "auth/register")
    data object Register : AuthFlowDestination()

    @com.jermey.quo.vadis.annotations.Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlowDestination()
}
