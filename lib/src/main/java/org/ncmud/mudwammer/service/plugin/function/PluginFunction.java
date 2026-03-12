package org.ncmud.mudwammer.service.plugin.function;

import android.os.Handler;

import org.ncmud.mudwammer.service.plugin.Plugin;

import org.keplerproject.luajava.JavaFunction;
import org.keplerproject.luajava.LuaState;

public abstract class PluginFunction extends JavaFunction {

    Plugin mPlugin = null;
    Handler mHandler = null;

    public PluginFunction(LuaState L, Plugin p, Handler h) {
        super(L);
        this.mPlugin = p;
        mHandler = h;
    }
}
