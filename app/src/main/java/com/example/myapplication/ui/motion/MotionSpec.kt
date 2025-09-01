package com.example.myapplication.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable

@Immutable
data class HeroSpec(
    val name: String,
    val durationMillis: Int,
    val scaleFrom: Float,
    val scaleTo: Float,
    val alphaFrom: Float,
    val alphaTo: Float,
    val cornerFrom: Float,
    val cornerTo: Float,
    val translateZ: Float = 0f,
    val easing: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val colorCrossfade: Boolean = true
)

/**
 * Macro motion spec samples (Figma-like), exported as JSON-like when needed.
 */
object MotionSpec {
    val CameraToReview = HeroSpec(
        name = "CameraToReviewHero",
        durationMillis = MotionTokens.DurationMedium,
        scaleFrom = 0.96f,
        scaleTo = 1f,
        alphaFrom = 0.7f,
        alphaTo = 1f,
        cornerFrom = 24f,
        cornerTo = 12f,
        translateZ = 8f,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
        colorCrossfade = true
    )

    val ReviewToEditor = HeroSpec(
        name = "ReviewToEditorHero",
        durationMillis = MotionTokens.DurationLarge,
        scaleFrom = 0.92f,
        scaleTo = 1f,
        alphaFrom = 0.6f,
        alphaTo = 1f,
        cornerFrom = 20f,
        cornerTo = 0f,
        translateZ = 12f,
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
        colorCrossfade = true
    )

    fun asJson(): String = """
        {
          "macros": [
            {
              "name": "${CameraToReview.name}",
              "duration": ${CameraToReview.durationMillis},
              "scale": [${CameraToReview.scaleFrom}, ${CameraToReview.scaleTo}],
              "alpha": [${CameraToReview.alphaFrom}, ${CameraToReview.alphaTo}],
              "cornerRadius": [${CameraToReview.cornerFrom}, ${CameraToReview.cornerTo}],
              "translateZ": ${CameraToReview.translateZ},
              "easing": "cubic(0.2,0,0,1)",
              "colorCrossfade": ${CameraToReview.colorCrossfade}
            },
            {
              "name": "${ReviewToEditor.name}",
              "duration": ${ReviewToEditor.durationMillis},
              "scale": [${ReviewToEditor.scaleFrom}, ${ReviewToEditor.scaleTo}],
              "alpha": [${ReviewToEditor.alphaFrom}, ${ReviewToEditor.alphaTo}],
              "cornerRadius": [${ReviewToEditor.cornerFrom}, ${ReviewToEditor.cornerTo}],
              "translateZ": ${ReviewToEditor.translateZ},
              "easing": "cubic(0.2,0,0,1)",
              "colorCrossfade": ${ReviewToEditor.colorCrossfade}
            }
          ]
        }
    """.trimIndent()
}

