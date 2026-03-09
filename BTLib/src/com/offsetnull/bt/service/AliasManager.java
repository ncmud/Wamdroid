/*
 * Copyright (C) Dan Block 2013
 */
package com.offsetnull.bt.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.offsetnull.bt.alias.AliasData;
import com.offsetnull.bt.service.function.SpecialCommand;
import com.offsetnull.bt.service.plugin.Plugin;

/** Manages alias parsing, semicolon splitting, special command handling, and keyboard alias replacement. */
public class AliasManager {

	/** The context providing access to connection state. */
	private final AliasContext mContext;

	/** String builder used by the alias parsing routine. */
	private final StringBuffer mDataToServer = new StringBuffer();
	/** String builder used by the alias parsing routine. */
	private final StringBuffer mDataToWindow = new StringBuffer();
	/** Semicolon matching pattern. */
	private final Pattern mSemicolon = Pattern.compile(";");
	/** Semicolon matcher. */
	private final Matcher mSemiMatcher = mSemicolon.matcher("");
	/** String builder used by the alias parsing routine. */
	private final StringBuffer mCommandBuilder = new StringBuffer();
	/** The pattern for the .command. */
	private Pattern mCommandPattern = Pattern.compile("^.(\\w+)\\s*(.*)$");
	/** The matcher for the .command. */
	private Matcher mCommandMatcher = mCommandPattern.matcher("");
	/** The map of special commands. */
	private HashMap<String, SpecialCommand> mSpecialCommands = new HashMap<String, SpecialCommand>();
	/** The configurable character denoting that the input to follow should be executed as a script. */
	private static String mScriptBlock = "/";

	/** Constructor.
	 *
	 * @param context The alias context providing access to connection state.
	 */
	public AliasManager(final AliasContext context) {
		mContext = context;
	}

	/** Gets the special commands map.
	 *
	 * @return The special commands map.
	 */
	public HashMap<String, SpecialCommand> getSpecialCommands() {
		return mSpecialCommands;
	}

	/** Utility class for alias replacement and special command parsing routine. */
	public static class Data {
		/** The string to send to the server. */
		String mCmdString;
		/** The string to echo back to the input window. */
		String mVisString;
		/** Generic constructor. */
		public Data() {
			mCmdString = "";
			mVisString = "";
		}
		/** Cmd string getter.
		 *
		 * @return The string.
		 */
		public final String getCmdString() {
			return mCmdString;
		}
		/** Vis string getter.
		 *
		 * @return The string.
		 */
		public final String getVisString() {
			return mVisString;
		}
		/** Vis string setter.
		 *
		 * @param vis Desired string.
		 */
		public final void setVisString(final String vis) {
			this.mVisString = vis;
		}
		/** Cmd string setter.
		 *
		 * @param cmd Desired string.
		 */
		public final void setCmdString(final String cmd) {
			this.mCmdString = cmd;
		}
	}

