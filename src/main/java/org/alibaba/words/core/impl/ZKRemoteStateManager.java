package org.alibaba.words.core.impl;

import org.alibaba.words.core.RemoteStateManager;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKRemoteStateManager implements RemoteStateManager<Long> {
	private static final Logger logger = LoggerFactory.getLogger(ZKRemoteStateManager.class);

	private String zkNodePath;
	private ZooKeeper zk;

	public ZKRemoteStateManager(String zkNodePath, ZooKeeper zk) {
		super();
		this.zkNodePath = zkNodePath;
		this.zk = zk;
	}

	public String getZkNode() {
		return zkNodePath;
	}

	public ZooKeeper getZk() {
		return zk;
	}

	@Override
	public void syncState(Long sinceId) throws InterruptedException {
		byte[] raw = String.valueOf(sinceId).getBytes();
		try {
			getZk().setData(getZkNode(), raw, -1);
		} catch (KeeperException e) {
			logger.warn("It seems the sinceId sync operation with zk is failed.", e);
		}
	}

	@Override
	public Long initialState() throws InterruptedException {
		Stat stat = null;
		byte[] raw = null;
		long data = 0l;
		try {
			stat = getZk().exists(getZkNode(), false);
			if (stat != null) {
				raw = getZk().getData(getZkNode(), false, stat);
				try {
					data = Long.parseLong(new String(raw));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			} else {
				getZk().create(getZkNode(), new byte[0], Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
				raw = String.valueOf(1l).getBytes();
				getZk().setData(getZkNode(), raw, -1);
			}
		} catch (KeeperException ke) {
			throw new RuntimeException("Fail to acquire state from zookeeper.");
		}
		return data;
	}
}