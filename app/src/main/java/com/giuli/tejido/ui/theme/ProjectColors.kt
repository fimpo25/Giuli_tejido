package com.giuli.tejido.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Tonos de un color de proyecto, para modo claro y oscuro. */
data class ProjectPalette(
    val name: String,
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
)

val ProjectPalettes: List<ProjectPalette> = listOf(
    ProjectPalette(
        "Frambuesa",
        Color(0xFF9C4A5A), Color(0xFFFFD9DF), Color(0xFF3F0018),
        Color(0xFFFFB1C0), Color(0xFF5E1129), Color(0xFF7D3243), Color(0xFFFFD9DF),
    ),
    ProjectPalette(
        "Coral",
        Color(0xFFA53D1F), Color(0xFFFFDBD1), Color(0xFF3B0900),
        Color(0xFFFFB5A0), Color(0xFF5F1500), Color(0xFF852810), Color(0xFFFFDBD1),
    ),
    ProjectPalette(
        "Mostaza",
        Color(0xFF7B5900), Color(0xFFFFDEA0), Color(0xFF261900),
        Color(0xFFF6BE2B), Color(0xFF412D00), Color(0xFF5D4200), Color(0xFFFFDEA0),
    ),
    ProjectPalette(
        "Oliva",
        Color(0xFF3B6A1C), Color(0xFFBCF295), Color(0xFF0B2000),
        Color(0xFFA1D57C), Color(0xFF173800), Color(0xFF245103), Color(0xFFBCF295),
    ),
    ProjectPalette(
        "Menta",
        Color(0xFF006A60), Color(0xFF74F8E5), Color(0xFF00201C),
        Color(0xFF53DBC9), Color(0xFF003731), Color(0xFF005048), Color(0xFF74F8E5),
    ),
    ProjectPalette(
        "Azul",
        Color(0xFF2D5DA8), Color(0xFFD7E3FF), Color(0xFF001A40),
        Color(0xFFABC7FF), Color(0xFF002F65), Color(0xFF08458F), Color(0xFFD7E3FF),
    ),
    ProjectPalette(
        "Lavanda",
        Color(0xFF6B4FA0), Color(0xFFEBDDFF), Color(0xFF25005A),
        Color(0xFFD4BBFF), Color(0xFF3B1C6F), Color(0xFF533787), Color(0xFFEBDDFF),
    ),
    ProjectPalette(
        "Piedra",
        Color(0xFF5B5F63), Color(0xFFDFE3E8), Color(0xFF181C20),
        Color(0xFFC3C7CC), Color(0xFF2D3135), Color(0xFF43474B), Color(0xFFDFE3E8),
    ),
)

fun paletteAt(index: Int): ProjectPalette =
    ProjectPalettes[index.mod(ProjectPalettes.size)]

/** Color de muestra para el selector (el principal en modo claro). */
fun swatchColor(index: Int): Color = paletteAt(index).lightPrimary

fun ColorScheme.withProjectColor(index: Int, dark: Boolean): ColorScheme {
    val p = paletteAt(index)
    return if (dark) {
        copy(
            primary = p.darkPrimary,
            onPrimary = p.darkOnPrimary,
            primaryContainer = p.darkContainer,
            onPrimaryContainer = p.darkOnContainer,
            secondaryContainer = p.darkContainer,
            onSecondaryContainer = p.darkOnContainer,
            surfaceTint = p.darkPrimary,
        )
    } else {
        copy(
            primary = p.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = p.lightContainer,
            onPrimaryContainer = p.lightOnContainer,
            secondaryContainer = p.lightContainer,
            onSecondaryContainer = p.lightOnContainer,
            surfaceTint = p.lightPrimary,
        )
    }
}

/** Aplica el color del proyecto a todo lo que esté adentro. */
@Composable
fun ProjectTheme(colorIndex: Int, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.withProjectColor(colorIndex, dark),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
