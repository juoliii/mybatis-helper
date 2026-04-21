package com.bitian.db.mybatis_helper.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主键生成策略注解，标注在 {@code @Id} 字段上。
 * <p>
 * 参考 tk-mapper 风格，支持多种主键生成方式：
 * <ul>
 *   <li>{@code @KeySql(type = IdGenerationType.IDENTITY)} — 数据库自增，插入后回填</li>
 *   <li>{@code @KeySql(type = IdGenerationType.UUID)} — 应用层生成 32 位 UUID</li>
 *   <li>{@code @KeySql(type = IdGenerationType.SQL, sql = "SELECT seq.nextval FROM DUAL")} — SQL 生成</li>
 *   <li>{@code @KeySql(type = IdGenerationType.CUSTOM, genId = MyGenId.class)} — 自定义生成器</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KeySql {

    /**
     * 主键生成策略类型
     */
    IdGenerationType type() default IdGenerationType.NONE;

    /**
     * SQL 表达式，仅在 {@code type = IdGenerationType.SQL} 时使用。
     * <p>
     * 例如：{@code "SELECT my_seq.nextval FROM DUAL"}
     */
    String sql() default "";

    /**
     * 自定义主键生成器类，仅在 {@code type = IdGenerationType.CUSTOM} 时使用。
     * <p>
     * 必须实现 {@link GenId} 接口，并提供无参构造函数。
     */
    Class<? extends GenId> genId() default GenId.None.class;
}
