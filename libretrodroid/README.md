# LibretroDroid 0.13.2, carried in this repo

[LibretroDroid](https://github.com/Swordfish90/LibretroDroid) by Filippo Scognamiglio, GPL-3.0,
runs the Game Boy core: it loads it, draws its frames with OpenGL and plays its sound.

Until 0.1.36 the app fetched it from JitPack. It is carried here so its Kotlin side can be
changed, and the native part is left exactly as it was:

- `src/main/java` is the Kotlin and Java source of upstream tag `0.13.2`.
- `src/main/jniLibs/arm64-v8a/liblibretrodroid.so` is the native library from the 0.13.2 AAR
  that JitPack served (sha256 `b7aa907dceb17bbfca87862504b2285e37995b8b8405cdb5709cb1011ae66719`).
  Its source is the C++ under `libretrodroid/src/main/cpp` at the same tag. Only arm64-v8a is
  kept, as for the core.

Changes from upstream are listed in the git history of this folder.
