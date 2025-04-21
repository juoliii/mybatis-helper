package com.bitian.db.mybatis.pagination.dialect

import com.bitian.common.dto.BaseForm
import com.bitian.db.mybatis.constants.DataBase

/**
 * @author admin
 */
abstract class PaginationDialect {

    abstract String countSql(String originSql,BaseForm form)

    abstract String pageSql(String originSql,BaseForm form)

    static PaginationDialect dialect(DataBase db){
        return switch (db){
            case DataBase.MYSQL-> MySQLPaginationDialect.instance
            case DataBase.MARIADB -> throw new IllegalStateException()
            case DataBase.ORACLE -> throw new IllegalStateException()
            case DataBase.DB2 -> throw new IllegalStateException()
            case DataBase.H2 -> throw new IllegalStateException()
            case DataBase.SQLITE -> throw new IllegalStateException()
            case DataBase.POSTGRESQL -> throw new IllegalStateException()
            case DataBase.SQLSERVER -> throw new IllegalStateException()
        }
    }
}