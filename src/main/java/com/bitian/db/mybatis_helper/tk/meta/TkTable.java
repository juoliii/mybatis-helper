package com.bitian.db.mybatis_helper.tk.meta;

/**
 * 强类型 SQL DSL 核心元模型：表
 */
public abstract class TkTable {

    protected final String tableName;
    protected final String alias;

    public TkTable(String tableName, String alias) {
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
    protected TkColumn createColumn(String columnName) {
        return new TkColumn(this, columnName);
    }
}
