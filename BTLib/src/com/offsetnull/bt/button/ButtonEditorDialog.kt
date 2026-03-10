package com.offsetnull.bt.button

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offsetnull.bt.R

class ButtonEditorDialog : Dialog, ColorPickerDialog.OnColorChangedListener,
    DialogInterface.OnCancelListener {

    enum class COLOR_FIELDS {
        COLOR_MAIN, COLOR_SELECTED, COLOR_FLIPPED, COLOR_LABEL, COLOR_FLIPLABEL
    }

    private companion object {
        const val EXIT_CANCEL = 0
        const val EXIT_DONE = 1
        const val EXIT_DELETE = 2
    }

    @JvmField var modCmd: String? = null
    @JvmField var modLbl: String? = null
    @JvmField var exitState = EXIT_CANCEL

    private var deleter: Handler? = null
    private var theButton: SlickButton? = null

    private var onColorChanged: ((Int) -> Unit)? = null

    constructor(context: Context, useme: SlickButton, callback: Handler) : super(context) {
        theButton = useme
        deleter = callback
        setOnCancelListener(this)
    }

    constructor(
        context: Context, themeid: Int, useme: SlickButton, callback: Handler
    ) : super(context, themeid) {
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
                    onPickColor = { _, currentColor, callback ->
                        onColorChanged = callback
                        ColorPickerDialog(context, this@ButtonEditorDialog, currentColor).show()
                    },
                    onDone = { result -> applyResult(button, data, result) },
                    onDelete = { exitState = EXIT_DELETE; dismiss() },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun applyResult(button: SlickButton, data: SlickButtonData, result: ButtonEditResult) {
        button.setLabel(result.label)
        button.text = result.command
        button.setFlipCommand(result.flipCommand)
        data.flipLabel = result.flipLabel
        data.primaryColor = result.normalColor
        data.selectedColor = result.focusColor
        data.flipColor = result.flipColor
        data.labelColor = result.labelColor
        data.flipLabelColor = result.flipLabelColor
        data.labelSize = result.labelSize
        data.x = result.x
        data.y = result.y
        data.width = result.width
        data.height = result.height
        data.targetSet = result.targetSet
        button.moveMethod = result.moveMethod

        button.dialog_launched = false
        button.iHaveChanged(button.orig_data)
        button.invalidate()
        exitState = EXIT_DONE
        dismiss()
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

private data class ButtonEditResult(
    val label: String, val command: String,
    val flipLabel: String, val flipCommand: String,
    val moveMethod: Int,
    val normalColor: Int, val focusColor: Int, val flipColor: Int,
    val labelColor: Int, val flipLabelColor: Int,
    val labelSize: Int, val x: Int, val y: Int, val width: Int, val height: Int,
    val targetSet: String
)

@Composable
private fun ButtonEditorContent(
    data: SlickButtonData,
    moveMethod: Int,
    onPickColor: (ButtonEditorDialog.COLOR_FIELDS, Int, (Int) -> Unit) -> Unit,
    onDone: (ButtonEditResult) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var label by remember { mutableStateOf(data.label ?: "") }
    var command by remember { mutableStateOf(data.text ?: "") }
    var flipLabel by remember { mutableStateOf(data.flipLabel ?: "") }
    var flipCommand by remember { mutableStateOf(data.flipCommand ?: "") }

    var selectedMove by remember { mutableIntStateOf(moveMethod) }

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

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        EditorTabBar(selectedTab) { selectedTab = it }
        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> ClickTab(label, { label = it }, command, { command = it })
            1 -> FlipTab(flipLabel, { flipLabel = it }, flipCommand, { flipCommand = it })
            2 -> AdvancedTab(
                selectedMove, { selectedMove = it }, targetSet, { targetSet = it },
                normalColor, focusColor, flipColor, labelColor, flipLabelColor,
                onPickColor,
                { normalColor = it }, { focusColor = it }, { flipColor = it },
                { labelColor = it }, { flipLabelColor = it },
                labelSizeText, { labelSizeText = it },
                xText, { xText = it }, yText, { yText = it },
                widthText, { widthText = it }, heightText, { heightText = it }
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        EditorActionButtons(onDelete, onCancel) {
            errorMessage = validateNumericFields(xText, yText, widthText, heightText, labelSizeText)
            if (errorMessage == null) {
                onDone(ButtonEditResult(
                    label, command, flipLabel, flipCommand, selectedMove,
                    normalColor, focusColor, flipColor, labelColor, flipLabelColor,
                    labelSizeText.toInt(), xText.toInt(), yText.toInt(),
                    widthText.toInt(), heightText.toInt(), targetSet
                ))
            }
        }
    }
}

private fun validateNumericFields(vararg fieldValues: String): String? {
    val names = listOf("X Coordinate", "Y Coordinate", "Width", "Height", "Label Size")
    for ((i, value) in fieldValues.withIndex()) {
        if (value.isBlank()) return "${names[i]} must not be blank."
        val num = value.toIntOrNull()
        if (num == null || num == 0) return "${names[i]} must be a non-zero number."
    }
    return null
}

@Composable
private fun EditorTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(selectedTabIndex = selectedTab) {
        Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }) {
            Text("Click", modifier = Modifier.padding(12.dp))
        }
        Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }) {
            Text("Flip", modifier = Modifier.padding(12.dp))
        }
        Tab(selected = selectedTab == 2, onClick = { onTabSelected(2) }) {
            Text("Advanced", modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun ClickTab(
    label: String, onLabelChange: (String) -> Unit,
    command: String, onCommandChange: (String) -> Unit
) {
    OutlinedTextField(
        value = label, onValueChange = onLabelChange,
        label = { Text("Label") }, modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = command, onValueChange = onCommandChange,
        label = { Text("Command") }, modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FlipTab(
    flipLabel: String, onFlipLabelChange: (String) -> Unit,
    flipCommand: String, onFlipCommandChange: (String) -> Unit
) {
    OutlinedTextField(
        value = flipLabel, onValueChange = onFlipLabelChange,
        label = { Text("Flip Label") }, modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = flipCommand, onValueChange = onFlipCommandChange,
        label = { Text("Flip Command") }, modifier = Modifier.fillMaxWidth()
    )
}

@Suppress("LongParameterList")
@Composable
private fun AdvancedTab(
    selectedMove: Int, onMoveChange: (Int) -> Unit,
    targetSet: String, onTargetSetChange: (String) -> Unit,
    normalColor: Int, focusColor: Int, flipColor: Int, labelColor: Int, flipLabelColor: Int,
    onPickColor: (ButtonEditorDialog.COLOR_FIELDS, Int, (Int) -> Unit) -> Unit,
    onNormalColor: (Int) -> Unit, onFocusColor: (Int) -> Unit, onFlipColor: (Int) -> Unit,
    onLabelColor: (Int) -> Unit, onFlipLabelColor: (Int) -> Unit,
    labelSizeText: String, onLabelSizeChange: (String) -> Unit,
    xText: String, onXChange: (String) -> Unit,
    yText: String, onYChange: (String) -> Unit,
    widthText: String, onWidthChange: (String) -> Unit,
    heightText: String, onHeightChange: (String) -> Unit
) {
    MovementSelector(selectedMove, onMoveChange)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = targetSet, onValueChange = onTargetSetChange,
        label = { Text("Target Set") }, modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))
    Text("Colors", style = MaterialTheme.typography.labelLarge)
    ColorButton("Normal", normalColor) {
        onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_MAIN, normalColor, onNormalColor)
    }
    ColorButton("Focus", focusColor) {
        onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_SELECTED, focusColor, onFocusColor)
    }
    ColorButton("Flip", flipColor) {
        onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_FLIPPED, flipColor, onFlipColor)
    }
    ColorButton("Label", labelColor) {
        onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_LABEL, labelColor, onLabelColor)
    }
    ColorButton("Flip Label", flipLabelColor) {
        onPickColor(ButtonEditorDialog.COLOR_FIELDS.COLOR_FLIPLABEL, flipLabelColor, onFlipLabelColor)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text("Position & Size", style = MaterialTheme.typography.labelLarge)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = labelSizeText, onValueChange = onLabelSizeChange,
            label = { Text("Label Size") }, modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = xText, onValueChange = onXChange,
            label = { Text("X") }, modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = yText, onValueChange = onYChange,
            label = { Text("Y") }, modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = widthText, onValueChange = onWidthChange,
            label = { Text("Width") }, modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = heightText, onValueChange = onHeightChange,
            label = { Text("Height") }, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MovementSelector(selectedMove: Int, onMoveChange: (Int) -> Unit) {
    Text("Movement", style = MaterialTheme.typography.labelLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedMove == SlickButtonData.MOVE_FREE,
            onClick = { onMoveChange(SlickButtonData.MOVE_FREE) })
        Text("Free", modifier = Modifier.clickable { onMoveChange(SlickButtonData.MOVE_FREE) })
        Spacer(modifier = Modifier.width(8.dp))
        RadioButton(selected = selectedMove == SlickButtonData.MOVE_NUDGE,
            onClick = { onMoveChange(SlickButtonData.MOVE_NUDGE) })
        Text("Nudge", modifier = Modifier.clickable { onMoveChange(SlickButtonData.MOVE_NUDGE) })
        Spacer(modifier = Modifier.width(8.dp))
        RadioButton(selected = selectedMove == SlickButtonData.MOVE_FREEZE,
            onClick = { onMoveChange(SlickButtonData.MOVE_FREEZE) })
        Text("Freeze", modifier = Modifier.clickable { onMoveChange(SlickButtonData.MOVE_FREEZE) })
    }
}

@Composable
private fun EditorActionButtons(onDelete: () -> Unit, onCancel: () -> Unit, onDone: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )) { Text("Delete") }
        Row {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onDone) { Text("Done") }
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
