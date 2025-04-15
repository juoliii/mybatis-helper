package com.bitian.db.mybatis.dto

import groovy.transform.builder.Builder

/**
 * @author admin
 */
@Builder
class EntityColumn {
    String property
    String column
    boolean updateable
    boolean pk
    Class<?> javaType
}
