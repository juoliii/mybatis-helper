package com.bitian.db.mybatis_helper.mp;

import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Select;

/**
 * @author admin
 */
public interface BTMapper<T> {

    @Select("select * from sys_user where id=#{id}")
    @Lang(GroovyLanguageDriver.class)
    T selectByPrimaryKey(Object id);

    int deleteByPrimaryKey(Object id);

    int updateByPrimaryKeySelective(T entity);

    int insertSelective(T entity);




}
