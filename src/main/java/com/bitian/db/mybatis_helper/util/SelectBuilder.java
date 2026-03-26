package com.bitian.db.mybatis_helper.util;

import com.bitian.db.mybatis_helper.tk.meta.TkColumn;
import com.bitian.db.mybatis_helper.tk.meta.TkTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 完整 SQL 构建器，支持 SELECT / FROM / JOIN / WHERE / EXISTS / GROUP BY / HAVING / ORDER BY / LIMIT。
 * <p>
 * 使用示例：
 * <pre>
 * SelectBuilder sb = new SelectBuilder()
 *     .select("u.id", "u.name", "d.name AS deptName")
 *     .from("sys_user u")
 *     .leftJoin("sys_dept d").on("u.dept_id = d.id")
 *     .where()
 *         .eq("u.status", 1)
 *         .like("u.name", "test")
 *     .and(w -> w.eq("a", 1).eq("b", 2))
 *     .exists(new SelectBuilder().select("1").from("sys_role r").where().apply("r.user_id = u.id"))
 *     .groupBy("u.dept_id")
 *     .having("COUNT(*) > 1")
 *     .orderByDesc("u.create_time")
 *     .limit(20);
 *
 * String sql = sb.toSql();
 * Map&lt;String, Object&gt; params = sb.getParams();
 * </pre>
 */
public class SelectBuilder {

    private final List<String> selectColumns = new ArrayList<>();
    private String fromTable;
    private final List<String> joins = new ArrayList<>();
    private final QueryWrapper queryWrapper = new QueryWrapper();
    private final List<String> groupBys = new ArrayList<>();
    private String havingClause;
    private final List<String> orderBys = new ArrayList<>();
    private Integer limitCount;
    private Integer offsetCount;
    private String alias;

    public static SelectBuilder n(){
        return new SelectBuilder();
    }

    /**
     * 为当前构建的 SelectBuilder（通常作为子查询使用时）设置别名
     */
    public SelectBuilder as(String alias) {
        this.alias = alias;
        return this;
    }

    // ============================
    // SELECT
    // ============================

    public SelectBuilder select(String... columns) {
        for (String col : columns) {
            selectColumns.add(col);
        }
        return this;
    }

    /**
     * 选择查询的列（强类型支持）
     */
    public SelectBuilder select(TkColumn... columns) {
        for (TkColumn col : columns) {
            selectColumns.add(col.getSelectSql());
        }
        return this;
    }

    /**
     * 选择一个子查询，如果设置了 alias() 则会自动带上 AS 别名
     * 例如：.select(new SelectBuilder().select("COUNT(*)").from("t").as("cnt"))
     */
    public SelectBuilder select(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " AS " + subBuilder.alias : "";
        selectColumns.add("(" + subBuilder.toSql() + ")" + aliasStr);
        return this;
    }

    // ============================
    // FROM
    // ============================

    public SelectBuilder from(String table) {
        this.fromTable = table;
        return this;
    }

    /**
     * 将一个强类型表模型作为 FROM 表
     */
    public SelectBuilder from(TkTable table) {
        this.fromTable = table.getTableName() + " " + table.getAlias();
        return this;
    }

    /**
     * 将一个子查询作为 FROM 表，如果设置了 alias() 则会自动带上别名
     */
    public SelectBuilder from(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " " + subBuilder.alias : "";
        this.fromTable = "(" + subBuilder.toSql() + ")" + aliasStr;
        return this;
    }

    // ============================
    // JOIN (返回 JoinClause 以链式调用 .on())
    // ============================

    public JoinClause leftJoin(String table) {
        return new JoinClause(this, "LEFT JOIN", table);
    }

    public JoinClause leftJoin(TkTable table) {
        return new JoinClause(this, "LEFT JOIN", table.getTableName() + " " + table.getAlias());
    }

    public JoinClause leftJoin(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " " + subBuilder.alias : "";
        return new JoinClause(this, "LEFT JOIN", "(" + subBuilder.toSql() + ")" + aliasStr);
    }

    public JoinClause rightJoin(String table) {
        return new JoinClause(this, "RIGHT JOIN", table);
    }

    public JoinClause rightJoin(TkTable table) {
        return new JoinClause(this, "RIGHT JOIN", table.getTableName() + " " + table.getAlias());
    }

    public JoinClause rightJoin(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " " + subBuilder.alias : "";
        return new JoinClause(this, "RIGHT JOIN", "(" + subBuilder.toSql() + ")" + aliasStr);
    }

    public JoinClause innerJoin(String table) {
        return new JoinClause(this, "INNER JOIN", table);
    }

    public JoinClause innerJoin(TkTable table) {
        return new JoinClause(this, "INNER JOIN", table.getTableName() + " " + table.getAlias());
    }

    public JoinClause innerJoin(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " " + subBuilder.alias : "";
        return new JoinClause(this, "INNER JOIN", "(" + subBuilder.toSql() + ")" + aliasStr);
    }

    public SelectBuilder crossJoin(String table) {
        joins.add("CROSS JOIN " + table);
        return this;
    }

    public SelectBuilder crossJoin(TkTable table) {
        joins.add("CROSS JOIN " + table.getTableName() + " " + table.getAlias());
        return this;
    }

