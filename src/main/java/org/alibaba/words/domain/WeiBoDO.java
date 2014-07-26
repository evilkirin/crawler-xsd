package org.alibaba.words.domain;

import java.util.Date;

public class WeiBoDO {
	
	private long id; //数据库ID
	
	private long weiBoId; //微博ID
	
	private String weiBoText; //微博内容
	
	private Date createdTime; // 微博创建时间
	
	private String userId; //微博用户ID
	
	private String nickName; //微博昵称
	
	private int repostsCount; //转发数
	
	private int commentsCount; //评论数

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getWeiBoId() {
		return weiBoId;
	}

	public void setWeiBoId(long weiBoId) {
		this.weiBoId = weiBoId;
	}

	public String getWeiBoText() {
		return weiBoText;
	}

	public void setWeiBoText(String weiBoText) {
		this.weiBoText = weiBoText;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public int getRepostsCount() {
		return repostsCount;
	}

	public void setRepostsCount(int repostsCount) {
		this.repostsCount = repostsCount;
	}

	public int getCommentsCount() {
		return commentsCount;
	}

	public void setCommentsCount(int commentsCount) {
		this.commentsCount = commentsCount;
	}
	

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	@Override
	public String toString() {
		return "WeiBoDO [id=" + id + ", weiBoId=" + weiBoId + ", weiBoText="
				+ weiBoText + ", createdTime=" + createdTime + ", userId="
				+ userId + ", nickName=" + nickName + ", repostsCount="
				+ repostsCount + ", commentsCount=" + commentsCount + "]";
	}
	
}
