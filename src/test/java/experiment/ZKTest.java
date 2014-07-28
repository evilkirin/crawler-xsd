package experiment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class ZKTest {

	private static final String ADDR = "10.125.193.148:2181";
	private static final String SLOT_ROOT = "/xieyutest_slot";
	private static int cardinal = 0;

	@Test
	public void testSlot() throws InterruptedException {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Slot s;
				try {
					s = new Slot(ADDR, SLOT_ROOT);
					System.out.println("Thread a try to take the slot " + new Date());
					s.take();
					System.out.println("Thread a successfully take the slot " + new Date());
					TimeUnit.SECONDS.sleep(5);
					s.leave();
					System.out.println("Thread a exits");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}).start();
		TimeUnit.SECONDS.sleep(2);
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					System.out.println("Thread b try to take the slot " + new Date());
					Slot s;
					s = new Slot(ADDR, SLOT_ROOT);
					s.take();
					System.out.println("Thread b successfully take the slot " + new Date());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}).start();
		TimeUnit.SECONDS.sleep(30);
	}

	class Worker implements Runnable {
		private Barrier barrier;
		private final int index;

		public Worker() throws KeeperException, InterruptedException, IOException {
			this.barrier = new Barrier(ADDR, "/xieyutest_barrier", 2);
			index = ++cardinal;
		}

		@Override
		public void run() {
			System.out.println("Worker " + index + " is entering the barrier");
			boolean result = false;
			try {
				result = barrier.enter();
			} catch (KeeperException e) {
				e.printStackTrace();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (result) {
				System.out.println("Worker " + index + " successfully enters the barrier");
			}
		}
	}

	@Test
	public void testBarrier() throws KeeperException, InterruptedException, IOException {
		Worker w1 = new Worker();
		Worker w2 = new Worker();
		ExecutorService service = Executors.newFixedThreadPool(2);
		service.submit(w1);
		service.submit(w2);
		TimeUnit.SECONDS.sleep(30);
		System.out.println("Test finishi");
	}

	class MyWatcher implements Watcher {

		@Override
		public void process(WatchedEvent event) {
			System.out.println(event);
		}

	}

	@Test
	public void testZK() throws IOException, KeeperException, InterruptedException {
		ZooKeeper zk = new ZooKeeper(ADDR, 5000, new MyWatcher());
		List<String> children = zk.getChildren("/substatus", true);
		System.out.println(children);
	}

	public void testReader() throws UnsupportedEncodingException {
		InputStream resourceAsStream = ZKTest.class.getClassLoader().getResourceAsStream("");
		InputStreamReader inr = new InputStreamReader(resourceAsStream, "utf-8");
		BufferedReader br = new BufferedReader(inr);
	}
}
