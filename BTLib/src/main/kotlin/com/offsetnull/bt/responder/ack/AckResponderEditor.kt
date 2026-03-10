package com.offsetnull.bt.responder.ack

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
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.ui.EditorDialogScaffold

class AckResponderEditor(
    context: Context,
    input: AckResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : Dialog(context) {

    private val original: AckResponder? = input?.copy()
    private val responder: AckResponder = input?.copy() ?: AckResponder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val composeView = ComposeView(context).apply {
            setContent {
                AckEditorContent(
                    initialAckWith = responder.ackWith ?: "",
                    onDone = { ackWith -> doExit(ackWith) },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun doExit(ackWith: String) {
        responder.ackWith = ackWith
        if (original != null) {
            finishWith.editTriggerResponder(responder, original)
        } else {
            finishWith.newTriggerResponder(responder)
        }
        dismiss()
    }
}

@Composable
private fun AckEditorContent(
    initialAckWith: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit
) {
    var ackWith by remember { mutableStateOf(initialAckWith) }

    EditorDialogScaffold(
        title = "Ack With Responder",
        onSave = { onDone(ackWith) },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = ackWith,
            onValueChange = { ackWith = it },
            label = { Text("Send this command") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