	/** Alias parsing and special command handling routine.
	 *
	 * @param data The data on its way to the server in need of processing.
	 * @param connection The connection instance needed by special commands.
	 * @return A Data object containing the string for the server and the string to the window.
	 * @throws UnsupportedEncodingException Problem with the String<==>byte[] conversion indicating a bad encoding option.
	 */
	public Data processOutputData(final String data, final Connection connection) throws UnsupportedEncodingException {
		mDataToServer.setLength(0);
		mDataToWindow.setLength(0);
		String out = data;
		if (out.endsWith("\n")) {
			out = out.substring(0, out.length() - 2);
		}

		if (out.equals("")) {
			Data enter = new Data();
			enter.mCmdString = "";
			enter.mVisString = null;
			return enter;
		}

		if (out.equals(";;")) {
			Data enter = new Data();
			enter.mCmdString = ";" + mContext.getCRLF();
			enter.mVisString = ";";
			return enter;
		}
		List<String> list = null;

		if (mContext.getConnectionSettings().isSemiIsNewLine()) {
			//commands = semicolon.split(out);
			list = splitSemicolonSafe(out);

		} else {
			list = new ArrayList<String>();
			list.add(out);
		}
		StringBuffer holdover = new StringBuffer();

		ListIterator<String> iterator = list.listIterator();
		while (iterator.hasNext()) {
			String cmd = iterator.next();

			if (cmd.endsWith("~")) {
				holdover.append(cmd.substring(0, cmd.length() - 1) + ";");
			} else {
				if (holdover.length() > 0) {
					cmd = holdover.toString() + cmd;
					holdover.setLength(0);
				}
				//2.5 run command through the global lua state
				Data d = null;

				if (cmd.startsWith(mScriptBlock)) {
					mContext.getConnectionSettings().runLuaString(cmd.substring(mScriptBlock.length(), cmd.length()));
				} else {
					d = processCommand(cmd, connection);
				}
				//3 - do special command processing.

				//4 - handle command processing output

				if (d != null) {
					boolean m = false;
					if (d.mCmdString != null && d.mVisString != null) {
						if (d.mCmdString.equals(d.mVisString)) {
							m = true; //aliases & regular commands will always have the same cmdString and visString
						}
					}

					//5 - alias replacement
					if (d.mCmdString != null && !d.mCmdString.equals("")) {
						boolean didReplace = false;
						byte[] tmp = null;
						ArrayList<Plugin> plugins = mContext.getPlugins();
						for (int i = 0; i < plugins.size() + 1; i++) {
							Plugin p = null;
							if (i == 0) {
								p = mContext.getConnectionSettings();
							} else {
								p = plugins.get(i - 1);
							}
							if (p.getSettings().getAliases().size() > 0) {
								Boolean reprocess = true;
								tmp = p.doAliasReplacement(d.mCmdString.getBytes(mContext.getConnectionSettings().getEncoding()), reprocess);
								String tmpstr = new String(tmp, mContext.getConnectionSettings().getEncoding());
								if (!d.mCmdString.equals(tmpstr)) {
									//alias replaced, needs to be processed

									List<String> aliasCommands = null;
									if (mContext.getConnectionSettings().isSemiIsNewLine()) {
										aliasCommands = splitSemicolonSafe(tmpstr);
									} else {
										aliasCommands = new ArrayList<String>(1);
										aliasCommands.add(tmpstr);
									}
									for (String acmd : aliasCommands) {
										iterator.add(acmd);
									}
									if (reprocess) {
										for (int ax = 0; ax < aliasCommands.size(); ax++) {
											iterator.previous();
										}
									}
									didReplace = true;
									i = plugins.size();
								}
							}
						}

						if (!didReplace) {
							if (tmp != null) {
								if (m) {
									String srv = new String(tmp, mContext.getConnectionSettings().getEncoding()) + mContext.getCRLF();
									mDataToServer.append(new String(srv));
									mDataToWindow.append(new String(tmp, mContext.getConnectionSettings().getEncoding()) + ";");
								} else {
									String srv = new String(tmp, mContext.getConnectionSettings().getEncoding()) + mContext.getCRLF();
									mDataToServer.append(new String(srv));
								}
							} else {
								mDataToServer.append(d.mCmdString + mContext.getCRLF());
								mDataToWindow.append(d.mCmdString);
							}
						}

					}

						//dataToServer.append(d.cmdString + crlf);
					if (d.mVisString != null && !d.mVisString.equals("")) {
						if (!m) {
							mDataToWindow.append(d.mVisString + ";");
						}
					}
				}


			}
		}
		//7 - return Data packet with commands to send to server, and data to send to window.
		Data d = new Data();
		d.mCmdString = mDataToServer.toString();
		d.mVisString = mDataToWindow.toString();

		if (d.mVisString.endsWith(";")) {
			d.mVisString = d.mVisString.substring(0, d.mVisString.length() - 1);
		}
		if (!d.mVisString.endsWith(mContext.getCRLF())) {
			d.mVisString = d.mVisString + mContext.getCRLF();
		}
		return d;
	}

	/** Semicolon splitting routine that looks for ;; smartly.
	 *
	 * @param string The string to process.
	 * @return The resulting list of strings.
	 */
	List<String> splitSemicolonSafe(final String string) {
		List<String> list = new ArrayList<String>();
		mSemiMatcher.reset(string);
		boolean matched = false;
		boolean append = false;
		boolean firstSemi = true;
		//int lastLength = -1;
		while (mSemiMatcher.find()) {
			matched = true;
			mCommandBuilder.setLength(0);

			mSemiMatcher.appendReplacement(mCommandBuilder, "");
			if (mCommandBuilder.length() == 0) {
				append = true;
				if (list.size() == 0) {
					if (!firstSemi) {
						list.add(";");
					} else {
						firstSemi = false; //don't add the first one, but add subsequent ones.
					}
				} else {
					list.add(list.remove(list.size() - 1) + ";");
				}
			} else {
				if (append) {
					if (list.size() == 0) {
						list.add(";");
					} else {
						list.add(list.remove(list.size() - 1) + mCommandBuilder.toString());
					}
					append = false;
				} else {
					list.add(mCommandBuilder.toString());
				}

			}
		}

		if (!matched) {
			list.add(string);
		} else {
			mCommandBuilder.setLength(0);
			mSemiMatcher.appendTail(mCommandBuilder);
			if (append) {
				if(list.size() != 0) {
					list.add(list.remove(list.size() - 1) + mCommandBuilder.toString());
				}
			} else {
				list.add(mCommandBuilder.toString());
			}
		}

		mCommandBuilder.setLength(0);
		return list;
	}

