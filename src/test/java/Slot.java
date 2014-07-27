import java.io.IOException;
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

public class Slot implements Watcher{

	private static final Logger logger = LoggerFactory.getLogger(Slot.class);

	private final String TOKEN = "token";
	private final ZooKeeper zk;
	private final String root;
	private Thread currentThread;

	public Slot(String addr, String root) throws IOException, KeeperException, InterruptedException {
		this.root = root;
		zk = new ZooKeeper(addr, 3000, this);

		// Create barrier node
		if (zk != null) {
			Stat s = zk.exists(root, false);
			if (s == null) {
				try {
					zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				} catch(KeeperException ke) {
					if(ke.code() != KeeperException.Code.NODEEXISTS)
						throw ke;
				}
			}
		}
	}

	public void take() throws InterruptedException {
		int retryCount = 2;
		currentThread = Thread.currentThread();
		while(true) {
			try {
				zk.create(root + "/" + TOKEN, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
				break;
			} catch(KeeperException ke) {
				if(ke.code() == KeeperException.Code.NODEEXISTS ) {
					System.out.println("Fail to take the slot.");
					Stat stat = null;
					try {
						stat = zk.exists(root + "/" + TOKEN, true);
					} catch(KeeperException e) {
						throw new RuntimeException("Fail to query the state of the token node.");
					}
					if(stat != null)
						LockSupport.park(this);
					System.out.println("Try again after park.");
				} else {
					if(--retryCount < 0) {
						throw new RuntimeException("Fail to create child node, but the reason is not that node already exists.", ke);
					}
					System.out.println("Try again.");
				}
			}
		}
	}

	@Override
	public void process(WatchedEvent event) {
		if(event.getType() == EventType.NodeDeleted) {
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
