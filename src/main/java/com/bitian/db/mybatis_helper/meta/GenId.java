package com.bitian.db.mybatis_helper.meta;

/**
 * 自定义主键生成器接口。
 * <p>
 * 实现此接口并在 {@code @KeySql(type = IdGenerationType.CUSTOM, genId = YourGenId.class)}
 * 中指定，即可使用自定义的主键生成逻辑。
 *
 * @param <T> 主键类型
 */
public interface GenId<T> {

    /**
     * 生成主键值
     *
     * @return 生成的主键值
     */
    T genId();

    /**
     * 空实现，用作注解默认值占位符
     */
    class None implements GenId<Object> {
        @Override
        public Object genId() {
            return null;
        }
    }
}
