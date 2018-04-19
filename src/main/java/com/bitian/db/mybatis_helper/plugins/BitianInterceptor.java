package com.bitian.db.mybatis_helper.plugins;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import com.bitian.db.mybatis_helper.Page;
import com.bitian.db.mybatis_helper.PageHelper;
import com.bitian.db.mybatis_helper.dialect.BtDialect;
import com.bitian.db.mybatis_helper.dialect.MySQLDialect;
import com.bitian.db.mybatis_helper.util.ReflectUtil;

@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }) })
public class BitianInterceptor extends PageHelper implements Interceptor {
	
	private BtDialect dialect;

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (invocation.getTarget() instanceof RoutingStatementHandler) {
			
			RoutingStatementHandler sth = (RoutingStatementHandler) invocation.getTarget();
			StatementHandler statementHandler = (StatementHandler) ReflectUtil.getFieldValue(sth, "delegate");
			BoundSql boundSql = statementHandler.getBoundSql();
			String sql = boundSql.getSql();
			// 查询sql
			if (isQuery(sql)) {
				MappedStatement mst=(MappedStatement) ReflectUtil.getFieldValueForMappedStatement(statementHandler);
				Connection connection = (Connection) invocation.getArgs()[0];
				setTotalRecord(mst, boundSql, connection);
				sql=dialect.pageSql(sql,PageHelper.getCurrentPage());
				ReflectUtil.setFieldValue(boundSql, "sql", sql);
			}
			System.out.println(sql);
		}
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler) {
			return Plugin.wrap(target, this);
		} else {
			return target;
		}
	}

	private void setTotalRecord(MappedStatement mappedStatement, BoundSql boundSql, Connection connection) {
		// 获取对应的BoundSql，这个BoundSql其实跟我们利用StatementHandler获取到的BoundSql是同一个对象。
		// delegate里面的boundSql也是通过mappedStatement.getBoundSql(paramObj)方法获取到的。
		// 获取到我们自己写在Mapper映射语句中对应的Sql语句
		String sql = boundSql.getSql();
		// 通过查询Sql语句获取到对应的计算总记录数的sql语句
		String countSql = dialect.countSql(sql);
		// 通过BoundSql获取对应的参数映射
		// 利用Configuration、查询记录数的Sql语句countSql、参数映射关系parameterMappings和参数对象page建立查询记录数对应的BoundSql对象。
		BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), countSql,
				boundSql.getParameterMappings(), boundSql.getParameterObject());
		// 通过mappedStatement、参数对象page和BoundSql对象countBoundSql建立一个用于设定参数的ParameterHandler对象
		ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(),
				countBoundSql);
		// 通过connection建立一个countSql对应的PreparedStatement对象。
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = connection.prepareStatement(countSql);
			// 通过parameterHandler给PreparedStatement对象设置参数
			parameterHandler.setParameters(pstmt);
			// 之后就是执行获取总记录数的Sql语句和获取结果了。
			rs = pstmt.executeQuery();
			if (rs.next()) {
				long totalRecord = rs.getLong("total");
				Page page=PageHelper.getCurrentPage();
				page.setTotal(totalRecord);
				// 给当前的参数page对象设置总记录数
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (pstmt != null)
					pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isQuery(String sql) {
		return sql.trim().toLowerCase().startsWith("select");
	}

	@Override
	public void setProperties(Properties properties) {
		String dialect=properties.getOrDefault("dialect", "mysql").toString();
		if(dialect.toLowerCase().equals("mysql")){
			this.dialect=new MySQLDialect();
		}
	}

}
