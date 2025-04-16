package com.bitian.db.mybatis.mp;

import com.bitian.db.mybatis.utils.SqlMethods;
import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import org.apache.ibatis.annotations.*;

/**
 * @author admin
 */
public interface BTMapper<T> {

    @Select("")
    @Lang(GroovyLanguageDriver.class)
    T selectByPrimaryKey(Object id);

    @Delete("")
    @Lang(GroovyLanguageDriver.class)
    int deleteByPrimaryKey(Object id);

    @Update("")
    @Lang(GroovyLanguageDriver.class)
    int updateByPrimaryKeySelective(T entity);

    @Insert("")
    @Lang(GroovyLanguageDriver.class)
    @Options(useGeneratedKeys = true)
    int insertSelective(T entity);




}
