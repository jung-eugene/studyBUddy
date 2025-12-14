package com.example.studybuddy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom Color Schemes for Light and Dark themes

//Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = BUPrimary,        // Red for main accents (buttons, top bar)
    secondary = BULight,        // Light red for highlights or chips
    background = BUBackground,  // Main background
    surface = BUSurface,        // Cards, surfaces
    onPrimary = Color.White,    // Text on red buttons
    onSecondary = Color.White,  // Text on secondary surfaces
    onBackground = BUOnSurface, // Text on background
    onSurface = BUOnSurface,    // Text on cards and general surfaces
    outline = BUOutline         // Divider lines, borders
)

//Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = BUPrimaryDark,    // Darker red
    secondary = BULight,
    background = BUDarkBackground,
    surface = BUDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = BUDarkOnSurface,
    outline = BUOutline
)

// Material 3 Theme Setup with Optional Dynamic Colors
@Composable
fun StudybuddyTheme(
    darkTheme: Boolean,   // explicitly controlled from ViewModel, not system
    dynamicColor: Boolean = false,   // <- force OFF dynamic color
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}



//@Composable
//fun StudybuddyTheme(     //Note the proper capitalization: matches your project name
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    // Optional dynamic color support for Android 12+
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context)
//            else dynamicLightColorScheme(context)
//        }
//
//        // Fallbacks for older gen android
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    // Apply color scheme + typography to all Composables
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}
