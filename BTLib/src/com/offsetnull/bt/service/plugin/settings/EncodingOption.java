package com.offsetnull.bt.service.plugin.settings;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

public class EncodingOption extends BaseOption {

	public EncodingOption() {
		this.type = TYPE.ENCODING;
		setValue("ISO-8859-1");
	}

	@Override
	public void setValue(Object o) {
		if(o instanceof String) {
			value = (String)o;
		} else {
			value = o.toString();
		}
	}

	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return (Object)value;
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

	public void saveToXML(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
		out.startTag("", "encoding");
		out.attribute("", "key", this.key);
		out.attribute("", "title", this.title);
		out.attribute("", "summary", this.description);
		out.text((String)this.value);
		out.endTag("", "encoding");
	}

}
