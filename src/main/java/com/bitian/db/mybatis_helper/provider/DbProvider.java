package com.bitian.db.mybatis_helper.provider;

import java.util.Map;

public class DbProvider {
    public String executeSql(Map<String, Object> param) {
        return String.valueOf(param.get("_dbutil_sql"));
    }
}