    public SelectBuilder crossJoin(SelectBuilder subBuilder) {
        for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
            this.queryWrapper.put(entry.getKey(), entry.getValue());
        }
        String aliasStr = subBuilder.alias != null ? " " + subBuilder.alias : "";
        joins.add("CROSS JOIN (" + subBuilder.toSql() + ")" + aliasStr);
        return this;
    }

    // ============================
    // WHERE
    // ============================

    /**
     * 以 lambda 表达式方式内联构建 WHERE 条件。这能保持 SelectBuilder 流式 API 的完美连贯性。
     */
    public SelectBuilder where(Consumer<QueryWrapper> consumer) {
        consumer.accept(this.queryWrapper);
        return this;
    }

    // ============================
    // GROUP BY / HAVING（可直接从 SelectBuilder 调用）
    // ============================

    public SelectBuilder groupBy(String... columns) {
        for (String col : columns) {
            groupBys.add(col);
        }
        return this;
    }

    public SelectBuilder groupBy(TkColumn... columns) {
        for (TkColumn col : columns) {
            groupBys.add(col.getSql());
        }
        return this;
    }

    public SelectBuilder having(String havingCondition) {
        this.havingClause = havingCondition;
        return this;
    }

    public SelectBuilder having(Consumer<QueryWrapper> consumer) {
        QueryWrapper wrapper = new QueryWrapper();
        consumer.accept(wrapper);
        List<String> subConds = wrapper.getConditions();
        if (!subConds.isEmpty()) {
            this.havingClause = String.join(" AND ", subConds);
            for (Map.Entry<String, Object> entry : wrapper.entrySet()) {
                this.queryWrapper.put(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    // ============================
    // ORDER BY
    // ============================

    public SelectBuilder orderByAsc(String column) {
        orderBys.add(column + " ASC");
        return this;
    }

    public SelectBuilder orderByAsc(TkColumn column) {
        orderBys.add(column.getSql() + " ASC");
        return this;
    }

    public SelectBuilder orderByDesc(String column) {
        orderBys.add(column + " DESC");
        return this;
    }

    public SelectBuilder orderByDesc(TkColumn column) {
        orderBys.add(column.getSql() + " DESC");
        return this;
    }

    // ============================
    // LIMIT / OFFSET
    // ============================

    public SelectBuilder limit(int count) {
        this.limitCount = count;
        return this;
    }

    public SelectBuilder limit(int count, int offset) {
        this.limitCount = count;
        this.offsetCount = offset;
        return this;
    }

    // ============================
    // SQL 生成
    // ============================

    /**
     * 构建完整 SQL：SELECT ... FROM ... JOIN ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT ...
     */
    public String toSql() {
        StringBuilder sb = new StringBuilder();

        // SELECT
        if (!selectColumns.isEmpty()) {
            sb.append("SELECT ").append(String.join(", ", selectColumns));
        } else {
            sb.append("SELECT *");
        }

        // FROM
        if (fromTable != null) {
            sb.append(" FROM ").append(fromTable);
        }

        // JOINs
        for (String join : joins) {
            sb.append(" ").append(join);
        }

        // WHERE
        List<String> conditions = queryWrapper.getConditions();
        if (!conditions.isEmpty()) {
            sb.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        // GROUP BY
        if (!groupBys.isEmpty()) {
            sb.append(" GROUP BY ").append(String.join(", ", groupBys));
        }

        // HAVING
        if (havingClause != null) {
            sb.append(" HAVING ").append(havingClause);
        }

        // ORDER BY
        if (!orderBys.isEmpty()) {
            sb.append(" ORDER BY ").append(String.join(", ", orderBys));
        }

        // LIMIT
        if (limitCount != null) {
            sb.append(" LIMIT ").append(limitCount);
            if (offsetCount != null) {
                sb.append(" OFFSET ").append(offsetCount);
            }
        }

        return sb.toString();
    }

    /**
     * 获取所有参数（包含子查询中的参数）
     */
    public Map<String, Object> getParams() {
        return queryWrapper.getParams();
    }

    // ============================
    // JoinClause 辅助类
    // ============================

    /**
     * JOIN 子句辅助类，用于分离 JOIN 和 ON
     */
    public static class JoinClause {
        private final SelectBuilder builder;
        private final String joinType;
        private final String table;

        JoinClause(SelectBuilder builder, String joinType, String table) {
            this.builder = builder;
            this.joinType = joinType;
            this.table = table;
        }

        public SelectBuilder on(String condition) {
            builder.joins.add(joinType + " " + table + " ON " + condition);
            return builder;
        }

        public SelectBuilder on(Consumer<QueryWrapper> consumer) {
            QueryWrapper wrapper = new QueryWrapper();
            consumer.accept(wrapper);
            List<String> subConds = wrapper.getConditions();
            if (!subConds.isEmpty()) {
                builder.joins.add(joinType + " " + table + " ON (" + String.join(" AND ", subConds) + ")");
                for (Map.Entry<String, Object> entry : wrapper.entrySet()) {
                    builder.queryWrapper.put(entry.getKey(), entry.getValue());
                }
            } else {
                builder.joins.add(joinType + " " + table);
            }
            return builder;
        }

        /**
         * ON 子句带参数绑定，参数以 key1, value1, key2, value2 ... 的形式传入
         */
        public SelectBuilder on(String condition, Object... kvPairs) {
            builder.joins.add(joinType + " " + table + " ON " + condition);
            for (int i = 0; i < kvPairs.length; i += 2) {
                builder.queryWrapper.put((String) kvPairs[i], kvPairs[i + 1]);
            }
            return builder;
        }
    }

    // (已移除 InternalQueryWrapper)
}
