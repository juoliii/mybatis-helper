package com.bitian.db.mybatis_helper.meta;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体类元数据解析器。
 * <p>
 * 通过 JPA 注解（{@link Table}、{@link Column}、{@link Id}、{@link Transient}）
 * 和自定义注解（{@link KeySql}）解析实体类与数据库表/列的映射关系，并缓存解析结果。
 * <p>
 * 命名规则：
 * <ul>
 *   <li>表名：优先取 {@code @Table(name = "...")}，否则将类名转为下划线命名</li>
 *   <li>列名：优先取 {@code @Column(name = "...")}，否则将字段名转为下划线命名</li>
 * </ul>
 */
public class EntityResolver {

    /** 元数据缓存，线程安全 */
    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    /**
     * 解析实体类元数据（带缓存）
     *
     * @param clazz 实体类
     * @return 解析后的元数据
     */
    public static EntityMetadata resolve(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, EntityResolver::doParse);
    }

    private static EntityMetadata doParse(Class<?> clazz) {
        // 解析表名
        String tableName = resolveTableName(clazz);

        // 解析所有字段（包括父类字段）
        List<ColumnInfo> columns = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                // 跳过 static、transient 修饰的字段
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                // 跳过 @Transient 注解标记的字段
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                String columnName = resolveColumnName(field);
                boolean isId = field.isAnnotationPresent(Id.class);

                // 解析主键生成策略
                IdGenerationType genType = IdGenerationType.NONE;
                String keySql = "";
                Class<? extends GenId> genIdClass = GenId.None.class;

                if (isId) {
                    KeySql keySqlAnn = field.getAnnotation(KeySql.class);
                    if (keySqlAnn != null) {
                        genType = keySqlAnn.type();
                        keySql = keySqlAnn.sql();
                        genIdClass = keySqlAnn.genId();
                    }
                }

                columns.add(new ColumnInfo(field, columnName, isId, genType, keySql, genIdClass));
            }
            current = current.getSuperclass();
        }

        if (columns.isEmpty()) {
            throw new RuntimeException("Entity class " + clazz.getName() + " has no mapped columns.");
        }

        return new EntityMetadata(tableName, columns);
    }

    /**
     * 解析表名：优先取 @Table(name)，否则类名转下划线
     */
    private static String resolveTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return camelToUnderscore(clazz.getSimpleName());
    }

    /**
     * 解析列名：优先取 @Column(name)，否则字段名转下划线
     */
    private static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return camelToUnderscore(field.getName());
    }

    /**
     * 驼峰命名转下划线命名。
     * 例如：userName -> user_name, HTMLParser -> html_parser
     */
    static String camelToUnderscore(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
