package org.alibaba.words;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.common.Crawler;
import org.alibaba.words.common.Slot;
import org.alibaba.words.common.Config;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Bootstrap {

	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	private ExecutorService service;
	private ZooKeeper zk;
	private Slot slot;

	public void start() throws InterruptedException {
		for(int i = 0; i < Config.accessTokens.length; i++) {
			Crawler worker = new Crawler(i + 1, Config.accessTokens[i], zk);
			try {
				worker.init();
				service.execute(worker);
			} catch (KeeperException e) {
				logger.error("Fail to initiate the crawler with accessToken:" + Config.accessTokens[i], e);
			}
		}
	}

	public void init() throws IOException, KeeperException, InterruptedException {
		zk = new ZooKeeper(Config.ZK_ADDR, Config.CONNECT_TIMEOUT, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				logger.info(event.toString());
			}
		});
		slot = new Slot(zk, Config.ZK_ADDR, Config.SLOT_ROOT);
		slot.take();
		service = Executors.newCachedThreadPool();
	}

	public void stop() {
		slot.leave();
		service.shutdown();
		try {
			service.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Error waiting for service to shutdown.", e);
		}
	}

	public static void main(String args[]) {
		Bootstrap bootstrap = new Bootstrap();
		try {
			bootstrap.init();
			bootstrap.start();
		} catch (Exception e) {
			logger.error("Bootstrap exits.", e);
			bootstrap.stop();
		}
	}
}


