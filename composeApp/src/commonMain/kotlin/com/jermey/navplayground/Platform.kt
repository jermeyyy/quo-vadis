package com.jermey.navplayground

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
