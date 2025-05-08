package com.bitian.db.mybatis_helper.script;

import com.bitian.db.mybatis.constants.Constant;
import com.bitian.db.mybatis.dto.ContextMap;
import com.bitian.db.mybatis.dto.Entity;
import com.bitian.db.mybatis.mp.SqlMethods;
import com.bitian.db.mybatis.utils.MapperUtil;
import org.apache.commons.lang3.StringUtils;
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
        //BTMapper自动注入的单表方法会在此处第一次调用方法的时候进行初始化
        if(StringUtils.isEmpty(this.originSql) && statement==null){
            //获取sqlSource对应的mappedStatement
//            statement=configuration.getMappedStatements().stream().filter(t->t instanceof MappedStatement).filter(t->t.getSqlSource()==this).findFirst().get();
            for (Object tmp : configuration.getMappedStatements()) {
                if(tmp instanceof MappedStatement && ((MappedStatement)tmp).getSqlSource()==this){
                    statement= (MappedStatement) tmp;
                    break;
                }
            }
            Class<?> entityClass=MapperUtil.getEntityClass(statement);
            if(entityClass!=null){
                Entity entity=MapperUtil.generateEntity(statement,entityClass);
                String msId=statement.getId();
                String methodName=msId.substring(msId.lastIndexOf(".")+1);
                //匹配上BTMapper中的方法；originSql值为空，并且是指定的方法名才能匹配
                if(StringUtils.isEmpty(this.originSql) && SqlMethods.methods.contains(methodName)){
                    //第一次调用自动生成的方法的时候修改sqlsource，会比较慢，后续会很快
                    this.init(MapperUtil.generateSql(entity,methodName));
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
