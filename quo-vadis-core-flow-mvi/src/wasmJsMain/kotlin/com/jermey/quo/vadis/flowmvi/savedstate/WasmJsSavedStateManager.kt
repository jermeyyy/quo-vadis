package com.jermey.quo.vadis.flowmvi.savedstate

/**
 * WebAssembly implementation.
 * 
 * Currently uses in-memory storage as WASM interop with browser APIs
 * is still evolving. In production, you would use WASM-JS interop
 * to access localStorage/sessionStorage.
 */
actual fun createPlatformSavedStateManager(): SavedStateManager {
    // TODO: Implement WASM-specific storage when interop is stable
    return InMemorySavedStateManager()
}
