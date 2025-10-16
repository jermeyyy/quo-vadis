# Technology Stack

## Languages
- **Kotlin**: Version 2.2.20 (Multiplatform)
- **Swift**: For iOS app wrapper

## Frameworks & Libraries

### Kotlin Multiplatform
- **Kotlin Multiplatform**: 2.2.20
- **Kotlin Standard Library**: 2.2.20

### Compose Multiplatform
- **Compose Multiplatform**: 1.9.0
- **Compose Compiler**: 2.2.20
- Compose Runtime, Foundation, Material3, UI
- Material Icons Extended (platform-specific)
- Compose Resources
- Compose UI Tooling Preview
- Compose Desktop (for JVM target)

### AndroidX Libraries
- **Activity Compose**: 1.11.0
- **AppCompat**: 1.7.1
- **Core KTX**: 1.17.0
- **Lifecycle**: 2.9.4
  - ViewModel Compose
  - Runtime Compose
- **Espresso**: 3.7.0 (testing)
- **Test Ext**: 1.3.0 (testing)

### Other Libraries
- **Compose Backhandler**: Predictive back navigation support
- **JUnit**: 4.13.2 (testing)
- **Kotlin Test**: For multiplatform testing

## Build System
- **Gradle**: 8.14.3 (with Kotlin DSL)
- **Android Gradle Plugin (AGP)**: 8.11.2

### Gradle Memory Configuration
- **Gradle Daemon JVM Args**: `-Xmx6144M` (6GB)
- **Gradle Build JVM Args**: `-Xmx8192M` (8GB)
- **Kotlin Daemon Args**: `-Xmx6144M` (6GB)
- **Configuration Cache**: Enabled
- **Build Cache**: Enabled

## Gradle Plugins Used
1. `com.android.application` - Android app module
2. `com.android.library` - Android library module
3. `org.jetbrains.compose` - Compose Multiplatform
4. `org.jetbrains.kotlin.plugin.compose` - Compose compiler
5. `org.jetbrains.kotlin.multiplatform` - KMP support
6. `com.android.kotlin.multiplatform.library` - KMP library support
7. `com.android.lint` - Android linting

## Platform Specifics

### Android
- **Min SDK**: 24
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin JVM Target**: JVM 11
- **Namespace**: `com.jermey.navplayground` (app), `com.jermey.quo.vadis.core` (library)

### iOS
- **Frameworks**: ComposeApp framework (static)
- **Supported Targets**: 
  - iosArm64 (physical devices)
  - iosSimulatorArm64 (M1/M2 Macs)
  - iosX64 (Intel Macs)
- **Framework Names**: 
  - ComposeApp (for demo app)
  - quo-vadis-coreKit (for library)

### Web Targets

#### JavaScript (JS)
- **Compiler**: IR (Intermediate Representation)
- **Rendering**: Canvas-based via ComposeViewport API
- **Browser Support**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- **Features**: Webpack bundling, hot reload, source maps
- **Entry Point**: `composeApp/src/jsMain/kotlin/main.js.kt`
- **HTML**: `composeApp/src/jsMain/resources/index.html`
- **Bundle**: JavaScript with Compose runtime

#### WebAssembly (Wasm)
- **Target**: wasmJs
- **Compiler**: Kotlin/Wasm
- **Rendering**: Canvas-based via ComposeViewport API
- **Browser Support**: Chrome 91+, Firefox 89+, Safari 15+, Edge 91+
- **Features**: Near-native performance, smaller bundle size
- **Entry Point**: `composeApp/src/wasmJsMain/kotlin/main.wasmJs.kt`
- **HTML**: `composeApp/src/wasmJsMain/resources/index.html`
- **Bundle**: Wasm binary + JavaScript glue code

**Important**: ComposeViewport requires a `<div>` element (not `<canvas>`), as it uses shadow DOM which canvas elements don't support.

### Desktop (JVM)
- **Target**: jvm("desktop")
- **Java Version**: 11+
- **Rendering**: Native Swing-based Compose Desktop
- **Window API**: `application { Window { } }`
- **Supported OS**: macOS, Windows, Linux
- **Entry Point**: `composeApp/src/desktopMain/kotlin/main.desktop.kt`
- **Distribution Formats**:
  - DMG (macOS)
  - MSI (Windows)
  - DEB (Linux)
- **Main Class**: `com.jermey.navplayground.Main_desktopKt`

### Compose Version Alignment
The project uses resolution strategy to force all Compose dependencies to version 1.9.0:
- material3:1.9.0
- material3-desktop:1.9.0
- ui:1.9.0
- ui-desktop:1.9.0
- runtime:1.9.0
- runtime-desktop:1.9.0

This prevents binary incompatibility issues between Compose artifacts.

## Repository Configuration
- **Google Maven**: For AndroidX and Google dependencies
- **Maven Central**: For Kotlin and other dependencies
- **Gradle Plugin Portal**: For build plugins
- **Maven Local**: For local library testing

## Publishing Configuration

### Maven Coordinates
- **Group ID**: `com.jermey.quo.vadis`
- **Artifact ID**: `quo-vadis-core`
- **Version**: `0.1.0-SNAPSHOT`

### Published Artifacts
All platforms published to Maven Local (`~/.m2/repository/`):

1. **Android**: `quo-vadis-core-android-0.1.0-SNAPSHOT.aar`
2. **iOS x64**: `quo-vadis-core-iosx64-0.1.0-SNAPSHOT.klib`
3. **iOS Arm64**: `quo-vadis-core-iosarm64-0.1.0-SNAPSHOT.klib`
4. **iOS Simulator Arm64**: `quo-vadis-core-iossimulatorarm64-0.1.0-SNAPSHOT.klib`
5. **JavaScript**: `quo-vadis-core-js-0.1.0-SNAPSHOT.klib`
6. **WebAssembly**: `quo-vadis-core-wasm-js-0.1.0-SNAPSHOT.klib`
7. **Desktop**: `quo-vadis-core-desktop-0.1.0-SNAPSHOT.jar`

### Publishing Command
```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```

This publishes all platform artifacts simultaneously.
