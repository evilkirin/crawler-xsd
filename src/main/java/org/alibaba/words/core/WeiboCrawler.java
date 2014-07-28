package org.alibaba.words.core;

import java.util.ArrayList;
import java.util.List;

import org.alibaba.words.common.Worker;
import org.alibaba.words.domain.WeiboDO;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class WeiboCrawler implements Crawler {
	private String accessToken;
	private Timeline timeline;

	public WeiboCrawler() {
	}

	public WeiboCrawler(String accessToken) {
		super();
		this.accessToken = accessToken;
		timeline = new Timeline();
	}

	@Override
	public String getAccessToken() {
		return accessToken;
	}

	public Timeline getTimeline() {
		return timeline;
	}

	@Override
	public List<WeiboDO> queryWeiboList(Paging page) throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		getTimeline().client.setToken(getAccessToken());
		StatusWapper status = getTimeline().getFriendsTimeline(0, 1, page);
		for (Status s : status.getStatuses()) {
			WeiboDO weiBoDO = packWeiBoDO(s);
			weiboDOList.add(weiBoDO);
		}
		return weiboDOList;
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