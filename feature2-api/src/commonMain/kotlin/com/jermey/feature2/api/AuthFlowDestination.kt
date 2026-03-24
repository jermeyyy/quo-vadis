package com.jermey.feature2.api

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@Stack(name = "auth", startDestination = AuthFlowDestination.Login::class)
sealed class AuthFlowDestination : NavDestination {
    @Destination(route = "auth/login")
    data object Login : AuthFlowDestination()

    @Destination(route = "auth/register")
    data object Register : AuthFlowDestination()

    @Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlowDestination()
}
