package org.alibaba.words.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.dao.impl.WeiboDAOImpl;
import org.alibaba.words.domain.WeiboDO;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weibo4j.Timeline;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.model.WeiboException;

public class Crawler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

	private int index;
	private String accessToken, zkNode;
	private long sinceId;
	private Timeline timeline;
	private final ZooKeeper zk;
	private static final WeiboDAO weiboDAO = WeiboDAOImpl.getInstance();

	public Crawler(int index, String accessToken, ZooKeeper zk) {
		this.index = index;
		this.accessToken = accessToken;
		sinceId = 1;
		timeline = new Timeline();
		this.zk = zk;
		zkNode = Config.SLOT_ROOT + "/" + accessToken;
	}

	public void init() throws KeeperException, InterruptedException {
		Stat stat = null;
		byte[] raw = null;
		long data = 0l;
		stat = zk.exists(zkNode, false);
		if(stat != null) {
			raw = zk.getData(zkNode, false, stat);
			try {
				data = Long.parseLong(new String(raw));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		} else {
			raw = String.valueOf(1l).getBytes();
			zk.setData(zkNode, raw, -1);
		}
		if(data > 0)
			sinceId = data;
	}

	public void run() {
		logger.info("#Crawler " + index + " begins");

		Thread currentThread = Thread.currentThread();

		try {
			while (!currentThread.isInterrupted()) {
				boolean initialLoad = sinceId == Config.DEFAULT_SINCE_ID;

				try {
					List<WeiboDO> list = queryWeiboList(getPage(sinceId, 1));
					updateSinceId(list);
					int recordsInserted = weiboDAO.batchInsert(list);
					logger.info("#Crawler " + index + " : " + recordsInserted
							+ " weibo out of " + list.size() + " inserted to the db.");

					if (initialLoad) {
						loadMoreThanJustRecentWeibo();
					}
				} catch (WeiboException e) {
					handleException(e);
				}
				logger.info("#Crawler " + index + " sleep for " + Config.PULL_INTERVAL + " minutes");
				TimeUnit.MINUTES.sleep(Config.PULL_INTERVAL);
			}
		} catch (InterruptedException e) {
			logger.info("#Crawler " + index + " exits.");
		} catch (RuntimeException re) {
			logger.info("Unexpected runtime exception.", re);
		}
	}

	private void loadMoreThanJustRecentWeibo() throws InterruptedException {
		List<WeiboDO> list;
		int recordsInserted;
		for (int i = 2; i <= Config.MAX_PAGE; i++) {
			try {
				list = queryWeiboList(getPage(Config.DEFAULT_SINCE_ID, i));

				recordsInserted = weiboDAO.batchInsert(list);
				logger.info("#Crawler " + index + " : " + recordsInserted + " weibo out of "
						+ list.size() + " inserted to the db.");

				int resultCount = list.size();
				// 表示数据不够maxPage*count
				// 在实际测试微博api的过程中发现可能总条数显示1992条，每页100条，但是指定页数后每页取回的
				// 数量可能不满100，比如可能第2页有100条，第3页有98条，第4页有95条，所以这里我们设了一个容忍值，
				// 认为大于等于count-10条这一页就算取满，还需要往下一页取数据。
				if (resultCount < Config.MAX_RECORDS_PER_REQUEST - 10) {
					break;
				}
			} catch (WeiboException e) {
				handleException(e);
			}
		}
	}

	private void handleException(WeiboException e) throws InterruptedException {
		int errorCode = e.getErrorCode();
		logger.error("fail to query weibo info accessToken = " + accessToken + ", sinceId = "
				+ sinceId, e);
		if (isRequestTooFrequent(errorCode)) {
			logger.error("#Crawler " + index + " : Too many request, some rest is needed.");
			TimeUnit.HOURS.sleep(1);
		} else if (isRequestTimeout(errorCode)) {
			logger.error("#Crawler " + index + " : Timeout, resume later.");
			TimeUnit.MINUTES.sleep(5);
		} else {
			logger.error("#Crawler " + index + " : Invalid parameters. Please check before execute.");
			throw new RuntimeException("Invalid configuration.", e);
		}
	}

	private boolean isRequestTimeout(int errorCode) {
		return 10010 == errorCode || 10009 == errorCode;
	}

	private boolean isRequestTooFrequent(int errorCode) {
		return 10004 == errorCode || 10022 == errorCode || 10023 == errorCode
				|| 10024 == errorCode;
	}

	private Paging getPage(long currentSince, int page) {
		Paging p = new Paging();
		p.setCount(Config.MAX_RECORDS_PER_REQUEST);
		p.setPage(page);
		p.setSinceId(currentSince);
		return p;
	}

	private List<WeiboDO> queryWeiboList(Paging page) throws WeiboException {
		List<WeiboDO> weiboDOList = new ArrayList<WeiboDO>();

		timeline.client.setToken(accessToken);
		StatusWapper status = timeline.getFriendsTimeline(0, 1, page);
		for (Status s : status.getStatuses()) {
			WeiboDO weiBoDO = packWeiBoDO(s);
			weiboDOList.add(weiBoDO);
		}
		return weiboDOList;
	}

	public WeiboDO packWeiBoDO(Status s) {
		WeiboDO weiBoDO = new WeiboDO();
		weiBoDO.setWeiboId(s.getIdstr());
		weiBoDO.setWeiboText(s.getText());
		weiBoDO.setCreatedTime(s.getCreatedAt());
		weiBoDO.setUserId(s.getUser().getId());
		weiBoDO.setNickName(s.getUser().getScreenName());
		weiBoDO.setRepostsCount(s.getRepostsCount());
		weiBoDO.setCommentsCount(s.getCommentsCount());
		return weiBoDO;
	}

	private void updateSinceId(List<WeiboDO> list) throws InterruptedException {
		long old = sinceId, newSinceId = -1;
		for (WeiboDO weiBoDO : list) {
			newSinceId = newSinceId > weiBoDO.getWeiboId() ? newSinceId : weiBoDO.getWeiboId();
		}
		if(newSinceId - sinceId > 50) {
			byte[] raw = String.valueOf(sinceId).getBytes();
			try {
				zk.setData(zkNode, raw, -1);
			} catch (KeeperException e) {
				logger.warn("It seems the sinceId sync operation with zk is failed.", e);
			}
		}
		sinceId = old > newSinceId ? old : newSinceId;
	}
}
