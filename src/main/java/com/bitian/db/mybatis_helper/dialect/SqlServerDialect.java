package com.bitian.db.mybatis_helper.dialect;

/**
 * SQL Server 分页方言（SQL Server 2012+）。
 * <p>
 * 使用 {@code OFFSET ... ROWS FETCH NEXT ... ROWS ONLY} 语法。
 * 注意：SQL Server 的 OFFSET-FETCH 要求必须有 ORDER BY 子句，
 * 如果原始 SQL 没有 ORDER BY，会自动追加 {@code ORDER BY (SELECT NULL)}。
 * </p>
 */
public class SqlServerDialect implements DialectFactory {

    @Override
    public String buildPaginationSql(String sql, int offset, int limit) {
        // SQL Server 的 OFFSET-FETCH 必须配合 ORDER BY
        String upperSql = sql.toUpperCase();
        if (!upperSql.contains("ORDER BY")) {
            sql = sql + " ORDER BY (SELECT NULL)";
        }
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
}
