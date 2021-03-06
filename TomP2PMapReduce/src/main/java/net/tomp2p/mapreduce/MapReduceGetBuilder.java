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

import java.util.NavigableMap;
import java.util.TreeMap;

import net.tomp2p.dht.GetBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.storage.Data;

/**
 * Class similar to {@link GetBuilder}. Additionally provides the possibility to add broadcast input which will be
 * released again in case the requesting node fails (not tested in real situation yet!!). Created when
 * {@link PeerMapReduce#get()} is invoked.Do not create this externally but use the provided wrapper of
 * {@link PeerMapReduce}.
 * 
 * @author Oliver Zihler
 *
 */
public class MapReduceGetBuilder extends BaseMapReduceBuilder<MapReduceGetBuilder> {

	/**
	 * Broadcast input used to resent broadcast in case the node accessing the data using {@link PeerMapReduce#get()}
	 * fails.
	 */
	private NavigableMap<Number640, byte[]> broadcastInput;

	public MapReduceGetBuilder(PeerMapReduce peerMapReduce, Number160 locationKey, Number160 domainKey) {
		super(peerMapReduce, locationKey, domainKey);
		self(this);
	}

	public MapReduceGetBuilder broadcastInput(NavigableMap<Number640, Data> broadcastInput) {
		if (broadcastInput != null) {
			this.broadcastInput = convertDataToByteArray(broadcastInput);
		}
		return this;
	}

	public NavigableMap<Number640, byte[]> broadcastInput() {
		return broadcastInput;
	}

	public FutureMapReduceData start() {
		return new DistributedTask(peerMapReduce.peer().distributedRouting(), peerMapReduce.taskRPC()).getTaskData(this,
				super.start());
	}

	public static NavigableMap<Number640, byte[]> convertDataToByteArray(NavigableMap<Number640, Data> input) {
		NavigableMap<Number640, byte[]> convertedBroadcastInput = new TreeMap<>();
		for (Number640 key : input.keySet()) {
			convertedBroadcastInput.put(key, input.get(key).toBytes());
		}
		return convertedBroadcastInput;
	}

	public static NavigableMap<Number640, Data> reconvertByteArrayToData(NavigableMap<Number640, byte[]> input) {
		NavigableMap<Number640, Data> convertedBroadcastInput = new TreeMap<>();
		for (Number640 key : input.keySet()) {
			convertedBroadcastInput.put(key, new Data(input.get(key)));
		}
		return convertedBroadcastInput;
	}
}
