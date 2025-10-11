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
- Material Icons Extended
- Compose Resources
- Compose UI Tooling Preview

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
- **Gradle**: 8.11+ (with Kotlin DSL)
- **Android Gradle Plugin (AGP)**: 8.11.2

## Gradle Plugins Used
1. `com.android.application` - Android app module
2. `com.android.library` - Android library module
3. `org.jetbrains.compose` - Compose Multiplatform
4. `org.jetbrains.kotlin.plugin.compose` - Compose compiler
5. `org.jetbrains.kotlin.multiplatform` - KMP support
6. `com.android.kotlin.multiplatform.library` - KMP library support
7. `com.android.lint` - Android linting

## Development Tools
- **Gradle Configuration Cache**: Enabled
- **Gradle Build Cache**: Enabled
- **Code Style**: Official Kotlin code style
- **Gradle Daemon JVM Args**: -Xmx3072M
- **Gradle Build JVM Args**: -Xmx4096M

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

## Repository Configuration
- **Google Maven**: For AndroidX and Google dependencies
- **Maven Central**: For Kotlin and other dependencies
- **Gradle Plugin Portal**: For build plugins
