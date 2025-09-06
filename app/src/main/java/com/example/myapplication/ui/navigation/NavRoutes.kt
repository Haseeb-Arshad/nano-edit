package com.example.myapplication.ui.navigation

object NavRoutes {
    const val Camera = "camera"
    const val Review = "review/{uri}"
    // Optional query param to auto-run SceneLift in editor
    const val Editor = "editor/{uri}?autolift={autolift}"
    const val Gallery = "gallery"
    const val Settings = "settings"

    fun review(uri: String) = "review/${uri}"
    fun editor(uri: String, autolift: Boolean = false) = "editor/${uri}?autolift=${autolift}"
}

