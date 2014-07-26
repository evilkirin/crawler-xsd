package org.alibaba.words.common;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.dao.impl.WeiboDAOImpl;
import org.alibaba.words.domain.WeiboDO;
import org.alibaba.words.manager.CrawlDataManager;
import org.alibaba.words.manager.impl.CrawlDataManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crawler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(WeiboDAOImpl.class);
	private int index;

	public void run() {
		System.out.println("#Thread " + index + " begins");
		while (true) {
			CrawlDataManager crawlDataManager = new CrawlDataManagerImpl();
			WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();
			CrawlerResult<List<WeiboDO>> crawlerResult = crawlDataManager.getDataFromWeb(
					UtilConfig.accessTokens[index],UtilConfig.sinceIds[index]);
			if (crawlerResult == null) {
				logger.error("get data from web error");
				sleepFor5MinutesUnlessInterrupted();
				continue;
			}

			if (crawlerResult.isSuccess() == false) {
				if (crawlerResult.getErrorCode() == UtilConfig.ERROR_CODE_NEED_SLEEP) {
					sleepForOneHourUnlessInterrupted();
					continue;
				}

				if (crawlerResult.getErrorCode() == UtilConfig.ERROR_CODE_TIME_EXCEPTION) {
					sleepFor5MinutesUnlessInterrupted();
					continue;
				}

				if (crawlerResult.getErrorCode() == UtilConfig.ERROR_CODE_METHOD_ERROR) {
					logger.warn("#Thread " + index +" exits.");
					return;
				}
			}

			updateSinceId(crawlerResult);

			weiboDAO.batchInsert(crawlerResult.getModel());

			return;
			//sleepFor5MinutesUnlessInterrupted();
		}
	}

	private void sleepForOneHourUnlessInterrupted() {
		try {
			TimeUnit.HOURS.sleep(1);
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
			e.printStackTrace();
		}
	}

	private long updateSinceId(CrawlerResult<List<WeiboDO>> crawlerResult) {
		long sinceIdIndex = UtilConfig.sinceIds[index];
		for (WeiboDO weiBoDO : crawlerResult.getModel()) {
			sinceIdIndex = sinceIdIndex > weiBoDO.getWeiBoId() ? sinceIdIndex : weiBoDO.getWeiBoId();
		}
		UtilConfig.sinceIds[index] = UtilConfig.sinceIds[index] > sinceIdIndex ? UtilConfig.sinceIds[index]
				: sinceIdIndex;
		return sinceIdIndex;
	}

	private void sleepFor5MinutesUnlessInterrupted() {
		logger.info("------Thread " + index + " sleep five minutes----------");
		try {
			TimeUnit.MINUTES.sleep(5);
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
			e.printStackTrace();
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
