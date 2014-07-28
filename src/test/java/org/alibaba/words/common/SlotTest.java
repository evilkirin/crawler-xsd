package org.alibaba.words.common;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class SlotTest {

	private static final String ADDR = "10.125.193.148:2181";
	private static final String SLOT_ROOT = "/xieyutest_slot";

	@Test
	public void testTake() throws Exception {
		final ZooKeeper zk = new ZooKeeper(ADDR, 3000, null);
		new Thread(new Runnable() {

			@Override
			public void run() {
				Slot s;
				try {
					s = new Slot(zk, SLOT_ROOT);
					System.out.println("Thread a try to take the slot " + new Date());
					s.take();
					System.out.println("Thread a successfully take the slot " + new Date());
					TimeUnit.SECONDS.sleep(5);
					s.leave();
					System.out.println("Thread a exits");
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
				Slot s;
				try {
					System.out.println("Thread b try to take the slot " + new Date());
					s = new Slot(zk, SLOT_ROOT);
					s.take();
					System.out.println("Thread b successfully take the slot " + new Date());
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

}
