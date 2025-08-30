package com.example.myapplication.ui.navigation

object NavRoutes {
    const val Camera = "camera"
    const val Review = "review/{uri}"
    const val Editor = "editor/{uri}"
    const val Gallery = "gallery"
    const val Settings = "settings"

    fun review(uri: String) = "review/${uri}"
    fun editor(uri: String) = "editor/${uri}"
}

