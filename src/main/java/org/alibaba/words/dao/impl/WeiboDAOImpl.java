package org.alibaba.words.dao.impl;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.List;

import org.alibaba.words.dao.WeiboDAO;
import org.alibaba.words.domain.WeiboDO;
import org.alibaba.words.mapper.WeiboMapper;
import org.alibaba.words.util.MyBatisUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeiboDAOImpl implements WeiboDAO {
	private static final Logger logger = LoggerFactory.getLogger(WeiboDAOImpl.class);
	private static final WeiboDAOImpl instance = new WeiboDAOImpl();
	private static final String CONFIG_PATH = "config/mybatis_config.xml";
    private static SqlSessionFactory factory;

    private WeiboDAOImpl() {
    	InputStream stream = null;
		try {
			stream = Resources.getResourceAsStream(CONFIG_PATH);
		} catch (IOException e) {
			throw new InvalidParameterException("Fail to load the mybatis configuration.");
		}
        factory = new SqlSessionFactoryBuilder()
                .build(stream);
    }

    public static WeiboDAO getInstance() {
    	return instance;
    }

	@Override
	public int batchInsert(List<WeiboDO> list) {
		SqlSession session = factory.openSession();
        WeiboMapper mapper = session.getMapper(WeiboMapper.class);
        int count = 0, affectedLines = -1;
        try {
        	for(WeiboDO weibo : list) {
        		if(Thread.currentThread().isInterrupted())
        			return count;
				try {
					affectedLines = mapper.insertOneWeibo(weibo);
					session.commit();
				} catch(Throwable e) {
					logger.error("Fail to insert one weibo:" + weibo, e);
				}
				if(affectedLines > 0)
					count++;

			}
		} finally {
			session.close();
		}
		return count;
	}

}
