# Codebase Structure

## Root Directory

```
NavPlayground/
├── build.gradle.kts           # Root build configuration
├── settings.gradle.kts        # Project settings and module includes
├── gradle.properties          # Gradle and JVM settings
├── gradle/
│   └── libs.versions.toml     # Version catalog
├── config/
│   └── detekt/
│       └── detekt.yml         # Detekt configuration
├── README.md                  # Project documentation
├── CONTRIBUTING.md            # Contribution guidelines
├── CHANGELOG.md               # Version history
├── LICENSE                    # MIT License
├── build-desktop.sh           # Desktop build script
├── build-web.sh               # Web build script
└── publish-local.sh           # Local Maven publish script
```

## Modules

### quo-vadis-core (Navigation Library)

```
quo-vadis-core/
├── build.gradle.kts
├── detekt-baseline.xml
├── dokka.json
└── src/
    ├── commonMain/kotlin/com/jermey/quo/vadis/core/navigation/
    │   ├── core/                  # Core interfaces
    │   │   ├── Navigator.kt       # Main navigator interface
    │   │   ├── Destination.kt     # Destination base
    │   │   ├── NavNode.kt         # Navigation node base
    │   │   ├── ScreenNode.kt      # Single screen node
    │   │   ├── StackNode.kt       # Stack container
    │   │   ├── TabNode.kt         # Tab container
    │   │   ├── PaneNode.kt        # Adaptive pane container
    │   │   ├── TreeNavigator.kt   # Tree-based navigator impl
    │   │   ├── TreeMutator.kt     # Tree mutation DSL
    │   │   ├── NavigationTransition.kt
    │   │   └── DeepLink.kt
    │   ├── compose/
    │   │   ├── NavigationHost.kt  # Main Compose entry
    │   │   ├── animation/         # Transition animations
    │   │   ├── navback/           # Predictive back
    │   │   ├── registry/          # Route/Screen registries
    │   │   ├── render/            # Tree rendering
    │   │   └── wrapper/           # Tab/Pane wrappers
    │   ├── integration/
    │   │   └── KoinIntegration.kt
    │   ├── serialization/         # State serialization
    │   ├── testing/
    │   │   ├── FakeNavigator.kt
    │   │   └── FakeNavRenderScope.kt
    │   └── utils/
    │       └── NavigationExtensions.kt
    ├── androidMain/               # Android-specific
    ├── iosMain/                   # iOS-specific
    ├── desktopMain/               # Desktop-specific
    ├── jsMain/                    # JS-specific
    ├── wasmJsMain/                # WASM-specific
    ├── commonTest/                # Shared tests
    └── desktopTest/               # Desktop tests (fast)
```

### quo-vadis-annotations

```
quo-vadis-annotations/
├── build.gradle.kts
└── src/commonMain/kotlin/com/jermey/quo/vadis/annotations/
    ├── Graph.kt                   # @Graph annotation
    ├── Route.kt                   # @Route annotation
    ├── Argument.kt                # @Argument annotation
    ├── Content.kt                 # @Content annotation
    └── DeepLink.kt                # @DeepLink annotation
```

### quo-vadis-ksp (Code Generator)

```
quo-vadis-ksp/
├── build.gradle.kts
├── detekt-baseline.xml
└── src/main/kotlin/com/jermey/quo/vadis/ksp/
    ├── QuoVadisSymbolProcessor.kt     # Main processor
    ├── GraphProcessor.kt              # @Graph processing
    ├── ContentProcessor.kt            # @Content processing
    ├── RouteGenerator.kt              # Route code gen
    └── GraphBuilderGenerator.kt       # Graph builder gen
```

### quo-vadis-core-flow-mvi

```
quo-vadis-core-flow-mvi/
├── build.gradle.kts
└── src/commonMain/kotlin/
    └── FlowMVI integration classes
```

### composeApp (Demo Application)

```
composeApp/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/jermey/navplayground/demo/
    │   ├── DemoApp.kt             # Main demo app
    │   ├── destinations/          # Destination definitions
    │   ├── graphs/                # Navigation graphs
    │   └── ui/screens/            # Demo screens
    ├── androidMain/
    │   └── MainActivity.kt        # Android entry
    ├── iosMain/
    │   └── MainViewController.kt  # iOS entry
    ├── desktopMain/
    │   └── main.desktop.kt        # Desktop entry
    ├── jsMain/
    │   └── main.js.kt             # JS entry
    └── wasmJsMain/
        └── main.wasmJs.kt         # WASM entry
```

### iosApp (iOS Wrapper)

```
iosApp/
├── iosApp.xcodeproj/
├── Configuration/Config.xcconfig
└── iosApp/
    ├── iOSApp.swift               # App entry
    ├── ContentView.swift          # SwiftUI wrapper
    └── Info.plist
```

### docs/site (Documentation Website)

```
docs/site/
├── index.html
├── package.json                   # Vite/React config
├── vite.config.ts
├── tsconfig.json
├── src/                           # React source
└── public/                        # Static assets
```

## Key File Locations

| Purpose | File |
|---------|------|
| Version catalog | `gradle/libs.versions.toml` |
| Detekt config | `config/detekt/detekt.yml` |
| Navigator interface | `quo-vadis-core/src/commonMain/.../core/Navigator.kt` |
| Main Compose host | `quo-vadis-core/src/commonMain/.../compose/NavigationHost.kt` |
| Tree navigator | `quo-vadis-core/src/commonMain/.../core/TreeNavigator.kt` |
| Desktop tests | `quo-vadis-core/src/desktopTest/` |
| Demo app | `composeApp/src/commonMain/.../demo/DemoApp.kt` |
