package com.swordfish.touchinput.radial.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The icons this app draws, all Material Symbols (Apache-2.0), kept as the path data they
 * ship as rather than transcribed into a builder: an icon is then one line, and adding one is
 * not a chore that invites reusing a wrong icon instead.
 *
 * This replaced Google's `material-icons-extended`, which is a different drawing of the same
 * ideas -- a heavier, rounder cut -- and which this app was using in two weights at once. Every
 * app of this shop draws from one set now, and any glyph that appears in more than one of them
 * is the same path data in each.
 *
 * The four arms of the d-pad, drawn over the game rather than beside it. The filled and the
 * outlined cuts of a chevron are the same path, so there is nothing to choose here.
 */
object Icons {

    /**
     * Material Symbols are authored in a 960 grid whose origin sits at the bottom left, so the
     * path data runs from -960 to 0 vertically. Shifting the whole thing down by 960 puts it in
     * the top-left grid Compose uses.
     */
    private fun symbol(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        )
            .addGroup(name = name, translationY = 960f)
            .addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.Black),
            )
            .clearGroup()
            .build()

    val ChevronUp: ImageVector = symbol("ChevronUp", "M480-528 296-344l-56-56 240-240 240 240-56 56-184-184Z")
    val ChevronDown: ImageVector = symbol("ChevronDown", "M480-344 240-584l56-56 184 184 184-184 56 56-240 240Z")
    val ChevronLeft: ImageVector = symbol("ChevronLeft", "M560-240 320-480l240-240 56 56-184 184 184 184-56 56Z")
    val ChevronRight: ImageVector = symbol("ChevronRight", "M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z")
}
