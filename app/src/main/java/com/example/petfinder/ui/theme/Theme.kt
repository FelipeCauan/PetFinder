package com.example.petfinder.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PetFinderPrimary,
    onPrimary = PetFinderOnPrimary,
    primaryContainer = PetFinderPrimaryContainer,
    onPrimaryContainer = PetFinderOnPrimaryContainer,
    background = PetFinderBackground,
    surface = PetFinderSurface,
    surfaceVariant = PetFinderSurfaceVariant,
    onBackground = PetFinderOnSurface,
    onSurface = PetFinderOnSurface,
    onSurfaceVariant = PetFinderOnSurfaceVariant,
    outlineVariant = PetFinderOutlineVariant,
    error = PetFinderError
)

@Composable
fun PetFinderTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
