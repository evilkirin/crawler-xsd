package org.alibaba.words.core;

import java.util.List;

import org.alibaba.words.common.Worker;
import org.alibaba.words.domain.WeiboDO;

import weibo4j.model.Paging;
import weibo4j.model.WeiboException;

public interface Crawler {

	public List<WeiboDO> queryWeiboList(Worker worker, Paging page) throws WeiboException;

	public String getAccessToken();

}