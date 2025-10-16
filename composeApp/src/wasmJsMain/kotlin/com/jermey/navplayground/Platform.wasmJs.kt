package com.jermey.navplayground

class WasmPlatform : Platform {
    override val name: String = "Web (WebAssembly)"
}

actual fun getPlatform(): Platform = WasmPlatform()
