package org.alibaba.words.manager;

import java.util.List;

import org.alibaba.words.common.CrawlerResult;

import org.alibaba.words.domain.WeiboDO;

public interface CrawlDataManager {
	public CrawlerResult<List<WeiboDO>> getDataFromWeb(String accessToken, long sinceId);
}
