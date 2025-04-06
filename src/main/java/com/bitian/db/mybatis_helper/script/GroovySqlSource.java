package com.bitian.db.mybatis_helper.script;

import com.bitian.db.mybatis_helper.util.ContextMap;
import com.bitian.db.mybatis_helper.util.ReflectUtil;
import groovy.text.GStringTemplateEngine;
import groovy.text.SimpleTemplateEngine;
import groovy.text.StreamingTemplateEngine;
import groovy.text.Template;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.session.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 */
public class GroovySqlSource implements SqlSource {

    private final Configuration configuration;

    private final String sql;

    Template template ;

    public GroovySqlSource(Configuration configuration, String sql, Class<?> parameterType) {
        this.configuration=configuration;
        this.sql=sql;
        try{
            SimpleTemplateEngine engine = new SimpleTemplateEngine(this.getClass().getClassLoader());
            template=engine.createTemplate(sql);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        ContextMap bindings;
        if (parameterObject != null && !(parameterObject instanceof Map)) {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
            bindings = new ContextMap(metaObject, existsTypeHandler);
        } else {
            bindings = new ContextMap(null, false);
        }
        bindings.put(DynamicContext.PARAMETER_OBJECT_KEY, parameterObject);
        bindings.put(DynamicContext.DATABASE_ID_KEY, configuration.getDatabaseId());
        // 将groovy template 解析成 sql
        String response = template.make(bindings).toString();
        //解析成 StaticSqlSource
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
        Class<?> clazz = parameterType == null ? Object.class : parameterType;
        SqlSource sqlSource = sqlSourceParser.parse(response, clazz, new HashMap<>());
        return sqlSource.getBoundSql(parameterObject);
    }
}
