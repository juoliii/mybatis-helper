package com.bitian.db.mybatis_helper.mp;

import com.bitian.db.mybatis.utils.SqlMethods;
import com.bitian.db.mybatis_helper.driver.GroovyLanguageDriver;
import org.apache.ibatis.annotations.*;

/**
 * @author admin
 */
public interface BTMapper<T> {

    @Select(SqlMethods.selectByPrimaryKey)
    @Lang(GroovyLanguageDriver.class)
    T selectByPrimaryKey(Object id);

    @Delete(SqlMethods.deleteByPrimaryKey)
    @Lang(GroovyLanguageDriver.class)
    int deleteByPrimaryKey(Object id);

    @Update(SqlMethods.updateByPrimaryKeySelective)
    @Lang(GroovyLanguageDriver.class)
    int updateByPrimaryKeySelective(T entity);

    @Insert(SqlMethods.insertSelective)
    @Lang(GroovyLanguageDriver.class)
    @Options(useGeneratedKeys = true,keyProperty = "id")
    int insertSelective(T entity);




}
