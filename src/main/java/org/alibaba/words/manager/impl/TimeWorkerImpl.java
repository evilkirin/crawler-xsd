package org.alibaba.words.manager.impl;

import org.alibaba.words.common.CrawlThread;
import org.alibaba.words.common.UtilConfig;


public class TimeWorkerImpl {
	
	public static void run() {
		for(int i = 0; i < UtilConfig.accessTokens.length; i++) {
			System.out.println("-------------start thread" +  i + "-------------");
			CrawlThread thread = new CrawlThread();
			thread.setIndex(i);
			new Thread(thread).start();
		}
	}	
	
	public static void main(String args[]) {
		run();
	}
}


