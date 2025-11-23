# FlowMVI Integration

## Overview

Quo Vadis integrates seamlessly with [FlowMVI](https://github.com/respawn-app/FlowMVI) to provide a clean architecture for state management and navigation. The integration allows you to treat navigation as a **side-effect of your business logic**, keeping your UI layer clean and testable.

### What is FlowMVI?

FlowMVI is a Kotlin Multiplatform MVI (Model-View-Intent) framework built on Kotlin Flow. It provides:
- **Store** - State container with intent processing
- **Container** - Abstraction over Store with lifecycle management
- **Plugins** - Extensible architecture (logging, recovery, persistence)
- **Testing DSL** - First-class testing support

### Why Integrate with Navigation?

Traditional approach mixes navigation with UI:

```kotlin
// ❌ Navigation logic in UI layer
@Composable
fun ProfileScreen(navigator: Navigator) {
    Button(onClick = { navigator.navigate(Settings) }) {
        Text("Settings")
    }
}
```

FlowMVI integration centralizes navigation in business logic:

```kotlin
// ✅ Navigation as business logic
@Composable
fun ProfileScreen(container: ProfileContainer) {
    Button(onClick = { container.intent(ProfileIntent.OpenSettings) }) {
        Text("Settings")
    }
}

class ProfileContainer(private val navigator: Navigator) {
    reduce { intent ->
        when (intent) {
            ProfileIntent.OpenSettings -> navigator.navigate(Settings)
        }
    }
}
```

### Key Benefits

- 🧪 **Testable** - Test navigation logic without UI
- 🔄 **Predictable** - Navigation as pure side-effect
- 🎯 **Centralized** - All navigation logic in one place
- 📦 **Decoupled** - UI doesn't know about destinations
- 🏗️ **Scalable** - Easy to maintain as app grows

## Installation

### 1. Add Dependencies

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Quo Vadis with FlowMVI
                implementation("io.github.jermeyyy:quo-vadis-core:0.1.0")
                implementation("io.github.jermeyyy:quo-vadis-core-flow-mvi:0.1.0")
                
                // FlowMVI
                implementation("pro.respawn.flowmvi:core:2.6.1")
                implementation("pro.respawn.flowmvi:compose:2.6.1")
                
                // Optional: For testing
                testImplementation("pro.respawn.flowmvi:test:2.6.1")
            }
        }
    }
}
```

### 2. Set Up Dependency Injection (Optional but Recommended)

```kotlin
// Using Koin
val profileModule = module {
    factory { (navigator: Navigator) ->
        ProfileContainer(
            navigator = navigator,
            repository = get()
        )
    }
}
```

## Core Concepts

### State, Intent, Action

```kotlin
// State - What the UI displays
sealed interface ProfileState : MVIState {
    data object Loading : ProfileState
    data class Content(val user: User, val isEditing: Boolean = false) : ProfileState
    data class Error(val message: String) : ProfileState
}

// Intent - What the user wants to do
sealed interface ProfileIntent : MVIIntent {
    data object LoadProfile : ProfileIntent
    data object StartEditing : ProfileIntent
    data class UpdateName(val name: String) : ProfileIntent
    data object SaveChanges : ProfileIntent
    data object NavigateToSettings : ProfileIntent
    data object NavigateBack : ProfileIntent
}

