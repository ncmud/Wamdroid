package org.ncmud.mudwammer.speedwalk;

public interface DirectionEditorDoneListener {
    public void newDirection(DirectionData d);

    public void editDirection(DirectionData old, DirectionData mod);
}
