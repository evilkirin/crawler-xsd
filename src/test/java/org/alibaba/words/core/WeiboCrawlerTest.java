package org.alibaba.words.core;

import java.util.List;

import org.alibaba.words.common.Config;
import org.alibaba.words.domain.WeiboDO;
import org.junit.Assert;
import org.junit.Test;

import weibo4j.model.Paging;


public class WeiboCrawlerTest {

	@Test
	public void testQueryWeiboList() throws Exception {
		Crawler crawler = new WeiboCrawler(Config.accessTokens[0]);
		Paging page = new Paging();
		page.setCount(5);
		page.setPage(1);
		List<WeiboDO> weiboList = crawler.queryWeiboList(page);
		Assert.assertEquals(5, weiboList.size());
	}

}