	/** Generic command processor. This looks for "." commands.
	 *
	 * @param cmd The input string to parse.
	 * @param connection The connection instance needed by special commands.
	 * @return The Data object containing the string to return to the server and the string to return to the window.
	 */
	public final Data processCommand(final String cmd, final Connection connection) {
		Data data = new Data();
		if (cmd.equals(".." + "\n") || cmd.equals("..")) {
			synchronized (mContext.getConnectionSettings()) {
				String outputmsg = "\n" + Colorizer.getRedColor() + "Dot command processing ";
				if (mContext.getConnectionSettings().isProcessPeriod()) {
					overrideProcessPeriods(false);
					outputmsg = outputmsg.concat("disabled.");
				} else {
					overrideProcessPeriods(true);
					outputmsg = outputmsg.concat("enabled.");
				}
				outputmsg = outputmsg.concat(Colorizer.getWhiteColor() + "\n");
				try {
					mContext.sendBytesToWindow(outputmsg.getBytes(mContext.getConnectionSettings().getEncoding()));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}

			return null;
		}


		if (cmd.startsWith(".") && mContext.getConnectionSettings().isProcessPeriod()) {

			if (cmd.startsWith("..")) {
				data.mCmdString = cmd.replace("..", ".");
				data.mVisString = cmd.replace("..", ".");
				return data;
			}


			mCommandMatcher.reset(cmd);
			if (mCommandMatcher.find()) {
				synchronized (mContext.getConnectionSettings()) {

					//string should be of the form .aliasname |settarget can have whitespace|

						String alias = mCommandMatcher.group(1);
						String argument = mCommandMatcher.group(2);


						if (mContext.getConnectionSettings().getSettings().getAliases().containsKey(alias)) {
							//real argument
							if (!argument.equals("")) {
								AliasData mod = mContext.getConnectionSettings().getSettings().getAliases().remove(alias);
								mod.setPost(argument);
								mContext.getConnectionSettings().getSettings().getAliases().put(alias, mod);
								data.mCmdString = "";
								if (mContext.getConnectionSettings().isEchoAliasUpdates()) {
									data.mVisString = "[" + alias + "=>" + argument + "]";
								} else {
									data.mVisString = "";
								}
								return data;
							} else {
								//display error message
								String noargMessage = "\n" + Colorizer.getRedColor() + " Alias \"" + alias + "\" can not be set to nothing. Acceptable format is \"."
													+ alias + " replacetext\"" + Colorizer.getWhiteColor() + "\n";
								try {
									mContext.sendBytesToWindow(noargMessage.getBytes(mContext.getConnectionSettings().getEncoding()));
								} catch (UnsupportedEncodingException e) {
									throw new RuntimeException(e);
								}
								return null;
							}
						} else if (mSpecialCommands.containsKey(alias)) {
							//Log.e("SERVICE","SERVICE FOUND SPECIAL COMMAND: " + alias);
							SpecialCommand command = mSpecialCommands.get(alias);
							data = (Data) command.execute(argument, connection);
							return data;
						} else {
							//format error message.

							String error = Colorizer.getRedColor() + "[*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*]\n";
							error += "  \"" + alias + "\" is not a recognized alias or command.\n";
							error += "   No data has been sent to the server. If you intended\n";
							error += "   this to be done, please type \".." + alias + "\"\n";
							error += "   To toggle command processing, input \"..\" with no arguments\n";
							error += "[*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*][*]" + Colorizer.getWhiteColor() + "\n";

							try {
								mContext.sendBytesToWindow(error.getBytes(mContext.getConnectionSettings().getEncoding()));
							} catch (UnsupportedEncodingException e) {
								throw new RuntimeException(e);
							}
							return null;
						}
					}
			}
			return data;
		} else {
			data.mCmdString = cmd;
			data.mVisString = cmd;
			return data;
		}

	}

	/** Overrides the process special commands setting and sets a new value.
	 *
	 * @param value The new value for the process periods command.
	 */
	private void overrideProcessPeriods(final boolean value) {
		synchronized (mContext.getConnectionSettings()) { //not sure why this is here.
			mContext.getConnectionSettings().setProcessPeriod(value);
		}
	}

	/** Helper function for the keyboard command. Does an alias replacement in a special kind of way.
	 *
	 * @param bytes Bytes to process.
	 * @param reprocess Weather to do recursive alias replacement.
	 * @return The processed command bytes.
	 */
	public final byte[] doKeyboardAliasReplace(final byte[] bytes, final Boolean reprocess) {
		ArrayList<Plugin> plugins = mContext.getPlugins();
		int count = plugins.size();
		for (int i = 0; i < count; i++) {
			Plugin p = plugins.get(i);
			byte[] tmp = p.doAliasReplacement(bytes, reprocess);
			if (tmp.length != bytes.length) {
				return tmp;
			} else {
				boolean same = true;
				for (int j = 0; j < tmp.length; j++) {
					if (tmp[j] != bytes[j]) {
						same = false;
						j = tmp.length;
					}
				}
				if (!same) {
					return tmp;
				}
			}
		}

		return bytes;
	}
}
