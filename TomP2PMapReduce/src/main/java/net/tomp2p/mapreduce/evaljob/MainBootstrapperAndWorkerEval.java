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
package net.tomp2p.mapreduce.evaljob;

import java.net.InetAddress;

import net.tomp2p.connection.ConnectionBean;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.mapreduce.PeerConnectionCloseListener;
import net.tomp2p.mapreduce.PeerMapReduce;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapConfiguration;

public class MainBootstrapperAndWorkerEval {

	private static final int waitingTime = 5000; 
	// private static int peerCounter = new Random().nextInt();
	private static int peerCounter = 2;

	public static void main(String[] args) throws Exception {
		// String myIP = "192.168.";

		ConnectionBean.DEFAULT_SLOW_RESPONSE_TIMEOUT_SECONDS = Integer.MAX_VALUE;
		ConnectionBean.DEFAULT_TCP_IDLE_MILLIS = Integer.MAX_VALUE;
		ConnectionBean.DEFAULT_CONNECTION_TIMEOUT_TCP = Integer.MAX_VALUE;
		// ConnectionBean.DEFAULT_UDP_IDLE_MILLIS = Integer.MAX_VALUE;
		// ChannelServerConfiguration c;
		// int nrOfFiles = 5;
		PeerConnectionCloseListener.WAITING_TIME = Integer.MAX_VALUE; // Should be less than shutdown time (reps*sleepingTime)
		//
		String bootstrapperToConnectTo = "130.60.156.102";
		int bootstrapperPortToConnectTo = 4004;
		// MapReduceBroadcastHandler broadcastHandler = new MapReduceBroadcastHandler();

		Number160 id = new Number160(peerCounter);

		PeerMapConfiguration pmc = new PeerMapConfiguration(id);
		pmc.peerNoVerification();
		PeerMap pm = new PeerMap(pmc);
		PeerBuilder peerBuilder = new PeerBuilder(id).peerMap(pm).ports(bootstrapperPortToConnectTo);
		// Bindings b = new Bindings().addAddress(InetAddresses.forString(myIP));

		PeerMapReduce peerMapReduce = new PeerMapReduce(peerBuilder, waitingTime);

		boolean isBootStrapper = (peerCounter == 2);
		if (!isBootStrapper) {
			peerMapReduce.peer().bootstrap().inetAddress(InetAddress.getByName(bootstrapperToConnectTo)).ports(bootstrapperPortToConnectTo).start().awaitUninterruptibly().addListener(new BaseFutureAdapter<FutureBootstrap>() {

				@Override
				public void operationComplete(FutureBootstrap future) throws Exception {
					if (future.isSuccess()) {
						System.err.println("successfully bootstrapped to " + bootstrapperToConnectTo + "/" + bootstrapperPortToConnectTo);
					} else {
						System.err.println("No success on bootstrapping: fail reason: " + future.failedReason());
					}
				}

			});
		}

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// Sniffer.main(null);
		// }
		// }).start();
//		new PeerMapReduce(peer, broadcastHandler);
	}
}
