package com.offsetnull.bt.button

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.offsetnull.bt.settings.ColorSetSettings

@Entity(tableName = "buttons")
class SlickButtonData(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var connectionId: Long = 0,
    var buttonSetName: String = "",
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 80,
    var height: Int = 80,
    text: String = "",
    label: String = "",
    flipCommand: String = "",
    targetSet: String = "",
    var primaryColor: Int = DEFAULT_COLOR,
    var selectedColor: Int = DEFAULT_SELECTED_COLOR,
    var flipColor: Int = DEFAULT_FLIP_COLOR,
    var labelColor: Int = DEFAULT_LABEL_COLOR,
    var labelSize: Int = DEFAULT_LABEL_SIZE,
    flipLabel: String = "",
    var flipLabelColor: Int = DEFAULT_FLIPLABEL_COLOR,
) {
    var text: String = text
        set(value) { field = value ?: "" }
    var label: String = label
        set(value) { field = value ?: "" }
    var flipCommand: String = flipCommand
        set(value) { field = value ?: "" }
    var targetSet: String = targetSet
        set(value) { field = value ?: "" }
    var flipLabel: String = flipLabel
        set(value) { field = value ?: "" }

    @Ignore
    @JvmField
    var MOVE_STATE: Int = MOVE_FREE

    companion object {
        const val MOVE_FREE = 0
        const val MOVE_NUDGE = 1
        const val MOVE_FREEZE = 2
        const val DEFAULT_COLOR = 0x880000FF.toInt()
        const val DEFAULT_SELECTED_COLOR = 0x8800FF00.toInt()
        const val DEFAULT_FLIP_COLOR = 0x88FF0000.toInt()
        const val DEFAULT_LABEL_COLOR = 0xAAAAAAAA.toInt()
        const val DEFAULT_FLIPLABEL_COLOR = 0x990000FF.toInt()
        const val DEFAULT_BUTTON_WDITH = 48
        const val DEFAULT_BUTTON_HEIGHT = 48
        const val DEFAULT_LABEL_SIZE = 16
    }

    constructor(ix: Int, iy: Int, itext: String, ilbl: String) : this(
        x = ix, y = iy, text = itext, label = ilbl
    )

    fun setFromSetSettings(new: ColorSetSettings, old: ColorSetSettings) {
        if (primaryColor == old.primaryColor) primaryColor = new.primaryColor
        if (selectedColor == old.selectedColor) selectedColor = new.selectedColor
        if (flipColor == old.flipColor) flipColor = new.flipColor
        if (labelColor == old.labelColor) labelColor = new.labelColor
        if (flipLabelColor == old.flipLabelColor) flipLabelColor = new.flipLabelColor
        if (labelSize == old.labelSize) labelSize = new.labelSize
        if (width == old.buttonWidth) width = new.buttonWidth
        if (height == old.buttonHeight) height = new.buttonHeight
    }

    fun copy(): SlickButtonData {
        val tmp = SlickButtonData()
        tmp.id = id; tmp.connectionId = connectionId; tmp.buttonSetName = buttonSetName
        tmp.x = x; tmp.y = y; tmp.width = width; tmp.height = height
        tmp.text = text; tmp.label = label; tmp.flipCommand = flipCommand
        tmp.targetSet = targetSet; tmp.primaryColor = primaryColor
        tmp.selectedColor = selectedColor; tmp.flipColor = flipColor
        tmp.labelColor = labelColor; tmp.labelSize = labelSize
        tmp.flipLabel = flipLabel; tmp.flipLabelColor = flipLabelColor
        tmp.MOVE_STATE = MOVE_STATE
        return tmp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SlickButtonData) return false
        return x == other.x && y == other.y && height == other.height && width == other.width
            && label == other.label && text == other.text && flipCommand == other.flipCommand
            && MOVE_STATE == other.MOVE_STATE && targetSet == other.targetSet
            && primaryColor == other.primaryColor && selectedColor == other.selectedColor
            && flipColor == other.flipColor && labelColor == other.labelColor
            && labelSize == other.labelSize && flipLabel == other.flipLabel
            && flipLabelColor == other.flipLabelColor
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + text.hashCode()
        result = 31 * result + label.hashCode()
        return result
    }

    override fun toString(): String {
        return "$x||$y||${text.ifEmpty { "[NONE]" }}||${label.ifEmpty { "[NONE]" }}" +
            "||${flipCommand.ifEmpty { "[NONE]" }}||$MOVE_STATE||$targetSet||$width||$height"
    }
}
