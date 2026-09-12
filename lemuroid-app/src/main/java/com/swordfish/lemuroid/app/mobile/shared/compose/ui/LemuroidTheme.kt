package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mudita.mmd.ThemeMMD

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        errorContainer = md_theme_light_errorContainer,
        onError = md_theme_light_onError,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inverseSurface = md_theme_light_inverseSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        errorContainer = md_theme_dark_errorContainer,
        onError = md_theme_dark_onError,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inverseSurface = md_theme_dark_inverseSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

/**
 * Two colours, and every Material role given one of them.
 *
 * Built up from [lightColorScheme] rather than assembled from scratch, which matters: a
 * scheme with a role left Unspecified does not fail loudly, it paints something
 * transparent or black wherever that role is read. The FAQ dialog found this the hard
 * way -- it took its background from a role nothing had set, and rendered as text
 * floating unreadably over the game list behind it.
 */
private val EInkColorScheme =
    lightColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        primaryContainer = Color.Black,
        onPrimaryContainer = Color.White,
        secondary = Color.Black,
        onSecondary = Color.White,
        // The selected tab reads as a black pill. On two colours, inverting is the
        // strongest "this one" available.
        secondaryContainer = Color.Black,
        onSecondaryContainer = Color.White,
        tertiary = Color.Black,
        onTertiary = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF2F2F2),
        onSurfaceVariant = Color.Black,
        surfaceTint = Color.White,
        // Material draws app bars and navigation bars from the surfaceContainer family,
        // and lightColorScheme's defaults for those are its baseline *purple-tinted*
        // greys -- which is how a pale lavender got into the top and bottom bars of an
        // app with no colour. Overriding the obvious roles is not enough; these have to
        // be named too.
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color(0xFFF2F2F2),
        surfaceContainerHighest = Color(0xFFE6E6E6),
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFE6E6E6),
        inverseSurface = Color.Black,
        inverseOnSurface = Color.White,
        outline = Color.Black,
        outlineVariant = Color(0xFFBDBDBD),
        error = Color.Black,
        onError = Color.White,
        errorContainer = Color.White,
        onErrorContainer = Color.Black,
        scrim = Color(0x99000000),
    )

/**
 * Every screen in the app goes through here, so this is the whole reskin.
 *
 * Three things upstream does are right for a phone and wrong for this one. It defaults to
 * **dark**, which on a reflective E Ink panel is a screenful of ink that ghosts and costs
 * contrast rather than saving power. It takes **dynamic colour** from the wallpaper, which
 * is meaningless where there is no colour. And it is Material's palette rather than the
 * one every other app on this phone uses.
 *
 * ThemeMMD is Mudita's own Material3 theme, so all of that is answered by delegating to
 * it: light, flat, high contrast, and consistent with the apps the phone shipped with.
 *
 * The [darkTheme] parameter is kept, unused, so the signature still matches upstream's and
 * merges stay quiet. There is no dark mode here and there should not be one.
 */
@Composable
fun AppTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    ThemeMMD {
        MaterialTheme(colorScheme = EInkColorScheme) {
            content()
        }
    }
}
