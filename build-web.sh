#!/bin/bash
# Script to build and run web targets

echo "🌐 Building Quo Vadis Web Targets"
echo "=================================="

show_help() {
    echo "Usage: ./build-web.sh [option]"
    echo ""
    echo "Options:"
    echo "  js          Build JavaScript version"
    echo "  wasm        Build WebAssembly version"
    echo "  both        Build both JS and Wasm (default)"
    echo "  run-js      Build and run JavaScript version"
    echo "  run-wasm    Build and run WebAssembly version"
    echo "  help        Show this help message"
    echo ""
}

build_js() {
    echo "📦 Building JavaScript version..."
    ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous &
    JS_PID=$!
    echo "✅ JS version building..."
    echo "🌐 Open http://localhost:8080 in your browser"
    echo "Press Ctrl+C to stop"
    wait $JS_PID
}

build_wasm() {
    echo "📦 Building WebAssembly version..."
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous &
    WASM_PID=$!
    echo "✅ Wasm version building..."
    echo "🌐 Open http://localhost:8080 in your browser"
    echo "Press Ctrl+C to stop"
    wait $WASM_PID
}

build_js_production() {
    echo "📦 Building JavaScript production bundle..."
    ./gradlew :composeApp:jsBrowserDistribution
    if [ $? -eq 0 ]; then
        echo "✅ JS production build complete!"
        echo "📁 Output: composeApp/build/dist/js/productionExecutable/"
    fi
}

build_wasm_production() {
    echo "📦 Building WebAssembly production bundle..."
    ./gradlew :composeApp:wasmJsBrowserDistribution
    if [ $? -eq 0 ]; then
        echo "✅ Wasm production build complete!"
        echo "📁 Output: composeApp/build/dist/wasmJs/productionExecutable/"
    fi
}

case "${1:-both}" in
    js)
        build_js_production
        ;;
    wasm)
        build_wasm_production
        ;;
    both)
        build_js_production
        build_wasm_production
        ;;
    run-js)
        build_js
        ;;
    run-wasm)
        build_wasm
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo "❌ Unknown option: $1"
        show_help
        exit 1
        ;;
esac
