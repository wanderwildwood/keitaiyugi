package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Box art comes down from the metadata database as full-colour JPEGs, and this screen has
 * no colour to show them in.
 *
 * Leaving them in colour does not mean they arrive in colour — it means the panel converts
 * them itself, and it does that badly: a cheap channel mix that sends saturated reds and
 * blues to nearly the same grey, so a shelf of covers turns into a shelf of similar
 * smudges. Converting here instead uses a proper luminance weighting, which is the
 * difference between a recognisable cover and a noisy rectangle.
 *
 * Applied to the fallback art too, which is otherwise a coloured tile with initials on it.
 */
internal val GreyscaleCover: ColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
