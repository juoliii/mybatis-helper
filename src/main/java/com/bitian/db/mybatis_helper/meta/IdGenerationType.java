package com.bitian.db.mybatis_helper.meta;

/**
 * 主键生成策略枚举
 */
public enum IdGenerationType {

    /** 不自动生成，用户提供主键值 */
    NONE,

    /** 数据库自增（MySQL AUTO_INCREMENT、PostgreSQL SERIAL 等），插入后回填 */
    IDENTITY,

    /** 应用层生成 UUID */
    UUID,

    /** 执行指定 SQL 获取主键值（如 Oracle 序列：SELECT seq.nextval FROM DUAL） */
    SQL,

    /** 自定义生成器，需实现 {@link GenId} 接口 */
    CUSTOM
}
