import java.util.Properties
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlinx-serialization")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    defaultConfig {
        // This fork's own version, not upstream's. The Lemuroid release it is based on
        // is recorded in the README instead, where it can be read without git.
        // versionCode is major*10000 + minor*100 + patch, as the other apps here.
        versionCode = 101
        versionName = "0.1.1"
        // Its own id, so it installs alongside anything upstream rather than colliding
        // with it. The code package stays com.swordfish.lemuroid: that is shared
        // code and its attribution, and renaming it would be a rename for its own sake.
        applicationId = "com.wanderwildwood.keitaiyugi"

        // The Kompakt reports arm64-v8a and nothing else can run on it, so the other
        // three ABIs are download and nothing more.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
    // No product flavours. Upstream has two dimensions: free/play, because some of its
    // dependencies are closed source, and bundle/dynamic, because Google Play delivers
    // cores as dynamic features. This app has one core, is sideloaded onto a degoogled
    // phone, and will never be on Play -- so both dimensions had exactly one live value
    // and the play half existed only to be not built. Removing it means Google Play
    // Services and Google Drive cannot be built here, rather than merely are not.
    //
    // Build tasks are assembleDebug / assembleRelease now, not assembleFreeBundle*.

    packagingOptions {
        jniLibs {
            // Stripping created some issues with some libretro cores such as ppsspp
            keepDebugSymbols += setOf("*/*/*_libretro_android.so")
            useLegacyPackaging = true

            // This app plays Game Boy games, so it carries the Game Boy core and no
            // others. Upstream bundles twenty, and they are most of the download --
            // fbneo alone is 208MB on disk, mame2003_plus another 126MB.
            //
            // Done here rather than by deleting anything, because the cores are a git
            // submodule shared with upstream and the files under bundled-cores are
            // symlinks into it. Excluding by name in our own build file leaves that tree
            // untouched and says plainly, in one readable list, what is left out.
            excludes +=
                setOf(
                    "**/libcitra_libretro_android.so",
                    "**/libdesmume_libretro_android.so",
                    "**/libdosbox_pure_libretro_android.so",
                    "**/libfbneo_libretro_android.so",
                    "**/libfceumm_libretro_android.so",
                    "**/libgenesis_plus_gx_libretro_android.so",
                    "**/libhandy_libretro_android.so",
                    "**/libmame2003_plus_libretro_android.so",
                    "**/libmednafen_ngp_libretro_android.so",
                    "**/libmednafen_pce_fast_libretro_android.so",
                    "**/libmednafen_wswan_libretro_android.so",
                    "**/libmelonds_libretro_android.so",
                    "**/libmgba_libretro_android.so",
                    "**/libmupen64plus_next_gles3_libretro_android.so",
                    "**/libpcsx_rearmed_libretro_android.so",
                    "**/libppsspp_libretro_android.so",
                    "**/libprosystem_libretro_android.so",
                    "**/libsnes9x_libretro_android.so",
                    "**/libstella_libretro_android.so",
                )
        }
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/library_release.kotlin_module")
        }
    }

    // A real keystore in signing/ signs every build type when it is present, so the very
    // first install is already release-signed and a later update can never hit
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE. It is gitignored, and **there is no fallback**:
    // a fresh clone builds an unsigned release APK, which will not install anywhere.
    //
    // Upstream's release config is deliberately gone. It named a keystore with the
    // password "lemuroid" in the file, which is exactly the shape of key that lets
    // anyone build an APK that installs over yours -- Android decides whether an update
    // is legitimate purely by signature. Unsigned is the right failure: it fails loudly
    // rather than handing someone something installable.
    val signingPropertiesFile = rootProject.file("signing/signing.properties")
    val realSigningConfig =
        if (signingPropertiesFile.isFile) {
            val signingProperties =
                Properties().apply { signingPropertiesFile.inputStream().use(::load) }
            signingConfigs.create("real") {
                storeFile = rootProject.file("signing/signing.keystore")
                storePassword = signingProperties.getProperty("STORE_PASSWORD")
                keyAlias = signingProperties.getProperty("KEY_ALIAS")
                keyPassword = signingProperties.getProperty("KEY_PASSWORD")
            }
        } else {
            null
        }

    buildTypes {
        getByName("release") {
            // The one thing that differs between a release built here and the one GitHub
            // publishes: AGP stamps the git revision into META-INF, and the build box works
            // from an rsync with no .git, so it writes NO_SUPPORTED_VCS_FOUND where the CI
            // runner writes the commit. Off, so the two have identical contents.
            vcsInfo {
                include = false
            }

            isMinifyEnabled = true
            // Upstream minifies code but not resources, so everything unreferenced still
            // shipped: drawables for twenty consoles, the leanback and TV resources this
            // fork orphaned, and font weights nothing asks for.
            isShrinkResources = true
            realSigningConfig?.let { signingConfig = it }
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            resValue("string", "lemuroid_name", "Handheld Games")
        }
        getByName("debug") {
            realSigningConfig?.let { signingConfig = it }
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "lemuroid_name", "Handheld Games (debug)")
        }
    }

    lint {
        disable += setOf("MissingTranslation", "ExtraTranslation", "EnsureInitializerMetadata")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = deps.versions.kotlinExtension
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "com.swordfish.lemuroid"
}

