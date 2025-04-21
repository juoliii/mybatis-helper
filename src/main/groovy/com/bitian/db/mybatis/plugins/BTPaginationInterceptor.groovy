package com.bitian.db.mybatis.plugins

import com.bitian.common.dto.BaseForm
import com.bitian.common.exception.CustomException
import com.bitian.db.mybatis.pagination.Constants
import com.bitian.db.mybatis.pagination.dialect.PaginationDialect
import com.bitian.db.mybatis.utils.DatabaseUtil
import com.bitian.db.mybatis.utils.MybatisUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.ibatis.executor.parameter.ParameterHandler
import org.apache.ibatis.executor.statement.BaseStatementHandler
import org.apache.ibatis.executor.statement.RoutingStatementHandler
import org.apache.ibatis.executor.statement.StatementHandler
import org.apache.ibatis.mapping.BoundSql
import org.apache.ibatis.mapping.MappedStatement
import org.apache.ibatis.plugin.Interceptor
import org.apache.ibatis.plugin.Intercepts
import org.apache.ibatis.plugin.Invocation
import org.apache.ibatis.plugin.Signature
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler
import org.apache.ibatis.session.ResultHandler

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * 分页插件
 * @author admin
 */
@Intercepts([
        @Signature(type = StatementHandler.class, method = "query", args = [ Statement.class,ResultHandler.class ])
])
@CompileStatic
class BTPaginationInterceptor implements Interceptor{

    @Override
    Object intercept(Invocation invocation) throws Throwable {
        // 必须是继承自BaseStatementHandler才执行分页逻辑
        if (invocation.getTarget() instanceof RoutingStatementHandler) {
            Statement statement = invocation.getArgs()[0] as Statement
            BaseStatementHandler handler=invocation.target.getProperties().get("delegate") as BaseStatementHandler
            def param= handler.parameterHandler.parameterObject
            BaseForm form= MybatisUtil.getBaseForm(param)
            //只有mapper的参数中有BaseForm类型才会执行分页
            if(form && form.pn>0 && form.ps>0){
                MappedStatement mappedStatement=handler.getProperties().get("mappedStatement") as MappedStatement
                BoundSql boundSql = handler.getBoundSql()
                PaginationDialect dialect=PaginationDialect.dialect(DatabaseUtil.parseDb(statement.connection.metaData.URL))
                long total=count(mappedStatement,boundSql,dialect,form,statement)
                form.set_total(total.toInteger())
                //构造分页boundSql
                BoundSql pageBoundSql = new BoundSql(mappedStatement.getConfiguration(), dialect.pageSql(boundSql.getSql(),form), boundSql.getParameterMappings(), boundSql.getParameterObject())
                boundSql.additionalParameters.forEach((k,v)->pageBoundSql.setAdditionalParameter(k.toString(),v))
                //替换StatementHandler中的boundSql
                handler.metaClass.setProperty(handler,'boundSql',pageBoundSql)
                //替换statement
                Statement stmt = handler.prepare(statement.connection, mappedStatement.timeout)
                handler.parameterize(stmt)
                invocation.getArgs()[0]=stmt
            }
        }
        return invocation.proceed()
    }

    private long count(MappedStatement mappedStatement, BoundSql boundSql, PaginationDialect dialect,BaseForm form,Statement statement) {
        // 通过查询Sql语句获取到对应的计算总记录数的sql语句
        String countSql = dialect.countSql(boundSql.getSql(),form)
        BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject())
        boundSql.additionalParameters.forEach((k,v)->countBoundSql.setAdditionalParameter(k.toString(),v))
        ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), countBoundSql)
        PreparedStatement pstmt = null
        ResultSet rs = null
        long totalRecord=0
        try {
            pstmt = statement.connection.prepareStatement(countSql)
            parameterHandler.setParameters(pstmt)
            rs = pstmt.executeQuery()
            if (rs.next()) {
                totalRecord = rs.getLong(Constants.COUNT_COLUMN_ALIAS)
            }
        } catch (SQLException e) {
            e.printStackTrace()
            throw new CustomException(e.getMessage())
        } finally {
            try {
                if (rs != null)
                    rs.close()
                if (pstmt != null)
                    pstmt.close()
            } catch (SQLException e) {
                e.printStackTrace()
            }
        }
        return totalRecord
    }

    @Override
    void setProperties(Properties properties) {
        super.setProperties(properties)
    }
}
