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

		while (!currentThread.isInterrupted()) {
			boolean initialLoad = sinceId == UtilConfig.defaultSinceId;

			try {
				List<WeiboDO> list = queryWeiboList(getPage(sinceId, 1));
				updateSinceId(list);
				int recordsInserted = weiboDAO.batchInsert(list);
				logger.info("# " + recordsInserted + " weibo inserted to the db.");

				if (initialLoad) {
					loadMoreThanJustRecentWeibo();
				}
			} catch (WeiboException e) {
				if (currentThread.isInterrupted())
					return;
				handleException(e);
			}

			if (currentThread.isInterrupted())
				return;
			sleepFor5MinutesUnlessInterrupted();
		}
	}

	private void loadMoreThanJustRecentWeibo() {
		List<WeiboDO> list;
		int recordsInserted;
		for (int i = 2; i <= maxPage; i++) {
			try {
				list = queryWeiboList(getPage(UtilConfig.defaultSinceId, i));

				recordsInserted = weiboDAO.batchInsert(list);
				logger.info("# " + recordsInserted + " weibo inserted to the db.");

				int resultCount = list.size();
				// 表示数据不够maxPage*count
				//在实际测试微博api的过程中发现可能总条数显示1992条，每页100条，但是指定页数后每页取回的
				//数量可能不满100，比如可能第2页有100条，第3页有98条，第4页有95条，所以这里我们设了一个容忍值，
				//认为大于等于count-10条这一页就算取满，还需要往下一页取数据。
				if (resultCount < count - 10) {
					break;
				}
			} catch (WeiboException e) {
				handleException(e);
			}
		}
	}

	private void handleException(WeiboException e) {
		int errorCode = e.getErrorCode();
		logger.error("fail to query weibo info accessToken = " + accessToken + ", sinceId = "
				+ sinceId, e);
		if (isRequestTooFrequent(errorCode)) {
			logger.error("Too many request, some rest is needed.");
			sleepForOneHourUnlessInterrupted();
		} else if (isRequestTimeout(errorCode)) {
			logger.error("Timeout, resume later.");
			sleepFor5MinutesUnlessInterrupted();
		} else {
			logger.error("Invalid parameters. Please check before execute.");
			throw new RuntimeException("Invalid configuration.");
		}
	}

	private boolean isRequestTimeout(int errorCode) {
		return 10010 == errorCode || 10009 == errorCode;
	}

	private boolean isRequestTooFrequent(int errorCode) {
		return 10004 == errorCode || 10022 == errorCode || 10023 == errorCode
				|| 10024 == errorCode;
	}

	private Paging getPage(long currentSince, int i) {
		Paging page = new Paging();
		page.setCount(count);
		page.setPage(i);
		page.setSinceId(currentSince);
		return page;
	}

	private List<WeiboDO> queryWeiboList(Paging page) throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		timeline.client.setToken(accessToken);
		StatusWapper status = timeline.getFriendsTimeline(0, 1, page);
		for (Status s : status.getStatuses()) {
			WeiboDO weiBoDO = packWeiBoDO(s);
			weiboDOList.add(weiBoDO);
		}
		System.out.println("page:"+page.getPage()+" totalCount:"+status.getTotalNumber()
				+" pageCount:"+status.getStatuses().size());
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
