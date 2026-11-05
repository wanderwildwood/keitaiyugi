package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.R

/**
 * This app's About: only the words and the address. The dialog, its drawers, its one Close button,
 * the Licenses drawer, the links and the llama are the shared HouseAbout.kt, the same in every app.
 *
 * Most of this app is other people's work, and the licence question is a real one rather than
 * a formality: the Game Boy core is GPL-2.0 with no "or later" clause, which is worth a line
 * to anyone who might redistribute a build (it is in the Terms). The help's questions follow
 * the short description, each a drawer.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val what = stringResource(R.string.about_what)
    val help = stringResource(R.string.lemuroid_help_content)
    val builtOn = stringResource(R.string.about_built_on)
    val privacyHead = stringResource(R.string.about_privacy_heading)
    val privacy = stringResource(R.string.about_privacy)
    val privacyMore =
        listOf(
            AboutSection(stringResource(R.string.privacy_who_title), stringResource(R.string.privacy_who)),
            AboutSection(stringResource(R.string.privacy_contact_title), stringResource(R.string.privacy_contact)),
            AboutSection(stringResource(R.string.privacy_rights_title), stringResource(R.string.privacy_rights)),
        )
    val termsHead = stringResource(R.string.about_terms_heading)
    val terms = stringResource(R.string.about_terms)
    val title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME)
    val data =
        remember(what, help, builtOn, privacy, privacyMore, terms, title) {
            AboutData(
                title = title,
                sections =
                    listOf(AboutSection(null, what), AboutSection(null, builtOn)) + helpSections(help) +
                        listOf(AboutSection(privacyHead, privacy)) + privacyMore +
                        listOf(AboutSection(termsHead, terms)),
                sourceUrl = SOURCE_URL,
            )
        }
    val labels =
        AboutLabels(
            close = stringResource(R.string.close),
            licences = stringResource(R.string.licences_title),
            feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
            noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
            writeTo = { address -> context.getString(R.string.about_write_to, address) },
            noMailApp = { address -> context.getString(R.string.about_no_mail_app, address) },
            openDrawer = { heading -> context.getString(R.string.about_open_drawer, heading) },
            closeDrawer = { heading -> context.getString(R.string.about_close_drawer, heading) },
            contact = stringResource(R.string.about_contact_head),
            links = stringResource(R.string.about_links_head),
        )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss)
}
