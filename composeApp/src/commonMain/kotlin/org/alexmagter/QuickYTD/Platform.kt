package org.alexmagter.QuickYTD

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform