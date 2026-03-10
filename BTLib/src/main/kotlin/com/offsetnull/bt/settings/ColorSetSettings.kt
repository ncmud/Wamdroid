package com.offsetnull.bt.settings

import com.offsetnull.bt.button.SlickButtonData

data class ColorSetSettings(
    var selectedColor: Int = SlickButtonData.DEFAULT_SELECTED_COLOR,
    var primaryColor: Int = SlickButtonData.DEFAULT_COLOR,
    var flipColor: Int = SlickButtonData.DEFAULT_FLIP_COLOR,
    var labelColor: Int = SlickButtonData.DEFAULT_LABEL_COLOR,
    var buttonHeight: Int = SlickButtonData.DEFAULT_BUTTON_HEIGHT,
    var buttonWidth: Int = SlickButtonData.DEFAULT_BUTTON_WDITH,
    var labelSize: Int = SlickButtonData.DEFAULT_LABEL_SIZE,
    var flipLabelColor: Int = SlickButtonData.DEFAULT_FLIPLABEL_COLOR,
    @get:JvmName("isLocked") var locked: Boolean = DEFAULT_LOCKED,
    @get:JvmName("isLockNewButtons") var lockNewButtons: Boolean = DEFAULT_LOCKNEWBUTTONS,
    @get:JvmName("isLockMoveButtons") var lockMoveButtons: Boolean = DEFAULT_LOCKMOVEBUTTONS,
    @get:JvmName("isLockEditButtons") var lockEditButtons: Boolean = DEFAULT_LOCKEDITBUTTONS,
) {
    companion object {
        const val DEFAULT_LOCKED = false
        const val DEFAULT_LOCKNEWBUTTONS = true
        const val DEFAULT_LOCKMOVEBUTTONS = false
        const val DEFAULT_LOCKEDITBUTTONS = false
    }

    fun copy(): ColorSetSettings = ColorSetSettings(
        selectedColor, primaryColor, flipColor, labelColor,
        buttonHeight, buttonWidth, labelSize, flipLabelColor,
        locked, lockNewButtons, lockMoveButtons, lockEditButtons,
    )

    fun toDefautls() {
        selectedColor = SlickButtonData.DEFAULT_SELECTED_COLOR
        primaryColor = SlickButtonData.DEFAULT_COLOR
        flipColor = SlickButtonData.DEFAULT_FLIP_COLOR
        labelColor = SlickButtonData.DEFAULT_LABEL_COLOR
        buttonWidth = SlickButtonData.DEFAULT_BUTTON_WDITH
        buttonHeight = SlickButtonData.DEFAULT_BUTTON_HEIGHT
        labelSize = SlickButtonData.DEFAULT_LABEL_SIZE
        flipLabelColor = SlickButtonData.DEFAULT_FLIPLABEL_COLOR
        locked = DEFAULT_LOCKED
        lockNewButtons = DEFAULT_LOCKNEWBUTTONS
        lockMoveButtons = DEFAULT_LOCKMOVEBUTTONS
        lockEditButtons = DEFAULT_LOCKEDITBUTTONS
    }
}
