package org.alibaba.words.core;

import java.util.List;

import org.alibaba.words.domain.WeiboDO;

public interface Crawler {

	public List<WeiboDO> queryWeiboList(long sinceId, int page, int count) throws InterruptedException;

}