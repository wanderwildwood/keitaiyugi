package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.DialogButtons
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.EInkDialog
import com.swordfish.lemuroid.app.shared.box.BoxSyncRunner
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.box.BoxLink
import java.text.DateFormat
import java.util.Date
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The game box: a Batocera box on the home network whose Game Boy games, saves and cheats this
 * app shares. One row for its address and one to sync by hand. Saves also go by themselves,
 * before a game opens and after it closes.
 */
@Composable
fun BoxSettings(
    link: BoxLink,
    runner: BoxSyncRunner,
) {
    var address by remember { mutableStateOf(link.address()) }
    var editing by remember { mutableStateOf(false) }
    val status by runner.status.collectAsState()
    val working =
        status !is BoxSyncRunner.Status.Idle && status !is BoxSyncRunner.Status.Done &&
            status !is BoxSyncRunner.Status.Unreachable

    LemuroidCardSettingsGroup(
        title = { TextMMD(text = stringResource(id = R.string.settings_category_box)) },
    ) {
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_box_address)) },
            subtitle = {
                TextMMD(text = address.ifEmpty { stringResource(id = R.string.settings_description_box_address) })
            },
            enabled = !working,
            onClick = { editing = true },
        )
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_box_sync)) },
            subtitle = { TextMMD(text = statusText(status, runner.lastSynced())) },
            enabled = address.isNotEmpty() && !working,
            onClick = { runner.syncNow(byHand = true) },
        )
    }

    if (editing) {
        var draft by remember { mutableStateOf(TextFieldValue(address, TextRange(address.length))) }
        val field = remember { FocusRequester() }
        val save = {
            link.setAddress(draft.text)
            address = link.address()
            editing = false
        }
        EInkDialog(onDismiss = { editing = false }) {
            // The field is what the dialog is for: it takes the focus as it opens and the keyboard
            // comes up. Asked from inside the dialog, a frame after it is drawn: the field lives in
            // the dialog's own window, which a request from the screen beneath does not reach.
            val keyboardNow = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) {
                withFrameNanos { }
                runCatching { field.requestFocus() }
                keyboardNow?.show()
            }
            TextMMD(text = stringResource(id = R.string.settings_box_address_prompt))
            Spacer(Modifier.height(12.dp))
            TextFieldMMD(
                value = draft,
                onValueChange = { draft = it.copy(text = it.text.trim()) },
                modifier = Modifier.fillMaxWidth().focusRequester(field),
                singleLine = true,
                // Done on the keyboard is OK.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
            Spacer(Modifier.height(16.dp))
            DialogButtons(
                cancel = stringResource(id = R.string.cancel),
                confirm = stringResource(id = R.string.ok),
                onCancel = { editing = false },
                onConfirm = { save() },
            )
        }
    }
}

@Composable
private fun statusText(
    status: BoxSyncRunner.Status,
    lastSynced: Long,
): String =
    when (status) {
        BoxSyncRunner.Status.Idle ->
            if (lastSynced == 0L) {
                stringResource(id = R.string.settings_box_sync_never)
            } else {
                stringResource(
                    id = R.string.settings_box_sync_last,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastSynced)),
                )
            }
        BoxSyncRunner.Status.Connecting -> stringResource(id = R.string.settings_box_sync_connecting)
        is BoxSyncRunner.Status.Games ->
            stringResource(id = R.string.settings_box_sync_games, status.done, status.total)
        BoxSyncRunner.Status.Cheats -> stringResource(id = R.string.settings_box_sync_cheats)
        BoxSyncRunner.Status.Library -> stringResource(id = R.string.settings_box_sync_library)
        BoxSyncRunner.Status.Saves -> stringResource(id = R.string.settings_box_sync_saves)
        is BoxSyncRunner.Status.Done ->
            stringResource(id = R.string.settings_box_sync_done, status.games, status.savesIn, status.savesOut)
        BoxSyncRunner.Status.Unreachable -> stringResource(id = R.string.settings_box_sync_unreachable)
    }
