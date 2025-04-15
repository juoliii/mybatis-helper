package com.bitian.db.mybatis.dto

/**
 * @author admin
 */
class Entity {
    String tableName
    Class<?> entityClass
    EntityColumn pkColumn
    List<EntityColumn> columns
}
