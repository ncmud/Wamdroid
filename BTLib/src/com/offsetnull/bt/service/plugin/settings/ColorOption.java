package com.offsetnull.bt.service.plugin.settings;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;

public class ColorOption extends BaseOption {

    public ColorOption() {
        this.type = TYPE.COLOR;
    }

    @Override
    public void setValue(Object o) {
        if (o instanceof Integer) {
            value = (Integer) o;
        } else if (o instanceof String) {
            try {
                String str = (String) o;
                if (str.startsWith("#")) {
                    // Log.e("COLOR","COLOR VALUE SET:"+str.substring(1, str.length()));
                    BigInteger bigint = new BigInteger(str.substring(1, str.length()), 16);
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
        out.startTag("", "color");
        out.attribute("", "key", this.key);
        out.attribute("", "title", this.title);
        out.attribute("", "summary", this.description);
        out.text("#" + Integer.toHexString((Integer) this.value).toUpperCase(Locale.ROOT));
        out.endTag("", "color");
    }
}
