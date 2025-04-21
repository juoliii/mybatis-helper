package com.bitian.db.mybatis.pagination.dialect

import com.bitian.common.dto.BaseForm
import com.bitian.db.mybatis.pagination.SqlParser

/**
 * mysql 分页方言
 * @author admin
 */
@Singleton
class MySQLPaginationDialect extends PaginationDialect{

    @Override
    String countSql(String originSql,BaseForm form) {
        return SqlParser.parseCountSql(originSql)
    }

    @Override
    String pageSql(String originSql,BaseForm form) {
        int offset=form.ps*(form.pn-1)
        return "${originSql} LIMIT ${form.ps} OFFSET ${offset}"
    }

}
