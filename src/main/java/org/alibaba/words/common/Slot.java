package org.alibaba.words.common;

import java.util.concurrent.locks.LockSupport;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slot implements Watcher {

	private static final Logger logger = LoggerFactory.getLogger(Slot.class);

	private final String TOKEN = "token";
	private final ZooKeeper zk;
	private final String root;
	private Thread currentThread;

	public Slot(ZooKeeper zk, String root) throws KeeperException, InterruptedException {
		this.root = root;
		this.zk = zk;

		if (zk != null) {
			Stat s = zk.exists(root, false);
			if (s == null) {
				try {
					zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				} catch (KeeperException ke) {
					if (ke.code() != KeeperException.Code.NODEEXISTS)
						throw ke;
				}
			}
		}
	}

	public void take() throws InterruptedException {
		int retryCount = 2;
		currentThread = Thread.currentThread();
		while (true) {
			try {
				zk.create(root + "/" + TOKEN, new byte[0], Ids.OPEN_ACL_UNSAFE,
						CreateMode.EPHEMERAL);
				if(logger.isInfoEnabled()) {
					logger.info("This node take the responsibility of pulling the weibo data!");
				}
				break;
			} catch (KeeperException ke) {
				if (ke.code() == KeeperException.Code.NODEEXISTS) {
					Stat stat = null;
					try {
						stat = zk.exists(root + "/" + TOKEN, this);
					} catch (KeeperException e) {
						throw new RuntimeException(
								"Fail to query the state of the token node.");
					}
					if (stat != null) {
						logger.warn("Some has already the the responsibility.");
						LockSupport.park(this);
					}
				} else {
					if (--retryCount < 0) {
						throw new RuntimeException(
								"Fail to create child node, but the reason is not that node already exists.",
								ke);
					}
				}
			}
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if (event.getType() == EventType.NodeDeleted) {
			LockSupport.unpark(currentThread);
		}
	}

	public void leave() {
		try {
			zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
