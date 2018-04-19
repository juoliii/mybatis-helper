package com.bitian.db.mybatis_helper;

import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Test;

import junit.framework.TestCase;

public class AppTest extends TestCase {
	
	@Test
    public void test1( ) {
    	BasicDataSource ds=new BasicDataSource();
    	ds.setUsername("root");
    	ds.setPassword("123456");
    	ds.setUrl("jdbc:mysql://192.168.0.15:3306/beesgarden?useUnicode\\=true&characterEncoding\\=utf8");
    	ds.setDriverClassName("com.mysql.jdbc.Driver");
    	TransactionFactory f=new JdbcTransactionFactory();
    	Environment en=new Environment("weffe", f, ds);
    	XMLConfigBuilder builder=new XMLConfigBuilder(PageHelper.class.getClassLoader().getResourceAsStream("mybatis-config.xml"));
    	Configuration c=builder.parse();
    	System.out.println(c.isMapUnderscoreToCamelCase());
    	System.out.println(c.getParameterMapNames().size());
    	System.out.println(c.getMappedStatementNames().size());
    	c.setEnvironment(en);
    	SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(c);
    	SqlSession session=factory.openSession();
    	PageHelper.startPage(1, 1);
    	System.out.println(session.getMapper(TestMapper.class).selectById(0));
    	Page page=PageHelper.getPage();
    	System.out.println(page.getTotal());
    	session.close();
    }

}
