package com.offsetnull.bt.service.plugin.settings;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

public class BooleanOption extends BaseOption {


	public BooleanOption() {
		type = TYPE.BOOLEAN;
	}

	@Override
	public void setValue(Object o) {
		if(o instanceof Boolean) {
			this.value = (Boolean)o;
		} else if(o instanceof String) {
			String str = (String)o;
			if(str.equals("true")) {
				value = (Boolean)true;
			} else if(str.equals("false")) {
				value = (Boolean)false;
			}
		}
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(Object o) {
		if(o instanceof Boolean) {
			defaultValue = (Boolean)o;
		}
	}

	public void saveToXML(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
		out.startTag("", "boolean");
		out.attribute("", "key", this.key);
		out.attribute("", "title", this.title);
		out.attribute("", "summary", this.description);
		out.text(((Boolean)this.value).toString());
		out.endTag("", "boolean");
	}
}
