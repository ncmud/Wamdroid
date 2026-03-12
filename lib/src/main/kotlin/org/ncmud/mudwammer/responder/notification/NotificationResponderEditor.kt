package org.ncmud.mudwammer.responder.notification

import android.app.AlertDialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ncmud.mudwammer.R
import org.ncmud.mudwammer.responder.TriggerResponder.FIRE_WHEN
import org.ncmud.mudwammer.responder.TriggerResponderEditorDoneListener
import org.ncmud.mudwammer.ui.EditorDialogScaffold
import org.ncmud.mudwammer.ui.setComposeContent
import java.io.File
import java.io.IOException

private data class NotificationResult(
    val title: String,
    val message: String,
    val useLights: Boolean,
    val lightColor: Int,
    val useVibrate: Boolean,
    val vibrateLength: Int,
    val useSound: Boolean,
    val soundPath: String,
    val spawnNew: Boolean
)

private const val SOUND_LIST_OFFSET = 2
private const val VIBRATE_LONG = 3
private const val VIBRATE_SUPER_LONG = 4

class NotificationResponderEditor(
    context: Context,
    input: NotificationResponder?,
    private val finishWith: TriggerResponderEditorDoneListener
) : ComponentDialog(context) {

    private val responder: NotificationResponder
    private val original: NotificationResponder?
    private val isEditor: Boolean
    private var mp = MediaPlayer()

    init {
        if (input == null) {
            responder = NotificationResponder()
            responder.fireType = FIRE_WHEN.WINDOW_BOTH
            original = null
            isEditor = false
        } else {
            responder = input
            original = input.copy()
            isEditor = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(R.drawable.dialog_window_crawler1)

        setComposeContent {
            NotificationEditorContent(
                initialTitle = responder.title ?: "",
                initialMessage = responder.message ?: "",
                initialUseLights = responder.isUseDefaultLight,
                initialLightColor = responder.colorToUse,
                initialUseVibrate = responder.isUseDefaultVibrate,
                initialVibrateLength = responder.vibrateLength,
                initialUseSound = responder.isUseDefaultSound,
                initialSoundPath = responder.soundPath ?: "",
                initialSpawnNew = responder.isSpawnNewNotification,
                onPickLightColor = { onChecked, callback -> pickLightColor(onChecked, callback) },
                onPickVibrate = { onChecked, callback -> pickVibrate(onChecked, callback) },
                onPickSound = { onChecked, callback -> pickSound(onChecked, callback) },
                onDone = { result -> doFinish(result) },
                onCancel = { dismiss() }
            )
        }
    }

    private fun doFinish(result: NotificationResult) {
        responder.title = result.title
        responder.message = result.message
        responder.isUseDefaultLight = result.useLights
        responder.colorToUse = result.lightColor
        responder.isUseDefaultVibrate = result.useVibrate
        responder.vibrateLength = result.vibrateLength
        responder.isUseDefaultSound = result.useSound
        responder.soundPath = result.soundPath
        responder.isSpawnNewNotification = result.spawnNew

        if (isEditor) {
            finishWith.editTriggerResponder(responder, original)
        } else {
            finishWith.newTriggerResponder(responder)
        }
        dismiss()
    }

    private val lightColors = arrayOf("Default", "Blue", "Green", "Red", "Magenta", "Cyan", "White")
    private val lightColorValues = intArrayOf(
        0x00000000, 0xFF0000FF.toInt(), 0xFF00FF00.toInt(), 0xFFFF0000.toInt(),
        0xFFFF00FF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFFFFF.toInt()
    )

    private fun pickLightColor(onChecked: Boolean, callback: (Boolean, Int, String) -> Unit) {
        if (!onChecked) {
            callback(false, 0, "Currently disabled.")
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select Color")
        builder.setItems(lightColors) { _, which ->
            val color = lightColorValues[which]
            val label = if (which == 0) "Currently using: default"
            else context.getString(R.string.fmt_currently_using, lightColors[which])
            callback(true, color, label)
        }
        builder.setOnCancelListener {
            callback(false, 0, "Currently disabled.")
        }
        builder.show()
    }

    private val vibrateLabels = arrayOf("Default", "Very Short", "Short", "Long", "Suuuper Long")

    private fun pickVibrate(onChecked: Boolean, callback: (Boolean, Int, String) -> Unit) {
        if (!onChecked) {
            callback(false, 0, "Currently disabled.")
            return
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select Sequence:")
        builder.setItems(vibrateLabels) { _, which ->
            val label = if (which == 0) "Currently using: default"
            else context.getString(R.string.fmt_currently_using, vibrateLabels[which])
            callback(true, which, label)
        }
        builder.setOnCancelListener {
            callback(false, 0, "Currently disabled.")
        }
        builder.show()
    }

    @Suppress("DEPRECATION")
    private fun scanAvailableSounds(): Map<String, String> {
        val paths = mutableMapOf<String, String>()
        val systemPaths = arrayOf(
            "/system/media/audio/ringtones/",
            "/system/media/audio/alarms/",
            "/system/media/audio/notificiation/"
        )
        for (path in systemPaths) {
            val dir = File(path)
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { paths[it.name] = it.path }
            }
        }
        val btDir = File(Environment.getExternalStorageDirectory(), "/BlowTorch/")
        btDir.listFiles { _, name -> name.endsWith(".mp3") }?.forEach {
            paths[it.name] = it.path
        }
        return paths
    }

    private fun findCurrentSoundPosition(soundNames: List<String>, paths: Map<String, String>): Int {
        if (responder.isUseDefaultSound && responder.soundPath.isNullOrEmpty()) return 1
        soundNames.forEachIndexed { idx, name ->
            if (paths[name] == responder.soundPath) return idx + SOUND_LIST_OFFSET
        }
        return 0
    }

    private fun previewSound(path: String) {
        try {
            mp.stop()
            mp = MediaPlayer()
            mp.setDataSource(path)
            mp.prepare()
            mp.start()
        } catch (e: IOException) {
            Log.e("NotificationEditor", "Failed to play sound preview", e)
        }
    }

    private fun pickSound(onChecked: Boolean, callback: (Boolean, String, String) -> Unit) {
        if (!onChecked) {
            callback(false, "", "Currently disabled.")
            return
        }

        val state = Environment.getExternalStorageState()
        if (state != Environment.MEDIA_MOUNTED_READ_ONLY && state != Environment.MEDIA_MOUNTED) {
            callback(false, "", "Currently disabled.")
            return
        }

        val paths = scanAvailableSounds()
        val soundNames = paths.keys.toList()
        val items = Array(soundNames.size + SOUND_LIST_OFFSET) { i ->
            when (i) {
                0 -> "Disabled"
                1 -> "Default"
                else -> soundNames[i - SOUND_LIST_OFFSET]
            }
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Pick Sound: (Back to Exit)")
        builder.setSingleChoiceItems(items, findCurrentSoundPosition(soundNames, paths)) { _, which ->
            when (which) {
                0 -> callback(false, "", "Currently disabled.")
                1 -> callback(true, "", "Currently using: default")
                else -> {
                    val name = soundNames[which - SOUND_LIST_OFFSET]
                    val path = paths[name] ?: ""
                    val label = if (path.isEmpty()) "Currently using: default"
                    else context.getString(R.string.fmt_currently_using, path)
                    callback(true, path, label)
                    previewSound(path)
                }
            }
        }
        builder.setOnCancelListener { mp.stop() }
        builder.show()
    }
}

@Suppress("LongParameterList")
@Composable
private fun NotificationEditorContent(
    initialTitle: String,
    initialMessage: String,
    initialUseLights: Boolean,
    initialLightColor: Int,
    initialUseVibrate: Boolean,
    initialVibrateLength: Int,
    initialUseSound: Boolean,
    initialSoundPath: String,
    initialSpawnNew: Boolean,
    onPickLightColor: (Boolean, (Boolean, Int, String) -> Unit) -> Unit,
    onPickVibrate: (Boolean, (Boolean, Int, String) -> Unit) -> Unit,
    onPickSound: (Boolean, (Boolean, String, String) -> Unit) -> Unit,
    onDone: (NotificationResult) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var message by remember { mutableStateOf(initialMessage) }
    var useLights by remember { mutableStateOf(initialUseLights) }
    var lightColor by remember { mutableIntStateOf(initialLightColor) }
    var lightsLabel by remember { mutableStateOf(formatLightLabel(initialUseLights, initialLightColor)) }
    var useVibrate by remember { mutableStateOf(initialUseVibrate) }
    var vibrateLength by remember { mutableIntStateOf(initialVibrateLength) }
    var vibrateLabel by remember { mutableStateOf(formatVibrateLabel(initialUseVibrate, initialVibrateLength)) }
    var useSound by remember { mutableStateOf(initialUseSound) }
    var soundPath by remember { mutableStateOf(initialSoundPath) }
    var soundLabel by remember { mutableStateOf(formatSoundLabel(initialUseSound, initialSoundPath)) }
    var spawnNew by remember { mutableStateOf(initialSpawnNew) }

    EditorDialogScaffold(
        title = "Notification Responder",
        onSave = {
            onDone(NotificationResult(title, message, useLights, lightColor, useVibrate,
                vibrateLength, useSound, soundPath, spawnNew))
        },
        onCancel = onCancel
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useLights,
                onCheckedChange = { checked ->
                    onPickLightColor(checked) { enabled, color, label ->
                        useLights = enabled
                        lightColor = color
                        lightsLabel = label
                    }
                }
            )
            Text("Lights: $lightsLabel")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useVibrate,
                onCheckedChange = { checked ->
                    onPickVibrate(checked) { enabled, length, label ->
                        useVibrate = enabled
                        vibrateLength = length
                        vibrateLabel = label
                    }
                }
            )
            Text("Vibrate: $vibrateLabel")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useSound,
                onCheckedChange = { checked ->
                    onPickSound(checked) { enabled, path, label ->
                        useSound = enabled
                        soundPath = path
                        soundLabel = label
                    }
                }
            )
            Text("Sound: $soundLabel")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = spawnNew, onCheckedChange = { spawnNew = it })
            Text("Spawn new notification")
        }
    }
}

private fun formatLightLabel(enabled: Boolean, color: Int): String {
    if (!enabled) return "Currently disabled."
    return when (color) {
        0 -> "Currently using: default"
        0xFF0000FF.toInt() -> "Currently using: Blue"
        0xFF00FF00.toInt() -> "Currently using: Green"
        0xFFFF0000.toInt() -> "Currently using: Red"
        0xFFFF00FF.toInt() -> "Currently using: Magenta"
        0xFF00FFFF.toInt() -> "Currently using: Cyan"
        0xFFFFFFFF.toInt() -> "Currently using: White"
        else -> "Currently using: default"
    }
}

private fun formatVibrateLabel(enabled: Boolean, length: Int): String {
    if (!enabled) return "Currently disabled."
    return when (length) {
        0 -> "Currently using: default"
        1 -> "Currently using: Very Short"
        2 -> "Currently using: Short"
        VIBRATE_LONG -> "Currently using: Long"
        VIBRATE_SUPER_LONG -> "Currently using: Suuper Long"
        else -> "Currently using: default"
    }
}

private fun formatSoundLabel(enabled: Boolean, path: String): String {
    if (!enabled) return "Currently disabled."
    return if (path.isEmpty()) "Using default sound" else path
}
