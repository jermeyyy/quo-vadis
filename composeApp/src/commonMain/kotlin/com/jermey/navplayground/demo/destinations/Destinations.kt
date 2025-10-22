package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

/**
 * Top-level destinations exposed by the demo application.
 */
sealed class DemoDestination : Destination {
    object Root : DemoDestination() {
        override val route = "demo_root"
    }
}

/**
 * Main bottom navigation destinations
 */
sealed class MainDestination(override val route: String) : Destination {
    object Home : MainDestination("home")
    object Explore : MainDestination("explore")
    object Profile : MainDestination("profile")
    object Settings : MainDestination("settings")
    object DeepLinkDemo : MainDestination("deeplink_demo")
}

/**
 * Master-Detail pattern destinations
 */
sealed class MasterDetailDestination : Destination {
    object List : MasterDetailDestination() {
        override val route = "master_detail_list"
    }

    data class Detail(val itemId: String) : MasterDetailDestination(), TypedDestination<DetailData> {
        companion object {
            const val ROUTE = "master_detail_detail"
        }
        override val route = ROUTE
        override val data = DetailData(itemId)
    }
}

/**
 * Serializable data for Detail destination.
 */
@Serializable
data class DetailData(val itemId: String)

/**
 * Tabs navigation destinations
 */
sealed class TabsDestination : Destination {
    object Main : TabsDestination() {
        override val route = "tabs_main"
    }

    /**
     * Serializable data for SubItem destination.
     */
    @Serializable
    data class SubItemData(val tabId: String, val itemId: String)

    data class Tab(val tabId: String) : TabsDestination() {
        override val route = "tabs_tab_$tabId"
        override val data = tabId
    }

    data class SubItem(val tabId: String, val itemId: String) : TabsDestination(), TypedDestination<SubItemData> {
        companion object {
            const val ROUTE = "tabs_subitem"
        }
        override val route = ROUTE
        override val data = SubItemData(tabId, itemId)
    }
}

/**
 * Process/Wizard flow destinations
 */
sealed class ProcessDestination : Destination {
    object Start : ProcessDestination() {
        override val route = "process_start"
    }

    /**
     * Serializable data for Step1 destination.
     */
    @Serializable
    data class Step1Data(val userType: String? = null)

    /**
     * Serializable data for Step2A/Step2B destinations.
     */
    @Serializable
    data class Step2Data(val stepData: String)

    /**
     * Serializable data for Step3 destination.
     */
    @Serializable
    data class Step3Data(val previousData: String, val branch: String)

    data class Step1(val userType: String? = null) : ProcessDestination(), TypedDestination<Step1Data> {
        companion object {
            const val ROUTE = "process_step1"
        }

        override val route = ROUTE
        override val data = Step1Data(userType)
    }

    data class Step2A(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        companion object {
            const val ROUTE = "process_step2a"
        }
        override val route = "process_step2a"
        override val data = Step2Data(stepData)
    }

    data class Step2B(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        companion object {
            const val ROUTE = "process_step2b"
        }
        override val route = "process_step2b"
        override val data = Step2Data(stepData)
    }

    data class Step3(val previousData: String, val branch: String) : ProcessDestination(), TypedDestination<Step3Data> {
        companion object {
            const val ROUTE = "process_step3"
        }
        override val route = "process_step3"
        override val data = Step3Data(previousData, branch)
    }

    object Complete : ProcessDestination() {
        override val route = "process_complete"
    }
}
