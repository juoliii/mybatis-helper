package com.bitian.db.mybatis_helper.dialect;

/**
 * 数据库分页方言接口。
 * <p>
 * 不同数据库对分页 SQL 的语法各不相同，通过实现此接口来适配各种数据库。
 * 内置支持 MySQL、PostgreSQL、Oracle、SQL Server、达梦等主流数据库，
 * 也可通过 {@link DialectRegistry#registerDialect(String, DialectFactory)} 注册自定义方言。
 * </p>
 */
public interface DialectFactory {

    /**
     * 将原始 SQL 包装为分页查询 SQL
     *
     * @param sql    原始 SQL
     * @param offset 偏移量（从 0 开始）
     * @param limit  每页条数
     * @return 带分页的 SQL
     */
    String buildPaginationSql(String sql, int offset, int limit);

    /**
     * 将原始 SQL 包装为 COUNT 查询 SQL（用于获取总记录数）。
     * <p>
     * 默认使用 {@link CountSqlOptimizer} 进行智能优化：
     * <ul>
     *     <li>移除 ORDER BY / LIMIT / OFFSET</li>
     *     <li>简单查询直接替换 SELECT 列为 COUNT(*)</li>
     *     <li>复杂查询（DISTINCT / GROUP BY / UNION）包装为子查询</li>
     * </ul>
     * 子类可覆写此方法以提供特定数据库的优化实现。
     * </p>
     *
     * @param sql 原始 SQL
     * @return COUNT 查询 SQL
     */
    default String buildCountSql(String sql) {
        return CountSqlOptimizer.toCountSql(sql);
    }
}
