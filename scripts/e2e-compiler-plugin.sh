#!/bin/bash
# E2E verification script for compiler plugin mode
# Usage: ./scripts/e2e-compiler-plugin.sh [target]
# Targets: desktop, android, ios, js, wasm, all

set -e

PROPERTY="-PquoVadis.useCompilerPlugin=true"

case "${1:-all}" in
  desktop)
    echo "=== Building Desktop ==="
    ./gradlew :composeApp:jvmJar $PROPERTY
    ;;
  android)
    echo "=== Building Android ==="
    ./gradlew :composeApp:assembleDebug $PROPERTY
    ;;
  ios)
    echo "=== Building iOS Framework ==="
    ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 $PROPERTY
    ;;
  js)
    echo "=== Building JS ==="
    ./gradlew :composeApp:jsBrowserProductionWebpack $PROPERTY
    ;;
  wasm)
    echo "=== Building WasmJS ==="
    ./gradlew :composeApp:wasmJsBrowserProductionWebpack $PROPERTY
    ;;
  all)
    echo "=== Full E2E Verification ==="
    echo ""
    echo "--- Desktop ---"
    ./gradlew :composeApp:jvmJar $PROPERTY
    echo ""
    echo "--- Android ---"
    ./gradlew :composeApp:assembleDebug $PROPERTY
    echo ""
    echo "--- Multi-module check ---"
    ./gradlew :feature1:allMetadataJar :feature2:allMetadataJar $PROPERTY
    echo ""
    echo "=== All E2E checks passed ==="
    ;;
  *)
    echo "Usage: $0 [desktop|android|ios|js|wasm|all]"
    exit 1
    ;;
esac
