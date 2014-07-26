package org.alibaba.words.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.dao.impl.WeiboDAOImpl;
import org.alibaba.words.domain.WeiboDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class Crawler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(WeiboDAOImpl.class);
	private int index;
	private String accessToken;
	private long sinceId;
	private int count = 100;

	public Crawler(int index, String accessToken) {
		this.index = index;
		this.accessToken = accessToken;
		sinceId = 1;
	}

	public void run() {
		logger.info("#Crawler " + index + " begins");
		Thread currentThread = Thread.currentThread();

		WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();

		while (!currentThread.isInterrupted()) {
			CrawlerResult<List<WeiboDO>> crawlerResult = getDataFromWeb();
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

			int count = weiboDAO.batchInsert(crawlerResult.getModel());
			logger.info("# " + count + " weibo inserted to the db.");

			sleepFor5MinutesUnlessInterrupted();
		}
	}

	public CrawlerResult<List<WeiboDO>> getDataFromWeb() {
		CrawlerResult<List<WeiboDO>> crawlerResult = new CrawlerResult<List<WeiboDO>>();
		List<WeiboDO> weiboDOList;
		try {
			weiboDOList = queryWeiboList();
		} catch (WeiboException e) {
			int errorCode = e.getErrorCode();
			String errorInfo = e.getError();

			logger.error("fail to query weibo info accessToken = " + accessToken + ", sinceId = " + sinceId, e);
			if (10004 == errorCode || 10022 == errorCode || 10023 == errorCode || 10024 == errorCode) {
				logger.error("Too many request, some rest is needed.");
				crawlerResult.setFailureResult(UtilConfig.ERROR_CODE_NEED_SLEEP, errorInfo);
				return crawlerResult;
			}
			if (10010 == errorCode || 10009 == errorCode) {
				logger.error("Timeout, resume later.");
				crawlerResult.setFailureResult(UtilConfig.ERROR_CODE_TIME_EXCEPTION, errorInfo);
				return crawlerResult;
			}
			logger.error("Invalid parameters. Please check before execute.");
			crawlerResult.setFailureResult(UtilConfig.ERROR_CODE_METHOD_ERROR, errorInfo);
			return crawlerResult;
		}
		crawlerResult.setModel(weiboDOList);
		return crawlerResult;
	}

	private List<WeiboDO> queryWeiboList()
			throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		Timeline tm = new Timeline();
		tm.client.setToken(accessToken);

		Paging page = new Paging();
		page.setCount(count);
		page.setSinceId(sinceId);

		StatusWapper status = tm.getFriendsTimeline(0, 0, page);
		for(Status s : status.getStatuses()){
			WeiboDO weiBoDO = packWeiBoDO(s);
			weiboDOList.add(weiBoDO);
		}
		return weiboDOList;
	}

	public WeiboDO packWeiBoDO(Status s) {
		WeiboDO weiBoDO = new WeiboDO();
		weiBoDO.setWeiboId(s.getIdstr());
		weiBoDO.setWeiboText(s.getText());
		weiBoDO.setCreatedTime(s.getCreatedAt());
		weiBoDO.setUserId(s.getUser().getId());
		weiBoDO.setNickName(s.getUser().getScreenName());
		weiBoDO.setRepostsCount(s.getRepostsCount());
		weiBoDO.setCommentsCount(s.getCommentsCount());
		return weiBoDO;
	}

	private boolean updateSinceId(CrawlerResult<List<WeiboDO>> crawlerResult) {
		long old = sinceId, newSinceId = -1;
		for (WeiboDO weiBoDO : crawlerResult.getModel()) {
			newSinceId = old > weiBoDO.getWeiboId() ? old : weiBoDO.getWeiboId();
		}
		sinceId = old > newSinceId ? old : newSinceId;
		return sinceId == old;
	}

	private void sleepForOneHourUnlessInterrupted() {
		try {
			TimeUnit.HOURS.sleep(1);
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
			e.printStackTrace();
		}
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
}
