# 携帯遊戯 keitaiyugi — Handheld Games

Game Boy games on the [Mudita Kompakt](https://mudita.com/products/kompakt/), drawn for its
E Ink screen rather than in spite of it.

*Keitai yūgi* is 携帯遊戯 — portable play. Which is what a Game Boy was, and what this phone
can be on a quiet afternoon.

A fork of **[Lemuroid](https://github.com/Swordfish90/Lemuroid)** by Filippo Scognamiglio,
cut down to one console and reskinned for a screen with two colours. Emulation is
[Gambatte](https://docs.libretro.com/library/gambatte/) through
[LibretroDroid](https://github.com/Swordfish90/LibretroDroid). None of the hard part is mine.

## Why this screen at all

The Kompakt's panel is 480 pixels wide. A Game Boy is 160. That is exactly **3x** — the
picture fills the width with no resampling and no stretching, and leaves room underneath for
the controls. The fit is a coincidence, but it is a good one.

What it asks for in return is patience with motion. A room in Zelda holds still while a
sprite crosses it, so only a small rectangle of the screen changes and the panel only has to
repaint that. Mario scrolls, and although that changes fewer pixels than you would think —
about a tenth, since flat ground shifted sideways is still flat ground — those changes land
everywhere, so the rectangle that must be repainted is nearly the whole screen. Measured here
it is 0.9% against 93%, from the same sprite moving at the same speed.

So: puzzle, turn-based and fixed-camera games play well. Zelda plays well, which is the
surprising one. Mario does not.

## What is different from Lemuroid

- **One console.** Twenty bundled cores become one, and four ABIs become one — the Kompakt is
  arm64-v8a. Upstream's debug APK is 212 MB; the signed release here is 12 MB.
- **Grey, not green.** Upstream renders the Game Boy's own green LCD. This panel has no colour
  to show it in, so the tint arrives only as contrast lost between the four shades that are
  the whole picture. The palette is a true grey ramp; frame mixing is off, because the screen
  smears without help.
- **Mudita's design system.** Every screen goes through [MMD](https://github.com/mudita/MMD),
  so it looks like the apps the phone shipped with: light, flat, high contrast. Upstream
  defaults to a dark theme with colours taken from the wallpaper, both of which cost more than
  they give here.
- **Paged lists.** Scrolling on E Ink is limited by the panel, not the processor, so the only
  thing that helps is producing fewer frames. The lists use MMD's, which page rather than
  glide.
- **Grey cover art.** Converted properly rather than left for the panel to do badly.
- **No system chooser.** There is one system; the tab opens the games.

## Games

**No games are included and none ever will be.** The app reads Game Boy files you provide:
point it at a directory in Settings.

If you have none, the [Homebrew Hub](https://hh.gbdev.io) is an archive of Game Boy games
written in the last few years and given away by the people who wrote them. Plenty of it is
puzzle and turn-based work that suits this screen better than most commercial releases do.

## Where this is up to

Version 0.1.0. It plays, on a real Kompakt, and has not been through anybody else's hands yet.
Forked from Lemuroid at `53752bf2`.

## Licence

GPL-3.0-only, © wander wildwood, inheriting Lemuroid's GPL-3.0. See [COPYING](COPYING).
