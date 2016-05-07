/* 
 * Copyright 2016 Oliver Zihler 
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
package net.tomp2p.mapreduce.evaljob;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.mapreduce.FutureMapReduceData;
import net.tomp2p.mapreduce.PeerMapReduce;
import net.tomp2p.mapreduce.Task;
import net.tomp2p.mapreduce.utils.InputUtils;
import net.tomp2p.mapreduce.utils.NumberUtils;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.storage.Data;

public class PrintTaskEval extends Task {

	private static int counter = 0;

	private static Logger logger = LoggerFactory.getLogger(PrintTaskEval.class);
	private static AtomicBoolean finished = new AtomicBoolean(false);
	private static AtomicBoolean isBeingExecuted = new AtomicBoolean(false);

	public PrintTaskEval(Number640 previousId, Number640 currentId) {
		super(previousId, currentId);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8206142810699508919L;

	@Override
	public void broadcastReceiver(NavigableMap<Number640, Data> input, PeerMapReduce pmr) throws Exception {
//		startTaskCounter.incrementAndGet();

		int execID = counter++;
		if (pmr.peer().peerID().intValue() != 1 && pmr.peer().peerID().intValue() != 2) {
			System.err.println("PRINTTASK Returning for senderID: " + pmr.peer().peerID().intValue());
			return; // I do this that only two request can be made to the data. Therefore, only two results will be printed on id's 1 and 3
		}
//		TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> START EXECUTING PRINTTASK [" + execID + "] for job [" + input.get(NumberUtils.JOB_ID) + "]");
		if (finished.get() || isBeingExecuted.get()) {
			logger.info("Already executed/Executing reduce results >> ignore call");
//			TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> RETURNED EXECUTING PRINTTASK [" + execID + "]");

			return;
		}

		isBeingExecuted.set(true);
		Data storageKeyData = input.get(NumberUtils.OUTPUT_STORAGE_KEY);
		if (storageKeyData != null) {
			Number640 storageKey = (Number640) storageKeyData.object();
			pmr.get(storageKey.locationKey(), storageKey.domainKey(), new TreeMap<>()/*input*/).start().addListener(new BaseFutureAdapter<FutureMapReduceData>() {

				@Override
				public void operationComplete(FutureMapReduceData future) throws Exception {
					if (future.isSuccess()) {
						Map<String, Integer> reduceResults = new TreeMap<>((Map<String, Integer>) future.data().object());
						// logger.info("==========WORDCOUNT RESULTS OF PEER WITH ID: " + pmr.peer().peerID().intValue() + ", time [" + time + "]==========");
						// logger.info("=====================================");
						// for (String word : reduceResults.keySet()) {
						// logger.info(word + " " + reduceResults.get(word));
						// }
						// logger.info("=====================================");
						String filename = "temp_[" + reduceResults.keySet().size() + "]words_[" + DateFormat.getDateTimeInstance().format(new Date()) + "]";
						filename = filename.replace(":", "_").replace(",", "_").replace(" ", "_");
						printResults(filename, reduceResults, pmr.peer().peerID().intValue());
						NavigableMap<Number640, Data> newInput = new TreeMap<>();
						InputUtils.keepInputKeyValuePairs(input, newInput, new String[] { "JOB_KEY", "INPUTTASKID", "MAPTASKID", "REDUCETASKID", "WRITETASKID", "SHUTDOWNTASKID", "RECEIVERS" });
						newInput.put(NumberUtils.CURRENT_TASK, input.get(NumberUtils.allSameKeys("WRITETASKID")));
						newInput.put(NumberUtils.NEXT_TASK, input.get(NumberUtils.allSameKeys("SHUTDOWNTASKID")));
						newInput.put(NumberUtils.INPUT_STORAGE_KEY, input.get(NumberUtils.OUTPUT_STORAGE_KEY));
						// Number640 o = new Number640(new Random());
						// newInput.put(NumberUtils.OUTPUT_STORAGE_KEY, input.get(NumberUtils.OUTPUT_STORAGE_KEY)); //Below replaces this because bc handler has a set with msgs and would not execute it if the outputkey was the same (see bchandler)
						newInput.put(NumberUtils.OUTPUT_STORAGE_KEY, new Data(new Number640(new Random())));

						newInput.put(NumberUtils.SENDER, new Data(pmr.peer().peerAddress()));
						// newInput.put(NumberUtils.SENDER, new Data(pmr.peer().peerAddress()));
						finished.set(true);
						logger.info(">>>>>>>>>>>>>>>>>>>> FINISHED EXECUTING PRINTTASK [" + execID + "] with [" + reduceResults.keySet().size() + "] words");
//						TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> FINISHED EXECUTING PRINTTASK [" + execID + "] with [" + reduceResults.keySet().size() + "] words");
						// TestInformationGatherUtils.writeOut();
						pmr.peer().broadcast(new Number160(new Random())).dataMap(newInput).start();
//						finishedTaskCounter.incrementAndGet();

					} else {
						// Do nothing
					}
				}

			});
		} else {
			logger.info("Ignored");
		}

	}

	public static void printResults(String filename, Map<String, Integer> reduceResults, int peerId) throws Exception {
		// System.err.println("FILENAME: "+ filename);
		File f = new File(filename);
		if (f.exists()) {
			f.delete();
		}
		f.createNewFile();

		Path file = Paths.get(filename);
		try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.defaultCharset(), StandardOpenOption.APPEND)) {
			writer.write("==========WORDCOUNT RESULTS OF PEER WITH ID: " + peerId + ", #words [" + reduceResults.keySet().size() + "] time [" + DateFormat.getDateTimeInstance().format(new Date()) + "]==========");
			writer.newLine();

			for (String word : reduceResults.keySet()) {
				writer.write(word + ", " + reduceResults.get(word));
				writer.newLine();
			}
			writer.write("=====================================");
			writer.newLine();
			writer.close();
		}
	}
}