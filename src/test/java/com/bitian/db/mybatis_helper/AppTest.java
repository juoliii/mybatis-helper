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
    	ds.setPassword("598741236hh");
    	ds.setUrl("jdbc:mysql://sh-cdb-0t0erw5p.sql.tencentcdb.com:63987/yonganjituan?useUnicode\\=true&characterEncoding\\=utf8");
    	ds.setDriverClassName("com.mysql.jdbc.Driver");
    	TransactionFactory f=new JdbcTransactionFactory();
    	Environment en=new Environment("weffe", f, ds);
    	XMLConfigBuilder builder=new XMLConfigBuilder(App.class.getClassLoader().getResourceAsStream("mybatis-config.xml"));
    	Configuration c=builder.parse();
    	System.out.println(c.isMapUnderscoreToCamelCase());
    	System.out.println(c.getParameterMapNames().size());
    	System.out.println(c.getMappedStatementNames().size());
    	c.setEnvironment(en);
    	SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(c);
    	SqlSession session=factory.openSession();
    	List<Map<String, Object>> list= session.selectList("com.bitian.test.selectAll");
    	System.out.println(list);
    	System.out.println(session.getMapper(TestMapper.class).selectById("1"));
    	session.close();
        System.out.println( "Hello World!" );
    }

}
