package org.alibaba.words.domain;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Visitor {
    private int id;
    private String name;
    private String email;
    private int status;
    private Date createTime;

    public Visitor() {
        createTime = new Date();
    }

    public Visitor(String name, String email) {
        this.name = name;
        this.email = email;
        this.setStatus(1);
        this.createTime = new Date();
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public Date getCreateTime() {
        return createTime;
    }

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.format("{Id: %d, Name: %s, CreateTime: %s}", id, name,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTime));
    }
}

