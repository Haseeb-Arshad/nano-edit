package com.example.myapplication.data

data class ImageAnalysisResult(
    val detectedObjects: List<String>,
    val suggestedFilters: List<FilterSuggestion>,
    val confidence: Float,
    val analysisTime: Long
)

data class FilterSuggestion(
    val name: String,
    val description: String,
    val confidence: Float,
    val filterType: FilterType
)

enum class FilterType {
    PORTRAIT,
    LANDSCAPE,
    FOOD,
    NIGHT,
    VINTAGE,
    DRAMATIC,
    NATURAL,
    ARTISTIC
}