package com.ekko.inputeditor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform