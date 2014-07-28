package org.alibaba.words.dao;

import java.util.List;

import org.alibaba.words.domain.WeiboDO;

public interface WeiboDAO {
	public int batchInsert(List<WeiboDO> list);

	public abstract int deleteByUserId(String userId);
}
