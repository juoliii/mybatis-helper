package com.bitian.db.mybatis_helper;

import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TestMapper {
	@Select(value= {"select * from SYS_CODE where code_flow=#{codeflow}"})
	public Map<String,Object> selectById(@Param("codeflow")String codeflow);
}
