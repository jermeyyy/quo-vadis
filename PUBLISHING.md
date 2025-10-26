# Publishing Quo Vadis to Maven Local

## Overview

The Quo Vadis navigation library can be published to your local Maven repository for testing and development purposes before releasing to remote repositories like Maven Central.

---

## Quick Start

### Option 1: Using the Helper Script (Recommended)

```bash
./publish-local.sh
```

This script will:
1. Stop any running Gradle daemon
2. Clean previous builds
3. Publish all artifacts to Maven Local
4. Show usage instructions

### Option 2: Using Gradle Directly

```bash
# Publish all artifacts
./gradlew :quo-vadis-core:publishToMavenLocal

# Or with clean build
./gradlew :quo-vadis-core:clean :quo-vadis-core:publishToMavenLocal
```

---

## Configuration Details

### Library Coordinates

```kotlin
groupId    = "com.jermey.quo.vadis"
artifactId = "quo-vadis-core"
version    = "0.1.0-SNAPSHOT"
```

### Published Artifacts

The following artifacts are published to Maven Local (7 platforms):

1. **Common Metadata** - `quo-vadis-core-0.1.0-SNAPSHOT.module`
2. **Android** - `quo-vadis-core-android-0.1.0-SNAPSHOT.aar`
3. **iOS x64** - `quo-vadis-core-iosx64-0.1.0-SNAPSHOT.klib`
4. **iOS Arm64** - `quo-vadis-core-iosarm64-0.1.0-SNAPSHOT.klib`
5. **iOS Simulator Arm64** - `quo-vadis-core-iossimulatorarm64-0.1.0-SNAPSHOT.klib`
6. **JavaScript** - `quo-vadis-core-js-0.1.0-SNAPSHOT.klib`
7. **WebAssembly** - `quo-vadis-core-wasm-js-0.1.0-SNAPSHOT.klib`
8. **Desktop (JVM)** - `quo-vadis-core-desktop-0.1.0-SNAPSHOT.jar`

### Maven Local Location

Artifacts are published to:
```
~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/
```

---

## Using the Published Library

### In Another KMP Project

Add to your project's `build.gradle.kts`:

```kotlin
repositories {
    mavenLocal()  // Add this first to prioritize local artifacts
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
            }
        }
    }
}
```

### In an Android-Only Project

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    implementation("com.jermey.quo.vadis:quo-vadis-core-android:0.1.0-SNAPSHOT")
}
```

---

## Version Management

### Current Version: `0.1.0-SNAPSHOT`

To change the version, edit `quo-vadis-core/build.gradle.kts`:

```kotlin
group = "com.jermey.quo.vadis"
version = "0.1.0-SNAPSHOT"  // ← Change this
```

### Version Naming Convention

- **Development/Testing:** `X.Y.Z-SNAPSHOT` (e.g., `0.1.0-SNAPSHOT`)
- **Alpha Releases:** `X.Y.Z-alpha.N` (e.g., `0.1.0-alpha.1`)
- **Beta Releases:** `X.Y.Z-beta.N` (e.g., `0.1.0-beta.1`)
- **Release Candidates:** `X.Y.Z-rc.N` (e.g., `0.1.0-rc.1`)
- **Stable Releases:** `X.Y.Z` (e.g., `0.1.0`, `1.0.0`)

---

## Gradle Tasks Reference

```bash
# List all publishing tasks
./gradlew :quo-vadis-core:tasks --group publishing

# Publish to Maven Local (all targets)
./gradlew :quo-vadis-core:publishToMavenLocal

# Publish specific target
./gradlew :quo-vadis-core:publishAndroidReleasePublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosX64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosArm64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosSimulatorArm64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishJsPublicationToMavenLocal
./gradlew :quo-vadis-core:publishWasmJsPublicationToMavenLocal
./gradlew :quo-vadis-core:publishDesktopPublicationToMavenLocal

