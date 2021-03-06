package com.bitian.db.mybatis_helper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper {
	@Select(value= {" select * from banner where id>#{id} order by id desc"})
	public List<Map<String,Object>> select(@Param("id")int id);
}
