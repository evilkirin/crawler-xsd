package org.alibaba.words.common;

import weibo4j.util.WeiboConfig;

public class UtilConfig {

	public static String[] accessTokens;
	public static String[] nickNames;
	public static long[] sinceIds;

	static {
		accessTokens = WeiboConfig.getValue("accessToken").split(",");
		nickNames = WeiboConfig.getValue("nickName").split(",");
		sinceIds = new long[nickNames.length];
		for (int i = 0; i < sinceIds.length; i++) {
			sinceIds[i] = 1;
		}
	}

	public static final String ERROR_CODE_NEED_SLEEP = "E001"; //主要处理接口调用次数过多
	public static final String ERROR_CODE_TIME_EXCEPTION = "E002";  //处理接口超时
	public static final String ERROR_CODE_METHOD_ERROR = "E003";  //方法、参数等错误，调用不成功

}
