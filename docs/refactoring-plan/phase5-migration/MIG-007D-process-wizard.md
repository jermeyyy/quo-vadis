# MIG-007D: Process/Wizard Flow Migration

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007D |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 hours |
| **Dependencies** | MIG-007A (Foundation Destinations) |
| **Output** | Migrated process/wizard screens with `@Screen` bindings |

## Objective

Migrate all **process/wizard flow screens** to `@Screen` annotation bindings and update navigation calls to use type-safe stack clearing with `navigateAndClear()` using KClass references.

This subtask focuses on the **branching wizard pattern** where the flow branches based on user selection (Personal vs Business account type).

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/
├── ProcessStartScreen.kt      # Wizard entry point
├── ProcessStep1Screen.kt      # Account type selection (branching point)
├── ProcessStep2AScreen.kt     # Personal account path
├── ProcessStep2BScreen.kt     # Business account path
├── ProcessStep3Screen.kt      # Review step (merges branches)
└── ProcessCompleteScreen.kt   # Completion with stack clearing
```

### Reference Recipes

| Recipe | Pattern |
|--------|---------|
| [MIG-004](./MIG-004-process-wizard-recipe.md) | Process/Wizard Flow patterns |
| [LinearWizardRecipe](../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/LinearWizardRecipe.kt) | Sequential wizard steps |
| [BranchingWizardRecipe](../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/BranchingWizardRecipe.kt) | Conditional branching |

---

## Process Flow Diagram

```
                                   ┌─────────────────┐
                                   │  ProcessStart   │
                                   │   (Entry)       │
                                   └────────┬────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │  ProcessStep1   │
                                   │ (Account Type)  │
                                   └────────┬────────┘
                                            │
                        ┌───────────────────┴───────────────────┐
                        │ accountType == "personal"             │ accountType == "business"
                        ▼                                       ▼
               ┌─────────────────┐                     ┌─────────────────┐
               │  ProcessStep2A  │                     │  ProcessStep2B  │
               │   (Personal)    │                     │   (Business)    │
               └────────┬────────┘                     └────────┬────────┘
                        │                                       │
                        └───────────────────┬───────────────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │  ProcessStep3   │
                                   │    (Review)     │
                                   └────────┬────────┘
                                            │
                                            ▼
                                   ┌─────────────────┐
                                   │ ProcessComplete │
                                   │   (Clear Stack) │
                                   └─────────────────┘
```

---

## Migration Steps

### Step 1: ProcessStartScreen.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessStartScreen.kt`

This is a simple entry screen with no parameters - receives callbacks only.

```kotlin
// OLD - Callback-based (current)
@Composable
fun ProcessStartScreen(
    onStart: () -> Unit,
    onCancel: () -> Unit
)

// NEW - @Screen binding with Navigator
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination

@Screen(ProcessDestination.Start::class)
@Composable
fun ProcessStartScreen(navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Process") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ProcessStartContent(
                onStart = { navigator.navigate(ProcessDestination.Step1()) }
            )
        }
    }
}
```

**Key Changes:**
- Add `@Screen(ProcessDestination.Start::class)` annotation
- Replace callbacks with `navigator: Navigator` parameter
- `onCancel` → `navigator.navigateBack()`
- `onStart` → `navigator.navigate(ProcessDestination.Step1())`

---

### Step 2: ProcessStep1Screen.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessStep1Screen.kt`

This is the **branching point** - routes to Step2A or Step2B based on account type.

```kotlin
// OLD - Callback-based with parameters
@Composable
fun ProcessStep1Screen(
    initialUserType: String?,
    onNext: (userType: String, data: String) -> Unit,
    onBack: () -> Unit
)

// NEW - @Screen binding with destination instance
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination

@Screen(ProcessDestination.Step1::class)
@Composable
fun ProcessStep1Screen(
    destination: ProcessDestination.Step1,
    navigator: Navigator
) {
    var selectedType by remember { mutableStateOf(destination.accountType ?: "personal") }
    var name by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 1: Account Type") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // ... content
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    // CONDITIONAL BRANCHING: Navigate based on account type
                    val nextDestination = if (selectedType == "personal") {
                        ProcessDestination.Step2A(
                            accountType = selectedType,
                            userName = name
                        )
                    } else {
                        ProcessDestination.Step2B(
                            accountType = selectedType,
                            userName = name
                        )
                    }
                    navigator.navigate(nextDestination)
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank()
            ) {
                Text("Next")
            }
        }
    }
}
```

