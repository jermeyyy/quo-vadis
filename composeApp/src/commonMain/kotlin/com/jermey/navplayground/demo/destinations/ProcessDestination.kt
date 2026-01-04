package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Process/Wizard flow destinations
 */
@Stack(name = "process", startDestination = ProcessDestination.Start::class)
sealed class ProcessDestination : NavDestination {
    @Destination(route = "process/start")
    data object Start : ProcessDestination()

    @Destination(route = "process/step1")
    data class Step1(
        @Argument(optional = true) val userType: String? = null
    ) : ProcessDestination()

    @Destination(route = "process/step2a/{stepData}")
    data class Step2A(
        @Argument val stepData: String
    ) : ProcessDestination()

    @Destination(route = "process/step2b/{stepData}")
    data class Step2B(
        @Argument val stepData: String
    ) : ProcessDestination()

    @Destination(route = "process/step3/{previousData}/{branch}")
    data class Step3(
        @Argument val previousData: String,
        @Argument val branch: String
    ) : ProcessDestination()

    @Destination(route = "process/complete")
    data object Complete : ProcessDestination()
}
