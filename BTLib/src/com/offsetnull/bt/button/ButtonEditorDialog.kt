package com.offsetnull.bt.button

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offsetnull.bt.R
import com.offsetnull.bt.window.MainWindow

class ButtonEditorDialog : Dialog, ColorPickerDialog.OnColorChangedListener, DialogInterface.OnCancelListener {

    enum class COLOR_FIELDS {
        COLOR_MAIN, COLOR_SELECTED, COLOR_FLIPPED, COLOR_LABEL, COLOR_FLIPLABEL
    }

    private val EXIT_CANCEL = 0
    private val EXIT_DONE = 1
    private val EXIT_DELETE = 2

    @JvmField var mod_cmd: String? = null
    @JvmField var mod_lbl: String? = null
    @JvmField var EXIT_STATE = EXIT_CANCEL

    private var deleter: Handler? = null
    private var theButton: SlickButton? = null

    private var activeColorField = COLOR_FIELDS.COLOR_MAIN
    private var onColorChanged: ((Int) -> Unit)? = null

    constructor(context: Context, useme: SlickButton, callback: Handler) : super(context) {
        theButton = useme
        deleter = callback
        setOnCancelListener(this)
    }

    constructor(context: Context, themeid: Int, useme: SlickButton, callback: Handler) : super(context, themeid) {
        theButton = useme
        deleter = callback
        setOnCancelListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val button = theButton ?: return
        val data = button.data

        val composeView = ComposeView(context).apply {
            setContent {
                ButtonEditorContent(
                    data = data,
                    moveMethod = button.moveMethod,
                    onPickColor = { field, currentColor, callback ->
                        activeColorField = field
                        onColorChanged = callback
                        ColorPickerDialog(context, this@ButtonEditorDialog, currentColor).show()
                    },
                    onDone = { label, command, flipLabel, flipCommand, moveMethod,
                               normalColor, focusColor, flipColor, labelColor, flipLabelColor,
                               labelSize, x, y, width, height, targetSet ->
                        button.setLabel(label)
                        button.text = command
                        button.setFlipCommand(flipCommand)
                        data.flipLabel = flipLabel
                        data.primaryColor = normalColor
                        data.selectedColor = focusColor
                        data.flipColor = flipColor
                        data.labelColor = labelColor
                        data.flipLabelColor = flipLabelColor
                        data.labelSize = labelSize
                        data.x = x
                        data.y = y
                        data.width = width
                        data.height = height
                        data.targetSet = targetSet
                        button.moveMethod = moveMethod

                        button.dialog_launched = false
                        button.iHaveChanged(button.orig_data)
                        button.invalidate()
                        EXIT_STATE = EXIT_DONE
                        dismiss()
                    },
                    onDelete = {
                        EXIT_STATE = EXIT_DELETE
                        dismiss()
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

    override fun colorChanged(color: Int) {
        onColorChanged?.invoke(color)
    }

    override fun onCancel(dialog: DialogInterface) {
        val button = theButton ?: return
        button.moving = false
        button.button_down = false
        button.doing_flip = false
        button.hasfocus = false
        button.dialog_launched = false
        button.invalidate()
    }
}

@Composable
private fun ButtonEditorContent(
    data: SlickButtonData,
    moveMethod: Int,
    onPickColor: (ButtonEditorDialog.COLOR_FIELDS, Int, (Int) -> Unit) -> Unit,
    onDone: (label: String, command: String, flipLabel: String, flipCommand: String,
             moveMethod: Int, normalColor: Int, focusColor: Int, flipColor: Int,
             labelColor: Int, flipLabelColor: Int, labelSize: Int,
             x: Int, y: Int, width: Int, height: Int, targetSet: String) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var label by remember { mutableStateOf(data.label ?: "") }
    var command by remember { mutableStateOf(data.text ?: "") }
    var flipLabel by remember { mutableStateOf(data.flipLabel ?: "") }
    var flipCommand by remember { mutableStateOf(data.flipCommand ?: "") }

    var moveFree by remember { mutableStateOf(moveMethod == SlickButtonData.MOVE_FREE) }
    var moveNudge by remember { mutableStateOf(moveMethod == SlickButtonData.MOVE_NUDGE) }
    var moveFreeze by remember { mutableStateOf(moveMethod == SlickButtonData.MOVE_FREEZE) }

    var normalColor by remember { mutableIntStateOf(data.primaryColor) }
    var focusColor by remember { mutableIntStateOf(data.selectedColor) }
    var flipColor by remember { mutableIntStateOf(data.flipColor) }
    var labelColor by remember { mutableIntStateOf(data.labelColor) }
    var flipLabelColor by remember { mutableIntStateOf(data.flipLabelColor) }

    var labelSizeText by remember { mutableStateOf(data.labelSize.toString()) }
    var xText by remember { mutableStateOf(data.x.toString()) }
    var yText by remember { mutableStateOf(data.y.toString()) }
    var widthText by remember { mutableStateOf(data.width.toString()) }
    var heightText by remember { mutableStateOf(data.height.toString()) }
    var targetSet by remember { mutableStateOf(data.targetSet ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        val fields = listOf(
            "X Coordinate" to xText,
            "Y Coordinate" to yText,
            "Width" to widthText,
            "Height" to heightText,
            "Label Size" to labelSizeText
        )
        for ((name, value) in fields) {
            if (value.isBlank()) {
                errorMessage = "$name must not be blank."
                return false
            }
            val num = value.toIntOrNull()
            if (num == null || num == 0) {
                errorMessage = "$name must be a non-zero number."
                return false
            }
        }
        errorMessage = null
        return true
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Click", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Flip", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("Advanced", modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Label") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    label = { Text("Command") }, modifier = Modifier.fillMaxWidth()
                )
            }
            1 -> {
                OutlinedTextField(
                    value = flipLabel, onValueChange = { flipLabel = it },
                    label = { Text("Flip Label") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = flipCommand, onValueChange = { flipCommand = it },
                    label = { Text("Flip Command") }, modifier = Modifier.fillMaxWidth()
                )
            }
            2 -> {
                // Movement method
                Text("Movement", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = moveFree, onClick = {
                        moveFree = true; moveNudge = false; moveFreeze = false
                    })
                    Text("Free", modifier = Modifier.clickable {
                        moveFree = true; moveNudge = false; moveFreeze = false
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = moveNudge, onClick = {
                        moveFree = false; moveNudge = true; moveFreeze = false
                    })
                    Text("Nudge", modifier = Modifier.clickable {
                        moveFree = false; moveNudge = true; moveFreeze = false
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = moveFreeze, onClick = {
                        moveFree = false; moveNudge = false; moveFreeze = true
                    })
                    Text("Freeze", modifier = Modifier.clickable {
                        moveFree = false; moveNudge = false; moveFreeze = true
                    })
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetSet, onValueChange = { targetSet = it },
                    label = { Text("Target Set") }, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Colors", style = MaterialTheme.typography.labelLarge)

                ColorButton("Normal", normalColor) {
                    onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_MAIN, normalColor) { normalColor = it }
                }
                ColorButton("Focus", focusColor) {
                    onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_SELECTED, focusColor) { focusColor = it }
                }
                ColorButton("Flip", flipColor) {
                    onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_FLIPPED, flipColor) { flipColor = it }
                }
                ColorButton("Label", labelColor) {
                    onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_LABEL, labelColor) { labelColor = it }
                }
                ColorButton("Flip Label", flipLabelColor) {
                    onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_FLIPLABEL, flipLabelColor) { flipLabelColor = it }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Position & Size", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = labelSizeText, onValueChange = { labelSizeText = it },
                        label = { Text("Label Size") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = xText, onValueChange = { xText = it },
                        label = { Text("X") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yText, onValueChange = { yText = it },
                        label = { Text("Y") }, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthText, onValueChange = { widthText = it },
                        label = { Text("Width") }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText, onValueChange = { heightText = it },
                        label = { Text("Height") }, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )) { Text("Delete") }
            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (validate()) {
                        val currentMoveMethod = when {
                            moveFree -> SlickButtonData.MOVE_FREE
                            moveNudge -> SlickButtonData.MOVE_NUDGE
                            moveFreeze -> SlickButtonData.MOVE_FREEZE
                            else -> SlickButtonData.MOVE_FREE
                        }
                        onDone(
                            label, command, flipLabel, flipCommand, currentMoveMethod,
                            normalColor, focusColor, flipColor, labelColor, flipLabelColor,
                            labelSizeText.toInt(), xText.toInt(), yText.toInt(),
                            widthText.toInt(), heightText.toInt(), targetSet
                        )
                    }
                }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ColorButton(label: String, color: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(label, modifier = Modifier.width(80.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(color)),
            modifier = Modifier.height(32.dp).width(64.dp),
            contentPadding = PaddingValues(0.dp)
        ) {}
    }
}
