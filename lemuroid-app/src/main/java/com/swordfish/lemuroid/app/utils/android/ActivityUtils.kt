package com.swordfish.lemuroid.app.utils.android

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.DarkMode

// TODO COMPOSE... How do they look in the post compose world?
fun Activity.displayErrorDialog(
    messageId: Int,
    actionLabelId: Int,
    action: () -> Unit,
) {
    displayErrorDialog(resources.getString(messageId), resources.getString(actionLabelId), action)
}

fun Activity.displayErrorDialog(
    message: String,
    actionLabel: String,
    action: () -> Unit,
) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton(actionLabel) { _, _ -> action() }
        .setCancelable(false)
        .show()
        .also { dialog -> dialog.window?.let { DarkMode.window(it.decorView) } }
}
