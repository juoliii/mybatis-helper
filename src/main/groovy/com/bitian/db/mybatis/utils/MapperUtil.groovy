package com.bitian.db.mybatis.utils

import com.bitian.db.mybatis.dto.Entity
import com.bitian.db.mybatis.dto.EntityColumn
import com.bitian.db.mybatis_helper.mp.BTMapper
import org.apache.ibatis.cache.Cache
import org.apache.ibatis.mapping.MappedStatement

import javax.persistence.Column
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Transient
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author admin
 */
class MapperUtil {

    static final Map<String,Class<?>> CLASS_CACHE = new LinkedHashMap<>()

    static final Map<String,Class<?>> entityClassMap = new LinkedHashMap<>()

    /**
     * 将mapperStatement的id转换成mapper的类
     * @param msId
     * @return
     */
    static Class<?> getMapperClass(String msId) {
        if (msId.indexOf(".") == -1) {
            throw new RuntimeException("ms id is error");
        }
        def className = msId.substring(0, msId.lastIndexOf("."));
        //由于一个接口中的每个方法都会进行下面的操作，因此缓存
        Class<?> mapperClass = (Class<?>) CLASS_CACHE.get(className);
        if(mapperClass != null){
            return mapperClass;
        }

        mapperClass = Class.forName(className)
        CLASS_CACHE.put(className, mapperClass);
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

    static Entity generateEntity(Class<?> entityClass){
        def isBaseType={
            it.isPrimitive() || it in [Integer, Long, Double, Float, Short, Byte, Character, Boolean,String]
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
                    Id id = it.getAnnotation(Id.class)
                    String name=column?(column.name()?:it.name):it.name
                    EntityColumn
                            .builder()
                            .pk(!!id)
                            .property(it.name)
                            .column(name)
                            .updateable(column?column.updatable():true)
                            .build()
                }
                entity.columns=columns
                // 目前只支持单主键模式
                entity.pkColumn=columns.find {it.pk}
            }
        }
        return entity
    }



}
