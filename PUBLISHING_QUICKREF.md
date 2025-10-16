# Quo Vadis - Quick Publishing Reference

## 🚀 Publish to Maven Local

```bash
./publish-local.sh
```

## 📦 Library Coordinates

```kotlin
implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
```

## 🔧 Manual Commands

```bash
# Full publish
./gradlew :quo-vadis-core:publishToMavenLocal

# Clean + Publish
./gradlew :quo-vadis-core:clean :quo-vadis-core:publishToMavenLocal

# Stop daemon
./gradlew --stop
```

## 📍 Published Location

```
~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/
```

## 🎯 Use in Another Project

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
}
```

## 🗑️ Clear Local Maven Cache

```bash
rm -rf ~/.m2/repository/com/jermey/quo/vadis/
```

## 📋 Version Update

Edit `quo-vadis-core/build.gradle.kts`:
```kotlin
version = "0.1.0-SNAPSHOT"  // Change this
```

See `PUBLISHING.md` for full documentation.
