package com.bitian.db.mybatis_helper.mapper;

import java.util.List;
import java.util.Map;

import com.bitian.common.dto.BaseForm;
import com.bitian.db.mybatis.constants.Constant;
import com.bitian.db.mybatis.pagination.Constants;
import com.bitian.db.mybatis_helper.TestDto;
import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import com.bitian.db.mybatis_helper.entity.SysUser;
import com.bitian.db.mybatis.mp.BTMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper extends BTMapper<SysUser> {
	@Select("select * from sys_user <% this.testkey=1; %> ${ sql(1,'where id=#{testkey}' ) } ")
	@Lang(GroovyLanguageDriver.class)
	List<SysUser> select(int id, String test, BaseForm form);

	@Select("select * from sys_user <% this.testkey=1L; %> ${ sql(id,'where id=#{testkey}' ) } ")
	@Lang(GroovyLanguageDriver.class)
	List<SysUser> select1(long id,List list);

	@Select("select * from sys_user <% this.testkey=1; %>  ")
	@Lang(GroovyLanguageDriver.class)
	List<SysUser> selectPage(BaseForm form);

	@Insert("insert into test(name) values(#{name})")
	void insert(String name);

	void insertBatch(List<String> list);

}
