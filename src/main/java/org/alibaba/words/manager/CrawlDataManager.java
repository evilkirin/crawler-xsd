package org.alibaba.words.manager;

import java.util.List;

import org.alibaba.words.common.TeResult;

import org.alibaba.words.domain.WeiBoDO;

public interface CrawlDataManager {
	public TeResult<List<WeiBoDO>> getDataFromWeb(String accessToken, String nickName, long sinceId);
}
