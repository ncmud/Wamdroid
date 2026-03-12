package org.ncmud.mudwammer.ui

import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * Creates a [ComposeView] and sets it as the dialog's content view.
 *
 * The caller must extend [ComponentDialog] (not plain [android.app.Dialog]) so that
 * `ViewTreeLifecycleOwner` is set on the decor view—otherwise Compose will crash.
 */
fun ComponentDialog.setComposeContent(content: @Composable () -> Unit) {
    val composeView = ComposeView(context).apply { setContent(content) }
    setContentView(
        composeView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
}
