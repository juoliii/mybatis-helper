package com.bitian.db.mybatis_helper.mapper;

import java.util.List;
import java.util.Map;

import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import com.bitian.db.mybatis_helper.entity.SysUser;
import com.bitian.db.mybatis.mp.BTMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper extends BTMapper<SysUser> {
	@Select("select * from sys_user <% this.testkey=10; %> <%=sql(0,\"where id=#{testkey}\") %> ")
	@Lang(GroovyLanguageDriver.class)
	List<Map<String,Object>> select(@Param("id")int id,@Param("test") String test);

	@Insert("insert into test(name) values(#{name})")
	void insert(String name);

	void insertBatch(List<String> list);

}
