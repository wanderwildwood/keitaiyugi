plugins {
    id("com.android.library")
    id("kotlin-android")
}

// LibretroDroid 0.13.2 (Swordfish90, GPL-3.0), carried here instead of fetched from JitPack so
// that its Kotlin side can be changed. The native library is the arm64-v8a
// liblibretrodroid.so from the 0.13.2 AAR, unchanged (sha256 b7aa907d...); its source is the
// upstream tag 0.13.2. See README.md in this folder.
android {
    namespace = "com.swordfish.libretrodroid"
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(deps.libs.androidx.lifecycle.runtime)
}
