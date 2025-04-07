package com.bitian.db.mybatis_helper;

import java.util.List;
import java.util.Map;

import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper {
	@Select("select * from sys_user <% if(id>0) \n print abc ; this.testkey=123%> order by id desc")
	@Lang(GroovyLanguageDriver.class)
	List<Map<String,Object>> select(@Param("id")int id,@Param("test") String test);

	@Insert("insert into test(name) values(#{name})")
	void insert(String name);

	void insertBatch(List<String> list);

	@Select(" SELECT * FROM sys_user <% if(ids) \n print abc %> ")
	@Lang(GroovyLanguageDriver.class)
	List<Map<String,Object>> selectAll1(TestDto dto);
}