**Key Changes:**
- Add `@Screen(ProcessDestination.Step1::class)` annotation
- Receive `destination: ProcessDestination.Step1` for initial state
- Replace `initialUserType` with `destination.accountType`
- Conditional navigation to `Step2A` or `Step2B` based on `selectedType`

---

### Step 3: ProcessStep2AScreen.kt (Personal Account Path)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessStep2AScreen.kt`

```kotlin
// OLD - Callback-based
@Composable
fun ProcessStep2AScreen(
    previousData: String,
    onNext: (data: String) -> Unit,
    onBack: () -> Unit
)

// NEW - @Screen binding
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination

@Screen(ProcessDestination.Step2A::class)
@Composable
fun ProcessStep2AScreen(
    destination: ProcessDestination.Step2A,
    navigator: Navigator
) {
    var email by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Personal Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // ... content using destination.userName for greeting
        
        Text(
            "Hello ${destination.userName}! Please provide your personal details.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // ... form fields
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    navigator.navigate(
                        ProcessDestination.Step3(
                            accountType = destination.accountType,
                            userName = destination.userName,
                            email = email,
                            birthdate = birthdate
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = email.isNotBlank() && birthdate.isNotBlank()
            ) {
                Text("Next")
            }
        }
    }
}
```

**Key Changes:**
- Add `@Screen(ProcessDestination.Step2A::class)` annotation
- Receive `destination: ProcessDestination.Step2A` with `accountType` and `userName`
- Access data via `destination.userName`, `destination.accountType`
- Navigate to `Step3` with accumulated data

---

### Step 4: ProcessStep2BScreen.kt (Business Account Path)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessStep2BScreen.kt`

```kotlin
// OLD - Callback-based
@Composable
fun ProcessStep2BScreen(
    previousData: String,
    onNext: (data: String) -> Unit,
    onBack: () -> Unit
)

// NEW - @Screen binding
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination

@Screen(ProcessDestination.Step2B::class)
@Composable
fun ProcessStep2BScreen(
    destination: ProcessDestination.Step2B,
    navigator: Navigator
) {
    var companyName by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Business Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // ... content using destination.userName for greeting
        
        Text(
            "Hello ${destination.userName}! Please provide your business details.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // ... form fields
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    navigator.navigate(
                        ProcessDestination.Step3(
                            accountType = destination.accountType,
                            userName = destination.userName,
                            companyName = companyName,
                            taxId = taxId,
                            industry = industry
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = companyName.isNotBlank() && taxId.isNotBlank()
            ) {
                Text("Next")
            }
        }
    }
}
```

**Key Changes:**
- Add `@Screen(ProcessDestination.Step2B::class)` annotation
- Receive `destination: ProcessDestination.Step2B` with business context
- Navigate to `Step3` with business-specific accumulated data

---

### Step 5: ProcessStep3Screen.kt (Review)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessStep3Screen.kt`

This step merges both branches and shows review data based on account type.

```kotlin
// OLD - Callback-based with string parsing
@Composable
fun ProcessStep3Screen(
    previousData: String,
    branch: String,
    onComplete: () -> Unit,
    onBack: () -> Unit
)

// NEW - @Screen binding with structured data
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination

@Screen(ProcessDestination.Step3::class)
@Composable
fun ProcessStep3Screen(
    destination: ProcessDestination.Step3,
    navigator: Navigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 3: Review") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProcessStepIndicator(currentStep = 3, totalSteps = 4)

                Text(
                    "Review Your Information",
                    style = MaterialTheme.typography.headlineMedium
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Account Type",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            destination.accountType.uppercase(),
                            style = MaterialTheme.typography.titleMedium
                        )

                        HorizontalDivider()

                        ReviewRow("Name", destination.userName)

                        // Show fields based on account type
                        if (destination.accountType == "personal") {
                            ReviewRow("Email", destination.email)
                            ReviewRow("Birth Date", destination.birthdate)
                        } else {
                            ReviewRow("Company", destination.companyName)
                            ReviewRow("Tax ID", destination.taxId)
                            ReviewRow("Industry", destination.industry)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { navigator.navigateBack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        navigator.navigate(ProcessDestination.Complete)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Complete")
                }
            }
        }
    }
}
```

