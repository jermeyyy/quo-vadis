# Web Targets - Quick Reference

## 🚀 Build & Run

```bash
# Development (hot reload)
./build-web.sh run-js      # JavaScript
./build-web.sh run-wasm    # WebAssembly

# Production build
./build-web.sh both        # Both JS and Wasm
./build-web.sh js          # JavaScript only
./build-web.sh wasm        # Wasm only
```

## 🌐 Access Demo

**Development:** `http://localhost:8080`

**Production output:**
- JS: `composeApp/build/dist/js/productionExecutable/`
- Wasm: `composeApp/build/dist/wasmJs/productionExecutable/`

## 📦 Targets Supported

| Platform | Build Command |
|----------|---------------|
| JavaScript | `./gradlew :composeApp:jsBrowserDistribution` |
| WebAssembly | `./gradlew :composeApp:wasmJsBrowserDistribution` |

## 🔧 Gradle Tasks

```bash
# Development with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous

# Production build
./gradlew :composeApp:jsBrowserDistribution
./gradlew :composeApp:wasmJsBrowserDistribution

# Library only
./gradlew :quo-vadis-core:jsJar
./gradlew :quo-vadis-core:wasmJsJar
```

## 🗂️ Files Created

**Quo Vadis Core:**
- `src/jsMain/` - JavaScript source set
- `src/wasmJsMain/` - WebAssembly source set

**Demo App:**
- `src/jsMain/kotlin/` - JS platform code
- `src/jsMain/resources/index.html` - JS HTML entry
- `src/wasmJsMain/kotlin/` - Wasm platform code
- `src/wasmJsMain/resources/index.html` - Wasm HTML entry

## 🌍 Browser Support

**JavaScript:** Chrome 90+, Firefox 88+, Safari 14+, Edge 90+  
**WebAssembly:** Chrome 91+, Firefox 89+, Safari 15+, Edge 91+

## 📱 All Platforms

- ✅ Android
- ✅ iOS (x64, Arm64, Simulator)
- ✅ **JavaScript (NEW)**
- ✅ **WebAssembly (NEW)**

See `WEB_TARGET_SUPPORT.md` for full documentation.
