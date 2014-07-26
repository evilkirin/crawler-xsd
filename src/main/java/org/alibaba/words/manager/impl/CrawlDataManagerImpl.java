package org.alibaba.words.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.alibaba.words.common.CrawlerResult;
import org.alibaba.words.common.UtilConfig;
import org.alibaba.words.domain.WeiboDO;
import org.alibaba.words.manager.CrawlDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class CrawlDataManagerImpl implements CrawlDataManager{
	private static final Logger logger = LoggerFactory.getLogger(CrawlDataManagerImpl.class);

	private static final int count = 100;
	@Override
	public CrawlerResult<List<WeiboDO>> getDataFromWeb(String accessToken, long sinceId) {
		CrawlerResult<List<WeiboDO>> crawlerResult = new CrawlerResult<List<WeiboDO>>();
		List<WeiboDO> weiboDOList;
		try {
			weiboDOList = queryWeiboList(accessToken, sinceId);
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

	private List<WeiboDO> queryWeiboList(String accessToken, long sinceId)
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

}
