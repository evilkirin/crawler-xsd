package org.alibaba.words.mapper;

import org.alibaba.words.domain.WeiboDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

public interface WeiboMapper {

	static final String BATCH_INSERT = "Insert into Weibo (weiboId, weiboText, userId, nickName, repostsCount, commentsCount, createdTime) " +
			"Values (#{weiboId}, #{weiboText}, #{userId}, #{nickName}, #{repostsCount}, #{commentsCount}, #{createdTime})";
	static final String DELETE_BY_ID = "Delete from";

	@Insert(BATCH_INSERT)
	public int insertOneWeibo(WeiboDO weibo);

	@Delete("")
	public int deleteById(long id);
}
