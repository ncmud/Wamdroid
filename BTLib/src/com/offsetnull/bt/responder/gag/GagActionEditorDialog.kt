package com.offsetnull.bt.responder.gag

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponder
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.ui.EditorDialogScaffold

class GagActionEditorDialog(
    context: Context,
    private val original: TriggerResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val gagOriginal = original as? GagAction
        val initialOutput = gagOriginal?.isGagOutput ?: true
        val initialLog = gagOriginal?.isGagLog ?: true
        val initialRetarget = gagOriginal?.retarget ?: ""

        val composeView = ComposeView(context).apply {
            setContent {
                GagEditorContent(
                    initialGagOutput = initialOutput,
                    initialGagLog = initialLog,
                    initialRetarget = initialRetarget,
                    onDone = { gagOutput, gagLog, retarget ->
                        doExit(gagOutput, gagLog, retarget)
                    },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun doExit(gagOutput: Boolean, gagLog: Boolean, retarget: String) {
        val action = GagAction().apply {
            isGagOutput = gagOutput
            isGagLog = gagLog
            this.retarget = retarget
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
private fun GagEditorContent(
    initialGagOutput: Boolean,
    initialGagLog: Boolean,
    initialRetarget: String,
    onDone: (Boolean, Boolean, String) -> Unit,
    onCancel: () -> Unit
) {
    var gagOutput by remember { mutableStateOf(initialGagOutput) }
    var gagLog by remember { mutableStateOf(initialGagLog) }
    var retarget by remember { mutableStateOf(initialRetarget) }

    EditorDialogScaffold(
        title = "Gag Responder",
        onSave = { onDone(gagOutput, gagLog, retarget) },
        onCancel = onCancel
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = gagOutput, onCheckedChange = { gagOutput = it })
            Text("Gag from output")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = gagLog, onCheckedChange = { gagLog = it })
            Text("Gag from log")
        }
        OutlinedTextField(
            value = retarget,
            onValueChange = { retarget = it },
            label = { Text("Retarget to window") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
