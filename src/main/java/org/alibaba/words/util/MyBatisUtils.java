package org.alibaba.words.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class MyBatisUtils {

    private static final String CONFIG_PATH = "config/mybatis_config.xml";
    private static SqlSessionFactory factory;

    static {
    	InputStream stream = null;
		try {
			stream = Resources.getResourceAsStream(CONFIG_PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}
        factory = new SqlSessionFactoryBuilder()
                .build(stream);
    }

    /*
     * 获取数据库访问链接
     */
    public static SqlSession getSqlSession() {
        SqlSession session = null;
        try {
            session = factory.openSession(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return session;
    }

    public static SqlSessionFactory getFactory() {
    	return factory;
    }

    /*
     * 获取数据库访问链接
     */
    public static void closeSession(SqlSession session) {
        session.close();
    }
}

