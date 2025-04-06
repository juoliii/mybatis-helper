package com.bitian.db.mybatis_helper;

import java.util.List;
import java.util.Map;

import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper {
	@Select("select * from sys_user where id>#{id} order by id desc")
	List<Map<String,Object>> select(@Param("id")int id,@Param("test") String test);

	@Insert("insert into test(name) values(#{name})")
	void insert(String name);

	void insertBatch(List<String> list);

	@Select("<% import com.bitian.db.mybatis_helper.util.ReflectUtil;%> \n SELECT * FROM sys_user <% if(id>0) \n print 'order by id' %> ")
	@Lang(GroovyLanguageDriver.class)
	List<Map<String,Object>> selectAll1(TestDto dto);
}
