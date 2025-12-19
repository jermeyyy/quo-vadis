package com.jermey.feature2

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.NavDestination

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
@Stack(name = "auth", startDestination = AuthFlowDestination.Login::class)
sealed class AuthFlowDestination : NavDestination {
    @Destination(route = "auth/login")
    data object Login : AuthFlowDestination()

    @Destination(route = "auth/register")
    data object Register : AuthFlowDestination()

    @Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlowDestination()
}