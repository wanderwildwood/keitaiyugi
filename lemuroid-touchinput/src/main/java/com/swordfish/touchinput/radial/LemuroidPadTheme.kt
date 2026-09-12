package com.swordfish.touchinput.radial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LemuroidPadTheme {
    private fun gray(
        luminosity: Float,
        opacity: Float,
    ): Color {
        return Color(luminosity, luminosity, luminosity, opacity)
    }

    val foregroundPadding: Dp = 8.dp
    val padding: Dp = 4.dp

    // Every one of these was a translucent grey over a blurred drop shadow -- icons at
    // 50% opacity, button faces at 50%, the pad body at 12.5%, backgrounds at 5-10%. That
    // is a sensible way to float controls over a colour screen and the worst possible one
    // here: an E Ink panel has no mid greys to render them in, so it dithers each of them
    // into a field of noise, and the blur spreads that noise past the edge of the shape.
    //
    // Solid white faces, solid black glyphs, inverted while held, and nothing blurred.
    //
    // ⚠ White-on-black, not the black-on-white this panel would prefer, and not by
    // choice. The area these sit on is not ours: LibretroDroid clears its GL surface to
    // opaque black in native code (video.cpp, glClearColor(0,0,0,1)) and that fills the
    // whole view, letterbox included. Black faces simply disappeared into it. Inverting
    // for real needs a LibretroDroid fork -- the same fork a halftone shader would need,
    // so the two wants travel together.
    // GlassSurface only draws its shadow when the colour has alpha, so a transparent one
    // turns the blur off rather than merely hiding it.
    private val icons = gray(0.0f, 1.0f)
    private val iconsPressed = gray(1.0f, 1.0f)

    private val level3Fill = gray(1.0f, 1.0f)
    private val level3FillPressed = gray(0.0f, 1.0f)
    val level3Shadow = Color.Transparent
    val level3ShadowWidth = 0.dp

    private val level2Fill = gray(1.0f, 1.0f)
    private val level2FillPressed = gray(0.0f, 1.0f)
    val level2Shadow = Color.Transparent
    val level2ShadowWidth = 0.dp

    val level1Fill = Color.Transparent
    val level1Shadow = Color.Transparent
    val level1ShadowWidth = 0.dp

    val level0CornerRadius = 0.dp
    val level0Fill = Color.Transparent
    val level0Shadow = Color.Transparent
    val level0ShadowWidth = 0.dp

    fun compositeFill(pressed: Boolean): Color {
        return if (pressed) {
            level2FillPressed
        } else {
            level2Fill
        }
    }

    fun foregroundFill(pressed: Boolean): Color {
        return if (pressed) {
            level3FillPressed
        } else {
            level3Fill
        }
    }

    fun icons(pressed: Boolean): Color {
        return if (pressed) {
            iconsPressed
        } else {
            icons
        }
    }
}

val LocalLemuroidPadTheme =
    compositionLocalOf<LemuroidPadTheme> {
        error("LemuroidPadTheme is missing")
    }
