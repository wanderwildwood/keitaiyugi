package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.R
import de.charlex.compose.material3.HtmlText

/**
 * What this is, what it does with what is on the phone, and whose the parts are.
 *
 * The `i` in the top bar used to open the help text and nothing else — no version, no licence,
 * no source, and no way out of it but a tap on the scrim. It is an About now, in the order
 * every other app of this shop uses, and it has a Close button.
 *
 * Most of this app is other people's work, and the licence question is a real one rather than
 * a formality: the Game Boy core is GPL-2.0 with no "or later" clause, which is worth a line
 * to anyone who might redistribute a build.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(14.dp))
        // Paged, not scrolled, and paged one paragraph at a time. The help runs taller than the
        // panel at 480x800, and it used to run off the bottom with Close underneath it. MMD's
        // list steps four items a swipe; four of these paragraphs are more than a screen, so a
        // swipe would have stepped straight past text nobody had read.
        val help = stringResource(R.string.lemuroid_help_content)
        val questions = remember(help) {
            help.split(PARAGRAPH_BREAK).map { it.trim() }.filter { it.isNotEmpty() }
        }
        LazyColumnMMD(
            modifier = Modifier.heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            scrollStep = 1,
        ) {
            items(questions) { question -> HtmlText(text = question) }
            item {
                TextMMD(
                    text = stringResource(R.string.about_privacy),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            item {
                TextMMD(
                    text = stringResource(R.string.about_built_on),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            item {
                TextMMD(
                    text = stringResource(R.string.about_licence),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            item { Llama() }
        }

        Spacer(Modifier.height(18.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.close), style = MaterialTheme.typography.bodySmall) }
    }
}

/** The blank line between two questions of the help, in every translation of it. */
private val PARAGRAPH_BREAK = Regex("""<br\s*/?>\s*<br\s*/?>""")

/**
 * The site's address, then a llama which opens the page a donation goes to, on one line.
 *
 * Only the llama and its words open the page; the address beside it is there to be read.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(text = "wanderthe.dev", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clickable {
                        // Straight to the checkout. The Donate button on the site only leads
                        // here anyway, so the page in between is a press the reader does not need.
                        // The short square.link form, not the long checkout.square.site address it
                        // redirects to -- the short one is what the site itself links to, so a
                        // regenerated checkout follows it and a published app does not break.
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://square.link/u/AGu8oT10")),
                            )
                        }.onFailure {
                            Toast.makeText(
                                context,
                                context.getString(R.string.about_no_browser),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    .padding(vertical = 4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.labelSmall)
        }
    }
}
