package org.alibaba.words.core.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.alibaba.words.common.Config;
import org.alibaba.words.core.Crawler;
import org.alibaba.words.domain.WeiboDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class WeiboCrawler implements Crawler {
	private static final Logger logger = LoggerFactory.getLogger(WeiboCrawler.class);

	private String accessToken;
	private Timeline timeline;

	public WeiboCrawler(String accessToken) {
		super();
		this.accessToken = accessToken;
		timeline = new Timeline();
		timeline.client.setToken(accessToken);
	}

	public Timeline getTimeline() {
		return timeline;
	}

	@Override
	@Nonnull
	public List<WeiboDO> queryWeiboList(long sinceId, int page, int count)
			throws InterruptedException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();
		Paging p = getPage(sinceId, page, count);

		innerQuery(p, weiboDOList);

		if (sinceId == Config.DEFAULT_SINCE_ID) {
			loadMoreThanJustRecentWeibo(weiboDOList);
		}

		logger.info("Crawler with token " + accessToken + " pull back " + weiboDOList.size()
				+ " weibos.");
		return weiboDOList;
	}

	private void loadMoreThanJustRecentWeibo(List<WeiboDO> list) throws InterruptedException {
		for (int i = 2; i <= Config.MAX_PAGE; i++) {
			int resultCount = innerQuery(getPageForInitialLoad(i), list);
			// 表示数据不够maxPage*count
			// 在实际测试微博api的过程中发现可能总条数显示1992条，每页100条，但是指定页数后每页取回的
			// 数量可能不满100，比如可能第2页有100条，第3页有98条，第4页有95条，所以这里我们设了一个容忍值，
			// 认为大于等于count-10条这一页就算取满，还需要往下一页取数据。
			if (resultCount < Config.MAX_RECORDS_PER_REQUEST - 10) {
				break;
			}
		}
	}

	private Paging getPageForInitialLoad(int page) {
		return getPage(Config.DEFAULT_SINCE_ID, page, Config.MAX_RECORDS_PER_REQUEST);
	}

	private Paging getPage(long since, int page, int count) {
		Paging p = new Paging();
		p.setCount(count);
		p.setPage(page);
		p.setSinceId(since);
		return p;
	}

	private int innerQuery(Paging p, List<WeiboDO> list)
			throws InterruptedException {
		List<Status> statuses = getStatuses(p);
		int count = 0;
		for (Status s : statuses) {
			WeiboDO weiBoDO = packWeiBoDO(s);
			list.add(weiBoDO);
			count++;
		}
		return count;
	}

	@Nonnull
	private List<Status> getStatuses(Paging p) throws InterruptedException {
		StatusWapper status = null;
		try {
			status = getTimeline().getFriendsTimeline(0, 1, p);
		} catch (WeiboException e) {
			handleException(e, p.getSinceId());
		}
		if (status != null && status.getStatuses() != null)
			return status.getStatuses();
		return Collections.emptyList();
	}

	private void handleException(WeiboException e, long sinceId) throws InterruptedException {
		int errorCode = e.getErrorCode();
		logger.error("fail to query weibo info accessToken = " + accessToken + ", sinceId = "
				+ sinceId);
		if (isRequestTooFrequent(errorCode)) {
			logger.error("#Crawler with access token:" + accessToken
					+ " : Too many request, some rest is needed. " + errorCode);
			TimeUnit.HOURS.sleep(1);
		} else if (isRequestTimeout(errorCode)) {
			logger.error("#Crawler with access token:" + accessToken
					+ " : Timeout, resume later." + errorCode);
			TimeUnit.MINUTES.sleep(5);
		} else {
			logger.error("#Crawler with access token:" + accessToken
					+ " : Invalid parameters. Please check before execute.");
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

	private WeiboDO packWeiBoDO(Status s) {
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
}