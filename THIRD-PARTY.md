# What this app is built from

Handheld Games is a fork, and most of it is other people's work. This file names all
of it, with the licence each part carries and where its source can be had.

## The app

| | |
|---|---|
| **[Lemuroid](https://github.com/Swordfish90/Lemuroid)** | GPL-3.0 — Filippo Scognamiglio |

This app is a fork of Lemuroid, cut down to one console and reskinned for an E Ink
screen. The emulation, the library scanner, the touch controls and the shape of the
whole thing are theirs. Its git history is preserved in this repository.

## Bundled at runtime

| | |
|---|---|
| **[LibretroDroid](https://github.com/Swordfish90/LibretroDroid)** | GPL-3.0 — Filippo Scognamiglio |
| **[Gambatte](https://github.com/libretro/gambatte-libretro)** | **GPL-2.0-only** — Sindre Aamås and libretro contributors |

Gambatte is the Game Boy emulator itself. It ships inside the APK as a prebuilt
`libgambatte_libretro_android.so`, taken from
[LemuroidCores](https://github.com/Swordfish90/LemuroidCores), and is loaded at runtime
through the libretro interface. Its complete corresponding source is at the link above.

⚠ **Gambatte is GPL-2.0 without an "or any later version" clause, and this app is
GPL-3.0.** Those two licences are not compatible for combining into a single work. The
position relied on here is the same one Lemuroid, RetroArch and every other libretro
frontend rely on: the core is a separate program, distributed alongside rather than
linked into the application, communicating across a defined C interface. That reading is
widely used and not universally agreed. It is recorded here rather than left implicit,
because anyone redistributing this should know it is the arrangement they are inheriting.

## Data and design

| | |
|---|---|
| **[libretro database](https://github.com/libretro/libretro-database)** | CC-BY-SA-4.0 |
| **[MMD](https://github.com/mudita/MMD)** | Apache-2.0 — Mudita |
| **Lato** | SIL Open Font License 1.1, bundled by MMD |

The metadata database supplies game titles and developers. The copy here has been cut
down to Game Boy and Game Boy Color rows; see `tools/trim-metadata.py`.

## Not included

No game files are bundled and none ever will be.
