package com.example.myapplication.ui.filters

import com.example.myapplication.ui.filters.FilterPresets.Cinematic
import com.example.myapplication.ui.filters.FilterPresets.Cool
import com.example.myapplication.ui.filters.FilterPresets.Film
import com.example.myapplication.ui.filters.FilterPresets.HighContrast
import com.example.myapplication.ui.filters.FilterPresets.Mono
import com.example.myapplication.ui.filters.FilterPresets.Night
import com.example.myapplication.ui.filters.FilterPresets.Retro
import com.example.myapplication.ui.filters.FilterPresets.Sepia
import com.example.myapplication.ui.filters.FilterPresets.Soft
import com.example.myapplication.ui.filters.FilterPresets.Vibrant
import com.example.myapplication.ui.filters.FilterPresets.Vivid
import com.example.myapplication.ui.filters.FilterPresets.Warm

// Groups presets into user-facing categories for the editor UI

data class FilterCategory(
    val id: String,
    val name: String,
    val presets: List<FilterPreset>
)

object FilterCategories {
    val Tone = FilterCategory(
        id = "tone",
        name = "Tone",
        presets = listOf(Vibrant, Warm, Cool, HighContrast, Soft)
    )
    val Vintage = FilterCategory(
        id = "vintage",
        name = "Vintage",
        presets = listOf(Sepia, Film, Retro)
    )
    val Cinema = FilterCategory(
        id = "cinema",
        name = "Cinema",
        presets = listOf(Cinematic, Vivid)
    )
    val Mono = FilterCategory(
        id = "mono",
        name = "Mono",
        presets = listOf(Mono)
    )
    val Night = FilterCategory(
        id = "night",
        name = "Night",
        presets = listOf(Night)
    )

    val All = listOf(Tone, Vintage, Cinema, Mono, Night)
}

