package com.bitian.db.mybatis.dto

/**
 * @author admin
 */
class Entity {
    String tableName
    Class<?> entityClass
    EntityColumn pkColumn
    List<EntityColumn> columns

    List<String> columnNames(){
        return columns.collect{it.column}
    }
}
