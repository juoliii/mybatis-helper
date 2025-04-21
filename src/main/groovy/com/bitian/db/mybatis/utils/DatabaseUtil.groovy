package com.bitian.db.mybatis.utils

import com.bitian.common.exception.CustomException
import com.bitian.db.mybatis.constants.DataBase

/**
 * @author admin
 */
class DatabaseUtil {

    /**
     * 根据jdbc连接获取数据库类型
     * @param jdbcUrl
     * @return
     */
    static DataBase parseDb(String jdbcUrl) {
        String url = jdbcUrl.toLowerCase()
        if (url.contains(":mysql:")) {
            return DataBase.MYSQL;
        } else if (url.contains(":mariadb:")) {
            return DataBase.MARIADB;
        } else if (url.contains(":oracle:")) {
            return DataBase.ORACLE;
        } else if (url.contains(":sqlserver:") || url.contains(":microsoft:")) {
            return DataBase.SQLSERVER;
        } else if (url.contains(":postgresql:")) {
            return DataBase.POSTGRESQL;
        } else if (url.contains(":db2:")) {
            return DataBase.DB2;
        } else if (url.contains(":sqlite:")) {
            return DataBase.SQLITE;
        } else if (url.contains(":h2:")) {
            return DataBase.H2;
        } else {
            throw new CustomException("unsupported database")
        }
    }

}
