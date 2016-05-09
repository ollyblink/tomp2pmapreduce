/* 
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
package net.tomp2p.mapreduce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import net.tomp2p.connection.ChannelCreator;
import net.tomp2p.dht.EvaluatingSchemeDHT;
import net.tomp2p.dht.VotingSchemeDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureDone;
import net.tomp2p.futures.FutureForkJoin;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.futures.FutureRouting;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

/**
 * Future for retrieving data for a {@link Task}
 * 
 * @author Oliver Zihler
 *
 */
public class FutureMapReduceData extends FutureDone<Void> {

	private final List<FutureResponse> requests = new ArrayList<FutureResponse>(6);
	private FutureRouting futureRouting;

	protected FutureDone<Void> futuresCompleted;
	private Map<PeerAddress, Map<Number640, Data>> rawData;
	private EvaluatingSchemeDHT evaluationScheme = new VotingSchemeDHT();

	public FutureMapReduceData() {
		self(this);
	}

	/**
	 * Adds all requests that have been created for the DHT operations. Those were created after the routing process.
	 * 
	 * @param futureResponse
	 *            The futureResponse that has been created
	 */
	public FutureDone<Void> addRequests(final FutureResponse futureResponse) {
		synchronized (lock) {
			requests.add(futureResponse);
		}
		return self();
	}

	public List<FutureResponse> requests() {
		synchronized (lock) {
			return requests;
		}
	}

	public void done(final FutureDone<Void> futuresCompleted) {
		synchronized (lock) {
			if (!completedAndNotify()) {
				return;
			}
			this.futuresCompleted = futuresCompleted;
			this.type = FutureType.OK;
			this.reason = "ok";
		}
		notifyListeners();
	}

	/**
	 * Adds a listener to the response future and releases all acquired channels in channel creator.
	 * 
	 * @param channelCreator
	 *            The channel creator that will be shutdown and all connections will be closed
	 */
	public void addFutureDHTReleaseListener(final ChannelCreator channelCreator) {
		addListener(new BaseFutureAdapter<FutureMapReduceData>() {
			@Override
			public void operationComplete(final FutureMapReduceData future) throws Exception {
				futureRequests().addListener(new BaseFutureAdapter<FutureForkJoin<FutureResponse>>() {
					@Override
					public void operationComplete(FutureForkJoin<FutureResponse> future) throws Exception {
						channelCreator.shutdown();
					}
				});
			}
		});
	}

	/**
	 * Sets the future object that was used for the routing. Before the FutureDHT is used, FutureRouting has to be completed successfully.
	 * 
	 * @param futureRouting
	 *            The future object to set
	 */
	public void futureRouting(final FutureRouting futureRouting) {
		synchronized (lock) {
			this.futureRouting = futureRouting;
		}
	}

	public FutureDone<Void> futuresCompleted() {
		synchronized (lock) {
			return futuresCompleted;
		}
	}

	/**
	 * Returns the future object that was used for the routing. Before the FutureDHT is used, FutureRouting has to be completed successfully.
	 * 
	 * @return The future object during the previous routing, or null if routing failed completely.
	 */
	public FutureRouting futureRouting() {
		synchronized (lock) {
			return futureRouting;
		}
	}

	/**
	 * Returns back those futures that are still running. If 6 storage futures are started at the same time and 5 of them finish, and we specified that we are fine if 5 finishes, then futureDHT
	 * returns success. However, the future that may still be running is the one that stores the content to the closest peer. For testing this is not acceptable, thus after waiting for futureDHT, one
	 * needs to wait for the running futures as well.
	 * 
	 * @return A future that finishes if all running futures are finished.
	 */
	public FutureForkJoin<FutureResponse> futureRequests() {
		synchronized (lock) {
			final int size = requests.size();
			final FutureResponse[] futureResponses = new FutureResponse[size];

			for (int i = 0; i < size; i++) {
				futureResponses[i] = requests.get(i);
			}
			return new FutureForkJoin<FutureResponse>(new AtomicReferenceArray<FutureResponse>(futureResponses));
		}
	}

	// public void receivedData(Map<PeerAddress, Map<Number640, Byte>> rawData) {
	/**
	 * Finish the future and set the keys and data that have been received.
	 * 
	 * @param rawData
	 *            The keys and data that have been received with information from which peer it has been received.
	 * @param rawDigest
	 *            The hashes of the content stored with information from which peer it has been received.
	 * @param rawStatus
	 * @param futuresCompleted
	 */
	public void receivedData(Map<PeerAddress, Map<Number640, Data>> rawData, FutureDone<Void> futuresCompleted) {
		synchronized (lock) {
			if (!completedAndNotify()) {
				return;
			}
			this.rawData = rawData;
			this.futuresCompleted = futuresCompleted;
			final int size = rawData.size();
			this.type = size > 0 ? FutureType.OK : FutureType.FAILED;
			this.reason = size > 0 ? "Minimum number of answers reached" : "Expected >0 answers, but got " + size;
		}
		notifyListeners();
	}
	// }

	/**
	 * Return the data from get() after evaluation. The evaluation gets rid of the PeerAddress information, by either a majority vote or cumulation.
	 * 
	 * @return The evaluated data that have been received.
	 */
	public Map<Number640, Data> dataMap() {
		synchronized (lock) {
			return evaluationScheme.evaluate2(rawData);
		}
	}

	/**
	 * @return The first data object from get() after evaluation.
	 */
	public Data data() {
		Map<Number640, Data> dataMap = dataMap();
		if (dataMap.size() == 0) {
			return null;
		}
		return dataMap.values().iterator().next();
	}
}
