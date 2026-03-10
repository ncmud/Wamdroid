package com.offsetnull.bt.service.plugin.settings;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public abstract class BaseOption extends Option {

    protected Object value;
    protected Object defaultValue;

    public abstract void setValue(Object o);

    public abstract Object getValue();

    public abstract Object getDefaultValue();

    public abstract void setDefaultValue(Object o);

    public abstract void saveToXML(XmlSerializer out)
            throws IllegalArgumentException, IllegalStateException, IOException;
}
