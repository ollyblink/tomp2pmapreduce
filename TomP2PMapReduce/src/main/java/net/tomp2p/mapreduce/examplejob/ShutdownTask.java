package net.tomp2p.mapreduce.examplejob;

import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.mapreduce.Job;
import net.tomp2p.mapreduce.PeerMapReduce;
import net.tomp2p.mapreduce.Task;
import net.tomp2p.mapreduce.utils.JobTransferObject;
import net.tomp2p.mapreduce.utils.NumberUtils;
import net.tomp2p.mapreduce.utils.ReadLog;
import net.tomp2p.mapreduce.utils.TestInformationGatherUtils;
import net.tomp2p.peers.Number640;
import net.tomp2p.storage.Data;

public class ShutdownTask extends Task {
	private static int counter = 0;

	private static Logger logger = LoggerFactory.getLogger(ShutdownTask.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = -5543401293112052880L;
	private static int DEFAULT_SLEEPING_TIME_REPS = 15;
	private static long DEFAULT_SLEEPING_TIME = 1000;

	private long sleepingTime = DEFAULT_SLEEPING_TIME;
	private int sleepingTimeReps = DEFAULT_SLEEPING_TIME_REPS;
	private int retrievalCounter = 0;
	private int nrOfParticipatingPeers;
	public AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

	private long nrOfNodes;

	public ShutdownTask(Number640 previousId, Number640 currentId, int nrOfParticipatingPeers, int sleepingTimeReps, long sleepingTime, long nrOfNodes) {
		super(previousId, currentId);
		this.nrOfNodes = nrOfNodes;
		this.nrOfParticipatingPeers = nrOfParticipatingPeers;
		this.sleepingTimeReps = sleepingTimeReps;
		this.sleepingTime = sleepingTime;
	}

	public ShutdownTask(Number640 previousId, Number640 currentId, int nrOfParticipatingPeers, long nrOfNodes) {
		this(previousId, currentId, nrOfParticipatingPeers, DEFAULT_SLEEPING_TIME_REPS, DEFAULT_SLEEPING_TIME, nrOfNodes);
	}

	@Override
	public void broadcastReceiver(NavigableMap<Number640, Data> input, PeerMapReduce pmr) throws Exception {
		startTaskCounter.incrementAndGet();

		int execID = counter++;
		TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> START EXECUTING SHUTDOWNTASK [" + execID + "]");
		if (!input.containsKey(NumberUtils.OUTPUT_STORAGE_KEY)) {
			logger.info("Received shutdown but not for the printing task. Ignored");
			TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> RETURNED EXECUTING SHUTDOWNTASK [" + execID + "]");
			return;
		}
		logger.info("Received REAL shutdown from ACTUAL PRINTING TASK. shutdown initiated.");

		if (shutdownInitiated.get()) {
			logger.info("Shutdown already initiated. ignored");
			TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> RETURNED EXECUTING SHUTDOWNTASK [" + execID + "]");
			return;
		}
		++retrievalCounter;
		logger.info("Retrieval counter: " + retrievalCounter + ", (" + retrievalCounter + " >= " + nrOfParticipatingPeers + ")? " + (retrievalCounter >= nrOfParticipatingPeers));
		if (retrievalCounter >= nrOfParticipatingPeers) {
			shutdownInitiated.set(true);
			logger.info("Received shutdown message. Counter is: " + retrievalCounter + ": SHUTDOWN IN 5 SECONDS");
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub

					int cnt = 0;
					while (cnt < sleepingTimeReps) {
						logger.info("[" + (cnt++) + "/" + sleepingTimeReps + "] times slept for " + (sleepingTime / 1000) + "s");
						try {
							Thread.sleep(sleepingTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					finishedTaskCounter.incrementAndGet();

					List<String> taskDetails = pmr.broadcastHandler().shutdown();
					TestInformationGatherUtils.addLogEntry(">>>>>>>>>>>>>>>>>>>> FINISHED EXECUTING SHUTDOWNTASK [" + execID + "]");
					try {
						Data jobData = input.get(NumberUtils.JOB_DATA);
						if (jobData != null) {
							JobTransferObject serializedJob = ((JobTransferObject) jobData.object());
							Job job = Job.deserialize(serializedJob);
							TestInformationGatherUtils.writeOut(job.id().longValue() + "_peer[" + pmr.peer().peerID().intValue() + "]_pcs[" + nrOfNodes + "]", taskDetails);

						} else {
							TestInformationGatherUtils.writeOut("NO_ID_peer[" + pmr.peer().peerID().intValue() + "]_pcs[" + nrOfNodes + "]", taskDetails);

						}
					} catch (Exception e) {

					}
					// ReadLog.main(null);

					try {
						pmr.peer().shutdown().await().addListener(new BaseFutureAdapter<BaseFuture>() {

							@Override
							public void operationComplete(BaseFuture future) throws Exception {
								// TODO Auto-generated method stub
								if (future.isSuccess()) {
									logger.info("Success on shutdown peer [" + pmr.peer().peerID().shortValue() + "]");
								} else {
									logger.info("NOOO SUCCEESSS on shutdown peer [" + pmr.peer().peerID().shortValue() + "], reason: " + future.failedReason());
								}
								System.exit(0);
							}
						});
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					logger.info("Shutdown peer.");

				}
			}).start();
		} else {
			logger.info("RetrievalCounter is only: " + retrievalCounter);
		}
	}

}