# Generate POM files (for verification)
./gradlew :quo-vadis-core:generatePomFileForKotlinMultiplatformPublication
```

---

## Troubleshooting

### Clear Maven Local Cache

If you need to completely remove the library from Maven Local:

```bash
rm -rf ~/.m2/repository/com/jermey/quo/vadis/
```

### Verify Published Artifacts

Check what was published:

```bash
ls -la ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
```

### Build Issues

If you encounter build failures:

1. **Clean and stop daemon:**
   ```bash
   ./gradlew --stop
   ./gradlew clean
   ```

2. **Check Gradle configuration cache:**
   ```bash
   rm -rf .gradle/configuration-cache
   ```

3. **Increase heap space** (already configured in `gradle.properties`):
   ```properties
   org.gradle.jvmargs=-Xmx8192M -XX:MaxMetaspaceSize=2048M
   ```

### Memory Issues During iOS Framework Linking

The iOS framework linking is memory-intensive. If you still encounter OOM errors:

1. Publish Android first:
   ```bash
   ./gradlew :quo-vadis-core:publishAndroidReleasePublicationToMavenLocal
   ```

2. Then publish iOS targets individually:
   ```bash
   ./gradlew :quo-vadis-core:publishIosX64PublicationToMavenLocal
   ./gradlew :quo-vadis-core:publishIosArm64PublicationToMavenLocal
   ./gradlew :quo-vadis-core:publishIosSimulatorArm64PublicationToMavenLocal
   ```

---

## POM Configuration

The library is published with the following metadata:

- **Name:** Quo Vadis - Navigation Library
- **Description:** A comprehensive type-safe navigation library for Compose Multiplatform
- **License:** Apache License 2.0
- **Repository:** https://github.com/jermeyyy/quo-vadis

This metadata is included in the POM file and helps with dependency resolution.

---

## Next Steps for Public Release

Once tested locally, you can publish to remote repositories:

### Maven Central

1. Set up Sonatype account and GPG signing
2. Add publishing configuration:
   ```kotlin
   publishing {
       repositories {
           maven {
               name = "sonatype"
               url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
               credentials {
                   username = project.findProperty("ossrhUsername") as String?
                   password = project.findProperty("ossrhPassword") as String?
               }
           }
       }
   }
   ```

### GitHub Packages

```kotlin
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/jermeyyy/quo-vadis")
            credentials {
                username = project.findProperty("gpr.user") as String?
                password = project.findProperty("gpr.key") as String?
            }
        }
    }
}
```

---

## Documentation Publishing

The project documentation website is automatically published to GitHub Pages when changes are pushed to the `main` branch.

### Documentation Components

1. **Static Website** - Located in `/docs/site/`
   - Homepage, getting started, features, demo pages
   - CSS, JavaScript, and images

2. **API Documentation** - Auto-generated from source code
   - Generated via Dokka during GitHub Actions workflow
   - Published at: https://jermeyyy.github.io/quo-vadis/api/

### Publishing Process

The documentation is deployed automatically via GitHub Actions:

```yaml
# Triggered on push to main branch
on:
  push:
    branches: [ main ]
```

**Workflow steps:**
1. Generate Dokka documentation from source code
2. Copy static site files from `/docs/site/`
3. Combine content and deploy to GitHub Pages

### Manual Documentation Generation

To generate API documentation locally:

```bash
# Generate Dokka HTML
./gradlew :quo-vadis-core:dokkaGenerateHtml

# View output
open quo-vadis-core/build/dokka/html/index.html
```

See [docs/SITE_MAINTENANCE.md](docs/SITE_MAINTENANCE.md) for detailed website maintenance instructions.

---

## Summary

✅ **Maven Local publishing configured**  
✅ **Helper script created** (`publish-local.sh`)  
✅ **All KMP targets supported** (Android, iOS x64, iOS Arm64, iOS Simulator Arm64)  
✅ **POM metadata configured**  
✅ **Version management ready**  

**To publish:** Run `./publish-local.sh` or `./gradlew :quo-vadis-core:publishToMavenLocal`

**Published location:** `~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/`
