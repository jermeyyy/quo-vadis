# Tech Stack

## Languages & Frameworks

- **Kotlin**: 2.2.21 (Multiplatform)
- **Compose Multiplatform**: 1.10.0-rc02
- **Gradle**: 8.14.3
- **Android Gradle Plugin**: 8.13.2

## Android Configuration

- **Min SDK**: 24
- **Target SDK**: 36
- **Compile SDK**: 36

## Key Dependencies

### Core
- `kotlinx-serialization-json`: 1.9.0 - JSON serialization for type-safe arguments
- `kotlinx-datetime`: 0.7.1 - Multiplatform datetime

### AndroidX
- `androidx-activity-compose`: 1.12.0
- `androidx-lifecycle-viewmodel-compose`: 2.9.6
- `androidx-lifecycle-runtime-compose`: 2.9.6
- `androidx-material3-windowsizeclass`: 1.4.0

### Code Generation
- **KSP**: 2.3.0 - Kotlin Symbol Processing
- **KotlinPoet**: 2.2.0 - Kotlin code generation

### Testing
- `kotlin-test` - Multiplatform testing
- `junit`: 4.13.2 - JVM testing

### Optional Integrations
- **FlowMVI**: 3.2.1 - MVI architecture (optional module)
- **Koin**: 4.1.1 - Dependency injection support

## Build Tools

- **Detekt**: 2.0.0-alpha.1 - Static code analysis
- **Dokka**: 2.1.0 - API documentation generation
- **Vanniktech Maven Publish**: 0.34.0 - Maven Central publishing

## Build Configuration

```properties
# gradle.properties optimizations
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# JVM memory settings
org.gradle.jvmargs=-Xmx8192M -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

## Platform Targets

### Kotlin Multiplatform Targets
- `androidTarget` - Android library
- `iosArm64` - iOS device (ARM64)
- `iosSimulatorArm64` - iOS simulator (Apple Silicon)
- `iosX64` - iOS simulator (Intel)
- `js(IR)` - JavaScript with IR compiler
- `wasmJs` - WebAssembly
- `jvm("desktop")` - Desktop JVM

### Compose Platforms
All platforms use Compose Multiplatform with Canvas-based rendering for web targets.

## Serialization

The library uses `kotlinx.serialization` for:
- Type-safe navigation arguments (`TypedDestination<T>`)
- Navigation state persistence
- Deep link parameter parsing

## IDE Requirements

- **Android Studio** or **IntelliJ IDEA** with Kotlin Multiplatform plugin
- **Xcode** for iOS development (macOS only)
- **JDK 17+** for desktop development
