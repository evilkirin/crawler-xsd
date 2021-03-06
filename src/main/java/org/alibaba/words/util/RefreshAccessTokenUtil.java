package org.alibaba.words.util;

import java.util.ArrayList;
import java.util.List;

import org.alibaba.words.domain.AccountInfo;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import weibo4j.Oauth;
import weibo4j.util.WeiboConfig;

public class RefreshAccessTokenUtil {

	private static String clientId;			// appKey
	@SuppressWarnings("unused")
	private static String clientSecret;		// appSecret
	private static List<AccountInfo> accountInfoList = new ArrayList<AccountInfo>();
	private static List<String> accessToken = new ArrayList<String>();				// 每个用户对应的access_token
	private static final Logger logger = Logger.getLogger(RefreshAccessTokenUtil.class);

	public static void main(String[] args){
		RefreshAccessTokenUtil.generate();
	}

	public List<String> getTokenList(){
		return accessToken;
	}

	public static void initData(){
		clientId = WeiboConfig.getValue("client_ID");
		//System.out.println("clientId:"+clientId);
		clientSecret = WeiboConfig.getValue("client_SERCRET");
		//System.out.println("clientSecret:"+clientSecret);
		String[] accountInfoString = WeiboConfig.getValue("accountInfo").split(",");
		for(int i=0;i<accountInfoString.length;i++){
			AccountInfo ai = new AccountInfo();
			String[] info = accountInfoString[i].split("-");
			ai.setUserId(info[0]);
			ai.setPwd(info[1]);
			accountInfoList.add(ai);
			//System.out.println(ai.getUserId()+"..."+ai.getPwd());
		}
	}

	private static void updateAccessToken(){
		StringBuffer buffer = new StringBuffer();
		for(int i=0;i<accessToken.size();i++){
			if(i==accessToken.size()-1){
				buffer.append(accessToken.get(i));
			}
			else{
				buffer.append(accessToken.get(i)+",");
			}
		}
		WeiboConfig.updateProperties("accessToken", "hehe");
	}

	// 授权，生成access_token
	// 使用情形：①程序初始化；②每隔一天左右重新授权access_token
	public static void generate() {
		initData();
		logger.info("用户授权中...");
		try {
			// https://api.weibo.com/oauth2/authorize?client_id=750123511&redirect_uri=https://api.weibo.com/oauth2/default.html&response_type=code
			String url = "https://api.weibo.com/oauth2/authorize";
			String redirectUri = "https://api.weibo.com/oauth2/default.html";

			for (int i = 0; i < accountInfoList.size(); i++) {

				// 构造授权的url参数
				PostMethod postMethod = new PostMethod(url);
				postMethod.addParameter("client_id", clientId);
				postMethod.addParameter("redirect_uri", redirectUri);
				postMethod.addParameter("userId", accountInfoList.get(i).getUserId());
				postMethod.addParameter("passwd", accountInfoList.get(i).getPwd());
				postMethod.addParameter("isLoginSina", "0");
	            postMethod.addParameter("action", "submit");
				postMethod.addParameter("response_type", "code");
				HttpMethodParams param = postMethod.getParams();
				param.setContentCharset("UTF-8");

				// 伪造头部域信息
				List<Header> headers = new ArrayList<Header>();
				headers.add(new Header("Referer", "https://api.weibo.com/oauth2/authorize?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&from=sina&response_type=code"));
				headers.add(new Header("Host", "api.weibo.com"));
				headers.add(new Header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:11.0) Gecko/20100101 Firefox/11.0"));

				// 发送HTTP请求
				HttpClient client = new HttpClient();
				client.getHostConfiguration().getParams().setParameter("http.default-headers", headers);
				client.executeMethod(postMethod);

				// 获取授权响应
				int status = postMethod.getStatusCode();
				if (status == 302) {
					Header location = postMethod.getResponseHeader("location");
					if (location != null) {
						String retUrl = location.getValue();
						int begin = retUrl.indexOf("code=");
						int end = retUrl.length();
						String code = retUrl.substring(begin + 5, end);
						if (code != null) {
							Oauth oauth = new Oauth();
							String token = oauth.getAccessTokenByCode(code).getAccessToken();
							accessToken.add(token);
							logger.info("user:"+accountInfoList.get(i).getUserId()+"  access_token：" + token);
						}
					}
				}
				else {
					logger.error("用户授权失败了！");
				}
			}

			updateAccessToken();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("授权发生异常！");
		}
	}

}