package org.alibaba.words.core.impl;

import org.alibaba.words.common.Config;
import org.alibaba.words.core.RemoteStateManager;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Test;


public class ZKRemoteStateManagerTest {

	@Test
	public void testUpdate() throws Exception {
		ZooKeeper zooKeeper = new ZooKeeper(Config.ZK_ADDR, Config.CONNECT_TIMEOUT, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				System.out.println(event.toString());
			}
		});
		RemoteStateManager<Long> stateManager = new ZKRemoteStateManager("/zk_remotestatemanager_test", zooKeeper);
		long state = stateManager.query();
		stateManager.update(11l);
		state = stateManager.query();
		Assert.assertEquals(11l, state);
	}

}
