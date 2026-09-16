#!/usr/bin/env python3
"""
Draws the launcher mark.

A controller face: the cross under the left thumb, two buttons under the right. Not a
console — the shape of a Game Boy is Nintendo's and this app is careful not to be about
one particular machine anyway. What is drawn is the thing your hands do, which is also
what the name says: 携帯遊戯, portable play.

**Drawn as pixels**, on a grid of 6-unit cells, because that is what the app is for and
what it does: it takes a picture made of large square pixels and puts it on a screen
made of small ones. A mark drawn in the idiom of the thing it launches says more than a
tasteful line drawing of a controller would, and it settles an ambiguity that the line
version could not — a thin cross reads as a plus sign, and at 48 pixels the first
attempt here read as a plus sign next to a colon. Blocked out, it reads as a d-pad.

The rules this follows are the ones measured off the owner's chanterelle and recorded in
kinokocho's tools/make-art.py:

  Few lines.  Three shapes and twenty-two cells. At the size a launcher shows this,
              a fourth shape would close up into a blot.
  One weight. Nothing is ranked above anything else.

Two are answered differently here than in a drawing, and the difference is the point:

  Crooked.    A pixel grid cannot wobble, and faking a wobble in it would just look
              like a mistake. So the crookedness is in the composition instead: the
              two buttons sit on a diagonal rather than stacked, at different distances
              from the cross, and the cross sits left of the centre of the frame rather
              than in it. Nothing here is centred, but everything is on the grid.
  Thin.       Does not apply. A launcher mark is a mark, not a diagram, and a solid
              shape is what survives being scaled to 24 pixels. yorimichi's blaze is
              solid black for the same reason.

The fifth, *no black*, is about tone on a page of line drawings, and a launcher icon is
the one place exempt from it.

    python3 tools/make-icon.py

Adaptive icons are masked to a shape the launcher picks, and only the central 66 of the
108 units are certain to survive. Every filled cell is checked corner by corner against
that circle rather than against the bounding box — square pixels reach further into the
corners than a curve does, which is exactly the mistake that gets a d-pad's arm clipped.
"""

import math
import os
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
DRAWABLE = os.path.join(ROOT, "lemuroid-app", "src", "main", "res", "drawable")
MIPMAP = os.path.join(ROOT, "lemuroid-app", "src", "main", "res", "mipmap-anydpi-v26")
RES = os.path.join(ROOT, "lemuroid-app", "src", "main", "res")
SHOTS = os.path.join(ROOT, "screenshots")

CENTRE = (54.0, 54.0)
SAFE_RADIUS = 33.0
CELL = 4.2
BLEED = 0.02                # rectangles overlap by a hair so no seam shows between them

# Twelve columns, ten rows. The cross takes the left six, the two buttons the right, set
# on a diagonal at unequal distances from it rather than stacked in a tidy column.
#
# Two of those columns are air. The mark began ten wide, with a single cell between the
# cross's arm and the A button, and at launcher size the two closed up into one shape —
# a thumb and a blot rather than a cross and two buttons. Each column of air costs cell
# size, because the drawing has to stay inside the 33 units the adaptive mask guarantees:
# 5.0 at ten wide, 4.6 at eleven, 4.2 here. Paying in pixel size rather than in
# composition is deliberate — the buttons stay on their diagonal, off-centre and at
# unequal distances, which is the one place this drawing is allowed to be crooked.
GRID = [
    "............",
    ".........XXX",
    "..XX.....XXX",
    "..XX.....XXX",
    "XXXXXX......",
    "XXXXXX......",
    "..XX........",
    "..XX....XXX.",
    "........XXX.",
    "........XXX.",
]

# Top-left of the grid, so whatever shape it is centres on 54,54.
ORIGIN = (CENTRE[0] - len(GRID[0]) * CELL / 2.0, CENTRE[1] - len(GRID) * CELL / 2.0)


def cells():
    for row, line in enumerate(GRID):
        for col, mark in enumerate(line):
            if mark == "X":
                yield col, row


def check_inside():
    """A filled cell whose corner leaves the circle is a corner the launcher may cut."""
    worst = 0.0
    for col, row in cells():
        x0 = ORIGIN[0] + col * CELL
        y0 = ORIGIN[1] + row * CELL
        for x, y in ((x0, y0), (x0 + CELL, y0), (x0, y0 + CELL), (x0 + CELL, y0 + CELL)):
            reach = math.hypot(x - CENTRE[0], y - CENTRE[1])
            worst = max(worst, reach)
            if reach > SAFE_RADIUS:
                raise SystemExit(
                    "the cell at column %d row %d reaches %.1f from centre, past the %.0f "
                    "the mask guarantees" % (col, row, reach, SAFE_RADIUS)
                )
    return worst


