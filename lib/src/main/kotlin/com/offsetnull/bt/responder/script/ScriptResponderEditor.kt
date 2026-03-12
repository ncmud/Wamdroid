package com.offsetnull.bt.responder.script

import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.ui.EditorDialogScaffold
import com.offsetnull.bt.ui.setComposeContent

class ScriptResponderEditor(
    context: Context,
    input: ScriptResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : ComponentDialog(context) {

    private val original: ScriptResponder? = input?.copy()
    private val responder: ScriptResponder = input?.copy() ?: ScriptResponder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        setComposeContent {
            ScriptEditorContent(
                initialFunction = responder.function ?: "",
                onDone = { function -> doExit(function) },
                onCancel = { dismiss() }
            )
        }
    }

    private fun doExit(function: String) {
        responder.function = function
        if (original != null) {
            finishWith.editTriggerResponder(responder, original)
        } else {
            finishWith.newTriggerResponder(responder)
        }
        dismiss()
    }
}

@Composable
private fun ScriptEditorContent(
    initialFunction: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit
) {
    var function by remember { mutableStateOf(initialFunction) }

    EditorDialogScaffold(
        title = "Script Responder",
        onSave = { onDone(function) },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = function,
            onValueChange = { function = it },
            label = { Text("Execute Script Function") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
