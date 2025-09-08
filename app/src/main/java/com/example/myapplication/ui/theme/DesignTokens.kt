package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

/**
 * Design tokens for the AI Camera App
 * Following the master prompt specifications for minimal, Instagram-inspired design
 */
object DesignTokens {
    
    // Colors - Light Theme
    object Colors {
        object Light {
            val background = Color(0xFF0C0C0E)
            val surface = Color(0x8C121214) // rgba(18,18,20,0.55)
            val accent = Color(0xFFFF2D55)
            val accentAlt = Color(0xFF00D3A7)
            val textHigh = Color(0xFFFFFFFF)
            val textMid = Color(0xFFBEBEC2)
            val textLow = Color(0xFF8C8C91)
            val success = Color(0xFF2ECC71)
            val warning = Color(0xFFFFC542)
            val error = Color(0xFFFF4D4F)
        }
        
        object Dark {
            val background = Color(0xFF0C0C0E)
            val surface = Color(0xB3121214) // rgba(18,18,20,0.7)
            val accent = Color(0xFFFF3D65) // +10% brightness
            val accentAlt = Color(0xFF10E3B7) // +10% brightness
            val textHigh = Color(0xFFFFFFFF)
            val textMid = Color(0xFFBEBEC2)
            val textLow = Color(0xFF8C8C91)
            val success = Color(0xFF3EDC81)
            val warning = Color(0xFFFFD552)
            val error = Color(0xFFFF5D5F)
        }
    }
    
    // Typography
    object Typography {
        val displayFontFamily = "Space Grotesk" // System fallback in Compose
        val uiFontFamily = "Inter" // System fallback in Compose
        
        val display = 28.sp
        val title = 22.sp
        val headline = 18.sp
        val body = 16.sp
        val label = 14.sp
        val caption = 12.sp
        
        val trackingTight = -0.01f // -1% for headings
    }
    
    // Radius
    object Radius {
        val card = 16.dp
        val sheet = 12.dp
        val chip = 8.dp
        val pill = 28.dp
        val lg = 16.dp
        val md = 12.dp
    }
    
    // Spacing scale
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
        val xxxxl = 40.dp
    }
    
    // Elevation
    object Elevation {
        val small = 4.dp
        val medium = 8.dp
        val large = 12.dp
    }
    
    // Component sizes
    object Sizes {
        val shutterButton = 84.dp
        val galleryThumb = 48.dp
        val touchTarget = 44.dp
        val topBarHeight = 64.dp
        val bottomBarHeight = 112.dp
        val edgeToolsWidth = 56.dp
        val suggestionCardThumb = 120.dp to 80.dp
    }
    
    // Motion
    object Motion {
        val standardEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val decelerateEasing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        
        val shutterCompress = 160
        val shutterRebound = 220
        val sheetOpen = 320
        val chipApply = 180
        val cardStagger = 40
        val libraryInsert = 140
    }
}