def runs():
    """Maximal horizontal runs, so the drawing is a handful of rectangles, not 22."""
    for row, line in enumerate(GRID):
        col = 0
        while col < len(line):
            if line[col] != "X":
                col += 1
                continue
            start = col
            while col < len(line) and line[col] == "X":
                col += 1
            yield start, col - start, row


def path_data():
    parts = []
    for start, length, row in runs():
        x = ORIGIN[0] + start * CELL - BLEED
        y = ORIGIN[1] + row * CELL - BLEED
        w = length * CELL + BLEED * 2
        h = CELL + BLEED * 2
        parts.append("M%.2f,%.2f h%.2f v%.2f h%.2f z" % (x, y, w, h, -w))
    return " ".join(parts)


def main():
    worst = check_inside()
    data = path_data()

    os.makedirs(DRAWABLE, exist_ok=True)
    os.makedirs(MIPMAP, exist_ok=True)
    os.makedirs(SHOTS, exist_ok=True)

    with open(os.path.join(DRAWABLE, "lemuroid_launcher_foreground.xml"), "w") as handle:
        handle.write(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            "<!-- Generated by tools/make-icon.py. Do not edit by hand. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n\n'
            "    <!-- the cross and the two buttons, on a pixel grid -->\n"
            '    <path\n        android:fillColor="#FF000000"\n'
            '        android:pathData="%s" />\n\n</vector>\n' % data
        )

    with open(os.path.join(DRAWABLE, "lemuroid_launcher_background_drawable.xml"), "w") as handle:
        handle.write(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            '    <path android:fillColor="#FFFFFF" android:pathData="M0,0h108v108h-108z" />\n'
            "</vector>\n"
        )

    for name in ("lemuroid_launcher.xml", "lemuroid_launcher_round.xml"):
        with open(os.path.join(MIPMAP, name), "w") as handle:
            handle.write(
                '<?xml version="1.0" encoding="utf-8"?>\n'
                '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                '    <background android:drawable="@drawable/lemuroid_launcher_background_drawable" />\n'
                '    <foreground android:drawable="@drawable/lemuroid_launcher_foreground" />\n'
                '    <monochrome android:drawable="@drawable/lemuroid_launcher_monochrome" />\n'
                "</adaptive-icon>\n"
            )

    with open(os.path.join(SHOTS, "icon.svg"), "w") as handle:
        handle.write(
            '<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" '
            'viewBox="0 0 108 108">\n'
            '  <rect width="108" height="108" fill="#fff"/>\n'
            '  <path d="%s" fill="#000"/>\n</svg>\n' % data
        )

    # The monochrome layer is the same drawing; a themed launcher tints it itself.
    import shutil
    shutil.copyfile(
        os.path.join(DRAWABLE, "lemuroid_launcher_foreground.xml"),
        os.path.join(DRAWABLE, "lemuroid_launcher_monochrome.xml"),
    )

    # Legacy PNGs. minSdk is 23 here, so anything below 26 gets these rather than the
    # adaptive icon, and they have to carry the background themselves. Cropped to the
    # central 72 units, which is what a launcher actually shows of an adaptive icon.
    legacy = os.path.join(SHOTS, "icon-legacy.svg")
    with open(legacy, "w") as handle:
        handle.write(
            '<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" '
            'viewBox="18 18 72 72">\n'
            '  <rect x="18" y="18" width="72" height="72" fill="#fff"/>\n'
            '  <path d="%s" fill="#000"/>\n</svg>\n' % data
        )
    for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                          ("xxhdpi", 144), ("xxxhdpi", 192)):
        out = os.path.join(RES, "mipmap-" + density)
        os.makedirs(out, exist_ok=True)
        square = os.path.join(out, "lemuroid_launcher.png")
        subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size),
                        legacy, "-o", square], check=True)
        subprocess.run(["convert", square,
                        "(", "-size", "%dx%d" % (size, size), "xc:black",
                        "-fill", "white", "-draw",
                        "circle %g,%g %g,%g" % (size / 2.0, size / 2.0, size / 2.0, 0),
                        ")", "-alpha", "off", "-compose", "CopyOpacity", "-composite",
                        os.path.join(out, "lemuroid_launcher_round.png")], check=True)

    filled = sum(line.count("X") for line in GRID)
    print("%d cells, furthest corner %.1f of the %.0f allowed" % (filled, worst, SAFE_RADIUS))


if __name__ == "__main__":
    main()
