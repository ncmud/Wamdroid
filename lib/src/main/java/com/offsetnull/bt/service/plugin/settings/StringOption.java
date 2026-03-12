package com.offsetnull.bt.service.plugin.settings;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class StringOption extends BaseOption {

    // protected ArrayList<String> items;

    public StringOption() {
        type = TYPE.STRING;
        // items = new ArrayList<String>();
        this.value = new String();
    }

    public StringOption copy() {
        StringOption tmp = new StringOption();
        tmp.key = this.key;
        tmp.value = this.value;
        tmp.title = this.title;
        tmp.description = this.description;

        return tmp;
    }

    public void reset() {
        this.key = "";
        this.value = new String();
        this.description = "";
        this.title = "";
        // this.items.clear();
    }

    @Override
    public void setValue(Object o) {
        if (o instanceof String) {
            value = (String) o;
        } else if (o instanceof String) {
            value = o.toString();
        }
    }

    @Override
    public Object getValue() {
        // TODO Auto-generated method stub
        return (Object) value;
    }

    @Override
    public Object getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDefaultValue(Object o) {
        // TODO Auto-generated method stub

    }

    public void saveToXML(XmlSerializer out)
            throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag("", "string");
        out.attribute("", "key", this.key);
        out.attribute("", "title", this.title);
        out.attribute("", "summary", this.description);
        // out.attribute("","value",Integer.toString((Integer)this.value));
        // out.startTag("", "value");
        out.text((String) this.value);
        // out.endTag("", "value");
        // have to save the list items.

        out.endTag("", "string");
    }
}
