/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import android.Manifest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.offsetnull.bt.service.plugin.Plugin;
import com.offsetnull.bt.service.plugin.settings.BaseOption;
import com.offsetnull.bt.service.plugin.settings.ConnectionSetttingsParser;
import com.offsetnull.bt.service.plugin.settings.Option;
import com.offsetnull.bt.service.plugin.settings.PluginParser;
import com.offsetnull.bt.service.plugin.settings.SettingsGroup;
import com.offsetnull.bt.settings.ConfigurationLoader;

import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlSerializer;

/** Handles settings persistence operations extracted from Connection. */
public class SettingsPersistence {

	/** Minimum starting font size for the fit routine. */
	private static final float MIN_FONT_SIZE = 8.0f;

	/** Target char width for fit routine. */
	private static final float TARGET_FIT_WIDTH = 80.0f;

	/** The value of 4. */
	private static final int FOUR = 4;

	/** String name of the default output window. */
	private static final String MAIN_WINDOW = "mainDisplay";

	/** Pattern for matching .xml extensions not case sensitive. */
	private final Pattern mXMLExtensionPattern = Pattern.compile("^.+\\.[xX][mM][lL]$");
	/** Matcher for matching .xml extensions not case sensitive. */
	private final Matcher mXMLExtensionMatcher = mXMLExtensionPattern.matcher("");

	private final SettingsContext context;

	public SettingsPersistence(SettingsContext context) {
		this.context = context;
	}

	/** The main starting point for the save settings routine. */
	public final void saveMainSettings() {
		Pattern invalidchars = Pattern.compile("\\W");
		Matcher replacebadchars = invalidchars.matcher(context.getDisplay());
		String prefsname = replacebadchars.replaceAll("");
		prefsname = prefsname.replaceAll("/", "");
		String rootPath = prefsname + ".xml";

		String internal = context.getService().getApplicationContext().getApplicationInfo().dataDir + "/files/";
		String oldpath = internal + rootPath;
		exportSettings(oldpath);
	}

	/** Export settings routine.
	 *
	 * @param path File name to save to. Must be absolute from the OS root directory.
	 */
	public final void exportSettings(final String path) {
		boolean domessage = false;
		boolean addextra = false;
		String filename = path;
		int state = ContextCompat.checkSelfPermission(context.getService().getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
		boolean external = (state == PackageManager.PERMISSION_GRANTED) ? true : false;
		File cachedir = context.getContext().getCacheDir();
		String btdir = "/BlowTorch";
		if (!filename.startsWith("/")) {
			domessage = true;
			File ext = Environment.getExternalStorageDirectory();
			String dir = ConfigurationLoader.getConfigurationValue("exportDirectory", context.getService().getApplicationContext());
			if(external) {
				btdir = ext.getAbsolutePath() + "/" + dir + "/";
				filename = ext.getAbsolutePath() + "/" + dir + "/" + filename;
			} else {
				btdir = context.getService().getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
				filename = context.getService().getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/" + filename;
			}
			mXMLExtensionMatcher.reset(filename);
			if (!mXMLExtensionMatcher.matches()) {
				filename = filename + ".xml";
				addextra = true;
			}

			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
				cachedir = context.getContext().getExternalCacheDir();
			} else {
				String packagename = context.getContext().getPackageName();
				cachedir = new File(Environment.getExternalStorageDirectory(),"/Android/data/"+packagename+"/cache/");
			}
		}

		boolean passed = true;
		File file = new File(filename);
		FileOutputStream fos = null;
		File tmpfile = null;
		try {
		tmpfile = File.createTempFile("settings", "xml",cachedir);

		fos = new FileOutputStream(tmpfile);
		String foo = ConnectionSetttingsParser.outputXML(context.getConnectionSettings(), context.getPlugins());
		fos.write(foo.getBytes());
		fos.close();
		} catch (Exception e) {
			context.getService().dispatchSaveError(e.getLocalizedMessage());
			passed = false;
		} finally {
			if(passed) {
				try {
					fos.close();
				} catch (IOException e) {
					//we are in real trouble here.
				}
				File makeme = new File(btdir);
				makeme.mkdirs();
				boolean success = tmpfile.renameTo(file);
				if(success) {
					Log.e("BT","file shadow copy success");
				} else {
					Log.e("BT","file shadow copy failed");
				}
			} else {
				if(fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						//real trouble.
					}
				}
			}
		}


