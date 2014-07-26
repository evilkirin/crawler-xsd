package org.alibaba.words.mapper;

import org.alibaba.words.domain.WeiboDO;
import org.apache.ibatis.annotations.Insert;

public interface WeiboMapper {

	static final String BATCH_INSERT = "Insert into Weibo ('weiboId', 'weiboText', 'userId', 'nickName', 'repostsCount', 'commentsCount', 'createdTime')" +
			"Values (#{weiboId}, #{weiboText}, #{userId}, #{nickName}, #{repostsCount}, #{commentsCount}, #{createdTime})";

	@Insert(BATCH_INSERT)
	public int insertOneWeibo(WeiboDO weibo);

}
