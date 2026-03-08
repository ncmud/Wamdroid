package com.offsetnull.bt.service.plugin.settings;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlSerializer;

public class FileOption extends ListOption {

	ArrayList<String> paths;
	ArrayList<String> extensions;
	//ArrayList<String> items;

	public FileOption() {
		//super();
		this.type = TYPE.FILE;
		paths = new ArrayList<String>(0);
		extensions = new ArrayList<String>(0);
		items = new ArrayList<String>(0);
		setValue("");
	}

	public void addPath(String path) {
		paths.add(path);
	}

	public void addExtension(String extension) {
		extensions.add(extension);
	}

	@Override
	public void setValue(Object o) {
		if(o instanceof String) {
			value = (String)o;
		} else {
			//dunno.
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

	@Override
	public void saveToXML(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
		//this is a different xml serializer routine than the list option.
		out.startTag("", "file");
		out.attribute("", "key", this.key);
		out.attribute("", "title", this.title);
		out.attribute("", "summary", this.description);
		//out.attribute("", "value", (String)this.value);
		out.startTag("", "value");
		out.text((String)this.value);
		out.endTag("", "value");
		for(String path : paths) {
			out.startTag("", "path");
			out.text(path);
			out.endTag("", "path");
		}

		for(String ext : extensions) {
			out.startTag("", "extension");
			out.text(ext);
			out.endTag("", "extension");
		}
		out.endTag("", "file");
	}

	@Override
	public FileOption copy() {
		FileOption tmp = new FileOption();
		tmp.title = this.title;
		tmp.description = this.description;
		tmp.key = this.key;
		tmp.value = this.value;

		tmp.paths = new ArrayList<String>();
		for(String path : this.paths) {
			tmp.paths.add(path);
		}

		tmp.extensions = new ArrayList<String>();
		for(String extension : this.extensions) {
			tmp.extensions.add(extension);
		}

		return tmp;
	}

	@Override
	public void reset() {
		this.title = "";
		this.description = "";
		this.value = new Object();
		this.key = "";
		this.extensions.clear();
		this.paths.clear();
	}
}
