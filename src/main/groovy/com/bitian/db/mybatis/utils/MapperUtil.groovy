package com.bitian.db.mybatis.utils

import com.bitian.db.mybatis.dto.Entity
import com.bitian.db.mybatis.dto.EntityColumn
import com.bitian.db.mybatis.mp.BTMapper
import com.bitian.db.mybatis.mp.SqlMethods
import com.bitian.db.mybatis.pagination.Constants
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.mapping.ResultMap
import org.apache.ibatis.session.Configuration

import javax.persistence.*
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author admin
 */
class MapperUtil {

    static final Map<String,Class<?>> mapClassMap = new LinkedHashMap<>()
    static final Map<String,Class<?>> entityClassMap = new LinkedHashMap<>()
    static final Map<String,Entity> entityObjectMap = new LinkedHashMap<>()

    /**
     * 将mapperStatement的id转换成mapper的类
     * @param msId
     * @return
     */
    static Class<?> getMapperClass(String msId) {
        if (msId.indexOf(".") == -1) {
            throw new RuntimeException("ms id is error");
        }
        def className = msId.substring(0, msId.lastIndexOf("."))
        //由于一个接口中的每个方法都会进行下面的操作，因此缓存
        Class<?> mapperClass = mapClassMap.get(className)
        if(mapperClass != null){
            return mapperClass
        }
        mapperClass = Class.forName(className)
        mapClassMap.put(className, mapperClass);
        return mapperClass;
    }

    /**
     * 获取当前mapper注入的泛型实体类信息
     * mapper必须继承BTMapper类才行
     * @param ms
     * @return
     */
    static Class<?> getEntityClass(MappedStatement ms) {
        String msId = ms.getId();
        if (entityClassMap.containsKey(msId)) {
            return entityClassMap.get(msId)
        } else {
            Class<?> mapperClass = getMapperClass(msId)
            Type[] types = mapperClass.getGenericInterfaces()
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType t = (ParameterizedType) type
                    if (t.getRawType() == BTMapper.class || BTMapper.class.isAssignableFrom((Class<?>) t.getRawType())) {
                        Class<?> returnType = (Class<?>) t.getActualTypeArguments()[0]
                        entityClassMap.put(msId, returnType)
                        return returnType
                    }
                }
            }
        }
        return null
    }

    /**
     * 生成entity类信息
     * @param msId
     * @param entityClass
     * @return
     */
    static Entity generateEntity(MappedStatement ms,Class<?> entityClass){
        def isBaseType={
            it.isPrimitive() || it.isEnum() || it in [Integer, Long, Double, Float, Short, Byte, Character, Boolean,String]
        }
        Entity entity = null;
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            if (table.name()) {
                entity = new Entity(entityClass:entityClass)
                entity.tableName=table.name()

                List<EntityColumn> columns=entityClass.declaredFields.findAll {
                    Transient tt = it.getAnnotation(Transient.class)
                    !Modifier.isStatic(it.modifiers) && !Modifier.isTransient(it.getModifiers()) && tt==null && isBaseType(it.type)
                }.collect{
                    Column column = it.getAnnotation(Column.class)
                    GeneratedValue generatedValue = it.getAnnotation(GeneratedValue.class)
                    Id id = it.getAnnotation(Id.class)
                    String name=column?(column.name()?:it.name):it.name
                    EntityColumn.builder()
                            .pk(id!=null)
                            .generatedValue(generatedValue)
                            .property(it.name)
                            .column(name)
                            .javaType(it.type)
                            .updateable(column?column.updatable():true)
                            .build()
                }
                entity.columns=columns
                // 目前只支持单主键模式
                entity.pkColumn=columns.find {it.pk}
                if(entity.pkColumn){
                    // 此处目前只支持useGeneratedKeys 设置 主键值，后续支持selectkey方式
                    ms.metaClass.setProperty(ms,"keyProperties",[entity.pkColumn.property] as String[])
                    ms.metaClass.setProperty(ms,"keyColumns",[entity.pkColumn.column] as String[])
                }
            }
        }
        entityObjectMap.put(ms.getId(),entity)
        return entity
    }

    /**
     * 生成动态sql
     * @param entity
     * @param type
     * @return
     */
    static String generateSql(Entity entity,String type){
        String sql=switch (type){
            case SqlMethods.selectByPrimaryKey -> SqlMethods.genSelectByPrimaryKey(entity)
            case SqlMethods.deleteByPrimaryKey -> SqlMethods.genDeleteByPrimaryKey(entity)
            case SqlMethods.updateByPrimaryKeySelective -> SqlMethods.genUpdateByPrimaryKeySelective(entity)
            case SqlMethods.insertSelective-> SqlMethods.genInsertSelective(entity)
        }
        assert sql!=""
        return sql
    }

    static MappedStatement buildPaginationCountMappedStatement(MappedStatement ms) {
        String countId = ms.getId() + Constants.COUNT_MS_SUFFIX
        Configuration configuration = ms.getConfiguration()
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, countId, ms.getSqlSource(), ms.getSqlCommandType())
        builder.resource(ms.getResource())
        builder.fetchSize(ms.getFetchSize())
        builder.statementType(ms.getStatementType())
        builder.timeout(ms.getTimeout())
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(Collections.singletonList(new ResultMap.Builder(configuration, ms.getId(), Long.class, Collections.emptyList()).build()))
        builder.resultSetType(ms.getResultSetType())
        builder.cache(ms.getCache())
        builder.flushCacheRequired(ms.isFlushCacheRequired())
        builder.useCache(ms.isUseCache())
        builder.build()
    }

}
