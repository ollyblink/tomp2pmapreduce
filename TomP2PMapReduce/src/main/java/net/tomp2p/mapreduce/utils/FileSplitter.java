/* 
 * Copyright 2016 Oliver Zihler 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.tomp2p.mapreduce.utils;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tomp2p.mapreduce.FutureMapReduceData;
import net.tomp2p.mapreduce.MapReducePutBuilder;
import net.tomp2p.mapreduce.PeerMapReduce;
import net.tomp2p.peers.Number160;

public class FileSplitter {
	private static Logger logger = LoggerFactory.getLogger(FileSplitter.class);

	/**
	 * Splits a text file located at keyFilePath into pieces of at max maxFileSize. Words are not split! Meaning, this method is appropriate for word count
	 * 
	 * @param keyfilePath
	 * @param dht
	 *            connection to put it into the dht
	 * @param maxFileSize
	 * @param fileEncoding
	 *            (e.g. UTF-8)
	 * @return a map containing all generated dht keys of the file splits to retrieve them together with the FuturePut to be called in Futures.whenAllSucess(...)
	 */
	public static Map<Number160, FutureMapReduceData> splitWithWordsAndWrite(String keyfilePath, PeerMapReduce pmr, int nrOfExecutions, Number160 domainKey, int maxFileSize, String fileEncoding) {
		Map<Number160, FutureMapReduceData> dataKeysAndFuturePuts = Collections.synchronizedMap(new HashMap<>());
		// System.err.println("Filepath: " + keyfilePath);
		try {
			RandomAccessFile aFile = new RandomAccessFile(keyfilePath, "r");
			FileChannel inChannel = aFile.getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(maxFileSize);
			// int filePartCounter = 0;
			String split = "";
			String actualData = "";
			String remaining = "";
//			Integer.parseInt(remaining);
			while (inChannel.read(buffer) > 0) {
				buffer.flip();
				// String all = "";
				// for (int i = 0; i < buffer.limit(); i++) {
				byte[] data = new byte[buffer.limit()];
				buffer.get(data);
				// }
				// System.out.println(all);
				split = new String(data);
				split = remaining += split;

				remaining = "";
				// System.out.println(all);
				// Assure that words are not split in parts by the buffer: only
				// take the split until the last occurrance of " " and then
				// append that to the first again

				if (split.getBytes(Charset.forName(fileEncoding)).length >= maxFileSize) {
					actualData = split.substring(0, split.lastIndexOf(" ")).trim();
					remaining = split.substring(split.lastIndexOf(" ") + 1, split.length()).trim();
				} else {
					actualData = split.trim();
					remaining = "";
				}
				// System.err.println("Put data: " + actualData + ", remaining data: " + remaining);
				Number160 dataKey = Number160.createHash(keyfilePath);
				MapReducePutBuilder put = pmr.put(dataKey, domainKey, actualData, nrOfExecutions);
//				logger.info("put[k[" + dataKey + "], d[" + domainKey + "]");
//				TestInformationGatherUtils
//				.addLogEntry("put[k[" + dataKey + "], d[" + domainKey + "]");
//				put.execId = "STARTTASK [" + dataKey.shortValue() + "]";

				FutureMapReduceData futureTask = put.start();

				dataKeysAndFuturePuts.put(dataKey, futureTask);

				buffer.clear();
				split = "";
				actualData = "";
			}
			inChannel.close();
			aFile.close();
		} catch (Exception e) {
			logger.warn("Exception on reading file at location: " + keyfilePath, e);
		}

		return dataKeysAndFuturePuts;
	}

}
