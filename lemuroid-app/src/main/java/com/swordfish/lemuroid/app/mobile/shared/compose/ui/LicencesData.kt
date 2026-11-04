package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/keitaiyugi"

/**
 * Each distinct licence text once: family id and the raw resource. The Game Boy core is
 * GPL-2.0 only, which is not the GPL-3.0 the app is under; its page shows its own GPL-2.0 text
 * (config/licenses/gambatte-gpl2.json), and the same text is kept here under the id "gpl2"
 * (see [LICENCE_EXTRA_TEXT_IDS]). No MIT family text: every MIT library on the page carries its
 * own license file with its copyright line (config/licenses).
 */
val LICENCE_RAW: List<Pair<String, Int>> =
    listOf(
        "gpl" to R.raw.gpl_3_0,
        "gpl2" to R.raw.gpl_2_0,
        "apache" to R.raw.apache_2_0,
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
        // One row per artifact in the APK, listed by the build; config/libraries adds what Gradle
        // cannot see (the fork's upstream, the Game Boy core, the game database, fonts, icons).
        components = rememberGeneratedComponents(R.raw.aboutlibraries),
        texts =
            LICENCE_RAW.map { (id, raw) ->
                LicenceText(
                    id,
                    stringResource(
                        when (id) {
                            "gpl" -> R.string.licences_full_gpl
                            "gpl2" -> R.string.licences_full_gpl2
                            "apache" -> R.string.licences_full_apache
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
