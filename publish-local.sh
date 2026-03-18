#!/bin/bash
# Script to publish Quo Vadis library to Maven Local

echo "🚀 Publishing Quo Vadis to Maven Local..."
echo "========================================"

# Stop any running Gradle daemon to ensure fresh build
./gradlew --stop

# Clean build
echo "📦 Cleaning previous builds..."
./gradlew :quo-vadis-core:clean :quo-vadis-annotations:clean :quo-vadis-ksp:clean

# Publish libraries to Maven Local
echo "📤 Publishing libraries to Maven Local..."
./gradlew :quo-vadis-core:publishToMavenLocal :quo-vadis-annotations:publishToMavenLocal :quo-vadis-ksp:publishToMavenLocal

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ FAILED! Publishing libraries encountered an error"
    echo "Check the logs above for details"
    exit 1
fi

# Publish Compiler Plugin to Maven Local
echo "📤 Publishing Compiler Plugin to Maven Local..."
./gradlew :quo-vadis-compiler-plugin:publishToMavenLocal :quo-vadis-compiler-plugin-native:publishToMavenLocal

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ FAILED! Publishing Compiler Plugin encountered an error"
    echo "Check the logs above for details"
    exit 1
fi

# Publish Gradle Plugin to Maven Local
echo "📤 Publishing Gradle Plugin to Maven Local..."
cd quo-vadis-gradle-plugin
../gradlew clean publishToMavenLocal
cd ..

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS! Quo Vadis has been published to Maven Local"
    echo ""
    echo "📍 Location: ~/.m2/repository/io/github/jermeyyy/"
    echo ""
    echo "To use in another project, add to build.gradle.kts:"
    echo "---------------------------------------------------"
    echo "repositories {"
    echo "    mavenLocal()"
    echo "}"
    echo ""
    echo "plugins {"
    echo "    id(\"io.github.jermeyyy.quo-vadis\") version \"VERSION\""
    echo "}"
    echo ""
    echo "dependencies {"
    echo "    implementation(\"io.github.jermeyyy:quo-vadis-core:VERSION\")"
    echo "    implementation(\"io.github.jermeyyy:quo-vadis-annotations:VERSION\")"
    echo "    ksp(\"io.github.jermeyyy:quo-vadis-ksp:VERSION\")"
    echo "}"
    echo ""
else
    echo ""
    echo "❌ FAILED! Publishing Gradle Plugin encountered an error"
    echo "Check the logs above for details"
    exit 1
fi
