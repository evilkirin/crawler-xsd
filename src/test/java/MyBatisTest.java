import java.util.List;

import org.alibaba.words.domain.Visitor;
import org.alibaba.words.mapper.IVisitorOperation;
import org.alibaba.words.util.MyBatisUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

import com.google.common.collect.Lists;

public class MyBatisTest {

	@Test
	public void testUsabitlity() {
		int id = 1;
		SqlSession session = MyBatisUtils.getSqlSession();
        try {
            IVisitorOperation operation = session.getMapper(IVisitorOperation.class);
            Visitor visitor = operation.basicQuery(id);
            MyBatisUtils.closeSession(session);
            System.out.println(visitor);
        } catch (Exception e) {
            // TODO: handle exception
        }
	}

	@Test
	public void testInsertData () {
		Visitor visitor = new Visitor("test3", "xieyu@test.com");
		SqlSession session = MyBatisUtils.getSqlSession();
        try {
            IVisitorOperation operation = session.getMapper(IVisitorOperation.class);
            System.out.println(operation.insertOneVisitor(visitor));
            MyBatisUtils.closeSession(session);
        } catch (Exception e) {
            // TODO: handle exception
        	System.out.println(e);
        }
	}

	private void batchInsert(List<Visitor> list) {
		SqlSession session = MyBatisUtils.getFactory().openSession(ExecutorType.BATCH);
        IVisitorOperation operation = session.getMapper(IVisitorOperation.class);
		try {
			for(Visitor v : list) {
				operation.insertOneVisitor(v);
			}
			session.flushStatements();
			session.clearCache();
			session.commit();
		} catch(Exception e) {
			System.out.println(e);
			session.rollback();
		} finally {
			session.close();
		}
	}

	private List<Visitor> getListOfVisitor () {
		List<Visitor> list = Lists.newArrayList();
		list.add(new Visitor("v1", "visitor@email.com"));
		list.add(new Visitor("v2", "visitor@email.com"));
		list.add(new Visitor("v3", "visitor@email.com"));
		list.add(new Visitor("v4", "visitor@email.com"));
		return list;
	}

	@Test
	public void testBatchInsert () {
		batchInsert(getListOfVisitor());
	}

}
