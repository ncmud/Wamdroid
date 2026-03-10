package com.offsetnull.bt.window;

import java.util.ArrayList;

public class StatusGroupData {

    ArrayList<Integer> data = null;

    public StatusGroupData() {
        data = new ArrayList<Integer>();
    }

    public void addInt(int value) {
        data.add(Integer.valueOf(value));
    }
}
