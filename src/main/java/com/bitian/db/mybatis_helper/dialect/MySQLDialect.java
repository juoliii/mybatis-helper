package com.bitian.db.mybatis_helper.dialect;

import com.bitian.db.mybatis_helper.Page;

public class MySQLDialect extends BtDialect{

	@Override
	public String countSql(String sql) {
		sql="select count(*) as total from ("+sql+") tefwe";
		return sql;
	}

	@Override
	public String pageSql(String sql,Page page) {
		int offset=page.getPs()*(page.getPn()-1);
		sql=sql+ " limit "+offset+","+page.getPs();
		return sql;
	}

}
