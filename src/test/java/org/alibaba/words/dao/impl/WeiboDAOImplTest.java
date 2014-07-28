package org.alibaba.words.dao.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.domain.WeiboDO;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class WeiboDAOImplTest {

	private static final String SPECIALUSERID = "998877112233";

	@Test
	public void testBatchInsert() throws Exception {
		WeiboDAO dao = WeiboDAOImpl.getInstance();
		int result = dao.batchInsert(getWeiboList(10));
		Assert.assertEquals(10, result);
		dao.deleteByUserId(SPECIALUSERID);
	}

	private List<WeiboDO> getWeiboList(int n) {
		List<WeiboDO> list = Lists.newArrayList();
		for(;n > 0; n--) {
			list.add(getWeiboDO());
			try {
				TimeUnit.MILLISECONDS.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list;
	}

	private WeiboDO getWeiboDO() {
		WeiboDO weibo = new WeiboDO();
		weibo.setCommentsCount(0);
		weibo.setCreatedTime(new Date());
		weibo.setNickName(Thread.currentThread().getName() + " " + System.currentTimeMillis());
		weibo.setRepostsCount(0);
		weibo.setUserId(SPECIALUSERID);
		weibo.setWeiboId(System.nanoTime());
		weibo.setWeiboText("just test");
		return weibo;
	}

}
