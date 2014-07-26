package org.alibaba.words;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.common.Crawler;
import org.alibaba.words.common.UtilConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Bootstrap {

	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

	private ExecutorService service;

	public void start() {
		for(int i = 0; i < 1; i++) {
			Crawler worker = new Crawler(i + 1, UtilConfig.accessTokens[i]);
			service.execute(worker);
		}
	}

	public void init() {
		service = Executors.newCachedThreadPool();
	}

	public void stop() {
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
			logger.error("Bootstrap exits.");
			bootstrap.stop();
		}
	}
}


