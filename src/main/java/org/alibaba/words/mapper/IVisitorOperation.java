package org.alibaba.words.mapper;

import org.alibaba.words.domain.Visitor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface IVisitorOperation {

	@Select("select * from Visitor where id=#{id} and Status>0 order by Id")
	public Visitor basicQuery(int id);

	@Insert("INSERT INTO Visitor (`Name`,`Email`,`Status`,`CreateTime`) VALUES (#{name}, #{email}, #{status}, #{createTime})")
	public int insertOneVisitor(Visitor visitor);
}
