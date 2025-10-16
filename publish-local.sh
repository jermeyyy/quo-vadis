#!/bin/bash
# Script to publish Quo Vadis library to Maven Local

echo "🚀 Publishing Quo Vadis to Maven Local..."
echo "========================================"

# Stop any running Gradle daemon to ensure fresh build
./gradlew --stop

# Clean build
echo "📦 Cleaning previous builds..."
./gradlew :quo-vadis-core:clean

# Publish to Maven Local
echo "📤 Publishing to Maven Local..."
./gradlew :quo-vadis-core:publishToMavenLocal

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS! Quo Vadis has been published to Maven Local"
    echo ""
    echo "📍 Location: ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/"
    echo ""
    echo "To use in another project, add to build.gradle.kts:"
    echo "---------------------------------------------------"
    echo "repositories {"
    echo "    mavenLocal()"
    echo "}"
    echo ""
    echo "dependencies {"
    echo "    implementation(\"com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT\")"
    echo "}"
    echo ""
else
    echo ""
    echo "❌ FAILED! Publishing encountered an error"
    echo "Check the logs above for details"
    exit 1
fi
