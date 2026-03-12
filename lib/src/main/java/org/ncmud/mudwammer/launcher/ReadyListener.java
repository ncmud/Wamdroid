package org.ncmud.mudwammer.launcher;

public interface ReadyListener {
    public void ready(MudConnection newData);

    public void modify(MudConnection old, MudConnection newData);
}
