package org.alibaba.words.common;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.core.Crawler;
import org.alibaba.words.core.RemoteStateManager;
import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.dao.impl.WeiboDAOImpl;
import org.alibaba.words.domain.WeiboDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable {

	static final Logger logger = LoggerFactory.getLogger(Worker.class);

	private int index;
	private long sinceId;
	private int count = 0;
	private int lastSyncCount = 0;

	private static final WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();
	private final Crawler crawler;
	private final RemoteStateManager<Long> stateManager;

	public Worker(int index, RemoteStateManager<Long> stateManager,
			Crawler crawler) {
		this.index = index;
		sinceId = 1;
		this.stateManager = stateManager;
		this.crawler = crawler;
	}

	public void init() throws InterruptedException {
		long data = (Long) stateManager.query();
		if (data > 0) {
			sinceId = data;
		}
		if(logger.isInfoEnabled()) {
			logger.info("#Worker " + index + " starts pulling from sinceId :" + sinceId);
		}
	}

	public void run() {
		logger.info("#Worker " + index + " begins");

		Thread currentThread = Thread.currentThread();
		try {
			while (!currentThread.isInterrupted()) {
				List<WeiboDO> list = crawler.queryWeiboList(sinceId, 1,
						Config.MAX_RECORDS_PER_REQUEST);
				int recordsInserted = weiboDAO.batchInsert(list);
				count += recordsInserted;
				updateSinceId(list);
				logger.info("#Worker " + index + " : " + recordsInserted + " weibo out of "
						+ list.size() + " inserted to the db. Total : " + count
						+ ". sleep for " + Config.PULL_INTERVAL + " minutes");
				TimeUnit.MINUTES.sleep(Config.PULL_INTERVAL);
			}
		} catch (InterruptedException e) {
			logger.error("#Worker " + index + " exits.");
		} catch (RuntimeException re) {
			logger.error("Unexpected runtime exception.", re);
		}
	}

	private void updateSinceId(List<WeiboDO> list) throws InterruptedException {
		long old = sinceId, newSinceId = 0;
		for (WeiboDO weiBoDO : list) {
			newSinceId = newSinceId > weiBoDO.getWeiboId() ? newSinceId : weiBoDO.getWeiboId();
		}
		sinceId = old > newSinceId ? old : newSinceId;
		if (count - lastSyncCount > 25) {
			stateManager.update(sinceId);
			lastSyncCount = count;
		}
	}

	public long getSinceId() {
		return sinceId;
	}
}
