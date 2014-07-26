package org.alibaba.words.common;

import java.util.List;

import org.alibaba.words.dao.WeiBoDAO;
import org.alibaba.words.dao.impl.WeiBoDAOImpl;
import org.alibaba.words.domain.WeiBoDO;
import org.alibaba.words.manager.CrawlDataManager;
import org.alibaba.words.manager.impl.CrawlDataManagerImpl;
import org.apache.log4j.Logger;

public class CrawlThread implements Runnable{
	
	private static final Logger log = Logger.getLogger(CrawlThread.class);
	private static final long oneHour = 65 * 60 * 1000;
	private static final long fiveMinutes = 5 * 60 * 1000;
	private static final long oneMinute = 1 * 60 * 1000;
	private int index;
	
    public void run() {
    	System.out.println("------Thread " + index + " begin----------");
    	while(true) {  		
    		CrawlDataManager crawlDataManager = new CrawlDataManagerImpl();		
    		WeiBoDAO WeiBoDAO = new WeiBoDAOImpl();
    		TeResult<List<WeiBoDO>> teResult = crawlDataManager.getDataFromWeb(UtilConfig.accessTokens[index], 
        			UtilConfig.nickNames[index], UtilConfig.sinceIds[index]);
    		if (teResult == null) {
    			log.error("get data from web error");
    			try {
    				Thread.sleep(oneMinute);
    			} catch (InterruptedException e) {
    				log.error("sleep error", e);
    				e.printStackTrace();
    			}
    			System.out.println("------Thread " + index + " sleep five minutes----------");
    			continue;
    		}
    		
    		if (teResult.isSuccess() == false && teResult.getErrorCode() == UtilConfig.ERROR_CODE_NEED_SLEEP) {
    			try {
    				Thread.sleep(oneHour);
    			} catch (InterruptedException e) {
    				log.error("sleep error", e);
    				e.printStackTrace();
    			}
    			continue;
    		}
    		
    		if (teResult.isSuccess() == false && teResult.getErrorCode() == UtilConfig.ERROR_CODE_TIME_EXCEPTION) {
    			try {
    				Thread.sleep(oneMinute);
    			} catch (InterruptedException e) {
    				log.error("sleep error", e);
    				e.printStackTrace();
    			}
    			System.out.println("------Thread " + index + " sleep five minutes----------");
    			continue;
    		}
    		
    		if (teResult.isSuccess() == false && teResult.getErrorCode() == UtilConfig.ERROR_CODE_METHOD_ERROR) {
    			System.out.println("------Thread " + index + " end----------");
    			return;
    		}
    		
    		long sinceIdIndex = UtilConfig.sinceIds[index];
    		for (WeiBoDO weiBoDO : teResult.getModel()) {
    			WeiBoDAO.insert(weiBoDO);
    			sinceIdIndex = sinceIdIndex > weiBoDO.getWeiBoId() ? sinceIdIndex : weiBoDO.getWeiBoId();
    		}		

    		UtilConfig.sinceIds[index] = UtilConfig.sinceIds[index] > sinceIdIndex ? UtilConfig.sinceIds[index] : sinceIdIndex;
    		try {
				Thread.sleep(oneMinute);
			} catch (InterruptedException e) {
				log.error("sleep error", e);
				e.printStackTrace();
			}
			System.out.println("------Thread " + index + " sleep five minutes----------");
    	} 	
    }

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
}
