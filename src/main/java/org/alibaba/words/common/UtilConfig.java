package org.alibaba.words.common;

import weibo4j.util.WeiboConfig;

public class UtilConfig {

	public static String[] accessTokens;

	public static final int defaultSinceId = 1;

	static {
		accessTokens = WeiboConfig.getValue("accessToken").split(",");
	}
}
