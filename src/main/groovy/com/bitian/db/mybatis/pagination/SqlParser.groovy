package com.bitian.db.mybatis.pagination

import net.sf.jsqlparser.expression.Alias
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.*

/**
 * @author admin
 */
class SqlParser {

    static String wrapSql(String originSql) {
        return "SELECT COUNT(*) AS ${Constants.COUNT_COLUMN_ALIAS} FROM (${originSql}) _TMP_TAB_TOTAL"
    }

    static String parseCountSql(String originSql){

        Select select= CCJSqlParserUtil.parse(originSql)
        //如果sql包含union | union all 则解析出的对象为SetOperationList，此时无法处理，只能包装下返回
        if (select instanceof SetOperationList) {
            return wrapSql(originSql)
        }

        PlainSelect plainSelect = (PlainSelect) select

        // 如果最外层sql 有distinct | groupby 则无法处理
        if(plainSelect.distinct!=null || plainSelect.groupBy!=null){
            return wrapSql(originSql)
        }

        // 如果有with语句则无法处理
        if(plainSelect.withItemsList!=null){
            return wrapSql(originSql)
        }

        List<OrderByElement> orderByList = plainSelect.getOrderByElements()
        if(orderByList){
            int tmpNum=orderByList.count{it.expression.toString().contains("?")}
            if(tmpNum==0)
                //去除最外层sql中的order by
                plainSelect.orderByElements=null
        }

        int columnNum=plainSelect.selectItems.count{it.toString().contains("?")}
        // 如果主查询的列里有参数注入，则返回处理后的sql
        if(columnNum>0)
            return wrapSql(select.toString())

        //设置原始sql的查询column为count(*)
        plainSelect.selectItems=[new SelectItem<>(new Column().withColumnName(Constants.COUNT_COLUMN)).withAlias(new Alias(Constants.COUNT_COLUMN_ALIAS))]
        return plainSelect.toString()
    }

}
