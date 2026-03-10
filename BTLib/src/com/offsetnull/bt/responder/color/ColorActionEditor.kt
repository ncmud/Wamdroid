package com.offsetnull.bt.responder.color

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponder
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.service.Colorizer

class ColorActionEditor(
    context: Context,
    private val original: TriggerResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val initialFg = (original as? ColorAction)?.color ?: ColorAction.DEFAULT_COLOR
        val initialBg = (original as? ColorAction)?.backgroundColor ?: ColorAction.DEFAULT_BACKGROUND_COLOR

        val composeView = ComposeView(context).apply {
            setContent {
                ColorEditorContent(
                    initialForeground = initialFg,
                    initialBackground = initialBg,
                    onDone = { fg, bg -> doExit(fg, bg) },
                    onCancel = { dismiss() }
                )
            }
        }
        setContentView(composeView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun doExit(foregroundColor: Int, backgroundColor: Int) {
        val action = ColorAction().apply {
            color = foregroundColor
            setBackgroundColor(backgroundColor)
        }
        if (original != null) {
            finishWith.editTriggerResponder(action, original)
        } else {
            finishWith.newTriggerResponder(action)
        }
        dismiss()
    }
}

private enum class EditMode { FOREGROUND, BACKGROUND }
private enum class ColorTab { XTERM256, GREYSCALE, SYSTEM }

@Composable
private fun ColorEditorContent(
    initialForeground: Int,
    initialBackground: Int,
    onDone: (fg: Int, bg: Int) -> Unit,
    onCancel: () -> Unit
) {
    var foregroundColor by remember { mutableIntStateOf(initialForeground) }
    var backgroundColor by remember { mutableIntStateOf(initialBackground) }
    var editMode by remember { mutableStateOf(EditMode.FOREGROUND) }
    var selectedTab by remember { mutableStateOf(ColorTab.XTERM256) }

    var redInt by remember { mutableIntStateOf(2) }
    var greenInt by remember { mutableIntStateOf(2) }
    var blueInt by remember { mutableIntStateOf(2) }
    var greyInt by remember { mutableIntStateOf(7) }
    var sysInt by remember { mutableIntStateOf(7) }
    var bright by remember { mutableStateOf(false) }

    fun currentColor() = if (editMode == EditMode.FOREGROUND) foregroundColor else backgroundColor

    fun setCurrentColor(value: Int) {
        when (editMode) {
            EditMode.FOREGROUND -> foregroundColor = value
            EditMode.BACKGROUND -> backgroundColor = value
        }
    }

    fun computeRgb() { setCurrentColor(16 + (redInt * 36) + (greenInt * 6) + blueInt) }
    fun computeGrey() { setCurrentColor(232 + greyInt) }
    fun computeSys() { setCurrentColor(if (bright) sysInt + 8 else sysInt) }

    // Initialize from existing color values
    LaunchedEffect(editMode) {
        val startVal = currentColor()
        when {
            startVal in 1..15 -> {
                if (startVal > 7) {
                    sysInt = startVal - 8
                    bright = true
                } else {
                    sysInt = startVal
                    bright = false
                }
                selectedTab = ColorTab.SYSTEM
            }
            startVal in 16..231 -> {
                val v = startVal - 16
                redInt = v / 36
                greenInt = (v % 36) / 6
                blueInt = (v % 36) % 6
                selectedTab = ColorTab.XTERM256
            }
            startVal in 232..255 -> {
                greyInt = startVal - 232
                selectedTab = ColorTab.GREYSCALE
            }
        }
    }

    val fgArgb = Colorizer.get256ColorValue(foregroundColor)
    val bgArgb = Colorizer.get256ColorValue(backgroundColor)

    Column(modifier = Modifier.padding(16.dp)) {
        // Preview swatch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(bgArgb)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (editMode == EditMode.FOREGROUND) "Foreground" else "Background",
                color = Color(fgArgb),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Foreground / Background toggle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterChip(
                selected = editMode == EditMode.FOREGROUND,
                onClick = { editMode = EditMode.FOREGROUND },
                label = { Text("Foreground") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = editMode == EditMode.BACKGROUND,
                onClick = { editMode = EditMode.BACKGROUND },
                label = { Text("Background") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab row
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(selected = selectedTab == ColorTab.XTERM256, onClick = {
                selectedTab = ColorTab.XTERM256
                computeRgb()
            }) { Text("256", modifier = Modifier.padding(12.dp)) }
            Tab(selected = selectedTab == ColorTab.GREYSCALE, onClick = {
                selectedTab = ColorTab.GREYSCALE
                computeGrey()
            }) { Text("BW", modifier = Modifier.padding(12.dp)) }
            Tab(selected = selectedTab == ColorTab.SYSTEM, onClick = {
                selectedTab = ColorTab.SYSTEM
                computeSys()
            }) { Text("Sys", modifier = Modifier.padding(12.dp)) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab content
        when (selectedTab) {
            ColorTab.XTERM256 -> {
                ColorSlider("Red", redInt, 0, 5) { redInt = it; computeRgb() }
                ColorSlider("Green", greenInt, 0, 5) { greenInt = it; computeRgb() }
                ColorSlider("Blue", blueInt, 0, 5) { blueInt = it; computeRgb() }
            }
            ColorTab.GREYSCALE -> {
                ColorSlider("Grey", greyInt, 0, 23) { greyInt = it; computeGrey() }
            }
            ColorTab.SYSTEM -> {
                ColorSlider("Sys", sysInt, 0, 7) { sysInt = it; computeSys() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bright, onCheckedChange = { bright = it; computeSys() })
                    Text("Bright")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Done / Cancel buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDone(foregroundColor, backgroundColor) }) { Text("Done") }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.width(48.dp))
        Button(onClick = { if (value > min) onValueChange(value - 1) },
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("-") }
        Text(
            value.toString(),
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = { if (value < max) onValueChange(value + 1) },
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("+") }
    }
}
