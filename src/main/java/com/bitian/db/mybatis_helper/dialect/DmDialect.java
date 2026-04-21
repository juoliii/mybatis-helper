package com.bitian.db.mybatis_helper.dialect;

/**
 * 达梦（DM）数据库分页方言。
 * <p>达梦数据库兼容 {@code LIMIT ... OFFSET ...} 语法。</p>
 */
public class DmDialect implements DialectFactory {

    @Override
    public String buildPaginationSql(String sql, int offset, int limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }
}