**Key Changes:**
- Add `@Screen(ProcessDestination.Step3::class)` annotation
- Receive `destination: ProcessDestination.Step3` with all accumulated data
- No more string parsing - access structured properties directly
- Branch detection via `destination.accountType`

---

### Step 6: ProcessCompleteScreen.kt (Stack Clearing)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/ProcessCompleteScreen.kt`

This is the **critical migration point** for type-safe stack clearing.

```kotlin
// OLD - Callback-based
@Composable
fun ProcessCompleteScreen(
    onDone: () -> Unit,
    onRestart: () -> Unit
)

// OLD Navigation Call (in parent/host):
navigator.navigateAndClearTo(
    destination = AppDestination.Home,
    upToRoute = "process/start",
    inclusive = true
)

// NEW - @Screen binding with type-safe navigation
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.AppDestination

@Screen(ProcessDestination.Complete::class)
@Composable
fun ProcessCompleteScreen(navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Complete") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Setup Complete!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Your account has been successfully configured.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            // TYPE-SAFE STACK CLEARING
            Button(
                onClick = {
                    // Clear up to ProcessDestination.Start and navigate to Home
                    navigator.navigateAndClear(
                        destination = AppDestination.MainTabs,
                        clearUpTo = ProcessDestination.Start::class,
                        inclusive = true
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Home")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    // Pop to start to restart the wizard
                    navigator.popTo(ProcessDestination.Start::class, inclusive = false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Over")
            }
        }
    }
}
```

**Key Transformations:**

```kotlin
// OLD: String-based navigation
navigator.navigateAndClearTo(
    destination = AppDestination.Home,
    upToRoute = "process/start",
    inclusive = true
)

// NEW: Type-safe navigation with KClass
navigator.navigateAndClear(
    destination = AppDestination.MainTabs,
    clearUpTo = ProcessDestination.Start::class,
    inclusive = true
)
```

```kotlin
// OLD: String-based popTo
navigator.popTo(route = "process/start", inclusive = false)

// NEW: Type-safe popTo with KClass
navigator.popTo(ProcessDestination.Start::class, inclusive = false)
```

---

## Updated ProcessDestination Definition

Ensure MIG-007A defines `ProcessDestination` with all required parameters:

```kotlin
@Stack(name = "process", startDestination = "Start")
sealed class ProcessDestination : DestinationInterface {
    
    @Destination(route = "process/start")
    data object Start : ProcessDestination()
    
    @Destination(route = "process/step1")
    data class Step1(
        @Argument val accountType: String? = null
    ) : ProcessDestination()
    
    @Destination(route = "process/step2a/{accountType}/{userName}")
    data class Step2A(
        @Argument val accountType: String,
        @Argument val userName: String
    ) : ProcessDestination()
    
    @Destination(route = "process/step2b/{accountType}/{userName}")
    data class Step2B(
        @Argument val accountType: String,
        @Argument val userName: String
    ) : ProcessDestination()
    
    @Destination(route = "process/step3/{accountType}/{userName}")
    data class Step3(
        @Argument val accountType: String,
        @Argument val userName: String,
        // Personal account fields
        val email: String = "",
        val birthdate: String = "",
        // Business account fields
        val companyName: String = "",
        val taxId: String = "",
        val industry: String = ""
    ) : ProcessDestination()
    
    @Destination(route = "process/complete")
    data object Complete : ProcessDestination()
}
```

---

## Navigation Pattern Summary

