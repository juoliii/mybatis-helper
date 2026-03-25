package com.bitian.db.mybatis_helper.tk;

import org.apache.ibatis.mapping.MappedStatement;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;
import tk.mybatis.mapper.mapperhelper.SqlHelper;

public class SelectByQueryProvider extends MapperTemplate {

    public SelectByQueryProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
        super(mapperClass, mapperHelper);
    }

    /**
     * 基于 QueryWrapper 动态查询
     *
     * @param ms MappedStatement
     * @return 返回生成的动态 SQL
     */
    public String selectByQuery(MappedStatement ms) {
        Class<?> entityClass = getEntityClass(ms);
        // 修改返回值类型为实体类型
        setResultType(ms, entityClass);

        StringBuilder sql = new StringBuilder();
        // SELECT 所有的列
        sql.append(SqlHelper.selectAllColumns(entityClass));
        // FROM 表名
        sql.append(SqlHelper.fromTable(entityClass, tableName(entityClass)));
        // 动态追加 QueryWrapper 中生成的条件（WHERE ... ORDER BY ...）
        // 因为 QueryWrapper 继承了 HashMap 并覆盖了 get 方法，所以这里的 ${sqlSegment} 会被正确解析成条件字符串
        sql.append("${sqlSegment}");
        
        return sql.toString();
    }
}
