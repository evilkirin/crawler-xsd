package org.alibaba.words.common;

import weibo4j.util.WeiboConfig;

public class Config {

	public static String[] accessTokens;

	public static final int DEFAULT_SINCE_ID = 1;
	public static final String SLOT_ROOT = "/xieyu_crawler";

	public static final int MAX_RECORDS_PER_REQUEST = Integer
			.parseInt(WeiboConfig.getValue("recordsPerRequest"));// 每次请求获取的记录条数，API限制最大设置为100

	public static final int MAX_PAGE = Integer.parseInt(WeiboConfig.getValue("maxPage"));// 当sinceId为1时，也就是第一次请求数据时，为了满足数据量，会多次API请求

	// 但是受限于一个token一小时150次请求，所以这里请求50页数据
	public static final int PULL_INTERVAL = Integer.parseInt(WeiboConfig
			.getValue("pullInterval")); // minutes

	static {
		accessTokens = WeiboConfig.getValue("accessToken").split(",");
	}

	public static final String ZK_ADDR = WeiboConfig.getValue("zkAddress");

	public static final int CONNECT_TIMEOUT = Integer.parseInt(WeiboConfig.getValue("zkConnectTime"));
}
