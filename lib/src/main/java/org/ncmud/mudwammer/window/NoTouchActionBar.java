package org.ncmud.mudwammer.window;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.Toolbar;

// import android.widget.Toolbar;

public class NoTouchActionBar extends Toolbar {

    public NoTouchActionBar(Context c) {
        super(c);
    }

    public NoTouchActionBar(Context c, AttributeSet s) {
        super(c, s);
    }

    public NoTouchActionBar(Context c, AttributeSet s, int d) {
        super(c, s, d);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
