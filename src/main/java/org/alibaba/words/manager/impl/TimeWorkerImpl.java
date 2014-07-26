package org.alibaba.words.manager.impl;

import org.alibaba.words.common.Crawler;
import org.alibaba.words.common.UtilConfig;


public class TimeWorkerImpl {

	public static void run() {
		for(int i = 0; i < 1; i++) {
			Crawler thread = new Crawler(i);
			new Thread(thread).start();
		}
	}

	public static void main(String args[]) {
		run();
	}
}


