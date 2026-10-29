package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One piece of other people's work that ships inside the app, and the licence it comes under. */
data class Library(
    val name: String,
    val by: String,
    val licence: String,
    val link: String,
)

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/keitaiyugi"

/**
 * Everything the APK carries that this project did not write, as the Gradle build pulls it in
 * (the app and the modules under it) plus the Game Boy core it loads at runtime. Licences are
 * the ones each project declares in its own published metadata.
 */
val LIBRARIES: List<Library> =
    listOf(
        Library(
            "Lemuroid, the app this is forked from",
            "Filippo Scognamiglio",
            "GPL-3.0",
            "https://github.com/Swordfish90/Lemuroid",
        ),
        Library("LibretroDroid", "Filippo Scognamiglio", "GPL-3.0", "https://github.com/Swordfish90/LibretroDroid"),
        Library(
            "Gambatte core (Game Boy), loaded at run time",
            "Sindre Aamås and libretro contributors",
            "GPL-2.0-only",
            "https://github.com/libretro/gambatte-libretro",
        ),
        Library(
            "libretro database (game titles and developers)",
            "libretro contributors",
            "CC-BY-SA-4.0",
            "https://github.com/libretro/libretro-database",
        ),
        Library("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        Library("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        Library(
            "AndroidX: Core, AppCompat, Activity, Lifecycle, Navigation, Room, Paging, Preference, WorkManager, " +
                "Startup, DocumentFile, ConstraintLayout, RecyclerView",
            "Google",
            "Apache-2.0",
            "https://developer.android.com/jetpack",
        ),
        Library(
            "Jetpack Compose (UI, Foundation, Material 3) and Accompanist",
            "Google",
            "Apache-2.0",
            "https://developer.android.com/jetpack/compose",
        ),
        Library(
            "Material Components for Android",
            "Google",
            "Apache-2.0",
            "https://github.com/material-components/material-components-android",
        ),
        Library("Guava", "Google", "Apache-2.0", "https://github.com/google/guava"),
        Library("Dagger", "Google", "Apache-2.0", "https://github.com/google/dagger"),
        Library(
            "Kotlin standard library, coroutines, serialization, immutable collections",
            "JetBrains",
            "Apache-2.0",
            "https://kotlinlang.org",
        ),
        Library("OkHttp and Okio", "Square", "Apache-2.0", "https://square.github.io/okhttp/"),
        Library("Retrofit", "Square", "Apache-2.0", "https://square.github.io/retrofit/"),
        Library("Coil", "Coil contributors", "Apache-2.0", "https://coil-kt.github.io/coil/"),
        Library("Timber", "Jake Wharton", "Apache-2.0", "https://github.com/JakeWharton/timber"),
        Library("PadKit", "Filippo Scognamiglio", "Apache-2.0", "https://github.com/Swordfish90/PadKit"),
        Library("Compose Settings", "Alorma", "MIT", "https://github.com/alorma/compose-settings"),
        Library(
            "Flow Preferences",
            "Fred Porciuncula",
            "Apache-2.0",
            "https://github.com/fredporciuncula/flow-preferences",
        ),
        Library("Harmony", "Frybits", "Apache-2.0", "https://github.com/frybits/harmony"),
        Library(
            "Compose Material 3 HTML text",
            "Charlex",
            "Apache-2.0",
            "https://github.com/charlex/compose-material-html-text",
        ),
        Library("Material Symbols icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
    )

/**
 * A licence text as the paragraphs a reader turns pages by. The texts are wrapped at about
 * seventy columns with a blank line between paragraphs; each paragraph is joined into one
 * row, and no word is dropped or repeated by it.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }

/** The raw resources holding each licence's full text, with the heading each is shown under. */
val LICENCE_TEXTS: List<Pair<Int, Int>> =
    listOf(
        R.string.licences_gpl3 to R.raw.gpl_3_0,
        R.string.licences_gpl2 to R.raw.gpl_2_0,
        R.string.licences_apache to R.raw.apache_2_0,
        R.string.licences_mit to R.raw.mit,
        R.string.licences_ofl to R.raw.ofl_1_1,
        R.string.licences_ccbysa to R.raw.cc_by_sa_4_0,
    )

/** The paragraphs of every licence in [LICENCE_TEXTS], in the same order, read off the main thread. */
@Composable
fun rememberLicenceTexts(): List<List<String>> {
    val context = LocalContext.current
    return LICENCE_TEXTS.map { (_, raw) -> produceTexts(context, raw).value }
}

/**
 * The Licences page as rows for a paged list: where the source is, the licence this app is
 * under, what it is built with, and the full text of each licence once. Each paragraph is a row
 * so a page turns on whole paragraphs, like every list here.
 */
fun LazyListScope.licencesItems(texts: List<List<String>>) {
    item { Heading(stringResource(R.string.licences_source_heading)) }
    item { Link(stringResource(R.string.licences_source, SOURCE_URL.removePrefix("https://")), SOURCE_URL) }
    item { Heading(stringResource(R.string.licences_licence_heading)) }
    item { Paragraph(stringResource(R.string.licences_licence)) }
    item { Heading(stringResource(R.string.licences_built_heading)) }
    LIBRARIES.forEach { library ->
        item { Link("${library.name}, ${library.by}: ${library.licence}", library.link) }
    }
    LICENCE_TEXTS.forEachIndexed { index, (heading, _) ->
        item { Heading(stringResource(heading)) }
        texts.getOrNull(index).orEmpty().forEach { item { Paragraph(it) } }
    }
}

@Composable
private fun produceTexts(
    context: Context,
    @RawRes resource: Int,
): State<List<String>> =
    produceState(initialValue = emptyList<String>(), resource) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.resources.openRawResource(resource).bufferedReader().use { it.readText() }
                    licenceParagraphs(text)
                }.getOrDefault(emptyList())
            }
    }

@Composable
private fun Heading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Paragraph(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
}

/** A line that opens an address in a browser; a phone with none says so rather than doing nothing. */
@Composable
private fun Link(
    text: String,
    address: String,
) {
    val context = LocalContext.current
    val none = stringResource(R.string.licences_no_browser)
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textDecoration = TextDecoration.Underline,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address))) }
                        .onFailure { Toast.makeText(context, none, Toast.LENGTH_SHORT).show() }
                }
                .wrapContentHeight(Alignment.CenterVertically),
    )
}
