package com.offsetnull.bt.service.plugin.settings;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.math.BigInteger;

public class IntegerOption extends BaseOption {

    public IntegerOption() {
        this.type = TYPE.INTEGER;
        this.setValue(Integer.valueOf(0));
    }

    @Override
    public void setValue(Object o) {
        if (o instanceof Integer) {
            value = (Integer) o;
        } else if (o instanceof String) {
            try {
                String str = (String) o;
                if (str.startsWith("#")) {
                    BigInteger bigint = new BigInteger(str.substring(1, str.length() - 1), 16);
                    value = bigint.intValue();
                } else {
                    int num = Integer.parseInt((String) o);
                    value = (Integer) num;
                }
            } catch (NumberFormatException e) {

            }
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
        out.startTag("", "integer");
        out.attribute("", "key", this.key);
        out.attribute("", "title", this.title);
        out.attribute("", "summary", this.description);
        out.text(Integer.toString((Integer) this.value));
        out.endTag("", "integer");
    }
}
