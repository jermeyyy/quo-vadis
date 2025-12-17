import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/*
 * MVI integration example with navigation and result passing between destinations
 */

// Quo-Vadis APIs:

// Extension function to navigate to a destination and await a result, currently available navigate function should be left unchanged
suspend fun <Result> Navigator.navigate(destination: com.jermey.quo.vadis.core.navigation.core.Destination): Result? {
    // navigation on main thread
    return TODO("Return result of type Result after navigation is finished with navigateBack<Result>(resultValue)")
}

fun <Result> Navigator.navigateBack(result: Result) {
    // execute navigation back on main thread
    // pass result to previous destination and coroutine waiting for it
}

interface NavigationLifecycle {
    fun onEnter() // When navigation just starts
    fun onExit() // When navigation ends or navigates away
    fun onDestroy() // When the screen (destination) is being destroyed
}

fun Navigator.registerNavigationLifecycle(lifecycle: NavigationLifecycle, screenKey: String) {
    // register lifecycle callbacks for the specific screen
}

fun Navigator.unregisterNavigationLifecycle(lifecycle: NavigationLifecycle) {
    // unregister lifecycle callbacks
}

// API usage example:

// Base container class for all business logic with access to navigator, every screen will have its own container (like ViewModel)
// not to be confused with Containers in Quo-Vadis navigation (TabNode/PaneNode)
// screenKey should be obtained from LocalScreenNode.current?.key in Compose
// Container creates and owns its own CoroutineScope that survives navigation
abstract class BaseContainer<S, D>(
    val navigator: Navigator, // navigator provided by dependency injection
    screenKey: String, // obtained from LocalScreenNode.current?.key
) : NavigationLifecycle {

    // Container-owned scope - survives navigation, runs on Default dispatcher for business logic
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // register lifecycle callbacks with navigator to receive onEnter, onExit, onDestroy events
        navigator.registerNavigationLifecycle(this, screenKey)
    }


    override fun onEnter() {
        // Called when the container (and screen) becomes active
    }

    override fun onExit() {
        // Called when the container (and screen) is no longer active (ex not visible)
    }

    override fun onDestroy() {
        // Called when the container (screen) is being destroyed
        scope.cancel() // Cancel any ongoing coroutines when screen is destroyed
        // Unregister lifecycle callbacks
        navigator.unregisterNavigationLifecycle(this)
    }
}

@Destination
data class SomeDestination(
    val someParams: String
) : com.jermey.quo.vadis.core.navigation.core.Destination

data class SomeResult(val data: String)

class FooContainer(
    navigator: Navigator,
    screenKey: String,
) : BaseContainer<FooState, FooDestination>(navigator, screenKey) {

    suspend fun someLogic() {
        val result = scope.run {
            // Navigate to SomeDestination and wait for the result
            // Waiting for the navigation result on scope's Default dispatcher
            navigator.navigate<SomeResult>(SomeDestination("someParams"))
        }
        print(result)
    }
}

// Container for SomeDestination
class SomeContainer(
    navigator: Navigator,
    screenKey: String,
) : BaseContainer<SomeState, SomeDestination>(navigator, screenKey) {

    fun someLogic() {
        // Navigate back with a result
        navigator.navigateBack<SomeResult>(SomeResult("resultValue"))
    }
}