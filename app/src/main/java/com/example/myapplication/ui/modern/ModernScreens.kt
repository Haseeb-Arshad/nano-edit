package com.example.myapplication.ui.modern

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.myapplication.controller.EditController
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.DesignTokens

@Composable
fun ModernEditorScreen(
    src: Uri?,
    controller: EditController,
    onBack: () -> Unit,
    autoSceneLift: Boolean = false
) {
    // Delegate to existing EditorScreen for now
    com.example.myapplication.ui.screens.EditorScreen(
        src = src, 
        controller = controller, 
        autoSmartEnhance = autoSceneLift, 
        onBack = onBack
    )
}

@Composable
fun ModernGalleryScreen(
    onBack: () -> Unit,
    onOpen: (Uri) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md)
                    .statusBarsPadding(),
                cornerRadius = DesignTokens.Radius.pill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignTokens.Sizes.topBarHeight)
                        .padding(horizontal = DesignTokens.Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "Gallery",
                        color = DesignTokens.Colors.Light.textHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.size(40.dp)) // Balance the back button
                }
            }
            
            // Gallery content placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassmorphicCard(cornerRadius = DesignTokens.Radius.lg) {
                    Text(
                        text = "Gallery coming soon",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernSettingsScreen(
    onBack: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md)
                    .statusBarsPadding(),
                cornerRadius = DesignTokens.Radius.pill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignTokens.Sizes.topBarHeight)
                        .padding(horizontal = DesignTokens.Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "Settings",
                        color = DesignTokens.Colors.Light.textHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.size(40.dp)) // Balance the back button
                }
            }
            
            // Settings content placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassmorphicCard(cornerRadius = DesignTokens.Radius.lg) {
                    Text(
                        text = "Settings coming soon",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl)
                    )
                }
            }
        }
    }
}