package org.alibaba.words.domain;

import java.util.Date;

public class WeiboDO {

	private long id;

	private long weiboId;

	private String weiboText; // main content

	private Date createdTime;

	private String userId; //id of the user who create the weibo

	private String nickName; //name of the user

	private int repostsCount;

	private int commentsCount;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getWeiboId() {
		return weiboId;
	}

	public void setWeiboId(long weiBoId) {
		this.weiboId = weiBoId;
	}

	public String getWeiboText() {
		return weiboText;
	}

	public void setWeiboText(String weiBoText) {
		this.weiboText = weiBoText;
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
		return "WeiBoDO [id=" + id + ", weiBoId=" + weiboId + ", weiBoText="
				+ weiboText + ", createdTime=" + createdTime + ", userId="
				+ userId + ", nickName=" + nickName + ", repostsCount="
				+ repostsCount + ", commentsCount=" + commentsCount + "]";
	}

}
