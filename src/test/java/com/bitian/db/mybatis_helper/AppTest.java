package com.bitian.db.mybatis_helper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.bitian.db.mybatis.MybatisInject;
import com.bitian.db.mybatis_helper.entity.SysUser;
import com.bitian.db.mybatis_helper.mapper.TestMapper;
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
    	ds.setPassword("1qaz2wsx");
    	ds.setUrl("jdbc:mysql://192.168.31.99:3306/caika?useUnicode=true&characterEncoding=utf8");
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
//		List<String> slist=new ArrayList<>();
//		for(int i=0;i<1000;i++){
//			slist.add("batchname"+i);
//			System.out.println(i);
//		}
//		session.getMapper(TestMapper.class).insertBatch(slist);
//		session.commit();
//		session=factory.openSession();
//    	PageHelper.startPage(1, 20);
//    	List<Map<String, Object>> list=session.getMapper(TestMapper.class).select(0,"12");
//    	System.out.println(list);
		SysUser su=new SysUser();
		su.setId(16831L);
		su.setNickname("fwefwef");
		int n=session.getMapper(TestMapper.class).updateByPrimaryKeySelective(su);
		System.out.println(n);
//		TestDto dto=new TestDto();
//		dto.setId(1L);
//		dto.setAbc("abc123");
//		List<Map<String, Object>> list1=session.getMapper(TestMapper.class).selectAll1(dto);
//    	session.close();
    }

	public static void main(String[] args) {
		BasicDataSource ds=new BasicDataSource();
		ds.setUsername("root");
		ds.setPassword("asdf-123");
		ds.setUrl("jdbc:mysql://192.168.20.14:3306/test_groovy?useUnicode=true&characterEncoding=utf8");
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
		SqlSession session=factory.openSession(true);
//		SysUser su=new SysUser();
//		su.setNickname("fwefwef11");
//		su.setUsername("fwefwefffeeff");
//		su.setPassword("ffwefwefgergrg0988");
//		int n=session.getMapper(TestMapper.class).insertSelective(su);
//		System.out.println(n+"===="+su.getId());
	}

}
