package com.bitian.db.mybatis_helper.meta;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体类的数据库映射元数据，包含表名和所有列信息
 */
public class EntityMetadata {

    /** 数据库表名 */
    private final String tableName;

    /** 所有列映射信息 */
    private final List<ColumnInfo> columns;

    /** 主键列映射信息（缓存） */
    private final List<ColumnInfo> idColumns;

    public EntityMetadata(String tableName, List<ColumnInfo> columns) {
        this.tableName = tableName;
        this.columns = Collections.unmodifiableList(columns);
        this.idColumns = Collections.unmodifiableList(
                columns.stream().filter(ColumnInfo::isId).collect(Collectors.toList())
        );
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public List<ColumnInfo> getIdColumns() {
        return idColumns;
    }

    /**
     * 获取非主键列
     */
    public List<ColumnInfo> getNonIdColumns() {
        return columns.stream().filter(c -> !c.isId()).collect(Collectors.toList());
    }

    /**
     * 校验实体类必须包含至少一个 @Id 标记的主键字段
     */
    public void requireId() {
        if (idColumns.isEmpty()) {
            throw new RuntimeException("Entity class mapped to table '" + tableName
                    + "' has no @Id annotated field. update/delete by entity requires a primary key.");
        }
    }
}
