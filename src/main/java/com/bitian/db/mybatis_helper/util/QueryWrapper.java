package com.bitian.db.mybatis_helper.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryWrapper extends HashMap<String, Object> {
    private int paramCount = 0;
    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderBys = new ArrayList<>();

    // --- EQ (等于) ---
    public QueryWrapper eq(String column, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
            String paramName = "_qw_param_" + paramCount++;
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
                String paramName = "_qw_param_" + paramCount++;
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
