package org.alibaba.words.manager.impl;

import java.util.ArrayList;
import java.util.List;

import org.alibaba.words.common.TeResult;
import org.alibaba.words.common.UtilConfig;
import org.alibaba.words.domain.WeiBoDO;
import org.alibaba.words.manager.CrawlDataManager;
import org.apache.log4j.Logger;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class CrawlDataManagerImpl implements CrawlDataManager{
	
	private static final Logger log = Logger.getLogger(CrawlDataManagerImpl.class);
	
	@Override
	public TeResult<List<WeiBoDO>> getDataFromWeb(String accessToken, String nickName, long sinceId) {
		TeResult<List<WeiBoDO>> teResult = new TeResult<List<WeiBoDO>>();
		List<WeiBoDO> weiBoDOList = new ArrayList<WeiBoDO>();
		Timeline tm = new Timeline();
		tm.client.setToken(accessToken);		
		Paging page = new Paging();
		page.setSinceId(sinceId);
		try {
//			StatusWapper status = tm.getUserTimeline();
//			StatusWapper status = tm.getUserTimelineByName(nickName, page, 0, 0);
			StatusWapper status = tm.getFriendsTimeline(0, 0, new Paging(1));
			for(Status s : status.getStatuses()){
				WeiBoDO weiBoDO = packWeiBoDO(s);
				System.out.println(s.getText());
				weiBoDOList.add(weiBoDO);
			}
		} catch (WeiboException e) {			
			int errorCode = e.getErrorCode();
			String errorInfo = e.getError();
			
			log.error("爬取微博账号失败 nickName = " + nickName + ", accessToken = " + accessToken + ", sinceId = " + sinceId, e);
			if (10004 == errorCode || 10022 == errorCode || 10023 == errorCode || 10024 == errorCode) {
				log.error("请求次数过多，需要休息一会");
				teResult.setFailureResult(UtilConfig.ERROR_CODE_NEED_SLEEP, errorInfo);
				return teResult;
			}
			if (10010 == errorCode || 10009 == errorCode) {
				log.error("请求 超时，请再次请求");
				teResult.setFailureResult(UtilConfig.ERROR_CODE_TIME_EXCEPTION, errorInfo);
				return teResult;
			}
			log.error("方法参数等错误，请检查");
			teResult.setFailureResult(UtilConfig.ERROR_CODE_METHOD_ERROR, errorInfo);
			return teResult;
		}
		teResult.setModel(weiBoDOList);
		return teResult;
	}
	
	public WeiBoDO packWeiBoDO(Status s) {
		WeiBoDO weiBoDO = new WeiBoDO();
		weiBoDO.setWeiBoId(s.getIdstr());
		weiBoDO.setWeiBoText(s.getText());
		weiBoDO.setCreatedTime(s.getCreatedAt());
		weiBoDO.setUserId(s.getUser().getId());
		weiBoDO.setNickName(s.getUser().getScreenName());
		weiBoDO.setRepostsCount(s.getRepostsCount());
		weiBoDO.setCommentsCount(s.getCommentsCount());
		return weiBoDO;
	}

}
