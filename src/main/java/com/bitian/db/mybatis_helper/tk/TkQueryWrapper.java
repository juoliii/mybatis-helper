package com.bitian.db.mybatis_helper.tk;

import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TkQueryWrapper<T> extends HashMap<String, Object> {
    private int paramCount = 0;
    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderBys = new ArrayList<>();
    private final Class<T> entityClass;
    private final Map<String, String> propertyToColumnMap = new HashMap<>();

    public TkQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        initColumnMap();
    }

    private void initColumnMap() {
        try {
            Set<EntityColumn> columns = EntityHelper.getColumns(entityClass);
            for (EntityColumn column : columns) {
                propertyToColumnMap.put(column.getProperty(), column.getColumn());
            }
        } catch (Exception e) {
            // fallback if entity class hasn't been initialized by tkmapper
        }
    }

    private String getColumn(String property) {
        String column = propertyToColumnMap.get(property);
        if (column == null) {
            // camel case to underscore conversion fallback if not found
            return camelToUnderline(property);
        }
        return column;
    }

    private String camelToUnderline(String param) {
        if (param == null || param.isEmpty()) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // --- EQ (等于) ---
    public TkQueryWrapper<T> eq(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " = #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> eq(String property, Object value) {
        return eq(property, value, true);
    }

    // --- NE (不等于) ---
    public TkQueryWrapper<T> ne(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " != #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> ne(String property, Object value) {
        return ne(property, value, true);
    }

    // --- GT (大于) ---
    public TkQueryWrapper<T> gt(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " > #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> gt(String property, Object value) {
        return gt(property, value, true);
    }

    // --- GE (大于等于) ---
    public TkQueryWrapper<T> ge(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " >= #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> ge(String property, Object value) {
        return ge(property, value, true);
    }

    // --- LT (小于) ---
    public TkQueryWrapper<T> lt(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " < #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> lt(String property, Object value) {
        return lt(property, value, true);
    }

    // --- LE (小于等于) ---
    public TkQueryWrapper<T> le(String property, Object value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " <= #{" + paramName + "}");
            this.put(paramName, value);
        }
        return this;
    }

    public TkQueryWrapper<T> le(String property, Object value) {
        return le(property, value, true);
    }

    // --- LIKE (模糊查询) ---
    public TkQueryWrapper<T> like(String property, String value, boolean condition) {
        if (condition) {
            String paramName = "_qw_param_" + paramCount++;
            conditions.add(getColumn(property) + " LIKE #{" + paramName + "}");
            // 自动包裹 % 符号
            this.put(paramName, "%" + value + "%");
        }
        return this;
    }

    public TkQueryWrapper<T> like(String property, String value) {
        return like(property, value, true);
    }

    // --- IN (包含) ---
    public TkQueryWrapper<T> in(String property, List<?> values, boolean condition) {
        if (condition && values != null && !values.isEmpty()) {
            List<String> inParams = new ArrayList<>();
            for (Object value : values) {
                String paramName = "_qw_param_" + paramCount++;
                inParams.add("#{" + paramName + "}");
                this.put(paramName, value);
            }
            conditions.add(getColumn(property) + " IN (" + String.join(", ", inParams) + ")");
        }
        return this;
    }

    public TkQueryWrapper<T> in(String property, List<?> values) {
        return in(property, values, true);
    }

    // --- IS NULL ---
    public TkQueryWrapper<T> isNull(String property, boolean condition) {
        if (condition) {
            conditions.add(getColumn(property) + " IS NULL");
        }
        return this;
    }

    public TkQueryWrapper<T> isNull(String property) {
        return isNull(property, true);
    }

    // --- IS NOT NULL ---
    public TkQueryWrapper<T> isNotNull(String property, boolean condition) {
        if (condition) {
            conditions.add(getColumn(property) + " IS NOT NULL");
        }
        return this;
    }

    public TkQueryWrapper<T> isNotNull(String property) {
        return isNotNull(property, true);
    }

    // --- ORDER BY ---
    public TkQueryWrapper<T> orderByAsc(String property, boolean condition) {
        if (condition) {
            orderBys.add(getColumn(property) + " ASC");
        }
        return this;
    }

    public TkQueryWrapper<T> orderByAsc(String property) {
        return orderByAsc(property, true);
    }

    public TkQueryWrapper<T> orderByDesc(String property, boolean condition) {
        if (condition) {
            orderBys.add(getColumn(property) + " DESC");
        }
        return this;
    }

    public TkQueryWrapper<T> orderByDesc(String property) {
        return orderByDesc(property, true);
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
