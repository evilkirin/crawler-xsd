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

import weibo4j.model.Paging;

public class Crawler implements Runnable {

	private static final Logger logger = LoggerFactory
			.getLogger(WeiboDAOImpl.class);

	public static final int count = 100;// 每次请求获取的记录条数，API限制最大设置为100

	public static final int maxPage = 50;// 当sinceId为1时，也就是第一次请求数据时，为了满足数据量，会多次API请求
											// 但是受限于一个token一小时150次请求，所以这里请求50页数据
	private int index;

	public void run() {
		System.out.println("#Thread " + index + " begins");
		while (true) {
			CrawlDataManager crawlDataManager = new CrawlDataManagerImpl();
			WeiboDAO weiboDAO = new WeiboDAOImpl();
			int pageCount = 1;
			if (UtilConfig.sinceIds[index] == UtilConfig.defaultSinceId) {
				pageCount = maxPage;
			}
			long newSinceId = UtilConfig.sinceIds[index];
			for (int i = 1; i <= pageCount; i++) {
				Paging page = new Paging();
				page.setCount(count);
				page.setPage(i);
				page.setSinceId(UtilConfig.sinceIds[index]);

				CrawlerResult<List<WeiboDO>> crawlerResult = crawlDataManager
						.getDataFromWeb(UtilConfig.accessTokens[index], page,
								UtilConfig.sinceIds[index]);
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
						logger.warn("#Thread " + index + " exits.");
						return;
					}
				}
				if (i == 1) {
					newSinceId = updateSinceId(crawlerResult);
				}

				weiboDAO.batchInsert(crawlerResult.getModel());
				int resultCount = crawlerResult.getModel().size();

				System.out.println("PAGE:" +i+"...COUNT:"+ resultCount);
				if (resultCount < count - 1) {// 表示数据不够maxPage*count
												// 注意，你要求每页取count条记录，api最多返回count-1条数据
					break;
				}
			}
			UtilConfig.sinceIds[index] = UtilConfig.sinceIds[index] > newSinceId ? UtilConfig.sinceIds[index]
					: newSinceId;

			sleepFor5MinutesUnlessInterrupted();
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
			sinceIdIndex = sinceIdIndex > weiBoDO.getWeiBoId() ? sinceIdIndex
					: weiBoDO.getWeiBoId();
		}
		// UtilConfig.sinceIds[index] = UtilConfig.sinceIds[index] >
		// sinceIdIndex ? UtilConfig.sinceIds[index]
		// : sinceIdIndex;
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
