package com.bitian.db.mybatis_helper.dialect;

import com.bitian.db.mybatis_helper.Page;

public abstract class BtDialect {
	public abstract String countSql(String sql);
	public abstract String pageSql(String sql,Page page);
}
