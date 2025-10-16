# Dokka Integration for API Documentation

## Overview
The project uses **Dokka 2.0.0** for generating comprehensive Kotlin API documentation with HTML output. Dokka is configured in the `quo-vadis-core` module to create professional API documentation.

## Configuration

### Gradle Setup
- **Version**: Dokka 2.0.0 (defined in `gradle/libs.versions.toml`)
- **Plugin**: Applied to root `build.gradle.kts` and `quo-vadis-core/build.gradle.kts`
- **Mode**: V2Enabled (modern Dokka plugin mode)

### Properties (`gradle.properties`)
```properties
org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled
org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn=true
```

### Key Configuration (`quo-vadis-core/build.gradle.kts`)
```kotlin
dokka {
    moduleName.set("Quo Vadis Navigation Library")
    moduleVersion.set(project.version.toString())
    
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }
    
    dokkaSourceSets.configureEach {
        // GitHub source links
        sourceLink { ... }
        
        // External documentation (Android, Coroutines)
        externalDocumentationLinks.create("android") { ... }
        externalDocumentationLinks.create("coroutines") { ... }
        
        // Suppress internal packages
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }
    }
}
```

## Generating Documentation

### Command
```bash
./gradlew :quo-vadis-core:dokkaGenerate
```

### Output
- **Location**: `quo-vadis-core/build/dokka/html/`
- **Format**: HTML with navigation, search, and styling
- **Entry Point**: `index.html`

### Documented Packages
All non-internal packages are documented:
- `com.jermey.quo.vadis.core.navigation.core`
- `com.jermey.quo.vadis.core.navigation.compose`
- `com.jermey.quo.vadis.core.navigation.mvi`
- `com.jermey.quo.vadis.core.navigation.integration`
- `com.jermey.quo.vadis.core.navigation.testing`
- `com.jermey.quo.vadis.core.navigation.serialization`
- `com.jermey.quo.vadis.core.navigation.utils`

Internal packages (matching `.*\.internal.*`) are automatically suppressed.

## Features

### Source Links
- Links to GitHub repository source code
- Direct navigation from documentation to implementation

### External Documentation
- **Android Reference**: Links to Android/AndroidX documentation
- **Kotlinx Coroutines**: Links to coroutines documentation
- Enables navigation from library types to their documentation

### Suppression Options
- **suppressObviousFunctions**: Hides standard methods (equals, hashCode, toString)
- **suppressInheritedMembers**: Shows inherited members for context
- **skipEmptyPackages**: Hides packages with no documented content

### Package Filtering
- Internal packages automatically hidden
- Focus on public API surface
- Clean, professional documentation

## Documentation in README
Instructions for generating documentation are in the main README.md under "📚 Documentation" section.

## Build Integration
- Documentation generation is independent of main build
- Can be run on-demand or integrated into CI/CD
- Output in `build/` is already gitignored

Dokka V2 expects specific format for module/package documentation files. The existing markdown files are structured as standalone documentation, not in the format Dokka expects. This is acceptable since the KDoc comments provide comprehensive API documentation.

## Future Enhancements
1. Create properly formatted module documentation for Dokka includes
2. Add Dokka generation to CI/CD pipeline
3. Consider publishing documentation to GitHub Pages
4. Add Javadoc format generation for Java interop (`dokkaPublications.javadoc`)
5. Add Markdown format for easy embedding (`dokkaPublications.gfm`)

## References
- Dokka Documentation: https://kotlinlang.org/docs/dokka-introduction.html
- Migration Guide: https://kotl.in/dokka-gradle-migration
- Generated docs location: `quo-vadis-core/build/dokka/html/index.html`
