package org.alibaba.words.domain;

public class AccountInfo {
	
	private String userId;
	
	private String pwd;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	@Override
	public String toString() {
		return "AccountInfo [userId=" + userId + ", pwd=" + pwd + "]";
	}
	
	
	

}
