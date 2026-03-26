package com.bitian.db.mybatis_helper.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class QueryWrapper extends HashMap<String, Object> {
    private static final AtomicInteger globalParamCount = new AtomicInteger();

    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderBys = new ArrayList<>();

    // --- EQ (等于) ---
    public QueryWrapper eq(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " = #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper eq(String column, Object value) {
        return eq(column, value, true);
    }

    // --- NE (不等于) ---
    public QueryWrapper ne(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " != #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper ne(String column, Object value) {
        return ne(column, value, true);
    }

    // --- GT (大于) ---
    public QueryWrapper gt(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " > #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper gt(String column, Object value) {
        return gt(column, value, true);
    }

    // --- GE (大于等于) ---
    public QueryWrapper ge(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " >= #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper ge(String column, Object value) {
        return ge(column, value, true);
    }

    // --- LT (小于) ---
    public QueryWrapper lt(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " < #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper lt(String column, Object value) {
        return lt(column, value, true);
    }

    // --- LE (小于等于) ---
    public QueryWrapper le(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " <= #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public QueryWrapper le(String column, Object value) {
        return le(column, value, true);
    }

    // --- LIKE (模糊查询) ---
    public QueryWrapper like(String column, String value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " LIKE #{" + paramName + "}");
            // 自动包裹 % 符号
            this.put(paramName, "%" + value + "%");
        }
        return this;
    }

    public QueryWrapper like(String column, String value) {
        return like(column, value, true);
    }

    // --- IN (包含) ---
    public QueryWrapper in(String column, List<?> values, boolean condition) {
        if (condition && values != null && !values.isEmpty()) {
            List<String> inParams = new ArrayList<>();
            for (Object value : values) {
                String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
                inParams.add("#{" + paramName + "}");
                this.put(paramName, value);
            }
            conditions.add(column + " IN (" + String.join(", ", inParams) + ")");
        }
        return this;
    }

    public QueryWrapper in(String column, List<?> values) {
        return in(column, values, true);
    }

    // --- IS NULL ---
    public QueryWrapper isNull(String column, boolean condition) {
        if (condition) {
            conditions.add(column + " IS NULL");
        }
        return this;
    }

    public QueryWrapper isNull(String column) {
        return isNull(column, true);
    }

    // --- IS NOT NULL ---
    public QueryWrapper isNotNull(String column, boolean condition) {
        if (condition) {
            conditions.add(column + " IS NOT NULL");
        }
        return this;
    }

    public QueryWrapper isNotNull(String column) {
        return isNotNull(column, true);
    }

    // --- ORDER BY ---
    public QueryWrapper orderByAsc(String column, boolean condition) {
        if (condition) {
            orderBys.add(column + " ASC");
        }
        return this;
    }

    public QueryWrapper orderByAsc(String column) {
        return orderByAsc(column, true);
    }

    public QueryWrapper orderByDesc(String column, boolean condition) {
        if (condition) {
            orderBys.add(column + " DESC");
        }
        return this;
    }

    public QueryWrapper orderByDesc(String column) {
        return orderByDesc(column, true);
    }

    // --- OR / AND 嵌套 ---
    public QueryWrapper or(Consumer<QueryWrapper> consumer) {
        QueryWrapper subWrapper = new QueryWrapper();
        consumer.accept(subWrapper);
        List<String> subConds = subWrapper.getConditions();
        if (!subConds.isEmpty()) {
            for (Map.Entry<String, Object> entry : subWrapper.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            String orCond = "(" + String.join(" AND ", subConds) + ")";
            if (!conditions.isEmpty()) {
                String last = conditions.remove(conditions.size() - 1);
                conditions.add(last + " OR " + orCond);
            } else {
                conditions.add(orCond);
            }
        }
        return this;
    }

    public QueryWrapper and(Consumer<QueryWrapper> consumer) {
        QueryWrapper subWrapper = new QueryWrapper();
        consumer.accept(subWrapper);
        List<String> subConds = subWrapper.getConditions();
        if (!subConds.isEmpty()) {
            for (Map.Entry<String, Object> entry : subWrapper.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            conditions.add("(" + String.join(" AND ", subConds) + ")");
        }
        return this;
    }

    // --- APPLY (自定义片段) ---
    public QueryWrapper apply(String sqlSegment) {
        conditions.add(sqlSegment);
        return this;
    }

    public QueryWrapper apply(String sqlSegment, boolean condition) {
        if (condition) {
            conditions.add(sqlSegment);
        }
        return this;
    }

    public QueryWrapper apply(String sqlSegment, Object... kvPairs) {
        conditions.add(sqlSegment);
        for (int i = 0; i < kvPairs.length; i += 2) {
            this.put((String) kvPairs[i], kvPairs[i + 1]);
        }
        return this;
    }

    // --- BETWEEN / NOT BETWEEN ---
    public QueryWrapper between(String column, Object start, Object end, boolean condition) {
        if (condition) {
            String paramStart = "_qw_param_" + globalParamCount.getAndIncrement();
            String paramEnd = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " BETWEEN #{" + paramStart + "} AND #{" + paramEnd + "}");
            this.put(paramStart, start);
            this.put(paramEnd, end);
        }
        return this;
    }

    public QueryWrapper between(String column, Object start, Object end) {
        return between(column, start, end, true);
    }

    public QueryWrapper notBetween(String column, Object start, Object end, boolean condition) {
        if (condition) {
            String paramStart = "_qw_param_" + globalParamCount.getAndIncrement();
            String paramEnd = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " NOT BETWEEN #{" + paramStart + "} AND #{" + paramEnd + "}");
            this.put(paramStart, start);
            this.put(paramEnd, end);
        }
        return this;
    }

    public QueryWrapper notBetween(String column, Object start, Object end) {
        return notBetween(column, start, end, true);
    }

    // --- NOT IN ---
    public QueryWrapper notIn(String column, List<?> values, boolean condition) {
        if (condition && values != null && !values.isEmpty()) {
            List<String> inParams = new ArrayList<>();
            for (Object value : values) {
                String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
                inParams.add("#{" + paramName + "}");
                this.put(paramName, value);
            }
            conditions.add(column + " NOT IN (" + String.join(", ", inParams) + ")");
        }
        return this;
    }

    public QueryWrapper notIn(String column, List<?> values) {
        return notIn(column, values, true);
    }

    // --- LIKE LEFT / RIGHT ---
    public QueryWrapper likeLeft(String column, String value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " LIKE #{" + paramName + "}");
            this.put(paramName, "%" + value);
        }
        return this;
    }

    public QueryWrapper likeLeft(String column, String value) {
        return likeLeft(column, value, true);
    }

    public QueryWrapper likeRight(String column, String value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + globalParamCount.getAndIncrement();
            conditions.add(column + " LIKE #{" + paramName + "}");
            this.put(paramName, value + "%");
        }
        return this;
    }

    public QueryWrapper likeRight(String column, String value) {
        return likeRight(column, value, true);
    }

    // --- SUBQUERIES (EXISTS, IN) ---
    public QueryWrapper exists(SelectBuilder subBuilder, boolean condition) {
        if (condition) {
            for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            conditions.add("EXISTS (" + subBuilder.toSql() + ")");
        }
        return this;
    }

    public QueryWrapper exists(SelectBuilder subBuilder) {
        return exists(subBuilder, true);
    }

    public QueryWrapper notExists(SelectBuilder subBuilder, boolean condition) {
        if (condition) {
            for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            conditions.add("NOT EXISTS (" + subBuilder.toSql() + ")");
        }
        return this;
    }

    public QueryWrapper notExists(SelectBuilder subBuilder) {
        return notExists(subBuilder, true);
    }

    public QueryWrapper inSql(String column, SelectBuilder subBuilder, boolean condition) {
        if (condition) {
            for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            conditions.add(column + " IN (" + subBuilder.toSql() + ")");
        }
        return this;
    }

    public QueryWrapper inSql(String column, SelectBuilder subBuilder) {
        return inSql(column, subBuilder, true);
    }

    public QueryWrapper notInSql(String column, SelectBuilder subBuilder, boolean condition) {
        if (condition) {
            for (Map.Entry<String, Object> entry : subBuilder.getParams().entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            conditions.add(column + " NOT IN (" + subBuilder.toSql() + ")");
        }
        return this;
    }

    public QueryWrapper notInSql(String column, SelectBuilder subBuilder) {
        return notInSql(column, subBuilder, true);
    }

    // --- 获取结果 ---
    public String getSqlSegment() {
        StringBuilder sb = new StringBuilder();
        if (!conditions.isEmpty()) {
            sb.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        if (!orderBys.isEmpty()) {
            sb.append(" ORDER BY ").append(String.join(", ", orderBys));
        }
        return sb.toString();
    }

    public List<String> getConditions() {
        return conditions;
    }

    public List<String> getOrderBys() {
        return orderBys;
    }

    public Map<String, Object> getParams() {
        return this;
    }

    @Override
    public Object get(Object key) {
        if ("sqlSegment".equals(key)) {
            return getSqlSegment();
        }
        return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        if ("sqlSegment".equals(key)) {
            return true;
        }
        return super.containsKey(key);
    }
}
