package com.bitian.db.mybatis_helper.meta;

/**
 * 强类型 SQL DSL 核心元模型：列
 */
public class BtColumn {

    private final BtTable table;
    private final String columnName;
    private String columnAlias;

    public BtColumn(BtTable table, String columnName) {
        this.table = table;
        this.columnName = columnName;
    }

    /**
     * 为当前列起别名，通常仅在 SELECT 投影时使用
     * @param alias 字段别名
     * @return 全新的带有别名的 BtColumn 实例（保护静态单例常量）
     */
    public BtColumn as(String alias) {
        BtColumn cloned = new BtColumn(this.table, this.columnName);
        cloned.columnAlias = alias;
        return cloned;
    }

    public BtTable getTable() {
        return table;
    }

    public String getColumnName() {
        return columnName;
    }

    /**
     * 获取组装好的完整 SQL 片段，如 "u.user_name"
     */
    public String getSql() {
        if (table.getAlias() != null && !table.getAlias().isEmpty()) {
            return table.getAlias() + "." + columnName;
        }
        return columnName;
    }

    /**
     * 获取用于 SELECT 的 SQL 片段（带 AS 别名），如 "u.user_name AS userName"
     */
    public String getSelectSql() {
        String baseSql = getSql();
        if (columnAlias != null && !columnAlias.isEmpty()) {
            return baseSql + " AS " + columnAlias;
        }
        return baseSql;
    }
}
