package experiment;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class Barrier extends SyncPrimitive {

	private int size;
	private String name;

	Barrier(String address, String name, int size) throws KeeperException,
			InterruptedException, IOException {
		super(address);
		this.root = name;
		this.size = size;

		// Create barrier node
		if (zk != null) {
			Stat s = zk.exists(root, false);
			if (s == null) {
				zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
		}

		// My node name
		this.name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString() + System.currentTimeMillis());
	}

	boolean enter() throws KeeperException, InterruptedException {
		zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		while (true) {
			synchronized (mutex) {
				List<String> list = zk.getChildren(root, true);

				if (list.size() < size) {
					mutex.wait();
				} else {
					return true;
				}
			}
		}
	}

	boolean leave() throws KeeperException, InterruptedException {
		zk.delete(root + "/" + name, 0);
		while (true) {
			synchronized (mutex) {
				List<String> list = zk.getChildren(root, true);
				if (list.size() > 0) {
					mutex.wait();
				} else {
					return true;
				}
			}
		}
	}

}
