package com.bitian.db.mybatis_helper.meta;

import java.lang.reflect.Field;

/**
 * 实体类字段与数据库列的映射信息
 */
public class ColumnInfo {

    /** Java 字段对象 */
    private final Field field;

    /** 数据库列名 */
    private final String columnName;

    /** 是否为主键 */
    private final boolean isId;

    /** 主键生成策略类型（仅主键字段有效） */
    private final IdGenerationType generationType;

    /** SQL 生成主键时使用的 SQL（仅 SQL 策略有效） */
    private final String keySql;

    /** 自定义生成器类（仅 CUSTOM 策略有效） */
    private final Class<? extends GenId> genIdClass;

    public ColumnInfo(Field field, String columnName, boolean isId) {
        this(field, columnName, isId, IdGenerationType.NONE, "", GenId.None.class);
    }

    public ColumnInfo(Field field, String columnName, boolean isId,
                      IdGenerationType generationType, String keySql,
                      Class<? extends GenId> genIdClass) {
        this.field = field;
        this.field.setAccessible(true);
        this.columnName = columnName;
        this.isId = isId;
        this.generationType = generationType;
        this.keySql = keySql;
        this.genIdClass = genIdClass;
    }

    public Field getField() {
        return field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isId() {
        return isId;
    }

    public IdGenerationType getGenerationType() {
        return generationType;
    }

    public String getKeySql() {
        return keySql;
    }

    public Class<? extends GenId> getGenIdClass() {
        return genIdClass;
    }

    /**
     * 从实体对象中获取该字段的值
     */
    public Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + field.getName(), e);
        }
    }

    /**
     * 将值设置到实体对象的该字段上（自动进行数字类型转换）
     */
    public void setValue(Object entity, Object value) {
        try {
            field.set(entity, convertType(value, field.getType()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + field.getName(), e);
        }
    }

    /**
     * 将值转换为目标类型（处理数据库返回的数字类型与实体字段类型不一致的情况）
     */
    private static Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            }
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            }
            if (targetType == Short.class || targetType == short.class) {
                return num.shortValue();
            }
            if (targetType == Byte.class || targetType == byte.class) {
                return num.byteValue();
            }
            if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            }
            if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            }
            if (targetType == String.class) {
                return num.toString();
            }
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }
}
