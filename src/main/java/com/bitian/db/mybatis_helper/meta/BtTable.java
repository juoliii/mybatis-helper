package com.bitian.db.mybatis_helper.meta;

/**
 * 强类型 SQL DSL 核心元模型：表
 */
public abstract class BtTable {

    protected final String tableName;
    protected final String alias;

    public BtTable(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
    }

    public String getTableName() {
        return tableName;
    }

    public String getAlias() {
        return alias;
    }

    /**
     * 创建该表对应的列定义对象
     */
    protected BtColumn createColumn(String columnName) {
        return new BtColumn(this, columnName);
    }
}
