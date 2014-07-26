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

	public static final int count = 100;// 每次请求获取的记录条数，API限制最大设置为100

	public static final int maxPage = 50;// 当sinceId为1时，也就是第一次请求数据时，为了满足数据量，会多次API请求
											// 但是受限于一个token一小时150次请求，所以这里请求50页数据
	private int index;
	private String accessToken;
	private long sinceId;
	private Timeline timeline;

	public Crawler(int index, String accessToken) {
		this.index = index;
		this.accessToken = accessToken;
		sinceId = 1;
		timeline = new Timeline();
	}

	public void run() {
		logger.info("#Crawler " + index + " begins");

		Thread currentThread = Thread.currentThread();
		WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();

		while (!currentThread.isInterrupted()) {
			int pageCount = 1;
			if (sinceId == UtilConfig.defaultSinceId) {
				pageCount = maxPage;
			}
			long workingSinceId = sinceId;
			for (int i = 1; i <= pageCount; i++) {
				CrawlerResult<List<WeiboDO>> crawlerResult = getDataFromWeb(getPage(workingSinceId, i));

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
					updateSinceId(crawlerResult);
					workingSinceId = sinceId;
				}

				int recordsInserted = weiboDAO.batchInsert(crawlerResult.getModel());
				logger.info("# " + recordsInserted + " weibo inserted to the db.");

				int resultCount = crawlerResult.getModel().size();
				// 表示数据不够maxPage*count
				// 注意，你要求每页取count条记录，api最多返回count-1条数据
				if (resultCount < count - 1) {
					break;
				}
			}
			sleepFor5MinutesUnlessInterrupted();
		}
	}

	private Paging getPage(long currentSince, int i) {
		Paging page = new Paging();
		page.setCount(count);
		page.setPage(i);
		page.setSinceId(currentSince);
		return page;
	}

	public CrawlerResult<List<WeiboDO>> getDataFromWeb(Paging page) {
		CrawlerResult<List<WeiboDO>> crawlerResult = new CrawlerResult<List<WeiboDO>>();
		List<WeiboDO> weiboDOList;
		try {
			weiboDOList = queryWeiboList(page);
		} catch (WeiboException e) {
			int errorCode = e.getErrorCode();
			String errorInfo = e.getError();

			logger.error("fail to query weibo info accessToken = " + accessToken
					+ ", sinceId = " + sinceId, e);
			if (10004 == errorCode || 10022 == errorCode || 10023 == errorCode
					|| 10024 == errorCode) {
				logger.error("Too many request, some rest is needed.");
				crawlerResult.setFailureResult(UtilConfig.ERROR_CODE_NEED_SLEEP, errorInfo);
				return crawlerResult;
			}
			if (10010 == errorCode || 10009 == errorCode) {
				logger.error("Timeout, resume later.");
				crawlerResult
						.setFailureResult(UtilConfig.ERROR_CODE_TIME_EXCEPTION, errorInfo);
				return crawlerResult;
			}
			logger.error("Invalid parameters. Please check before execute.");
			crawlerResult.setFailureResult(UtilConfig.ERROR_CODE_METHOD_ERROR, errorInfo);
			return crawlerResult;
		}
		crawlerResult.setModel(weiboDOList);
		return crawlerResult;
	}

	private List<WeiboDO> queryWeiboList(Paging page) throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		timeline.client.setToken(accessToken);
		StatusWapper status = timeline.getFriendsTimeline(0, 0, page);
		for (Status s : status.getStatuses()) {
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
		logger.info("#Thread " + index + " sleeps for five minutes");
		try {
			TimeUnit.MINUTES.sleep(5);
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
			e.printStackTrace();
		}
	}
}