| Action | Old API | New API |
|--------|---------|---------|
| Start wizard | `onStart()` callback | `navigator.navigate(ProcessDestination.Step1())` |
| Next step | `onNext(data)` callback | `navigator.navigate(ProcessDestination.Step2A(...))` |
| Back step | `onBack()` callback | `navigator.navigateBack()` |
| Complete & exit | `navigateAndClearTo(dest, upToRoute, inclusive)` | `navigator.navigateAndClear(dest, clearUpTo::class, inclusive)` |
| Restart wizard | `popTo(route, inclusive)` | `navigator.popTo(Destination::class, inclusive)` |
| Conditional branch | Callback with type string | Direct navigate to `Step2A` or `Step2B` |

---

## Checklist

### Screen Bindings
- [ ] Add `@Screen(ProcessDestination.Start::class)` to `ProcessStartScreen`
- [ ] Add `@Screen(ProcessDestination.Step1::class)` to `ProcessStep1Screen`
- [ ] Add `@Screen(ProcessDestination.Step2A::class)` to `ProcessStep2AScreen`
- [ ] Add `@Screen(ProcessDestination.Step2B::class)` to `ProcessStep2BScreen`
- [ ] Add `@Screen(ProcessDestination.Step3::class)` to `ProcessStep3Screen`
- [ ] Add `@Screen(ProcessDestination.Complete::class)` to `ProcessCompleteScreen`

### Parameter Migration
- [ ] `ProcessStep1Screen` receives `destination: ProcessDestination.Step1`
- [ ] `ProcessStep2AScreen` receives `destination: ProcessDestination.Step2A`
- [ ] `ProcessStep2BScreen` receives `destination: ProcessDestination.Step2B`
- [ ] `ProcessStep3Screen` receives `destination: ProcessDestination.Step3`
- [ ] Remove string parsing from `ProcessStep3Screen`

### Navigation API Updates
- [ ] All `onBack()` → `navigator.navigateBack()`
- [ ] All `onNext()` → `navigator.navigate(DestinationClass(...))`
- [ ] `navigateAndClearTo()` → `navigateAndClear()` with KClass
- [ ] `popTo(route)` → `popTo(Destination::class)`

### Conditional Branching
- [ ] Step1 correctly branches to Step2A for "personal"
- [ ] Step1 correctly branches to Step2B for "business"
- [ ] Step3 displays correct fields based on `accountType`

### Cleanup
- [ ] Remove all callback parameters from screen functions
- [ ] Remove pipe-delimited string data passing
- [ ] Remove string-based route references

---

## Verification

```bash
# Compile check
./gradlew :composeApp:compileKotlinMetadata

# Check for remaining legacy patterns
grep -r "onNext\|onBack\|onComplete\|onStart\|onCancel" \
  composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/

grep -r "navigateAndClearTo" \
  composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/

grep -r "upToRoute" \
  composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/process/

# Should return empty results after migration
```

---

## Testing Scenarios

### Happy Path - Personal Account
1. Start → Step1 (select "personal", enter name) → Step2A (enter email, birthdate) → Step3 (review personal data) → Complete → Home

### Happy Path - Business Account
1. Start → Step1 (select "business", enter name) → Step2B (enter company, taxId, industry) → Step3 (review business data) → Complete → Home

### Back Navigation
1. Navigate to Step3, press Back, verify returns to Step2A/Step2B
2. Press Back again, verify returns to Step1 with previous selection

### Restart Flow
1. Complete wizard, press "Start Over"
2. Verify returns to ProcessStart screen
3. Verify previous data is cleared

### Stack Clearing
1. Complete wizard, press "Go to Home"
2. Verify navigates to Home/MainTabs
3. Press system back, verify does NOT return to wizard

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Dependency)
- [MIG-004: Process/Wizard Recipe](./MIG-004-process-wizard-recipe.md) (Reference pattern)
- [LinearWizardRecipe.kt](../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/LinearWizardRecipe.kt) (Code reference)
- [BranchingWizardRecipe.kt](../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/BranchingWizardRecipe.kt) (Code reference)
