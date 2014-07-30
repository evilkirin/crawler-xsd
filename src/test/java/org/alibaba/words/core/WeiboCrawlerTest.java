package org.alibaba.words.core;

import java.util.List;

import org.alibaba.words.common.Config;
import org.alibaba.words.core.impl.WeiboCrawler;
import org.alibaba.words.domain.WeiboDO;
import org.junit.Assert;
import org.junit.Test;


public class WeiboCrawlerTest {

	@Test
	public void testQueryWeiboList() throws Exception {
		Crawler crawler = new WeiboCrawler(Config.accessTokens[0]);
		List<WeiboDO> weiboList = crawler.queryWeiboList(2, 1, 5);
		Assert.assertEquals(5, weiboList.size());
	}

}
