package com.jermey.navplayground

class JsPlatform : Platform {
    override val name: String = "Web (JavaScript)"
}

actual fun getPlatform(): Platform = JsPlatform()
