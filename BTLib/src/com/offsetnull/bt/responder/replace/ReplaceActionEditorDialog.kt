package com.offsetnull.bt.responder.replace

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponder
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.ui.EditorDialogScaffold

class ReplaceActionEditorDialog(
    context: Context,
    private val original: TriggerResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val initialWith = (original as? ReplaceResponder)?.with ?: ""

        val composeView = ComposeView(context).apply {
            setContent {
                ReplaceEditorContent(
                    initialWith = initialWith,
                    onDone = { replaceWith -> doExit(replaceWith) },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun doExit(replaceWith: String) {
        val action = ReplaceResponder().apply {
            with = replaceWith
            if (original != null) {
                fireType = original.fireType
            }
        }
        if (original != null) {
            finishWith.editTriggerResponder(action, original)
        } else {
            finishWith.newTriggerResponder(action)
        }
        dismiss()
    }
}

@Composable
private fun ReplaceEditorContent(
    initialWith: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit
) {
    var replaceWith by remember { mutableStateOf(initialWith) }

    EditorDialogScaffold(
        title = "Replace Responder",
        onSave = { onDone(replaceWith) },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = replaceWith,
            onValueChange = { replaceWith = it },
            label = { Text("Replace triggered text with") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
