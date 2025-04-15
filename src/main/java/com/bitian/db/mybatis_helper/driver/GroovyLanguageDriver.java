package com.bitian.db.mybatis_helper.driver;

import com.bitian.common.exception.CustomException;
import com.bitian.db.mybatis.MybatisInject;
import com.bitian.db.mybatis_helper.script.GroovySqlSource;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

/**
 * @author admin
 */
public class GroovyLanguageDriver implements LanguageDriver {

    @Override
    public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
    }

    @Override
    public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
        return createSqlSource(configuration,script.getStringBody(),parameterType);
    }

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        if (script.startsWith("<script>")) {
            throw new CustomException(" this is groovy template , not xml ");
        }
        script = PropertyParser.parse(script, configuration.getVariables());
        return new GroovySqlSource(configuration,script,parameterType);
    }
}
