package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
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
        HtmlText(text = stringResource(R.string.lemuroid_help_content))

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = stringResource(R.string.about_privacy),
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = stringResource(R.string.about_built_on),
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = stringResource(R.string.about_licence),
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(14.dp))
        Llama()

        Spacer(Modifier.height(18.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.close), style = MaterialTheme.typography.bodySmall) }
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 *
 * The Kompakt may have nothing registered for a web address, so the press is allowed to fail
 * and says so out loud rather than dying quietly.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
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