dependencies {
    implementation(project(":retrograde-util"))
    implementation(project(":retrograde-app-shared"))
    implementation(project(":lemuroid-metadata-libretro-db"))
    implementation(project(":lemuroid-touchinput"))

    "baselineProfile"(project(":baselineprofile"))
    implementation(deps.libs.androidx.profileInstaller)

    implementation(project(":bundled-cores"))

    implementation(project(":lemuroid-app-ext-free"))

    implementation(deps.libs.androidx.navigation.navigationFragment)
    implementation(deps.libs.androidx.navigation.navigationUi)
    implementation(deps.libs.androidx.navigation.compose)
    implementation(deps.libs.material)
    implementation(deps.libs.coil.coil)
    implementation(deps.libs.coil.coilCompose)
    implementation(deps.libs.androidx.appcompat.constraintLayout)
    implementation(deps.libs.androidx.activity.activity)
    implementation(deps.libs.androidx.activity.activityKtx)
    implementation(deps.libs.androidx.activity.compose)
    implementation(deps.libs.androidx.appcompat.appcompat)
    implementation(deps.libs.androidx.preferences.preferencesKtx)
    implementation(deps.libs.arch.work.runtime)
    implementation(deps.libs.arch.work.runtimeKtx)
    implementation(deps.libs.androidx.lifecycle.commonJava8)
    implementation(deps.libs.androidx.lifecycle.reactiveStreams)

    kapt(deps.libs.androidx.lifecycle.processor)

    implementation(deps.libs.androidx.appcompat.recyclerView)
    implementation(deps.libs.androidx.paging.common)
    implementation(deps.libs.androidx.paging.runtime)
    implementation(deps.libs.androidx.room.common)
    implementation(deps.libs.androidx.room.runtime)
    implementation(deps.libs.androidx.room.ktx)
    implementation(deps.libs.dagger.android.core)
    implementation(deps.libs.dagger.android.support)
    implementation(deps.libs.dagger.core)
    implementation(deps.libs.kotlinxCoroutinesAndroid)
    implementation(deps.libs.okHttp3)
    implementation(deps.libs.okio)
    implementation(deps.libs.retrofit)
    implementation(deps.libs.flowPreferences)
    implementation(deps.libs.guava)
    implementation(deps.libs.androidx.documentfile)
    implementation(deps.libs.harmony)
    implementation(deps.libs.startup)
    implementation(deps.libs.kotlin.serialization)
    implementation(deps.libs.kotlin.serializationJson)

    implementation(platform(deps.libs.androidx.compose.composeBom))
    implementation(deps.libs.androidx.compose.material3)
    // Mudita's own design system: what makes this look like the rest of the phone.
    implementation("com.mudita:MMD:1.0.2")
    implementation(deps.libs.androidx.compose.constraintLayout)
    debugImplementation(deps.libs.androidx.compose.tooling)
    implementation(deps.libs.androidx.compose.toolingPreview)
    implementation(deps.libs.androidx.compose.accompanist.systemUiController)
    implementation(deps.libs.androidx.compose.accompanist.navigationMaterial)
    implementation(deps.libs.androidx.compose.accompanist.drawablePainter)
    implementation(deps.libs.androidx.paging.compose)
    implementation(deps.libs.androidx.lifecycle.viewModelCompose)
    implementation(deps.libs.composeHtmlText)

    implementation(deps.libs.composeSettings.uiTiles)
    implementation(deps.libs.composeSettings.uiTilesExtended)
    implementation(deps.libs.composeSettings.diskStorage)
    implementation(deps.libs.composeSettings.memoryStorage)

    implementation(deps.libs.libretrodroid)

    // Uncomment this when using a local aar file.
    // implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    kapt(deps.libs.dagger.android.processor)
    kapt(deps.libs.dagger.compiler)
}

fun usePlayDynamicFeatures(): Boolean {
    val task = gradle.startParameter.taskRequests.toString()
    return task.contains("Play") && task.contains("Dynamic")
}
