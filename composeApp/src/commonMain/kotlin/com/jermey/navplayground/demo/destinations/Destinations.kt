package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Route
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.RouteRegistry
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

/**
 * Top-level destinations exposed by the demo application.
 */
sealed class DemoDestination : Destination {
    @Route("demo_root")
    object Root : DemoDestination()
}

/**
 * Main bottom navigation destinations
 */
@Graph("main")
sealed class MainDestination : Destination {
    @Route("main/home")
    data object Home : MainDestination()
    
    @Route("main/explore")
    data object Explore : MainDestination()
    
    @Route("main/profile")
    data object Profile : MainDestination()
    
    @Route("main/settings")
    data object Settings : MainDestination()
    
    @Route("main/deeplink_demo")
    data object DeepLinkDemo : MainDestination()
}

/**
 * Master-Detail pattern destinations
 */
@Graph("master_detail")
sealed class MasterDetailDestination : Destination {
    @Route("master_detail/list")
    data object List : MasterDetailDestination()

    @Route("master_detail/detail")
    @Argument(DetailData::class)
    data class Detail(val itemId: String) : MasterDetailDestination(), TypedDestination<DetailData> {
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
@Graph("tabs")
sealed class TabsDestination : Destination {
    @Route("tabs/main")
    data object Main : TabsDestination()

    /**
     * Serializable data for SubItem destination.
     */
    @Serializable
    data class SubItemData(val tabId: String, val itemId: String)

    // Dynamic route - register manually at creation
    data class Tab(val tabId: String) : TabsDestination() {
        init {
            RouteRegistry.register(this::class, "tabs_tab_$tabId")
        }
        override val data = tabId
    }

    @Route("tabs/subitem")
    @Argument(SubItemData::class)
    data class SubItem(val tabId: String, val itemId: String) : TabsDestination(), TypedDestination<SubItemData> {
        override val data = SubItemData(tabId, itemId)
    }
}

/**
 * Process/Wizard flow destinations
 */
@Graph("process")
sealed class ProcessDestination : Destination {
    @Route("process/start")
    data object Start : ProcessDestination()

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

    @Route("process/step1")
    @Argument(Step1Data::class)
    data class Step1(val userType: String? = null) : ProcessDestination(), TypedDestination<Step1Data> {
        override val data = Step1Data(userType)
    }

    @Route("process/step2a")
    @Argument(Step2Data::class)
    data class Step2A(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        override val data = Step2Data(stepData)
    }

    @Route("process/step2b")
    @Argument(Step2Data::class)
    data class Step2B(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        override val data = Step2Data(stepData)
    }

    @Route("process/step3")
    @Argument(Step3Data::class)
    data class Step3(val previousData: String, val branch: String) : ProcessDestination(), TypedDestination<Step3Data> {
        override val data = Step3Data(previousData, branch)
    }

    @Route("process/complete")
    data object Complete : ProcessDestination()
}
