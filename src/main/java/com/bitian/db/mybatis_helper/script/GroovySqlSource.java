package com.bitian.db.mybatis_helper.script;

import com.bitian.db.mybatis.constants.Constant;
import com.bitian.db.mybatis.dto.Entity;
import com.bitian.db.mybatis.utils.MapperUtil;
import com.bitian.db.mybatis_helper.util.ContextMap;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.session.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author admin
 */
public class GroovySqlSource implements SqlSource {

    private final Configuration configuration;

    private String sql;

    private String originSql;

    SimpleTemplateEngine.SimpleTemplate template;

    MappedStatement statement;

    private void init(String tmpSql){
        this.originSql=tmpSql;
        this.sql= Constant.templateHeader+tmpSql;
        try{
            SimpleTemplateEngine engine = new SimpleTemplateEngine(this.getClass().getClassLoader());
            template=engine.createTemplate(this.sql);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GroovySqlSource(Configuration configuration, String sql, Class<?> parameterType) {
        this.configuration=configuration;
        this.init(sql);
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        //获取sqlSource对应的mappedStatement
        if(statement==null){
            statement=configuration.getMappedStatements().stream().filter(t->t.getSqlSource()==this).findFirst().get();
            Class<?> entityClass=MapperUtil.getEntityClass(statement);
            if(entityClass!=null){
                Entity entity=MapperUtil.generateEntity(statement.getId(),entityClass);
                //匹配上BTMapper中的方法
                if(statement.getId().endsWith(this.originSql)){
                    this.init(MapperUtil.generateSql(entity,this.originSql));
                }
            }
        }
        ContextMap bindings;
        if (parameterObject != null && !(parameterObject instanceof Map)) {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            boolean existsTypeHandler = configuration.getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass());
            bindings = new ContextMap(metaObject, existsTypeHandler);
        }else {
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
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        bindings.forEach((k,v)->boundSql.setAdditionalParameter(k.toString(),v));
        return boundSql;
    }
}
