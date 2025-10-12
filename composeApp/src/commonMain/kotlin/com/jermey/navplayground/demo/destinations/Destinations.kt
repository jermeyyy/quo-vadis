package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.core.navigation.core.Destination

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

    data class Detail(val itemId: String) : MasterDetailDestination() {
        override val route = "master_detail_detail"
        override val arguments = mapOf("itemId" to itemId)
    }
}

/**
 * Tabs navigation destinations
 */
sealed class TabsDestination : Destination {
    object Main : TabsDestination() {
        override val route = "tabs_main"
    }

    data class Tab(val tabId: String) : TabsDestination() {
        override val route = "tabs_tab_$tabId"
        override val arguments = mapOf("tabId" to tabId)
    }

    data class SubItem(val tabId: String, val itemId: String) : TabsDestination() {
        override val route = "tabs_subitem"
        override val arguments = mapOf("tabId" to tabId, "itemId" to itemId)
    }
}

/**
 * Process/Wizard flow destinations
 */
sealed class ProcessDestination : Destination {
    object Start : ProcessDestination() {
        override val route = "process_start"
    }

    data class Step1(val userType: String? = null) : ProcessDestination() {
        override val route = "process_step1"
        override val arguments = userType?.let { mapOf("userType" to it) } ?: emptyMap()
    }

    data class Step2A(val data: String) : ProcessDestination() {
        override val route = "process_step2a"
        override val arguments = mapOf("data" to data)
    }

    data class Step2B(val data: String) : ProcessDestination() {
        override val route = "process_step2b"
        override val arguments = mapOf("data" to data)
    }

    data class Step3(val previousData: String, val branch: String) : ProcessDestination() {
        override val route = "process_step3"
        override val arguments = mapOf(
            "previousData" to previousData,
            "branch" to branch
        )
    }

    object Complete : ProcessDestination() {
        override val route = "process_complete"
    }
}
