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

	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

	public static final int count = 100;// 每次请求获取的记录条数，API限制最大设置为100

	public static final int maxPage = 50;// 当sinceId为1时，也就是第一次请求数据时，为了满足数据量，会多次API请求
											// 但是受限于一个token一小时150次请求，所以这里请求50页数据
	private int index;
	private String accessToken;
	private long sinceId;
	private Timeline timeline;
	private static final WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();

	public Crawler(int index, String accessToken) {
		this.index = index;
		this.accessToken = accessToken;
		sinceId = 1;
		timeline = new Timeline();
	}

	public void run() {
		logger.info("#Crawler " + index + " begins");

		Thread currentThread = Thread.currentThread();

		try {
			while (!currentThread.isInterrupted()) {
				boolean initialLoad = sinceId == UtilConfig.defaultSinceId;

				try {
					List<WeiboDO> list = queryWeiboList(getPage(sinceId, 1));
					updateSinceId(list);
					int recordsInserted = weiboDAO.batchInsert(list);
					logger.info("#Crawler " + index + " : " + recordsInserted + " weibo out of " + list.size() + " inserted to the db.");

					if (initialLoad) {
						loadMoreThanJustRecentWeibo();
					}
				} catch (WeiboException e) {
					handleException(e);
				}
				logger.info("#Crawler " + index + " sleep for a while.");
				TimeUnit.MINUTES.sleep(5);
			}
		} catch(InterruptedException e) {
			logger.info("#Crawler " + index + " exits.");
		}
	}

	private void loadMoreThanJustRecentWeibo() throws InterruptedException {
		List<WeiboDO> list;
		int recordsInserted;
		for (int i = 2; i <= maxPage; i++) {
			try {
				list = queryWeiboList(getPage(UtilConfig.defaultSinceId, i));

				recordsInserted = weiboDAO.batchInsert(list);
				logger.info("#Crawler " + index + " : " + recordsInserted + " weibo out of " + list.size() + " inserted to the db.");

				int resultCount = list.size();
				// 表示数据不够maxPage*count
				// 注意，你要求每页取count条记录，api最多返回count-1条数据
				if (resultCount < count - 1) {
					break;
				}
			} catch (WeiboException e) {
				handleException(e);
			}
		}
	}

	private void handleException(WeiboException e) throws InterruptedException {
		int errorCode = e.getErrorCode();
		logger.error("fail to query weibo info accessToken = " + accessToken + ", sinceId = "
				+ sinceId, e);
		if (isRequestTooFrequent(errorCode)) {
			logger.error("Too many request, some rest is needed.");
			TimeUnit.HOURS.sleep(1);
		} else if (isRequestTimeout(errorCode)) {
			logger.error("Timeout, resume later.");
			TimeUnit.MINUTES.sleep(5);
		} else {
			logger.error("Invalid parameters. Please check before execute.");
			throw new RuntimeException("Invalid configuration.", e);
		}
	}

	private boolean isRequestTimeout(int errorCode) {
		return 10010 == errorCode || 10009 == errorCode;
	}

	private boolean isRequestTooFrequent(int errorCode) {
		return 10004 == errorCode || 10022 == errorCode || 10023 == errorCode
				|| 10024 == errorCode;
	}

	private Paging getPage(long currentSince, int page) {
		Paging p = new Paging();
		p.setCount(count);
		p.setPage(page);
		p.setSinceId(currentSince);
		return p;
	}

	private List<WeiboDO> queryWeiboList(Paging page) throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		timeline.client.setToken(accessToken);
		StatusWapper status = timeline.getFriendsTimeline(0, 1, page);

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

	private boolean updateSinceId(List<WeiboDO> list) {
		long old = sinceId, newSinceId = -1;
		for (WeiboDO weiBoDO : list) {
			newSinceId = newSinceId > weiBoDO.getWeiboId() ? newSinceId : weiBoDO.getWeiboId();
		}
		sinceId = old > newSinceId ? old : newSinceId;
		return sinceId == old;
	}
}