		for (String link : context.getLinkMap().keySet()) {
			ArrayList<String> plugins  = context.getLinkMap().get(link);
			boolean doExport = false;
			String fullpath = "";
			for (String plugin : plugins) {
				Plugin p = context.getPluginMap().get(plugin);
				if (p.getSettings().isDirty()) {
					doExport = true;
					fullpath = p.getFullPath();
				}
			}

			if (doExport) {
				XmlSerializer out = Xml.newSerializer();
				StringWriter writer = new StringWriter();

				File extfile = null;
				FileOutputStream extfilestream = null;
				passed = true;
				File extcachedir = null;
				if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
					extcachedir = context.getContext().getExternalCacheDir();
				} else {
					String packagename = context.getContext().getPackageName();
					extcachedir = new File(Environment.getExternalStorageDirectory(),"/Android/data/"+packagename+"/cache/");
				}
				File tmppluginfile = null;
				String currentplugin = "";
				try {

				out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				out.setOutput(writer);
				out.startDocument("UTF-8", true);
				out.startTag("", "blowtorch");
				out.attribute("", "xmlversion", "2");
				out.startTag("", "plugins");

				for (String plugin :plugins) {
					currentplugin = plugin;
					Plugin p = context.getPluginMap().get(plugin);
					PluginParser.saveToXml(out, p);
					p.getSettings().setDirty(false);
				}

				out.endTag("", "plugins");
				out.endTag("", "blowtorch");
				out.endDocument();

				tmppluginfile = File.createTempFile("plugin_settings", "xml",extcachedir);

				extfile = new File(fullpath);
				extfilestream = new FileOutputStream(tmppluginfile);
				extfilestream.write(writer.toString().getBytes());
				extfilestream.close();
				} catch(Exception e) {
					context.getService().dispatchPluginSaveError(currentplugin,e.getLocalizedMessage());
					passed = false;
				} finally {
					if(extfilestream != null) {
						try {
							extfilestream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					if(passed) {
						long start = System.currentTimeMillis();
						boolean success = tmppluginfile.renameTo(extfile);
						int duraction = (int)(System.currentTimeMillis() - start);
						if(success) {
							Log.e("BT","Plugin shadow copy success, took " + duraction);
						} else {
							Log.e("BT","Plugin shadow copy failure, took " + duraction);
						}
					}
				}
			}

		}


		if (domessage) {
			String message = "Settings Exported to " + filename;
			if (addextra) {
				message = message + "\n.xml extension added.";
			}
			context.getService().dispatchToast(message, true);
		}
	}

	/** Utility method that generates the font size necessary to fit 80 chars to the window width.
	 *
	 * @return the font size that will produce nearest to 80 chars as possible.
	 */
	public int calculate80CharFontSize() {
		int windowWidth = context.getService().getResources().getDisplayMetrics().widthPixels;
		if (context.getService().getResources().getDisplayMetrics().heightPixels > windowWidth) {
			windowWidth = context.getService().getResources().getDisplayMetrics().heightPixels;
		}
		float fontSize = MIN_FONT_SIZE;
		float delta = 1.0f;
		Paint p = new Paint();
		p.setTextSize(MIN_FONT_SIZE);
		p.setTypeface(Typeface.MONOSPACE);
		boolean done = false;

		float charWidth = p.measureText("A");
		float charsPerLine = windowWidth / charWidth;

		if (charsPerLine < TARGET_FIT_WIDTH) {
			done = true;
		} else {
			fontSize += delta;
			p.setTextSize(fontSize);
		}

		while (!done) {
			charWidth = p.measureText("A");
			charsPerLine = windowWidth / charWidth;
			if (charsPerLine < TARGET_FIT_WIDTH) {
				done = true;
				fontSize -= delta;
			} else {
				fontSize += delta;
				p.setTextSize(fontSize);
			}
		}
		return (int) fontSize;
	}

	/** Build settings page routine. */
	@SuppressWarnings("deprecation")
	public void buildSettingsPage() {
		ConnectionWindowManager windowManager = context.getWindowManager();
		if (context.getConnectionSettings().getSettings().getWindows().size() < 1) {
			WindowToken token = new WindowToken(MAIN_WINDOW, null, null, context.getDisplay());
			RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			LayoutGroup g = new LayoutGroup();
			g.setType(LayoutGroup.LAYOUT_TYPE.normal);
			g.setLandscapeParams(p);
			g.setPortraitParams(p);
			windowManager.getWindows().add(0, token);
		} else {
			windowManager.getWindows().add(0, context.getConnectionSettings().getSettings().getWindows().get(MAIN_WINDOW));
		}

		context.getConnectionSettings().doBackgroundStartup();
		for (Plugin pl : context.getPlugins()) {
			pl.doBackgroundStartup();
		}

		context.getConnectionSettings().buildAliases();
		for (Plugin pl : context.getPlugins()) {
			pl.buildAliases();
		}

		windowManager.getWindows().get(0).getSettings().setListener(context.createWindowSettingsChangedListener(windowManager.getWindows().get(0).getName()));
		context.getConnectionSettings().getSettings().getOptions().addOptionAt(windowManager.getWindows().get(0).getSettings(), FOUR);
	}

	/** Starts the recursive settings initialization routine. */
	public void initSettings() {
		initSetting(context.getConnectionSettings().getSettings().getOptions());
	}

	/** Recursive settings initialization routine.
	 *
	 * @param s the SettingsGroup to dump.
	 */
	private void initSetting(final SettingsGroup s) {
		for (Option o : s.getOptions()) {
			if (o instanceof SettingsGroup) {
				initSetting((SettingsGroup) o);
			} else {
				BaseOption tmp = (BaseOption) o;
				context.updateSetting(o.getKey(), tmp.getValue().toString());
			}
		}
	}
}
