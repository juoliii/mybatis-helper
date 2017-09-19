package com.bitian.db.mybatis_helper.plugins;

import java.sql.Connection;
import java.util.Properties;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class}) })
public class BitianInterceptor implements Interceptor{

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        System.out.println(sql);
        return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
		System.out.println(properties);
	}

}
