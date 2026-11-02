package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/keitaiyugi"

/**
 * Everything the APK carries that this project did not write, as the Gradle build pulls it in
 * (the app and the modules under it) plus the Game Boy core it loads at runtime. Licences are
 * the ones each project declares in its own published metadata.
 */
val LICENCE_COMPONENTS: List<LicenceComponent> =
    listOf(
        LicenceComponent(
            "Lemuroid, the app this is forked from",
            "Filippo Scognamiglio",
            "GPL-3.0",
            "https://github.com/Swordfish90/Lemuroid",
        ),
        LicenceComponent("LibretroDroid", "Filippo Scognamiglio", "GPL-3.0", "https://github.com/Swordfish90/LibretroDroid"),
        LicenceComponent(
            "Gambatte core (Game Boy), loaded at run time",
            "Sindre Aamås and libretro contributors",
            "GPL-2.0-only",
            "https://github.com/libretro/gambatte-libretro",
        ),
        LicenceComponent(
            "libretro database (game titles and developers)",
            "libretro contributors",
            "CC-BY-SA-4.0",
            "https://github.com/libretro/libretro-database",
        ),
        LicenceComponent("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        LicenceComponent("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        LicenceComponent(
            "AndroidX: Core, AppCompat, Activity, Lifecycle, Navigation, Room, Paging, Preference, WorkManager, " +
                "Startup, DocumentFile, ConstraintLayout, RecyclerView",
            "Google",
            "Apache-2.0",
            "https://developer.android.com/jetpack",
        ),
        LicenceComponent(
            "Jetpack Compose (UI, Foundation, Material 3) and Accompanist",
            "Google",
            "Apache-2.0",
            "https://developer.android.com/jetpack/compose",
        ),
        LicenceComponent(
            "Material Components for Android",
            "Google",
            "Apache-2.0",
            "https://github.com/material-components/material-components-android",
        ),
        LicenceComponent("Guava", "Google", "Apache-2.0", "https://github.com/google/guava"),
        LicenceComponent("Dagger", "Google", "Apache-2.0", "https://github.com/google/dagger"),
        LicenceComponent(
            "Kotlin standard library, coroutines, serialization, immutable collections",
            "JetBrains",
            "Apache-2.0",
            "https://kotlinlang.org",
        ),
        LicenceComponent("OkHttp and Okio", "Square", "Apache-2.0", "https://square.github.io/okhttp/"),
        LicenceComponent("Retrofit", "Square", "Apache-2.0", "https://square.github.io/retrofit/"),
        LicenceComponent("Coil", "Coil contributors", "Apache-2.0", "https://coil-kt.github.io/coil/"),
        LicenceComponent("Timber", "Jake Wharton", "Apache-2.0", "https://github.com/JakeWharton/timber"),
        LicenceComponent("PadKit", "Filippo Scognamiglio", "Apache-2.0", "https://github.com/Swordfish90/PadKit"),
        LicenceComponent("Compose Settings", "Alorma", "MIT", "https://github.com/alorma/compose-settings"),
        LicenceComponent(
            "Flow Preferences",
            "Fred Porciuncula",
            "Apache-2.0",
            "https://github.com/fredporciuncula/flow-preferences",
        ),
        LicenceComponent("Harmony", "Frybits", "Apache-2.0", "https://github.com/frybits/harmony"),
        LicenceComponent(
            "Compose Material 3 HTML text",
            "Charlex",
            "Apache-2.0",
            "https://github.com/charlex/compose-material-html-text",
        ),
        LicenceComponent("Material Symbols icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
    )

/**
 * Each distinct licence text once: family id and the raw resource. The Game Boy core is
 * GPL-2.0 only, which is not the GPL-3.0 the app is under, so its text is listed too under
 * the id "gpl2" (see [LICENCE_EXTRA_TEXT_IDS]).
 */
val LICENCE_RAW: List<Pair<String, Int>> =
    listOf(
        "gpl" to R.raw.gpl_3_0,
        "gpl2" to R.raw.gpl_2_0,
        "apache" to R.raw.apache_2_0,
        "mit" to R.raw.mit,
        "ofl" to R.raw.ofl_1_1,
        "cc-by" to R.raw.cc_by_sa_4_0,
    )

/** Texts that share a licence family with another one, so the shared check cannot match them to a component. */
val LICENCE_EXTRA_TEXT_IDS: Set<String> = setOf("gpl2")

@Composable
fun rememberLicencesData(): LicencesData =
    LicencesData(
        appName = "Handheld Games",
        sourceUrl = SOURCE_URL,
        licenceLine = stringResource(R.string.licences_licence),
        components = LICENCE_COMPONENTS,
        texts =
            LICENCE_RAW.map { (id, raw) ->
                LicenceText(
                    id,
                    stringResource(
                        when (id) {
                            "gpl" -> R.string.licences_full_gpl
                            "gpl2" -> R.string.licences_full_gpl2
                            "apache" -> R.string.licences_full_apache
                            "mit" -> R.string.licences_full_mit
                            "ofl" -> R.string.licences_full_ofl
                            else -> R.string.licences_full_ccbysa
                        },
                    ),
                    raw,
                )
            },
    )

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}
