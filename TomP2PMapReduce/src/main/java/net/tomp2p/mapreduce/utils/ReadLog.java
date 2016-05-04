package net.tomp2p.mapreduce.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReadLog {
	public static void main(String[] args) {
		// readLog();
		// readLog2();
	}

	public static void readLog(BufferedWriter writer) {
		Charset charset = Charset.forName("UTF-8");
		ArrayList<String> logLines = FileUtils.INSTANCE.readLinesFromFile(new File("").getAbsolutePath() + "/p2p.log", charset);
		Map<String, Map<String, List<Long>>> msgCounter = Collections.synchronizedMap(new TreeMap<>());
		// for (int i = 0; i < logLines.size(); ++i) {
		// String line = logLines.get(i);
		// if (line.contains("DEBUG net.tomp2p.message.Decoder - Decoding of TomP2P starts now. Readable:")) {
		// long size = Integer.parseInt(line.substring(line.lastIndexOf("Readable: ") + "Readable: ".length(), line.lastIndexOf(".")));
		// String threadPart = line.substring(line.indexOf("[NETTY-TOMP2P - worker-client/server - -1-"), line.indexOf("DEBUG"));
		// // System.err.println("Size: " + size);
		for (int j = 0; j < logLines.size(); ++j) {
			String nextLine = logLines.get(j);
			if (nextLine.contains("DEBUG net.tomp2p.message.Decoder") && nextLine.contains("About to")) {
				String startOfData = nextLine.substring(nextLine.indexOf("msgid="), nextLine.length());
				String[] cnt = startOfData.split(",");
				// System.err.println(startOfData);
				String requestType = "";
				String c = "";
				long size = Long.parseLong(nextLine.substring(nextLine.lastIndexOf("Buffer to read: ") + "Buffer to read: ".length(), nextLine.lastIndexOf(".")));
				String msgId = "";
				for (String s : cnt) {
					if (s.contains("msgid=")) {
						msgId = s.replace("msgid=", "");
					}

					if (s.contains("t=")) {
						// System.err.println();
						requestType = s.replace("t=", "");

					}
					if (s.contains("c=")) {
						c = s.replace("c=", "");
					}
					// if (s.contains("s=")) {
					// System.err.println("s:" + s.replace("s=", ""));
					// }
					if (requestType.length() > 0 && c.length() > 0 && msgId.length() > 0) {
						String concat = requestType + "_" + c;
						// if (requestType.equals("REQUEST_1") && c.equals("GCM")) {
						// concat = "PUT_REQUEST";
						// }
						// if (requestType.equals("REQUEST_2") && c.equals("GCM")) {
						// concat = "GET_REQUEST";
						// }
						Map<String, List<Long>> map = msgCounter.get(concat);
						if (map == null) {
							map = Collections.synchronizedMap(new HashMap<>());
							msgCounter.put(concat, map);
						}
						List<Long> vals = map.get(msgId);
						if (vals == null) {
							vals = Collections.synchronizedList(new ArrayList<>());
							map.put(msgId, vals);
						}
						vals.add(size);
						c = "";
						requestType = "";
						msgId = "";
					}
				}
				// System.err.println();
				// int msgId = Integer.parseInt(nextLine.substring(nextLine.indexOf("msgid=") + "msgid=", nextLine.indexOf("msgid=") + "msgid="));
				// break;
				// }
				// }
			}
		}

		// Map<String, List<Long>> tree = Collections.synchronizedMap();

		try {
			writer.write("==================READLOG.READLOG1()====================\n");
			long overallSize = 0;
			for (String requestType : msgCounter.keySet()) {
				long requestSize = 0;
				Map<String, List<Long>> map = msgCounter.get(requestType);
				for (String msgId : map.keySet()) {
					List<Long> list = map.get(msgId);
					long sum = 0;
					for (Long l : list) {
						sum += l;
					}
					overallSize += sum;
					requestSize += sum;
				}
				System.err.println(requestType + ": #msgs[" + map.keySet().size() + "], overall size[" + (requestSize / (1024d * 1024d)) + "]MB");

				writer.write(requestType + ": #msgs[" + map.keySet().size() + "], overall size[" + (requestSize / (1024d * 1024d)) + "]MB\n");

			}
			System.err.println("Overall size: [" + (overallSize / (1024d * 1024d)) + "]MB");
			writer.write("Overall size: [" + (overallSize / (1024d * 1024d)) + "]MB\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void readLog2(BufferedWriter writer) {
		Charset charset = Charset.forName("UTF-8");
		ArrayList<String> logLines = FileUtils.INSTANCE.readLinesFromFile(new File("").getAbsolutePath() + "/p2p.log", charset);
		Map<String, Map<String, List<Long>>> msgCounter = Collections.synchronizedMap(new TreeMap<>());
		for (int i = 0; i < logLines.size(); ++i) {
			String line = logLines.get(i);
			if (line.contains("DEBUG net.tomp2p.message.Decoder - Decoding of TomP2P starts now. Readable:")) {
				long size = Integer.parseInt(line.substring(line.lastIndexOf("Readable: ") + "Readable: ".length(), line.lastIndexOf(".")));
				String threadPart = line.substring(line.indexOf("[NETTY-TOMP2P - worker-client/server - -1-"), line.indexOf("DEBUG"));
				// System.err.println("Size: " + size);
				for (int j = i + 1; j < logLines.size(); ++j) {
					String nextLine = logLines.get(j);
					if (nextLine.contains("DEBUG net.tomp2p.message.Decoder") && nextLine.contains(threadPart)) {
						String startOfData = nextLine.substring(nextLine.indexOf("msgid="), nextLine.length());
						String[] cnt = startOfData.split(",");
						// System.err.println(startOfData);
						String requestType = "";
						String c = "";
						String msgId = "";
						for (String s : cnt) {
							if (s.contains("msgid=")) {
								msgId = s.replace("msgid=", "");
							}

							if (s.contains("t=")) {
								// System.err.println();
								requestType = s.replace("t=", "");

							}
							if (s.contains("c=")) {
								c = s.replace("c=", "");
							}
							// if (s.contains("s=")) {
							// System.err.println("s:" + s.replace("s=", ""));
							// }
							if (requestType.length() > 0 && c.length() > 0 && msgId.length() > 0) {
								String concat = requestType + "_" + c;
								// if (requestType.equals("REQUEST_1") && c.equals("GCM")) {
								// concat = "PUT_REQUEST";
								// }
								// if (requestType.equals("REQUEST_2") && c.equals("GCM")) {
								// concat = "GET_REQUEST";
								// }
								Map<String, List<Long>> map = msgCounter.get(concat);
								if (map == null) {
									map = Collections.synchronizedMap(new HashMap<>());
									msgCounter.put(concat, map);
								}
								List<Long> vals = map.get(msgId);
								if (vals == null) {
									vals = Collections.synchronizedList(new ArrayList<>());
									map.put(msgId, vals);
								}
								vals.add(size);
								c = "";
								requestType = "";
								msgId = "";
							}
						}
						// System.err.println();
						// int msgId = Integer.parseInt(nextLine.substring(nextLine.indexOf("msgid=") + "msgid=", nextLine.indexOf("msgid=") + "msgid="));
						break;
					}
				}
			}
		}

		// Map<String, List<Long>> tree = Collections.synchronizedMap();
		try {
			writer.write("==================READLOG.READLOG2()====================\n");

			long overallSize = 0;
			for (String requestType : msgCounter.keySet()) {
				long requestSize = 0;
				Map<String, List<Long>> map = msgCounter.get(requestType);
				for (String msgId : map.keySet()) {
					List<Long> list = map.get(msgId);
					long sum = 0;
					for (Long l : list) {
						sum += l;
					}
					overallSize += sum;
					requestSize += sum;
				}
				System.err.println(requestType + ": #msgs[" + map.keySet().size() + "], overall size[" + (requestSize / (1024d * 1024d)) + "]MB");

				writer.write(requestType + ": #msgs[" + map.keySet().size() + "], overall size[" + (requestSize / (1024d * 1024d)) + "]MB\n");

			}
			System.err.println("Overall size: [" + (overallSize / (1024d * 1024d)) + "]MB");
			writer.write("Overall size: [" + (overallSize / (1024d * 1024d)) + "]MB\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}