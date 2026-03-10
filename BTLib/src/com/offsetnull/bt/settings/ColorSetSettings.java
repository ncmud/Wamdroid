package com.offsetnull.bt.settings;

import com.offsetnull.bt.button.SlickButtonData;

public class ColorSetSettings {

    private int selectedColor;
    private int primaryColor;
    private int flipColor;
    private int labelColor;
    private int buttonHeight;
    private int buttonWidth;
    private int labelSize;
    private int flipLabelColor;
    private boolean locked;
    private boolean lockNewButtons;
    private boolean lockMoveButtons;
    private boolean lockEditButtons;

    public static final boolean DEFAULT_LOCKED = false;
    public static final boolean DEFAULT_LOCKNEWBUTTONS = true;
    public static final boolean DEFAULT_LOCKMOVEBUTTONS = false;
    public static final boolean DEFAULT_LOCKEDITBUTTONS = false;

    public ColorSetSettings() {
        toDefautls();
    }

    public ColorSetSettings copy() {
        ColorSetSettings tmp = new ColorSetSettings();
        tmp.selectedColor = this.selectedColor;
        tmp.primaryColor = this.primaryColor;
        tmp.flipColor = this.flipColor;
        tmp.labelColor = this.labelColor;
        tmp.buttonHeight = this.buttonHeight;
        tmp.buttonWidth = this.buttonWidth;
        tmp.labelSize = this.labelSize;
        tmp.flipLabelColor = this.flipLabelColor;
        tmp.locked = this.locked;
        tmp.lockNewButtons = this.lockNewButtons;
        tmp.lockMoveButtons = this.lockMoveButtons;
        tmp.lockEditButtons = this.lockEditButtons;
        return tmp;
    }

    public void toDefautls() {
        selectedColor = SlickButtonData.DEFAULT_SELECTED_COLOR;
        primaryColor = SlickButtonData.DEFAULT_COLOR;
        flipColor = SlickButtonData.DEFAULT_FLIP_COLOR;
        labelColor = SlickButtonData.DEFAULT_LABEL_COLOR;
        buttonWidth = SlickButtonData.DEFAULT_BUTTON_WDITH;
        buttonHeight = SlickButtonData.DEFAULT_BUTTON_HEIGHT;
        labelSize = SlickButtonData.DEFAULT_LABEL_SIZE;
        flipLabelColor = SlickButtonData.DEFAULT_FLIPLABEL_COLOR;
        locked = ColorSetSettings.DEFAULT_LOCKED;
        lockNewButtons = ColorSetSettings.DEFAULT_LOCKNEWBUTTONS;
        lockMoveButtons = ColorSetSettings.DEFAULT_LOCKMOVEBUTTONS;
        lockEditButtons = ColorSetSettings.DEFAULT_LOCKEDITBUTTONS;
    }

    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof ColorSetSettings)) {
            return false;
        }

        ColorSetSettings test = (ColorSetSettings) o;

        if (this.selectedColor != test.selectedColor) return false;
        if (this.flipColor != test.flipColor) return false;
        if (this.primaryColor != test.primaryColor) return false;
        if (this.labelColor != test.labelColor) return false;
        if (this.flipLabelColor != test.flipLabelColor) return false;
        if (this.labelSize != test.labelSize) return false;
        if (this.buttonHeight != test.buttonHeight) return false;
        if (this.buttonWidth != test.buttonWidth) return false;
        if (this.locked != test.locked) return false;
        if (this.lockNewButtons != test.lockNewButtons) return false;
        if (this.lockMoveButtons != test.lockMoveButtons) return false;
        if (this.lockEditButtons != test.lockEditButtons) return false;
        return true;
    }

    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setPrimaryColor(int primaryColor) {
        this.primaryColor = primaryColor;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public void setFlipColor(int flipColor) {
        this.flipColor = flipColor;
    }

    public int getFlipColor() {
        return flipColor;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public void setButtonHeight(int buttonHeight) {
        this.buttonHeight = buttonHeight;
    }

    public int getButtonHeight() {
        return buttonHeight;
    }

    public void setButtonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
    }

    public int getButtonWidth() {
        return buttonWidth;
    }

    public void setLabelSize(int labelSize) {
        this.labelSize = labelSize;
    }

    public int getLabelSize() {
        return labelSize;
    }

    public void setFlipLabelColor(int flipLabelColor) {
        this.flipLabelColor = flipLabelColor;
    }

    public int getFlipLabelColor() {
        return flipLabelColor;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLockNewButtons(boolean lockNewButtons) {
        this.lockNewButtons = lockNewButtons;
    }

    public boolean isLockNewButtons() {
        return lockNewButtons;
    }

    public void setLockMoveButtons(boolean lockMoveButtons) {
        this.lockMoveButtons = lockMoveButtons;
    }

    public boolean isLockMoveButtons() {
        return lockMoveButtons;
    }

    public void setLockEditButtons(boolean lockEditButtons) {
        this.lockEditButtons = lockEditButtons;
    }

    public boolean isLockEditButtons() {
        return lockEditButtons;
    }
}
