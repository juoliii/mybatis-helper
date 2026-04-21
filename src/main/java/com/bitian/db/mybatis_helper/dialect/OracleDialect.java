package com.bitian.db.mybatis_helper.dialect;

/**
 * Oracle 分页方言（Oracle 12c+）。
 * <p>使用 {@code OFFSET ... ROWS FETCH NEXT ... ROWS ONLY} 语法。</p>
 */
public class OracleDialect implements DialectFactory {

    @Override
    public String buildPaginationSql(String sql, int offset, int limit) {
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }
}
