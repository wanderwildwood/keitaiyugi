package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

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
 * it: light, flat, high contrast, and consistent with the apps the phone shipped with. The
 * scheme is [monochrome] rather than MMD's own, for the reason written there — and it is
 * the same file in every app of this shop, so the greys this app used to carry for its
 * cards and bars are gone. A card draws its edge with [CardMMD] instead.
 *
 * The [darkTheme] parameter is kept, unused, so the signature still matches upstream's and
 * merges stay quiet. There is no dark mode here and there should not be one.
 */
@Composable
fun AppTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    ThemeMMD(colorScheme = monochrome, content = content)
}
