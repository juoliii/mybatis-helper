package com.bitian.db.mybatis_helper.tk;

import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TkQueryWrapper<T> extends HashMap<String, Object> {
    private int paramCount = 0;
    /**
     * 条件片段列表，每个元素是一个完整的条件片段。
     * 普通条件直接存储（如 "name = #{_qw_param_0}"），
     * 分组条件存储为 "AND (...)" 或 "OR (...)" 。
     */
    private final List<String> conditions = new ArrayList<>();
    private final List<String> orderBys = new ArrayList<>();
    private final Class<T> entityClass;
    private final Map<String, String> propertyToColumnMap = new HashMap<>();

    /**
     * 静态实例化方法
     * @param entityClass
     * @return
     * @param <T>
     */
    public static <T> TkQueryWrapper<T> n(Class<T> entityClass){
        return new TkQueryWrapper<>(entityClass);
    }

    public TkQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        initColumnMap();
    }

    /**
     * 内部构造，用于创建不需要初始化列映射的子 wrapper（and/or 分组时使用）。
     * 共享父 wrapper 的 entityClass 和 propertyToColumnMap。
     */
    private TkQueryWrapper(Class<T> entityClass, Map<String, String> columnMap, int paramStartIndex) {
        this.entityClass = entityClass;
        this.propertyToColumnMap.putAll(columnMap);
        this.paramCount = paramStartIndex;
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

    // --- APPLY (自定义 SQL 片段) ---

    /**
     * 添加自定义 SQL 条件片段。
     * <pre>
     * wrapper.apply("DATE(create_time) = '2024-01-01'");
     * </pre>
     */
    public TkQueryWrapper<T> apply(String sqlSegment) {
        conditions.add(sqlSegment);
        return this;
    }

    public TkQueryWrapper<T> apply(String sqlSegment, boolean condition) {
        if (condition) {
            conditions.add(sqlSegment);
        }
        return this;
    }

    // ============================
    // AND / OR 分组
    // ============================

    /**
     * AND 分组：将 Consumer 中构建的条件用 AND (...) 包裹。
     * <pre>
     * wrapper.eq("status", 1)
     *        .and(w -> w.eq("name", "test").or().eq("name", "test2"));
     * // 生成: status = ? AND (name = ? OR name = ?)
     * </pre>
     */
    public TkQueryWrapper<T> and(Consumer<TkQueryWrapper<T>> consumer) {
        TkQueryWrapper<T> subWrapper = new TkQueryWrapper<>(entityClass, propertyToColumnMap, paramCount);
        consumer.accept(subWrapper);
        return mergeGroup("AND", subWrapper);
    }

    /**
     * OR 分组：将 Consumer 中构建的条件用 OR (...) 包裹。
     * <pre>
     * wrapper.eq("status", 1)
     *        .or(w -> w.eq("name", "test").eq("age", 20));
     * // 生成: status = ? OR (name = ? AND age = ?)
     * </pre>
     */
    public TkQueryWrapper<T> or(Consumer<TkQueryWrapper<T>> consumer) {
        TkQueryWrapper<T> subWrapper = new TkQueryWrapper<>(entityClass, propertyToColumnMap, paramCount);
        consumer.accept(subWrapper);
        return mergeGroup("OR", subWrapper);
    }

    /**
     * 切换下一个条件的连接符为 OR（不分组）。
     * <pre>
     * wrapper.eq("status", 1).or().eq("status", 2);
     * // 生成: status = ? OR status = ?
     * </pre>
     */
    public TkQueryWrapper<T> or() {
        // 添加一个特殊标记，在 getSqlSegment 中处理
        conditions.add("__OR__");
        return this;
    }

    /**
     * 合并子 wrapper 的分组条件到当前 wrapper。
     */
    private TkQueryWrapper<T> mergeGroup(String connector, TkQueryWrapper<T> subWrapper) {
        String subSql = subWrapper.buildConditionSql();
        if (!subSql.isEmpty()) {
            // 同步 paramCount，避免后续参数名冲突
            this.paramCount = subWrapper.paramCount;
            // 合并参数
            for (Map.Entry<String, Object> entry : subWrapper.entrySet()) {
                String key = entry.getKey();
                // 跳过非参数的 key
                if (key.startsWith("_qw_param_")) {
                    this.put(key, entry.getValue());
                }
            }
            conditions.add(connector + " (" + subSql + ")");
        }
        return this;
    }

    /**
     * 将 conditions 列表中的条件构建为 SQL 字符串（不含 WHERE 关键字）。
     * 处理 __OR__ 标记来切换连接符。
     */
    private String buildConditionSql() {
        if (conditions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        boolean nextIsOr = false;

        for (String cond : conditions) {
            if ("__OR__".equals(cond)) {
                nextIsOr = true;
                continue;
            }

            if (first) {
                // 对于分组条件（AND (...) / OR (...)），第一个就可能是分组
                if (cond.startsWith("AND (") || cond.startsWith("OR (")) {
                    // 提取括号内的内容直接添加（第一个条件不需要连接符前缀）
                    sb.append(cond.substring(cond.indexOf('(') ));
                } else {
                    sb.append(cond);
                }
                first = false;
            } else {
                if (cond.startsWith("AND (") || cond.startsWith("OR (")) {
                    sb.append(" ").append(cond);
                } else {
                    sb.append(nextIsOr ? " OR " : " AND ").append(cond);
                }
            }
            nextIsOr = false;
        }

        return sb.toString();
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
        String conditionSql = buildConditionSql();
        if (!conditionSql.isEmpty()) {
            sb.append(" WHERE ").append(conditionSql);
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
