package com.offsetnull.bt.launcher

import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentDialog
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
import androidx.compose.ui.text.input.KeyboardType
import com.offsetnull.bt.R
import com.offsetnull.bt.ui.EditorDialogScaffold
import com.offsetnull.bt.ui.setComposeContent

class NewConnectionDialog : ComponentDialog {

    private val reportTo: ReadyListener
    private val prev: MudConnection?

    constructor(context: Context, useme: ReadyListener) : super(context) {
        reportTo = useme
        prev = null
    }

    constructor(context: Context, useme: ReadyListener, old: MudConnection) : super(context) {
        reportTo = useme
        prev = old
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        setComposeContent {
            NewConnectionContent(
                initialDisplay = prev?.displayName ?: "",
                initialHost = prev?.hostName ?: "",
                initialPort = if (prev != null) prev.portString else "",
                onSave = { display, host, port -> doSave(display, host, port) },
                onCancel = { dismiss() }
            )
        }
    }

    private fun doSave(display: String, host: String, port: String) {
        if (display.isBlank() || host.isBlank() || port.isBlank()) return
        port.toIntOrNull() ?: return

        if (prev != null) {
            val m = prev.copy()
            m.displayName = display
            m.hostName = host
            m.portString = port
            reportTo.modify(prev, m)
        } else {
            val m = MudConnection()
            m.displayName = display
            m.hostName = host
            m.portString = port
            reportTo.ready(m)
        }
        dismiss()
    }
}

@Composable
private fun NewConnectionContent(
    initialDisplay: String,
    initialHost: String,
    initialPort: String,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var display by remember { mutableStateOf(initialDisplay) }
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }

    EditorDialogScaffold(
        title = "Connection Properties",
        onSave = { onSave(display, host, port) },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = { display = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