// Action - Side effects (navigation, toasts, etc.)
sealed interface ProfileAction : MVIAction {
    data class ShowToast(val message: String) : ProfileAction
    data class ShowError(val error: String) : ProfileAction
    data object ProfileSaved : ProfileAction
}
```

### Container with Navigator

```kotlin
class ProfileContainer(
    private val navigator: Navigator,
    private val repository: ProfileRepository,
    private val debuggable: Boolean = false
) : Container<ProfileState, ProfileIntent, ProfileAction> {
    
    override val store: Store<ProfileState, ProfileIntent, ProfileAction> = store(
        initial = ProfileState.Loading
    ) {
        configure {
            debuggable = this@ProfileContainer.debuggable
            name = "ProfileStore"
            parallelIntents = false
        }
        
        // Initialize on start
        init {
            intent(ProfileIntent.LoadProfile)
        }
        
        // Handle intents
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> handleLoadProfile()
                is ProfileIntent.NavigateToSettings -> handleNavigateToSettings()
                is ProfileIntent.NavigateBack -> handleNavigateBack()
                // ... other intents
            }
        }
        
        // Handle errors
        recover { exception ->
            action(ProfileAction.ShowError(exception.message ?: "Unknown error"))
            updateState { ProfileState.Error(exception.message ?: "An error occurred") }
            null // Suppress exception
        }
        
        if (debuggable) {
            enableLogging()
        }
    }
    
    private suspend fun PipelineContext<ProfileState, ProfileIntent, ProfileAction>.handleLoadProfile() {
        updateState { ProfileState.Loading }
        try {
            val user = repository.getUser()
            updateState { ProfileState.Content(user = user) }
        } catch (e: Exception) {
            throw e // Let recover handle it
        }
    }
    
    private suspend fun PipelineContext<ProfileState, ProfileIntent, ProfileAction>.handleNavigateToSettings() {
        // Navigation as side-effect
        navigator.navigate(SettingsDestination)
    }
    
    private suspend fun PipelineContext<ProfileState, ProfileIntent, ProfileAction>.handleNavigateBack() {
        navigator.navigateBack()
    }
}
```

## Usage in UI

### StoreScreen Pattern

The `quo-vadis-core-flow-mvi` module provides convenient composables:

```kotlin
@Composable
fun ProfileScreen(
    navigator: Navigator = koinInject(),
    container: ProfileContainer = koinInject { parametersOf(navigator) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // StoreScreen handles state subscription and actions
    StoreScreen(
        container = container,
        onAction = { action ->
            scope.launch {
                when (action) {
                    is ProfileAction.ShowToast -> {
                        snackbarHostState.showSnackbar(action.message)
                    }
                    is ProfileAction.ShowError -> {
                        snackbarHostState.showSnackbar(
                            message = action.error,
                            duration = SnackbarDuration.Long
                        )
                    }
                    // ... handle other actions
                }
            }
        }
    ) { state, intentReceiver ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (state) {
                    is ProfileState.Loading -> LoadingView()
                    is ProfileState.Content -> ProfileContent(
                        user = state.user,
                        isEditing = state.isEditing,
                        onIntent = intentReceiver::intent
                    )
                    is ProfileState.Error -> ErrorView(
                        message = state.message,
                        onRetry = { intentReceiver.intent(ProfileIntent.LoadProfile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    isEditing: Boolean,
    onIntent: (ProfileIntent) -> Unit
) {
    Column {
        Text("Name: ${user.name}")
        Text("Email: ${user.email}")
        
        if (!isEditing) {
            Button(onClick = { onIntent(ProfileIntent.StartEditing) }) {
                Text("Edit Profile")
            }
            Button(onClick = { onIntent(ProfileIntent.NavigateToSettings) }) {
                Text("Settings")
            }
        } else {
            // Edit form...
            Button(onClick = { onIntent(ProfileIntent.SaveChanges) }) {
                Text("Save")
            }
        }
    }
}
```

### Manual Subscription

For more control, subscribe manually:

```kotlin
@Composable
fun ProfileScreen(container: ProfileContainer) {
    with(container.store) {
        val state by subscribe { action ->
            // Handle actions
            when (action) {
                is ProfileAction.ShowToast -> { /* ... */ }
            }
        }
        
        // Render based on state
        when (state) {
            is ProfileState.Loading -> LoadingView()
            is ProfileState.Content -> ProfileContent(
                state = state as ProfileState.Content,
                onIntent = ::intent
            )
            // ...
        }
    }
}
```

## Complete Example

### 1. Define Contract

```kotlin
// State
sealed interface SettingsState : MVIState {
    data object Loading : SettingsState
    data class Content(
        val theme: Theme,
        val notificationsEnabled: Boolean,
        val language: String
    ) : SettingsState
}

// Intents
sealed interface SettingsIntent : MVIIntent {
    data object LoadSettings : SettingsIntent
    data class ChangeTheme(val theme: Theme) : SettingsIntent
    data class ToggleNotifications(val enabled: Boolean) : SettingsIntent
    data class ChangeLanguage(val language: String) : SettingsIntent
    data object NavigateBack : SettingsIntent
    data object NavigateToAbout : SettingsIntent
}

// Actions
sealed interface SettingsAction : MVIAction {
    data class ShowToast(val message: String) : SettingsAction
    data object SettingsSaved : SettingsAction
}
```

### 2. Create Container

```kotlin
class SettingsContainer(
    private val navigator: Navigator,
    private val repository: SettingsRepository
) : Container<SettingsState, SettingsIntent, SettingsAction> {
    
    override val store = store<SettingsState, SettingsIntent, SettingsAction>(
        initial = SettingsState.Loading
    ) {
        configure {
            name = "SettingsStore"
            parallelIntents = false
        }
        
        init {
            intent(SettingsIntent.LoadSettings)
        }
        
        reduce { intent ->
            when (intent) {
                is SettingsIntent.LoadSettings -> {
                    updateState { SettingsState.Loading }
                    val settings = repository.getSettings()
                    updateState {
                        SettingsState.Content(
                            theme = settings.theme,
                            notificationsEnabled = settings.notificationsEnabled,
                            language = settings.language
                        )
                    }
                }
                
                is SettingsIntent.ChangeTheme -> {
                    val current = state as? SettingsState.Content ?: return@reduce
                    updateState { current.copy(theme = intent.theme) }
                    repository.saveTheme(intent.theme)
                    action(SettingsAction.ShowToast("Theme changed"))
                }
                
                is SettingsIntent.ToggleNotifications -> {
                    val current = state as? SettingsState.Content ?: return@reduce
                    updateState { current.copy(notificationsEnabled = intent.enabled) }
                    repository.saveNotificationsEnabled(intent.enabled)
                    action(SettingsAction.ShowToast("Notifications ${if (intent.enabled) "enabled" else "disabled"}"))
                }
                
                is SettingsIntent.NavigateBack -> {
                    navigator.navigateBack()
                }
                
                is SettingsIntent.NavigateToAbout -> {
                    navigator.navigate(AboutDestination)
                }
            }
        }
        
        recover { exception ->
            action(SettingsAction.ShowToast("Error: ${exception.message}"))
            null
        }
    }
}
```

### 3. Use in UI

```kotlin
@Composable
fun SettingsScreen(
    navigator: Navigator = koinInject(),
    container: SettingsContainer = koinInject { parametersOf(navigator) }
) {
    StoreScreen(
        container = container,
        onAction = { action ->
            when (action) {
                is SettingsAction.ShowToast -> {
                    // Show toast
                }
                SettingsAction.SettingsSaved -> {
                    // Handle saved
                }
            }
        }
    ) { state, intentReceiver ->
        when (state) {
            SettingsState.Loading -> CircularProgressIndicator()
            is SettingsState.Content -> SettingsContent(
                state = state,
                onIntent = intentReceiver::intent
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState.Content,
    onIntent: (SettingsIntent) -> Unit
) {
    Column {
        // Theme selector
        DropdownMenu(
            selected = state.theme,
            onSelect = { theme -> onIntent(SettingsIntent.ChangeTheme(theme)) }
        )
        
        // Notifications toggle
        Switch(
            checked = state.notificationsEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleNotifications(it)) }
        )
        
        // About button
        Button(onClick = { onIntent(SettingsIntent.NavigateToAbout) }) {
            Text("About")
        }
    }
}
```

### 4. Dependency Injection

```kotlin
val settingsModule = module {
    single { SettingsRepository() }
    
    factory { (navigator: Navigator) ->
        SettingsContainer(
            navigator = navigator,
            repository = get()
        )
    }
}
```

## Testing

### Testing Navigation Logic

```kotlin
class ProfileContainerTest {
    
    @Test
    fun `navigateToSettings should call navigator`() = runTest {
        // Arrange
        val navigator = FakeNavigator()
        val repository = FakeProfileRepository()
        val container = ProfileContainer(navigator, repository, debuggable = false)
        
        // Act
        container.store.subscribeAndTest {
            container.store.intent(ProfileIntent.NavigateToSettings)
            
            // Assert
            assertEquals(SettingsDestination, navigator.lastDestination)
        }
    }
    
    @Test
    fun `loadProfile should update state to content`() = runTest {
        val navigator = FakeNavigator()
        val repository = FakeProfileRepository().apply {
            user = User(name = "John", email = "john@example.com")
        }
        val container = ProfileContainer(navigator, repository)
        
        container.store.subscribeAndTest {
            // Wait for init to load profile
            val state = awaitItem() as ProfileState.Content
            
            assertEquals("John", state.user.name)
            assertEquals("john@example.com", state.user.email)
        }
    }
}
```

### Using FlowMVI Test DSL

```kotlin
@Test
fun `test complete flow`() = runTest {
    val navigator = FakeNavigator()
    val repository = FakeProfileRepository()
    val container = ProfileContainer(navigator, repository)
    
    container.store.subscribeAndTest {
        // Initial loading state
        awaitItem() shouldBe ProfileState.Loading
        
        // Content loaded
        val content = awaitItem() as ProfileState.Content
        content.user.name shouldBe "Test User"
        
        // Start editing
        container.store.intent(ProfileIntent.StartEditing)
        (awaitItem() as ProfileState.Content).isEditing shouldBe true
        
        // Navigate to settings
        container.store.intent(ProfileIntent.NavigateToSettings)
        navigator.lastDestination shouldBe SettingsDestination
    }
}
```

## Advanced Patterns

### Navigation with Arguments

```kotlin
sealed interface ProductIntent : MVIIntent {
    data class NavigateToDetails(val productId: String) : ProductIntent
}

class ProductContainer(private val navigator: Navigator) {
    reduce { intent ->
        when (intent) {
            is ProductIntent.NavigateToDetails -> {
                navigator.navigate(ProductDetailsDestination(intent.productId))
            }
        }
    }
}
```

### Conditional Navigation

```kotlin
reduce { intent ->
    when (intent) {
        is ProfileIntent.SaveChanges -> {
            val current = state as? ProfileState.Content ?: return@reduce
            
            try {
                repository.saveProfile(current.user)
                action(ProfileAction.ShowToast("Profile saved"))
                
                // Navigate back only on success
                navigator.navigateBack()
            } catch (e: ValidationException) {
                action(ProfileAction.ShowError(e.message))
                // Don't navigate on error
            }
        }
    }
}
```

### Navigation with Result

```kotlin
reduce { intent ->
    when (intent) {
        ProfileIntent.NavigateToEditPhoto -> {
            val result = navigator.navigateForResult<PhotoResult>(EditPhotoDestination)
            
            result?.let { photo ->
                updateState { 
                    (state as ProfileState.Content).copy(
                        user = user.copy(photoUrl = photo.url)
                    )
                }
            }
        }
    }
}
```

## Best Practices

### ✅ DO:

- **Centralize navigation** - Keep all navigation in Container, not UI
- **Use meaningful intents** - `OpenSettings` instead of `Navigate`
- **Handle errors gracefully** - Use `recover` plugin
- **Test navigation logic** - Use `FakeNavigator` in tests
- **Inject Navigator** - Pass via constructor for testability
- **Use actions for side effects** - Toasts, analytics, etc.

### ❌ DON'T:

- **Don't navigate from UI** - Always use intents
- **Don't skip error handling** - Use `recover` plugin
- **Don't ignore actions** - Handle all side effects
- **Don't hardcode destinations** - Use sealed classes
- **Don't mix concerns** - Keep business logic in Container

## API Reference

### StoreScreen

```kotlin
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreScreen(
    container: Container<S, I, A>,
    onAction: suspend (A) -> Unit = {},
    content: @Composable (state: S, intentReceiver: IntentReceiver<I>) -> Unit
)
```

Automatic state subscription with action handling.

### StoreContent

```kotlin
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreContent(
    container: Container<S, I, A>,
    content: @Composable (state: S, intentReceiver: IntentReceiver<I>) -> Unit
)
```

Lightweight wrapper without action handling.

## Common Issues

### Navigator not injected

**Problem:** `NullPointerException` when navigating.

**Solution:** Ensure Navigator is passed to Container:

```kotlin
factory { (navigator: Navigator) ->
    ProfileContainer(navigator = navigator, repository = get())
}
```

### State not updating

**Problem:** UI doesn't reflect state changes.

**Solution:** Use `updateState` in reduce block:

```kotlin
reduce { intent ->
    updateState { newState }  // ✅ Correct
    // state = newState       // ❌ Wrong
}
```

### Actions not received

**Problem:** Actions not triggering in UI.

**Solution:** Handle actions in `onAction` callback:

```kotlin
StoreScreen(
    container = container,
    onAction = { action ->  // ✅ Must handle
        when (action) { /* ... */ }
    }
) { /* ... */ }
```

## See Also

- [FlowMVI Documentation](https://github.com/respawn-app/FlowMVI)
- [API Reference](API_REFERENCE.md)
- [Architecture Guide](ARCHITECTURE.md)
- [Testing Guide](../README.md#testing)
