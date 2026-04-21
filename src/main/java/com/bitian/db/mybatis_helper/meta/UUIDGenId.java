package com.bitian.db.mybatis_helper.meta;

import java.util.UUID;

/**
 * 内置 UUID 主键生成器，生成 32 位无分隔符的 UUID 字符串。
 * <p>
 * 使用方式：{@code @KeySql(type = IdGenerationType.UUID)}
 * 无需显式指定 genId，UUID 策略会自动使用此生成器。
 */
public class UUIDGenId implements GenId<String> {

    @Override
    public String genId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
