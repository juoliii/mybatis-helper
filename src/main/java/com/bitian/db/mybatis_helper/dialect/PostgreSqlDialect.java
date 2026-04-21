package com.bitian.db.mybatis_helper.dialect;

/**
 * PostgreSQL / KingbaseES / openGauss 分页方言。
 * <p>使用 {@code LIMIT ... OFFSET ...} 语法。</p>
 */
public class PostgreSqlDialect implements DialectFactory {

    @Override
    public String buildPaginationSql(String sql, int offset, int limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }
}
