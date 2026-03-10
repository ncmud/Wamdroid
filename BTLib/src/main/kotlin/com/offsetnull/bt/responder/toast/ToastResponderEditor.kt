package com.offsetnull.bt.responder.toast

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.KeyboardType
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.ui.EditorDialogScaffold

class ToastResponderEditor(
    context: Context,
    input: ToastResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : Dialog(context) {

    private val original: ToastResponder? = input?.copy()
    private val responder: ToastResponder = input?.copy() ?: ToastResponder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val composeView = ComposeView(context).apply {
            setContent {
                ToastEditorContent(
                    initialMessage = responder.message ?: "",
                    initialDelay = responder.delay,
                    onDone = { message, delay -> doExit(message, delay) },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun doExit(message: String, delay: Int) {
        responder.message = message
        responder.delay = delay
        if (original != null) {
            finishWith.editTriggerResponder(responder, original)
        } else {
            finishWith.newTriggerResponder(responder)
        }
        dismiss()
    }
}

@Composable
private fun ToastEditorContent(
    initialMessage: String,
    initialDelay: Int,
    onDone: (String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var message by remember { mutableStateOf(initialMessage) }
    var delayText by remember { mutableStateOf(initialDelay.toString()) }

    EditorDialogScaffold(
        title = "Toast Responder",
        onSave = {
            val delay = delayText.toIntOrNull() ?: 0
            if (message.isNotBlank() && delay > 0) {
                onDone(message, delay)
            }
        },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = delayText,
            onValueChange = { delayText = it },
            label = { Text("Show for (seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
