package com.offsetnull.bt.responder.color

import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offsetnull.bt.R
import com.offsetnull.bt.responder.TriggerResponder
import com.offsetnull.bt.responder.TriggerResponderEditorDoneListener
import com.offsetnull.bt.service.Colorizer
import com.offsetnull.bt.ui.setComposeContent

class ColorActionEditor(
    context: Context,
    private val original: TriggerResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : ComponentDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        val initialFg = (original as? ColorAction)?.color ?: ColorAction.DEFAULT_COLOR
        val initialBg = (original as? ColorAction)?.backgroundColor ?: ColorAction.DEFAULT_BACKGROUND_COLOR

        setComposeContent {
            ColorEditorContent(
                initialForeground = initialFg,
                initialBackground = initialBg,
                onDone = { fg, bg -> doExit(fg, bg) },
                onCancel = { dismiss() }
            )
        }
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

/** Xterm-256 color encoding constants. */
private object Xterm256 {
    const val SYS_COLOR_MAX = 15
    const val SYS_BRIGHT_OFFSET = 8
    const val SYS_BRIGHT_THRESHOLD = 7
    const val CUBE_START = 16
    const val CUBE_END = 231
    const val CUBE_RED_FACTOR = 36
    const val CUBE_GREEN_FACTOR = 6
    const val CUBE_CHANNEL_MAX = 5
    const val GREY_START = 232
    const val GREY_END = 255
    const val GREY_MAX = 23
}

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
    var greyInt by remember { mutableIntStateOf(Xterm256.SYS_BRIGHT_THRESHOLD) }
    var sysInt by remember { mutableIntStateOf(Xterm256.SYS_BRIGHT_THRESHOLD) }
    var bright by remember { mutableStateOf(false) }

    fun currentColor() = if (editMode == EditMode.FOREGROUND) foregroundColor else backgroundColor

    fun setCurrentColor(value: Int) {
        when (editMode) {
            EditMode.FOREGROUND -> foregroundColor = value
            EditMode.BACKGROUND -> backgroundColor = value
        }
    }

    fun computeRgb() {
        setCurrentColor(
            Xterm256.CUBE_START + (redInt * Xterm256.CUBE_RED_FACTOR) +
                (greenInt * Xterm256.CUBE_GREEN_FACTOR) + blueInt
        )
    }
    fun computeGrey() { setCurrentColor(Xterm256.GREY_START + greyInt) }
    fun computeSys() {
        setCurrentColor(if (bright) sysInt + Xterm256.SYS_BRIGHT_OFFSET else sysInt)
    }

    LaunchedEffect(editMode) {
        val startVal = currentColor()
        when {
            startVal in 1..Xterm256.SYS_COLOR_MAX -> {
                if (startVal > Xterm256.SYS_BRIGHT_THRESHOLD) {
                    sysInt = startVal - Xterm256.SYS_BRIGHT_OFFSET
                    bright = true
                } else {
                    sysInt = startVal
                    bright = false
                }
                selectedTab = ColorTab.SYSTEM
            }
            startVal in Xterm256.CUBE_START..Xterm256.CUBE_END -> {
                val v = startVal - Xterm256.CUBE_START
                redInt = v / Xterm256.CUBE_RED_FACTOR
                greenInt = (v % Xterm256.CUBE_RED_FACTOR) / Xterm256.CUBE_GREEN_FACTOR
                blueInt = (v % Xterm256.CUBE_RED_FACTOR) % Xterm256.CUBE_GREEN_FACTOR
                selectedTab = ColorTab.XTERM256
            }
            startVal in Xterm256.GREY_START..Xterm256.GREY_END -> {
                greyInt = startVal - Xterm256.GREY_START
                selectedTab = ColorTab.GREYSCALE
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        ColorPreviewSwatch(foregroundColor, backgroundColor, editMode)
        Spacer(modifier = Modifier.height(8.dp))
        EditModeSelector(editMode) { editMode = it }
        Spacer(modifier = Modifier.height(8.dp))
        ColorTabBar(selectedTab, { selectedTab = it; computeRgb() },
            { selectedTab = it; computeGrey() }, { selectedTab = it; computeSys() })
        Spacer(modifier = Modifier.height(12.dp))
        ColorTabContent(selectedTab, redInt, greenInt, blueInt, greyInt, sysInt, bright,
            onRedChange = { redInt = it; computeRgb() },
            onGreenChange = { greenInt = it; computeRgb() },
            onBlueChange = { blueInt = it; computeRgb() },
            onGreyChange = { greyInt = it; computeGrey() },
            onSysChange = { sysInt = it; computeSys() },
            onBrightChange = { bright = it; computeSys() })
        Spacer(modifier = Modifier.height(16.dp))
        ActionButtons(onCancel) { onDone(foregroundColor, backgroundColor) }
    }
}

@Composable
private fun ColorPreviewSwatch(foregroundColor: Int, backgroundColor: Int, editMode: EditMode) {
    val fgArgb = Colorizer.get256ColorValue(foregroundColor)
    val bgArgb = Colorizer.get256ColorValue(backgroundColor)
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(bgArgb)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (editMode == EditMode.FOREGROUND) "Foreground" else "Background",
            color = Color(fgArgb),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun EditModeSelector(editMode: EditMode, onModeChange: (EditMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        FilterChip(
            selected = editMode == EditMode.FOREGROUND,
            onClick = { onModeChange(EditMode.FOREGROUND) },
            label = { Text("Foreground") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = editMode == EditMode.BACKGROUND,
            onClick = { onModeChange(EditMode.BACKGROUND) },
            label = { Text("Background") }
        )
    }
}

@Composable
private fun ColorTabBar(
    selectedTab: ColorTab,
    onXterm: (ColorTab) -> Unit,
    onGrey: (ColorTab) -> Unit,
    onSys: (ColorTab) -> Unit
) {
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        Tab(selected = selectedTab == ColorTab.XTERM256,
            onClick = { onXterm(ColorTab.XTERM256) }) {
            Text("256", modifier = Modifier.padding(12.dp))
        }
        Tab(selected = selectedTab == ColorTab.GREYSCALE,
            onClick = { onGrey(ColorTab.GREYSCALE) }) {
            Text("BW", modifier = Modifier.padding(12.dp))
        }
        Tab(selected = selectedTab == ColorTab.SYSTEM,
            onClick = { onSys(ColorTab.SYSTEM) }) {
            Text("Sys", modifier = Modifier.padding(12.dp))
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ColorTabContent(
    selectedTab: ColorTab,
    redInt: Int, greenInt: Int, blueInt: Int, greyInt: Int, sysInt: Int, bright: Boolean,
    onRedChange: (Int) -> Unit, onGreenChange: (Int) -> Unit, onBlueChange: (Int) -> Unit,
    onGreyChange: (Int) -> Unit, onSysChange: (Int) -> Unit, onBrightChange: (Boolean) -> Unit
) {
    when (selectedTab) {
        ColorTab.XTERM256 -> {
            ColorSlider("Red", redInt, 0, Xterm256.CUBE_CHANNEL_MAX, onRedChange)
            ColorSlider("Green", greenInt, 0, Xterm256.CUBE_CHANNEL_MAX, onGreenChange)
            ColorSlider("Blue", blueInt, 0, Xterm256.CUBE_CHANNEL_MAX, onBlueChange)
        }
        ColorTab.GREYSCALE -> {
            ColorSlider("Grey", greyInt, 0, Xterm256.GREY_MAX, onGreyChange)
        }
        ColorTab.SYSTEM -> {
            ColorSlider("Sys", sysInt, 0, Xterm256.SYS_BRIGHT_THRESHOLD, onSysChange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = bright, onCheckedChange = onBrightChange)
                Text("Bright")
            }
        }
    }
}

@Composable
private fun ActionButtons(onCancel: () -> Unit, onDone: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onDone) { Text("Done") }
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
        Text(value.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        Button(onClick = { if (value < max) onValueChange(value + 1) },
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("+") }
    }
}
