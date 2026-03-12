package org.ncmud.mudwammer.responder.replace

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
import org.ncmud.mudwammer.R
import org.ncmud.mudwammer.responder.TriggerResponder
import org.ncmud.mudwammer.responder.TriggerResponderEditorDoneListener
import org.ncmud.mudwammer.ui.EditorDialogScaffold
import org.ncmud.mudwammer.ui.setComposeContent

class ReplaceActionEditorDialog(
    context: Context,
    private val original: TriggerResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : ComponentDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val initialWith = (original as? ReplaceResponder)?.with ?: ""

        setComposeContent {
            ReplaceEditorContent(
                initialWith = initialWith,
                onDone = { replaceWith -> doExit(replaceWith) },
                onCancel = { dismiss() }
            )
        }
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
