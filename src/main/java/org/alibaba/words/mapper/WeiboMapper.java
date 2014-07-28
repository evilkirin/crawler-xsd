package org.alibaba.words.mapper;

import org.alibaba.words.domain.WeiboDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

public interface WeiboMapper {

	static final String BATCH_INSERT = "Insert into Weibo (weiboId, weiboText, userId, nickName, repostsCount, commentsCount, createdTime) " +
			"Values (#{weiboId}, #{weiboText}, #{userId}, #{nickName}, #{repostsCount}, #{commentsCount}, #{createdTime})";
	static final String DELETE_BY_ID = "Delete from Weibo where id=#{id}";
	static final String DELETE_BY_USER_ID = "Delete from Weibo where userId=#{id}";

	@Insert(BATCH_INSERT)
	public int insertOneWeibo(WeiboDO weibo);

	@Delete(DELETE_BY_ID)
	public int deleteById(long id);

	@Delete(DELETE_BY_USER_ID)
	public int deleteByUserId(String id);
}
